import type {
    Position,
    RegionGeometry,
} from "@/domain/World";
import type {WorldCanvasGeometry} from "../types";

const MAX_BINARY_SCALE_EXPONENT = 1022;
const MIN_BINARY_SCALE_EXPONENT = -1022;

function assertFinitePosition(position: Position, label: string): void {
    if (!Number.isFinite(position.x) || !Number.isFinite(position.y)) {
        throw new Error(`${label} must be finite`);
    }
}

function assertPositiveFinite(value: number, label: string): void {
    if (!Number.isFinite(value) || value <= 0) {
        throw new Error(`${label} must be finite and positive`);
    }
}

/**
 * Converts radial pointer travel in viewport pixels into a multiplicative
 * authored-size scale. Moving one configured doubling distance farther from
 * the target centre doubles it; moving the same distance inward halves it.
 *
 * The calculation intentionally uses the *change* in radial distance rather
 * than current/start distance. It therefore has no singularity when the
 * gesture begins at the target centre and does not depend on world zoom.
 */
export function radialResizeScale(
    startPointerScreen: Position,
    currentPointerScreen: Position,
    targetCenterScreen: Position,
    doublingScreenPixels: number,
): number {
    assertFinitePosition(startPointerScreen, "Resize start pointer");
    assertFinitePosition(currentPointerScreen, "Resize current pointer");
    assertFinitePosition(targetCenterScreen, "Resize target centre");
    assertPositiveFinite(doublingScreenPixels, "Resize doubling distance");

    const startDistance = Math.hypot(
        startPointerScreen.x - targetCenterScreen.x,
        startPointerScreen.y - targetCenterScreen.y,
    );
    const currentDistance = Math.hypot(
        currentPointerScreen.x - targetCenterScreen.x,
        currentPointerScreen.y - targetCenterScreen.y,
    );
    const exponent = Math.min(
        MAX_BINARY_SCALE_EXPONENT,
        Math.max(
            MIN_BINARY_SCALE_EXPONENT,
            (currentDistance - startDistance) / doublingScreenPixels,
        ),
    );
    return 2 ** exponent;
}

/** Uniformly scales a region about its centre while preserving its aspect ratio. */
export function scaleRegionFromCenter(
    start: RegionGeometry,
    requestedScale: number,
    minimum: {width: number; height: number},
): RegionGeometry {
    assertPositiveFinite(start.width, "Region resize width");
    assertPositiveFinite(start.height, "Region resize height");
    assertFinitePosition(start, "Region resize centre");
    assertPositiveFinite(requestedScale, "Region resize scale");
    assertPositiveFinite(minimum.width, "Region minimum width");
    assertPositiveFinite(minimum.height, "Region minimum height");

    const minimumScale = Math.max(
        minimum.width / start.width,
        minimum.height / start.height,
    );
    const finiteScaleCeiling = Number.MAX_VALUE /
        Math.max(start.width, start.height) / 4;
    const scale = Math.min(
        finiteScaleCeiling,
        Math.max(minimumScale, requestedScale),
    );
    return {
        x: start.x,
        y: start.y,
        width: start.width * scale,
        height: start.height * scale,
    };
}

/** Uniformly scales the centred world canvas. */
export function scaleWorldCanvasFromCenter(
    start: WorldCanvasGeometry,
    requestedScale: number,
    minimum: {width: number; height: number},
): WorldCanvasGeometry {
    assertPositiveFinite(start.width, "World resize width");
    assertPositiveFinite(start.height, "World resize height");
    assertPositiveFinite(requestedScale, "World resize scale");
    assertPositiveFinite(minimum.width, "World minimum width");
    assertPositiveFinite(minimum.height, "World minimum height");

    const minimumScale = Math.max(
        minimum.width / start.width,
        minimum.height / start.height,
    );
    const finiteScaleCeiling = Number.MAX_VALUE /
        Math.max(start.width, start.height) / 4;
    const scale = Math.min(
        finiteScaleCeiling,
        Math.max(minimumScale, requestedScale),
    );
    return {
        width: start.width * scale,
        height: start.height * scale,
    };
}
