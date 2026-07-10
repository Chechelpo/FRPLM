import { computed, shallowRef } from "vue";

export type ErrorResponse = {
    status: number;
    type: string;
    message: string;
    path: string;
};

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