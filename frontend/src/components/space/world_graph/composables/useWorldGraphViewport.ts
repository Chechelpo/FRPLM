import {
    computed,
    onBeforeUnmount,
    onMounted,
    reactive,
    ref,
    watch,
    type ComputedRef,
    type Ref,
} from "vue";
import type {
    Position,
    RegionGeometry,
} from "@/domain/World";
import {screenToWorldPosition} from "../utils/geometry";
import {
    cameraViewBox,
    calculateFittedViewport,
    MAX_VIEWPORT_ZOOM,
    MIN_VIEWPORT_ZOOM,
    type HostSize,
    type ViewportState,
} from "../utils/viewport";
export type {HostSize, ViewportState} from "../utils/viewport";
export {calculateFittedViewport} from "../utils/viewport";

export type WorldGraphViewportOptions = {
    /** Keep the authored graph's per-world browser-local viewport by default. */
    persist?: boolean;
    storagePrefix?: string;
};

const DEFAULT_ZOOM = 1;
const DEFAULT_STORAGE_PREFIX = "world-edit-graph:viewport:";

function clamp(value: number, minimum: number, maximum: number): number {
    return Math.min(Math.max(value, minimum), maximum);
}

export function parseViewportState(raw: string | null): ViewportState | null {
    if (raw === null) return null;
    try {
        const parsed = JSON.parse(raw) as Partial<ViewportState>;
        if (
            typeof parsed.pan?.x !== "number" || !Number.isFinite(parsed.pan.x) ||
            typeof parsed.pan?.y !== "number" || !Number.isFinite(parsed.pan.y) ||
            typeof parsed.zoom !== "number" || !Number.isFinite(parsed.zoom)
        ) return null;
        return {
            pan: {
                x: parsed.pan.x,
                y: parsed.pan.y,
            },
            zoom: clamp(parsed.zoom, MIN_VIEWPORT_ZOOM, MAX_VIEWPORT_ZOOM),
        };
    } catch {
        return null;
    }
}

export function useWorldGraphViewport(
    worldId: ComputedRef<number>,
    hostRef: Ref<HTMLElement | null>,
    options: WorldGraphViewportOptions = {},
) {
    const persist = options.persist ?? true;
    const storagePrefix = options.storagePrefix ?? DEFAULT_STORAGE_PREFIX;
    const hostSize = reactive<HostSize>({
        width: 1,
        height: 1,
    });
    const pan = ref<Position>({x: 0, y: 0});
    const zoom = ref(DEFAULT_ZOOM);
    let resizeObserver: ResizeObserver | null = null;
    let persistenceFrame: number | null = null;
    let centerOnNextMeasure = true;

    function storageKey(id = worldId.value): string {
        return `${storagePrefix}${id}`;
    }

    function measureHost(): void {
        const host = hostRef.value;
        if (host === null) return;
        const rect = host.getBoundingClientRect();
        hostSize.width = Math.max(1, rect.width);
        hostSize.height = Math.max(1, rect.height);
        if (centerOnNextMeasure) {
            pan.value = {
                x: hostSize.width / 2,
                y: hostSize.height / 2,
            };
            centerOnNextMeasure = false;
        }
    }

    function persistViewport(): void {
        if (!persist || typeof window === "undefined") return;
        if (persistenceFrame !== null) {
            window.cancelAnimationFrame(persistenceFrame);
        }
        const id = worldId.value;
        const state: ViewportState = {
            pan: {...pan.value},
            zoom: zoom.value,
        };
        persistenceFrame = window.requestAnimationFrame(() => {
            persistenceFrame = null;
            try {
                window.localStorage.setItem(
                    storageKey(id),
                    JSON.stringify(state),
                );
            } catch (error) {
                console.warn("Unable to store the world graph viewport", error);
            }
        });
    }

    function restoreViewport(): void {
        let restored: ViewportState | null = null;
        if (persist && typeof window !== "undefined") {
            try {
                restored = parseViewportState(
                    window.localStorage.getItem(storageKey()),
                );
            } catch (error) {
                console.warn("Unable to restore the world graph viewport", error);
            }
        }

        if (restored === null) {
            zoom.value = DEFAULT_ZOOM;
            if (hostSize.width > 1 || hostSize.height > 1) {
                pan.value = {
                    x: hostSize.width / 2,
                    y: hostSize.height / 2,
                };
                centerOnNextMeasure = false;
            } else {
                pan.value = {x: 0, y: 0};
                centerOnNextMeasure = true;
            }
            return;
        }

        pan.value = restored.pan;
        zoom.value = restored.zoom;
        centerOnNextMeasure = false;
    }

    function setPan(next: Position, save = false): void {
        pan.value = {
            x: Number.isFinite(next.x) ? next.x : pan.value.x,
            y: Number.isFinite(next.y) ? next.y : pan.value.y,
        };
        if (save) persistViewport();
    }

    function setViewport(
        state: ViewportState,
        save = false,
    ): void {
        if (
            !Number.isFinite(state.pan.x) ||
            !Number.isFinite(state.pan.y) ||
            !Number.isFinite(state.zoom) ||
            state.zoom <= 0
        ) {
            throw new Error("Viewport state must be finite with positive zoom");
        }
        pan.value = {...state.pan};
        zoom.value = clamp(state.zoom, MIN_VIEWPORT_ZOOM, MAX_VIEWPORT_ZOOM);
        centerOnNextMeasure = false;
        if (save) persistViewport();
    }

    function fitGeometry(
        geometry: RegionGeometry,
        padding = 24,
        save = false,
    ): void {
        setViewport(
            calculateFittedViewport(hostSize, geometry, padding),
            save,
        );
    }

    function clientToScreen(clientX: number, clientY: number): Position {
        const host = hostRef.value;
        if (host === null) return {x: 0, y: 0};
        const rect = host.getBoundingClientRect();
        return {
            x: clientX - rect.left,
            y: clientY - rect.top,
        };
    }

    function screenToWorld(screen: Position): Position {
        return screenToWorldPosition(
            screen,
            pan.value,
            zoom.value,
        );
    }

    function worldToScreen(position: Position): Position {
        return {
            x: position.x * zoom.value + pan.value.x,
            y: position.y * zoom.value + pan.value.y,
        };
    }

    function setZoomAtScreen(
        nextZoom: number,
        screenAnchor: Position,
    ): void {
        const anchor = screenToWorld(screenAnchor);
        const normalized = clamp(nextZoom, MIN_VIEWPORT_ZOOM, MAX_VIEWPORT_ZOOM);
        zoom.value = normalized;
        pan.value = {
            x: screenAnchor.x - anchor.x * normalized,
            y: screenAnchor.y - anchor.y * normalized,
        };
        persistViewport();
    }

    function zoomBy(factor: number, anchor?: Position): void {
        setZoomAtScreen(
            zoom.value * factor,
            anchor ?? {
                x: hostSize.width / 2,
                y: hostSize.height / 2,
            },
        );
    }

    function handleWheel(event: WheelEvent): void {
        const anchor = clientToScreen(event.clientX, event.clientY);
        setZoomAtScreen(
            zoom.value * Math.exp(-event.deltaY * 0.0015),
            anchor,
        );
    }

    function resetViewport(): void {
        zoom.value = DEFAULT_ZOOM;
        pan.value = {
            x: hostSize.width / 2,
            y: hostSize.height / 2,
        };
        persistViewport();
    }

    const viewportViewBox = computed(() => cameraViewBox(
        {
            width: Math.max(1, hostSize.width),
            height: Math.max(1, hostSize.height),
        },
        {
            pan: pan.value,
            zoom: zoom.value,
        },
    ));
    /**
     * @deprecated The camera is encoded by viewportViewBox. Kept as an
     * identity transform so existing consumers that bind both values do not
     * accidentally apply the camera twice during migration.
     */
    const viewportTransform = computed(
        () => "translate(0 0) scale(1)",
    );
    const zoomPercentage = computed(
        () => `${Math.round(zoom.value * 100)}%`,
    );

    watch(worldId, restoreViewport, {immediate: true});

    onMounted(() => {
        measureHost();
        if (typeof ResizeObserver !== "undefined") {
            resizeObserver = new ResizeObserver(measureHost);
            if (hostRef.value !== null) {
                resizeObserver.observe(hostRef.value);
            }
        }
    });

    onBeforeUnmount(() => {
        resizeObserver?.disconnect();
        resizeObserver = null;
        if (
            persistenceFrame !== null &&
            typeof window !== "undefined"
        ) {
            window.cancelAnimationFrame(persistenceFrame);
        }
    });

    return {
        hostSize,
        pan,
        zoom,
        viewportViewBox,
        viewportTransform,
        zoomPercentage,
        setPan,
        setViewport,
        fitGeometry,
        persistViewport,
        zoomBy,
        resetViewport,
        handleWheel,
        clientToScreen,
        screenToWorld,
        worldToScreen,
        measureHost,
    };
}
