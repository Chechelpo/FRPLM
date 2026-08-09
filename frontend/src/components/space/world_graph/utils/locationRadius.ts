import type {
    Location,
    Position,
    RegionGeometry,
} from "@/domain/World";
import {
    AUTOMATIC_LOCATION_MAX_RADIUS,
    DEFAULT_LOCATION_RADIUS,
    LOCATION_RADIUS_GROWTH,
    MAX_LOCATION_RADIUS,
    MIN_LOCATION_RADIUS,
} from "../constants";
import type {
    LocationRadiusState,
} from "../types";
import {
    clamp,
    geometryToBounds,
    locationEntityKey,
} from "./geometry";

/**
 * Local adapter for the nullable backend radius field.
 *
 * World.ts is read-only context and may still expose radius as a required
 * number. Runtime reads and writes remain attached to the Location entity's
 * backend-backed data map; this adapter does not introduce persisted state.
 */
type RadiusBackedLocation = {
    hasAttribute(name: string): boolean;
    get(field: "radius"): unknown;
    dataMap: Record<string, unknown>;
};

export type ExplicitLocationRadius = number;

export function isValidExplicitLocationRadius(value: unknown): value is number {
    return typeof value === "number" &&
        Number.isFinite(value) &&
        value >= MIN_LOCATION_RADIUS &&
        value <= MAX_LOCATION_RADIUS;
}

export function assertValidExplicitLocationRadius(value: unknown): asserts value is number {
    if (!isValidExplicitLocationRadius(value)) {
        throw new Error(
            `Location radius must be finite and at least ${MIN_LOCATION_RADIUS}`,
        );
    }
}

/** Missing radius data is compatible with automatic sizing. */
export function readPersistedLocationRadius(location: Location): number | null {
    const adapted = location as unknown as RadiusBackedLocation;
    if (!adapted.hasAttribute("radius")) return null;

    const value = adapted.get("radius");
    if (value === null || value === undefined) return null;
    assertValidExplicitLocationRadius(value);
    return value;
}

/** Applies a backend-confirmed radius to the entity's reactive data map. */
export function commitPersistedLocationRadius(
    location: Location,
    radius: number | null,
): void {
    if (radius !== null) assertValidExplicitLocationRadius(radius);
    const adapted = location as unknown as RadiusBackedLocation;
    adapted.dataMap.radius = radius;
}

export function locationUsesExplicitRadius(location: Location): boolean {
    return readPersistedLocationRadius(location) !== null;
}

export function calculateAutomaticLocationRadius(
    degree: number,
    baseRadius = DEFAULT_LOCATION_RADIUS,
    growthRate = LOCATION_RADIUS_GROWTH,
    maximumRadius = AUTOMATIC_LOCATION_MAX_RADIUS,
): number {
    if (!Number.isFinite(degree) || degree < 0) {
        throw new Error("Location degree must be a finite non-negative number");
    }
    if (!Number.isFinite(baseRadius) || baseRadius <= 0) {
        throw new Error("Automatic base radius must be finite and positive");
    }
    if (!Number.isFinite(growthRate) || growthRate < 0) {
        throw new Error("Automatic radius growth must be finite and non-negative");
    }
    if (!Number.isFinite(maximumRadius) || maximumRadius < baseRadius) {
        throw new Error("Automatic maximum radius must be finite and no smaller than the base radius");
    }

    return Math.min(
        maximumRadius,
        baseRadius + growthRate * Math.log2(degree + 1),
    );
}

export function resolvePersistedOrAutomaticRadius(
    location: Location,
    degree: number,
): number {
    return readPersistedLocationRadius(location) ??
        calculateAutomaticLocationRadius(degree);
}

/**
 * Resolves the one effective radius used by every graph subsystem.
 * Priority: active preview, pending preview, persisted explicit, automatic.
 */
export function resolveEffectiveLocationRadius(
    location: Location,
    degree: number,
    state: LocationRadiusState = {
        activePreviewRadius: new Map(),
        pendingRadius: new Map(),
    },
): number {
    const key = locationEntityKey(location);
    const active = state.activePreviewRadius.get(key);
    if (active !== undefined) {
        assertValidExplicitLocationRadius(active);
        return active;
    }

    const pending = state.pendingRadius.get(key);
    if (pending !== undefined) {
        assertValidExplicitLocationRadius(pending);
        return pending;
    }

    return resolvePersistedOrAutomaticRadius(location, degree);
}

export function clampExplicitLocationRadius(value: number): number {
    if (!Number.isFinite(value)) {
        throw new Error("Location radius candidate must be finite");
    }
    return clamp(value, MIN_LOCATION_RADIUS, MAX_LOCATION_RADIUS);
}

export function calculatePointerLocationRadius(
    center: Position,
    pointer: Position,
): number {
    const deltaX = pointer.x - center.x;
    const deltaY = pointer.y - center.y;
    const radius = Math.hypot(deltaX, deltaY);
    if (!Number.isFinite(radius)) {
        throw new Error("Location radius calculation produced a non-finite value");
    }
    return radius;
}

/** Maximum circle radius that preserves padding inside a centered rectangle. */
export function maximumContainedLocationRadius(
    center: Position,
    region: RegionGeometry,
    padding = 0,
): number {
    if (!Number.isFinite(padding) || padding < 0) {
        throw new Error("Location containment padding must be finite and non-negative");
    }
    const bounds = geometryToBounds(region);
    return Math.min(
        center.x - bounds.left - padding,
        bounds.right - center.x - padding,
        center.y - bounds.top - padding,
        bounds.bottom - center.y - padding,
    );
}

export function maximumValidLocationRadius(
    center: Position,
    region: RegionGeometry,
    padding = 0,
): number {
    return Math.min(
        MAX_LOCATION_RADIUS,
        maximumContainedLocationRadius(center, region, padding),
    );
}

export function clampLocationRadiusToRegion(
    candidate: number,
    center: Position,
    region: RegionGeometry,
    padding = 0,
    minimumRadius = MIN_LOCATION_RADIUS,
): number | null {
    if (!Number.isFinite(minimumRadius) || minimumRadius < MIN_LOCATION_RADIUS) {
        throw new Error("Location resize minimum must be finite and positive");
    }
    const maximum = maximumValidLocationRadius(center, region, padding);
    if (!Number.isFinite(maximum) || maximum < minimumRadius) return null;
    return clamp(clampExplicitLocationRadius(candidate), minimumRadius, maximum);
}

export function locationRadiusHandlePosition(
    center: Position,
    radius: number,
): Position {
    assertValidExplicitLocationRadius(radius);
    return {
        x: center.x + radius,
        y: center.y,
    };
}

export function canDisplayLocationRadiusHandle(
    selectedLocationCount: number,
    selectedRegionCount: number,
    selected: boolean,
    _pending: boolean,
): boolean {
    return selected &&
        selectedLocationCount === 1 &&
        selectedRegionCount === 0;
}

export function canBeginLocationRadiusResize(
    selectedLocationCount: number,
    selectedRegionCount: number,
    selected: boolean,
    pending: boolean,
): boolean {
    return canDisplayLocationRadiusHandle(
        selectedLocationCount,
        selectedRegionCount,
        selected,
        pending,
    ) && !pending;
}

export type RadiusMutationScope = {
    worldId: number;
    locationKey: string;
};

export function radiusMutationStillApplies(
    scope: RadiusMutationScope,
    activeWorldId: number,
    availableLocationKeys: ReadonlySet<string>,
): boolean {
    return scope.worldId === activeWorldId &&
        availableLocationKeys.has(scope.locationKey);
}
