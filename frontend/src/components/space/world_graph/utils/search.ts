import type {
    Location,
    Region,
    RegionGeometry,
} from "@/domain/World";
import type {
    SelectedGraphEntity,
} from "../types";
import {
    boundsToGeometry,
    geometryToBounds,
    locationEntityKey,
    regionDepth,
    regionEntityKey,
    type RegionIndex,
} from "./geometry";

export type WorldGraphSearchKind =
    | "character"
    | "location"
    | "region";

export type WorldGraphSearchQuery = {
    kind: WorldGraphSearchKind;
    term: string;
};

export type WorldGraphSearchParseResult =
    | {ok: true; query: WorldGraphSearchQuery}
    | {ok: false; message: string};

/** Presentation-safe search item consumed by the bottom-left combobox. */
export type WorldGraphSearchResult = Readonly<{
    key: string;
    kind: WorldGraphSearchKind;
    name: string;
    context: string;
}>;

const SEARCH_PATTERN = /^\s*(character|location|region)\s*:\s*(.*?)\s*$/i;

export function normalizeSearchText(value: string): string {
    return value
        .toLocaleLowerCase()
        .replace(/\s+/g, "");
}

export function parseWorldGraphSearch(
    input: string,
): WorldGraphSearchParseResult {
    const match = SEARCH_PATTERN.exec(input);

    if (match === null) {
        return {
            ok: false,
            message: "Use character:name, location:name, or region:name.",
        };
    }

    const rawKind = match[1];
    const rawTerm = match[2];

    if (rawKind === undefined || rawTerm === undefined) {
        return {
            ok: false,
            message: "Use character:name, location:name, or region:name.",
        };
    }

    const term = rawTerm.trim();

    if (term.length === 0) {
        return {
            ok: false,
            message: "Enter a name after the search prefix.",
        };
    }

    return {
        ok: true,
        query: {
            kind: rawKind.toLocaleLowerCase() as WorldGraphSearchKind,
            term,
        },
    };
}

export function formatWorldGraphSearchParameter(
    result: Pick<WorldGraphSearchResult, "kind" | "name">,
): string {
    return `${result.kind}:${result.name}`;
}

/**
 * Circular keyboard traversal for the result list. Keeping this arithmetic
 * outside the component makes the ArrowUp/ArrowDown contract deterministic.
 */
export function nextWorldGraphSearchResultIndex(
    currentIndex: number,
    resultCount: number,
    direction: -1 | 1,
): number {
    if (!Number.isInteger(resultCount) || resultCount <= 0) return -1;

    const normalizedCurrent = Number.isInteger(currentIndex) &&
        currentIndex >= 0 &&
        currentIndex < resultCount
        ? currentIndex
        : direction > 0
            ? -1
            : 0;

    return (
        normalizedCurrent + direction + resultCount
    ) % resultCount;
}

export function nameMatchesSearch(
    name: string,
    term: string,
): boolean {
    const normalizedTerm = normalizeSearchText(term);

    return normalizedTerm.length > 0 &&
        normalizeSearchText(name).includes(normalizedTerm);
}

export function searchLocationsByName(
    locations: readonly Location[],
    term: string,
): readonly Location[] {
    return locations.filter(
        location => nameMatchesSearch(location.get("name"), term),
    );
}

export function searchRegionsByName(
    regions: readonly Region[],
    term: string,
): readonly Region[] {
    return regions.filter(
        region => nameMatchesSearch(region.get("name"), term),
    );
}

export function regionAncestors(
    region: Region,
    index: RegionIndex,
    includeRegion: boolean,
): readonly Region[] {
    const result: Region[] = [];
    const visited = new Set<number>();
    const initialParentId = region.get("parent_region_id");
    let current: Region | null = includeRegion
        ? region
        : initialParentId === null
            ? null
            : index.get(initialParentId) ?? null;

    while (current !== null) {
        const id = current.get("id");

        if (visited.has(id)) {
            throw new Error("Cycle detected while resolving search visibility");
        }

        visited.add(id);
        result.push(current);

        const parentId = current.get("parent_region_id");
        current = parentId === null
            ? null
            : index.get(parentId) ?? null;
    }

    return result.reverse();
}

/**
 * Returns every region that must be expanded before the supplied search
 * entities can be rendered. Location targets include their containing region;
 * region targets include themselves because search results must be opened,
 * plus every ancestor required to reveal them.
 */
export function requiredSearchExpansionRegions(
    entities: readonly SelectedGraphEntity[],
    index: RegionIndex,
): readonly Region[] {
    const byKey = new Map<string, Region>();

    for (const entity of entities) {
        if (entity.kind === "location") {
            const regionId = entity.location.get("region_id");
            if (regionId === null) continue;
            const region = index.get(regionId);
            if (region === undefined) continue;

            for (const ancestor of regionAncestors(region, index, true)) {
                byKey.set(regionEntityKey(ancestor), ancestor);
            }
            continue;
        }

        for (const ancestor of regionAncestors(entity.region, index, true)) {
            byKey.set(regionEntityKey(ancestor), ancestor);
        }
    }

    return [...byKey.values()].sort(
        (left, right) => regionDepth(left, index) - regionDepth(right, index),
    );
}

export function entityVisibleAfterExpansion(
    entity: SelectedGraphEntity,
    index: RegionIndex,
): boolean {
    if (entity.kind === "location") {
        const regionId = entity.location.get("region_id");
        if (regionId === null) return false;
        const region = index.get(regionId);
        if (region === undefined || region.get("collapsed")) return false;

        return regionAncestors(region, index, false).every(
            ancestor => !ancestor.get("collapsed"),
        );
    }

    return regionAncestors(entity.region, index, false).every(
        ancestor => !ancestor.get("collapsed"),
    );
}

export async function mapWithConcurrency<T, R>(
    values: readonly T[],
    concurrency: number,
    mapper: (value: T, index: number) => Promise<R>,
): Promise<readonly R[]> {
    if (!Number.isInteger(concurrency) || concurrency <= 0) {
        throw new Error("Search concurrency must be a positive integer");
    }

    const result = new Array<R>(values.length);
    let nextIndex = 0;

    async function worker(): Promise<void> {
        while (true) {
            const index = nextIndex;
            nextIndex += 1;

            if (index >= values.length) return;

            const value = values[index];
            if (value === undefined) continue;
            result[index] = await mapper(value, index);
        }
    }

    await Promise.all(
        Array.from(
            {length: Math.min(concurrency, Math.max(1, values.length))},
            () => worker(),
        ),
    );

    return result;
}

export type SearchFocusGeometryOptions = {
    locationGeometry: (location: Location) => RegionGeometry;
    regionGeometry: (region: Region) => RegionGeometry;
    minimumExtent?: number;
};

export function searchFocusGeometry(
    entities: readonly SelectedGraphEntity[],
    options: SearchFocusGeometryOptions,
): RegionGeometry | null {
    if (entities.length === 0) return null;

    let left = Number.POSITIVE_INFINITY;
    let right = Number.NEGATIVE_INFINITY;
    let top = Number.POSITIVE_INFINITY;
    let bottom = Number.NEGATIVE_INFINITY;

    for (const entity of entities) {
        const geometry = entity.kind === "location"
            ? options.locationGeometry(entity.location)
            : options.regionGeometry(entity.region);
        const bounds = geometryToBounds(geometry);
        left = Math.min(left, bounds.left);
        right = Math.max(right, bounds.right);
        top = Math.min(top, bounds.top);
        bottom = Math.max(bottom, bounds.bottom);
    }

    if (![left, right, top, bottom].every(Number.isFinite)) return null;

    const minimumExtent = options.minimumExtent ?? 180;
    const centerX = (left + right) / 2;
    const centerY = (top + bottom) / 2;
    const width = Math.max(minimumExtent, right - left);
    const height = Math.max(minimumExtent, bottom - top);

    return boundsToGeometry({
        left: centerX - width / 2,
        right: centerX + width / 2,
        top: centerY - height / 2,
        bottom: centerY + height / 2,
    });
}

export function selectedEntityKey(entity: SelectedGraphEntity): string {
    return entity.kind === "location"
        ? locationEntityKey(entity.location)
        : regionEntityKey(entity.region);
}
