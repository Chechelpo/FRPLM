import type {
    Location,
    LocationEdge,
    Position,
    Region,
    RegionGeometry,
    World,
} from "@frplm/host-sdk";
import {
    REGION_CONTENT_PADDING,
    WORLD_CONTENT_PADDING,
} from "../constants.js";
import type {
    LocationPreviewMap,
    LocationRadiusState,
    RegionPreviewMap,
    SelectedGraphEntity,
    WorldCanvasGeometry,
    WorldGraphData,
} from "../types.js";
import {
    readPersistedLocationRadius,
    resolveEffectiveLocationRadius,
} from "./locationRadius.js";
import {
    buildRegionIndex,
    circleInsideGeometry,
    circleIntersectsGeometry,
    edgeEntityKey,
    geometryInsideGeometry,
    geometryInsideWorld,
    getAbsoluteLocationPosition,
    getAbsoluteRegionGeometry,
    isRegionDescendantOf,
    locationEntityKey,
    regionDepth,
    regionEntityKey,
    type RegionIndex,
} from "./geometry.js";
import {zoomAwareCollapsedRegionSize} from "./sizing.js";

export type RegionsByParent = ReadonlyMap<number | null, readonly Region[]>;
export type LocationsByRegion = ReadonlyMap<number | null, readonly Location[]>;
export type LocationDegreeIndex = ReadonlyMap<string, number>;

export async function loadWorldGraph(world: World): Promise<WorldGraphData> {
    const [locations, regions, edges] = await Promise.all([
        world.getLocations(),
        world.getAllRegions(),
        world.getAllEdges(),
    ]);
    return {locations, regions, edges};
}

export function buildRegionsByParent(regions: readonly Region[]): RegionsByParent {
    const result = new Map<number | null, Region[]>();
    for (const region of regions) {
        const parentId = region.get("parent_region_id");
        const siblings = result.get(parentId) ?? [];
        siblings.push(region);
        result.set(parentId, siblings);
    }
    return result;
}

export function buildLocationsByRegion(locations: readonly Location[]): LocationsByRegion {
    const result = new Map<number | null, Location[]>();
    for (const location of locations) {
        const regionId = location.get("region_id");
        const siblings = result.get(regionId) ?? [];
        siblings.push(location);
        result.set(regionId, siblings);
    }
    return result;
}

export function buildLocationDegreeIndex(edges: readonly LocationEdge[]): LocationDegreeIndex {
    const result = new Map<string, number>();
    for (const edge of edges) {
        const worldId = edge.get("world_id");
        const source = `location:${worldId}:${edge.get("from_id")}`;
        const destination = `location:${worldId}:${edge.get("to_id")}`;
        result.set(source, (result.get(source) ?? 0) + 1);
        result.set(destination, (result.get(destination) ?? 0) + 1);
    }
    return result;
}

export function locationRadius(
    location: Location,
    index: LocationDegreeIndex,
    state?: LocationRadiusState,
): number {
    const degree = index.get(locationEntityKey(location)) ?? 0;
    return resolveEffectiveLocationRadius(location, degree, state);
}

export function getDescendantRegions(
    region: Region,
    byParent: RegionsByParent,
): readonly Region[] {
    const result: Region[] = [];
    const queue = [...(byParent.get(region.get("id")) ?? [])];
    const visited = new Set<number>([region.get("id")]);
    while (queue.length > 0) {
        const current = queue.shift();
        if (current === undefined) break;
        const id = current.get("id");
        if (visited.has(id)) throw new Error("Cycle detected in region hierarchy");
        visited.add(id);
        result.push(current);
        queue.push(...(byParent.get(id) ?? []));
    }
    return result;
}

export function getRegionSubtree(
    region: Region,
    byParent: RegionsByParent,
): readonly Region[] {
    return [region, ...getDescendantRegions(region, byParent)];
}

/**
 * Collapse is recursive; expansion is intentionally local to the requested
 * region so descendants keep the collapsed state they already have.
 */
export function regionCollapseTargets(
    region: Region,
    collapsed: boolean,
    byParent: RegionsByParent,
): readonly Region[] {
    return collapsed
        ? getRegionSubtree(region, byParent)
        : [region];
}

/**
 * A region lock protects only that region from a direct collapse/expand
 * request. Ancestor collapse remains recursive and deliberately ignores
 * descendant locks because that changes the descendant only absolutely as
 * part of the parent's state transition.
 */
export function regionOwnCollapseChangeBlockedByLock(
    region: Region,
    collapsed: boolean,
    isLocked: (region: Region) => boolean,
): boolean {
    return region.get("collapsed") !== collapsed && isLocked(region);
}

export function sortRegionsForRender(
    regions: readonly Region[],
    index: RegionIndex = buildRegionIndex(regions),
): readonly Region[] {
    return regions
        .map((region, order) => ({region, order, depth: regionDepth(region, index)}))
        .sort((a, b) => a.depth - b.depth || a.order - b.order)
        .map(({region}) => region);
}

export function sortRegionsDeepestFirst(
    regions: readonly Region[],
    index: RegionIndex = buildRegionIndex(regions),
): readonly Region[] {
    return regions
        .map((region, order) => ({region, order, depth: regionDepth(region, index)}))
        .sort((a, b) => b.depth - a.depth || b.order - a.order)
        .map(({region}) => region);
}

export function getVisibleRegionKeys(
    regionsInRenderOrder: readonly Region[],
    index: RegionIndex,
): ReadonlySet<string> {
    const visible = new Set<string>();
    for (const region of regionsInRenderOrder) {
        let parentId = region.get("parent_region_id");
        let hidden = false;
        const visited = new Set<number>();
        while (parentId !== null) {
            if (visited.has(parentId)) throw new Error("Cycle detected in region hierarchy");
            visited.add(parentId);
            const parent = index.get(parentId);
            if (parent === undefined) throw new Error("Region references a missing parent");
            if (parent.get("collapsed")) {
                hidden = true;
                break;
            }
            parentId = parent.get("parent_region_id");
        }
        if (!hidden) visible.add(regionEntityKey(region));
    }
    return visible;
}

export function getVisibleLocationKeys(
    locations: readonly Location[],
    index: RegionIndex,
    visibleRegionKeys: ReadonlySet<string>,
): ReadonlySet<string> {
    const visible = new Set<string>();
    for (const location of locations) {
        const regionId = location.get("region_id");
        if (regionId === null) continue;
        const region = index.get(regionId);
        if (region === undefined) continue;
        if (!visibleRegionKeys.has(regionEntityKey(region))) continue;
        if (region.get("collapsed")) continue;
        visible.add(locationEntityKey(location));
    }
    return visible;
}

export function collapsedDisplayGeometry(
    region: Region,
    index: RegionIndex,
    previews: RegionPreviewMap = new Map(),
    zoom = 1,
    screenWidth?: number,
    screenHeight?: number,
): RegionGeometry {
    const expanded = getAbsoluteRegionGeometry(region, index, previews);
    const display = zoomAwareCollapsedRegionSize(
        expanded,
        zoom,
        screenWidth,
        screenHeight,
    );
    return {
        x: expanded.x,
        y: expanded.y,
        width: display.width,
        height: display.height,
    };
}

export function validateGraph(graph: WorldGraphData, worldId: number): void {
    const index = buildRegionIndex(graph.regions);
    const edgeKeys = new Set<string>();
    const locationIds = new Set<number>();

    for (const region of graph.regions) {
        if (region.get("world_id") !== worldId) throw new Error("Region belongs to another world");
        getAbsoluteRegionGeometry(region, index);
    }

    for (const location of graph.locations) {
        if (location.get("worldID") !== worldId) throw new Error("Location belongs to another world");
        if (locationIds.has(location.get("id"))) throw new Error("Duplicate location ID");
        locationIds.add(location.get("id"));
        readPersistedLocationRadius(location);
        const regionId = location.get("region_id");
        if (regionId !== null && !index.has(regionId)) throw new Error("Location references a missing region");
        getAbsoluteLocationPosition(location, index);
    }

    for (const edge of graph.edges) {
        if (edge.get("world_id") !== worldId) throw new Error("Edge belongs to another world");
        if (!locationIds.has(edge.get("from_id")) || !locationIds.has(edge.get("to_id"))) {
            throw new Error("Edge references a missing location");
        }
        const key = edgeEntityKey(edge);
        if (edgeKeys.has(key)) throw new Error("Duplicate directed edge");
        edgeKeys.add(key);
    }
}

export function getEffectiveMovingRegions(
    selected: readonly Region[],
    index: RegionIndex,
): readonly Region[] {
    return selected.filter((region) => !selected.some((other) => (
        !other.equals(region) && isRegionDescendantOf(region, other, index)
    )));
}

export function locationIsInRegionSubtree(
    location: Location,
    region: Region,
    index: RegionIndex,
): boolean {
    const regionId = location.get("region_id");
    if (regionId === null) return false;
    const containing = index.get(regionId);
    if (containing === undefined) return false;
    return containing.equals(region) || isRegionDescendantOf(containing, region, index);
}

export function getEffectiveMovingLocations(
    selected: readonly Location[],
    effectiveRegions: readonly Region[],
    index: RegionIndex,
): readonly Location[] {
    return selected.filter((location) => !effectiveRegions.some((region) => (
        locationIsInRegionSubtree(location, region, index)
    )));
}

export function entityKey(entity: SelectedGraphEntity): string {
    return entity.kind === "location"
        ? locationEntityKey(entity.location)
        : regionEntityKey(entity.region);
}

export function resolveLocationParent(
    location: Location,
    absoluteCenter: Position,
    regionsInRenderOrder: readonly Region[],
    index: RegionIndex,
    degreeIndex: LocationDegreeIndex,
    regionPreviews: RegionPreviewMap,
    unavailable: ReadonlySet<string>,
    radiusState?: LocationRadiusState,
    padding = REGION_CONTENT_PADDING,
): Region | null {
    let selected: Region | null = null;
    let selectedDepth = -1;
    let selectedRenderOrder = -1;
    const radius = locationRadius(location, degreeIndex, radiusState);

    for (let order = 0; order < regionsInRenderOrder.length; order += 1) {
        const region = regionsInRenderOrder[order];
        if (region === undefined) continue;
        if (region.get("world_id") !== location.get("worldID")) continue;
        if (unavailable.has(regionEntityKey(region))) continue;
        const geometry = getAbsoluteRegionGeometry(region, index, regionPreviews);
        if (!circleInsideGeometry(absoluteCenter, radius, geometry, padding)) continue;
        const depth = regionDepth(region, index);
        if (depth > selectedDepth || (depth === selectedDepth && order > selectedRenderOrder)) {
            selected = region;
            selectedDepth = depth;
            selectedRenderOrder = order;
        }
    }
    return selected;
}

export function resolveRegionParent(
    region: Region,
    absoluteGeometry: RegionGeometry,
    regionsInRenderOrder: readonly Region[],
    index: RegionIndex,
    regionPreviews: RegionPreviewMap,
    unavailable: ReadonlySet<string>,
    padding = REGION_CONTENT_PADDING,
): Region | null {
    let selected: Region | null = null;
    let selectedDepth = -1;
    let selectedRenderOrder = -1;

    for (let order = 0; order < regionsInRenderOrder.length; order += 1) {
        const candidate = regionsInRenderOrder[order];
        if (candidate === undefined) continue;
        const key = regionEntityKey(candidate);
        if (candidate.get("world_id") !== region.get("world_id")) continue;
        if (unavailable.has(key) || candidate.equals(region)) continue;
        if (isRegionDescendantOf(candidate, region, index)) continue;
        const candidateGeometry = getAbsoluteRegionGeometry(candidate, index, regionPreviews);
        if (!geometryInsideGeometry(absoluteGeometry, candidateGeometry, padding)) continue;
        const depth = regionDepth(candidate, index);
        if (depth > selectedDepth || (depth === selectedDepth && order > selectedRenderOrder)) {
            selected = candidate;
            selectedDepth = depth;
            selectedRenderOrder = order;
        }
    }
    return selected;
}

export function getMovingSubtreeKeys(
    effectiveRegions: readonly Region[],
    byParent: RegionsByParent,
): ReadonlySet<string> {
    const result = new Set<string>();
    for (const region of effectiveRegions) {
        for (const member of getRegionSubtree(region, byParent)) {
            result.add(regionEntityKey(member));
        }
    }
    return result;
}

export function makeMovementPreviews(
    effectiveRegions: readonly Region[],
    effectiveLocations: readonly Location[],
    allRegions: readonly Region[],
    allLocations: readonly Location[],
    index: RegionIndex,
    byParent: RegionsByParent,
    delta: Position,
    baseRegionPreviews: RegionPreviewMap = new Map(),
    baseLocationPreviews: LocationPreviewMap = new Map(),
): {regions: RegionPreviewMap; locations: LocationPreviewMap} {
    const movedRegionKeys = getMovingSubtreeKeys(effectiveRegions, byParent);
    const regionPreviews = new Map<string, RegionGeometry>();
    const locationPreviews = new Map<string, Position>();

    for (const region of allRegions) {
        if (!movedRegionKeys.has(regionEntityKey(region))) continue;
        const current = getAbsoluteRegionGeometry(region, index, baseRegionPreviews);
        regionPreviews.set(regionEntityKey(region), {
            ...current,
            x: current.x + delta.x,
            y: current.y + delta.y,
        });
    }

    for (const location of allLocations) {
        const followsRegion = effectiveRegions.some((region) => locationIsInRegionSubtree(location, region, index));
        const movesIndependently = effectiveLocations.some((candidate) => candidate.equals(location));
        if (!followsRegion && !movesIndependently) continue;
        const current = getAbsoluteLocationPosition(location, index, baseRegionPreviews, baseLocationPreviews);
        locationPreviews.set(locationEntityKey(location), {
            x: current.x + delta.x,
            y: current.y + delta.y,
        });
    }

    return {regions: regionPreviews, locations: locationPreviews};
}

export function marqueeEntityKeys(
    marquee: RegionGeometry,
    visibleRegions: readonly Region[],
    visibleLocations: readonly Location[],
    index: RegionIndex,
    degreeIndex: LocationDegreeIndex,
    regionPreviews: RegionPreviewMap = new Map(),
    locationPreviews: LocationPreviewMap = new Map(),
    radiusState?: LocationRadiusState,
    zoom = 1,
    collapsedScreenWidth?: number,
    collapsedScreenHeight?: number,
): ReadonlySet<string> {
    const result = new Set<string>();

    for (const location of visibleLocations) {
        const center = getAbsoluteLocationPosition(location, index, regionPreviews, locationPreviews);
        if (circleIntersectsGeometry(center, locationRadius(location, degreeIndex, radiusState), marquee)) {
            result.add(locationEntityKey(location));
        }
    }

    for (const region of visibleRegions) {
        const display = region.get("collapsed")
            ? collapsedDisplayGeometry(
                region,
                index,
                regionPreviews,
                zoom,
                collapsedScreenWidth,
                collapsedScreenHeight,
            )
            : getAbsoluteRegionGeometry(region, index, regionPreviews);
        if (geometryInsideGeometry(display, marquee, 0)) result.add(regionEntityKey(region));
    }

    return result;
}

export function validateCompletePlacement(
    effectiveRegions: readonly Region[],
    effectiveLocations: readonly Location[],
    regionParents: ReadonlyMap<string, Region | null>,
    locationParents: ReadonlyMap<string, Region | null>,
    regionPreviews: RegionPreviewMap,
    locationPreviews: LocationPreviewMap,
    index: RegionIndex,
    canvas: WorldCanvasGeometry,
    degreeIndex: LocationDegreeIndex,
    radiusState?: LocationRadiusState,
    regionPadding = REGION_CONTENT_PADDING,
    worldPadding = WORLD_CONTENT_PADDING,
): boolean {
    for (const region of effectiveRegions) {
        const geometry = regionPreviews.get(regionEntityKey(region));
        if (geometry === undefined) return false;
        const parent = regionParents.get(regionEntityKey(region));
        if (parent === undefined) return false;
        if (parent === null) {
            if (!geometryInsideWorld(geometry, canvas, worldPadding)) return false;
        } else {
            const parentGeometry = getAbsoluteRegionGeometry(parent, index, regionPreviews);
            if (!geometryInsideGeometry(geometry, parentGeometry, regionPadding)) return false;
        }
    }

    for (const location of effectiveLocations) {
        const center = locationPreviews.get(locationEntityKey(location));
        const parent = locationParents.get(locationEntityKey(location));
        if (center === undefined || parent == null) return false;
        const parentGeometry = getAbsoluteRegionGeometry(parent, index, regionPreviews);
        if (!circleInsideGeometry(
            center,
            locationRadius(location, degreeIndex, radiusState),
            parentGeometry,
            regionPadding,
        )) return false;
    }
    return true;
}
