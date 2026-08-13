import {
    nextTick,
    onScopeDispose,
    ref,
    watch,
    type ComputedRef,
    type Ref,
} from "vue";
import type {
    Location,
    Region,
} from "@/domain/World";
import type {
    SelectedGraphEntity,
    WorldGraphData,
} from "../types";
import {
    locationEntityKey,
    regionDepth,
    regionEntityKey,
    type RegionIndex,
} from "../utils/geometry";
import {
    entityVisibleAfterExpansion,
    formatWorldGraphSearchParameter,
    mapWithConcurrency,
    nameMatchesSearch,
    normalizeSearchText,
    parseWorldGraphSearch,
    requiredSearchExpansionRegions,
    searchLocationsByName,
    searchRegionsByName,
    selectedEntityKey,
    type WorldGraphSearchKind,
    type WorldGraphSearchResult,
} from "../utils/search";

const CHARACTER_SEARCH_CONCURRENCY = 8;
export const WORLD_GRAPH_SEARCH_DEBOUNCE_MS = 250;

export type WorldGraphSearchOptions = {
    worldId: ComputedRef<number>;
    graph: Ref<WorldGraphData>;
    regionIndex: ComputedRef<RegionIndex>;
    setRegionCollapsed: (
        region: Region,
        collapsed: boolean,
    ) => Promise<boolean>;
    replaceSelection: (
        entities: readonly SelectedGraphEntity[],
    ) => number;
    focusEntities: (
        entities: readonly SelectedGraphEntity[],
    ) => void;
    dismissPopovers: () => void;
};

type ResolvedWorldGraphSearchResult = Readonly<{
    result: WorldGraphSearchResult;
    entities: readonly SelectedGraphEntity[];
}>;

type SearchResolution = Readonly<{
    results: readonly ResolvedWorldGraphSearchResult[];
    failedCharacterLookups: number;
}>;

type StartingCharacter = Awaited<
    ReturnType<Location["getStartingHere"]>
>[number];

type CharacterBucket = {
    key: string;
    name: string;
    locations: Map<string, Location>;
};

export function useWorldGraphSearch(
    options: WorldGraphSearchOptions,
) {
    const query = ref("");
    const isSearching = ref(false);
    const message = ref<string | null>(null);
    const messageKind = ref<
        "success" | "error" | "neutral"
    >("neutral");
    const results = ref<readonly WorldGraphSearchResult[]>([]);
    const matchCount = ref<number | null>(null);

    /*
     * Only regions which were collapsed immediately before search expanded
     * them are owned here. Pre-existing user expansion is therefore never
     * undone when a query stops matching.
     */
    const searchExpandedRegions = new Map<string, Region>();
    const resolvedResultsByKey = new Map<
        string,
        ResolvedWorldGraphSearchResult
    >();

    let generation = 0;
    let debounceTimer:
        | ReturnType<typeof setTimeout>
        | null = null;
    let suppressedQueryWatchValue: string | null = null;

    function cancelDebounce(): void {
        if (debounceTimer === null) return;
        clearTimeout(debounceTimer);
        debounceTimer = null;
    }

    function clearMessage(): void {
        message.value = null;
        messageKind.value = "neutral";
    }

    function setQueryWithoutScheduling(value: string): void {
        if (query.value === value) return;
        suppressedQueryWatchValue = value;
        query.value = value;
    }

    function clearResolvedPresentation(
        nextMatchCount: number | null = null,
    ): void {
        resolvedResultsByKey.clear();
        results.value = [];
        matchCount.value = nextMatchCount;
    }

    function setResolvedPresentation(
        resolved: readonly ResolvedWorldGraphSearchResult[],
    ): void {
        resolvedResultsByKey.clear();
        for (const item of resolved) {
            resolvedResultsByKey.set(item.result.key, item);
        }
        results.value = resolved.map(item => item.result);
        matchCount.value = resolved.length;
    }

    function locationContext(location: Location): string {
        const regionId = location.get("region_id");
        if (regionId === null) return "Location · no containing region";

        const region = options.regionIndex.value.get(regionId);
        return region === undefined
            ? "Location · containing region unavailable"
            : `Location · ${region.get("name")}`;
    }

    function regionContext(region: Region): string {
        const parentId = region.get("parent_region_id");
        if (parentId === null) return "Region · root";

        const parent = options.regionIndex.value.get(parentId);
        return parent === undefined
            ? "Region · parent unavailable"
            : `Region · inside ${parent.get("name")}`;
    }

    function characterId(character: StartingCharacter): number {
        return (
            character as unknown as {
                get(field: "id"): number;
            }
        ).get("id");
    }

    function characterContext(
        locations: readonly Location[],
    ): string {
        if (locations.length === 1) {
            return `Character · starts at ${locations[0]?.get("name") ?? "unnamed location"}`;
        }

        const previewNames = locations
            .slice(0, 3)
            .map(location => location.get("name"));
        const omitted = locations.length - previewNames.length;
        return `Character · starts at ${locations.length} locations · ${previewNames.join(", ")}${omitted > 0 ? ` +${omitted}` : ""}`;
    }

    function compareResolvedResults(
        left: ResolvedWorldGraphSearchResult,
        right: ResolvedWorldGraphSearchResult,
    ): number {
        const byName = normalizeSearchText(left.result.name)
            .localeCompare(normalizeSearchText(right.result.name));
        return byName !== 0
            ? byName
            : left.result.key.localeCompare(right.result.key);
    }

    async function searchCharacterResults(
        term: string,
        expectedWorldId: number,
        expectedGeneration: number,
    ): Promise<SearchResolution | null> {
        let failedLookups = 0;
        const inspected = await mapWithConcurrency(
            options.graph.value.locations,
            CHARACTER_SEARCH_CONCURRENCY,
            async location => {
                try {
                    const characters = (
                        await location.getStartingHere()
                    ).filter(character => (
                        nameMatchesSearch(
                            character.get("name"),
                            term,
                        )
                    ));

                    return {location, characters};
                } catch (error) {
                    console.error(
                        "Unable to inspect a location's starting characters",
                        error,
                    );
                    failedLookups += 1;
                    return {
                        location,
                        characters: [] as StartingCharacter[],
                    };
                }
            },
        );

        if (
            generation !== expectedGeneration ||
            options.worldId.value !== expectedWorldId
        ) {
            return null;
        }

        const buckets = new Map<string, CharacterBucket>();

        for (const inspection of inspected) {
            for (const character of inspection.characters) {
                const key = `character:${characterId(character)}`;
                let bucket = buckets.get(key);

                if (bucket === undefined) {
                    bucket = {
                        key,
                        name: character.get("name"),
                        locations: new Map(),
                    };
                    buckets.set(key, bucket);
                }

                bucket.locations.set(
                    locationEntityKey(inspection.location),
                    inspection.location,
                );
            }
        }

        const resolved = [...buckets.values()].map(
            (bucket): ResolvedWorldGraphSearchResult => {
                const locations = [...bucket.locations.values()];
                return {
                    result: {
                        key: bucket.key,
                        kind: "character",
                        name: bucket.name,
                        context: characterContext(locations),
                    },
                    entities: locations.map(location => ({
                        kind: "location" as const,
                        location,
                    })),
                };
            },
        ).sort(compareResolvedResults);

        return {
            results: resolved,
            failedCharacterLookups: failedLookups,
        };
    }

    async function resolveResults(
        kind: WorldGraphSearchKind,
        term: string,
        expectedWorldId: number,
        expectedGeneration: number,
    ): Promise<SearchResolution | null> {
        if (kind === "location") {
            return {
                results: searchLocationsByName(
                    options.graph.value.locations,
                    term,
                ).map((location): ResolvedWorldGraphSearchResult => ({
                    result: {
                        key: locationEntityKey(location),
                        kind: "location",
                        name: location.get("name"),
                        context: locationContext(location),
                    },
                    entities: [{kind: "location", location}],
                })).sort(compareResolvedResults),
                failedCharacterLookups: 0,
            };
        }

        if (kind === "region") {
            return {
                results: searchRegionsByName(
                    options.graph.value.regions,
                    term,
                ).map((region): ResolvedWorldGraphSearchResult => ({
                    result: {
                        key: regionEntityKey(region),
                        kind: "region",
                        name: region.get("name"),
                        context: regionContext(region),
                    },
                    entities: [{kind: "region", region}],
                })).sort(compareResolvedResults),
                failedCharacterLookups: 0,
            };
        }

        return searchCharacterResults(
            term,
            expectedWorldId,
            expectedGeneration,
        );
    }

    function resultNoun(
        kind: WorldGraphSearchKind,
        count: number,
    ): string {
        if (kind === "region") {
            return count === 1 ? "region" : "regions";
        }
        return count === 1 ? "location" : "locations";
    }

    function resolvedEntities(
        resolved: readonly ResolvedWorldGraphSearchResult[],
    ): readonly SelectedGraphEntity[] {
        const unique = new Map<string, SelectedGraphEntity>();
        for (const item of resolved) {
            for (const entity of item.entities) {
                unique.set(selectedEntityKey(entity), entity);
            }
        }
        return [...unique.values()];
    }

    async function synchronizeSearchExpansions(
        requiredRegions: readonly Region[],
        expectedWorldId: number,
        expectedGeneration: number,
    ): Promise<number> {
        const desiredKeys = new Set(
            requiredRegions.map(regionEntityKey),
        );
        let failures = 0;

        const released = [...searchExpandedRegions.values()]
            .filter(region => (
                !desiredKeys.has(regionEntityKey(region))
            ))
            .sort((left, right) => (
                regionDepth(right, options.regionIndex.value) -
                regionDepth(left, options.regionIndex.value)
            ));

        for (const region of released) {
            if (
                generation !== expectedGeneration ||
                options.worldId.value !== expectedWorldId
            ) {
                return failures;
            }

            const key = regionEntityKey(region);
            if (region.get("collapsed")) {
                searchExpandedRegions.delete(key);
                continue;
            }

            const collapsed =
                await options.setRegionCollapsed(
                    region,
                    true,
                );

            if (collapsed) {
                searchExpandedRegions.delete(key);
            } else {
                failures += 1;
            }
        }

        const orderedRequired = [...requiredRegions].sort(
            (left, right) => (
                regionDepth(left, options.regionIndex.value) -
                regionDepth(right, options.regionIndex.value)
            ),
        );

        for (const region of orderedRequired) {
            if (
                generation !== expectedGeneration ||
                options.worldId.value !== expectedWorldId
            ) {
                return failures;
            }

            const key = regionEntityKey(region);
            if (!region.get("collapsed")) continue;

            const expanded =
                await options.setRegionCollapsed(
                    region,
                    false,
                );

            if (expanded) {
                searchExpandedRegions.set(key, region);
            } else {
                failures += 1;
            }
        }

        return failures;
    }

    async function releaseAllSearchExpansions(
        expectedWorldId: number,
        expectedGeneration: number,
    ): Promise<number> {
        return synchronizeSearchExpansions(
            [],
            expectedWorldId,
            expectedGeneration,
        );
    }

    async function applyResolvedResults(
        kind: WorldGraphSearchKind,
        resolved: readonly ResolvedWorldGraphSearchResult[],
        failedCharacterLookups: number,
        expectedWorldId: number,
        expectedGeneration: number,
    ): Promise<void> {
        const entities = resolvedEntities(resolved);
        const expansionRegions =
            requiredSearchExpansionRegions(
                entities,
                options.regionIndex.value,
            );

        const expansionFailures =
            await synchronizeSearchExpansions(
                expansionRegions,
                expectedWorldId,
                expectedGeneration,
            );

        if (
            generation !== expectedGeneration ||
            options.worldId.value !== expectedWorldId
        ) {
            return;
        }

        const visibleEntities = entities.filter(
            entity => entityVisibleAfterExpansion(
                entity,
                options.regionIndex.value,
            ),
        );

        /*
         * Multi-region search deliberately preserves the current viewport.
         * Single region/location results focus. Character results retain the
         * existing group-focus behavior because one character may start in
         * more than one location.
         */
        const shouldFocus = kind === "character"
            ? visibleEntities.length > 0
            : resolved.length === 1 &&
                visibleEntities.length === 1;

        if (shouldFocus) {
            options.focusEntities(visibleEntities);
            /*
             * Focusing a single tiny result may move it across the LOD
             * threshold. Selection happens afterwards so LOD remains the
             * authoritative interaction boundary instead of being bypassed.
             */
            await nextTick();
        }

        const selectedCount =
            options.replaceSelection(visibleEntities);
        const structurallyUnavailableCount =
            entities.length - visibleEntities.length;
        const lodCulledCount =
            visibleEntities.length - selectedCount;
        const qualifier = [
            structurallyUnavailableCount > 0
                ? `${structurallyUnavailableCount} remained hidden because its region could not be expanded`
                : null,
            lodCulledCount > 0
                ? `${lodCulledCount} remained unselected because viewport LOD culled them`
                : null,
            expansionFailures > 0
                ? `${expansionFailures} region expansion changes could not be saved`
                : null,
            failedCharacterLookups > 0
                ? `${failedCharacterLookups} locations could not be checked`
                : null,
        ].filter(
            (value): value is string => value !== null,
        );

        message.value = selectedCount === 0
            ? "Matches were found, but none could be made visible."
            : `Selected ${selectedCount} ${resultNoun(kind, selectedCount)}${qualifier.length > 0 ? `. ${qualifier.join("; ")}.` : "."}`;
        messageKind.value = selectedCount > 0
            ? "success"
            : "error";
    }

    async function submit(
        rawQuery = query.value,
    ): Promise<void> {
        cancelDebounce();

        const expectedGeneration = ++generation;
        const expectedWorldId = options.worldId.value;
        const parsed = parseWorldGraphSearch(rawQuery);

        isSearching.value = true;
        clearMessage();
        options.dismissPopovers();

        try {
            if (!parsed.ok) {
                clearResolvedPresentation();
                await releaseAllSearchExpansions(
                    expectedWorldId,
                    expectedGeneration,
                );
                options.replaceSelection([]);

                if (rawQuery.trim().length > 0) {
                    message.value = parsed.message;
                    messageKind.value = "error";
                }
                return;
            }

            const resolved = await resolveResults(
                parsed.query.kind,
                parsed.query.term,
                expectedWorldId,
                expectedGeneration,
            );

            if (resolved === null) return;
            if (
                generation !== expectedGeneration ||
                options.worldId.value !== expectedWorldId
            ) {
                return;
            }
            setResolvedPresentation(resolved.results);

            /* Location search remains selection-safe until one item is chosen. */
            if (
                parsed.query.kind === "location" &&
                resolved.results.length > 1
            ) {
                await releaseAllSearchExpansions(
                    expectedWorldId,
                    expectedGeneration,
                );
                options.replaceSelection([]);
                message.value =
                    `Found ${resolved.results.length} locations. Choose one result or refine the query.`;
                messageKind.value = "neutral";
                return;
            }

            if (resolved.results.length === 0) {
                await releaseAllSearchExpansions(
                    expectedWorldId,
                    expectedGeneration,
                );
                options.replaceSelection([]);
                message.value =
                    resolved.failedCharacterLookups > 0
                        ? "No matches found. Some locations could not be checked."
                        : "No matching graph entities were found.";
                messageKind.value = "neutral";
                return;
            }

            await applyResolvedResults(
                parsed.query.kind,
                resolved.results,
                resolved.failedCharacterLookups,
                expectedWorldId,
                expectedGeneration,
            );
        } finally {
            if (
                generation === expectedGeneration &&
                options.worldId.value === expectedWorldId
            ) {
                isSearching.value = false;
            }
        }
    }

    async function chooseResult(
        resultKey: string,
    ): Promise<void> {
        const chosen = resolvedResultsByKey.get(resultKey);
        if (chosen === undefined) return;

        cancelDebounce();
        const expectedGeneration = ++generation;
        const expectedWorldId = options.worldId.value;

        setQueryWithoutScheduling(
            formatWorldGraphSearchParameter(chosen.result),
        );
        setResolvedPresentation([chosen]);
        isSearching.value = true;
        clearMessage();
        options.dismissPopovers();

        try {
            await applyResolvedResults(
                chosen.result.kind,
                [chosen],
                0,
                expectedWorldId,
                expectedGeneration,
            );
        } finally {
            if (
                generation === expectedGeneration &&
                options.worldId.value === expectedWorldId
            ) {
                isSearching.value = false;
            }
        }
    }

    function scheduleSubmit(
        rawQuery: string,
    ): void {
        cancelDebounce();

        /* Invalidate in-flight work and stale suggestions immediately. */
        generation += 1;
        isSearching.value = false;
        clearResolvedPresentation();

        debounceTimer = setTimeout(() => {
            debounceTimer = null;
            void submit(rawQuery);
        }, WORLD_GRAPH_SEARCH_DEBOUNCE_MS);
    }

    async function clearQuery(): Promise<void> {
        cancelDebounce();
        setQueryWithoutScheduling("");
        clearResolvedPresentation();
        clearMessage();
        await submit("");
    }

    function resetForWorldChange(): void {
        cancelDebounce();
        generation += 1;
        setQueryWithoutScheduling("");
        isSearching.value = false;
        searchExpandedRegions.clear();
        clearResolvedPresentation();
        clearMessage();
    }

    watch(
        query,
        value => {
            if (suppressedQueryWatchValue === value) {
                suppressedQueryWatchValue = null;
                return;
            }

            suppressedQueryWatchValue = null;
            scheduleSubmit(value);
        },
    );

    onScopeDispose(() => {
        cancelDebounce();
        generation += 1;
        resolvedResultsByKey.clear();
    });

    return {
        query,
        isSearching,
        message,
        messageKind,
        results,
        matchCount,
        submit,
        chooseResult,
        clearQuery,
        clearMessage,
        resetForWorldChange,
    };
}
