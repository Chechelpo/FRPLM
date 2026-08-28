import {
    computed,
    shallowRef,
    watch,
    type ComputedRef,
    type Ref,
} from "vue";
import type {
    Location,
    Position,
    Region,
    RegionGeometry,
    World,
} from "@frplm/host-sdk";
import type {
    CanvasScalePersistResult,
    LocationPreviewMap,
    LocationRadiusPersistResult,
    LocationRadiusState,
    MovementPersistResult,
    MovementPlan,
    RegionPreviewMap,
    SelectedGraphEntity,
    WorldCanvasGeometry,
    WorldGraphData,
} from "../types.js";
import {
    circleInsideGeometry,
    getAbsoluteLocationPosition,
    getAbsoluteRegionGeometry,
    isRegionDescendantOf,
    locationEntityKey,
    marqueeGeometry,
    regionEntityKey,
    type RegionIndex,
} from "../utils/geometry.js";
import {
    entityKey,
    getEffectiveMovingLocations,
    getEffectiveMovingRegions,
    getMovingSubtreeKeys,
    getRegionSubtree,
    locationIsInRegionSubtree,
    makeMovementPreviews,
    marqueeEntityKeys,
    resolveLocationParent,
    resolveRegionParent,
    validateCompletePlacement,
    type LocationDegreeIndex,
    type RegionsByParent,
} from "../utils/graph.js";
import {
    buildCanvasScalePlan,
    buildRegionScalePreview,
    buildWorldScalePreview,
    validateScalePreview,
    type ScalePreview,
} from "../utils/scaling.js";
import {
    calculateAutomaticLocationRadius,
    clampLocationRadiusToRegion,
    maximumValidLocationRadius,
    readPersistedLocationRadius,
    resolveEffectiveLocationRadius,
} from "../utils/locationRadius.js";
import {
    movedPastDragThreshold,
    resolvePrimaryModifierMode,
} from "../utils/interaction.js";
import {
    locationParentDeltaConstraint,
    regionParentDeltaConstraint,
    resolveParentWhileLatched,
    resolveReparentResistance,
    type MovementDeltaConstraint,
} from "../utils/reparentResistance.js";
import {
    zoomAwareLocationMinimumRadius,
    zoomAwareRegionMinimum,
    zoomAwareRegionPadding,
    zoomAwareWorldPadding,
} from "../utils/sizing.js";
import {
    radialResizeScale,
    scaleRegionFromCenter,
    scaleWorldCanvasFromCenter,
} from "../utils/resizeGesture.js";
import type {WorldGraphViewerSettings} from "../utils/viewerSettings.js";
import {
    MIN_WORLD_HEIGHT,
    MIN_WORLD_WIDTH,
} from "../constants.js";

export type WorldGraphInteractionOptions = {
    world: ComputedRef<World>;
    worldId: ComputedRef<number>;
    hostRef: Ref<HTMLElement | null>;
    graph: Ref<WorldGraphData>;
    worldCanvas: ComputedRef<WorldCanvasGeometry>;
    regionIndex: ComputedRef<RegionIndex>;
    regionsByParent: ComputedRef<RegionsByParent>;
    locationDegreeIndex: ComputedRef<LocationDegreeIndex>;
    regionsInRenderOrder: ComputedRef<readonly Region[]>;
    visibleRegions: ComputedRef<readonly Region[]>;
    visibleLocations: ComputedRef<readonly Location[]>;
    visibleRegionKeys: ComputedRef<ReadonlySet<string>>;
    visibleLocationKeys: ComputedRef<ReadonlySet<string>>;
    viewerSettings: Ref<WorldGraphViewerSettings>;
    pan: Ref<Position>;
    zoom: Ref<number>;
    clientToScreen: (clientX: number, clientY: number) => Position;
    screenToWorld: (screen: Position) => Position;
    worldToScreen: (world: Position) => Position;
    setPan: (next: Position, persist?: boolean) => void;
    persistViewport: () => void;
    persistMovement: (plan: MovementPlan) => Promise<MovementPersistResult>;
    persistLocationRadius: (location: Location, radius: number | null) => Promise<LocationRadiusPersistResult>;
    persistCanvasScale: (plan: ReturnType<typeof buildCanvasScalePlan>) => Promise<CanvasScalePersistResult>;
    dismissPopovers: () => void;
    isModalOpen: () => boolean;
    setInteractionMessage: (message: string | null) => void;
    isWorldExternallyPending: () => boolean;
    isRegionExternallyPending: (region: Region) => boolean;
    isLocationExternallyPending: (location: Location) => boolean;
    isRegionLocked: (region: Region) => boolean;
    isLocationLocked: (location: Location) => boolean;
};

type PointerBase = {
    worldId: number;
    pointerId: number;
    startScreen: Position;
    startWorld: Position;
    dragged: boolean;
};

type PanPointer = PointerBase & {
    mode: "pan";
    startPan: Position;
};

type MarqueePointer = PointerBase & {
    mode: "marquee";
    additive: boolean;
    baseSelection: ReadonlySet<string>;
};

type TogglePointer = PointerBase & {
    mode: "toggle";
    entity: SelectedGraphEntity | null;
};

type WorldClickPointer = PointerBase & {
    mode: "world-click";
};

type ParentLatch = {
    parent: Region;
    constraint: MovementDeltaConstraint;
};

type MovePointer = PointerBase & {
    mode: "move";
    clicked: SelectedGraphEntity;
    collapseOnClick: boolean;
    effectiveRegions: readonly Region[];
    effectiveLocations: readonly Location[];
    baseRegionPreviews: RegionPreviewMap;
    baseLocationPreviews: LocationPreviewMap;
    capturedLocationRadii: ReadonlyMap<string, number>;
    parentLatches: ReadonlyMap<string, ParentLatch>;
    releasedParentKeys: ReadonlySet<string>;
    latestDelta: Position;
};

type ResizeGestureBase = {
    worldId: number;
    startPointerScreen: Position;
    targetCenterScreen: Position;
    dirty: boolean;
};

type LocationResizeGesture = ResizeGestureBase & {
    kind: "location";
    location: Location;
    containingRegion: Region;
    center: Position;
    initialEffectiveRadius: number;
    maximumRadius: number;
    latestRadius: number;
};

type RegionResizeGesture = ResizeGestureBase & {
    kind: "region";
    region: Region;
    startGeometry: RegionGeometry;
    affectedRegions: readonly Region[];
    affectedLocations: readonly Location[];
    baseRegionPreviews: RegionPreviewMap;
    baseLocationPreviews: LocationPreviewMap;
    latestGeometry: RegionGeometry;
    valid: boolean;
};

type WorldResizeGesture = ResizeGestureBase & {
    kind: "world";
    startCanvas: WorldCanvasGeometry;
    baseRegionPreviews: RegionPreviewMap;
    baseLocationPreviews: LocationPreviewMap;
    latestCanvas: WorldCanvasGeometry;
    valid: boolean;
};

type ActivePointer =
    | PanPointer
    | MarqueePointer
    | TogglePointer
    | WorldClickPointer
    | MovePointer;

type KeyboardResizeGesture =
    | LocationResizeGesture
    | RegionResizeGesture
    | WorldResizeGesture;

function mergeMaps<K, V>(
    base: ReadonlyMap<K, V>,
    override: ReadonlyMap<K, V>,
): ReadonlyMap<K, V> {
    if (override.size === 0) return base;
    const result = new Map(base);
    for (const [key, value] of override) result.set(key, value);
    return result;
}

function removeMapKeys<K, V>(source: ReadonlyMap<K, V>, keys: Iterable<K>): ReadonlyMap<K, V> {
    const result = new Map(source);
    for (const key of keys) result.delete(key);
    return result;
}

function addSetValues<T>(source: ReadonlySet<T>, values: Iterable<T>): ReadonlySet<T> {
    const result = new Set(source);
    for (const value of values) result.add(value);
    return result;
}

function removeSetValues<T>(source: ReadonlySet<T>, values: Iterable<T>): ReadonlySet<T> {
    const result = new Set(source);
    for (const value of values) result.delete(value);
    return result;
}

export function useWorldGraphInteractions(options: WorldGraphInteractionOptions) {
    const regionPadding = () => zoomAwareRegionPadding(
        options.zoom.value,
        options.viewerSettings.value.regionContentPaddingScreen,
    );
    const worldPadding = () => zoomAwareWorldPadding(
        options.zoom.value,
        options.viewerSettings.value.worldContentPaddingScreen,
    );
    const locationMinimumRadius = () => zoomAwareLocationMinimumRadius(
        options.zoom.value,
        options.viewerSettings.value.locationMinResizeScreenRadius,
    );
    const regionMinimum = () => zoomAwareRegionMinimum(
        options.zoom.value,
        options.viewerSettings.value.regionMinResizeScreenWidth,
        options.viewerSettings.value.regionMinResizeScreenHeight,
    );

    const selectedKeys = shallowRef<ReadonlySet<string>>(new Set());
    const marqueeRect = shallowRef<RegionGeometry | null>(null);
    const marqueePreviewKeys = shallowRef<ReadonlySet<string>>(new Set());

    const activeRegionPreviews = shallowRef<RegionPreviewMap>(new Map());
    const activeLocationPreviews = shallowRef<LocationPreviewMap>(new Map());
    const pendingRegionPreviews = shallowRef<RegionPreviewMap>(new Map());
    const pendingLocationPreviews = shallowRef<LocationPreviewMap>(new Map());
    const activeLocationRadiusPreviews = shallowRef<ReadonlyMap<string, number>>(new Map());
    const pendingLocationRadiusPreviews = shallowRef<ReadonlyMap<string, number>>(new Map());
    const failedRadiusLocationKeys = shallowRef<ReadonlySet<string>>(new Set());
    const activeWorldCanvas = shallowRef<WorldCanvasGeometry | null>(null);
    const pendingWorldCanvas = shallowRef<WorldCanvasGeometry | null>(null);

    const candidateLocationParents = shallowRef<ReadonlyMap<string, Region | null>>(new Map());
    const candidateRegionParents = shallowRef<ReadonlyMap<string, Region | null>>(new Map());
    const rootCandidateKeys = shallowRef<ReadonlySet<string>>(new Set());
    const pendingCandidateBatches = shallowRef<ReadonlyMap<number, ReadonlySet<string>>>(new Map());
    const pendingRootCandidateBatches = shallowRef<ReadonlyMap<number, ReadonlySet<string>>>(new Map());
    const invalidLocationKeys = shallowRef<ReadonlySet<string>>(new Set());
    const invalidRegionKeys = shallowRef<ReadonlySet<string>>(new Set());

    const pendingLocationKeys = shallowRef<ReadonlySet<string>>(new Set());
    const pendingRegionKeys = shallowRef<ReadonlySet<string>>(new Set());
    const failedLocationKeys = shallowRef<ReadonlySet<string>>(new Set());
    const failedRegionKeys = shallowRef<ReadonlySet<string>>(new Set());
    const worldScalePending = shallowRef(false);
    const worldScaleFailed = shallowRef(false);
    const activePointer = shallowRef<ActivePointer | null>(null);
    const keyboardResize = shallowRef<KeyboardResizeGesture | null>(null);

    const locationByKey = computed(() => new Map(
        options.graph.value.locations.map((location) => [locationEntityKey(location), location]),
    ));
    const regionByKey = computed(() => new Map(
        options.graph.value.regions.map((region) => [regionEntityKey(region), region]),
    ));

    const regionPreviews = computed<RegionPreviewMap>(() => mergeMaps(
        pendingRegionPreviews.value,
        activeRegionPreviews.value,
    ));
    const locationPreviews = computed<LocationPreviewMap>(() => mergeMaps(
        pendingLocationPreviews.value,
        activeLocationPreviews.value,
    ));
    const locationRadiusState = computed<LocationRadiusState>(() => ({
        activePreviewRadius: activeLocationRadiusPreviews.value,
        pendingRadius: pendingLocationRadiusPreviews.value,
    }));
    const displayedWorldCanvas = computed(() => (
        activeWorldCanvas.value ?? pendingWorldCanvas.value ?? options.worldCanvas.value
    ));

    const selectedLocations = computed(() => [...selectedKeys.value]
        .map((key) => locationByKey.value.get(key))
        .filter((value): value is Location => value !== undefined));
    const selectedRegions = computed(() => [...selectedKeys.value]
        .map((key) => regionByKey.value.get(key))
        .filter((value): value is Region => value !== undefined));
    const selectedEntities = computed<readonly SelectedGraphEntity[]>(() => {
        const result: SelectedGraphEntity[] = [];
        for (const key of selectedKeys.value) {
            const location = locationByKey.value.get(key);
            if (location !== undefined) result.push({kind: "location", location});
            else {
                const region = regionByKey.value.get(key);
                if (region !== undefined) result.push({kind: "region", region});
            }
        }
        return result;
    });
    const selectionSummary = computed(() => ({
        locations: selectedLocations.value.length,
        regions: selectedRegions.value.length,
        total: selectedKeys.value.size,
    }));

    function effectiveLocationRadius(location: Location): number {
        const degree = options.locationDegreeIndex.value.get(locationEntityKey(location)) ?? 0;
        return resolveEffectiveLocationRadius(location, degree, locationRadiusState.value);
    }

    const displayedSelectionKeys = computed<ReadonlySet<string>>(() => {
        const pointer = activePointer.value;
        if (pointer?.mode !== "marquee" || !pointer.dragged) return selectedKeys.value;
        if (!pointer.additive) return marqueePreviewKeys.value;
        return addSetValues(pointer.baseSelection, marqueePreviewKeys.value);
    });

    const candidateRegionKeys = computed<ReadonlySet<string>>(() => {
        const result = new Set<string>();
        for (const parent of candidateLocationParents.value.values()) {
            if (parent !== null) result.add(regionEntityKey(parent));
        }
        for (const parent of candidateRegionParents.value.values()) {
            if (parent !== null) result.add(regionEntityKey(parent));
        }
        for (const batch of pendingCandidateBatches.value.values()) {
            for (const key of batch) result.add(key);
        }
        return result;
    });

    const displayedRootCandidateKeys = computed<ReadonlySet<string>>(() => {
        const result = new Set(rootCandidateKeys.value);
        for (const batch of pendingRootCandidateBatches.value.values()) {
            for (const key of batch) result.add(key);
        }
        return result;
    });

    const hasPendingPersistence = computed(() => (
        worldScalePending.value ||
        pendingRegionKeys.value.size > 0 ||
        pendingLocationKeys.value.size > 0
    ));

    function capturePointer(pointerId: number): void {
        try {
            options.hostRef.value?.setPointerCapture(pointerId);
        } catch {
            // Pointer capture can fail when the host was detached between events.
        }
    }

    function releasePointer(pointerId: number): void {
        try {
            if (options.hostRef.value?.hasPointerCapture(pointerId)) {
                options.hostRef.value.releasePointerCapture(pointerId);
            }
        } catch {
            // The browser may already have released capture.
        }
        if (activePointer.value?.pointerId === pointerId) activePointer.value = null;
    }

    function entityFromTarget(target: EventTarget | null): SelectedGraphEntity | null {
        if (!(target instanceof Element)) return null;
        const element = target.closest<HTMLElement>("[data-graph-entity-key]");
        if (element === null) return null;
        const key = element.dataset.graphEntityKey ?? "";
        if (element.dataset.graphEntityKind === "location") {
            const location = locationByKey.value.get(key);
            return location === undefined ? null : {kind: "location", location};
        }
        if (element.dataset.graphEntityKind === "region") {
            const region = regionByKey.value.get(key);
            return region === undefined ? null : {kind: "region", region};
        }
        return null;
    }

    function isEntityVisible(entity: SelectedGraphEntity): boolean {
        return entity.kind === "location"
            ? options.visibleLocationKeys.value.has(locationEntityKey(entity.location))
            : options.visibleRegionKeys.value.has(regionEntityKey(entity.region));
    }

    function selectOnly(entity: SelectedGraphEntity): void {
        if (!isEntityVisible(entity)) return;
        selectedKeys.value = new Set([entityKey(entity)]);
    }

    function toggle(entity: SelectedGraphEntity): void {
        const key = entityKey(entity);
        const next = new Set(selectedKeys.value);
        if (next.has(key)) next.delete(key);
        else if (isEntityVisible(entity)) next.add(key);
        selectedKeys.value = next;
    }

    function clearSelection(): void {
        selectedKeys.value = new Set();
    }

    function removeEntityFromSelection(entity: SelectedGraphEntity): void {
        const next = new Set(selectedKeys.value);
        next.delete(entityKey(entity));
        selectedKeys.value = next;
    }

    function pruneSelection(validKeys: ReadonlySet<string>): void {
        selectedKeys.value = new Set([...selectedKeys.value].filter((key) => validKeys.has(key)));
    }

    function prepareEntityContext(entity: SelectedGraphEntity): void {
        if (!selectedKeys.value.has(entityKey(entity))) selectOnly(entity);
    }

    function isLocationSelected(location: Location): boolean {
        return selectedKeys.value.has(locationEntityKey(location));
    }

    function isRegionSelected(region: Region): boolean {
        return selectedKeys.value.has(regionEntityKey(region));
    }

    function isLocationPreviewSelected(location: Location): boolean {
        return displayedSelectionKeys.value.has(locationEntityKey(location)) &&
            !selectedKeys.value.has(locationEntityKey(location));
    }

    function isRegionPreviewSelected(region: Region): boolean {
        return displayedSelectionKeys.value.has(regionEntityKey(region)) &&
            !selectedKeys.value.has(regionEntityKey(region));
    }

    function isLocationPending(location: Location): boolean {
        return pendingLocationKeys.value.has(locationEntityKey(location)) ||
            options.isLocationExternallyPending(location);
    }

    function isRegionPending(region: Region): boolean {
        return pendingRegionKeys.value.has(regionEntityKey(region)) ||
            options.isRegionExternallyPending(region);
    }

    function isLocationFailed(location: Location): boolean {
        return failedLocationKeys.value.has(locationEntityKey(location));
    }

    function isLocationRadiusFailed(location: Location): boolean {
        return failedRadiusLocationKeys.value.has(locationEntityKey(location));
    }

    function isLocationRadiusPending(location: Location): boolean {
        return pendingLocationRadiusPreviews.value.has(locationEntityKey(location));
    }

    function isLocationRadiusResizing(location: Location): boolean {
        const resize = keyboardResize.value;
        return resize?.kind === "location" &&
            resize.location.equals(location);
    }

    function isRegionFailed(region: Region): boolean {
        return failedRegionKeys.value.has(regionEntityKey(region));
    }

    function isLocationInvalid(location: Location): boolean {
        return invalidLocationKeys.value.has(locationEntityKey(location));
    }

    function isRegionInvalid(region: Region): boolean {
        return invalidRegionKeys.value.has(regionEntityKey(region));
    }

    function isRegionCandidate(region: Region): boolean {
        return candidateRegionKeys.value.has(regionEntityKey(region));
    }

    function isRootCandidate(region: Region): boolean {
        return displayedRootCandidateKeys.value.has(regionEntityKey(region));
    }

    function isEntityMoving(key: string): boolean {
        const pointer = activePointer.value;
        return pointer?.mode === "move" && pointer.dragged && (
            activeRegionPreviews.value.has(key) || activeLocationPreviews.value.has(key)
        );
    }

    function isRegionScaling(region: Region): boolean {
        const resize = keyboardResize.value;
        return resize?.kind === "region" && resize.region.equals(region);
    }

    function isWorldScaling(): boolean {
        return keyboardResize.value?.kind === "world";
    }

    function clearActivePreview(): void {
        activeRegionPreviews.value = new Map();
        activeLocationPreviews.value = new Map();
        activeLocationRadiusPreviews.value = new Map();
        activeWorldCanvas.value = null;
        candidateLocationParents.value = new Map();
        candidateRegionParents.value = new Map();
        rootCandidateKeys.value = new Set();
        invalidLocationKeys.value = new Set();
        invalidRegionKeys.value = new Set();
    }

    function externalUnavailableRegionKeys(): ReadonlySet<string> {
        const result = new Set<string>(pendingRegionKeys.value);
        for (const region of options.graph.value.regions) {
            if (
                region.get("collapsed") ||
                options.isRegionExternallyPending(region)
            ) {
                result.add(regionEntityKey(region));
            }
        }
        return result;
    }

    function selectionHasPendingEntity(): boolean {
        return selectedEntities.value.some((entity) => entity.kind === "location"
            ? isLocationPending(entity.location)
            : isRegionPending(entity.region));
    }

    function regionSubtreeHasPendingLocation(region: Region): boolean {
        return options.graph.value.locations.some((location) => (
            isLocationPending(location) &&
            locationIsInRegionSubtree(location, region, options.regionIndex.value)
        ));
    }

    function startBase(event: PointerEvent): PointerBase {
        const startScreen = options.clientToScreen(event.clientX, event.clientY);
        return {
            worldId: options.worldId.value,
            pointerId: event.pointerId,
            startScreen,
            startWorld: options.screenToWorld(startScreen),
            dragged: false,
        };
    }

    function onHostPointerDownCapture(event: PointerEvent): void {
        if (
            event.button !== 0 || options.isModalOpen() ||
            activePointer.value !== null || keyboardResize.value !== null
        ) return;
        options.dismissPopovers();
        const base = startBase(event);

        const modifierMode = resolvePrimaryModifierMode(event);
        if (modifierMode === null) return;

        event.preventDefault();
        event.stopPropagation();
        if (modifierMode === "pan") {
            activePointer.value = {...base, mode: "pan", startPan: {...options.pan.value}};
        } else if (modifierMode === "marquee") {
            activePointer.value = {
                ...base,
                mode: "marquee",
                additive: event.ctrlKey,
                baseSelection: new Set(selectedKeys.value),
            };
        } else {
            activePointer.value = {
                ...base,
                mode: "toggle",
                entity: entityFromTarget(event.target),
            };
        }
        capturePointer(event.pointerId);
    }

    function onWorldPointerDown(event: PointerEvent): void {
        if (
            event.button !== 0 || event.altKey || event.shiftKey || event.ctrlKey ||
            options.isModalOpen() || activePointer.value !== null ||
            keyboardResize.value !== null
        ) return;
        options.dismissPopovers();
        activePointer.value = {...startBase(event), mode: "world-click"};
        capturePointer(event.pointerId);
    }

    function onEntityPointerDown(event: PointerEvent, clicked: SelectedGraphEntity): void {
        if (
            event.button !== 0 || event.altKey || event.shiftKey || event.ctrlKey ||
            options.isModalOpen() || activePointer.value !== null ||
            keyboardResize.value !== null ||
            worldScalePending.value
        ) return;

        const clickedPending = clicked.kind === "location"
            ? isLocationPending(clicked.location)
            : isRegionPending(clicked.region);
        if (clickedPending) return;

        event.preventDefault();
        options.dismissPopovers();
        const key = entityKey(clicked);
        const wasSelected = selectedKeys.value.has(key);
        const collapseOnClick = wasSelected && selectedKeys.value.size > 1;
        if (!wasSelected) selectOnly(clicked);

        if (selectionHasPendingEntity()) return;

        const effectiveRegions = getEffectiveMovingRegions(selectedRegions.value, options.regionIndex.value);
        const effectiveLocations = getEffectiveMovingLocations(
            selectedLocations.value,
            effectiveRegions,
            options.regionIndex.value,
        );
        const lockedRelativeMover =
            effectiveRegions.some(options.isRegionLocked) ||
            effectiveLocations.some(options.isLocationLocked);
        if (lockedRelativeMover) {
            options.setInteractionMessage(
                "Locked nodes cannot move relative to their parent. Move an ancestor instead or unlock them first.",
            );
            return;
        }

        if (effectiveRegions.some(regionSubtreeHasPendingLocation)) {
            options.setInteractionMessage(
                "A region containing a location-size update cannot be moved until that update finishes.",
            );
            return;
        }

        const baseRegionPreviews = new Map(pendingRegionPreviews.value);
        const baseLocationPreviews = new Map(pendingLocationPreviews.value);
        const capturedLocationRadii = new Map(effectiveLocations.map((location) => [
            locationEntityKey(location),
            effectiveLocationRadius(location),
        ]));
        const parentLatches = new Map<string, ParentLatch>();

        for (const region of effectiveRegions) {
            const parentId = region.get("parent_region_id");
            if (parentId === null) continue;
            const parent = options.regionIndex.value.get(parentId);
            if (parent === undefined) continue;
            parentLatches.set(regionEntityKey(region), {
                parent,
                constraint: regionParentDeltaConstraint(
                    getAbsoluteRegionGeometry(region, options.regionIndex.value, baseRegionPreviews),
                    getAbsoluteRegionGeometry(parent, options.regionIndex.value, baseRegionPreviews),
                    regionPadding(),
                ),
            });
        }

        for (const location of effectiveLocations) {
            const parentId = location.get("region_id");
            if (parentId === null) continue;
            const parent = options.regionIndex.value.get(parentId);
            const radius = capturedLocationRadii.get(locationEntityKey(location));
            if (parent === undefined || radius === undefined) continue;
            parentLatches.set(locationEntityKey(location), {
                parent,
                constraint: locationParentDeltaConstraint(
                    getAbsoluteLocationPosition(
                        location,
                        options.regionIndex.value,
                        baseRegionPreviews,
                        baseLocationPreviews,
                    ),
                    radius,
                    getAbsoluteRegionGeometry(parent, options.regionIndex.value, baseRegionPreviews),
                    regionPadding(),
                ),
            });
        }

        activePointer.value = {
            ...startBase(event),
            mode: "move",
            clicked,
            collapseOnClick,
            effectiveRegions,
            effectiveLocations,
            baseRegionPreviews,
            baseLocationPreviews,
            capturedLocationRadii,
            parentLatches,
            releasedParentKeys: new Set(),
            latestDelta: {x: 0, y: 0},
        };
        failedLocationKeys.value = new Set();
        failedRegionKeys.value = new Set();
        capturePointer(event.pointerId);
    }

    function onLocationPointerDown(event: PointerEvent, location: Location): void {
        onEntityPointerDown(event, {kind: "location", location});
    }

    function beginLocationResize(
        location: Location,
        startPointerScreen: Position,
    ): boolean {
        if (isLocationPending(location)) return false;
        const regionId = location.get("region_id");
        const containingRegion = regionId === null
            ? null
            : options.regionIndex.value.get(regionId) ?? null;
        if (containingRegion === null || isRegionPending(containingRegion)) {
            options.setInteractionMessage("The location cannot be resized without an available containing region.");
            return false;
        }

        const center = getAbsoluteLocationPosition(
            location,
            options.regionIndex.value,
            regionPreviews.value,
            locationPreviews.value,
        );
        const containingGeometry = getAbsoluteRegionGeometry(
            containingRegion,
            options.regionIndex.value,
            regionPreviews.value,
        );
        const maximumRadius = maximumValidLocationRadius(
            center,
            containingGeometry,
            regionPadding(),
        );
        const initialEffectiveRadius = effectiveLocationRadius(location);
        const effectiveMinimumRadius = Math.min(
            locationMinimumRadius(),
            initialEffectiveRadius,
        );
        if (maximumRadius < effectiveMinimumRadius) {
            options.setInteractionMessage("The containing region does not provide enough space to resize this location.");
            return false;
        }

        keyboardResize.value = {
            kind: "location",
            worldId: options.worldId.value,
            startPointerScreen: {...startPointerScreen},
            targetCenterScreen: options.worldToScreen(center),
            dirty: false,
            location,
            containingRegion,
            center,
            initialEffectiveRadius,
            maximumRadius,
            latestRadius: initialEffectiveRadius,
        };
        failedRadiusLocationKeys.value = new Set(
            [...failedRadiusLocationKeys.value].filter((key) => key !== locationEntityKey(location)),
        );
        return true;
    }

    function onRegionPointerDown(event: PointerEvent, region: Region): void {
        onEntityPointerDown(event, {kind: "region", region});
    }

    function beginRegionResize(
        region: Region,
        startPointerScreen: Position,
    ): boolean {
        if (region.get("collapsed")) {
            options.setInteractionMessage("Expand the region before resizing it.");
            return false;
        }
        if (isRegionPending(region) || worldScalePending.value) return false;

        const affectedRegions = getRegionSubtree(region, options.regionsByParent.value);
        const affectedLocations = options.graph.value.locations.filter((location) => (
            locationIsInRegionSubtree(location, region, options.regionIndex.value)
        ));
        if (
            affectedRegions.some(isRegionPending) ||
            affectedLocations.some(isLocationPending)
        ) {
            options.setInteractionMessage("The region cannot be resized while it or its descendants have pending changes.");
            return false;
        }

        const baseRegionPreviews = new Map(pendingRegionPreviews.value);
        const baseLocationPreviews = new Map(pendingLocationPreviews.value);
        const startGeometry = getAbsoluteRegionGeometry(region, options.regionIndex.value, baseRegionPreviews);
        keyboardResize.value = {
            kind: "region",
            worldId: options.worldId.value,
            startPointerScreen: {...startPointerScreen},
            targetCenterScreen: options.worldToScreen(startGeometry),
            dirty: false,
            region,
            startGeometry,
            affectedRegions,
            affectedLocations,
            baseRegionPreviews,
            baseLocationPreviews,
            latestGeometry: startGeometry,
            valid: true,
        };
        failedLocationKeys.value = new Set();
        failedRegionKeys.value = new Set();
        return true;
    }

    function beginWorldResize(startPointerScreen: Position): boolean {
        if (hasPendingPersistence.value || options.isWorldExternallyPending()) return false;
        const startCanvas = options.worldCanvas.value;
        keyboardResize.value = {
            kind: "world",
            worldId: options.worldId.value,
            startPointerScreen: {...startPointerScreen},
            targetCenterScreen: options.worldToScreen({x: 0, y: 0}),
            dirty: false,
            startCanvas,
            baseRegionPreviews: new Map(pendingRegionPreviews.value),
            baseLocationPreviews: new Map(pendingLocationPreviews.value),
            latestCanvas: startCanvas,
            valid: true,
        };
        worldScaleFailed.value = false;
        return true;
    }

    function beginKeyboardResize(startPointerScreen: Position | null): boolean {
        if (
            startPointerScreen === null ||
            options.isModalOpen() ||
            activePointer.value !== null ||
            keyboardResize.value !== null
        ) {
            if (startPointerScreen === null) {
                options.setInteractionMessage("Move the pointer over the graph before starting Shift+S resize.");
            }
            return false;
        }

        const selection = selectedEntities.value;
        if (selection.length > 1) {
            options.setInteractionMessage("Shift+S resize requires no selected node for the world, or exactly one selected region/location.");
            return false;
        }

        options.dismissPopovers();
        options.setInteractionMessage(null);
        if (selection.length === 0) return beginWorldResize(startPointerScreen);
        const target = selection[0];
        return target.kind === "location"
            ? beginLocationResize(target.location, startPointerScreen)
            : beginRegionResize(target.region, startPointerScreen);
    }

    function updateMovement(pointer: MovePointer, currentWorld: Position): void {
        const requestedDelta = {
            x: currentWorld.x - pointer.startWorld.x,
            y: currentWorld.y - pointer.startWorld.y,
        };
        const resistance = resolveReparentResistance(
            requestedDelta,
            [...pointer.parentLatches].map(([key, latch]) => ({
                key,
                constraint: latch.constraint,
            })),
            pointer.releasedParentKeys,
            options.zoom.value,
            options.viewerSettings.value.reparentReleaseScreenPixels,
        );
        pointer.releasedParentKeys = resistance.releasedKeys;
        pointer.latestDelta = resistance.delta;
        const preview = makeMovementPreviews(
            pointer.effectiveRegions,
            pointer.effectiveLocations,
            options.graph.value.regions,
            options.graph.value.locations,
            options.regionIndex.value,
            options.regionsByParent.value,
            pointer.latestDelta,
            pointer.baseRegionPreviews,
            pointer.baseLocationPreviews,
        );
        const combinedRegions = mergeMaps(pointer.baseRegionPreviews, preview.regions);
        const combinedLocations = mergeMaps(pointer.baseLocationPreviews, preview.locations);
        const movementRadiusState: LocationRadiusState = {
            activePreviewRadius: pointer.capturedLocationRadii,
            pendingRadius: locationRadiusState.value.pendingRadius,
        };
        const externallyUnavailable = externalUnavailableRegionKeys();
        const movingSubtreeKeys = getMovingSubtreeKeys(pointer.effectiveRegions, options.regionsByParent.value);
        const unavailableRegionParents = addSetValues(externallyUnavailable, movingSubtreeKeys);
        const regionParents = new Map<string, Region | null>();
        const locationParents = new Map<string, Region | null>();
        const invalidRegions = new Set<string>();
        const invalidLocations = new Set<string>();
        const rootCandidates = new Set<string>();

        for (const region of pointer.effectiveRegions) {
            const key = regionEntityKey(region);
            const geometry = preview.regions.get(key);
            if (geometry === undefined) {
                invalidRegions.add(key);
                continue;
            }
            const latch = pointer.parentLatches.get(key);
            const parent = resolveRegionParent(
                region,
                geometry,
                options.visibleRegions.value,
                options.regionIndex.value,
                combinedRegions,
                unavailableRegionParents,
                regionPadding(),
            );
            const effectiveParent = latch !== undefined && !pointer.releasedParentKeys.has(key)
                ? resolveParentWhileLatched(
                    latch.parent,
                    parent,
                    (candidate, latchedParent) => isRegionDescendantOf(
                        candidate,
                        latchedParent,
                        options.regionIndex.value,
                    ),
                )
                : parent;
            regionParents.set(key, effectiveParent);
            if (effectiveParent === null) {
                const validRoot = validateCompletePlacement(
                    [region],
                    [],
                    new Map([[key, null]]),
                    new Map(),
                    combinedRegions,
                    combinedLocations,
                    options.regionIndex.value,
                    displayedWorldCanvas.value,
                    options.locationDegreeIndex.value,
                    movementRadiusState,
                    regionPadding(),
                    worldPadding(),
                );
                if (validRoot) rootCandidates.add(key);
                else invalidRegions.add(key);
            }
        }

        for (const location of pointer.effectiveLocations) {
            const key = locationEntityKey(location);
            const center = preview.locations.get(key);
            if (center === undefined) {
                invalidLocations.add(key);
                continue;
            }
            const latch = pointer.parentLatches.get(key);
            const parent = resolveLocationParent(
                location,
                center,
                options.visibleRegions.value,
                options.regionIndex.value,
                options.locationDegreeIndex.value,
                combinedRegions,
                externallyUnavailable,
                movementRadiusState,
                regionPadding(),
            );
            const effectiveParent = latch !== undefined && !pointer.releasedParentKeys.has(key)
                ? resolveParentWhileLatched(
                    latch.parent,
                    parent,
                    (candidate, latchedParent) => isRegionDescendantOf(
                        candidate,
                        latchedParent,
                        options.regionIndex.value,
                    ),
                )
                : parent;
            locationParents.set(key, effectiveParent);
            if (effectiveParent === null) invalidLocations.add(key);
        }

        activeRegionPreviews.value = preview.regions;
        activeLocationPreviews.value = preview.locations;
        candidateRegionParents.value = regionParents;
        candidateLocationParents.value = locationParents;
        invalidRegionKeys.value = invalidRegions;
        invalidLocationKeys.value = invalidLocations;
        rootCandidateKeys.value = rootCandidates;
    }

    function gestureScale(
        resize: KeyboardResizeGesture,
        currentScreen: Position,
    ): number {
        return radialResizeScale(
            resize.startPointerScreen,
            currentScreen,
            resize.targetCenterScreen,
            options.viewerSettings.value.resizeGestureDoublingScreenPixels,
        );
    }

    function updateLocationResize(
        resize: LocationResizeGesture,
        currentScreen: Position,
    ): void {
        const containingGeometry = getAbsoluteRegionGeometry(
            resize.containingRegion,
            options.regionIndex.value,
            regionPreviews.value,
        );
        const minimumRadius = Math.min(
            locationMinimumRadius(),
            resize.initialEffectiveRadius,
        );
        const candidate = Math.min(
            resize.maximumRadius,
            resize.initialEffectiveRadius * gestureScale(resize, currentScreen),
        );
        const clamped = clampLocationRadiusToRegion(
            candidate,
            resize.center,
            containingGeometry,
            regionPadding(),
            minimumRadius,
        );
        if (clamped === null) return;
        resize.latestRadius = Math.min(clamped, resize.maximumRadius);
        resize.dirty = resize.latestRadius !== resize.initialEffectiveRadius;
        activeLocationRadiusPreviews.value = new Map(
            activeLocationRadiusPreviews.value,
        ).set(locationEntityKey(resize.location), resize.latestRadius);
    }

    function updateRegionResize(
        resize: RegionResizeGesture,
        currentScreen: Position,
    ): void {
        const configuredMinimum = regionMinimum();
        const nextGeometry = scaleRegionFromCenter(
            resize.startGeometry,
            gestureScale(resize, currentScreen),
            {
                width: Math.min(configuredMinimum.width, resize.startGeometry.width),
                height: Math.min(configuredMinimum.height, resize.startGeometry.height),
            },
        );
        const preview = buildRegionScalePreview(
            resize.region,
            nextGeometry,
            options.graph.value.locations,
            options.regionIndex.value,
            options.regionsByParent.value,
            resize.baseRegionPreviews,
            resize.baseLocationPreviews,
        );
        const combined: ScalePreview = {
            regions: mergeMaps(resize.baseRegionPreviews, preview.regions),
            locations: mergeMaps(resize.baseLocationPreviews, preview.locations),
        };
        resize.latestGeometry = nextGeometry;
        resize.dirty = nextGeometry.width !== resize.startGeometry.width ||
            nextGeometry.height !== resize.startGeometry.height;
        resize.valid = validateScalePreview(
            options.graph.value.regions,
            options.graph.value.locations,
            options.regionIndex.value,
            options.locationDegreeIndex.value,
            displayedWorldCanvas.value,
            combined,
            locationRadiusState.value,
            regionPadding(),
            worldPadding(),
        );
        activeRegionPreviews.value = preview.regions;
        activeLocationPreviews.value = preview.locations;
        invalidRegionKeys.value = resize.valid ? new Set() : new Set([regionEntityKey(resize.region)]);
    }

    function updateWorldResize(
        resize: WorldResizeGesture,
        currentScreen: Position,
    ): void {
        const nextCanvas = scaleWorldCanvasFromCenter(
            resize.startCanvas,
            gestureScale(resize, currentScreen),
            {
                width: Math.min(MIN_WORLD_WIDTH, resize.startCanvas.width),
                height: Math.min(MIN_WORLD_HEIGHT, resize.startCanvas.height),
            },
        );
        const preview = buildWorldScalePreview(
            resize.startCanvas,
            nextCanvas,
            options.graph.value.regions,
            options.graph.value.locations,
            options.regionIndex.value,
            resize.baseRegionPreviews,
            resize.baseLocationPreviews,
        );
        const combined: ScalePreview = {
            regions: mergeMaps(resize.baseRegionPreviews, preview.regions),
            locations: mergeMaps(resize.baseLocationPreviews, preview.locations),
        };
        resize.latestCanvas = nextCanvas;
        resize.dirty = nextCanvas.width !== resize.startCanvas.width ||
            nextCanvas.height !== resize.startCanvas.height;
        resize.valid = validateScalePreview(
            options.graph.value.regions,
            options.graph.value.locations,
            options.regionIndex.value,
            options.locationDegreeIndex.value,
            nextCanvas,
            combined,
            locationRadiusState.value,
            regionPadding(),
            worldPadding(),
        );
        activeWorldCanvas.value = nextCanvas;
        activeRegionPreviews.value = preview.regions;
        activeLocationPreviews.value = preview.locations;
        invalidRegionKeys.value = resize.valid
            ? new Set()
            : new Set(options.graph.value.regions.map(regionEntityKey));
    }

    function updateKeyboardResize(currentScreen: Position): void {
        const resize = keyboardResize.value;
        if (resize === null) return;
        switch (resize.kind) {
            case "location":
                updateLocationResize(resize, currentScreen);
                break;
            case "region":
                updateRegionResize(resize, currentScreen);
                break;
            case "world":
                updateWorldResize(resize, currentScreen);
                break;
        }
    }

    function onPointerMove(event: PointerEvent): void {
        const currentScreen = options.clientToScreen(event.clientX, event.clientY);
        if (keyboardResize.value !== null) {
            event.preventDefault();
            updateKeyboardResize(currentScreen);
            return;
        }
        const pointer = activePointer.value;
        if (pointer === null || pointer.pointerId !== event.pointerId) return;
        event.preventDefault();
        const currentWorld = options.screenToWorld(currentScreen);
        if (!pointer.dragged && movedPastDragThreshold(
            pointer.startScreen,
            currentScreen,
            options.viewerSettings.value.dragThresholdScreenPixels,
        )) {
            pointer.dragged = true;
        }
        if (!pointer.dragged) return;

        switch (pointer.mode) {
            case "pan":
                options.setPan({
                    x: pointer.startPan.x + currentScreen.x - pointer.startScreen.x,
                    y: pointer.startPan.y + currentScreen.y - pointer.startScreen.y,
                });
                break;
            case "marquee": {
                const rect = marqueeGeometry(pointer.startWorld, currentWorld);
                marqueeRect.value = rect;
                marqueePreviewKeys.value = marqueeEntityKeys(
                    rect,
                    options.visibleRegions.value,
                    options.visibleLocations.value,
                    options.regionIndex.value,
                    options.locationDegreeIndex.value,
                    regionPreviews.value,
                    locationPreviews.value,
                    locationRadiusState.value,
                    options.zoom.value,
                    options.viewerSettings.value.collapsedRegionScreenWidth,
                    options.viewerSettings.value.collapsedRegionScreenHeight,
                );
                break;
            }
            case "move":
                updateMovement(pointer, currentWorld);
                break;
            case "toggle":
            case "world-click":
                break;
        }
    }

    function transferActivePreviewToPending(): {
        regions: RegionPreviewMap;
        locations: LocationPreviewMap;
    } {
        const regions = new Map(activeRegionPreviews.value);
        const locations = new Map(activeLocationPreviews.value);
        pendingRegionPreviews.value = mergeMaps(pendingRegionPreviews.value, regions);
        pendingLocationPreviews.value = mergeMaps(pendingLocationPreviews.value, locations);
        activeRegionPreviews.value = new Map();
        activeLocationPreviews.value = new Map();
        return {regions, locations};
    }

    async function persistRadiusPreview(
        location: Location,
        targetRadius: number | null,
        previewRadius: number,
        expectedWorldId: number,
    ): Promise<boolean> {
        const key = locationEntityKey(location);
        pendingLocationRadiusPreviews.value = new Map(
            pendingLocationRadiusPreviews.value,
        ).set(key, previewRadius);
        pendingLocationKeys.value = addSetValues(pendingLocationKeys.value, [key]);
        failedRadiusLocationKeys.value = removeSetValues(failedRadiusLocationKeys.value, [key]);

        try {
            const result = await options.persistLocationRadius(location, targetRadius);
            if (options.worldId.value !== expectedWorldId) return result.ok;
            if (!result.ok) {
                failedRadiusLocationKeys.value = addSetValues(
                    failedRadiusLocationKeys.value,
                    [key],
                );
                return false;
            }

            const confirmedPreview = result.radius === null
                ? calculateAutomaticLocationRadius(
                    options.locationDegreeIndex.value.get(key) ?? 0,
                )
                : result.radius;
            pendingLocationRadiusPreviews.value = new Map(
                pendingLocationRadiusPreviews.value,
            ).set(key, confirmedPreview);
            return true;
        } finally {
            if (options.worldId.value === expectedWorldId) {
                pendingLocationRadiusPreviews.value = removeMapKeys(
                    pendingLocationRadiusPreviews.value,
                    [key],
                );
                pendingLocationKeys.value = removeSetValues(
                    pendingLocationKeys.value,
                    [key],
                );
            }
        }
    }

    async function commitLocationRadius(
        pointer: LocationResizeGesture,
    ): Promise<void> {
        const key = locationEntityKey(pointer.location);
        activeLocationRadiusPreviews.value = removeMapKeys(
            activeLocationRadiusPreviews.value,
            [key],
        );

        const currentLocation = options.graph.value.locations.find((location) => (
            location.equals(pointer.location)
        ));
        const currentRegion = options.regionIndex.value.get(pointer.containingRegion.get("id"));
        if (
            options.worldId.value !== pointer.worldId ||
            currentLocation === undefined ||
            currentRegion === undefined ||
            currentLocation.get("region_id") !== currentRegion.get("id")
        ) return;

        const currentCenter = getAbsoluteLocationPosition(
            currentLocation,
            options.regionIndex.value,
            regionPreviews.value,
            locationPreviews.value,
        );
        const regionGeometry = getAbsoluteRegionGeometry(
            currentRegion,
            options.regionIndex.value,
            regionPreviews.value,
        );
        const centerUnchanged = Math.hypot(
            currentCenter.x - pointer.center.x,
            currentCenter.y - pointer.center.y,
        ) < 0.001;
        const valid = centerUnchanged &&
            Number.isFinite(pointer.latestRadius) &&
            circleInsideGeometry(
                pointer.center,
                pointer.latestRadius,
                regionGeometry,
                regionPadding(),
            );
        if (!valid) {
            failedRadiusLocationKeys.value = addSetValues(
                failedRadiusLocationKeys.value,
                [key],
            );
            options.setInteractionMessage("The location cannot be resized to that radius.");
            return;
        }

        await persistRadiusPreview(
            currentLocation,
            pointer.latestRadius,
            pointer.latestRadius,
            pointer.worldId,
        );
    }

    async function resetLocationRadius(location: Location): Promise<boolean> {
        options.dismissPopovers();
        if (
            readPersistedLocationRadius(location) === null ||
            isLocationPending(location) ||
            options.worldId.value !== location.get("worldID")
        ) return false;

        const regionId = location.get("region_id");
        const region = regionId === null
            ? null
            : options.regionIndex.value.get(regionId) ?? null;
        if (region === null || isRegionPending(region)) {
            options.setInteractionMessage("Automatic sizing cannot be restored without an available containing region.");
            return false;
        }

        const key = locationEntityKey(location);
        const automaticRadius = calculateAutomaticLocationRadius(
            options.locationDegreeIndex.value.get(key) ?? 0,
        );
        const center = getAbsoluteLocationPosition(
            location,
            options.regionIndex.value,
            regionPreviews.value,
            locationPreviews.value,
        );
        const geometry = getAbsoluteRegionGeometry(
            region,
            options.regionIndex.value,
            regionPreviews.value,
        );
        if (!circleInsideGeometry(
            center,
            automaticRadius,
            geometry,
            regionPadding(),
        )) {
            options.setInteractionMessage("Automatic sizing does not fit inside the containing region.");
            return false;
        }

        return persistRadiusPreview(
            location,
            null,
            automaticRadius,
            options.worldId.value,
        );
    }

    async function commitMovement(pointer: MovePointer): Promise<void> {
        const regionParents = new Map(candidateRegionParents.value);
        const locationParents = new Map(candidateLocationParents.value);
        const combinedRegions = regionPreviews.value;
        const combinedLocations = locationPreviews.value;
        const movementRadiusState: LocationRadiusState = {
            activePreviewRadius: pointer.capturedLocationRadii,
            pendingRadius: locationRadiusState.value.pendingRadius,
        };
        const valid = invalidRegionKeys.value.size === 0 &&
            invalidLocationKeys.value.size === 0 &&
            validateCompletePlacement(
                pointer.effectiveRegions,
                pointer.effectiveLocations,
                regionParents,
                locationParents,
                combinedRegions,
                combinedLocations,
                options.regionIndex.value,
                displayedWorldCanvas.value,
                options.locationDegreeIndex.value,
                movementRadiusState,
                regionPadding(),
                worldPadding(),
            );

        if (!valid) {
            failedRegionKeys.value = new Set(pointer.effectiveRegions.map(regionEntityKey));
            failedLocationKeys.value = new Set(pointer.effectiveLocations.map(locationEntityKey));
            clearActivePreview();
            options.setInteractionMessage("The selected entities cannot be placed at that position.");
            return;
        }

        const plan: MovementPlan = {
            worldId: pointer.worldId,
            regions: pointer.effectiveRegions.map((region) => {
                const key = regionEntityKey(region);
                const geometry = combinedRegions.get(key);
                const parent = regionParents.get(key);
                if (geometry === undefined || parent === undefined) throw new Error("Incomplete region movement preview");
                const parentGeometry = parent === null
                    ? {x: 0, y: 0}
                    : getAbsoluteRegionGeometry(parent, options.regionIndex.value, combinedRegions);
                return {
                    region,
                    parent,
                    absoluteCenter: {x: geometry.x, y: geometry.y},
                    localCenter: {x: geometry.x - parentGeometry.x, y: geometry.y - parentGeometry.y},
                };
            }),
            locations: pointer.effectiveLocations.map((location) => {
                const key = locationEntityKey(location);
                const absoluteCenter = combinedLocations.get(key);
                const region = locationParents.get(key);
                if (absoluteCenter === undefined || region == null) throw new Error("Incomplete location movement preview");
                const parentGeometry = getAbsoluteRegionGeometry(region, options.regionIndex.value, combinedRegions);
                return {
                    location,
                    region,
                    absoluteCenter,
                    localCenter: {
                        x: absoluteCenter.x - parentGeometry.x,
                        y: absoluteCenter.y - parentGeometry.y,
                    },
                };
            }),
        };

        const pendingPreview = transferActivePreviewToPending();
        const pendingCandidates = new Set<string>();
        for (const parent of regionParents.values()) if (parent !== null) pendingCandidates.add(regionEntityKey(parent));
        for (const parent of locationParents.values()) if (parent !== null) pendingCandidates.add(regionEntityKey(parent));
        pendingCandidateBatches.value = new Map(pendingCandidateBatches.value).set(pointer.pointerId, pendingCandidates);
        pendingRootCandidateBatches.value = new Map(pendingRootCandidateBatches.value).set(pointer.pointerId, new Set(rootCandidateKeys.value));
        candidateLocationParents.value = new Map();
        candidateRegionParents.value = new Map();
        rootCandidateKeys.value = new Set();
        invalidLocationKeys.value = new Set();
        invalidRegionKeys.value = new Set();
        pendingRegionKeys.value = addSetValues(pendingRegionKeys.value, pendingPreview.regions.keys());
        pendingLocationKeys.value = addSetValues(pendingLocationKeys.value, pendingPreview.locations.keys());

        try {
            const result = await options.persistMovement(plan);
            if (options.worldId.value !== pointer.worldId) return;
            failedRegionKeys.value = new Set(result.regions.failed.map((update) => regionEntityKey(update.region)));
            failedLocationKeys.value = new Set(result.locations.failed.map((update) => locationEntityKey(update.location)));
        } finally {
            if (options.worldId.value === pointer.worldId) {
                pendingRegionPreviews.value = removeMapKeys(pendingRegionPreviews.value, pendingPreview.regions.keys());
                pendingLocationPreviews.value = removeMapKeys(pendingLocationPreviews.value, pendingPreview.locations.keys());
                pendingRegionKeys.value = removeSetValues(pendingRegionKeys.value, pendingPreview.regions.keys());
                pendingLocationKeys.value = removeSetValues(pendingLocationKeys.value, pendingPreview.locations.keys());
                const nextCandidates = new Map(pendingCandidateBatches.value);
                nextCandidates.delete(pointer.pointerId);
                pendingCandidateBatches.value = nextCandidates;
                const nextRoots = new Map(pendingRootCandidateBatches.value);
                nextRoots.delete(pointer.pointerId);
                pendingRootCandidateBatches.value = nextRoots;
            }
        }
    }

    async function commitRegionScale(pointer: RegionResizeGesture): Promise<void> {
        if (!pointer.valid) {
            failedRegionKeys.value = new Set([regionEntityKey(pointer.region)]);
            clearActivePreview();
            options.setInteractionMessage("The region cannot be scaled to that size or position.");
            return;
        }
        const preview = transferActivePreviewToPending();
        const completePreview: ScalePreview = {
            regions: mergeMaps(pointer.baseRegionPreviews, preview.regions),
            locations: mergeMaps(pointer.baseLocationPreviews, preview.locations),
        };
        const plan = buildCanvasScalePlan(
            options.world.value,
            null,
            pointer.affectedRegions,
            pointer.affectedLocations,
            options.regionIndex.value,
            completePreview,
        );
        pendingRegionKeys.value = addSetValues(pendingRegionKeys.value, preview.regions.keys());
        pendingLocationKeys.value = addSetValues(pendingLocationKeys.value, preview.locations.keys());
        invalidRegionKeys.value = new Set();

        try {
            const result = await options.persistCanvasScale(plan);
            if (options.worldId.value !== pointer.worldId) return;
            failedRegionKeys.value = new Set(result.regions.failed.map((update) => regionEntityKey(update.region)));
            failedLocationKeys.value = new Set(result.locations.failed.map((update) => locationEntityKey(update.location)));
        } finally {
            if (options.worldId.value === pointer.worldId) {
                pendingRegionPreviews.value = removeMapKeys(pendingRegionPreviews.value, preview.regions.keys());
                pendingLocationPreviews.value = removeMapKeys(pendingLocationPreviews.value, preview.locations.keys());
                pendingRegionKeys.value = removeSetValues(pendingRegionKeys.value, preview.regions.keys());
                pendingLocationKeys.value = removeSetValues(pendingLocationKeys.value, preview.locations.keys());
            }
        }
    }

    async function commitWorldScale(pointer: WorldResizeGesture): Promise<void> {
        if (!pointer.valid || activeWorldCanvas.value === null) {
            worldScaleFailed.value = true;
            clearActivePreview();
            options.setInteractionMessage("The world canvas cannot be scaled to that size.");
            return;
        }
        const nextCanvas = activeWorldCanvas.value;
        const preview = transferActivePreviewToPending();
        pendingWorldCanvas.value = nextCanvas;
        activeWorldCanvas.value = null;
        const completePreview: ScalePreview = {
            regions: mergeMaps(pointer.baseRegionPreviews, preview.regions),
            locations: mergeMaps(pointer.baseLocationPreviews, preview.locations),
        };
        const plan = buildCanvasScalePlan(
            options.world.value,
            nextCanvas,
            options.graph.value.regions,
            options.graph.value.locations,
            options.regionIndex.value,
            completePreview,
        );
        worldScalePending.value = true;
        pendingRegionKeys.value = addSetValues(pendingRegionKeys.value, preview.regions.keys());
        pendingLocationKeys.value = addSetValues(pendingLocationKeys.value, preview.locations.keys());
        invalidRegionKeys.value = new Set();

        try {
            const result = await options.persistCanvasScale(plan);
            if (options.worldId.value !== pointer.worldId) return;
            worldScaleFailed.value = result.worldSucceeded === false;
            failedRegionKeys.value = new Set(result.regions.failed.map((update) => regionEntityKey(update.region)));
            failedLocationKeys.value = new Set(result.locations.failed.map((update) => locationEntityKey(update.location)));
        } finally {
            if (options.worldId.value === pointer.worldId) {
                pendingWorldCanvas.value = null;
                worldScalePending.value = false;
                pendingRegionPreviews.value = removeMapKeys(pendingRegionPreviews.value, preview.regions.keys());
                pendingLocationPreviews.value = removeMapKeys(pendingLocationPreviews.value, preview.locations.keys());
                pendingRegionKeys.value = removeSetValues(pendingRegionKeys.value, preview.regions.keys());
                pendingLocationKeys.value = removeSetValues(pendingLocationKeys.value, preview.locations.keys());
            }
        }
    }

    function onPointerUp(event: PointerEvent): void {
        const pointer = activePointer.value;
        if (pointer === null || pointer.pointerId !== event.pointerId) return;
        event.preventDefault();
        releasePointer(event.pointerId);

        switch (pointer.mode) {
            case "pan":
                if (pointer.dragged) options.persistViewport();
                break;
            case "marquee":
                if (pointer.dragged) {
                    selectedKeys.value = pointer.additive
                        ? addSetValues(pointer.baseSelection, marqueePreviewKeys.value)
                        : new Set(marqueePreviewKeys.value);
                }
                marqueeRect.value = null;
                marqueePreviewKeys.value = new Set();
                break;
            case "toggle":
                if (!pointer.dragged && pointer.entity !== null) toggle(pointer.entity);
                break;
            case "world-click":
                if (!pointer.dragged) clearSelection();
                break;
            case "move":
                if (pointer.dragged) void commitMovement(pointer);
                else if (pointer.collapseOnClick) selectOnly(pointer.clicked);
                break;
        }
    }

    function endKeyboardResize(): boolean {
        const resize = keyboardResize.value;
        if (resize === null) return false;
        keyboardResize.value = null;

        if (!resize.dirty) {
            clearActivePreview();
            return true;
        }

        switch (resize.kind) {
            case "location":
                void commitLocationRadius(resize);
                break;
            case "region":
                void commitRegionScale(resize);
                break;
            case "world":
                void commitWorldScale(resize);
                break;
        }
        return true;
    }

    function cancelKeyboardResize(): boolean {
        if (keyboardResize.value === null) return false;
        keyboardResize.value = null;
        clearActivePreview();
        return true;
    }

    function cancelActivePointer(restorePan = true): void {
        const pointer = activePointer.value;
        if (pointer === null) return;
        if (pointer.mode === "pan" && restorePan) options.setPan(pointer.startPan, false);
        marqueeRect.value = null;
        marqueePreviewKeys.value = new Set();
        clearActivePreview();
        releasePointer(pointer.pointerId);
    }

    function onPointerCancel(event: PointerEvent): void {
        if (activePointer.value?.pointerId !== event.pointerId) return;
        cancelActivePointer();
    }

    function onEscape(): void {
        if (options.isModalOpen()) return;
        const cancelledResize = cancelKeyboardResize();
        cancelActivePointer();
        options.dismissPopovers();
        if (!cancelledResize) clearSelection();
        options.setInteractionMessage(null);
    }

    watch(
        () => [
            ...options.visibleRegionKeys.value,
            ...options.visibleLocationKeys.value,
        ].sort().join("|"),
        () => {
            const valid = new Set([
                ...options.visibleRegionKeys.value,
                ...options.visibleLocationKeys.value,
            ]);
            pruneSelection(valid);
        },
    );

    watch(
        () => [...selectedKeys.value].sort().join("|"),
        () => {
            const resize = keyboardResize.value;
            if (resize === null) return;
            const selectionMatches = resize.kind === "world"
                ? selectedKeys.value.size === 0
                : selectedKeys.value.size === 1 && selectedKeys.value.has(
                    resize.kind === "location"
                        ? locationEntityKey(resize.location)
                        : regionEntityKey(resize.region),
                );
            if (!selectionMatches) cancelKeyboardResize();
        },
    );

    watch(options.worldId, () => {
        cancelKeyboardResize();
        cancelActivePointer(false);
        clearSelection();
        marqueeRect.value = null;
        marqueePreviewKeys.value = new Set();
        activeRegionPreviews.value = new Map();
        activeLocationPreviews.value = new Map();
        pendingRegionPreviews.value = new Map();
        pendingLocationPreviews.value = new Map();
        activeLocationRadiusPreviews.value = new Map();
        pendingLocationRadiusPreviews.value = new Map();
        failedRadiusLocationKeys.value = new Set();
        activeWorldCanvas.value = null;
        pendingWorldCanvas.value = null;
        candidateLocationParents.value = new Map();
        candidateRegionParents.value = new Map();
        rootCandidateKeys.value = new Set();
        pendingCandidateBatches.value = new Map();
        pendingRootCandidateBatches.value = new Map();
        invalidLocationKeys.value = new Set();
        invalidRegionKeys.value = new Set();
        pendingLocationKeys.value = new Set();
        pendingRegionKeys.value = new Set();
        failedLocationKeys.value = new Set();
        failedRegionKeys.value = new Set();
        worldScalePending.value = false;
        worldScaleFailed.value = false;
    });

    return {
        selectedKeys,
        displayedSelectionKeys,
        selectedEntities,
        selectedLocations,
        selectedRegions,
        selectionSummary,
        marqueeRect,
        marqueePreviewKeys,
        regionPreviews,
        locationPreviews,
        locationRadiusState,
        displayedWorldCanvas,
        candidateLocationParents,
        candidateRegionParents,
        candidateRegionKeys,
        rootCandidateKeys,
        invalidLocationKeys,
        invalidRegionKeys,
        pendingLocationKeys,
        pendingRegionKeys,
        failedLocationKeys,
        failedRegionKeys,
        worldScalePending,
        worldScaleFailed,
        activePointer,
        keyboardResize,
        hasPendingPersistence,
        isLocationSelected,
        isRegionSelected,
        isLocationPreviewSelected,
        isRegionPreviewSelected,
        isLocationPending,
        isRegionPending,
        isLocationFailed,
        isLocationRadiusFailed,
        isLocationRadiusPending,
        isLocationRadiusResizing,
        effectiveLocationRadius,
        isRegionFailed,
        isLocationInvalid,
        isRegionInvalid,
        isRegionCandidate,
        isRootCandidate,
        isEntityMoving,
        isRegionScaling,
        isWorldScaling,
        selectOnly,
        toggle,
        clearSelection,
        removeEntityFromSelection,
        pruneSelection,
        prepareEntityContext,
        onHostPointerDownCapture,
        onWorldPointerDown,
        onLocationPointerDown,
        onRegionPointerDown,
        beginKeyboardResize,
        endKeyboardResize,
        cancelKeyboardResize,
        onPointerMove,
        onPointerUp,
        onPointerCancel,
        onEscape,
        resetLocationRadius,
        dispose: () => {
            cancelKeyboardResize();
            cancelActivePointer(false);
        },
    };
}
