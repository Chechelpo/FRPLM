import type {
    Position,
    RegionGeometry,
} from "@frplm/host-sdk";
import {clamp} from "./geometry.js";

export type HostSize = {
    width: number;
    height: number;
};

export type ViewportState = {
    pan: Position;
    zoom: number;
};

export const MIN_VIEWPORT_ZOOM = 0.05;
/**
 * Deliberately high practical ceiling: zoom is the editor's precision control,
 * so ordinary hierarchical authoring must not hit a 100x detail wall.
 */
export const MAX_VIEWPORT_ZOOM = 1_000_000;

/**
 * Encodes the viewport camera directly in SVG world coordinates.
 *
 * Keeping the camera in the root viewBox avoids asking the browser to retain
 * a rasterized SVG picture and then magnify that picture through an enormous
 * descendant transform. The mapping is exactly equivalent to
 * `screen = world * zoom + pan`:
 *
 *     viewX = -pan.x / zoom
 *     viewY = -pan.y / zoom
 *     viewWidth = host.width / zoom
 *     viewHeight = host.height / zoom
 */
export function cameraViewBox(
    host: HostSize,
    viewport: ViewportState,
): string {
    if (
        !Number.isFinite(host.width) || host.width <= 0 ||
        !Number.isFinite(host.height) || host.height <= 0
    ) {
        throw new Error("Viewport host dimensions must be finite and positive");
    }
    if (
        !Number.isFinite(viewport.pan.x) ||
        !Number.isFinite(viewport.pan.y) ||
        !Number.isFinite(viewport.zoom) ||
        viewport.zoom <= 0
    ) {
        throw new Error("Viewport camera must have finite pan and positive zoom");
    }

    const zoom = viewport.zoom;
    return [
        -viewport.pan.x / zoom,
        -viewport.pan.y / zoom,
        host.width / zoom,
        host.height / zoom,
    ].join(" ");
}

export function calculateFittedViewport(
    host: HostSize,
    geometry: RegionGeometry,
    padding = 24,
): ViewportState {
    if (
        !Number.isFinite(host.width) || host.width <= 0 ||
        !Number.isFinite(host.height) || host.height <= 0
    ) {
        throw new Error("Viewport host dimensions must be finite and positive");
    }
    if (
        !Number.isFinite(geometry.x) ||
        !Number.isFinite(geometry.y) ||
        !Number.isFinite(geometry.width) || geometry.width <= 0 ||
        !Number.isFinite(geometry.height) || geometry.height <= 0
    ) {
        throw new Error("Fit geometry must be finite and positive");
    }
    if (!Number.isFinite(padding) || padding < 0) {
        throw new Error("Viewport fit padding must be finite and non-negative");
    }

    const availableWidth = Math.max(1, host.width - padding * 2);
    const availableHeight = Math.max(1, host.height - padding * 2);
    const zoom = clamp(
        Math.min(
            availableWidth / geometry.width,
            availableHeight / geometry.height,
        ),
        MIN_VIEWPORT_ZOOM,
        MAX_VIEWPORT_ZOOM,
    );

    return {
        zoom,
        pan: {
            x: host.width / 2 - geometry.x * zoom,
            y: host.height / 2 - geometry.y * zoom,
        },
    };
}
