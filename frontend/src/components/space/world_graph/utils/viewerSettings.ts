import {
    COLLAPSED_REGION_HEIGHT,
    COLLAPSED_REGION_LABEL_HEIGHT_RATIO,
    COLLAPSED_REGION_WIDTH,
    DEFAULT_LOCATION_RADIUS,
    DEFAULT_LOD_DETAIL,
    DEFAULT_REGION_HEIGHT,
    DEFAULT_REGION_WIDTH,
    DRAG_THRESHOLD_SCREEN_PIXELS,
    EDGE_ARROW_STROKE_MULTIPLIER,
    EDGE_BIDIRECTIONAL_LANE_OFFSET,
    EDGE_HALO_STROKE_MULTIPLIER,
    EDGE_MIN_SCREEN_STROKE_WIDTH,
    EDGE_SELECTED_STROKE_MULTIPLIER,
    EDGE_STROKE_RADIUS_RATIO,
    LOCATION_LABEL_RADIUS_RATIO,
    LOCATION_MIN_RESIZE_SCREEN_RADIUS,
    LOCATION_NODE_HIT_SCREEN_RADIUS,
    MAX_LOD_DETAIL,
    MIN_LOD_DETAIL,
    NODE_LABEL_MIN_SCREEN_FONT_SIZE,
    NODE_MIN_RENDER_SCREEN_EXTENT,
    REGION_CONTENT_PADDING,
    REGION_LABEL_HEIGHT_RATIO,
    REGION_LABEL_WIDTH_RATIO,
    REGION_MIN_RESIZE_SCREEN_HEIGHT,
    REGION_MIN_RESIZE_SCREEN_WIDTH,
    REGION_NODE_HIT_SCREEN_SIZE,
    REPARENT_RELEASE_SCREEN_PIXELS,
    RESIZE_GESTURE_DOUBLING_SCREEN_PIXELS,
    WORLD_CONTENT_PADDING,
} from "../constants";

/**
 * Per-world editor/view preferences. These values deliberately mirror the
 * user-tunable presentation/interaction constants while leaving persistence
 * protocol and geometry-safety invariants compile-time only.
 */
export type WorldGraphViewerSettings = {
    lodDetail: number;
    nodeMinRenderScreenExtent: number;
    nodeLabelMinScreenFontSize: number;

    defaultRegionScreenWidth: number;
    defaultRegionScreenHeight: number;
    regionMinResizeScreenWidth: number;
    regionMinResizeScreenHeight: number;
    defaultLocationScreenRadius: number;
    locationMinResizeScreenRadius: number;

    regionContentPaddingScreen: number;
    worldContentPaddingScreen: number;
    collapsedRegionScreenWidth: number;
    collapsedRegionScreenHeight: number;

    locationLabelRadiusRatio: number;
    regionLabelWidthRatio: number;
    regionLabelHeightRatio: number;
    collapsedRegionLabelHeightRatio: number;

    locationNodeHitScreenRadius: number;
    regionNodeHitScreenSize: number;
    resizeGestureDoublingScreenPixels: number;
    dragThresholdScreenPixels: number;
    reparentReleaseScreenPixels: number;

    edgeStrokeRadiusRatio: number;
    edgeHaloStrokeMultiplier: number;
    edgeSelectedStrokeMultiplier: number;
    edgeMinScreenStrokeWidth: number;
    edgeArrowStrokeMultiplier: number;
    edgeBidirectionalLaneOffset: number;
};

export const DEFAULT_WORLD_GRAPH_VIEWER_SETTINGS: Readonly<WorldGraphViewerSettings> = Object.freeze({
    lodDetail: DEFAULT_LOD_DETAIL,
    nodeMinRenderScreenExtent: NODE_MIN_RENDER_SCREEN_EXTENT,
    nodeLabelMinScreenFontSize: NODE_LABEL_MIN_SCREEN_FONT_SIZE,

    defaultRegionScreenWidth: DEFAULT_REGION_WIDTH,
    defaultRegionScreenHeight: DEFAULT_REGION_HEIGHT,
    regionMinResizeScreenWidth: REGION_MIN_RESIZE_SCREEN_WIDTH,
    regionMinResizeScreenHeight: REGION_MIN_RESIZE_SCREEN_HEIGHT,
    defaultLocationScreenRadius: DEFAULT_LOCATION_RADIUS,
    locationMinResizeScreenRadius: LOCATION_MIN_RESIZE_SCREEN_RADIUS,

    regionContentPaddingScreen: REGION_CONTENT_PADDING,
    worldContentPaddingScreen: WORLD_CONTENT_PADDING,
    collapsedRegionScreenWidth: COLLAPSED_REGION_WIDTH,
    collapsedRegionScreenHeight: COLLAPSED_REGION_HEIGHT,

    locationLabelRadiusRatio: LOCATION_LABEL_RADIUS_RATIO,
    regionLabelWidthRatio: REGION_LABEL_WIDTH_RATIO,
    regionLabelHeightRatio: REGION_LABEL_HEIGHT_RATIO,
    collapsedRegionLabelHeightRatio: COLLAPSED_REGION_LABEL_HEIGHT_RATIO,

    locationNodeHitScreenRadius: LOCATION_NODE_HIT_SCREEN_RADIUS,
    regionNodeHitScreenSize: REGION_NODE_HIT_SCREEN_SIZE,
    resizeGestureDoublingScreenPixels: RESIZE_GESTURE_DOUBLING_SCREEN_PIXELS,
    dragThresholdScreenPixels: DRAG_THRESHOLD_SCREEN_PIXELS,
    reparentReleaseScreenPixels: REPARENT_RELEASE_SCREEN_PIXELS,

    edgeStrokeRadiusRatio: EDGE_STROKE_RADIUS_RATIO,
    edgeHaloStrokeMultiplier: EDGE_HALO_STROKE_MULTIPLIER,
    edgeSelectedStrokeMultiplier: EDGE_SELECTED_STROKE_MULTIPLIER,
    edgeMinScreenStrokeWidth: EDGE_MIN_SCREEN_STROKE_WIDTH,
    edgeArrowStrokeMultiplier: EDGE_ARROW_STROKE_MULTIPLIER,
    edgeBidirectionalLaneOffset: EDGE_BIDIRECTIONAL_LANE_OFFSET,
});

export type WorldGraphViewerSettingKey = keyof WorldGraphViewerSettings;

export type WorldGraphViewerSettingBounds = {
    min: number;
    max: number;
};

const SETTING_BOUNDS: Record<WorldGraphViewerSettingKey, WorldGraphViewerSettingBounds> = {
    lodDetail: {min: MIN_LOD_DETAIL, max: MAX_LOD_DETAIL},
    nodeMinRenderScreenExtent: {min: 0.1, max: 64},
    nodeLabelMinScreenFontSize: {min: 1, max: 48},

    defaultRegionScreenWidth: {min: 8, max: 4096},
    defaultRegionScreenHeight: {min: 8, max: 4096},
    regionMinResizeScreenWidth: {min: 1, max: 1024},
    regionMinResizeScreenHeight: {min: 1, max: 1024},
    defaultLocationScreenRadius: {min: 1, max: 512},
    locationMinResizeScreenRadius: {min: 0.5, max: 256},

    regionContentPaddingScreen: {min: 0, max: 512},
    worldContentPaddingScreen: {min: 0, max: 512},
    collapsedRegionScreenWidth: {min: 8, max: 2048},
    collapsedRegionScreenHeight: {min: 8, max: 1024},

    locationLabelRadiusRatio: {min: 0.01, max: 2},
    regionLabelWidthRatio: {min: 0.001, max: 1},
    regionLabelHeightRatio: {min: 0.001, max: 1},
    collapsedRegionLabelHeightRatio: {min: 0.01, max: 1},

    locationNodeHitScreenRadius: {min: 1, max: 128},
    regionNodeHitScreenSize: {min: 1, max: 128},
    resizeGestureDoublingScreenPixels: {min: 8, max: 1024},
    dragThresholdScreenPixels: {min: 0, max: 64},
    reparentReleaseScreenPixels: {min: 0, max: 512},

    edgeStrokeRadiusRatio: {min: 0.001, max: 1},
    edgeHaloStrokeMultiplier: {min: 1, max: 12},
    edgeSelectedStrokeMultiplier: {min: 1, max: 8},
    edgeMinScreenStrokeWidth: {min: 0.01, max: 16},
    edgeArrowStrokeMultiplier: {min: 0.25, max: 16},
    edgeBidirectionalLaneOffset: {min: 0, max: 1024},
};

export function createDefaultWorldGraphViewerSettings(): WorldGraphViewerSettings {
    return {...DEFAULT_WORLD_GRAPH_VIEWER_SETTINGS};
}

export function worldGraphViewerSettingBounds(
    key: WorldGraphViewerSettingKey,
): Readonly<WorldGraphViewerSettingBounds> {
    return SETTING_BOUNDS[key];
}

/**
 * Accepts partial/older persisted objects, rejects non-finite values, and
 * clamps extreme input so the settings panel cannot create unusable geometry.
 */
export function normalizeWorldGraphViewerSettings(
    value: unknown,
): WorldGraphViewerSettings {
    const source = value !== null && typeof value === "object"
        ? value as Record<string, unknown>
        : {};
    const normalized: Record<string, number> = {};

    for (const key of Object.keys(DEFAULT_WORLD_GRAPH_VIEWER_SETTINGS) as WorldGraphViewerSettingKey[]) {
        const fallback = DEFAULT_WORLD_GRAPH_VIEWER_SETTINGS[key];
        const candidate = source[key];
        const finite = typeof candidate === "number" && Number.isFinite(candidate)
            ? candidate
            : fallback;
        const bounds = SETTING_BOUNDS[key];
        normalized[key] = Math.min(bounds.max, Math.max(bounds.min, finite));
    }

    return normalized as WorldGraphViewerSettings;
}
