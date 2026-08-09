<script setup lang="ts">
import {
    computed,
    getCurrentInstance,
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
    RegionGeometry,
} from "@/domain/World";
import {useLocationContextGraphData} from "./composables/useLocationContextGraphData";
import {useWorldGraphBackgroundAsset} from "./composables/useWorldGraphBackgrounds";
import {useWorldGraphViewport} from "./composables/useWorldGraphViewport";
import type {
    BackgroundTarget,
    LocationContextNodeRenderModel,
} from "./types";
import {
    clampOverlayPosition,
    edgeArrowWorldSize,
    normalizedOpacity,
    preserveAspectRatio,
    splitLocationLabel,
} from "./utils/rendering";
import {publishEdgeTraversability, subscribeEdgeTraversability, synchronizeEdgeTraversability} from "./utils/edgeSync";
import {
    buildLocationContextEdgeModels,
    buildLocationContextNodeModels,
    locationContextSceneGeometry,
} from "./utils/locationContext";

const props = defineProps<{
    location: Location;
}>();

const emit = defineEmits<{
    /** The user selected a directly reachable neighbouring location. */
    (event: "destinationIntent", location: Location): void;
    /** User-facing feedback for an attempted movement that cannot be made. */
    (event: "message", message: string): void;
}>();

const hostRef = ref<HTMLElement | null>(null);
const locationRef = computed(() => props.location);
const worldId = computed(() => props.location.get("worldID"));
const data = useLocationContextGraphData(locationRef);
const viewport = useWorldGraphViewport(
    worldId,
    hostRef,
    {persist: false},
);

const backgroundTarget = computed<BackgroundTarget | null>(() => {
    const region = data.projection.value?.currentRegion;
    return region === undefined
        ? null
        : {kind: "region", region};
});
const background = useWorldGraphBackgroundAsset(backgroundTarget);

const instanceId = getCurrentInstance()?.uid ?? 0;
const clipId = `location-context-clip-${instanceId}`;
const arrowMarkerId = `location-context-arrow-${instanceId}`;
const blockedMarkerId = `location-context-blocked-${instanceId}`;
const edgeArrowMarkerSize = computed(() => edgeArrowWorldSize(viewport.zoom.value));

const nodes = computed(() => {
    const projection = data.projection.value;
    return projection === null
        ? []
        : buildLocationContextNodeModels(projection);
});

const edges = computed(() => {
    const projection = data.projection.value;
    return projection === null
        ? []
        : buildLocationContextEdgeModels(
            projection,
            nodes.value,
        );
});

const movementMessage = shallowRef<string | null>(null);
const intendedDestinationKey = shallowRef<string | null>(null);

const neighboursByDestinationId = computed(() => new Map(
    (data.projection.value?.neighbours ?? []).map(neighbour => [
        neighbour.destination.get("id"),
        neighbour,
    ]),
));

watch(
    () => [
        props.location.get("worldID"),
        props.location.get("id"),
    ] as const,
    () => {
        movementMessage.value = null;
        intendedDestinationKey.value = null;
    },
);

watch(
    () => data.projection.value?.neighbours.map(({edge}) => (
        `${edge.get("world_id")}:${edge.get("from_id")}:${edge.get("to_id")}:${Number(edge.get("is_traversable"))}`
    )).join("|") ?? "",
    () => {
        for (const neighbour of data.projection.value?.neighbours ?? []) {
            publishEdgeTraversability(neighbour.edge);
        }
    },
    {flush: "sync"},
);

let stopEdgeTraversabilitySync: () => void = () => undefined;

onMounted(() => {
    stopEdgeTraversabilitySync = subscribeEdgeTraversability((change) => {
        const projection = data.projection.value;
        if (projection === null) return;
        synchronizeEdgeTraversability(
            projection.neighbours.map(neighbour => neighbour.edge),
            change,
        );
    });
});

onBeforeUnmount(() => {
    stopEdgeTraversabilitySync();
});

const currentRegionGeometry = computed<RegionGeometry | null>(() => {
    const region = data.projection.value?.currentRegion;
    if (region === undefined) return null;
    return {
        x: 0,
        y: 0,
        width: region.get("width"),
        height: region.get("height"),
    };
});

const currentRegionRect = computed(() => {
    const geometry = currentRegionGeometry.value;
    if (geometry === null) return null;
    return {
        x: -geometry.width / 2,
        y: -geometry.height / 2,
        width: geometry.width,
        height: geometry.height,
    };
});

const sceneGeometry = computed<RegionGeometry | null>(() => {
    const region = currentRegionGeometry.value;
    return region === null
        ? null
        : locationContextSceneGeometry(
            region,
            nodes.value,
        );
});

const regionBackgroundVisible = computed(() => {
    const region = data.projection.value?.currentRegion;
    return region !== undefined &&
        region.get("background_visible") &&
        background.url.value !== null;
});

const regionBackgroundOpacity = computed(() => {
    const region = data.projection.value?.currentRegion;
    return region === undefined
        ? 1
        : normalizedOpacity(
            region.get("background_opacity"),
        );
});

const regionBackgroundFit = computed(() => {
    const region = data.projection.value?.currentRegion;
    return region === undefined
        ? "xMidYMid meet"
        : preserveAspectRatio(
            region.get("background_fit"),
        );
});

const hoveredNodeKey = shallowRef<string | null>(null);
const focusedNodeKey = shallowRef<string | null>(null);
const activeNodeKey = computed(
    () => focusedNodeKey.value ?? hoveredNodeKey.value,
);
const activeNode = computed<LocationContextNodeRenderModel | null>(() => {
    const key = activeNodeKey.value;
    if (key === null) return null;
    return nodes.value.find(node => node.key === key) ?? null;
});
const visibleNodeInformation = computed(() => {
    const node = activeNode.value;
    if (node === null) return null;
    const description = node.description?.trim() ?? "";
    if (description.length === 0) return null;
    return {
        name: node.displayName,
        description,
    };
});

const nodeInformationStyle = computed(() => {
    const node = activeNode.value;
    if (node === null) return {};
    const screen = viewport.worldToScreen({
        x: node.position.x,
        y: node.position.y + node.radius,
    });
    const position = clampOverlayPosition(
        {
            x: screen.x + 12,
            y: screen.y + 12,
        },
        viewport.hostSize.width,
        viewport.hostSize.height,
        260,
        140,
        10,
    );
    return {
        left: `${position.x}px`,
        top: `${position.y}px`,
    };
});

function nodeLabelLines(
    node: LocationContextNodeRenderModel,
): readonly string[] {
    return splitLocationLabel(
        node.displayName,
        node.radius,
    );
}

function nodeMovementAccessibleLabel(
    node: LocationContextNodeRenderModel,
): string {
    if (node.current) return node.accessibleLabel;
    const neighbour = neighboursByDestinationId.value.get(
        node.location.get("id"),
    );
    if (neighbour === undefined) return node.accessibleLabel;
    return neighbour.edge.get("is_traversable")
        ? `${node.accessibleLabel}. Select as destination.`
        : `${node.accessibleLabel}. This path is not traversable.`;
}

function selectDestination(node: LocationContextNodeRenderModel): void {
    // The current location is context, never a movement destination.
    if (node.current) return;

    const neighbour = neighboursByDestinationId.value.get(
        node.location.get("id"),
    );
    if (neighbour === undefined) return;

    if (!neighbour.edge.get("is_traversable")) {
        intendedDestinationKey.value = null;
        const message = `The path to ${node.displayName} is not traversable.`;
        movementMessage.value = message;
        emit("message", message);
        return;
    }

    movementMessage.value = null;
    intendedDestinationKey.value = node.key;
    emit("destinationIntent", neighbour.destination);
}

function onActivationKey(
    event: KeyboardEvent,
    action: () => void,
): void {
    if (event.key !== "Enter" && event.key !== " ") return;
    event.preventDefault();
    action();
}

function onNodeKeydown(
    event: KeyboardEvent,
    node: LocationContextNodeRenderModel,
): void {
    if (node.current) return;
    onActivationKey(event, () => selectDestination(node));
}

function fitScene(): void {
    const geometry = sceneGeometry.value;
    if (geometry === null) return;
    viewport.fitGeometry(geometry, 24);
}

function zoomIn(): void {
    viewport.zoomBy(1.2);
}

function zoomOut(): void {
    viewport.zoomBy(1 / 1.2);
}

type PanPointer = {
    pointerId: number;
    lastScreen: Position;
};

const panPointer = shallowRef<PanPointer | null>(null);

function onPointerDown(event: PointerEvent): void {
    if (event.button !== 0) return;
    const target = event.target;
    if (
        target instanceof Element &&
        target.closest("[data-context-action]") !== null
    ) return;

    const host = hostRef.value;
    if (host === null) return;
    panPointer.value = {
        pointerId: event.pointerId,
        lastScreen: viewport.clientToScreen(
            event.clientX,
            event.clientY,
        ),
    };
    host.setPointerCapture(event.pointerId);
}

function onPointerMove(event: PointerEvent): void {
    const pointer = panPointer.value;
    if (
        pointer === null ||
        pointer.pointerId !== event.pointerId
    ) return;

    const current = viewport.clientToScreen(
        event.clientX,
        event.clientY,
    );
    viewport.setPan({
        x: viewport.pan.value.x +
            current.x - pointer.lastScreen.x,
        y: viewport.pan.value.y +
            current.y - pointer.lastScreen.y,
    });
    pointer.lastScreen = current;
}

function finishPan(event: PointerEvent): void {
    const pointer = panPointer.value;
    if (
        pointer === null ||
        pointer.pointerId !== event.pointerId
    ) return;

    const host = hostRef.value;
    if (host?.hasPointerCapture(event.pointerId)) {
        host.releasePointerCapture(event.pointerId);
    }
    panPointer.value = null;
}

watch(
    () => [
        sceneGeometry.value?.x,
        sceneGeometry.value?.y,
        sceneGeometry.value?.width,
        sceneGeometry.value?.height,
        viewport.hostSize.width,
        viewport.hostSize.height,
    ],
    async () => {
        await nextTick();
        fitScene();
    },
    {immediate: true},
);
</script>

<template>
    <section
        ref="hostRef"
        class="location-context-graph"
        :class="{'location-context-graph--panning': panPointer !== null}"
        aria-label="Location context map"
        @pointerdown="onPointerDown"
        @pointermove="onPointerMove"
        @pointerup="finishPan"
        @pointercancel="finishPan"
        @wheel.prevent="viewport.handleWheel"
    >
        <header data-context-action class="location-context-graph__header" @pointerdown.stop>
            <div class="location-context-graph__identity">
                <strong>
                    {{
                        data.projection.value
                            ?.currentRegion.get("name")
                        ?? "Location context"
                    }}
                </strong>
                <span v-if="data.projection.value !== null">
                    {{ nodes.length - 1 }} outgoing
                    {{ nodes.length - 1 === 1 ? "neighbour" : "neighbours" }}
                </span>
            </div>

            <div
                class="location-context-graph__controls"
                aria-label="Map view controls"
            >
                <button
                    type="button"
                    title="Zoom out"
                    aria-label="Zoom out"
                    @click="zoomOut"
                >
                    −
                </button>
                <span aria-live="polite">
                    {{ viewport.zoomPercentage.value }}
                </span>
                <button
                    type="button"
                    title="Zoom in"
                    aria-label="Zoom in"
                    @click="zoomIn"
                >
                    +
                </button>
                <button
                    type="button"
                    title="Fit region and neighbours"
                    aria-label="Fit region and neighbours"
                    @click="fitScene"
                >
                    Fit
                </button>
            </div>
        </header>

        <div
            v-if="data.isLoading.value"
            class="location-context-graph__state"
            role="status"
        >
            Loading location context…
        </div>

        <div
            v-else-if="data.loadError.value !== null"
            class="location-context-graph__state location-context-graph__state--error"
            role="alert"
        >
            {{ data.loadError.value }}
        </div>

        <svg
            v-else-if="data.projection.value !== null"
            class="location-context-graph__viewport"
            :viewBox="viewport.viewportViewBox.value"
            role="img"
            :aria-label="
                `Map context for ${props.location.get('name')}`
            "
        >
            <defs>
                <clipPath :id="clipId">
                    <rect
                        v-if="currentRegionRect !== null"
                        :x="currentRegionRect.x"
                        :y="currentRegionRect.y"
                        :width="currentRegionRect.width"
                        :height="currentRegionRect.height"
                    />
                </clipPath>

                <marker
                    :id="arrowMarkerId"
                    viewBox="0 0 10 10"
                    refX="9"
                    refY="5"
                    :markerWidth="edgeArrowMarkerSize"
                    :markerHeight="edgeArrowMarkerSize"
                    markerUnits="userSpaceOnUse"
                    orient="auto-start-reverse"
                >
                    <path class="location-context-marker__arrow" d="M 0 0 L 10 5 L 0 10 z" />
                </marker>

                <marker
                    :id="blockedMarkerId"
                    viewBox="0 0 10 10"
                    refX="9"
                    refY="5"
                    :markerWidth="edgeArrowMarkerSize"
                    :markerHeight="edgeArrowMarkerSize"
                    markerUnits="userSpaceOnUse"
                    orient="auto-start-reverse"
                >
                    <path class="location-context-marker__arrow location-context-marker__arrow--blocked" d="M 0 0 L 10 5 L 0 10 z" />
                </marker>
            </defs>

            <g>
                <g class="location-context-graph__region">
                    <rect
                        v-if="currentRegionRect !== null"
                        class="location-context-graph__region-slate"
                        :x="currentRegionRect.x"
                        :y="currentRegionRect.y"
                        :width="currentRegionRect.width"
                        :height="currentRegionRect.height"
                    />

                    <image
                        v-if="
                            currentRegionRect !== null &&
                            regionBackgroundVisible
                        "
                        class="location-context-graph__background"
                        :x="currentRegionRect.x"
                        :y="currentRegionRect.y"
                        :width="currentRegionRect.width"
                        :height="currentRegionRect.height"
                        :href="background.url.value ?? undefined"
                        :opacity="regionBackgroundOpacity"
                        :preserveAspectRatio="regionBackgroundFit"
                        :clip-path="`url(#${clipId})`"
                    />

                    <rect
                        v-if="currentRegionRect !== null"
                        class="location-context-graph__region-boundary"
                        :x="currentRegionRect.x"
                        :y="currentRegionRect.y"
                        :width="currentRegionRect.width"
                        :height="currentRegionRect.height"
                    />
                </g>

                <g class="location-context-graph__edges">
                    <g
                        v-for="edge in edges"
                        :key="edge.key"
                        class="location-context-edge"
                        :class="{'location-context-edge--blocked': !edge.traversable}"
                    >
                        <path
                            class="location-context-edge__halo"
                            :d="edge.path"
                        />
                        <path
                            class="location-context-edge__line"
                            :d="edge.path"
                            :marker-end="
                                edge.traversable
                                    ? `url(#${arrowMarkerId})`
                                    : `url(#${blockedMarkerId})`
                            "
                        />
                        <path
                            class="location-context-edge__hit"
                            :d="edge.path"
                            :aria-label="edge.accessibleLabel"
                        >
                            <title>{{ edge.accessibleLabel }}</title>
                        </path>
                    </g>
                </g>

                <g class="location-context-graph__nodes">
                    <g
                        v-for="node in nodes"
                        :key="node.key"
                        data-context-action
                        class="location-context-node"
                        :class="{
                            'location-context-node--current': node.current,
                            'location-context-node--destination': !node.current,
                            'location-context-node--blocked': !node.current && !neighboursByDestinationId.get(node.location.get('id'))?.edge.get('is_traversable'),
                            'location-context-node--intended': intendedDestinationKey === node.key,
                            'location-context-node--concealed': !node.current && node.displayName === 'Unknown location',
                            'location-context-node--external': node.outsideCurrentRegion,
                        }"
                        :transform="
                            `translate(${node.position.x} ${node.position.y})`
                        "
                        :role="node.current ? 'img' : 'button'"
                        :tabindex="node.current ? -1 : 0"
                        :aria-label="nodeMovementAccessibleLabel(node)"
                        @pointerdown.stop
                        @pointerenter="hoveredNodeKey = node.key"
                        @pointerleave="hoveredNodeKey = null"
                        @focus="focusedNodeKey = node.key"
                        @blur="focusedNodeKey = null"
                        @click.stop="selectDestination(node)"
                        @keydown="onNodeKeydown($event, node)"
                    >
                        <title>{{ node.accessibleLabel }}</title>
                        <circle
                            class="location-context-node__focus"
                            :r="node.radius + 7"
                        />
                        <circle
                            class="location-context-node__body"
                            :r="node.radius"
                        />
                        <circle
                            v-if="node.current"
                            class="location-context-node__current-ring"
                            :r="node.radius + 4"
                        />
                        <text
                            class="location-context-node__label"
                            text-anchor="middle"
                            dominant-baseline="middle"
                        >
                            <tspan
                                v-for="(line, index) in nodeLabelLines(node)"
                                :key="`${node.key}:${index}`"
                                x="0"
                                :dy="
                                    index === 0
                                        ? nodeLabelLines(node).length === 1
                                            ? 0
                                            : -6
                                        : 12
                                "
                            >
                                {{ line }}
                            </tspan>
                        </text>
                    </g>
                </g>
            </g>
        </svg>

        <div
            v-if="
                data.projection.value !== null &&
                nodes.length === 1
            "
            class="location-context-graph__empty"
        >
            No visible outgoing connections.
        </div>

        <aside
            v-if="visibleNodeInformation !== null"
            class="location-context-graph__node-info"
            :style="nodeInformationStyle"
            aria-live="polite"
        >
            <strong>{{ visibleNodeInformation.name }}</strong>
            <p>{{ visibleNodeInformation.description }}</p>
        </aside>

        <div
            v-if="background.failed.value"
            class="location-context-graph__background-warning"
            role="status"
        >
            Region image unavailable; showing the map canvas.
        </div>

        <div
            v-if="movementMessage !== null"
            class="location-context-graph__movement-message"
            role="status"
            aria-live="polite"
        >
            {{ movementMessage }}
        </div>
    </section>
</template>

<style scoped>
.location-context-graph {
    position: relative;

    width: 100%;
    min-width: 0;
    min-height: 18rem;
    aspect-ratio: 16 / 10;

    overflow: hidden;

    color: rgb(var(--c-fg));
    background: rgb(var(--c-surface));
    border: 1px solid rgb(var(--c-border) / 0.45);
    border-radius: var(--radius-lg);

    cursor: grab;
    user-select: none;
    touch-action: none;
}

.location-context-graph--panning {
    cursor: grabbing;
}

.location-context-graph__header {
    position: absolute;
    z-index: 3;
    top: var(--space-2);
    right: var(--space-2);
    left: var(--space-2);

    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--space-2);

    min-width: 0;

    padding: 0.38rem 0.48rem;

    background: rgb(var(--c-surface-raised) / 0.9);
    border: 1px solid rgb(var(--c-border) / 0.34);
    border-radius: var(--radius-md);
    box-shadow: 0 3px 12px rgb(var(--c-shadow) / 0.12);

    backdrop-filter: blur(8px);
}

.location-context-graph__identity {
    display: flex;
    flex-direction: column;
    min-width: 0;
}

.location-context-graph__identity strong {
    overflow: hidden;
    color: rgb(var(--c-fg-strong));
    font-size: 0.78rem;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.location-context-graph__identity span {
    color: rgb(var(--c-muted));
    font-size: 0.65rem;
}

.location-context-graph__controls {
    display: flex;
    align-items: center;
    gap: var(--space-1);
}

.location-context-graph__controls button {
    min-width: 1.75rem;
    min-height: 1.75rem;
    padding: 0.18rem 0.42rem;

    color: rgb(var(--c-fg-strong));
    background: rgb(var(--c-surface));
    border: 1px solid rgb(var(--c-border) / 0.48);
    border-radius: var(--radius-sm);

    font: inherit;
    font-size: 0.7rem;
    font-weight: 750;
    cursor: pointer;
}

.location-context-graph__controls button:hover {
    border-color: rgb(var(--c-accent) / 0.68);
    background: rgb(var(--c-accent) / 0.1);
}

.location-context-graph__controls button:focus-visible {
    outline: var(--focus-ring-width) solid rgb(var(--focus-ring-color) / 0.34);
    outline-offset: 1px;
}

.location-context-graph__controls span {
    min-width: 2.8rem;
    color: rgb(var(--c-muted));
    font-size: 0.64rem;
    text-align: center;
}

.location-context-graph__viewport {
    position: absolute;
    inset: 0;

    width: 100%;
    height: 100%;
}

.location-context-graph__region-slate {
    fill: rgb(var(--c-surface-2));
}

.location-context-graph__background {
    pointer-events: none;
}

.location-context-graph__region-boundary {
    fill: none;
    stroke: rgb(var(--c-border-strong) / 0.72);
    stroke-width: 2;
    vector-effect: non-scaling-stroke;
    pointer-events: none;
}

.location-context-edge__halo {
    fill: none;
    stroke: rgb(var(--c-page) / 0.94);
    stroke-width: 7;
    vector-effect: non-scaling-stroke;
    pointer-events: none;
}

.location-context-edge__line {
    fill: none;
    stroke: rgb(var(--c-accent));
    stroke-width: 3.2;
    vector-effect: non-scaling-stroke;
    pointer-events: none;
}

.location-context-marker__arrow {
    fill: rgb(var(--c-accent));
    stroke: rgb(var(--c-page) / 0.94);
    stroke-width: 1.25;
    paint-order: stroke fill;
}

.location-context-marker__arrow--blocked { fill: rgb(var(--c-danger)); }
.location-context-edge--blocked .location-context-edge__halo,
.location-context-edge--blocked .location-context-edge__line {
    stroke-dasharray: 9 6;
}
.location-context-edge--blocked .location-context-edge__line { stroke: rgb(var(--c-danger)); }

.location-context-edge__hit {
    fill: none;
    stroke: transparent;
    stroke-width: 16;
    vector-effect: non-scaling-stroke;
    pointer-events: none;
}

.location-context-node {
    cursor: default;
}

.location-context-node--destination {
    cursor: pointer;
}

.location-context-node__body {
    fill: rgb(var(--c-surface-raised));
    stroke: rgb(var(--c-primary-strong));
    stroke-width: 2;
    vector-effect: non-scaling-stroke;

    transition:
        filter var(--duration-fast) var(--ease-standard),
        stroke-width var(--duration-fast) var(--ease-standard);
}

.location-context-node:hover .location-context-node__body,
.location-context-node:focus-visible .location-context-node__body {
    stroke-width: 3;
    filter: drop-shadow(0 3px 6px rgb(var(--c-shadow) / 0.24));
}

.location-context-node--blocked {
    cursor: not-allowed;
}

.location-context-node--blocked .location-context-node__body {
    stroke: rgb(var(--c-danger));
}

.location-context-node--intended .location-context-node__body {
    stroke: rgb(var(--c-accent));
    stroke-width: 4;
    filter: drop-shadow(0 3px 8px rgb(var(--c-accent) / 0.3));
}

.location-context-node__focus {
    fill: none;
    stroke: transparent;
    stroke-width: 3;
    vector-effect: non-scaling-stroke;
}

.location-context-node:focus-visible {
    outline: none;
}

.location-context-node:focus-visible .location-context-node__focus {
    stroke: rgb(var(--focus-ring-color) / 0.7);
}

.location-context-node__current-ring {
    fill: none;
    stroke: rgb(var(--c-accent));
    stroke-width: 2.5;
    stroke-dasharray: 5 3;
    vector-effect: non-scaling-stroke;
    pointer-events: none;
}

.location-context-node--current .location-context-node__body {
    fill: rgb(var(--c-accent-soft) / 0.82);
    stroke: rgb(var(--c-primary-strong));
}

.location-context-node--concealed .location-context-node__body {
    fill: rgb(var(--c-surface-3));
    stroke-dasharray: 4 3;
}

.location-context-node--external .location-context-node__body {
    stroke-dasharray: 8 4;
}

.location-context-node__label {
    fill: rgb(var(--c-fg-strong));
    font-family: var(--font-primary);
    font-size: 10px;
    font-weight: 750;
    pointer-events: none;
}

.location-context-graph__state {
    position: absolute;
    inset: 0;

    display: grid;
    place-items: center;

    padding: var(--space-6);

    color: rgb(var(--c-muted));
    font-size: 0.82rem;
    text-align: center;
}

.location-context-graph__state--error {
    color: rgb(var(--c-danger-strong));
}

.location-context-graph__empty,
.location-context-graph__background-warning,
.location-context-graph__movement-message {
    position: absolute;
    z-index: 2;
    right: var(--space-2);
    bottom: var(--space-2);

    max-width: min(22rem, calc(100% - var(--space-4)));
    padding: 0.3rem 0.48rem;

    color: rgb(var(--c-muted));
    background: rgb(var(--c-surface-raised) / 0.9);
    border: 1px solid rgb(var(--c-border) / 0.32);
    border-radius: var(--radius-sm);

    font-size: 0.66rem;
}

.location-context-graph__background-warning {
    right: auto;
    left: var(--space-2);
    color: rgb(var(--c-warning-strong));
}

.location-context-graph__movement-message {
    right: auto;
    left: 50%;
    max-width: min(28rem, calc(100% - var(--space-4)));
    transform: translateX(-50%);

    color: rgb(var(--c-danger-strong));
    border-color: rgb(var(--c-danger) / 0.42);
}

.location-context-graph__node-info {
    position: absolute;
    z-index: 4;

    width: min(16rem, calc(100% - var(--space-4)));
    max-height: 8.75rem;
    box-sizing: border-box;
    overflow: auto;

    padding: 0.48rem 0.56rem;

    color: rgb(var(--c-fg));
    background: rgb(var(--c-surface-raised) / 0.96);
    border: 1px solid rgb(var(--c-border-strong) / 0.48);
    border-radius: var(--radius-md);
    box-shadow: 0 5px 16px rgb(var(--c-shadow) / 0.2);

    pointer-events: none;
}

.location-context-graph__node-info strong {
    display: block;
    margin-bottom: var(--space-1);
    color: rgb(var(--c-fg-strong));
    font-size: 0.75rem;
}

.location-context-graph__node-info p {
    margin: 0;
    font-size: 0.7rem;
    line-height: 1.42;
    white-space: pre-wrap;
}

@media (max-width: 520px) {
    .location-context-graph {
        min-height: 15rem;
        aspect-ratio: 4 / 3;
    }

    .location-context-graph__header {
        align-items: flex-start;
    }

    .location-context-graph__controls span {
        display: none;
    }
}

@media (prefers-reduced-motion: reduce) {
    .location-context-node__body {
        transition: none;
    }
}
</style>
