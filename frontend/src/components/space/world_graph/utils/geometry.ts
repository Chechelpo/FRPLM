import type {
    Location,
    Position,
    Region,
    RegionGeometry,
    World,
} from "@/domain/World";
import {
    DEFAULT_WORLD_HEIGHT,
    DEFAULT_WORLD_WIDTH,
    MIN_REGION_HEIGHT,
    MIN_REGION_WIDTH,
} from "../constants";
import type {
    Bounds,
    LocationPreviewMap,
    RegionPreviewMap,
    WorldCanvasGeometry,
} from "../types";

export type RegionIndex = ReadonlyMap<number, Region>;

export function assertFinite(value: number, label: string): void {
    if (!Number.isFinite(value)) throw new Error(`${label} must be finite`);
}

export function clamp(value: number, minimum: number, maximum: number): number {
    return Math.min(Math.max(value, minimum), maximum);
}

export function locationEntityKey(location: Location): string {
    return `location:${location.get("worldID")}:${location.get("id")}`;
}

export function regionEntityKey(region: Region): string {
    return `region:${region.get("world_id")}:${region.get("id")}`;
}

export function edgeEntityKey(edge: {
    get(field: "world_id" | "from_id" | "to_id"): number;
}): string {
    return `edge:${edge.get("world_id")}:${edge.get("from_id")}->${edge.get("to_id")}`;
}

export function isFinitePosition(position: Position): boolean {
    return Number.isFinite(position.x) && Number.isFinite(position.y);
}

export function screenToWorldPosition(
    screen: Position,
    pan: Position,
    zoom: number,
): Position {
    if (!Number.isFinite(zoom) || zoom <= 0) {
        throw new Error("Viewport zoom must be finite and positive");
    }
    return {
        x: (screen.x - pan.x) / zoom,
        y: (screen.y - pan.y) / zoom,
    };
}


export function isFinitePositiveGeometry(geometry: RegionGeometry): boolean {
    return isFinitePosition(geometry) &&
        Number.isFinite(geometry.width) &&
        Number.isFinite(geometry.height) &&
        geometry.width > 0 &&
        geometry.height > 0;
}

export function normalizeWorldCanvas(world: World): WorldCanvasGeometry {
    const storedWidth = world.get("background_width");
    const storedHeight = world.get("background_height");

    return {
        width: typeof storedWidth === "number" && Number.isFinite(storedWidth) && storedWidth > 0
            ? storedWidth
            : DEFAULT_WORLD_WIDTH,
        height: typeof storedHeight === "number" && Number.isFinite(storedHeight) && storedHeight > 0
            ? storedHeight
            : DEFAULT_WORLD_HEIGHT,
    };
}

export function worldCanvasBounds(canvas: WorldCanvasGeometry): Bounds {
    return {
        left: -canvas.width / 2,
        right: canvas.width / 2,
        top: -canvas.height / 2,
        bottom: canvas.height / 2,
    };
}

export function geometryToBounds(geometry: RegionGeometry): Bounds {
    return {
        left: geometry.x - geometry.width / 2,
        right: geometry.x + geometry.width / 2,
        top: geometry.y - geometry.height / 2,
        bottom: geometry.y + geometry.height / 2,
    };
}

export function boundsToGeometry(bounds: Bounds): RegionGeometry {
    return {
        x: (bounds.left + bounds.right) / 2,
        y: (bounds.top + bounds.bottom) / 2,
        width: bounds.right - bounds.left,
        height: bounds.bottom - bounds.top,
    };
}

export function positionInsideBounds(position: Position, bounds: Bounds): boolean {
    return position.x >= bounds.left &&
        position.x <= bounds.right &&
        position.y >= bounds.top &&
        position.y <= bounds.bottom;
}

export function circleInsideGeometry(
    center: Position,
    radius: number,
    geometry: RegionGeometry,
    padding = 0,
): boolean {
    const bounds = geometryToBounds(geometry);
    const inset = radius + padding;
    return center.x - inset >= bounds.left &&
        center.x + inset <= bounds.right &&
        center.y - inset >= bounds.top &&
        center.y + inset <= bounds.bottom;
}

export function geometryInsideGeometry(
    child: RegionGeometry,
    parent: RegionGeometry,
    padding = 0,
): boolean {
    const childBounds = geometryToBounds(child);
    const parentBounds = geometryToBounds(parent);
    return childBounds.left >= parentBounds.left + padding &&
        childBounds.right <= parentBounds.right - padding &&
        childBounds.top >= parentBounds.top + padding &&
        childBounds.bottom <= parentBounds.bottom - padding;
}

export function geometryInsideWorld(
    geometry: RegionGeometry,
    canvas: WorldCanvasGeometry,
    padding = 0,
): boolean {
    const child = geometryToBounds(geometry);
    const parent = worldCanvasBounds(canvas);
    return child.left >= parent.left + padding &&
        child.right <= parent.right - padding &&
        child.top >= parent.top + padding &&
        child.bottom <= parent.bottom - padding;
}

export function circleIntersectsGeometry(
    center: Position,
    radius: number,
    geometry: RegionGeometry,
): boolean {
    const bounds = geometryToBounds(geometry);
    const nearestX = clamp(center.x, bounds.left, bounds.right);
    const nearestY = clamp(center.y, bounds.top, bounds.bottom);
    const dx = center.x - nearestX;
    const dy = center.y - nearestY;
    return dx * dx + dy * dy <= radius * radius;
}

export function marqueeGeometry(first: Position, second: Position): RegionGeometry {
    const left = Math.min(first.x, second.x);
    const right = Math.max(first.x, second.x);
    const top = Math.min(first.y, second.y);
    const bottom = Math.max(first.y, second.y);
    return boundsToGeometry({left, right, top, bottom});
}

export function buildRegionIndex(regions: readonly Region[]): RegionIndex {
    const result = new Map<number, Region>();
    let worldId: number | null = null;

    for (const region of regions) {
        if (worldId === null) worldId = region.get("world_id");
        if (region.get("world_id") !== worldId) {
            throw new Error("Cannot index regions from different worlds");
        }
        const id = region.get("id");
        if (result.has(id)) throw new Error("Duplicate region ID");
        result.set(id, region);
    }
    return result;
}

export function getAbsoluteRegionCenter(
    region: Region,
    index: RegionIndex,
    previews: RegionPreviewMap = new Map(),
    visiting: Set<number> = new Set(),
): Position {
    const preview = previews.get(regionEntityKey(region));
    if (preview !== undefined) return {x: preview.x, y: preview.y};

    const id = region.get("id");
    if (visiting.has(id)) throw new Error("Cycle detected in region hierarchy");
    visiting.add(id);

    const local = {x: region.get("x"), y: region.get("y")};
    const parentId = region.get("parent_region_id");
    if (parentId === null) {
        visiting.delete(id);
        return local;
    }

    const parent = index.get(parentId);
    if (parent === undefined) throw new Error("Region references a missing parent");
    if (parent.get("world_id") !== region.get("world_id")) {
        throw new Error("Region hierarchy crosses world boundaries");
    }

    const parentCenter = getAbsoluteRegionCenter(parent, index, previews, visiting);
    visiting.delete(id);
    return {x: parentCenter.x + local.x, y: parentCenter.y + local.y};
}

export function getAbsoluteRegionGeometry(
    region: Region,
    index: RegionIndex,
    previews: RegionPreviewMap = new Map(),
): RegionGeometry {
    const preview = previews.get(regionEntityKey(region));
    if (preview !== undefined) return preview;
    const center = getAbsoluteRegionCenter(region, index, previews);
    return {
        x: center.x,
        y: center.y,
        width: region.get("width"),
        height: region.get("height"),
    };
}

export function getAbsoluteLocationPosition(
    location: Location,
    index: RegionIndex,
    regionPreviews: RegionPreviewMap = new Map(),
    locationPreviews: LocationPreviewMap = new Map(),
): Position {
    const preview = locationPreviews.get(locationEntityKey(location));
    if (preview !== undefined) return preview;

    const local = {x: location.get("x"), y: location.get("y")};
    const regionId = location.get("region_id");
    if (regionId === null) return local;

    const region = index.get(regionId);
    if (region === undefined) throw new Error("Location references a missing region");
    if (region.get("world_id") !== location.get("worldID")) {
        throw new Error("Location and region belong to different worlds");
    }
    const center = getAbsoluteRegionCenter(region, index, regionPreviews);
    return {x: center.x + local.x, y: center.y + local.y};
}

export function absoluteToLocal(
    absolute: Position,
    parent: Region | null,
    index: RegionIndex,
    previews: RegionPreviewMap = new Map(),
): Position {
    if (parent === null) return {...absolute};
    const center = getAbsoluteRegionCenter(parent, index, previews);
    return {x: absolute.x - center.x, y: absolute.y - center.y};
}

export function regionDepth(region: Region, index: RegionIndex): number {
    let depth = 0;
    let current: Region = region;
    const visited = new Set<number>();

    while (current.get("parent_region_id") !== null) {
        const id = current.get("id");
        if (visited.has(id)) throw new Error("Cycle detected in region hierarchy");
        visited.add(id);
        const parent = index.get(current.get("parent_region_id") as number);
        if (parent === undefined) throw new Error("Region references a missing parent");
        current = parent;
        depth += 1;
    }
    return depth;
}

export function isRegionDescendantOf(
    region: Region,
    possibleAncestor: Region,
    index: RegionIndex,
): boolean {
    let parentId = region.get("parent_region_id");
    const visited = new Set<number>();
    while (parentId !== null) {
        if (visited.has(parentId)) throw new Error("Cycle detected in region hierarchy");
        visited.add(parentId);
        if (parentId === possibleAncestor.get("id")) return true;
        const parent = index.get(parentId);
        if (parent === undefined) throw new Error("Region references a missing parent");
        parentId = parent.get("parent_region_id");
    }
    return false;
}

export function estimateRegionMinimum(name: string): {width: number; height: number} {
    void name;
    return {
        width: MIN_REGION_WIDTH,
        height: MIN_REGION_HEIGHT,
    };
}

export function affineTransformPosition(
    position: Position,
    oldCanvas: RegionGeometry,
    newCanvas: RegionGeometry,
): Position {
    const scaleX = newCanvas.width / oldCanvas.width;
    const scaleY = newCanvas.height / oldCanvas.height;
    return {
        x: newCanvas.x + (position.x - oldCanvas.x) * scaleX,
        y: newCanvas.y + (position.y - oldCanvas.y) * scaleY,
    };
}

export function affineTransformGeometry(
    geometry: RegionGeometry,
    oldCanvas: RegionGeometry,
    newCanvas: RegionGeometry,
): RegionGeometry {
    const center = affineTransformPosition(geometry, oldCanvas, newCanvas);
    return {
        ...center,
        width: geometry.width * (newCanvas.width / oldCanvas.width),
        height: geometry.height * (newCanvas.height / oldCanvas.height),
    };
}

export function worldCanvasAsGeometry(canvas: WorldCanvasGeometry): RegionGeometry {
    return {x: 0, y: 0, width: canvas.width, height: canvas.height};
}
