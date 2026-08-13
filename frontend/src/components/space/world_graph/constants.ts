export const DEFAULT_WORLD_WIDTH = 1600;
export const DEFAULT_WORLD_HEIGHT = 1000;

/**
 * Region defaults are nominal screen-space creation targets at 100% zoom.
 * Creation converts them back to world units through the active viewport zoom.
 */
export const DEFAULT_REGION_WIDTH = 360;
export const DEFAULT_REGION_HEIGHT = 240;

export const MIN_WORLD_WIDTH = 480;
export const MIN_WORLD_HEIGHT = 320;

/** Technical guard against zero/negative authored geometry, not a UX minimum. */
export const GEOMETRY_EPSILON = 1e-6;
/** @deprecated Use zoomAwareRegionMinimum() for interactive resizing. */
export const MIN_REGION_WIDTH = GEOMETRY_EPSILON;
/** @deprecated Use zoomAwareRegionMinimum() for interactive resizing. */
export const MIN_REGION_HEIGHT = GEOMETRY_EPSILON;

/** Minimum region footprint while resizing, measured in screen pixels. */
export const REGION_MIN_RESIZE_SCREEN_WIDTH = 56;
export const REGION_MIN_RESIZE_SCREEN_HEIGHT = 40;

/** Nominal screen-space containment gutters at 100% zoom. */
export const REGION_CONTENT_PADDING = 18;
export const WORLD_CONTENT_PADDING = 24;

/** Automatic graph-derived location sizing. */
export const DEFAULT_LOCATION_RADIUS = 24;
export const LOCATION_RADIUS_GROWTH = 2;
export const AUTOMATIC_LOCATION_MAX_RADIUS = 64;

/**
 * Explicit authored radii have no meaningful fixed world-space minimum.
 * The epsilon only prevents degenerate circles; resizing applies a zoom-aware
 * screen-space minimum instead.
 */
export const MIN_LOCATION_RADIUS = GEOMETRY_EPSILON;
/** Compatibility export: containment, not an arbitrary global cap, limits size. */
export const MAX_LOCATION_RADIUS = Number.MAX_VALUE;
export const LOCATION_MIN_RESIZE_SCREEN_RADIUS = 6;
export const LOCATION_NODE_HIT_SCREEN_RADIUS = 12;

/** Node-label level-of-detail thresholds and proportional type scales. */
export const NODE_LABEL_MIN_SCREEN_FONT_SIZE = 7;
export const LOCATION_LABEL_RADIUS_RATIO = 0.46;
export const REGION_LABEL_WIDTH_RATIO = 0.038;
export const REGION_LABEL_HEIGHT_RATIO = 0.06;
export const COLLAPSED_REGION_LABEL_HEIGHT_RATIO = 0.25;
export const DEFAULT_LOD_DETAIL = 50;
export const MIN_LOD_DETAIL = 0;
export const MAX_LOD_DETAIL = 100;
/** Nodes smaller than this screen extent may be omitted by LOD. */
export const NODE_MIN_RENDER_SCREEN_EXTENT = 3;

export const DRAG_THRESHOLD_SCREEN_PIXELS = 5;

/** Radial viewport travel required to double/halve a Shift+S resize target. */
export const RESIZE_GESTURE_DOUBLING_SCREEN_PIXELS = 96;

/** Screen-space pull required before a child detaches from its current parent. */
export const REPARENT_RELEASE_SCREEN_PIXELS = 18;

/** Directed-edge arrowhead size at 100% zoom, expressed in screen pixels. */
export const EDGE_ARROW_BASE_SCREEN_SIZE = 8;
/** Keeps arrow direction readable at the minimum zoom without dominating the scene. */
export const EDGE_ARROW_MIN_SCREEN_SIZE = 4;
/** Prevents arrowheads from becoming oversized at extreme close zoom. */
export const EDGE_ARROW_MAX_SCREEN_SIZE = 14;

/** World-graph edge geometry scales from its smaller endpoint radius. */
export const EDGE_STROKE_RADIUS_RATIO = 0.12;
export const EDGE_HALO_STROKE_MULTIPLIER = 2.2;
export const EDGE_SELECTED_STROKE_MULTIPLIER = 1.32;
/** Edges below this rendered stroke width are hidden as zoomed-out clutter. */
export const EDGE_MIN_SCREEN_STROKE_WIDTH = 0.65;
/** Arrow marker viewport size in multiples of the referencing edge stroke. */
export const EDGE_ARROW_STROKE_MULTIPLIER = 3;

/**
 * Per-direction perpendicular offset used only when both A -> B and B -> A
 * exist. Reversing the edge reverses its local normal, so the same positive
 * offset naturally places the two directed chords on opposite sides.
 */
export const EDGE_BIDIRECTIONAL_LANE_OFFSET = 10;

/** Transient top-centre messages remain fully visible for exactly 1.5s. */
export const TRANSIENT_STATUS_VISIBLE_MS = 1500;
/** Fade/blur duration after the required fully-visible interval. */
export const STATUS_DISMISS_ANIMATION_MS = 240;

/** Collapsed cards are presentation geometry with a screen-space upper bound. */
export const COLLAPSED_REGION_WIDTH = 190;
export const COLLAPSED_REGION_HEIGHT = 52;
export const REGION_NODE_HIT_SCREEN_SIZE = 16;
