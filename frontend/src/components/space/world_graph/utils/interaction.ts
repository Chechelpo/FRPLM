import type {Position} from "@/domain/World";
import {DRAG_THRESHOLD_SCREEN_PIXELS} from "../constants";

export type PrimaryModifierMode =
    | "pan"
    | "marquee"
    | "toggle"
    | null;

export type PrimaryModifierState = {
    altKey: boolean;
    shiftKey: boolean;
    ctrlKey: boolean;
};

/** Modifier priority is independent of the graph object under the pointer. */
export function resolvePrimaryModifierMode(
    state: PrimaryModifierState,
): PrimaryModifierMode {
    if (state.altKey) return "pan";
    if (state.shiftKey) return "marquee";
    if (state.ctrlKey) return "toggle";
    return null;
}

export function movedPastDragThreshold(
    start: Position,
    current: Position,
    threshold = DRAG_THRESHOLD_SCREEN_PIXELS,
): boolean {
    if (!Number.isFinite(threshold) || threshold < 0) {
        throw new Error("Drag threshold must be finite and non-negative");
    }
    return Math.hypot(
        current.x - start.x,
        current.y - start.y,
    ) >= threshold;
}
