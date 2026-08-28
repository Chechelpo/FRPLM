import type {RegionGeometry} from "@frplm/host-sdk";
import {
    COLLAPSED_REGION_HEIGHT,
    COLLAPSED_REGION_LABEL_HEIGHT_RATIO,
    COLLAPSED_REGION_WIDTH,
    DEFAULT_LOD_DETAIL,
    DEFAULT_LOCATION_RADIUS,
    DEFAULT_REGION_HEIGHT,
    DEFAULT_REGION_WIDTH,
    EDGE_HALO_STROKE_MULTIPLIER,
    EDGE_MIN_SCREEN_STROKE_WIDTH,
    EDGE_SELECTED_STROKE_MULTIPLIER,
    EDGE_STROKE_RADIUS_RATIO,
    GEOMETRY_EPSILON,
    LOCATION_LABEL_RADIUS_RATIO,
    LOCATION_MIN_RESIZE_SCREEN_RADIUS,
    MAX_LOD_DETAIL,
    MIN_LOD_DETAIL,
    NODE_MIN_RENDER_SCREEN_EXTENT,
    NODE_LABEL_MIN_SCREEN_FONT_SIZE,
    REGION_CONTENT_PADDING,
    REGION_LABEL_HEIGHT_RATIO,
    REGION_LABEL_WIDTH_RATIO,
    REGION_MIN_RESIZE_SCREEN_HEIGHT,
    REGION_MIN_RESIZE_SCREEN_WIDTH,
    WORLD_CONTENT_PADDING,
} from "../constants.js";

function assertPositiveFinite(value: number, label: string): void {
    if (!Number.isFinite(value) || value <= 0) {
        throw new Error(`${label} must be finite and positive`);
    }
}

/** Converts a presentation/interaction size into authored world units. */
export function screenPixelsToWorldUnits(
    screenPixels: number,
    zoom: number,
): number {
    if (!Number.isFinite(screenPixels) || screenPixels < 0) {
        throw new Error("Screen size must be finite and non-negative");
    }
    assertPositiveFinite(zoom, "Viewport zoom");
    return screenPixels / zoom;
}

export function zoomAwareRegionMinimum(
    zoom: number,
    screenWidth = REGION_MIN_RESIZE_SCREEN_WIDTH,
    screenHeight = REGION_MIN_RESIZE_SCREEN_HEIGHT,
): {
    width: number;
    height: number;
} {
    return {
        width: Math.max(
            GEOMETRY_EPSILON,
            screenPixelsToWorldUnits(screenWidth, zoom),
        ),
        height: Math.max(
            GEOMETRY_EPSILON,
            screenPixelsToWorldUnits(screenHeight, zoom),
        ),
    };
}

export function zoomAwareRegionCreationSize(
    zoom: number,
    screenWidth = DEFAULT_REGION_WIDTH,
    screenHeight = DEFAULT_REGION_HEIGHT,
): {
    width: number;
    height: number;
} {
    return {
        width: Math.max(
            GEOMETRY_EPSILON,
            screenPixelsToWorldUnits(screenWidth, zoom),
        ),
        height: Math.max(
            GEOMETRY_EPSILON,
            screenPixelsToWorldUnits(screenHeight, zoom),
        ),
    };
}

export function zoomAwareLocationCreationRadius(
    zoom: number,
    screenRadius = DEFAULT_LOCATION_RADIUS,
): number {
    return Math.max(
        GEOMETRY_EPSILON,
        screenPixelsToWorldUnits(screenRadius, zoom),
    );
}

export function zoomAwareLocationMinimumRadius(
    zoom: number,
    screenRadius = LOCATION_MIN_RESIZE_SCREEN_RADIUS,
): number {
    return Math.max(
        GEOMETRY_EPSILON,
        screenPixelsToWorldUnits(screenRadius, zoom),
    );
}

export function zoomAwareRegionPadding(
    zoom: number,
    screenPadding = REGION_CONTENT_PADDING,
): number {
    return screenPixelsToWorldUnits(screenPadding, zoom);
}

export function zoomAwareWorldPadding(
    zoom: number,
    screenPadding = WORLD_CONTENT_PADDING,
): number {
    return screenPixelsToWorldUnits(screenPadding, zoom);
}

/**
 * Collapsed regions are UI cards rather than authored geometry. Their display
 * never exceeds the authored expanded rectangle and never grows beyond the
 * nominal collapsed-card footprint on screen.
 */
export function zoomAwareCollapsedRegionSize(
    expanded: Pick<RegionGeometry, "width" | "height">,
    zoom: number,
    screenWidth = COLLAPSED_REGION_WIDTH,
    screenHeight = COLLAPSED_REGION_HEIGHT,
): {width: number; height: number} {
    assertPositiveFinite(expanded.width, "Expanded region width");
    assertPositiveFinite(expanded.height, "Expanded region height");
    return {
        width: Math.max(
            GEOMETRY_EPSILON,
            Math.min(
                expanded.width,
                screenPixelsToWorldUnits(screenWidth, zoom),
            ),
        ),
        height: Math.max(
            GEOMETRY_EPSILON,
            Math.min(
                expanded.height,
                screenPixelsToWorldUnits(screenHeight, zoom),
            ),
        ),
    };
}

export function locationLabelWorldFontSize(
    radius: number,
    radiusRatio = LOCATION_LABEL_RADIUS_RATIO,
): number {
    assertPositiveFinite(radius, "Location radius");
    assertPositiveFinite(radiusRatio, "Location label radius ratio");
    return Math.max(GEOMETRY_EPSILON, radius * radiusRatio);
}

export function regionLabelWorldFontSize(
    geometry: Pick<RegionGeometry, "width" | "height">,
    collapsed: boolean,
    widthRatio = REGION_LABEL_WIDTH_RATIO,
    heightRatio = REGION_LABEL_HEIGHT_RATIO,
    collapsedHeightRatio = COLLAPSED_REGION_LABEL_HEIGHT_RATIO,
): number {
    assertPositiveFinite(geometry.width, "Region width");
    assertPositiveFinite(geometry.height, "Region height");
    assertPositiveFinite(widthRatio, "Region label width ratio");
    assertPositiveFinite(heightRatio, "Region label height ratio");
    assertPositiveFinite(collapsedHeightRatio, "Collapsed region label height ratio");
    return Math.max(
        GEOMETRY_EPSILON,
        collapsed
            ? geometry.height * collapsedHeightRatio
            : Math.min(
                geometry.width * widthRatio,
                geometry.height * heightRatio,
            ),
    );
}

export function labelIsReadable(
    worldFontSize: number,
    zoom: number,
    lodDetail = DEFAULT_LOD_DETAIL,
    minimumScreenFontSize = NODE_LABEL_MIN_SCREEN_FONT_SIZE,
): boolean {
    assertPositiveFinite(worldFontSize, "World font size");
    assertPositiveFinite(zoom, "Viewport zoom");
    assertPositiveFinite(minimumScreenFontSize, "Minimum screen font size");
    return worldFontSize * zoom >=
        minimumScreenFontSize * lodVisibilityThresholdMultiplier(lodDetail);
}

export function edgeWorldStrokeWidth(
    sourceRadius: number,
    destinationRadius: number,
    radiusRatio = EDGE_STROKE_RADIUS_RATIO,
): number {
    assertPositiveFinite(sourceRadius, "Edge source radius");
    assertPositiveFinite(destinationRadius, "Edge destination radius");
    assertPositiveFinite(radiusRatio, "Edge stroke radius ratio");
    return Math.max(
        GEOMETRY_EPSILON,
        Math.min(sourceRadius, destinationRadius) * radiusRatio,
    );
}

export function edgeWorldHaloWidth(
    edgeStrokeWidth: number,
    multiplier = EDGE_HALO_STROKE_MULTIPLIER,
): number {
    assertPositiveFinite(edgeStrokeWidth, "Edge stroke width");
    assertPositiveFinite(multiplier, "Edge halo stroke multiplier");
    return edgeStrokeWidth * multiplier;
}

export function edgeSelectedWorldStrokeWidth(
    edgeStrokeWidth: number,
    multiplier = EDGE_SELECTED_STROKE_MULTIPLIER,
): number {
    assertPositiveFinite(edgeStrokeWidth, "Edge stroke width");
    assertPositiveFinite(multiplier, "Selected edge stroke multiplier");
    return edgeStrokeWidth * multiplier;
}

export function edgeIsReadableAtZoom(
    edgeStrokeWidth: number,
    zoom: number,
    lodDetail = DEFAULT_LOD_DETAIL,
    minimumScreenStrokeWidth = EDGE_MIN_SCREEN_STROKE_WIDTH,
): boolean {
    assertPositiveFinite(edgeStrokeWidth, "Edge stroke width");
    assertPositiveFinite(zoom, "Viewport zoom");
    assertPositiveFinite(minimumScreenStrokeWidth, "Minimum edge screen stroke width");
    return edgeStrokeWidth * zoom >=
        minimumScreenStrokeWidth * lodVisibilityThresholdMultiplier(lodDetail);
}

export function normalizeLodDetail(value: number): number {
    if (!Number.isFinite(value)) return DEFAULT_LOD_DETAIL;
    return Math.min(MAX_LOD_DETAIL, Math.max(MIN_LOD_DETAIL, value));
}

/**
 * 0 = aggressive culling, 50 = nominal thresholds, 100 = maximum detail.
 * A smooth exponential mapping keeps the middle of the slider useful.
 */
export function lodVisibilityThresholdMultiplier(lodDetail: number): number {
    const normalized = normalizeLodDetail(lodDetail);
    return 2 ** ((DEFAULT_LOD_DETAIL - normalized) / DEFAULT_LOD_DETAIL);
}

export function nodeIsRenderableAtZoom(
    worldExtent: number,
    zoom: number,
    lodDetail = DEFAULT_LOD_DETAIL,
    minimumScreenExtent = NODE_MIN_RENDER_SCREEN_EXTENT,
): boolean {
    assertPositiveFinite(worldExtent, "Node world extent");
    assertPositiveFinite(zoom, "Viewport zoom");
    assertPositiveFinite(minimumScreenExtent, "Minimum node screen extent");
    return worldExtent * zoom >=
        minimumScreenExtent * lodVisibilityThresholdMultiplier(lodDetail);
}
