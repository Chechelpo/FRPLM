import type {
    BackgroundFit,
    LocationEdge,
    Position,
    Region,
    RegionGeometry,
} from "@frplm/host-sdk";
import {
    EDGE_ARROW_BASE_SCREEN_SIZE,
    EDGE_BIDIRECTIONAL_LANE_OFFSET,
    EDGE_ARROW_MAX_SCREEN_SIZE,
    EDGE_ARROW_MIN_SCREEN_SIZE,
    GEOMETRY_EPSILON,
    REGION_CONTENT_PADDING,
} from "../constants.js";
import type {
    Bounds,
    WorldCanvasGeometry,
} from "../types.js";
import {
    boundsToGeometry,
    clamp,
    geometryToBounds,
    worldCanvasBounds,
} from "./geometry.js";
import {
    locationLabelWorldFontSize,
    zoomAwareRegionCreationSize,
    zoomAwareRegionPadding,
    zoomAwareWorldPadding,
} from "./sizing.js";
export type EdgeRenderModel = {
    key: string;
    edge: LocationEdge;
    path: string;
    traversable: boolean;
    strokeWidth: number;
    haloWidth: number;
};

/**
 * Returns an arrowhead size in world units that produces a zoom-aware,
 * screen-bounded marker after the viewport transform is applied.
 */
export function edgeArrowWorldSize(zoom: number): number {
    if (!Number.isFinite(zoom) || zoom <= 0) {
        throw new Error("Viewport zoom must be finite and positive");
    }
    const screenSize = clamp(
        EDGE_ARROW_BASE_SCREEN_SIZE * zoom,
        EDGE_ARROW_MIN_SCREEN_SIZE,
        EDGE_ARROW_MAX_SCREEN_SIZE,
    );
    return screenSize / zoom;
}

export function edgeIsIncidentToSelectedLocation(
    fromId: number,
    toId: number,
    selectedLocationIds: ReadonlySet<number>,
): boolean {
    return selectedLocationIds.has(fromId) || selectedLocationIds.has(toId);
}

export function preserveAspectRatio(fit: BackgroundFit): string {
    return fit === "cover" ? "xMidYMid slice" : "xMidYMid meet";
}

export function clampOverlayPosition(
    position: Position,
    hostWidth: number,
    hostHeight: number,
    overlayWidth: number,
    overlayHeight: number,
    margin = 10,
): Position {
    return {
        x: clamp(position.x, margin, Math.max(margin, hostWidth - overlayWidth - margin)),
        y: clamp(position.y, margin, Math.max(margin, hostHeight - overlayHeight - margin)),
    };
}

export type ContextMenuPlacement = {
    position: Position;
    maxHeight: number;
};

/**
 * Places a measured screen-space menu next to its pointer anchor. Horizontal
 * side selection happens before vertical fitting so a lower-edge click cannot
 * be displaced by an assumed worst-case menu height.
 */
export function placeContextMenu(
    anchor: Position,
    hostWidth: number,
    hostHeight: number,
    menuWidth: number,
    menuHeight: number,
    gap = 8,
    margin = 10,
): ContextMenuPlacement {
    const safeHostWidth = Math.max(1, hostWidth);
    const safeHostHeight = Math.max(1, hostHeight);
    const safeWidth = Math.max(0, menuWidth);
    const safeHeight = Math.max(0, menuHeight);
    const right = anchor.x + gap;
    const left = anchor.x - gap - safeWidth;
    const x = right + safeWidth <= safeHostWidth - margin
        ? right
        : left >= margin
            ? left
            : clamp(right, margin, Math.max(margin, safeHostWidth - safeWidth - margin));

    const availableBelow = Math.max(0, safeHostHeight - margin - (anchor.y + gap));
    const availableAbove = Math.max(0, anchor.y - gap - margin);
    const maxHeight = Math.max(1, safeHostHeight - margin * 2);

    if (safeHeight <= availableBelow) {
        return {position: {x, y: anchor.y + gap}, maxHeight};
    }
    if (safeHeight <= availableAbove) {
        return {position: {x, y: anchor.y - gap - safeHeight}, maxHeight};
    }

    if (availableBelow >= availableAbove) {
        return {
            position: {x, y: clamp(anchor.y + gap, margin, safeHostHeight - margin)},
            maxHeight: Math.max(1, availableBelow),
        };
    }
    return {
        position: {x, y: margin},
        maxHeight: Math.max(1, availableAbove),
    };
}

function clampGeometryCenter(
    geometry: RegionGeometry,
    container: Bounds,
    padding: number,
): RegionGeometry {
    const halfWidth = geometry.width / 2;
    const halfHeight = geometry.height / 2;
    const halfContainerWidth = Math.max(0, (container.right - container.left) / 2);
    const halfContainerHeight = Math.max(0, (container.bottom - container.top) / 2);
    const horizontalPadding = Math.min(
        padding,
        Math.max(0, halfContainerWidth - halfWidth),
    );
    const verticalPadding = Math.min(
        padding,
        Math.max(0, halfContainerHeight - halfHeight),
    );
    return {
        ...geometry,
        x: clamp(
            geometry.x,
            container.left + horizontalPadding + halfWidth,
            container.right - horizontalPadding - halfWidth,
        ),
        y: clamp(
            geometry.y,
            container.top + verticalPadding + halfHeight,
            container.bottom - verticalPadding - halfHeight,
        ),
    };
}

export function createRootRegionGeometry(
    pointer: Position,
    canvas: WorldCanvasGeometry,
    zoom = 1,
    screenWidth?: number,
    screenHeight?: number,
    screenPadding?: number,
): RegionGeometry {
    const desired = zoomAwareRegionCreationSize(
        zoom,
        screenWidth,
        screenHeight,
    );
    const padding = zoomAwareWorldPadding(zoom, screenPadding);
    const width = Math.max(
        GEOMETRY_EPSILON,
        Math.min(desired.width, Math.max(GEOMETRY_EPSILON, canvas.width - padding * 2)),
    );
    const height = Math.max(
        GEOMETRY_EPSILON,
        Math.min(desired.height, Math.max(GEOMETRY_EPSILON, canvas.height - padding * 2)),
    );
    return clampGeometryCenter(
        {x: pointer.x, y: pointer.y, width, height},
        worldCanvasBounds(canvas),
        padding,
    );
}

export function createSubRegionGeometry(
    localPointer: Position,
    parent: Pick<RegionGeometry, "width" | "height">,
    zoom = 1,
    screenWidth?: number,
    screenHeight?: number,
    screenPadding?: number,
): RegionGeometry {
    const desired = zoomAwareRegionCreationSize(
        zoom,
        screenWidth,
        screenHeight,
    );
    const padding = zoomAwareRegionPadding(zoom, screenPadding);
    const width = Math.max(
        GEOMETRY_EPSILON,
        Math.min(desired.width, Math.max(GEOMETRY_EPSILON, parent.width - padding * 2)),
    );
    const height = Math.max(
        GEOMETRY_EPSILON,
        Math.min(desired.height, Math.max(GEOMETRY_EPSILON, parent.height - padding * 2)),
    );
    const parentBounds: Bounds = {
        left: -parent.width / 2,
        right: parent.width / 2,
        top: -parent.height / 2,
        bottom: parent.height / 2,
    };
    return clampGeometryCenter(
        {x: localPointer.x, y: localPointer.y, width, height},
        parentBounds,
        padding,
    );
}

export function clampLocationLocalPosition(
    local: Position,
    radius: number,
    parent: Pick<RegionGeometry, "width" | "height">,
    padding = REGION_CONTENT_PADDING,
): Position {
    const inset = radius + padding;
    const minimumX = -parent.width / 2 + inset;
    const maximumX = parent.width / 2 - inset;
    const minimumY = -parent.height / 2 + inset;
    const maximumY = parent.height / 2 - inset;
    return {
        x: minimumX <= maximumX ? clamp(local.x, minimumX, maximumX) : 0,
        y: minimumY <= maximumY ? clamp(local.y, minimumY, maximumY) : 0,
    };
}

export function makeDirectedEdgePath(
    source: Position,
    destination: Position,
    sourceRadius: number,
    destinationRadius: number,
    laneOffset = 0,
): string {
    const dx = destination.x - source.x;
    const dy = destination.y - source.y;
    const length = Math.hypot(dx, dy);
    if (length < 0.001) return `M ${source.x} ${source.y}`;

    const ux = dx / length;
    const uy = dy / length;
    const nx = -uy;
    const ny = ux;

    const maximumOffset = Math.max(
        0,
        Math.min(sourceRadius, destinationRadius) * 0.78,
    );
    const offset = clamp(
        laneOffset,
        -maximumOffset,
        maximumOffset,
    );
    const sourceAlong = Math.sqrt(Math.max(
        0,
        sourceRadius * sourceRadius - offset * offset,
    ));
    const destinationAlong = Math.sqrt(Math.max(
        0,
        destinationRadius * destinationRadius - offset * offset,
    ));
    const start = {
        x: source.x + nx * offset + ux * sourceAlong,
        y: source.y + ny * offset + uy * sourceAlong,
    };
    const end = {
        x: destination.x + nx * offset - ux * destinationAlong,
        y: destination.y + ny * offset - uy * destinationAlong,
    };
    return `M ${start.x} ${start.y} L ${end.x} ${end.y}`;
}

export function oppositeEdgeLaneOffset(
    edge: LocationEdge,
    existingKeys: ReadonlySet<string>,
    laneOffset = EDGE_BIDIRECTIONAL_LANE_OFFSET,
): number {
    const inverse = `edge:${edge.get("world_id")}:${edge.get("to_id")}->${edge.get("from_id")}`;
    if (!existingKeys.has(inverse)) return 0;
    return laneOffset;
}

export function splitLocationLabel(
    name: string,
    radius: number,
    fontSize = locationLabelWorldFontSize(radius),
): readonly string[] {
    const normalized = name.trim();
    const characterWidth = Math.max(GEOMETRY_EPSILON, fontSize * 0.56);
    const maxCharacters = Math.max(
        4,
        Math.floor((radius * 1.9) / characterWidth),
    );
    if (normalized.length <= maxCharacters) return [normalized];
    const words = normalized.split(/\s+/);
    if (words.length === 1) return [truncate(normalized, maxCharacters)];
    const midpoint = Math.ceil(words.length / 2);
    return [
        truncate(words.slice(0, midpoint).join(" "), maxCharacters),
        truncate(words.slice(midpoint).join(" "), maxCharacters),
    ];
}

export function truncate(value: string, maximum: number): string {
    if (value.length <= maximum) return value;
    return `${value.slice(0, Math.max(1, maximum - 1))}…`;
}

export function truncateRegionLabel(
    region: Region,
    geometry: RegionGeometry,
    fontSize = Math.max(GEOMETRY_EPSILON, geometry.height * 0.06),
): string {
    const characterWidth = Math.max(GEOMETRY_EPSILON, fontSize * 0.56);
    const availableWidth = Math.max(GEOMETRY_EPSILON, geometry.width * 0.78);
    return truncate(
        region.get("name"),
        Math.max(3, Math.floor(availableWidth / characterWidth)),
    );
}

export function regionLabelWidth(
    label: string,
    geometry: RegionGeometry,
    fontSize = Math.max(GEOMETRY_EPSILON, geometry.height * 0.06),
): number {
    const horizontalPadding = fontSize * 1.4;
    const desired = label.length * fontSize * 0.56 + horizontalPadding * 2;
    const available = Math.max(GEOMETRY_EPSILON, geometry.width - fontSize * 1.4);
    return Math.min(desired, available);
}

export function normalizedOpacity(value: number): number {
    return Number.isFinite(value) ? clamp(value, 0, 1) : 1;
}

export function geometryRectAttributes(geometry: RegionGeometry): {
    x: number;
    y: number;
    width: number;
    height: number;
} {
    const bounds = geometryToBounds(geometry);
    return {
        x: bounds.left,
        y: bounds.top,
        width: geometry.width,
        height: geometry.height,
    };
}

export function boundsRectGeometry(bounds: Bounds): RegionGeometry {
    return boundsToGeometry(bounds);
}
