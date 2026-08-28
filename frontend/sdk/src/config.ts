// src/config.ts
/**
 * The base URL used to talk to the FRPLM backend.
 *
 * In a browser environment, the application and the API are normally
 * served from the same origin so we derive the base from
 * `window.location.origin`. In non-browser environments (Node, tests)
 * the origin is unknown; callers can pass an explicit origin via
 * {@link configureApiBase}.
 */

let apiBaseOverride: URL | null = null;

export function configureApiBase(base: URL | string): void {
    apiBaseOverride = base instanceof URL
        ? base
        : new URL(base);
}

function defaultApiBase(): URL {
    if (typeof window !== "undefined") {
        return new URL("/api/", window.location.origin);
    }

    return new URL("http://localhost/api/");
}

export function getApiBase(): URL {
    return apiBaseOverride ?? defaultApiBase();
}

/**
 * Convenience constant for direct synchronous consumers.
 *
 * NOTE: It is initialised at module load. If you need a base URL that
 * responds to {@link configureApiBase} after the SDK is loaded, call
 * {@link getApiBase} instead.
 */
export const API_BASE: URL = getApiBase();
