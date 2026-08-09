import type {
    Location,
    Position,
    Region,
    RegionGeometry,
    World,
} from "@/domain/World";
import {
    MIN_WORLD_HEIGHT,
    MIN_WORLD_WIDTH,
    REGION_CONTENT_PADDING,
    WORLD_CONTENT_PADDING,
} from "../constants";
import type {
    CanvasScalePlan,
    LocationPreviewMap,
    LocationRadiusState,
    RegionPreviewMap,
    WorldCanvasGeometry,
} from "../types";
import {
    affineTransformGeometry,
    affineTransformPosition,
    geometryInsideGeometry,
    geometryInsideWorld,
    getAbsoluteLocationPosition,
    getAbsoluteRegionGeometry,
    isFinitePositiveGeometry,
    locationEntityKey,
    regionEntityKey,
    worldCanvasAsGeometry,
    type RegionIndex,
} from "./geometry";
import {
    circleInsideGeometry,
} from "./geometry";
import {
    getRegionSubtree,
    locationIsInRegionSubtree,
    locationRadius,
    type LocationDegreeIndex,
    type RegionsByParent,
} from "./graph";

export type ScalePreview = {
    regions: RegionPreviewMap;
    locations: LocationPreviewMap;
};

export function buildRegionScalePreview(
    target: Region,
    nextTargetGeometry: RegionGeometry,
    allLocations: readonly Location[],
    index: RegionIndex,
    byParent: RegionsByParent,
    baseRegionPreviews: RegionPreviewMap = new Map(),
    baseLocationPreviews: LocationPreviewMap = new Map(),
): ScalePreview {
    const oldTarget = getAbsoluteRegionGeometry(target, index, baseRegionPreviews);
    const regionPreviews = new Map<string, RegionGeometry>();
    const locationPreviews = new Map<string, Position>();
    const subtree = getRegionSubtree(target, byParent);

    for (const region of subtree) {
        const current = getAbsoluteRegionGeometry(region, index, baseRegionPreviews);
        regionPreviews.set(
            regionEntityKey(region),
            region.equals(target)
                ? nextTargetGeometry
                : affineTransformGeometry(current, oldTarget, nextTargetGeometry),
        );
    }

    for (const location of allLocations) {
        if (!locationIsInRegionSubtree(location, target, index)) continue;
        const current = getAbsoluteLocationPosition(location, index, baseRegionPreviews, baseLocationPreviews);
        locationPreviews.set(
            locationEntityKey(location),
            affineTransformPosition(current, oldTarget, nextTargetGeometry),
        );
    }

    return {regions: regionPreviews, locations: locationPreviews};
}

export function buildWorldScalePreview(
    oldCanvas: WorldCanvasGeometry,
    nextCanvas: WorldCanvasGeometry,
    regions: readonly Region[],
    locations: readonly Location[],
    index: RegionIndex,
    baseRegionPreviews: RegionPreviewMap = new Map(),
    baseLocationPreviews: LocationPreviewMap = new Map(),
): ScalePreview {
    const oldGeometry = worldCanvasAsGeometry(oldCanvas);
    const nextGeometry = worldCanvasAsGeometry(nextCanvas);
    const regionPreviews = new Map<string, RegionGeometry>();
    const locationPreviews = new Map<string, Position>();

    for (const region of regions) {
        regionPreviews.set(
            regionEntityKey(region),
            affineTransformGeometry(getAbsoluteRegionGeometry(region, index, baseRegionPreviews), oldGeometry, nextGeometry),
        );
    }

    for (const location of locations) {
        locationPreviews.set(
            locationEntityKey(location),
            affineTransformPosition(getAbsoluteLocationPosition(location, index, baseRegionPreviews, baseLocationPreviews), oldGeometry, nextGeometry),
        );
    }

    return {regions: regionPreviews, locations: locationPreviews};
}

export function validateScalePreview(
    regions: readonly Region[],
    locations: readonly Location[],
    index: RegionIndex,
    degreeIndex: LocationDegreeIndex,
    canvas: WorldCanvasGeometry,
    preview: ScalePreview,
    radiusState?: LocationRadiusState,
    regionPadding = REGION_CONTENT_PADDING,
    worldPadding = WORLD_CONTENT_PADDING,
): boolean {
    if (!Number.isFinite(canvas.width) || !Number.isFinite(canvas.height)) return false;
    if (canvas.width < MIN_WORLD_WIDTH || canvas.height < MIN_WORLD_HEIGHT) return false;

    for (const region of regions) {
        const geometry = getAbsoluteRegionGeometry(region, index, preview.regions);
        if (!isFinitePositiveGeometry(geometry)) return false;

        const parentId = region.get("parent_region_id");
        if (parentId === null) {
            if (!geometryInsideWorld(geometry, canvas, worldPadding)) return false;
        } else {
            const parent = index.get(parentId);
            if (parent === undefined) return false;
            const parentGeometry = getAbsoluteRegionGeometry(parent, index, preview.regions);
            if (!geometryInsideGeometry(geometry, parentGeometry, regionPadding)) return false;
        }
    }

    for (const location of locations) {
        const regionId = location.get("region_id");
        if (regionId === null) continue;
        const parent = index.get(regionId);
        if (parent === undefined) return false;
        const center = getAbsoluteLocationPosition(
            location,
            index,
            preview.regions,
            preview.locations,
        );
        const parentGeometry = getAbsoluteRegionGeometry(parent, index, preview.regions);
        if (!circleInsideGeometry(
            center,
            locationRadius(location, degreeIndex, radiusState),
            parentGeometry,
            regionPadding,
        )) return false;
    }

    return true;
}

export function buildCanvasScalePlan(
    world: World,
    canvas: WorldCanvasGeometry | null,
    affectedRegions: readonly Region[],
    affectedLocations: readonly Location[],
    index: RegionIndex,
    preview: ScalePreview,
): CanvasScalePlan {
    const regionUpdates = affectedRegions.map((region) => {
        const absolute = preview.regions.get(regionEntityKey(region));
        if (absolute === undefined) throw new Error("Missing region scale preview");
        const parentId = region.get("parent_region_id");
        const parent = parentId === null ? null : index.get(parentId) ?? null;
        if (parentId !== null && parent === null) throw new Error("Missing region parent");
        const parentCenter = parent === null
            ? {x: 0, y: 0}
            : getAbsoluteRegionGeometry(parent, index, preview.regions);
        return {
            region,
            localGeometry: {
                x: absolute.x - parentCenter.x,
                y: absolute.y - parentCenter.y,
                width: absolute.width,
                height: absolute.height,
            },
        };
    });

    const locationUpdates = affectedLocations.map((location) => {
        const absolute = preview.locations.get(locationEntityKey(location));
        if (absolute === undefined) throw new Error("Missing location scale preview");
        const regionId = location.get("region_id");
        if (regionId === null) {
            return {location, localPosition: absolute};
        }
        const parent = index.get(regionId);
        if (parent === undefined) throw new Error("Missing location parent");
        const parentCenter = getAbsoluteRegionGeometry(parent, index, preview.regions);
        return {
            location,
            localPosition: {
                x: absolute.x - parentCenter.x,
                y: absolute.y - parentCenter.y,
            },
        };
    });

    return {
        worldId: world.get("id"),
        world,
        worldCanvas: canvas,
        regions: regionUpdates,
        locations: locationUpdates,
    };
}
