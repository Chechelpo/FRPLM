import { computed, shallowRef } from "vue";
import {
    onGlobalError,
    type ErrorResponse,
    type GlobalErrorHandler,
} from "@frplm/host-sdk";

export type GlobalErrorEntry = ErrorResponse & {
    id: string;
    timestamp: number;
};

const errorQueue = shallowRef<GlobalErrorEntry[]>([]);

export const currentGlobalError = computed<GlobalErrorEntry | null>(
    () => errorQueue.value[0] ?? null,
);

function createErrorId(): string {
    if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
        return crypto.randomUUID();
    }

    return `${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

export function setGlobalError(error: ErrorResponse): void {
    const entry: GlobalErrorEntry = {
        ...error,
        id: createErrorId(),
        timestamp: Date.now(),
    };

    /*
     * Assignment is used instead of push because errorQueue is a shallowRef.
     */
    if (errorQueue.value.length > 5) return;
    errorQueue.value = [...errorQueue.value, entry];
}

export function dismissGlobalError(id?: string): void {
    if (!id) {
        errorQueue.value = errorQueue.value.slice(1);
        return;
    }

    errorQueue.value = errorQueue.value.filter(
        error => error.id !== id,
    );
}

export function clearGlobalErrors(): void {
    errorQueue.value = [];
}

/**
 * Wires the host application's global error queue to the SDK's
 * error channel. The SDK's `fetchApi` emits every non-EXPECTED
 * failure to handlers registered through `onGlobalError`. We push
 * those failures into the local queue that
 * `components/errors/GlobalError.vue` renders as a toast.
 *
 * Returns the unsubscribe function so tests can detach the handler.
 */
export function registerGlobalErrorHandler(
    register: (handler: GlobalErrorHandler) => () => void,
): () => void {
    return register(setGlobalError);
}

/**
 * Convenience entry point that uses the SDK's
 * {@link onGlobalError} directly. Call this once at startup.
 */
export function initGlobalErrorBridge(): () => void {
    return registerGlobalErrorHandler(onGlobalError);
}
