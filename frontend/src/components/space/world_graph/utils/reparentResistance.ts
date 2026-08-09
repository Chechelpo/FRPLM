import type {Position, RegionGeometry} from "@/domain/World";
import {geometryToBounds} from "./geometry";

export type MovementDeltaConstraint = {
    minX: number;
    maxX: number;
    minY: number;
    maxY: number;
};

export type ReparentLatch = {
    key: string;
    constraint: MovementDeltaConstraint;
};

export type ReparentResistanceResult = {
    delta: Position;
    releasedKeys: ReadonlySet<string>;
};

/**
 * A sticky-parent latch prevents an entity from detaching from its current
 * parent, but it must not prevent the entity from entering a more specific
 * descendant of that parent. This distinction is important for nested regions:
 * a child reparented to its ancestor must be able to move back into the nested
 * region without first leaving the ancestor just to release its latch.
 */
export function resolveParentWhileLatched<T>(
    latchedParent: T,
    candidate: T | null,
    candidateIsDescendantOfLatchedParent: (candidate: T, latchedParent: T) => boolean,
): T {
    return candidate !== null && candidateIsDescendantOfLatchedParent(candidate, latchedParent)
        ? candidate
        : latchedParent;
}

export function locationParentDeltaConstraint(
    center: Position,
    radius: number,
    parent: RegionGeometry,
    padding: number,
): MovementDeltaConstraint {
    const bounds = geometryToBounds(parent);
    const inset = radius + padding;
    return {
        minX: bounds.left + inset - center.x,
        maxX: bounds.right - inset - center.x,
        minY: bounds.top + inset - center.y,
        maxY: bounds.bottom - inset - center.y,
    };
}

export function regionParentDeltaConstraint(
    child: RegionGeometry,
    parent: RegionGeometry,
    padding: number,
): MovementDeltaConstraint {
    const childBounds = geometryToBounds(child);
    const parentBounds = geometryToBounds(parent);
    return {
        minX: parentBounds.left + padding - childBounds.left,
        maxX: parentBounds.right - padding - childBounds.right,
        minY: parentBounds.top + padding - childBounds.top,
        maxY: parentBounds.bottom - padding - childBounds.bottom,
    };
}

export function clampDeltaToConstraint(
    delta: Position,
    constraint: MovementDeltaConstraint,
): Position {
    return {
        x: Math.min(Math.max(delta.x, constraint.minX), constraint.maxX),
        y: Math.min(Math.max(delta.y, constraint.minY), constraint.maxY),
    };
}

export function intersectDeltaConstraints(
    constraints: readonly MovementDeltaConstraint[],
): MovementDeltaConstraint | null {
    const first = constraints[0];
    if (first === undefined) return null;
    return constraints.slice(1).reduce((combined, constraint) => ({
        minX: Math.max(combined.minX, constraint.minX),
        maxX: Math.min(combined.maxX, constraint.maxX),
        minY: Math.max(combined.minY, constraint.minY),
        maxY: Math.min(combined.maxY, constraint.maxY),
    }), first);
}

export function releaseDistanceInScreenPixels(
    requestedDelta: Position,
    constraint: MovementDeltaConstraint,
    zoom: number,
): number {
    if (!Number.isFinite(zoom) || zoom <= 0) return 0;
    const containedDelta = clampDeltaToConstraint(requestedDelta, constraint);
    return Math.hypot(
        requestedDelta.x - containedDelta.x,
        requestedDelta.y - containedDelta.y,
    ) * zoom;
}

/**
 * Releases each latch independently, then constrains the complete moving group
 * with the intersection of every latch that remains. Released latches never
 * re-arm; callers retain `releasedKeys` for the lifetime of the pointer drag.
 */
export function resolveReparentResistance(
    requestedDelta: Position,
    latches: readonly ReparentLatch[],
    alreadyReleased: ReadonlySet<string>,
    zoom: number,
    releaseThresholdScreenPixels: number,
): ReparentResistanceResult {
    const releasedKeys = new Set(alreadyReleased);
    for (const latch of latches) {
        if (releasedKeys.has(latch.key)) continue;
        if (
            releaseDistanceInScreenPixels(requestedDelta, latch.constraint, zoom) >=
            releaseThresholdScreenPixels
        ) {
            releasedKeys.add(latch.key);
        }
    }

    const activeConstraints = latches
        .filter((latch) => !releasedKeys.has(latch.key))
        .map((latch) => latch.constraint);
    const combined = intersectDeltaConstraints(activeConstraints);
    return {
        delta: combined === null
            ? {...requestedDelta}
            : clampDeltaToConstraint(requestedDelta, combined),
        releasedKeys,
    };
}
