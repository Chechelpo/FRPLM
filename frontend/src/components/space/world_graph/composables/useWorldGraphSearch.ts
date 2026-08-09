import {
    nextTick,
    onBeforeUnmount,
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
    regionDepth,
    regionEntityKey,
    type RegionIndex,
} from "../utils/geometry";
import {
    entityVisibleAfterExpansion,
    mapWithConcurrency,
    nameMatchesSearch,
    parseWorldGraphSearch,
    requiredSearchExpansionRegions,
    searchLocationsByName,
    searchRegionsByName,
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

export type CharacterLocationSearchResult = {
    locations: readonly Location[];
    failedLookups: number;
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

    /*
     * Only regions which were collapsed immediately before search expanded
     * them are owned here. Pre-existing user expansion is therefore never
     * undone when a query stops matching.
     */
    const searchExpandedRegions = new Map<string, Region>();

    let generation = 0;
    let debounceTimer:
        | ReturnType<typeof setTimeout>
        | null = null;

    function cancelDebounce(): void {
        if (debounceTimer === null) return;
        clearTimeout(debounceTimer);
        debounceTimer = null;
    }

    function clearMessage(): void {
        message.value = null;
        messageKind.value = "neutral";
    }

    function clearQuery(): void {
        query.value = "";
        clearMessage();
    }

    async function searchCharacterLocations(
        term: string,
        expectedWorldId: number,
        expectedGeneration: number,
    ): Promise<CharacterLocationSearchResult | null> {
        let failedLookups = 0;
        const results = await mapWithConcurrency(
            options.graph.value.locations,
            CHARACTER_SEARCH_CONCURRENCY,
            async location => {
                try {
                    const characters =
                        await location.getStartingHere();

                    const matches = characters.some(
                        character => nameMatchesSearch(
                            character.get("name"),
                            term,
                        ),
                    );

                    return matches ? location : null;
                } catch (error) {
                    console.error(
                        "Unable to inspect a location's starting characters",
                        error,
                    );
                    failedLookups += 1;
                    return null;
                }
            },
        );

        if (
            generation !== expectedGeneration ||
            options.worldId.value !== expectedWorldId
        ) {
            return null;
        }

        return {
            locations: results.filter(
                (location): location is Location =>
                    location !== null,
            ),
            failedLookups,
        };
    }

    async function resolveEntities(
        kind: "character" | "location" | "region",
        term: string,
        expectedWorldId: number,
        expectedGeneration: number,
    ): Promise<{
        entities: readonly SelectedGraphEntity[];
        failedCharacterLookups: number;
    } | null> {
        if (kind === "location") {
            return {
                entities: searchLocationsByName(
                    options.graph.value.locations,
                    term,
                ).map(location => ({
                    kind: "location" as const,
                    location,
                })),
                failedCharacterLookups: 0,
            };
        }

        if (kind === "region") {
            return {
                entities: searchRegionsByName(
                    options.graph.value.regions,
                    term,
                ).map(region => ({
                    kind: "region" as const,
                    region,
                })),
                failedCharacterLookups: 0,
            };
        }

        const characterResult =
            await searchCharacterLocations(
                term,
                expectedWorldId,
                expectedGeneration,
            );

        if (characterResult === null) return null;

        return {
            entities: characterResult.locations.map(
                location => ({
                    kind: "location" as const,
                    location,
                }),
            ),
            failedCharacterLookups:
                characterResult.failedLookups,
        };
    }

    function resultNoun(
        kind: "character" | "location" | "region",
        count: number,
    ): string {
        if (kind === "region") {
            return count === 1 ? "region" : "regions";
        }
        return count === 1 ? "location" : "locations";
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

            const resolved = await resolveEntities(
                parsed.query.kind,
                parsed.query.term,
                expectedWorldId,
                expectedGeneration,
            );

            if (resolved === null) return;

            /* Location search intentionally refuses ambiguous multi-match. */
            if (
                parsed.query.kind === "location" &&
                resolved.entities.length > 1
            ) {
                await releaseAllSearchExpansions(
                    expectedWorldId,
                    expectedGeneration,
                );
                options.replaceSelection([]);
                message.value =
                    `Found ${resolved.entities.length} locations. Refine the query until exactly one location matches.`;
                messageKind.value = "neutral";
                return;
            }

            if (resolved.entities.length === 0) {
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

            const expansionRegions =
                requiredSearchExpansionRegions(
                    resolved.entities,
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

            const visibleEntities = resolved.entities.filter(
                entity => entityVisibleAfterExpansion(
                    entity,
                    options.regionIndex.value,
                ),
            );

            /*
             * Multi-region search deliberately preserves the current
             * viewport. Single-region and single-location search focus the
             * sole target; character search retains v6's group-focus feature.
             */
            const shouldFocus =
                parsed.query.kind === "character" ||
                (
                    parsed.query.kind === "region"
                        ? resolved.entities.length === 1 &&
                            visibleEntities.length === 1
                        : visibleEntities.length === 1
                );

            if (shouldFocus) {
                options.focusEntities(visibleEntities);
                /*
                 * Focusing a single tiny result may move it across the LOD
                 * threshold. Selection happens afterwards so LOD remains the
                 * authoritative interaction boundary instead of being
                 * bypassed for search results.
                 */
                await nextTick();
            }

            const selectedCount =
                options.replaceSelection(visibleEntities);
            const structurallyUnavailableCount =
                resolved.entities.length - visibleEntities.length;
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
                resolved.failedCharacterLookups > 0
                    ? `${resolved.failedCharacterLookups} locations could not be checked`
                    : null,
            ].filter(
                (value): value is string => value !== null,
            );

            message.value = selectedCount === 0
                ? "Matches were found, but none could be made visible."
                : `Selected ${selectedCount} ${resultNoun(parsed.query.kind, selectedCount)}${qualifier.length > 0 ? `. ${qualifier.join("; ")}.` : "."}`;
            messageKind.value = selectedCount > 0
                ? "success"
                : "error";
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

        /* Invalidate any in-flight character lookup immediately. */
        generation += 1;
        isSearching.value = false;

        debounceTimer = setTimeout(() => {
            debounceTimer = null;
            void submit(rawQuery);
        }, WORLD_GRAPH_SEARCH_DEBOUNCE_MS);
    }

    function resetForWorldChange(): void {
        cancelDebounce();
        generation += 1;
        query.value = "";
        isSearching.value = false;
        searchExpandedRegions.clear();
        clearMessage();
    }

    watch(
        query,
        value => scheduleSubmit(value),
    );

    onBeforeUnmount(() => {
        cancelDebounce();
        generation += 1;
    });

    return {
        query,
        isSearching,
        message,
        messageKind,
        submit,
        clearQuery,
        clearMessage,
        resetForWorldChange,
    };
}
