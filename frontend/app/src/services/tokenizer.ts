import {     API_BASE,
    LLMConnection,
    fetchApi } from "@frplm/host-sdk";
// tokenizer.ts
import { readonly, ref } from "vue";


const STORAGE_KEY = "frplm.tokenizer.connectionId";

/**
 * Reads and validates the persisted tokenizer connection ID.
 */
function loadPersistedConnectionId(): number | null {
    if (typeof window === "undefined") {
        return null;
    }

    const storedValue = window.localStorage.getItem(STORAGE_KEY);

    if (storedValue == null) {
        return null;
    }

    const connectionId = Number(storedValue);

    if (!Number.isSafeInteger(connectionId) || connectionId < 0) {
        window.localStorage.removeItem(STORAGE_KEY);
        return null;
    }

    return connectionId;
}

const activeConnectionId = ref<number | null>(
    loadPersistedConnectionId(),
);

/**
 * Read-only reactive access to the selected tokenizer connection ID.
 */
export const tokenizerConnectionId = readonly(activeConnectionId);

/**
 * Selects an LLM connection for application-wide tokenization.
 *
 * Only the connection ID is persisted. The backend retrieves the
 * connection's current model whenever tokenize() is called.
 */
export function setTokenizerConnection(
    connection: LLMConnection,
): void {
    const connectionId = connection.get("id");

    setTokenizerConnectionId(connectionId);
}

/**
 * Selects an LLM connection directly by its database ID.
 */
export function setTokenizerConnectionId(
    connectionId: number,
): void {
    if (!Number.isSafeInteger(connectionId) || connectionId < 0) {
        throw new Error(
            `Invalid tokenizer connection ID: ${connectionId}`,
        );
    }

    activeConnectionId.value = connectionId;

    if (typeof window !== "undefined") {
        window.localStorage.setItem(
            STORAGE_KEY,
            String(connectionId),
        );
    }
}

/**
 * Clears the selected tokenizer connection and its persisted value.
 */
export function clearTokenizerConnection(): void {
    activeConnectionId.value = null;

    if (typeof window !== "undefined") {
        window.localStorage.removeItem(STORAGE_KEY);
    }
}

/**
 * Returns the currently selected tokenizer connection ID.
 */
export function getTokenizerConnectionId(): number | null {
    return activeConnectionId.value;
}

/**
 * Counts tokens using the model currently assigned to the persisted
 * LLM connection.
 */
export async function tokenize(text: string): Promise<number> {
    const connectionId = activeConnectionId.value;

    if (connectionId == null) {
        throw new Error(
            "No tokenizer connection has been selected",
        );
    }

    const url =
        `api/tokenizer/tokenize` +
        `?connectionId=${encodeURIComponent(String(connectionId))}`;

    const response = await fetchApi(
        url,
        {
            method: "POST",
            headers: new Headers({
                "Content-Type": "text/plain;charset=UTF-8",
                "Accept": "application/json",
            }),
            body: text,
        },
    );

    return await response.json() as number;
}
