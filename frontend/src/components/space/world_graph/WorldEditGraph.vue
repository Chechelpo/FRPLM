<script setup lang="ts">
import {
    computed,
    nextTick,
    onBeforeUnmount,
    onMounted,
    ref,
    shallowRef,
    watch,
} from "vue";
import type {
    Location,
    Position,
    Region,
    RegionGeometry,
    World,
} from "@/domain/World";
import {
    STATUS_DISMISS_ANIMATION_MS,
    TRANSIENT_STATUS_VISIBLE_MS,
} from "./constants";
import type {
    BackgroundTarget,
    BulkConnectionDirection,
    BulkConnectionRequest,
    BulkConnectionTopology,
    GraphEntityEditIntent,
    SelectedGraphEntity,
} from "./types";
import {useWorldGraphBackgrounds} from "./composables/useWorldGraphBackgrounds";
import {useWorldGraphData} from "./composables/useWorldGraphData";
import {useWorldGraphInteractions} from "./composables/useWorldGraphInteractions";
import {useWorldGraphViewport} from "./composables/useWorldGraphViewport";
import {useWorldGraphSearch} from "./composables/useWorldGraphSearch";
import WorldGraphSearch from "./WorldGraphSearch.vue";
import {
    circleInsideGeometry,
    edgeEntityKey,
    geometryToBounds,
    getAbsoluteLocationPosition,
    getAbsoluteRegionGeometry,
    locationEntityKey,
    normalizeWorldCanvas,
    positionInsideBounds,
    regionEntityKey,
    worldCanvasBounds,
} from "./utils/geometry";
import {searchFocusGeometry} from "./utils/search";
import {
    collapsedDisplayGeometry,
    getDescendantRegions,
    locationIsInRegionSubtree,
    regionOwnCollapseChangeBlockedByLock,
} from "./utils/graph";
import {
    clampLocationLocalPosition,
    clampOverlayPosition,
    createRootRegionGeometry,
    createSubRegionGeometry,
    edgeIsIncidentToSelectedLocation,
    makeDirectedEdgePath,
    normalizedOpacity,
    oppositeEdgeLaneOffset,
    placeContextMenu,
    preserveAspectRatio,
    regionLabelWidth,
    splitLocationLabel,
    truncateRegionLabel,
    type EdgeRenderModel,
} from "./utils/rendering";
import {publishEdgeTraversability, subscribeEdgeTraversability, synchronizeEdgeTraversability} from "./utils/edgeSync";
import {
    locationUsesExplicitRadius,
    resolvePersistedOrAutomaticRadius,
} from "./utils/locationRadius";
import {planBulkConnections} from "./utils/bulkConnections";
import {
    edgeIsReadableAtZoom,
    edgeSelectedWorldStrokeWidth,
    edgeWorldHaloWidth,
    edgeWorldStrokeWidth,
    labelIsReadable,
    locationLabelWorldFontSize,
    nodeIsRenderableAtZoom,
    normalizeLodDetail,
    regionLabelWorldFontSize,
    screenPixelsToWorldUnits,
    zoomAwareCollapsedRegionSize,
    zoomAwareLocationCreationRadius,
    zoomAwareRegionPadding,
} from "./utils/sizing";
import {
    createDefaultWorldGraphViewerSettings,
    normalizeWorldGraphViewerSettings,
    worldGraphViewerSettingBounds,
    type WorldGraphViewerSettingKey,
} from "./utils/viewerSettings";
import {WORLD_GRAPH_CONTROL_HELP_SECTIONS} from "./utils/controlHelp";

const props = withDefaults(defineProps<{
    world: World;
    exportingWorld?: boolean;
    exportError?: string | null;
}>(), {
    exportingWorld: false,
    exportError: null,
});

const emit = defineEmits<{
    (event: "back"): void;
    (event: "editWorld"): void;
    (event: "exportWorld"): void;
    (event: "closeEditing"): void;
    (event: "dismissExportError"): void;
    (event: "editRegion", region: Region, intent?: GraphEntityEditIntent): void;
    (event: "editLocation", location: Location, intent?: GraphEntityEditIntent): void;
    (event: "editConnection", first: Location, second: Location): void;
    (event: "regionDeleted", region: Region): void;
    (event: "locationDeleted", location: Location): void;
    (event: "edgeDeleted", source: Location, destination: Location): void;
    (event: "edgeCreated", source: Location, destination: Location): void;
    (event: "locationCreated", location: Location): void;
    (event: "regionCreated", region: Region): void;
}>();

const hostRef = ref<HTMLElement | null>(null);
const fileInputRef = ref<HTMLInputElement | null>(null);
const contextMenuRef = ref<HTMLElement | null>(null);
const contextSubmenuRef = ref<HTMLElement | null>(null);
const searchComponentRef = ref<{focusInput: () => void} | null>(null);
const worldRef = computed(() => props.world);
const worldId = computed(() => props.world.get("id"));
const lodSettingsOpen = ref(false);
const helpOpen = ref(false);
const viewerSettings = ref(createDefaultWorldGraphViewerSettings());
const VIEWER_SETTINGS_STORAGE_PREFIX = "world-edit-graph:viewer-settings:";
const LEGACY_LOD_STORAGE_PREFIX = "world-edit-graph:lod-detail:";

const lodDetail = computed({
    get: () => viewerSettings.value.lodDetail,
    set: (value: number) => {
        viewerSettings.value = normalizeWorldGraphViewerSettings({
            ...viewerSettings.value,
            lodDetail: value,
        });
    },
});

function restoreViewerSettings(): void {
    if (typeof window === "undefined") {
        viewerSettings.value = createDefaultWorldGraphViewerSettings();
        return;
    }
    try {
        const raw = window.localStorage.getItem(
            `${VIEWER_SETTINGS_STORAGE_PREFIX}${worldId.value}`,
        );
        if (raw !== null) {
            viewerSettings.value = normalizeWorldGraphViewerSettings(
                JSON.parse(raw),
            );
            return;
        }

        const legacyLod = window.localStorage.getItem(
            `${LEGACY_LOD_STORAGE_PREFIX}${worldId.value}`,
        );
        viewerSettings.value = normalizeWorldGraphViewerSettings(
            legacyLod === null
                ? {}
                : {lodDetail: normalizeLodDetail(Number(legacyLod))},
        );
    } catch (error) {
        console.warn("Unable to restore world graph viewer settings", error);
        viewerSettings.value = createDefaultWorldGraphViewerSettings();
    }
}

function persistViewerSettings(): void {
    if (typeof window === "undefined") return;
    try {
        window.localStorage.setItem(
            `${VIEWER_SETTINGS_STORAGE_PREFIX}${worldId.value}`,
            JSON.stringify(viewerSettings.value),
        );
    } catch (error) {
        console.warn("Unable to store world graph viewer settings", error);
    }
}

watch(worldId, restoreViewerSettings, {immediate: true});
watch(viewerSettings, persistViewerSettings);

type ViewerTuningFieldKey = Exclude<WorldGraphViewerSettingKey, "lodDetail">;
type ViewerTuningField = {
    key: ViewerTuningFieldKey;
    label: string;
    step: number;
    unit: string;
    help: string;
};
type ViewerTuningGroup = {
    label: string;
    fields: readonly ViewerTuningField[];
};

const viewerTuningGroups: readonly ViewerTuningGroup[] = [
    {
        label: "Detail culling",
        fields: [
            {key: "nodeMinRenderScreenExtent", label: "Node render cutoff", step: 0.25, unit: "px", help: "Nodes below this apparent size are eligible for LOD culling."},
            {key: "nodeLabelMinScreenFontSize", label: "Label cutoff", step: 0.5, unit: "px", help: "Labels below this apparent font size are hidden."},
        ],
    },
    {
        label: "Nodes & sizing",
        fields: [
            {key: "defaultRegionScreenWidth", label: "New region width", step: 8, unit: "px", help: "Target on-screen width for newly created regions."},
            {key: "defaultRegionScreenHeight", label: "New region height", step: 8, unit: "px", help: "Target on-screen height for newly created regions."},
            {key: "regionMinResizeScreenWidth", label: "Region minimum width", step: 1, unit: "px", help: "Smallest on-screen width reachable by resizing at the current zoom."},
            {key: "regionMinResizeScreenHeight", label: "Region minimum height", step: 1, unit: "px", help: "Smallest on-screen height reachable by resizing at the current zoom."},
            {key: "defaultLocationScreenRadius", label: "New location radius", step: 1, unit: "px", help: "Target on-screen radius persisted for newly created locations."},
            {key: "locationMinResizeScreenRadius", label: "Location minimum radius", step: 0.5, unit: "px", help: "Smallest on-screen radius reachable by location resizing."},
            {key: "regionContentPaddingScreen", label: "Region content padding", step: 1, unit: "px", help: "Containment gutter for children inside regions."},
            {key: "worldContentPaddingScreen", label: "World content padding", step: 1, unit: "px", help: "Containment gutter for root regions inside the world."},
            {key: "collapsedRegionScreenWidth", label: "Collapsed region width", step: 2, unit: "px", help: "Maximum on-screen width of a collapsed region card."},
            {key: "collapsedRegionScreenHeight", label: "Collapsed region height", step: 2, unit: "px", help: "Maximum on-screen height of a collapsed region card."},
        ],
    },
    {
        label: "Labels",
        fields: [
            {key: "locationLabelRadiusRatio", label: "Location label / radius", step: 0.01, unit: "×", help: "Location font size as a fraction of its radius."},
            {key: "regionLabelWidthRatio", label: "Region label / width", step: 0.001, unit: "×", help: "Width-derived region label scale."},
            {key: "regionLabelHeightRatio", label: "Region label / height", step: 0.005, unit: "×", help: "Height-derived region label scale."},
            {key: "collapsedRegionLabelHeightRatio", label: "Collapsed label / height", step: 0.01, unit: "×", help: "Collapsed-region font size as a fraction of card height."},
        ],
    },
    {
        label: "Interaction",
        fields: [
            {key: "locationNodeHitScreenRadius", label: "Location hit radius", step: 1, unit: "px", help: "Minimum pointer target radius for rendered locations."},
            {key: "regionNodeHitScreenSize", label: "Region hit size", step: 1, unit: "px", help: "Minimum pointer target width/height for rendered regions."},
            {key: "resizeGestureDoublingScreenPixels", label: "Resize doubling distance", step: 4, unit: "px", help: "Radial pointer travel that doubles/halves the Shift+S resize target."},
            {key: "dragThresholdScreenPixels", label: "Drag threshold", step: 1, unit: "px", help: "Pointer travel required before a click becomes a drag."},
            {key: "reparentReleaseScreenPixels", label: "Reparent resistance", step: 1, unit: "px", help: "Distance dragged beyond containment before a parent latch releases."},
        ],
    },
    {
        label: "Edges",
        fields: [
            {key: "edgeStrokeRadiusRatio", label: "Stroke / node radius", step: 0.01, unit: "×", help: "Base edge width relative to the smaller endpoint radius."},
            {key: "edgeHaloStrokeMultiplier", label: "Selection halo", step: 0.1, unit: "×", help: "Invisible/selection halo width relative to the edge stroke."},
            {key: "edgeSelectedStrokeMultiplier", label: "Selected edge emphasis", step: 0.05, unit: "×", help: "Stroke multiplier for edges incident to selected locations."},
            {key: "edgeMinScreenStrokeWidth", label: "Edge render cutoff", step: 0.05, unit: "px", help: "Edges thinner than this apparent width are eligible for LOD culling."},
            {key: "edgeArrowStrokeMultiplier", label: "Arrowhead size", step: 0.25, unit: "×", help: "Arrow marker size relative to its edge stroke."},
            {key: "edgeBidirectionalLaneOffset", label: "Bidirectional lane offset", step: 1, unit: "wu", help: "Per-direction lane separation in world units; endpoint radius still caps it."},
        ],
    },
];

function updateViewerSettingFromInput(
    key: ViewerTuningFieldKey,
    event: Event,
): void {
    const target = event.currentTarget;
    if (!(target instanceof HTMLInputElement)) return;
    viewerSettings.value = normalizeWorldGraphViewerSettings({
        ...viewerSettings.value,
        [key]: target.valueAsNumber,
    });
}

function resetViewerSettings(): void {
    viewerSettings.value = createDefaultWorldGraphViewerSettings();
}
const worldName = computed(() => {
    const name = String(props.world.get("name") ?? "").trim();
    return name || "Unnamed world";
});
const authoredWorldCanvas = computed(() => normalizeWorldCanvas(props.world));
const interactionMessage = ref<string | null>(null);
const logMessage = ref<string | null>(null);
const backgroundMessage = ref<string | null>(null);
const altPressed = ref(false);
const collapsePendingKeys = shallowRef<ReadonlySet<string>>(new Set());
const collapseFailedKeys = shallowRef<ReadonlySet<string>>(new Set());
const lockPendingKeys = shallowRef<ReadonlySet<string>>(new Set());
const lockFailedKeys = shallowRef<ReadonlySet<string>>(new Set());

type LockableGraphEntity = {
    hasAttribute?: (name: string) => boolean;
    get(field: "locked"): boolean | null | undefined;
    update(field: "locked", value: boolean): Promise<boolean>;
};

function asLockable(
    entity: Location | Region,
): LockableGraphEntity {
    /*
     * `locked` is intentionally consumed ahead of the domain typing update.
     * The user will add it to LocationData/RegionData; until then this local
     * structural adapter keeps the graph source type-safe and isolated.
     */
    return entity as unknown as LockableGraphEntity;
}

function isEntityLocked(
    entity: Location | Region,
): boolean {
    const lockable = asLockable(entity);
    if (
        typeof lockable.hasAttribute === "function" &&
        !lockable.hasAttribute("locked")
    ) {
        return false;
    }
    return lockable.get("locked") === true;
}

const data = useWorldGraphData(worldRef);
const backgrounds = useWorldGraphBackgrounds(worldRef, data.graph, (message) => {
    backgroundMessage.value = message;
});
const viewport = useWorldGraphViewport(worldId, hostRef);

const renderedRegions = computed<readonly Region[]>(() => (
    data.visibleRegions.value.filter((region) => {
        const expanded = getAbsoluteRegionGeometry(region, data.regionIndex.value);
        const display = region.get("collapsed")
            ? zoomAwareCollapsedRegionSize(
                expanded,
                viewport.zoom.value,
                viewerSettings.value.collapsedRegionScreenWidth,
                viewerSettings.value.collapsedRegionScreenHeight,
            )
            : expanded;
        return nodeIsRenderableAtZoom(
            Math.min(display.width, display.height),
            viewport.zoom.value,
            lodDetail.value,
            viewerSettings.value.nodeMinRenderScreenExtent,
        );
    })
));
const renderedRegionKeys = computed<ReadonlySet<string>>(() => new Set(
    renderedRegions.value.map(regionEntityKey),
));
const renderedLocations = computed<readonly Location[]>(() => (
    data.visibleLocations.value.filter((location) => {
        const key = locationEntityKey(location);
        const radius = resolvePersistedOrAutomaticRadius(
            location,
            data.locationDegreeIndex.value.get(key) ?? 0,
        );
        return nodeIsRenderableAtZoom(
            radius * 2,
            viewport.zoom.value,
            lodDetail.value,
            viewerSettings.value.nodeMinRenderScreenExtent,
        );
    })
));
const renderedLocationKeys = computed<ReadonlySet<string>>(() => new Set(
    renderedLocations.value.map(locationEntityKey),
));

function dismissPopovers(): void {
    contextMenu.value = null;
    contextSubmenu.value = null;
    creation.value = null;
    backgroundTarget.value = null;
    lodSettingsOpen.value = false;
    helpOpen.value = false;
}

function toggleLodSettings(): void {
    const open = !lodSettingsOpen.value;
    dismissPopovers();
    lodSettingsOpen.value = open;
}

function toggleHelp(): void {
    const open = !helpOpen.value;
    dismissPopovers();
    helpOpen.value = open;
}

function isModalOpen(): boolean {
    return confirmation.value !== null;
}

const interactions = useWorldGraphInteractions({
    world: worldRef,
    worldId,
    hostRef,
    graph: data.graph,
    worldCanvas: authoredWorldCanvas,
    regionIndex: data.regionIndex,
    regionsByParent: data.regionsByParent,
    locationDegreeIndex: data.locationDegreeIndex,
    regionsInRenderOrder: data.regionsInRenderOrder,
    visibleRegions: renderedRegions,
    visibleLocations: renderedLocations,
    visibleRegionKeys: renderedRegionKeys,
    visibleLocationKeys: renderedLocationKeys,
    viewerSettings,
    pan: viewport.pan,
    zoom: viewport.zoom,
    clientToScreen: viewport.clientToScreen,
    screenToWorld: viewport.screenToWorld,
    worldToScreen: viewport.worldToScreen,
    setPan: viewport.setPan,
    persistViewport: viewport.persistViewport,
    persistMovement: data.persistMovement,
    persistLocationRadius: data.persistLocationRadius,
    persistCanvasScale: data.persistCanvasScale,
    dismissPopovers,
    isModalOpen,
    setInteractionMessage: (message) => {
        interactionMessage.value = message;
    },
    isWorldExternallyPending: () => backgrounds.isBackgroundPending({kind: "world", world: props.world}),
    isRegionExternallyPending: (region) => (
        collapsePendingKeys.value.has(regionEntityKey(region)) ||
        lockPendingKeys.value.has(regionEntityKey(region)) ||
        backgrounds.isBackgroundPending({kind: "region", region})
    ),
    isLocationExternallyPending: (location) => (
        lockPendingKeys.value.has(locationEntityKey(location))
    ),
    isRegionLocked: isEntityLocked,
    isLocationLocked: isEntityLocked,
});

type ContextMenuTarget =
    | {kind: "world"}
    | {kind: "location"; location: Location}
    | {kind: "region"; region: Region};

type ContextMenuState = {
    target: ContextMenuTarget;
    anchor: Position;
    screen: Position;
    world: Position;
    maxHeight: number;
};

type ContextSubmenuState = {
    kind: "bulk-link";
    screen: Position;
    maxHeight: number;
};

type CreationLinkMode =
    | {kind: "automatic-neighbour"; source: Location}
    | {kind: "middle"; first: Location; second: Location}
    | null;

type CreationState = {
    kind: "root-region" | "sub-region" | "location";
    screen: Position;
    world: Position;
    region: Region | null;
    name: string;
    pending: boolean;
    locationLink: CreationLinkMode;
};

type ConfirmationState =
    | {kind: "delete-region"; region: Region}
    | {kind: "delete-location"; location: Location}
    | {kind: "delete-edge"; source: Location; destination: Location}
    | {kind: "delete-both"; first: Location; second: Location}
    | {kind: "delete-background"; target: BackgroundTarget}
    | {kind: "replace-background"; target: BackgroundTarget; file: File}
    | {kind: "delete-selection"; entities: readonly SelectedGraphEntity[]}
    | {kind: "replace-with-middle"; creation: CreationState};

const contextMenu = shallowRef<ContextMenuState | null>(null);
const contextSubmenu = shallowRef<ContextSubmenuState | null>(null);
const creation = shallowRef<CreationState | null>(null);
const confirmation = shallowRef<ConfirmationState | null>(null);
const backgroundTarget = shallowRef<BackgroundTarget | null>(null);
const confirmationPending = ref(false);
const connectionPending = ref(false);
const bulkConnectionTopology = ref<BulkConnectionTopology>("nearest-network");
const bulkConnectionDirection = ref<BulkConnectionDirection>("bidirectional");
const bulkConnectionTraversable = ref(true);
const secondaryPointer = shallowRef<{pointerId: number} | null>(null);
const lastPointerScreen = shallowRef<Position | null>(null);

const isEmpty = computed(() => data.graph.value.locations.length === 0 && data.graph.value.regions.length === 0);
const connectionPair = computed<readonly [Location, Location] | null>(() => {
    if (interactions.selectedLocations.value.length !== 2) return null;
    const first = interactions.selectedLocations.value[0];
    const second = interactions.selectedLocations.value[1];
    return first === undefined || second === undefined ? null : [first, second];
});
const connectionState = computed(() => {
    const pair = connectionPair.value;
    return pair === null ? null : data.getConnectionState(pair[0], pair[1]);
});
const connectionPairHasConflict = computed(() => {
    const pair = connectionPair.value;
    return interactions.worldScalePending.value || (
        pair !== null && pair.some((location) => interactions.isLocationPending(location))
    );
});
const selectionOnlyLocations = computed(() => (
    interactions.selectionSummary.value.total > 0 &&
    interactions.selectionSummary.value.total === interactions.selectedLocations.value.length
));
const selectionOnlyRegions = computed(() => (
    interactions.selectionSummary.value.total > 0 &&
    interactions.selectionSummary.value.total === interactions.selectedRegions.value.length
));
const singleLocationSelection = computed(() => (
    selectionOnlyLocations.value && interactions.selectedLocations.value.length === 1
));
const twoLocationSelection = computed(() => (
    selectionOnlyLocations.value && interactions.selectedLocations.value.length === 2
));
const singleRegionSelection = computed(() => (
    selectionOnlyRegions.value && interactions.selectedRegions.value.length === 1
));
const multiRegionSelection = computed(() => (
    selectionOnlyRegions.value && interactions.selectedRegions.value.length > 1
));
const selectedSingleLocation = computed<Location | null>(() => (
    singleLocationSelection.value
        ? interactions.selectedLocations.value[0] ?? null
        : null
));
const selectedSingleRegion = computed<Region | null>(() => (
    singleRegionSelection.value
        ? interactions.selectedRegions.value[0] ?? null
        : null
));
const selectedSingleLocationLocked = computed(() => (
    selectedSingleLocation.value !== null &&
    isEntityLocked(selectedSingleLocation.value)
));
const selectedSingleRegionLocked = computed(() => (
    selectedSingleRegion.value !== null &&
    isEntityLocked(selectedSingleRegion.value)
));
const selectedAllLocked = computed(() => (
    interactions.selectedEntities.value.length > 0 &&
    interactions.selectedEntities.value.every(entity => (
        isEntityLocked(
            entity.kind === "location"
                ? entity.location
                : entity.region,
        )
    ))
));
const selectedLockPending = computed(() => (
    interactions.selectedEntities.value.some(entity => (
        lockPendingKeys.value.has(graphEntityKey(entity))
    ))
));
const selectedLockActionLabel = computed(() => {
    const action = selectedAllLocked.value
        ? "Unlock"
        : "Lock";
    const total = interactions.selectionSummary.value.total;
    if (total === interactions.selectedLocations.value.length) {
        return `${action} selected locations`;
    }
    if (total === interactions.selectedRegions.value.length) {
        return `${action} selected regions`;
    }
    return `${action} selected nodes`;
});
const selectedRegionsCollapseLabel = computed(() => {
    const regions = interactions.selectedRegions.value;
    if (regions.length === 0) return "Collapse regions";
    return regions.every(region => region.get("collapsed"))
        ? "Expand regions"
        : "Collapse regions";
});
const canBulkConnect = computed(() => (
    interactions.selectedLocations.value.length >= 2
));
const bulkConnectionHasConflict = computed(() => (
    interactions.worldScalePending.value ||
    interactions.selectedLocations.value.some((location) => interactions.isLocationPending(location))
));
const worldBounds = computed(() => worldCanvasBounds(interactions.displayedWorldCanvas.value));
const statusDismissing = ref(false);
let statusDismissDelay: ReturnType<typeof setTimeout> | null = null;
let statusDismissRemoval: ReturnType<typeof setTimeout> | null = null;

function cancelStatusDismissTimers(): void {
    if (statusDismissDelay !== null) {
        clearTimeout(statusDismissDelay);
        statusDismissDelay = null;
    }
    if (statusDismissRemoval !== null) {
        clearTimeout(statusDismissRemoval);
        statusDismissRemoval = null;
    }
}

const worldRect = computed(() => ({
    x: worldBounds.value.left,
    y: worldBounds.value.top,
    width: interactions.displayedWorldCanvas.value.width,
    height: interactions.displayedWorldCanvas.value.height,
}));
const selectedLocationIds = computed<ReadonlySet<number>>(() => new Set(
    interactions.selectedLocations.value.map((location) => location.get("id")),
));

const edgeModels = computed<readonly EdgeRenderModel[]>(() => {
    const locationIndex = new Map(data.graph.value.locations.map((location) => [location.get("id"), location]));
    const edgeKeys = new Set(data.graph.value.edges.map(edgeEntityKey));
    const result: EdgeRenderModel[] = [];
    for (const edge of data.graph.value.edges) {
        const source = locationIndex.get(edge.get("from_id"));
        const destination = locationIndex.get(edge.get("to_id"));
        if (source === undefined || destination === undefined) continue;
        if (!renderedLocationKeys.value.has(locationEntityKey(source)) || !renderedLocationKeys.value.has(locationEntityKey(destination))) continue;
        const sourcePosition = locationWorldPosition(source);
        const destinationPosition = locationWorldPosition(destination);
        const sourceRadius = locationCircleRadius(source);
        const destinationRadius = locationCircleRadius(destination);
        const strokeWidth = edgeWorldStrokeWidth(
            sourceRadius,
            destinationRadius,
            viewerSettings.value.edgeStrokeRadiusRatio,
        );
        if (!edgeIsReadableAtZoom(
            strokeWidth,
            viewport.zoom.value,
            lodDetail.value,
            viewerSettings.value.edgeMinScreenStrokeWidth,
        )) continue;
        result.push({
            key: edgeEntityKey(edge),
            edge,
            path: makeDirectedEdgePath(
                sourcePosition,
                destinationPosition,
                sourceRadius,
                destinationRadius,
                oppositeEdgeLaneOffset(
                    edge,
                    edgeKeys,
                    viewerSettings.value.edgeBidirectionalLaneOffset,
                ),
            ),
            traversable: edge.get("is_traversable"),
            strokeWidth,
            haloWidth: edgeWorldHaloWidth(
                strokeWidth,
                viewerSettings.value.edgeHaloStrokeMultiplier,
            ),
        });
    }
    return result;
});

function regionGeometry(region: Region): RegionGeometry {
    return getAbsoluteRegionGeometry(region, data.regionIndex.value, interactions.regionPreviews.value);
}

function regionDisplayGeometry(region: Region): RegionGeometry {
    return region.get("collapsed")
        ? collapsedDisplayGeometry(
            region,
            data.regionIndex.value,
            interactions.regionPreviews.value,
            viewport.zoom.value,
            viewerSettings.value.collapsedRegionScreenWidth,
            viewerSettings.value.collapsedRegionScreenHeight,
        )
        : regionGeometry(region);
}

function regionRect(region: Region): {x: number; y: number; width: number; height: number} {
    const geometry = regionDisplayGeometry(region);
    return {x: geometry.x - geometry.width / 2, y: geometry.y - geometry.height / 2, width: geometry.width, height: geometry.height};
}

function expandedRegionRect(region: Region): {x: number; y: number; width: number; height: number} {
    const geometry = regionGeometry(region);
    return {x: geometry.x - geometry.width / 2, y: geometry.y - geometry.height / 2, width: geometry.width, height: geometry.height};
}

function locationWorldPosition(location: Location): Position {
    return getAbsoluteLocationPosition(location, data.regionIndex.value, interactions.regionPreviews.value, interactions.locationPreviews.value);
}

function locationCircleRadius(location: Location): number {
    return interactions.effectiveLocationRadius(location);
}

function locationHitRadius(location: Location): number {
    return Math.max(
        locationCircleRadius(location),
        screenPixelsToWorldUnits(
            viewerSettings.value.locationNodeHitScreenRadius,
            viewport.zoom.value,
        ),
    );
}

function locationSelectionOffset(): number {
    return 6 / viewport.zoom.value;
}

function locationRadiusFailureOffset(): number {
    return 12 / viewport.zoom.value;
}

function locationLabelFontSize(location: Location): number {
    return locationLabelWorldFontSize(
        locationCircleRadius(location),
        viewerSettings.value.locationLabelRadiusRatio,
    );
}

function locationLabelVisible(location: Location): boolean {
    return labelIsReadable(
        locationLabelFontSize(location),
        viewport.zoom.value,
        lodDetail.value,
        viewerSettings.value.nodeLabelMinScreenFontSize,
    );
}

function locationLabelLines(location: Location): readonly string[] {
    return splitLocationLabel(
        location.get("name"),
        locationCircleRadius(location),
        locationLabelFontSize(location),
    );
}

function hierarchyCueGap(): number {
    return 15 / viewport.zoom.value;
}

function hierarchyCueFontSize(): number {
    return 10 / viewport.zoom.value;
}

function locationHierarchyCue(location: Location): string | null {
    if (!interactions.isLocationSelected(location)) return null;
    const key = locationEntityKey(location);
    if (
        interactions.activePointer.value?.mode === "move" &&
        interactions.activePointer.value.dragged &&
        interactions.candidateLocationParents.value.has(key)
    ) {
        const candidate = interactions.candidateLocationParents.value.get(key) ?? null;
        return candidate === null ? "no valid parent" : candidate.get("name");
    }
    const parentId = location.get("region_id");
    if (parentId === null) return "no parent";
    return data.regionIndex.value.get(parentId)?.get("name") ?? "no parent";
}

function regionHierarchyCue(region: Region): string | null {
    if (!interactions.isRegionSelected(region)) return null;
    const key = regionEntityKey(region);
    if (
        interactions.activePointer.value?.mode === "move" &&
        interactions.activePointer.value.dragged &&
        interactions.candidateRegionParents.value.has(key)
    ) {
        const candidate = interactions.candidateRegionParents.value.get(key) ?? null;
        if (candidate !== null) return candidate.get("name");
        return interactions.isRootCandidate(region) ? "root" : "no valid parent";
    }
    const parentId = region.get("parent_region_id");
    if (parentId === null) return "root";
    return data.regionIndex.value.get(parentId)?.get("name") ?? "no parent";
}

function regionDisplayName(region: Region): string {
    const geometry = regionDisplayGeometry(region);
    return truncateRegionLabel(
        region,
        geometry,
        regionDisplayFontSize(region),
    );
}

function regionDisplayLabelWidth(region: Region): number {
    const geometry = regionDisplayGeometry(region);
    return regionLabelWidth(
        regionDisplayName(region),
        geometry,
        regionDisplayFontSize(region),
    );
}

function regionDisplayFontSize(region: Region): number {
    return regionLabelWorldFontSize(
        regionDisplayGeometry(region),
        region.get("collapsed"),
        viewerSettings.value.regionLabelWidthRatio,
        viewerSettings.value.regionLabelHeightRatio,
        viewerSettings.value.collapsedRegionLabelHeightRatio,
    );
}

function regionLabelVisible(region: Region): boolean {
    return labelIsReadable(
        regionDisplayFontSize(region),
        viewport.zoom.value,
        lodDetail.value,
        viewerSettings.value.nodeLabelMinScreenFontSize,
    );
}

function regionLabelBackgroundRect(region: Region): {
    x: number;
    y: number;
    width: number;
    height: number;
} {
    const rect = regionRect(region);
    const fontSize = regionDisplayFontSize(region);
    const inset = fontSize * 0.7;
    return {
        x: rect.x + inset,
        y: rect.y + inset,
        width: regionDisplayLabelWidth(region),
        height: fontSize * 1.8,
    };
}

function regionLabelPosition(region: Region): Position {
    const background = regionLabelBackgroundRect(region);
    const fontSize = regionDisplayFontSize(region);
    return {
        x: background.x + fontSize * 0.7,
        y: background.y + fontSize * 1.25,
    };
}

function regionCollapseMarkPosition(region: Region): Position {
    const rect = regionRect(region);
    const fontSize = regionDisplayFontSize(region);
    return {
        x: rect.x + rect.width - fontSize * 1.35,
        y: rect.y + fontSize * 1.95,
    };
}

function regionSelectionRect(region: Region): {
    x: number;
    y: number;
    width: number;
    height: number;
} {
    const rect = regionRect(region);
    const offset = screenPixelsToWorldUnits(5, viewport.zoom.value);
    return {
        x: rect.x - offset,
        y: rect.y - offset,
        width: rect.width + offset * 2,
        height: rect.height + offset * 2,
    };
}

function regionCornerRadius(region: Region): number {
    return screenPixelsToWorldUnits(
        region.get("collapsed") ? 12 : 8,
        viewport.zoom.value,
    );
}

function failureMarkFontSize(): number {
    return screenPixelsToWorldUnits(16, viewport.zoom.value);
}

function regionFailureMarkPosition(region: Region): Position {
    const rect = regionRect(region);
    return {
        x: rect.x + rect.width - screenPixelsToWorldUnits(16, viewport.zoom.value),
        y: rect.y + screenPixelsToWorldUnits(24, viewport.zoom.value),
    };
}

function locationFailureMarkPosition(location: Location): Position {
    return {
        x: locationCircleRadius(location) - screenPixelsToWorldUnits(4, viewport.zoom.value),
        y: -locationCircleRadius(location) + screenPixelsToWorldUnits(8, viewport.zoom.value),
    };
}

function regionInteractionRect(region: Region): {
    x: number;
    y: number;
    width: number;
    height: number;
} {
    const geometry = regionDisplayGeometry(region);
    const minimum = screenPixelsToWorldUnits(
        viewerSettings.value.regionNodeHitScreenSize,
        viewport.zoom.value,
    );
    const width = Math.max(geometry.width, minimum);
    const height = Math.max(geometry.height, minimum);
    return {
        x: geometry.x - width / 2,
        y: geometry.y - height / 2,
        width,
        height,
    };
}

function regionClipId(region: Region): string {
    return `world-graph-region-clip-${worldId.value}-${region.get("id")}`;
}

function regionClass(region: Region): Record<string, boolean> {
    const key = regionEntityKey(region);
    return {
        "is-selected": interactions.isRegionSelected(region),
        "is-preview-selected": interactions.isRegionPreviewSelected(region),
        "is-moving": interactions.isEntityMoving(key),
        "is-scaling": interactions.isRegionScaling(region),
        "is-pending": interactions.isRegionPending(region),
        "is-failed": interactions.isRegionFailed(region) || collapseFailedKeys.value.has(key) || lockFailedKeys.value.has(key) || backgrounds.isBackgroundFailed({kind: "region", region}),
        "is-locked": isEntityLocked(region),
        "is-candidate": interactions.isRegionCandidate(region),
        "is-root-candidate": interactions.isRootCandidate(region),
        "is-invalid": interactions.isRegionInvalid(region),
        "is-collapsed": region.get("collapsed"),
    };
}

function locationClass(location: Location): Record<string, boolean> {
    const key = locationEntityKey(location);
    return {
        "is-selected": interactions.isLocationSelected(location),
        "is-preview-selected": interactions.isLocationPreviewSelected(location),
        "is-moving": interactions.isEntityMoving(key),
        "is-pending": interactions.isLocationPending(location),
        "is-failed": interactions.isLocationFailed(location) || lockFailedKeys.value.has(key),
        "is-locked": isEntityLocked(location),
        "is-radius-failed": interactions.isLocationRadiusFailed(location),
        "is-radius-pending": interactions.isLocationRadiusPending(location),
        "is-radius-resizing": interactions.isLocationRadiusResizing(location),
        "is-invalid": interactions.isLocationInvalid(location),
    };
}

function regionHasPendingLocationOperation(region: Region): boolean {
    return data.graph.value.locations.some((location) => (
        interactions.isLocationPending(location) &&
        locationIsInRegionSubtree(location, region, data.regionIndex.value)
    ));
}

function regionHasConflictingOperation(region: Region): boolean {
    return interactions.isRegionPending(region) ||
        regionHasPendingLocationOperation(region);
}

function edgeClass(model: EdgeRenderModel): Record<string, boolean> {
    return {
        "is-traversable": model.traversable,
        "is-blocked": !model.traversable,
        "is-selected-incident": edgeIsIncidentToSelectedLocation(
            model.edge.get("from_id"),
            model.edge.get("to_id"),
            selectedLocationIds.value,
        ),
    };
}

function edgeMarker(model: EdgeRenderModel): string {
    if (edgeClass(model)["is-selected-incident"]) {
        return "url(#world-graph-arrow-selected)";
    }
    return model.traversable
        ? "url(#world-graph-arrow-traversable)"
        : "url(#world-graph-arrow-blocked)";
}

function edgeRenderStrokeWidth(model: EdgeRenderModel): number {
    return edgeClass(model)["is-selected-incident"]
        ? edgeSelectedWorldStrokeWidth(
            model.strokeWidth,
            viewerSettings.value.edgeSelectedStrokeMultiplier,
        )
        : model.strokeWidth;
}

function edgeRenderHaloWidth(model: EdgeRenderModel): number {
    return edgeClass(model)["is-selected-incident"]
        ? edgeSelectedWorldStrokeWidth(
            model.haloWidth,
            viewerSettings.value.edgeSelectedStrokeMultiplier,
        )
        : model.haloWidth;
}

function edgePathStyle(
    model: EdgeRenderModel,
    halo: boolean,
): Record<string, string> {
    const width = halo
        ? edgeRenderHaloWidth(model)
        : edgeRenderStrokeWidth(model);
    return {
        strokeWidth: `${width}px`,
        strokeDasharray: model.traversable
            ? "none"
            : `${model.strokeWidth * 2.8}px ${model.strokeWidth * 1.9}px`,
    };
}

function worldClass(): Record<string, boolean> {
    return {
        "is-scaling": interactions.isWorldScaling(),
        "is-pending": interactions.worldScalePending.value || backgrounds.isBackgroundPending({kind: "world", world: props.world}),
        "is-failed": interactions.worldScaleFailed.value || backgrounds.isBackgroundFailed({kind: "world", world: props.world}),
    };
}

function contextPosition(screen: Position, panelWidth: number, panelHeight: number): Position {
    return clampOverlayPosition(screen, viewport.hostSize.width, viewport.hostSize.height, panelWidth, panelHeight);
}

async function repositionContextMenu(state: ContextMenuState): Promise<void> {
    await nextTick();
    if (contextMenu.value !== state) return;
    const menu = contextMenuRef.value;
    if (menu === null) return;
    menu.style.maxHeight = `${Math.max(1, viewport.hostSize.height - 20)}px`;
    const bounds = menu.getBoundingClientRect();
    const placement = placeContextMenu(
        state.anchor,
        viewport.hostSize.width,
        viewport.hostSize.height,
        bounds.width,
        bounds.height,
    );
    if (contextMenu.value !== state) return;
    contextMenu.value = {
        ...state,
        screen: placement.position,
        maxHeight: placement.maxHeight,
    };

    await nextTick();
    contextMenuRef.value
        ?.querySelector<HTMLButtonElement>("button:not(:disabled)")
        ?.focus();
}

function openContextMenu(clientX: number, clientY: number, target: ContextMenuTarget): void {
    data.clearActionError();
    interactionMessage.value = null;
    backgroundMessage.value = null;
    if (target.kind === "location") interactions.prepareEntityContext({kind: "location", location: target.location});
    if (target.kind === "region") interactions.prepareEntityContext({kind: "region", region: target.region});
    const rawScreen = viewport.clientToScreen(clientX, clientY);
    const state: ContextMenuState = {
        target,
        anchor: rawScreen,
        screen: {x: rawScreen.x + 8, y: rawScreen.y + 8},
        world: viewport.screenToWorld(rawScreen),
        maxHeight: Math.max(1, viewport.hostSize.height - 20),
    };
    contextMenu.value = state;
    contextSubmenu.value = null;
    creation.value = null;
    void repositionContextMenu(state);
}

async function openBulkLinkSubmenu(): Promise<void> {
    const menu = contextMenuRef.value;
    const state = contextMenu.value;
    if (menu === null || state === null || !canBulkConnect.value) return;

    const menuRect = menu.getBoundingClientRect();
    const hostRect = hostRef.value?.getBoundingClientRect();
    if (hostRect === undefined) return;

    const width = Math.min(300, Math.max(1, viewport.hostSize.width - 20));
    const gap = 8;
    const menuLeft = menuRect.left - hostRect.left;
    const menuRight = menuRect.right - hostRect.left;
    const fitsRight = menuRight + gap + width <= viewport.hostSize.width - 10;
    const x = fitsRight
        ? menuRight + gap
        : Math.max(10, menuLeft - gap - width);
    const y = Math.max(10, Math.min(
        state.screen.y,
        viewport.hostSize.height - 10,
    ));

    contextSubmenu.value = {
        kind: "bulk-link",
        screen: {x, y},
        maxHeight: Math.max(1, viewport.hostSize.height - 20),
    };

    await nextTick();
    contextSubmenuRef.value
        ?.querySelector<HTMLButtonElement>("button:not(:disabled)")
        ?.focus();
}

function openContextMenuForSelection(): void {
    const entity = interactions.selectedEntities.value[0];
    const host = hostRef.value;
    if (entity === undefined || host === null) return;

    const worldPosition = entity.kind === "location"
        ? locationWorldPosition(entity.location)
        : {
            x: regionDisplayGeometry(entity.region).x,
            y: regionDisplayGeometry(entity.region).y,
        };
    const screen = viewport.worldToScreen(worldPosition);
    const hostBounds = host.getBoundingClientRect();

    openContextMenu(
        hostBounds.left + screen.x,
        hostBounds.top + screen.y,
        entity.kind === "location"
            ? {kind: "location", location: entity.location}
            : {kind: "region", region: entity.region},
    );
}

async function focusCreationInput(): Promise<void> {
    await nextTick();
    hostRef.value?.querySelector<HTMLInputElement>("[data-creation-input]")?.focus();
}

function startCreation(kind: CreationState["kind"], region: Region | null): void {
    if (interactions.worldScalePending.value || (region !== null && interactions.isRegionPending(region))) return;
    const menu = contextMenu.value;
    if (menu === null) return;
    creation.value = {
        kind,
        screen: contextPosition(menu.screen, 320, 170),
        world: menu.world,
        region,
        name: "",
        pending: false,
        locationLink: null,
    };
    contextMenu.value = null;
    contextSubmenu.value = null;
    void focusCreationInput();
}

function openLocationCreation(
    world: Position,
    region: Region,
    link: CreationLinkMode,
): void {
    const screen = viewport.worldToScreen(world);
    creation.value = {
        kind: "location",
        screen: contextPosition(screen, 320, 170),
        world,
        region,
        name: "",
        pending: false,
        locationLink: link,
    };
    contextMenu.value = null;
    contextSubmenu.value = null;
    void focusCreationInput();
}

function findExpandedCreationRegion(
    world: Position,
): Region | null {
    const radius = zoomAwareLocationCreationRadius(
        viewport.zoom.value,
        viewerSettings.value.defaultLocationScreenRadius,
    );
    const padding = zoomAwareRegionPadding(
        viewport.zoom.value,
        viewerSettings.value.regionContentPaddingScreen,
    );
    for (const region of data.regionsDeepestFirst.value) {
        if (
            region.get("collapsed") ||
            !renderedRegionKeys.value.has(regionEntityKey(region))
        ) {
            continue;
        }

        if (circleInsideGeometry(
            world,
            radius,
            regionGeometry(region),
            padding,
        )) {
            return region;
        }
    }

    return null;
}

function startAutomaticNeighbourCreation(
    source: Location,
): void {
    const parentId = source.get("region_id");
    const parent = parentId === null
        ? null
        : data.regionIndex.value.get(parentId) ?? null;

    if (parent === null || parent.get("collapsed")) {
        interactionMessage.value =
            "The selected location has no expanded parent region for the new linked location.";
        return;
    }

    const parentGeometry = regionGeometry(parent);
    const sourceWorld = locationWorldPosition(source);
    const radius = zoomAwareLocationCreationRadius(
        viewport.zoom.value,
        viewerSettings.value.defaultLocationScreenRadius,
    );
    const padding = zoomAwareRegionPadding(
        viewport.zoom.value,
        viewerSettings.value.regionContentPaddingScreen,
    );
    const preferredWorld = {
        x: sourceWorld.x +
            locationCircleRadius(source) +
            radius +
            padding,
        y: sourceWorld.y,
    };
    const preferredLocal = {
        x: preferredWorld.x - parentGeometry.x,
        y: preferredWorld.y - parentGeometry.y,
    };
    const local = clampLocationLocalPosition(
        preferredLocal,
        radius,
        parentGeometry,
        padding,
    );

    openLocationCreation(
        {
            x: parentGeometry.x + local.x,
            y: parentGeometry.y + local.y,
        },
        parent,
        {kind: "automatic-neighbour", source},
    );
}

function startMiddleLocationCreation(
    first: Location,
    second: Location,
): void {
    const firstPosition = locationWorldPosition(first);
    const secondPosition = locationWorldPosition(second);
    const middle = {
        x: (firstPosition.x + secondPosition.x) / 2,
        y: (firstPosition.y + secondPosition.y) / 2,
    };
    const parent = findExpandedCreationRegion(middle);

    if (parent === null) {
        interactionMessage.value =
            "The midpoint does not fit inside an expanded region, so a middle location cannot be created there.";
        contextMenu.value = null;
        contextSubmenu.value = null;
        return;
    }

    openLocationCreation(
        middle,
        parent,
        {kind: "middle", first, second},
    );
}

function startCursorLocationCreation(
    region: Region,
): void {
    if (region.get("collapsed")) {
        interactionMessage.value =
            "Expand the region before creating a location inside it.";
        return;
    }

    const pointerScreen = lastPointerScreen.value;
    if (pointerScreen === null) {
        interactionMessage.value =
            "Move the pointer over the target region before using A to create a location.";
        return;
    }

    const world = viewport.screenToWorld(pointerScreen);
    const radius = zoomAwareLocationCreationRadius(
        viewport.zoom.value,
        viewerSettings.value.defaultLocationScreenRadius,
    );
    if (!circleInsideGeometry(
        world,
        radius,
        regionGeometry(region),
        zoomAwareRegionPadding(
            viewport.zoom.value,
            viewerSettings.value.regionContentPaddingScreen,
        ),
    )) {
        interactionMessage.value =
            "The pointer is outside the usable bounds of the selected region.";
        return;
    }

    openLocationCreation(
        world,
        region,
        null,
    );
}

function creationTitle(state: CreationState): string {
    if (state.kind === "root-region") return "Create root region";
    if (state.kind === "sub-region") return "Create sub-region";
    if (state.locationLink?.kind === "middle") return "Create middle location";
    if (state.locationLink?.kind === "automatic-neighbour") return "Create linked location";
    return "Create location";
}

async function createLocationFromState(
    state: CreationState,
    name: string,
): Promise<Location | null> {
    const parent = state.region;
    if (parent === null || parent.get("collapsed")) return null;

    const parentGeometry = regionGeometry(parent);
    const local = {
        x: state.world.x - parentGeometry.x,
        y: state.world.y - parentGeometry.y,
    };
    const radius = zoomAwareLocationCreationRadius(
        viewport.zoom.value,
        viewerSettings.value.defaultLocationScreenRadius,
    );
    const padding = zoomAwareRegionPadding(
        viewport.zoom.value,
        viewerSettings.value.regionContentPaddingScreen,
    );
    const created = await data.createLocation(
        parent,
        name,
        clampLocationLocalPosition(
            local,
            radius,
            parentGeometry,
            padding,
        ),
    );

    if (created === null) return null;

    const radiusPersisted = await data.persistLocationRadius(
        created,
        radius,
    );
    if (!radiusPersisted.ok) {
        interactionMessage.value =
            "The location was created, but its zoom-aware size could not be saved.";
    }

    emit("locationCreated", created);

    const link = state.locationLink;
    if (link === null) return created;

    const requests: BulkConnectionRequest[] =
        link.kind === "automatic-neighbour"
            ? [
                {source: link.source, destination: created},
                {source: created, destination: link.source},
            ]
            : [
                {source: link.first, destination: created},
                {source: created, destination: link.first},
                {source: created, destination: link.second},
                {source: link.second, destination: created},
            ];

    const result = await data.createBulkConnections(
        requests,
        true,
    );

    /* These are entity-creation workflows, not the no-emit batch-link UI. */
    for (const request of result.created) {
        emit(
            "edgeCreated",
            request.source,
            request.destination,
        );
    }

    return created;
}

async function submitCreation(): Promise<void> {
    const state = creation.value;
    if (state === null || state.pending) return;
    const name = state.name.trim();
    if (name.length === 0) {
        interactionMessage.value = "Enter a name before creating the entity.";
        return;
    }
    state.pending = true;
    data.clearActionError();
    interactionMessage.value = null;
    try {
        if (state.kind === "root-region") {
            const created = await data.createRootRegion(
                name,
                createRootRegionGeometry(
                    state.world,
                    interactions.displayedWorldCanvas.value,
                    viewport.zoom.value,
                    viewerSettings.value.defaultRegionScreenWidth,
                    viewerSettings.value.defaultRegionScreenHeight,
                    viewerSettings.value.worldContentPaddingScreen,
                ),
            );
            if (created !== null) {
                emit("regionCreated", created);
                creation.value = null;
            }
            return;
        }
        const parent = state.region;
        if (parent === null || parent.get("collapsed")) return;
        const parentGeometry = regionGeometry(parent);
        const local = {x: state.world.x - parentGeometry.x, y: state.world.y - parentGeometry.y};
        if (state.kind === "sub-region") {
            const created = await data.createSubRegion(
                parent,
                name,
                createSubRegionGeometry(
                    local,
                    parentGeometry,
                    viewport.zoom.value,
                    viewerSettings.value.defaultRegionScreenWidth,
                    viewerSettings.value.defaultRegionScreenHeight,
                    viewerSettings.value.regionContentPaddingScreen,
                ),
            );
            if (created !== null) {
                emit("regionCreated", created);
                creation.value = null;
            }
            return;
        }

        if (state.locationLink?.kind === "middle") {
            const connection = data.getConnectionState(
                state.locationLink.first,
                state.locationLink.second,
            );
            if (
                connection.forward !== null ||
                connection.reverse !== null
            ) {
                state.pending = false;
                creation.value = null;
                confirmation.value = {
                    kind: "replace-with-middle",
                    creation: state,
                };
                return;
            }
        }

        const created = await createLocationFromState(
            state,
            name,
        );
        if (created !== null) {
            creation.value = null;
        }
    } finally {
        if (creation.value === state) state.pending = false;
    }
}

function editRegion(
    region: Region,
    intent: GraphEntityEditIntent = "open",
): void {
    contextMenu.value = null;
    emit("editRegion", region, intent);
}

function editWorld(): void {
    contextMenu.value = null;
    emit("editWorld");
}

function editLocation(
    location: Location,
    intent: GraphEntityEditIntent = "open",
): void {
    contextMenu.value = null;
    emit("editLocation", location, intent);
}

function graphEntityKey(
    entity: SelectedGraphEntity,
): string {
    return entity.kind === "location"
        ? locationEntityKey(entity.location)
        : regionEntityKey(entity.region);
}

async function toggleEntityLock(
    entity: SelectedGraphEntity,
): Promise<void> {
    const model = entity.kind === "location" ? entity.location : entity.region;
    await setEntityLocks(
        [entity],
        !isEntityLocked(model),
    );
}

async function setEntityLocks(
    entities: readonly SelectedGraphEntity[],
    locked: boolean,
): Promise<void> {
    const targets = entities.filter(entity => {
        const model = entity.kind === "location"
            ? entity.location
            : entity.region;
        return isEntityLocked(model) !== locked;
    });
    if (targets.length === 0) {
        contextMenu.value = null;
        contextSubmenu.value = null;
        return;
    }

    const targetKeys = new Set(targets.map(graphEntityKey));
    if ([...targetKeys].some(key => lockPendingKeys.value.has(key))) return;

    const expectedWorldId = worldId.value;
    contextMenu.value = null;
    contextSubmenu.value = null;
    interactionMessage.value = null;
    logMessage.value = null;
    lockPendingKeys.value = new Set([
        ...lockPendingKeys.value,
        ...targetKeys,
    ]);
    lockFailedKeys.value = new Set(
        [...lockFailedKeys.value].filter(key => !targetKeys.has(key)),
    );

    const results = await Promise.all(targets.map(async entity => {
        const model = entity.kind === "location"
            ? entity.location
            : entity.region;
        try {
            return await asLockable(model).update(
                "locked",
                locked,
            );
        } catch (error) {
            console.error(
                "Unable to update graph node lock state",
                error,
            );
            return false;
        }
    }));

    if (worldId.value !== expectedWorldId) return;

    lockPendingKeys.value = new Set(
        [...lockPendingKeys.value].filter(key => !targetKeys.has(key)),
    );
    const failedKeys = new Set(targets
        .filter((_, index) => results[index] !== true)
        .map(graphEntityKey));

    if (failedKeys.size > 0) {
        lockFailedKeys.value = new Set([
            ...lockFailedKeys.value,
            ...failedKeys,
        ]);
        interactionMessage.value =
            `${failedKeys.size} node lock ${failedKeys.size === 1 ? "change" : "changes"} could not be saved.`;
        return;
    }

    logMessage.value = `${targets.length} ${targets.length === 1 ? "node" : "nodes"} ${locked ? "locked" : "unlocked"}.`;
}

function toggleSelectedLocationLock(): void {
    const location = selectedSingleLocation.value;
    if (location === null) return;
    void toggleEntityLock({kind: "location", location});
}

function toggleSelectedRegionLock(): void {
    const region = selectedSingleRegion.value;
    if (region === null) return;
    void toggleEntityLock({kind: "region", region});
}

function toggleSelectedLocks(): void {
    const entities = [...interactions.selectedEntities.value];
    if (entities.length === 0 || selectedLockPending.value) return;
    void setEntityLocks(
        entities,
        !selectedAllLocked.value,
    );
}

function resetLocationAutomaticSize(location: Location): void {
    contextMenu.value = null;
    void interactions.resetLocationRadius(location);
}

function editConnection(): void {
    const pair = connectionPair.value;
    if (pair === null) return;
    contextMenu.value = null;
    emit("editConnection", pair[0], pair[1]);
}

async function createOneDirection(source: Location, destination: Location): Promise<void> {
    if (connectionPending.value || connectionPairHasConflict.value) return;
    contextMenu.value = null;
    connectionPending.value = true;
    const expectedWorldId = worldId.value;
    try {
        const edge = await data.createDirectedEdge(source, destination);
        if (worldId.value !== expectedWorldId) return;
        if (edge !== null) {
            emit("edgeCreated", source, destination);
            const pair = connectionPair.value;
            if (pair !== null) emit("editConnection", pair[0], pair[1]);
        }
    } finally {
        if (worldId.value === expectedWorldId) connectionPending.value = false;
    }
}

async function createBothDirections(): Promise<void> {
    const pair = connectionPair.value;
    if (pair === null || connectionPending.value || connectionPairHasConflict.value) return;
    contextMenu.value = null;
    connectionPending.value = true;
    const expectedWorldId = worldId.value;
    try {
        const result = await data.createBidirectionalConnection(pair[0], pair[1]);
        if (worldId.value !== expectedWorldId) return;
        if (result.forward !== null) emit("edgeCreated", pair[0], pair[1]);
        if (result.reverse !== null) emit("edgeCreated", pair[1], pair[0]);
        if (result.forward !== null || result.reverse !== null) emit("editConnection", pair[0], pair[1]);
    } finally {
        if (worldId.value === expectedWorldId) connectionPending.value = false;
    }
}

async function createBulkSelectedConnections(): Promise<void> {
    if (!canBulkConnect.value || connectionPending.value || bulkConnectionHasConflict.value) return;

    const selected = [...interactions.selectedLocations.value];
    const locationByKey = new Map(selected.map((location) => [locationEntityKey(location), location]));
    const plan = planBulkConnections(
        selected.map((location) => {
            const position = locationWorldPosition(location);
            return {key: locationEntityKey(location), x: position.x, y: position.y};
        }),
        bulkConnectionTopology.value,
        bulkConnectionDirection.value,
    );
    const requests: BulkConnectionRequest[] = [];
    for (const pair of plan) {
        const source = locationByKey.get(pair.sourceKey);
        const destination = locationByKey.get(pair.destinationKey);
        if (source !== undefined && destination !== undefined) requests.push({source, destination});
    }

    contextMenu.value = null;
    contextSubmenu.value = null;
    connectionPending.value = true;
    interactionMessage.value = null;
    logMessage.value = null;
    const expectedWorldId = worldId.value;
    try {
        const result = await data.createBulkConnections(requests, bulkConnectionTraversable.value);
        if (worldId.value !== expectedWorldId) return;
        if (result.failed.length === 0) {
            logMessage.value = result.created.length === 0
                ? `No new connections were needed; ${result.skipped.length} existing directions were kept.`
                : `Created ${result.created.length} connection directions${result.skipped.length > 0 ? `; skipped ${result.skipped.length} existing directions` : ""}.`;
        }
    } finally {
        if (worldId.value === expectedWorldId) connectionPending.value = false;
    }
}

function connectionDirectionLabel(source: Location, destination: Location): string {
    return `${source.get("name")} → ${destination.get("name")}`;
}

function requestDeleteRegion(region: Region): void {
    if (regionHasConflictingOperation(region)) return;
    contextMenu.value = null;
    confirmation.value = {kind: "delete-region", region};
}

function requestDeleteLocation(location: Location): void {
    contextMenu.value = null;
    confirmation.value = {kind: "delete-location", location};
}

function requestDeleteEdge(source: Location, destination: Location): void {
    contextMenu.value = null;
    confirmation.value = {kind: "delete-edge", source, destination};
}

function requestDeleteBoth(first: Location, second: Location): void {
    contextMenu.value = null;
    confirmation.value = {kind: "delete-both", first, second};
}

function requestDeleteBackground(target: BackgroundTarget): void {
    contextMenu.value = null;
    contextSubmenu.value = null;
    confirmation.value = {kind: "delete-background", target};
}

function requestDeleteSelection(): void {
    const entities = [...interactions.selectedEntities.value];
    if (entities.length === 0) return;

    contextMenu.value = null;
    contextSubmenu.value = null;

    if (entities.length === 1) {
        const entity = entities[0];
        if (entity?.kind === "location") {
            requestDeleteLocation(entity.location);
            return;
        }
        if (entity?.kind === "region") {
            requestDeleteRegion(entity.region);
            return;
        }
    }

    confirmation.value = {
        kind: "delete-selection",
        entities,
    };
}

function requestDeleteSelectedLocations(): void {
    const entities: readonly SelectedGraphEntity[] =
        interactions.selectedLocations.value.map(location => ({
            kind: "location" as const,
            location,
        }));
    if (entities.length === 0) return;

    contextMenu.value = null;
    contextSubmenu.value = null;
    confirmation.value = {
        kind: "delete-selection",
        entities,
    };
}

function confirmationTitle(state: ConfirmationState): string {
    if (state.kind === "delete-region") return "Delete region";
    if (state.kind === "delete-location") return "Delete location";
    if (state.kind === "delete-edge") return "Delete directed connection";
    if (state.kind === "delete-both") return "Delete both directions";
    if (state.kind === "replace-background") return "Replace background";
    if (state.kind === "delete-selection") {
        const hasLocations = state.entities.some(entity => entity.kind === "location");
        const hasRegions = state.entities.some(entity => entity.kind === "region");
        if (hasLocations && !hasRegions) return "Delete selected locations";
        if (hasRegions && !hasLocations) return "Delete selected regions";
        return "Delete selected nodes";
    }
    if (state.kind === "replace-with-middle") return "Replace direct connection";
    return "Delete background";
}

function confirmationText(state: ConfirmationState): string {
    if (state.kind === "delete-region") return `Delete “${state.region.get("name")}”? Descendant regions, locations, and their connections may also be removed by the backend cascade.`;
    if (state.kind === "delete-location") return `Delete “${state.location.get("name")}” and its directed connections?`;
    if (state.kind === "delete-edge") return `Delete ${state.source.get("name")} → ${state.destination.get("name")}?`;
    if (state.kind === "delete-both") return `Delete every existing direction between ${state.first.get("name")} and ${state.second.get("name")}?`;
    if (state.kind === "replace-background") return state.target.kind === "world"
        ? "Replace the existing world background image with the selected file?"
        : `Replace the existing background image of “${state.target.region.get("name")}” with the selected file?`;
    if (state.kind === "delete-selection") {
        const locations = state.entities.filter(entity => entity.kind === "location").length;
        const regions = state.entities.filter(entity => entity.kind === "region").length;
        if (regions === 0) {
            return `Delete ${locations} selected ${locations === 1 ? "location" : "locations"} and their directed connections?`;
        }
        if (locations === 0) {
            return `Delete ${regions} selected ${regions === 1 ? "region" : "regions"}? Region deletion may also remove descendants through the backend cascade.`;
        }
        return `Delete ${locations} selected ${locations === 1 ? "location" : "locations"} and ${regions} selected ${regions === 1 ? "region" : "regions"}? Region deletion may also remove descendants through the backend cascade.`;
    }
    if (state.kind === "replace-with-middle") {
        const link = state.creation.locationLink;
        if (link?.kind !== "middle") return "Replace the direct connection with a middle location?";
        return `Delete the existing direct connection directions between “${link.first.get("name")}” and “${link.second.get("name")}”, then create “${state.creation.name.trim()}” between them with bidirectional links?`;
    }
    return state.target.kind === "world" ? "Delete the world background image? The world canvas and graph content will remain unchanged." : `Delete the background image of “${state.target.region.get("name")}”? The region canvas and contained content will remain unchanged.`;
}

async function confirmDeletion(): Promise<void> {
    const state = confirmation.value;
    if (state === null || confirmationPending.value) return;
    confirmationPending.value = true;
    const expectedWorldId = worldId.value;
    try {
        if (state.kind === "delete-region") {
            const deleted = await data.deleteRegion(state.region);
            if (worldId.value === expectedWorldId && deleted) {
                interactions.removeEntityFromSelection({kind: "region", region: state.region});
                emit("regionDeleted", state.region);
            }
        } else if (state.kind === "delete-location") {
            const deleted = await data.deleteLocation(state.location);
            if (worldId.value === expectedWorldId && deleted) {
                interactions.removeEntityFromSelection({kind: "location", location: state.location});
                emit("locationDeleted", state.location);
            }
        } else if (state.kind === "delete-edge") {
            const deleted = await data.deleteDirectedEdge(state.source, state.destination);
            if (worldId.value === expectedWorldId && deleted) emit("edgeDeleted", state.source, state.destination);
        } else if (state.kind === "delete-both") {
            const before = data.getConnectionState(state.first, state.second);
            const result = await data.deleteBidirectionalConnection(state.first, state.second);
            if (worldId.value === expectedWorldId) {
                if (before.forward !== null && result.forwardDeleted) emit("edgeDeleted", state.first, state.second);
                if (before.reverse !== null && result.reverseDeleted) emit("edgeDeleted", state.second, state.first);
            }
        } else if (state.kind === "replace-background") {
            await backgrounds.setBackground(
                state.target,
                state.file,
            );
        } else if (state.kind === "delete-selection") {
            const locations = state.entities
                .filter((entity): entity is Extract<SelectedGraphEntity, {kind: "location"}> => entity.kind === "location")
                .map(entity => entity.location);
            const selectedRegionKeys = new Set(
                state.entities
                    .filter((entity): entity is Extract<SelectedGraphEntity, {kind: "region"}> => entity.kind === "region")
                    .map(entity => regionEntityKey(entity.region)),
            );
            const regions = data.regionsDeepestFirst.value.filter(region => (
                selectedRegionKeys.has(regionEntityKey(region))
            ));

            for (const location of locations) {
                const deleted = await data.deleteLocation(location);
                if (worldId.value !== expectedWorldId) return;
                if (deleted) {
                    interactions.removeEntityFromSelection({kind: "location", location});
                    emit("locationDeleted", location);
                }
            }

            for (const region of regions) {
                const deleted = await data.deleteRegion(region);
                if (worldId.value !== expectedWorldId) return;
                if (deleted) {
                    interactions.removeEntityFromSelection({kind: "region", region});
                    emit("regionDeleted", region);
                }
            }
        } else if (state.kind === "replace-with-middle") {
            const link = state.creation.locationLink;
            if (link?.kind !== "middle") return;

            const before = data.getConnectionState(
                link.first,
                link.second,
            );
            const result = await data.deleteBidirectionalConnection(
                link.first,
                link.second,
            );

            if (worldId.value !== expectedWorldId) return;

            if (before.forward !== null && result.forwardDeleted) {
                emit("edgeDeleted", link.first, link.second);
            }
            if (before.reverse !== null && result.reverseDeleted) {
                emit("edgeDeleted", link.second, link.first);
            }

            const deletionFailed =
                (before.forward !== null && !result.forwardDeleted) ||
                (before.reverse !== null && !result.reverseDeleted);

            if (deletionFailed) return;

            await createLocationFromState(
                state.creation,
                state.creation.name.trim(),
            );
        } else {
            await backgrounds.deleteBackground(state.target);
        }
    } finally {
        if (worldId.value === expectedWorldId && confirmation.value === state) {
            confirmationPending.value = false;
            confirmation.value = null;
        }
    }
}

async function setRegionCollapseState(
    region: Region,
    collapsed: boolean,
): Promise<boolean> {
    if (region.get("collapsed") === collapsed) return true;

    const lockBlocksChange = regionOwnCollapseChangeBlockedByLock(
        region,
        collapsed,
        isEntityLocked,
    );

    if (lockBlocksChange) {
        interactionMessage.value =
            "Unlock the region before changing its collapse state directly.";
        return false;
    }

    const key = regionEntityKey(region);
    if (collapsePendingKeys.value.has(key) || regionHasConflictingOperation(region)) return false;
    contextMenu.value = null;
    collapsePendingKeys.value = new Set([...collapsePendingKeys.value, key]);
    collapseFailedKeys.value = new Set([...collapseFailedKeys.value].filter((item) => item !== key));
    const expectedWorldId = worldId.value;

    try {
        const saved = await data.setRegionCollapsed(region, collapsed);
        if (worldId.value !== expectedWorldId) return false;
        if (!saved) collapseFailedKeys.value = new Set([...collapseFailedKeys.value, key]);
        if (saved && collapsed) {
            const descendants = new Set(getDescendantRegions(region, data.regionsByParent.value).map(regionEntityKey));
            const hiddenLocations = new Set(data.graph.value.locations.filter((location) => locationIsInRegionSubtree(location, region, data.regionIndex.value)).map(locationEntityKey));
            interactions.pruneSelection(new Set([...interactions.selectedKeys.value].filter((selected) => !descendants.has(selected) && !hiddenLocations.has(selected))));
            contextMenu.value = null;
            creation.value = null;
        }
        return saved;
    } finally {
        if (worldId.value === expectedWorldId) collapsePendingKeys.value = new Set([...collapsePendingKeys.value].filter((item) => item !== key));
    }
}

async function toggleRegionCollapse(region: Region): Promise<void> {
    await setRegionCollapseState(region, !region.get("collapsed"));
}

async function toggleSelectedRegionsCollapse(): Promise<void> {
    const regions = [...interactions.selectedRegions.value];
    if (regions.length === 0) return;

    const allCollapsed = regions.every(region => region.get("collapsed"));
    const allExpanded = regions.every(region => !region.get("collapsed"));
    const nextCollapsed = allCollapsed
        ? false
        : allExpanded
            ? true
            : true;

    contextMenu.value = null;
    contextSubmenu.value = null;
    const ordered = nextCollapsed
        ? data.regionsDeepestFirst.value.filter(candidate => (
            regions.some(region => region.equals(candidate))
        ))
        : regions;
    let skippedLocked = 0;

    for (const region of ordered) {
        if (regionOwnCollapseChangeBlockedByLock(
            region,
            nextCollapsed,
            isEntityLocked,
        )) {
            skippedLocked += 1;
            continue;
        }
        await setRegionCollapseState(
            region,
            nextCollapsed,
        );
    }

    if (skippedLocked > 0) {
        logMessage.value = `${skippedLocked} locked selected ${skippedLocked === 1 ? "region was" : "regions were"} skipped as a direct collapse-state target; ancestor effects still apply.`;
    }
}

function replaceSearchSelection(
    entities: readonly SelectedGraphEntity[],
): number {
    interactions.clearSelection();
    for (const entity of entities) interactions.toggle(entity);
    return interactions.selectionSummary.value.total;
}

function focusSearchEntities(
    entities: readonly SelectedGraphEntity[],
): void {
    const single = entities.length === 1 ? entities[0] : undefined;
    const minimumExtent = single?.kind === "location"
        ? locationCircleRadius(single.location) * 8
        : 0;
    const geometry = searchFocusGeometry(entities, {
        locationGeometry: (location) => {
            const center = locationWorldPosition(location);
            const radius = locationCircleRadius(location);
            return {
                x: center.x,
                y: center.y,
                width: radius * 2,
                height: radius * 2,
            };
        },
        regionGeometry: regionDisplayGeometry,
        minimumExtent,
    });

    if (geometry !== null) viewport.fitGeometry(geometry, 56, true);
}

const search = useWorldGraphSearch({
    worldId,
    graph: data.graph,
    regionIndex: data.regionIndex,
    setRegionCollapsed: (region, collapsed) => (
        setRegionCollapseState(region, collapsed)
    ),
    replaceSelection: replaceSearchSelection,
    focusEntities: focusSearchEntities,
    dismissPopovers,
});

const statusMessage = computed<string | null>(() => (
    props.exportError ??
    interactionMessage.value ??
    backgroundMessage.value ??
    data.actionError.value ??
    data.loadError.value ??
    logMessage.value ??
    search.message.value
));

const statusKind = computed<"error" | "log">(() => {
    if (
        props.exportError !== null ||
        interactionMessage.value !== null ||
        backgroundMessage.value !== null ||
        data.actionError.value !== null ||
        data.loadError.value !== null
    ) {
        return "error";
    }

    return search.message.value !== null &&
        search.messageKind.value === "error"
        ? "error"
        : "log";
});

const statusIsPersistentLoadError = computed(() => (
    data.loadError.value !== null &&
    props.exportError === null &&
    interactionMessage.value === null &&
    backgroundMessage.value === null &&
    data.actionError.value === null
));

function requestSetBackground(target: BackgroundTarget): void {
    contextMenu.value = null;
    backgroundTarget.value = target;
    const input = fileInputRef.value;
    if (input === null) return;
    input.value = "";
    input.click();
}

async function onBackgroundFileChange(event: Event): Promise<void> {
    const input = event.currentTarget as HTMLInputElement;
    const target = backgroundTarget.value;
    const file = input.files?.[0] ?? null;
    backgroundTarget.value = null;
    input.value = "";
    if (target === null || file === null) return;

    const replacingExisting = target.kind === "world"
        ? backgrounds.hasWorldBackground.value
        : backgrounds.hasRegionBackground(target.region);

    if (replacingExisting) {
        confirmation.value = {
            kind: "replace-background",
            target,
            file,
        };
        return;
    }

    await backgrounds.setBackground(target, file);
}

function clearGraphSelection(): void {
    interactions.clearSelection();
    contextMenu.value = null;
}

function updateSearchQuery(value: string): void {
    search.query.value = value;
}

function zoomIn(): void {
    dismissPopovers();
    viewport.zoomBy(1.2);
}

function zoomOut(): void {
    dismissPopovers();
    viewport.zoomBy(1 / 1.2);
}

function resetGraphViewport(): void {
    dismissPopovers();
    viewport.resetViewport();
}

function dismissStatus(): void {
    if (props.exportError !== null) emit("dismissExportError");
    interactionMessage.value = null;
    logMessage.value = null;
    backgroundMessage.value = null;
    data.clearActionError();
    search.clearMessage();
}

function isGraphOverlayTarget(target: EventTarget | null): boolean {
    return target instanceof Element && target.closest("[data-graph-popover], [data-graph-modal], [data-graph-control]") !== null;
}

function contextTargetAt(clientX: number, clientY: number): ContextMenuTarget | null {
    const element = document.elementFromPoint(clientX, clientY);
    const entityElement = element?.closest<HTMLElement>("[data-graph-entity-key]") ?? null;
    if (entityElement !== null) {
        const key = entityElement.dataset.graphEntityKey ?? "";
        if (entityElement.dataset.graphEntityKind === "location") {
            const location = renderedLocations.value.find((candidate) => locationEntityKey(candidate) === key);
            if (location !== undefined) return {kind: "location", location};
        }
        if (entityElement.dataset.graphEntityKind === "region") {
            const region = renderedRegions.value.find((candidate) => regionEntityKey(candidate) === key);
            if (region !== undefined) return {kind: "region", region};
        }
    }
    const screen = viewport.clientToScreen(clientX, clientY);
    const worldPosition = viewport.screenToWorld(screen);
    return positionInsideBounds(worldPosition, worldCanvasBounds(interactions.displayedWorldCanvas.value)) ? {kind: "world"} : null;
}

function handleHostPointerDownCapture(event: PointerEvent): void {
    lastPointerScreen.value = viewport.clientToScreen(
        event.clientX,
        event.clientY,
    );
    if (interactions.keyboardResize.value !== null) {
        event.preventDefault();
        event.stopPropagation();
        return;
    }
    const overlayTarget = isGraphOverlayTarget(event.target);
    if (!overlayTarget) {
        lodSettingsOpen.value = false;
        helpOpen.value = false;
    }
    if (event.button === 0 && overlayTarget) return;
    if (event.button === 2) {
        if (isModalOpen() || overlayTarget) return;
        event.preventDefault();
        secondaryPointer.value = {pointerId: event.pointerId};
        try { hostRef.value?.setPointerCapture(event.pointerId); } catch { /* Host may be detaching. */ }
        return;
    }
    interactions.onHostPointerDownCapture(event);
}

function handlePointerMove(event: PointerEvent): void {
    lastPointerScreen.value = viewport.clientToScreen(
        event.clientX,
        event.clientY,
    );
    interactions.onPointerMove(event);
}

function handleWheel(event: WheelEvent): void {
    if (interactions.keyboardResize.value !== null) return;
    viewport.handleWheel(event);
}

function handlePointerUp(event: PointerEvent): void {
    const secondary = secondaryPointer.value;
    if (secondary !== null && secondary.pointerId === event.pointerId) {
        event.preventDefault();
        secondaryPointer.value = null;
        try { if (hostRef.value?.hasPointerCapture(event.pointerId)) hostRef.value.releasePointerCapture(event.pointerId); } catch { /* Browser may have released capture. */ }
        const releaseElement = document.elementFromPoint(event.clientX, event.clientY);
        if (isGraphOverlayTarget(releaseElement)) return;
        const target = contextTargetAt(event.clientX, event.clientY);
        if (target !== null) openContextMenu(event.clientX, event.clientY, target);
        return;
    }
    interactions.onPointerUp(event);
}

function handlePointerCancel(event: PointerEvent): void {
    if (secondaryPointer.value?.pointerId === event.pointerId) {
        secondaryPointer.value = null;
        try { if (hostRef.value?.hasPointerCapture(event.pointerId)) hostRef.value.releasePointerCapture(event.pointerId); } catch { /* Browser may have released capture. */ }
    }
    interactions.onPointerCancel(event);
}

function isInteractiveShortcutTarget(
    target: EventTarget | null,
): boolean {
    if (!(target instanceof HTMLElement)) return false;
    return (
        target instanceof HTMLInputElement ||
        target instanceof HTMLTextAreaElement ||
        target instanceof HTMLSelectElement ||
        target instanceof HTMLButtonElement ||
        target.isContentEditable
    );
}

function moveContextMenuFocus(
    delta: -1 | 1,
): void {
    const menu = contextSubmenu.value !== null
        ? contextSubmenuRef.value
        : contextMenuRef.value;
    if (menu === null) return;

    const options = [...menu.querySelectorAll<HTMLElement>(
        "button:not(:disabled), input[type='range']:not(:disabled)",
    )];
    if (options.length === 0) return;

    const activeIndex = options.findIndex(option => (
        option === document.activeElement
    ));
    const nextIndex = activeIndex < 0
        ? 0
        : (activeIndex + delta + options.length) % options.length;
    options[nextIndex]?.focus();
}

function handleContextMenuNavigation(
    event: KeyboardEvent,
): boolean {
    if (contextMenu.value === null) return false;

    const key = event.key.toLocaleLowerCase();
    if (key !== "arrowdown" && key !== "s" && key !== "arrowup" && key !== "w") {
        return false;
    }

    if (event.target instanceof HTMLSelectElement) {
        return false;
    }
    if (
        event.target instanceof HTMLInputElement &&
        (event.key === "ArrowDown" || event.key === "ArrowUp")
    ) return false;

    event.preventDefault();
    moveContextMenuFocus(
        key === "arrowdown" || key === "s"
            ? 1
            : -1,
    );
    return true;
}

function handleKeyDown(event: KeyboardEvent): void {
    if (event.key === "Alt") altPressed.value = true;

    if (event.key === "Escape") {
        if (confirmation.value !== null) {
            event.preventDefault();
            if (!confirmationPending.value) confirmation.value = null;
            return;
        }

        if (
            contextMenu.value !== null ||
            contextSubmenu.value !== null ||
            creation.value !== null ||
            lodSettingsOpen.value ||
            helpOpen.value
        ) {
            event.preventDefault();
            dismissPopovers();
            return;
        }

        interactions.onEscape();
        emit("closeEditing");
        return;
    }

    if (
        event.key === "Enter" &&
        confirmation.value !== null
    ) {
        event.preventDefault();
        void confirmDeletion();
        return;
    }

    if (confirmation.value !== null) return;

    const key = event.key.toLocaleLowerCase();
    const resizeChord = key === "s" && event.shiftKey &&
        !event.altKey && !event.ctrlKey && !event.metaKey &&
        !isInteractiveShortcutTarget(event.target);
    if (resizeChord) {
        event.preventDefault();
        if (!event.repeat) {
            interactions.beginKeyboardResize(lastPointerScreen.value);
        }
        return;
    }

    if (handleContextMenuNavigation(event)) return;
    if (contextMenu.value !== null) return;
    if (isInteractiveShortcutTarget(event.target)) return;
    if (event.altKey || event.ctrlKey || event.metaKey) return;

    if (key === "f") {
        event.preventDefault();
        searchComponentRef.value?.focusInput();
        return;
    }

    if (
        key === "i" &&
        interactions.selectionSummary.value.total > 0
    ) {
        event.preventDefault();
        openContextMenuForSelection();
        return;
    }

    if (
        key === "e" &&
        interactions.selectedEntities.value.length === 1 &&
        !event.repeat
    ) {
        event.preventDefault();
        const entity = interactions.selectedEntities.value[0];
        if (entity?.kind === "location") {
            editLocation(entity.location, "toggle-if-current");
        } else if (entity?.kind === "region") {
            editRegion(entity.region, "toggle-if-current");
        }
        return;
    }

    if (
        key === "d" &&
        interactions.selectionSummary.value.total > 0
    ) {
        event.preventDefault();
        requestDeleteSelection();
        return;
    }

    if (
        event.code === "Space" &&
        interactions.selectedRegions.value.length > 0 &&
        !event.repeat
    ) {
        event.preventDefault();
        void toggleSelectedRegionsCollapse();
        return;
    }

    if (key === "a" && !event.repeat) {
        if (singleLocationSelection.value) {
            const location = interactions.selectedLocations.value[0];
            if (location !== undefined) {
                event.preventDefault();
                startAutomaticNeighbourCreation(location);
            }
            return;
        }

        if (singleRegionSelection.value) {
            const region = interactions.selectedRegions.value[0];
            if (region !== undefined) {
                event.preventDefault();
                startCursorLocationCreation(region);
            }
        }
    }
}

function handleKeyUp(event: KeyboardEvent): void {
    if (event.key === "Alt") altPressed.value = false;
    const key = event.key.toLocaleLowerCase();
    if (event.key === "Shift" || key === "s") {
        if (interactions.endKeyboardResize()) event.preventDefault();
    }
}

function handleWindowBlur(): void {
    altPressed.value = false;
    interactions.cancelKeyboardResize();
}

let stopEdgeTraversabilitySync: () => void = () => undefined;

onMounted(() => {
    window.addEventListener("keydown", handleKeyDown);
    window.addEventListener("keyup", handleKeyUp);
    window.addEventListener("blur", handleWindowBlur);
    stopEdgeTraversabilitySync = subscribeEdgeTraversability((change) => {
        synchronizeEdgeTraversability(data.graph.value.edges, change);
    });
});

onBeforeUnmount(() => {
    interactions.dispose();
    stopEdgeTraversabilitySync();
    cancelStatusDismissTimers();
    window.removeEventListener("keydown", handleKeyDown);
    window.removeEventListener("keyup", handleKeyUp);
    window.removeEventListener("blur", handleWindowBlur);
});

watch(worldId, () => {
    contextMenu.value = null;
    creation.value = null;
    confirmation.value = null;
    backgroundTarget.value = null;
    lodSettingsOpen.value = false;
    helpOpen.value = false;
    confirmationPending.value = false;
    connectionPending.value = false;
    interactionMessage.value = null;
    logMessage.value = null;
    backgroundMessage.value = null;
    collapsePendingKeys.value = new Set();
    collapseFailedKeys.value = new Set();
    lockPendingKeys.value = new Set();
    lockFailedKeys.value = new Set();
    search.resetForWorldChange();
});

watch(
    [statusMessage, statusIsPersistentLoadError],
    ([message, persistentLoadError]) => {
        cancelStatusDismissTimers();
        statusDismissing.value = false;
        if (message === null || persistentLoadError) return;

        statusDismissDelay = setTimeout(() => {
            statusDismissDelay = null;
            statusDismissing.value = true;
            statusDismissRemoval = setTimeout(() => {
                statusDismissRemoval = null;
                dismissStatus();
            }, STATUS_DISMISS_ANIMATION_MS);
        }, TRANSIENT_STATUS_VISIBLE_MS);
    },
    {immediate: true},
);

watch(
    () => data.graph.value.edges.map(edge => (
        `${edge.get("world_id")}:${edge.get("from_id")}:${edge.get("to_id")}:${Number(edge.get("is_traversable"))}`
    )).join("|"),
    () => {
        for (const edge of data.graph.value.edges) publishEdgeTraversability(edge);
    },
    {flush: "sync"},
);

watch(
    () => [viewport.hostSize.width, viewport.hostSize.height] as const,
    () => {
        const state = contextMenu.value;
        if (state !== null) void repositionContextMenu(state);
    },
);

const hostClass = computed(() => ({
    "is-alt-ready": altPressed.value && interactions.activePointer.value?.mode !== "pan",
    "is-panning": interactions.activePointer.value?.mode === "pan",
    "is-keyboard-resizing": interactions.keyboardResize.value !== null,
    "has-pending-persistence": interactions.hasPendingPersistence.value,
}));
</script>

<template>
    <div ref="hostRef" class="world-graph" :class="hostClass" tabindex="0" @pointerdown.capture="handleHostPointerDownCapture" @pointermove="handlePointerMove" @pointerup="handlePointerUp" @pointercancel="handlePointerCancel" @wheel.prevent="handleWheel" @contextmenu.prevent>
        <input ref="fileInputRef" class="world-graph__file-input" type="file" accept="image/*" @change="onBackgroundFileChange">
        <svg class="world-graph__viewport" :viewBox="viewport.viewportViewBox.value" role="application" aria-label="World graph editor">
            <defs>
                <pattern id="world-graph-grid-small" width="20" height="20" patternUnits="userSpaceOnUse"><path d="M 20 0 L 0 0 0 20" class="world-graph__grid-small" /></pattern>
                <pattern id="world-graph-grid-large" width="100" height="100" patternUnits="userSpaceOnUse"><rect width="100" height="100" fill="url(#world-graph-grid-small)" /><path d="M 100 0 L 0 0 0 100" class="world-graph__grid-large" /></pattern>
                <marker id="world-graph-arrow-traversable" viewBox="0 0 8 8" :markerWidth="viewerSettings.edgeArrowStrokeMultiplier" :markerHeight="viewerSettings.edgeArrowStrokeMultiplier" refX="7" refY="4" orient="auto" markerUnits="strokeWidth"><path d="M 0 0 L 8 4 L 0 8 z" class="world-graph__arrow world-graph__arrow--traversable" /></marker>
                <marker id="world-graph-arrow-blocked" viewBox="0 0 8 8" :markerWidth="viewerSettings.edgeArrowStrokeMultiplier" :markerHeight="viewerSettings.edgeArrowStrokeMultiplier" refX="7" refY="4" orient="auto" markerUnits="strokeWidth"><path d="M 0 0 L 8 4 L 0 8 z" class="world-graph__arrow world-graph__arrow--blocked" /></marker>
                <marker id="world-graph-arrow-selected" viewBox="0 0 8 8" :markerWidth="viewerSettings.edgeArrowStrokeMultiplier" :markerHeight="viewerSettings.edgeArrowStrokeMultiplier" refX="7" refY="4" orient="auto" markerUnits="strokeWidth"><path d="M 0 0 L 8 4 L 0 8 z" class="world-graph__arrow world-graph__arrow--selected" /></marker>
                <clipPath v-for="region in renderedRegions" :id="regionClipId(region)" :key="regionClipId(region)"><rect v-bind="expandedRegionRect(region)" :rx="regionCornerRadius(region)" /></clipPath>
            </defs>

            <g>
                <rect :x="-viewport.pan.value.x / viewport.zoom.value" :y="-viewport.pan.value.y / viewport.zoom.value" :width="viewport.hostSize.width / viewport.zoom.value" :height="viewport.hostSize.height / viewport.zoom.value" class="world-graph__margin" fill="url(#world-graph-grid-large)" />

                <g class="world-graph__world" :class="worldClass()">
                    <rect v-bind="worldRect" class="world-graph__world-surface" data-world-canvas @pointerdown="interactions.onWorldPointerDown" />
                    <image v-if="backgrounds.worldUrl.value !== null && props.world.get('background_visible')" :href="backgrounds.worldUrl.value" v-bind="worldRect" :opacity="normalizedOpacity(props.world.get('background_opacity'))" :preserveAspectRatio="preserveAspectRatio(props.world.get('background_fit'))" class="world-graph__background-image" />
                    <rect v-bind="worldRect" class="world-graph__world-outline" />
                </g>

                <g v-for="region in renderedRegions" :key="regionEntityKey(region)" class="world-graph__region" :class="regionClass(region)">
                    <rect v-bind="regionInteractionRect(region)" class="world-graph__node-hit" :data-graph-entity-key="regionEntityKey(region)" data-graph-entity-kind="region" @pointerdown="interactions.onRegionPointerDown($event, region)" />
                    <rect v-bind="regionRect(region)" :rx="regionCornerRadius(region)" class="world-graph__region-surface" :data-graph-entity-key="regionEntityKey(region)" data-graph-entity-kind="region" @pointerdown="interactions.onRegionPointerDown($event, region)" />
                    <image v-if="!region.get('collapsed') && backgrounds.regionUrl(region) !== null && region.get('background_visible')" :href="backgrounds.regionUrl(region) ?? undefined" v-bind="expandedRegionRect(region)" :opacity="normalizedOpacity(region.get('background_opacity'))" :preserveAspectRatio="preserveAspectRatio(region.get('background_fit'))" :clip-path="`url(#${regionClipId(region)})`" class="world-graph__background-image" />
                    <rect v-bind="regionRect(region)" :rx="regionCornerRadius(region)" class="world-graph__candidate-outline" />
                    <rect v-bind="regionSelectionRect(region)" :rx="screenPixelsToWorldUnits(13, viewport.zoom.value)" class="world-graph__region-selection" />
                    <template v-if="regionLabelVisible(region)">
                        <rect v-bind="regionLabelBackgroundRect(region)" :rx="regionDisplayFontSize(region) * 0.45" class="world-graph__region-label-background" />
                        <text :x="regionLabelPosition(region).x" :y="regionLabelPosition(region).y" :style="{fontSize: `${regionDisplayFontSize(region)}px`}" class="world-graph__region-label">{{ regionDisplayName(region) }}</text>
                        <text v-if="region.get('collapsed')" :x="regionCollapseMarkPosition(region).x" :y="regionCollapseMarkPosition(region).y" :style="{fontSize: `${regionDisplayFontSize(region) * 1.35}px`}" class="world-graph__collapse-mark">▸</text>
                    </template>
                    <text v-if="regionClass(region)['is-failed']" :x="regionFailureMarkPosition(region).x" :y="regionFailureMarkPosition(region).y" :style="{fontSize: `${failureMarkFontSize()}px`}" class="world-graph__failure-mark">!</text>
                </g>

                <g class="world-graph__edges">
                    <g v-for="model in edgeModels" :key="model.key">
                        <path :d="model.path" class="world-graph__edge-halo" :class="edgeClass(model)" :style="edgePathStyle(model, true)" />
                        <path :d="model.path" class="world-graph__edge" :class="edgeClass(model)" :style="edgePathStyle(model, false)" :marker-end="edgeMarker(model)" />
                    </g>
                </g>

                <g v-for="location in renderedLocations" :key="locationEntityKey(location)" class="world-graph__location" :class="locationClass(location)" :transform="`translate(${locationWorldPosition(location).x} ${locationWorldPosition(location).y})`" :data-graph-entity-key="locationEntityKey(location)" data-graph-entity-kind="location" @pointerdown="interactions.onLocationPointerDown($event, location)">
                    <circle :r="locationHitRadius(location)" class="world-graph__node-hit" />
                    <circle :r="locationCircleRadius(location)" class="world-graph__location-surface" />
                    <circle :r="locationCircleRadius(location) + locationSelectionOffset()" class="world-graph__location-selection" />
                    <circle v-if="interactions.isLocationRadiusFailed(location)" :r="locationCircleRadius(location) + locationRadiusFailureOffset()" class="world-graph__location-radius-failure" />
                    <text v-if="locationLabelVisible(location)" :style="{fontSize: `${locationLabelFontSize(location)}px`}" class="world-graph__location-label" text-anchor="middle">
                        <tspan v-for="(line, index) in locationLabelLines(location)" :key="`${locationEntityKey(location)}-${index}`" x="0" :dy="index === 0 ? (locationLabelLines(location).length === 1 ? '0.35em' : '-0.15em') : '1.1em'">{{ line }}</tspan>
                    </text>
                    <text v-if="interactions.isLocationFailed(location) || interactions.isLocationRadiusFailed(location)" :x="locationFailureMarkPosition(location).x" :y="locationFailureMarkPosition(location).y" :style="{fontSize: `${failureMarkFontSize()}px`}" class="world-graph__failure-mark">!</text>
                </g>

                <rect v-if="interactions.marqueeRect.value !== null" :x="interactions.marqueeRect.value.x - interactions.marqueeRect.value.width / 2" :y="interactions.marqueeRect.value.y - interactions.marqueeRect.value.height / 2" :width="interactions.marqueeRect.value.width" :height="interactions.marqueeRect.value.height" class="world-graph__marquee" />

                <g class="world-graph__hierarchy-cues">
                    <text v-for="region in renderedRegions" v-show="regionHierarchyCue(region) !== null" :key="`region-parent-${regionEntityKey(region)}`" :x="regionDisplayGeometry(region).x" :y="regionRect(region).y + regionRect(region).height + hierarchyCueGap()" :style="{fontSize: `${hierarchyCueFontSize()}px`}" class="world-graph__hierarchy-cue" text-anchor="middle">{{ regionHierarchyCue(region) }}</text>
                    <text v-for="location in renderedLocations" v-show="locationHierarchyCue(location) !== null" :key="`location-parent-${locationEntityKey(location)}`" :x="locationWorldPosition(location).x" :y="locationWorldPosition(location).y + locationCircleRadius(location) + hierarchyCueGap()" :style="{fontSize: `${hierarchyCueFontSize()}px`}" class="world-graph__hierarchy-cue" text-anchor="middle">{{ locationHierarchyCue(location) }}</text>
                </g>
            </g>
        </svg>

        <div class="world-graph__top-row">
            <div class="world-graph__world-toolbar-slot">
                <div class="world-graph__world-toolbar" data-graph-control @pointerdown.stop="dismissPopovers" aria-label="World editor controls">
                    <button type="button" class="world-graph__world-toolbar-icon" title="Back to worlds" aria-label="Back to worlds" @click="emit('back')">
                        <svg viewBox="0 0 24 24" aria-hidden="true">
                            <path d="m15 18-6-6 6-6" />
                        </svg>
                    </button>
                    <button type="button" class="world-graph__world-name" title="Edit world information" @click="editWorld">
                        {{ worldName }}
                    </button>
                    <button type="button" class="world-graph__world-toolbar-icon" title="Export world" aria-label="Export world" :disabled="props.exportingWorld" @click="emit('exportWorld')">
                        <span v-if="props.exportingWorld" class="world-graph__export-pending" aria-hidden="true">…</span>
                        <svg v-else viewBox="0 0 24 24" aria-hidden="true">
                            <path d="M12 3v12" />
                            <path d="m7 10 5 5 5-5" />
                            <path d="M5 21h14" />
                        </svg>
                    </button>
                </div>

                <div class="world-graph__zoom-controls world-graph__toolbar" data-graph-control @pointerdown.stop="dismissPopovers">
                    <button type="button" class="world-graph__control" aria-label="Zoom in" @click="zoomIn">+</button>
                    <button type="button" class="world-graph__zoom" @click="resetGraphViewport">{{ viewport.zoomPercentage.value }}</button>
                    <button type="button" class="world-graph__control" aria-label="Zoom out" @click="zoomOut">−</button>
                    <button type="button" class="world-graph__reset" @click="resetGraphViewport">Reset</button>
                </div>
            </div>

            <div class="world-graph__status-layer">
                <div v-if="statusMessage !== null" class="world-graph__status" :class="[`is-${statusKind}`, {'is-dismissing': statusDismissing}]" :role="statusKind === 'error' ? 'alert' : 'status'">
                    <span>{{ statusMessage }}</span>
                    <button v-if="statusIsPersistentLoadError" type="button" @click="data.reload()">Retry</button>
                    <button v-else type="button" aria-label="Dismiss message" @click="dismissStatus">×</button>
                </div>
            </div>

            <div class="world-graph__top-right">
                <div v-if="interactions.selectionSummary.value.total > 0" class="world-graph__selection-summary" aria-live="polite">{{ interactions.selectionSummary.value.locations }} {{ interactions.selectionSummary.value.locations === 1 ? "location" : "locations" }}, {{ interactions.selectionSummary.value.regions }} {{ interactions.selectionSummary.value.regions === 1 ? "region" : "regions" }} selected</div>
                <div class="world-graph__help-control" data-graph-control @pointerdown.stop>
                    <button
                        type="button"
                        class="world-graph__help-button"
                        :class="{'is-open': helpOpen}"
                        title="World graph controls"
                        aria-label="World graph controls help"
                        aria-controls="world-graph-controls-help"
                        :aria-expanded="helpOpen"
                        @click="toggleHelp"
                    >?</button>
                </div>
            </div>

            <section
                v-if="helpOpen"
                id="world-graph-controls-help"
                class="world-graph__help-popover"
                :style="{maxHeight: `${Math.max(1, viewport.hostSize.height - 108)}px`}"
                data-graph-popover
                role="dialog"
                aria-label="World graph controls"
                @pointerdown.stop
                @wheel.stop
            >
                <header class="world-graph__help-heading">
                    <div>
                        <strong>World graph controls</strong>
                        <span>Actual v10 mouse, keyboard, search, menu, and viewer controls.</span>
                    </div>
                    <button type="button" aria-label="Close controls help" title="Close controls help" @click="helpOpen = false">×</button>
                </header>

                <div class="world-graph__help-sections">
                    <section v-for="section in WORLD_GRAPH_CONTROL_HELP_SECTIONS" :key="section.title" class="world-graph__help-section">
                        <h3>{{ section.title }}</h3>
                        <dl>
                            <template v-for="entry in section.entries" :key="entry.control">
                                <dt><kbd>{{ entry.control }}</kbd></dt>
                                <dd>{{ entry.description }}</dd>
                            </template>
                        </dl>
                    </section>
                </div>
            </section>
        </div>

        <div class="world-graph__search-slot" @pointerdown.capture="lodSettingsOpen = false; helpOpen = false">
            <WorldGraphSearch
                ref="searchComponentRef"
                :model-value="search.query.value"
                :pending="search.isSearching.value"
                :message="null"
                :message-kind="search.messageKind.value"
                :results="search.results.value"
                :match-count="search.matchCount.value"
                :max-height="Math.max(1, viewport.hostSize.height / 2)"
                @update:model-value="updateSearchQuery"
                @search="search.submit()"
                @choose="search.chooseResult"
                @clear="search.clearQuery"
            />
        </div>

        <div class="world-graph__settings-control" data-graph-control @pointerdown.stop>
            <button
                type="button"
                class="world-graph__settings-button"
                :class="{'is-open': lodSettingsOpen}"
                title="Display settings"
                aria-label="Display settings"
                aria-controls="world-graph-display-settings"
                :aria-expanded="lodSettingsOpen"
                @click="toggleLodSettings"
            >
                <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.09a2 2 0 0 1 1 1.74v.5a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.38a2 2 0 0 0-.73-2.73l-.15-.09a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2Z" />
                    <circle cx="12" cy="12" r="3" />
                </svg>
            </button>

            <div v-if="lodSettingsOpen" id="world-graph-display-settings" class="world-graph__settings-popover" data-graph-popover @pointerdown.stop @wheel.stop>
                <div class="world-graph__settings-heading">Viewer config</div>
                <label class="world-graph__lod-control">
                    <span>LOD detail <output>{{ Math.round(lodDetail) }}%</output></span>
                    <input v-model.number="lodDetail" type="range" min="0" max="100" step="5" aria-label="Level of detail" :aria-valuetext="`${Math.round(lodDetail)} percent detail`">
                    <small>Higher keeps nodes, labels and edges visible farther out.</small>
                </label>

                <details v-for="group in viewerTuningGroups" :key="group.label" class="world-graph__settings-group">
                    <summary>{{ group.label }}</summary>
                    <div class="world-graph__settings-fields">
                        <label v-for="field in group.fields" :key="field.key" class="world-graph__settings-field">
                            <span>
                                <span>{{ field.label }}</span>
                                <span class="world-graph__settings-unit">{{ field.unit }}</span>
                            </span>
                            <input
                                type="number"
                                :value="viewerSettings[field.key]"
                                :min="worldGraphViewerSettingBounds(field.key).min"
                                :max="worldGraphViewerSettingBounds(field.key).max"
                                :step="field.step"
                                :aria-label="field.label"
                                @change="updateViewerSettingFromInput(field.key, $event)"
                            >
                            <small>{{ field.help }}</small>
                        </label>
                    </div>
                </details>

                <button type="button" class="world-graph__settings-reset" @click="resetViewerSettings">
                    Reset defaults
                </button>
            </div>
        </div>

        <div v-if="data.isLoading.value" class="world-graph__state" aria-live="polite">Loading graph…</div>
        <div v-else-if="isEmpty" class="world-graph__state" aria-live="polite">This world is empty. Secondary-click inside the world canvas to create a root region or set a background.</div>

        <div v-if="contextMenu !== null" ref="contextMenuRef" class="world-graph__menu" :style="{left: `${contextMenu.screen.x}px`, top: `${contextMenu.screen.y}px`, maxHeight: `${contextMenu.maxHeight}px`}" data-graph-popover @pointerdown.stop @wheel.stop>
            <template v-if="contextMenu.target.kind === 'world' && interactions.selectionSummary.value.total === 0">
                <div class="world-graph__menu-heading">World</div>
                <button type="button" :disabled="interactions.worldScalePending.value" @click="startCreation('root-region', null)">Create root region</button>
                <button type="button" :disabled="backgrounds.isBackgroundPending({kind: 'world', world: props.world})" @click="requestSetBackground({kind: 'world', world: props.world})">Set background</button>
                <button type="button" class="is-danger" :disabled="!backgrounds.hasWorldBackground.value || backgrounds.isBackgroundPending({kind: 'world', world: props.world})" @click="requestDeleteBackground({kind: 'world', world: props.world})">Remove background</button>
                <button type="button" @click="editWorld">Edit world</button>
            </template>

            <template v-else-if="contextMenu.target.kind === 'region' && singleRegionSelection">
                <div class="world-graph__menu-heading">{{ contextMenu.target.region.get("name") }}</div>
                <button type="button" @click="editRegion(contextMenu.target.region)">Edit region</button>
                <button type="button" :disabled="lockPendingKeys.has(regionEntityKey(contextMenu.target.region))" @click="toggleSelectedRegionLock">{{ selectedSingleRegionLocked ? "Unlock region" : "Lock region" }}</button>
                <button type="button" :disabled="regionHasConflictingOperation(contextMenu.target.region) || selectedSingleRegionLocked" @click="toggleRegionCollapse(contextMenu.target.region)">{{ contextMenu.target.region.get("collapsed") ? "Expand region" : "Collapse region" }}</button>
                <button type="button" :disabled="contextMenu.target.region.get('collapsed') || interactions.isRegionPending(contextMenu.target.region)" @click="startCreation('location', contextMenu.target.region)">Create location</button>
                <button type="button" :disabled="contextMenu.target.region.get('collapsed') || interactions.isRegionPending(contextMenu.target.region)" @click="startCreation('sub-region', contextMenu.target.region)">Create sub-region</button>
                <button type="button" :disabled="backgrounds.isBackgroundPending({kind: 'region', region: contextMenu.target.region})" @click="requestSetBackground({kind: 'region', region: contextMenu.target.region})">Set background</button>
                <button type="button" class="is-danger" :disabled="!backgrounds.hasRegionBackground(contextMenu.target.region) || backgrounds.isBackgroundPending({kind: 'region', region: contextMenu.target.region})" @click="requestDeleteBackground({kind: 'region', region: contextMenu.target.region})">Remove background</button>
                <button type="button" class="is-danger" :disabled="regionHasConflictingOperation(contextMenu.target.region)" @click="requestDeleteRegion(contextMenu.target.region)">Delete region</button>
            </template>

            <template v-else-if="contextMenu.target.kind === 'location' && singleLocationSelection">
                <div class="world-graph__menu-heading">{{ contextMenu.target.location.get("name") }}</div>
                <button type="button" @click="editLocation(contextMenu.target.location)">Edit location</button>
                <button type="button" :disabled="lockPendingKeys.has(locationEntityKey(contextMenu.target.location))" @click="toggleSelectedLocationLock">{{ selectedSingleLocationLocked ? "Unlock location" : "Lock location" }}</button>
                <button type="button" :disabled="!locationUsesExplicitRadius(contextMenu.target.location) || interactions.isLocationPending(contextMenu.target.location)" @click="resetLocationAutomaticSize(contextMenu.target.location)">Reset to automatic size</button>
                <button type="button" class="is-danger" :disabled="interactions.isLocationPending(contextMenu.target.location)" @click="requestDeleteLocation(contextMenu.target.location)">Delete location</button>
            </template>

            <template v-else-if="contextMenu.target.kind === 'location' && twoLocationSelection && connectionPair !== null && connectionState !== null">
                <div class="world-graph__menu-heading">Two locations</div>
                <button type="button" :disabled="selectedLockPending" @click="toggleSelectedLocks">{{ selectedLockActionLabel }}</button>
                <button type="button" :disabled="connectionPending || connectionPairHasConflict" @click="startMiddleLocationCreation(connectionPair[0], connectionPair[1])">Create middle location</button>
                <button type="button" :disabled="connectionPending || connectionPairHasConflict || connectionState.forward !== null" @click="createOneDirection(connectionPair[0], connectionPair[1])">Connect {{ connectionDirectionLabel(connectionPair[0], connectionPair[1]) }}</button>
                <button type="button" :disabled="connectionPending || connectionPairHasConflict || connectionState.reverse !== null" @click="createOneDirection(connectionPair[1], connectionPair[0])">Connect {{ connectionDirectionLabel(connectionPair[1], connectionPair[0]) }}</button>
                <button v-if="connectionState.forward === null && connectionState.reverse === null" type="button" :disabled="connectionPending || connectionPairHasConflict" @click="createBothDirections">Connect both directions</button>
                <button type="button" class="is-danger" :disabled="connectionPending || connectionPairHasConflict || connectionState.forward === null" @click="requestDeleteEdge(connectionPair[0], connectionPair[1])">Disconnect {{ connectionDirectionLabel(connectionPair[0], connectionPair[1]) }}</button>
                <button type="button" class="is-danger" :disabled="connectionPending || connectionPairHasConflict || connectionState.reverse === null" @click="requestDeleteEdge(connectionPair[1], connectionPair[0])">Disconnect {{ connectionDirectionLabel(connectionPair[1], connectionPair[0]) }}</button>
                <button type="button" class="is-danger" :disabled="connectionPending || connectionPairHasConflict || (connectionState.forward === null && connectionState.reverse === null)" @click="requestDeleteBoth(connectionPair[0], connectionPair[1])">Disconnect both</button>
                <button v-if="connectionState.forward !== null || connectionState.reverse !== null" type="button" :disabled="connectionPairHasConflict" @click="editConnection">Edit connection</button>
                <button type="button" :disabled="connectionPending || bulkConnectionHasConflict" @click="openBulkLinkSubmenu">Link locations ›</button>
                <button type="button" class="is-danger" @click="requestDeleteSelectedLocations">Delete locations</button>
            </template>

            <template v-else-if="contextMenu.target.kind === 'location' && selectionOnlyLocations && interactions.selectedLocations.value.length > 2">
                <div class="world-graph__menu-heading">{{ interactions.selectedLocations.value.length }} locations selected</div>
                <button type="button" :disabled="selectedLockPending" @click="toggleSelectedLocks">{{ selectedLockActionLabel }}</button>
                <button type="button" :disabled="connectionPending || bulkConnectionHasConflict" @click="openBulkLinkSubmenu">Link locations ›</button>
                <button type="button" class="is-danger" @click="requestDeleteSelectedLocations">Delete locations</button>
            </template>

            <template v-else-if="contextMenu.target.kind === 'region' && multiRegionSelection">
                <div class="world-graph__menu-heading">{{ interactions.selectedRegions.value.length }} regions selected</div>
                <button type="button" :disabled="selectedLockPending" @click="toggleSelectedLocks">{{ selectedLockActionLabel }}</button>
                <button type="button" @click="toggleSelectedRegionsCollapse">{{ selectedRegionsCollapseLabel }}</button>
                <button type="button" class="is-danger" @click="requestDeleteSelection">Delete selected regions</button>
            </template>

            <template v-else-if="interactions.selectionSummary.value.total > 0">
                <div class="world-graph__menu-heading">Selection</div>
                <button type="button" :disabled="selectedLockPending" @click="toggleSelectedLocks">{{ selectedLockActionLabel }}</button>
                <button type="button" class="is-danger" @click="requestDeleteSelection">Delete selected nodes</button>
            </template>

            <template v-if="connectionPair !== null && connectionState !== null && !(contextMenu.target.kind === 'location' && twoLocationSelection)">
                <div class="world-graph__menu-separator"></div>
                <div class="world-graph__menu-heading">Selected location pair</div>
                <button type="button" :disabled="connectionPending || connectionPairHasConflict" @click="startMiddleLocationCreation(connectionPair[0], connectionPair[1])">Create middle location</button>
                <button type="button" :disabled="connectionPending || connectionPairHasConflict || connectionState.forward !== null" @click="createOneDirection(connectionPair[0], connectionPair[1])">Connect {{ connectionDirectionLabel(connectionPair[0], connectionPair[1]) }}</button>
                <button type="button" :disabled="connectionPending || connectionPairHasConflict || connectionState.reverse !== null" @click="createOneDirection(connectionPair[1], connectionPair[0])">Connect {{ connectionDirectionLabel(connectionPair[1], connectionPair[0]) }}</button>
                <button v-if="connectionState.forward === null && connectionState.reverse === null" type="button" :disabled="connectionPending || connectionPairHasConflict" @click="createBothDirections">Connect both directions</button>
                <button type="button" class="is-danger" :disabled="connectionPending || connectionPairHasConflict || connectionState.forward === null" @click="requestDeleteEdge(connectionPair[0], connectionPair[1])">Disconnect {{ connectionDirectionLabel(connectionPair[0], connectionPair[1]) }}</button>
                <button type="button" class="is-danger" :disabled="connectionPending || connectionPairHasConflict || connectionState.reverse === null" @click="requestDeleteEdge(connectionPair[1], connectionPair[0])">Disconnect {{ connectionDirectionLabel(connectionPair[1], connectionPair[0]) }}</button>
                <button type="button" class="is-danger" :disabled="connectionPending || connectionPairHasConflict || (connectionState.forward === null && connectionState.reverse === null)" @click="requestDeleteBoth(connectionPair[0], connectionPair[1])">Disconnect both</button>
                <button v-if="connectionState.forward !== null || connectionState.reverse !== null" type="button" :disabled="connectionPairHasConflict" @click="editConnection">Edit connection</button>
                <button type="button" :disabled="connectionPending || bulkConnectionHasConflict" @click="openBulkLinkSubmenu">Link locations ›</button>
                <button type="button" class="is-danger" @click="requestDeleteSelectedLocations">Delete selected locations</button>
            </template>

            <template v-else-if="interactions.selectedLocations.value.length > 2 && !(contextMenu.target.kind === 'location' && selectionOnlyLocations)">
                <div class="world-graph__menu-separator"></div>
                <div class="world-graph__menu-heading">Selected locations</div>
                <button type="button" :disabled="connectionPending || bulkConnectionHasConflict" @click="openBulkLinkSubmenu">Link locations ›</button>
                <button type="button" class="is-danger" @click="requestDeleteSelectedLocations">Delete selected locations</button>
            </template>

            <template v-if="interactions.selectionSummary.value.total > 0">
                <div class="world-graph__menu-separator"></div>
                <button type="button" @click="clearGraphSelection">Clear selection</button>
            </template>
        </div>

        <div v-if="contextSubmenu !== null" ref="contextSubmenuRef" class="world-graph__menu world-graph__menu--submenu" :style="{left: `${contextSubmenu.screen.x}px`, top: `${contextSubmenu.screen.y}px`, maxHeight: `${contextSubmenu.maxHeight}px`}" data-graph-popover @pointerdown.stop @wheel.stop>
            <div class="world-graph__menu-heading">Link selected locations</div>
            <label class="world-graph__menu-field">
                <span>Topology</span>
                <select v-model="bulkConnectionTopology" :disabled="connectionPending">
                    <option value="nearest-network">Nearest network (MST)</option>
                    <option value="all-pairs">All pairs</option>
                </select>
            </label>
            <label class="world-graph__menu-field">
                <span>Direction</span>
                <select v-model="bulkConnectionDirection" :disabled="connectionPending">
                    <option value="bidirectional">Bidirectional</option>
                    <option value="one-way">One-way</option>
                </select>
            </label>
            <label class="world-graph__menu-check">
                <input v-model="bulkConnectionTraversable" type="checkbox" :disabled="connectionPending">
                <span>Traversable</span>
            </label>
            <button type="button" :disabled="connectionPending || bulkConnectionHasConflict" @click="createBulkSelectedConnections">Create missing connections</button>
            <button type="button" @click="contextSubmenu = null">Close submenu</button>
        </div>

        <form v-if="creation !== null" class="world-graph__creation" :style="{left: `${creation.screen.x}px`, top: `${creation.screen.y}px`}" data-graph-popover @submit.prevent="submitCreation" @pointerdown.stop @wheel.stop>
            <label for="world-graph-create-name">{{ creationTitle(creation) }}</label>
            <input id="world-graph-create-name" v-model="creation.name" data-creation-input type="text" maxlength="160" autocomplete="off" :disabled="creation.pending">
            <div class="world-graph__creation-actions"><button type="button" :disabled="creation.pending" @click="creation = null">Cancel</button><button type="submit" :disabled="creation.pending">{{ creation.pending ? "Creating…" : "Create" }}</button></div>
        </form>

        <div v-if="confirmation !== null" class="world-graph__modal-backdrop" data-graph-modal @pointerdown.stop @wheel.stop.prevent @contextmenu.prevent>
            <section class="world-graph__modal" role="dialog" aria-modal="true" :aria-label="confirmationTitle(confirmation)">
                <h2>{{ confirmationTitle(confirmation) }}</h2>
                <p>{{ confirmationText(confirmation) }}</p>
                <div class="world-graph__modal-actions"><button type="button" :disabled="confirmationPending" @click="confirmation = null">Cancel</button><button type="button" class="is-danger" :disabled="confirmationPending" @click="confirmDeletion">{{ confirmationPending ? "Applying…" : "Confirm" }}</button></div>
            </section>
        </div>
    </div>
</template>

<style scoped>
.world-graph { position: relative; width: 100%; height: 100%; min-width: 0; min-height: 0; box-sizing: border-box; overflow: hidden; isolation: isolate; border: 1px solid rgb(var(--c-border)); border-radius: var(--radius-lg); background: rgb(var(--c-page)); color: rgb(var(--c-fg)); outline: none; user-select: none; touch-action: none; font-family: var(--font-primary); }
.world-graph:focus-visible { box-shadow: 0 0 0 var(--focus-ring-width) rgb(var(--focus-ring-color) / 0.45); }
.world-graph.is-alt-ready { cursor: grab; }
.world-graph.is-panning { cursor: grabbing; }
.world-graph.is-keyboard-resizing, .world-graph.is-keyboard-resizing .world-graph__viewport, .world-graph.is-keyboard-resizing .world-graph__region-surface, .world-graph.is-keyboard-resizing .world-graph__location-surface { cursor: nwse-resize; }
.world-graph__file-input { display: none; }
.world-graph__viewport { display: block; width: 100%; height: 100%; min-width: 0; min-height: 0; overflow: hidden; }
.world-graph__margin { fill: rgb(var(--c-page)); pointer-events: none; }
.world-graph__grid-small { fill: none; stroke: rgb(var(--c-border) / 0.18); stroke-width: 1; }
.world-graph__grid-large { fill: none; stroke: rgb(var(--c-border-strong) / 0.28); stroke-width: 1.2; }
.world-graph__world-surface { fill: rgb(var(--c-surface)); stroke: rgb(var(--c-border-strong)); stroke-width: 2; pointer-events: all; vector-effect: non-scaling-stroke; }
.world-graph__world-outline { fill: none; stroke: rgb(var(--c-primary)); stroke-width: 2; pointer-events: none; vector-effect: non-scaling-stroke; }
.world-graph__world.is-scaling .world-graph__world-outline { stroke-width: 4; stroke-dasharray: 10 6; }
.world-graph__world.is-pending .world-graph__world-outline { stroke-dasharray: 5 4; animation: world-graph-dash 900ms linear infinite; }
.world-graph__world.is-failed .world-graph__world-outline { stroke: rgb(var(--c-danger)); stroke-width: 4; }
.world-graph__background-image { pointer-events: none; }
.world-graph__region { transition: opacity var(--duration-fast) var(--ease-standard); }
.world-graph__region-surface { fill: rgb(var(--c-surface-2) / 0.9); stroke: rgb(var(--c-border-strong)); stroke-width: 2; vector-effect: non-scaling-stroke; transition: stroke-width var(--duration-fast) var(--ease-standard), opacity var(--duration-fast) var(--ease-standard); }
.world-graph__region:hover .world-graph__region-surface { stroke-width: 3; }
.world-graph__region.is-locked .world-graph__region-surface, .world-graph__location.is-locked .world-graph__location-surface { stroke-dasharray: 3 3; cursor: not-allowed; }
.world-graph__region.is-collapsed .world-graph__region-surface { fill: rgb(var(--c-surface-raised)); }
.world-graph__region-selection, .world-graph__candidate-outline { fill: none; pointer-events: none; vector-effect: non-scaling-stroke; opacity: 0; }
.world-graph__region-selection { stroke: rgb(var(--c-primary)); stroke-width: 3; stroke-dasharray: 8 5; }
.world-graph__candidate-outline { stroke: rgb(var(--c-success)); stroke-width: 4; stroke-dasharray: 3 4; }
.world-graph__region.is-selected .world-graph__region-selection, .world-graph__region.is-preview-selected .world-graph__region-selection, .world-graph__region.is-candidate .world-graph__candidate-outline { opacity: 1; }
.world-graph__region.is-preview-selected .world-graph__region-selection { stroke-dasharray: 2 4; }
.world-graph__region.is-root-candidate .world-graph__region-selection { stroke: rgb(var(--c-info)); }
.world-graph__region.is-invalid .world-graph__region-surface, .world-graph__location.is-invalid .world-graph__location-surface { stroke: rgb(var(--c-danger)); stroke-width: 4; }
.world-graph__region.is-pending .world-graph__region-surface, .world-graph__location.is-pending .world-graph__location-surface { stroke-dasharray: 7 5; animation: world-graph-dash 900ms linear infinite; }
.world-graph__region.is-failed .world-graph__region-surface, .world-graph__location.is-failed .world-graph__location-surface { stroke: rgb(var(--c-danger)); stroke-width: 4; stroke-dasharray: 2 3; }
.world-graph__region.is-moving, .world-graph__location.is-moving, .world-graph__region.is-scaling, .world-graph__location.is-radius-resizing { filter: drop-shadow(0 8px 10px rgb(var(--c-shadow) / 0.26)); }
.world-graph__node-hit { fill: transparent; stroke: transparent; pointer-events: all; }
.world-graph__region-label-background { fill: rgb(var(--c-surface-raised) / 0.94); stroke: rgb(var(--c-border) / 0.55); pointer-events: none; vector-effect: non-scaling-stroke; }
.world-graph__region-label, .world-graph__collapse-mark { font-family: var(--font-primary); font-weight: 650; fill: rgb(var(--c-fg-strong)); pointer-events: none; }
.world-graph__edge-halo { fill: none; stroke: rgb(var(--c-page) / 0.94); pointer-events: none; }
.world-graph__edge { fill: none; stroke: rgb(var(--c-accent)); pointer-events: none; }
.world-graph__edge.is-blocked { stroke: rgb(var(--c-danger)); }
.world-graph__edge.is-selected-incident { stroke: rgb(var(--c-warning-strong)); }
.world-graph__arrow { stroke: rgb(var(--c-page) / 0.94); stroke-width: 1.25; paint-order: stroke fill; }
.world-graph__arrow--traversable { fill: rgb(var(--c-accent)); }
.world-graph__arrow--blocked { fill: rgb(var(--c-danger)); }
.world-graph__arrow--selected { fill: rgb(var(--c-warning-strong)); }
.world-graph__location-surface { fill: rgb(var(--c-surface-raised)); stroke: rgb(var(--c-primary-strong)); stroke-width: 2.5; vector-effect: non-scaling-stroke; transition: stroke-width var(--duration-fast) var(--ease-standard); }
.world-graph__location:hover .world-graph__location-surface { stroke-width: 4; }
.world-graph__location-selection { fill: none; stroke: rgb(var(--c-accent)); stroke-width: 3; stroke-dasharray: 6 4; vector-effect: non-scaling-stroke; opacity: 0; pointer-events: none; }
.world-graph__location.is-selected .world-graph__location-selection, .world-graph__location.is-preview-selected .world-graph__location-selection { opacity: 1; }
.world-graph__location.is-preview-selected .world-graph__location-selection { stroke-dasharray: 2 4; }
.world-graph__location-label { font-family: var(--font-primary); font-weight: 650; fill: rgb(var(--c-fg-strong)); pointer-events: none; }
.world-graph__hierarchy-cue { fill: rgb(var(--c-muted)); font-family: var(--font-primary); font-weight: 650; paint-order: stroke; stroke: rgb(var(--c-page)); stroke-width: 3px; vector-effect: non-scaling-stroke; pointer-events: none; }
.world-graph__location.is-radius-resizing .world-graph__location-surface { stroke-width: 4; stroke-dasharray: 8 4; }
.world-graph__location-radius-failure { fill: none; stroke: rgb(var(--c-danger)); stroke-width: 3; stroke-dasharray: 2 3 9 3; vector-effect: non-scaling-stroke; pointer-events: none; }
.world-graph__failure-mark { font-family: var(--font-primary); font-weight: 800; fill: rgb(var(--c-danger)); pointer-events: none; }
.world-graph__marquee { fill: rgb(var(--c-info) / 0.12); stroke: rgb(var(--c-info)); stroke-width: 2; stroke-dasharray: 7 5; vector-effect: non-scaling-stroke; pointer-events: none; }
.world-graph__top-row { position: absolute; z-index: 11; top: var(--space-3); left: var(--space-3); right: var(--space-3); display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); align-items: start; gap: var(--space-3); min-width: 0; pointer-events: none; }
.world-graph__top-right { display: flex; align-items: flex-start; justify-content: flex-end; justify-self: stretch; gap: var(--space-2); min-width: 0; pointer-events: none; }
.world-graph__world-toolbar-slot { display: flex; flex-direction: column; align-items: flex-start; justify-self: start; gap: var(--space-2); width: min(26rem, 100%); min-width: 0; pointer-events: auto; }
.world-graph__world-toolbar { display: grid; grid-template-columns: 2rem minmax(var(--space-1), 1fr) minmax(0, 3fr) minmax(var(--space-1), 1fr) 2rem; align-items: center; width: 100%; max-width: 100%; box-sizing: border-box; overflow: hidden; padding: var(--space-1); color: rgb(var(--c-fg)); background: rgb(var(--c-surface-raised) / 0.96); border: 1px solid rgb(var(--c-border)); border-radius: var(--radius-md); box-shadow: 0 5px 18px rgb(var(--c-shadow) / 0.18); pointer-events: auto; }
.world-graph__world-toolbar > :nth-child(1) { grid-column: 1; }
.world-graph__world-toolbar > :nth-child(2) { grid-column: 3; }
.world-graph__world-toolbar > :nth-child(3) { grid-column: 5; }
.world-graph__world-toolbar button { height: 2rem; border: 0; border-radius: var(--radius-sm); background: transparent; color: rgb(var(--c-fg)); font: inherit; cursor: pointer; }
.world-graph__world-toolbar button:hover:not(:disabled) { background: rgb(var(--c-surface-hover)); }
.world-graph__world-toolbar button:disabled { opacity: 0.5; cursor: not-allowed; }
.world-graph__world-toolbar-icon { display: grid; place-items: center; width: 2rem; padding: 0; }
.world-graph__world-toolbar-icon svg { width: 1.05rem; height: 1.05rem; fill: none; stroke: currentColor; stroke-width: 1.8; stroke-linecap: round; stroke-linejoin: round; }
.world-graph__world-name { min-width: 0; padding: 0 var(--space-2); overflow: hidden; color: rgb(var(--c-fg-strong)) !important; font-size: 0.84rem !important; font-weight: 800 !important; text-overflow: ellipsis; white-space: nowrap; }
.world-graph__export-pending { font-size: 1rem; font-weight: 800; }
.world-graph__toolbar { display: flex; align-items: center; gap: var(--space-1); padding: var(--space-1); border: 1px solid rgb(var(--c-border)); border-radius: var(--radius-md); background: rgb(var(--c-surface-raised) / 0.96); box-shadow: 0 5px 18px rgb(var(--c-shadow) / 0.18); }
.world-graph__zoom-controls { flex: none; flex-wrap: wrap; max-width: 100%; box-sizing: border-box; overflow: hidden; pointer-events: auto; }
.world-graph__search-slot { position: absolute; z-index: 11; left: var(--space-3); bottom: var(--space-3); width: min(29rem, max(0px, calc(100% - 5rem))); min-width: 0; container-type: inline-size; pointer-events: auto; }
.world-graph__search-slot :deep(.world-graph-search) { width: 100%; max-width: 100%; }
.world-graph__settings-control { position: absolute; z-index: 12; right: var(--space-3); bottom: var(--space-3); display: flex; justify-content: flex-end; width: min(390px, calc(100% - var(--space-3) - var(--space-3))); min-width: 0; pointer-events: none; }
.world-graph__settings-button { display: grid; flex: none; place-items: center; width: 2.25rem; height: 2.25rem; padding: 0; border: 1px solid rgb(var(--c-border)); border-radius: var(--radius-md); background: rgb(var(--c-surface-raised) / 0.96); color: rgb(var(--c-fg)); box-shadow: 0 5px 18px rgb(var(--c-shadow) / 0.18); cursor: pointer; pointer-events: auto; }
.world-graph__settings-button:hover, .world-graph__settings-button.is-open { border-color: rgb(var(--c-border-hover)); background: rgb(var(--c-surface-hover)); }
.world-graph__settings-button svg { width: 1.1rem; height: 1.1rem; fill: none; stroke: currentColor; stroke-width: 1.8; stroke-linecap: round; stroke-linejoin: round; }
.world-graph__settings-popover { position: absolute; right: 0; bottom: calc(100% + var(--space-2)); z-index: var(--z-popover); width: 100%; min-width: 0; max-height: min(76vh, calc(100vh - 5rem)); box-sizing: border-box; overflow: auto; padding: var(--space-3); border: 1px solid rgb(var(--c-border)); border-radius: var(--radius-md); background: rgb(var(--c-surface-raised)); box-shadow: 0 12px 34px rgb(var(--c-shadow-strong) / 0.3); pointer-events: auto; }
.world-graph__settings-heading { margin-bottom: var(--space-2); color: rgb(var(--c-muted)); font-size: 0.75rem; font-weight: 750; text-transform: uppercase; letter-spacing: 0.055em; }
.world-graph__help-control { flex: none; pointer-events: auto; }
.world-graph__help-button { display: grid; place-items: center; width: 2rem; height: 2rem; padding: 0; border: 1px solid rgb(var(--c-border)); border-radius: 999px; background: rgb(var(--c-surface-raised) / 0.96); color: rgb(var(--c-fg)); box-shadow: 0 5px 18px rgb(var(--c-shadow) / 0.18); font: 800 0.88rem var(--font-primary); cursor: pointer; }
.world-graph__help-button:hover, .world-graph__help-button.is-open { border-color: rgb(var(--c-border-hover)); background: rgb(var(--c-surface-hover)); }
.world-graph__help-popover { position: absolute; z-index: var(--z-popover); top: calc(2rem + var(--space-2)); right: 0; width: min(42rem, 100%); min-width: 0; box-sizing: border-box; overflow: auto; padding: 0 var(--space-3) var(--space-3); border: 1px solid rgb(var(--c-border)); border-radius: var(--radius-md); background: rgb(var(--c-surface-raised)); box-shadow: 0 12px 34px rgb(var(--c-shadow-strong) / 0.3); color: rgb(var(--c-fg)); pointer-events: auto; }
.world-graph__help-heading { position: sticky; z-index: 1; top: 0; display: flex; align-items: flex-start; justify-content: space-between; gap: var(--space-3); margin: 0 calc(-1 * var(--space-3)); padding: var(--space-3); border-bottom: 1px solid rgb(var(--c-border)); background: rgb(var(--c-surface-raised)); }
.world-graph__help-heading > div { display: grid; gap: 0.2rem; min-width: 0; }
.world-graph__help-heading strong { color: rgb(var(--c-fg-strong)); font-size: 0.92rem; }
.world-graph__help-heading span { color: rgb(var(--c-muted)); font-size: 0.7rem; line-height: 1.35; }
.world-graph__help-heading button { flex: none; width: 1.8rem; height: 1.8rem; padding: 0; border: 1px solid rgb(var(--c-border)); border-radius: var(--radius-sm); background: rgb(var(--c-surface)); color: rgb(var(--c-fg)); font: 700 1rem var(--font-primary); cursor: pointer; }
.world-graph__help-heading button:hover { border-color: rgb(var(--c-border-hover)); background: rgb(var(--c-surface-hover)); }
.world-graph__help-sections { display: grid; gap: var(--space-3); padding-top: var(--space-3); }
.world-graph__help-section { min-width: 0; }
.world-graph__help-section + .world-graph__help-section { padding-top: var(--space-3); border-top: 1px solid rgb(var(--c-border) / 0.72); }
.world-graph__help-section h3 { margin: 0 0 var(--space-2); color: rgb(var(--c-muted)); font-size: 0.72rem; font-weight: 800; letter-spacing: 0.045em; text-transform: uppercase; }
.world-graph__help-section dl { display: grid; grid-template-columns: minmax(5.5rem, 0.72fr) minmax(0, 1.65fr); gap: 0.45rem var(--space-3); margin: 0; }
.world-graph__help-section dt, .world-graph__help-section dd { min-width: 0; margin: 0; }
.world-graph__help-section dt { align-self: start; }
.world-graph__help-section kbd { display: inline-block; max-width: 100%; box-sizing: border-box; padding: 0.2rem 0.4rem; border: 1px solid rgb(var(--c-border)); border-bottom-color: rgb(var(--c-border-strong)); border-radius: var(--radius-sm); background: rgb(var(--c-surface)); color: rgb(var(--c-fg-strong)); font: 700 0.68rem var(--font-monospace); line-height: 1.3; overflow-wrap: anywhere; white-space: normal; }
.world-graph__help-section dd { color: rgb(var(--c-muted)); font-size: 0.7rem; line-height: 1.4; overflow-wrap: anywhere; }
.world-graph__toolbar button, .world-graph__menu button, .world-graph__creation button, .world-graph__modal button, .world-graph__status button { border: 1px solid rgb(var(--c-border)); border-radius: var(--radius-sm); background: rgb(var(--c-surface)); color: rgb(var(--c-fg)); font: inherit; cursor: pointer; }
.world-graph__toolbar button { min-width: 34px; height: 32px; padding: 0 var(--space-2); }
.world-graph__toolbar button:hover:not(:disabled), .world-graph__menu button:hover:not(:disabled), .world-graph__creation button:hover:not(:disabled), .world-graph__modal button:hover:not(:disabled) { background: rgb(var(--c-surface-hover)); border-color: rgb(var(--c-border-hover)); }
.world-graph button:disabled { opacity: 0.5; cursor: not-allowed; }
.world-graph__zoom { min-width: 64px !important; font-family: var(--font-monospace) !important; }
.world-graph__selection-summary, .world-graph__state, .world-graph__status { z-index: 10; border: 1px solid rgb(var(--c-border)); border-radius: var(--radius-md); background: rgb(var(--c-surface-raised) / 0.95); box-shadow: 0 5px 18px rgb(var(--c-shadow) / 0.16); }
.world-graph__selection-summary { justify-self: end; max-width: 100%; min-width: 0; box-sizing: border-box; padding: var(--space-2) var(--space-3); overflow-wrap: anywhere; font-size: 0.82rem; pointer-events: auto; }
.world-graph__state { position: absolute; left: 50%; bottom: calc(var(--space-3) + 3rem); transform: translateX(-50%); max-width: min(620px, calc(100% - 2rem)); padding: var(--space-2) var(--space-4); color: rgb(var(--c-muted)); text-align: center; pointer-events: none; }
.world-graph__status-layer { display: flex; justify-content: center; justify-self: center; width: 100%; min-width: 0; pointer-events: none; }
.world-graph__status { display: flex; align-items: center; gap: var(--space-3); max-width: 100%; box-sizing: border-box; padding: var(--space-2) var(--space-3); color: rgb(var(--c-fg)); opacity: 1; filter: blur(0); transform: translateY(0); pointer-events: auto; transition: opacity 240ms var(--ease-standard), filter 240ms var(--ease-standard), transform 240ms var(--ease-standard); }
.world-graph__status span { min-width: 0; overflow-wrap: anywhere; }
.world-graph__status.is-error { color: rgb(var(--c-danger-strong)); }
.world-graph__status.is-log { color: rgb(var(--c-fg)); }
.world-graph__status.is-dismissing { opacity: 0; filter: blur(7px); transform: translateY(-4px); pointer-events: none; }
.world-graph__status button { padding: var(--space-1) var(--space-2); }
.world-graph__menu, .world-graph__creation { position: absolute; z-index: var(--z-popover); box-sizing: border-box; width: 300px; max-width: calc(100% - 20px); padding: var(--space-2); border: 1px solid rgb(var(--c-border)); border-radius: var(--radius-md); background: rgb(var(--c-surface-raised)); box-shadow: 0 12px 34px rgb(var(--c-shadow-strong) / 0.3); }
.world-graph__menu { display: flex; flex-direction: column; gap: var(--space-1); max-height: calc(100% - 20px); overflow: auto; }
.world-graph__menu button { width: 100%; padding: 0.55rem 0.7rem; text-align: left; }
.world-graph__menu-field { display: grid; gap: 0.3rem; padding: 0.25rem var(--space-2); color: rgb(var(--c-muted)); font-size: 0.78rem; font-weight: 700; }
.world-graph__menu-field select { width: 100%; box-sizing: border-box; padding: 0.48rem 0.55rem; border: 1px solid rgb(var(--c-border)); border-radius: var(--radius-sm); background: rgb(var(--c-surface)); color: rgb(var(--c-fg)); font: inherit; }
.world-graph__lod-control { display: grid; gap: 0.4rem; color: rgb(var(--c-muted)); font-size: 0.78rem; font-weight: 700; }
.world-graph__lod-control > span { display: flex; justify-content: space-between; gap: var(--space-2); color: rgb(var(--c-fg)); }
.world-graph__lod-control output { color: rgb(var(--c-muted)); font-family: var(--font-monospace); }
.world-graph__lod-control input[type="range"] { width: 100%; margin: 0; accent-color: rgb(var(--c-accent)); }
.world-graph__lod-control small { color: rgb(var(--c-muted)); font-size: 0.68rem; font-weight: 550; line-height: 1.35; }
.world-graph__settings-group { margin-top: var(--space-2); border-top: 1px solid rgb(var(--c-border) / 0.7); }
.world-graph__settings-group summary { padding: var(--space-2) 0; color: rgb(var(--c-fg)); font-size: 0.78rem; font-weight: 750; cursor: pointer; user-select: none; }
.world-graph__settings-fields { display: grid; gap: var(--space-2); padding-bottom: var(--space-2); }
.world-graph__settings-field { display: grid; grid-template-columns: minmax(0, 1fr) 6.75rem; gap: 0.25rem var(--space-2); align-items: center; color: rgb(var(--c-fg)); font-size: 0.74rem; font-weight: 650; }
.world-graph__settings-field > span { display: flex; min-width: 0; align-items: baseline; justify-content: space-between; gap: var(--space-1); }
.world-graph__settings-unit { flex: none; color: rgb(var(--c-muted)); font-family: var(--font-monospace); font-size: 0.68rem; font-weight: 550; }
.world-graph__settings-field input[type="number"] { width: 100%; min-width: 0; box-sizing: border-box; padding: 0.35rem 0.45rem; border: 1px solid rgb(var(--c-border)); border-radius: var(--radius-sm); background: rgb(var(--c-surface)); color: rgb(var(--c-fg)); font: 650 0.72rem var(--font-monospace); }
.world-graph__settings-field input[type="number"]:focus { outline: none; border-color: rgb(var(--c-border-hover)); box-shadow: 0 0 0 2px rgb(var(--focus-ring-color) / 0.28); }
.world-graph__settings-field small { grid-column: 1 / -1; color: rgb(var(--c-muted)); font-size: 0.66rem; font-weight: 500; line-height: 1.3; }
.world-graph__settings-reset { width: 100%; margin-top: var(--space-2); padding: 0.48rem 0.65rem; border: 1px solid rgb(var(--c-border)); border-radius: var(--radius-sm); background: rgb(var(--c-surface)); color: rgb(var(--c-fg)); font: 700 0.75rem var(--font-primary); cursor: pointer; }
.world-graph__settings-reset:hover { border-color: rgb(var(--c-border-hover)); background: rgb(var(--c-surface-hover)); }
.world-graph__menu-check { display: flex; align-items: center; gap: 0.5rem; padding: 0.35rem var(--space-2); color: rgb(var(--c-fg)); font-size: 0.82rem; font-weight: 650; cursor: pointer; }
.world-graph__menu-check input { margin: 0; accent-color: rgb(var(--c-accent)); }
.world-graph__menu-heading { padding: var(--space-1) var(--space-2); color: rgb(var(--c-muted)); font-size: 0.75rem; font-weight: 750; text-transform: uppercase; letter-spacing: 0.055em; }
.world-graph__menu-separator { height: 1px; margin: var(--space-2) 0; background: rgb(var(--c-border) / 0.65); }
.world-graph .is-danger { border-color: rgb(var(--c-danger) / 0.5); color: rgb(var(--c-danger-strong)); }
.world-graph__creation { display: grid; gap: var(--space-3); }
.world-graph__creation label { font-weight: 700; }
.world-graph__creation input { width: 100%; box-sizing: border-box; padding: 0.6rem 0.7rem; border: 1px solid rgb(var(--c-border)); border-radius: var(--radius-sm); background: rgb(var(--c-surface)); color: rgb(var(--c-fg)); font: inherit; }
.world-graph__creation input:focus { outline: none; box-shadow: 0 0 0 3px rgb(var(--focus-ring-color) / 0.35); }
.world-graph__creation-actions, .world-graph__modal-actions { display: flex; justify-content: flex-end; gap: var(--space-2); }
.world-graph__creation-actions button, .world-graph__modal-actions button { padding: 0.55rem 0.85rem; }
.world-graph__modal-backdrop { position: absolute; inset: 0; z-index: var(--z-modal); display: grid; place-items: center; padding: var(--space-4); background: rgb(var(--c-shadow-strong) / 0.48); }
.world-graph__modal { width: min(480px, 100%); padding: var(--space-5); border: 1px solid rgb(var(--c-border)); border-radius: var(--radius-lg); background: rgb(var(--c-surface-raised)); box-shadow: 0 20px 50px rgb(var(--c-shadow-strong) / 0.38); }
.world-graph__modal h2 { margin: 0 0 var(--space-3); font-size: 1.15rem; }
.world-graph__modal p { margin: 0 0 var(--space-5); color: rgb(var(--c-muted)); line-height: 1.5; }
@keyframes world-graph-dash { to { stroke-dashoffset: -24; } }
@media (prefers-reduced-motion: reduce) { .world-graph__region, .world-graph__region-surface, .world-graph__location-surface { transition: none; } .world-graph__world.is-pending .world-graph__world-outline, .world-graph__region.is-pending .world-graph__region-surface, .world-graph__location.is-pending .world-graph__location-surface, .world-graph__location.is-radius-pending .world-graph__location-radius-marker { animation: none; } }
</style>
