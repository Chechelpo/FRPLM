// tests/apiClient.test.ts
import { test } from "node:test";
import assert from "node:assert/strict";

import {
    ApiRequestError,
    Severity,
    apiClient,
    clearCsrfToken,
    fetchApi,
    initializeApiSecurity,
    onGlobalError,
} from "../dist/services/apiClient.js";
import type { ErrorResponse } from "../dist/services/apiClient.js";

interface CapturedCall {
    url: string;
    init: RequestInit | undefined;
}

interface FetchStub {
    calls: CapturedCall[];
    impl: (input: RequestInfo | URL, init?: RequestInit) => Promise<Response>;
}

function installFetch(
    impl: (input: RequestInfo | URL, init?: RequestInit) => Promise<Response>,
): FetchStub {
    const calls: CapturedCall[] = [];
    const stub: FetchStub = {
        calls,
        impl,
    };
    globalThis.fetch = ((
        input: RequestInfo | URL,
        init?: RequestInit,
    ) => {
        calls.push({
            url: typeof input === "string"
                ? input
                : input instanceof URL
                    ? input.toString()
                    : input.url,
            init,
        });
        return stub.impl(input, init);
    }) as typeof fetch;

    return stub;
}

function makeJsonResponse(
    body: unknown,
    init: ResponseInit = {},
): Response {
    return new Response(JSON.stringify(body), {
        status: 200,
        ...init,
        headers: {
            "content-type": "application/json",
            ...(init.headers ?? {}),
        },
    });
}

function makeErrorResponse(
    body: unknown,
    status: number,
    contentType = "application/json",
): Response {
    return new Response(JSON.stringify(body), {
        status,
        headers: {
            "content-type": contentType,
        },
    });
}

test("fetchApi normalises string paths to /api/...", async () => {
    const stub = installFetch(async () => makeJsonResponse({ ok: true }));

    const response = await fetchApi("characters");
    await response.json();

    assert.equal(stub.calls.length, 1);
    assert.equal(stub.calls[0]?.url, "/api/characters");
});

test("fetchApi does not double-prefix already-prefixed paths", async () => {
    const stub = installFetch(async () => makeJsonResponse({ ok: true }));

    await fetchApi("/api/characters/42");

    assert.equal(stub.calls.length, 1);
    assert.equal(stub.calls[0]?.url, "/api/characters/42");
});

test("fetchApi normalises URL inputs by rewriting the path", async () => {
    const stub = installFetch(async () => makeJsonResponse({ ok: true }));

    const url = new URL("https://example.test/characters");
    await fetchApi(url);

    assert.equal(stub.calls.length, 1);
    assert.equal(
        stub.calls[0]?.url,
        "https://example.test/api/characters",
    );
});

test("fetchApi throws ApiRequestError on transport failure", async () => {
    installFetch(async () => {
        throw new DOMException("aborted", "AbortError");
    });
    clearCsrfToken();

    await assert.rejects(
        fetchApi("characters", { showGlobalError: false }),
        (error: unknown): boolean => {
            assert.ok(error instanceof ApiRequestError);
            assert.equal(error.details.severity, Severity.EXPECTED);
            assert.equal(error.details.type, "AbortError");
            return true;
        },
    );
});

test("fetchApi parses problem+json error responses", async () => {
    installFetch(async () => makeErrorResponse({
        status: 422,
        type: "ValidationError",
        message: "name is required",
        path: "/api/characters",
        severity: "USER",
    }, 422));

    await assert.rejects(
        fetchApi("characters", { showGlobalError: false }),
        (error: unknown): boolean => {
            assert.ok(error instanceof ApiRequestError);
            assert.equal(error.details.status, 422);
            assert.equal(error.details.type, "ValidationError");
            assert.equal(error.details.message, "name is required");
            assert.equal(error.details.severity, Severity.USER);
            return true;
        },
    );
});

test("fetchApi infers USER severity for 4xx without explicit severity", async () => {
    installFetch(async () => makeErrorResponse({
        message: "forbidden",
    }, 403));

    await assert.rejects(
        fetchApi("characters", { showGlobalError: false }),
        (error: unknown): boolean => {
            assert.ok(error instanceof ApiRequestError);
            assert.equal(error.details.severity, Severity.USER);
            return true;
        },
    );
});

test("fetchApi infers SYSTEM severity for 5xx without explicit severity", async () => {
    installFetch(async () => makeErrorResponse({
        message: "internal error",
    }, 500));

    await assert.rejects(
        fetchApi("characters", { showGlobalError: false }),
        (error: unknown): boolean => {
            assert.ok(error instanceof ApiRequestError);
            assert.equal(error.details.severity, Severity.SYSTEM);
            return true;
        },
    );
});

test("onGlobalError delivers errors to all registered handlers", async () => {
    installFetch(async () => makeErrorResponse({
        message: "explode",
    }, 500));

    const received: ErrorResponse[] = [];
    const offA = onGlobalError(err => received.push(err));
    const offB = onGlobalError(err => received.push(err));

    await assert.rejects(
        fetchApi("characters"),
    );

    assert.equal(received.length, 2);

    offA();
    offB();

    installFetch(async () => makeErrorResponse({
        message: "explode again",
    }, 500));
    await assert.rejects(
        fetchApi("characters"),
    );

    // Unregistered handlers do not receive new errors.
    assert.equal(received.length, 2);
});

test("fetchApi skips global error for EXPECTED severity", async () => {
    installFetch(async () => {
        throw new DOMException("aborted", "AbortError");
    });
    clearCsrfToken();

    const received: ErrorResponse[] = [];
    onGlobalError(err => received.push(err));

    await assert.rejects(
        fetchApi("characters"),
    );

    assert.equal(received.length, 0);
});

test("fetchApi skips global error when caller opts out", async () => {
    installFetch(async () => makeErrorResponse({
        message: "explode",
    }, 500));

    const received: ErrorResponse[] = [];
    onGlobalError(err => received.push(err));

    await assert.rejects(
        fetchApi("characters", { showGlobalError: false }),
    );

    assert.equal(received.length, 0);
});

test("apiClient requests CSRF token for unsafe methods", async () => {
    clearCsrfToken();
    let csrfRequests = 0;
    installFetch(async (input) => {
        const url = typeof input === "string"
            ? input
            : input instanceof URL
                ? input.toString()
                : input.url;
        if (url.endsWith("/api/security/csrf")) {
            csrfRequests += 1;
            return new Response(JSON.stringify({
                headerName: "X-XSRF-TOKEN",
                token: "csrf-token",
            }), {
                status: 200,
                headers: { "content-type": "application/json" },
            });
        }
        return makeJsonResponse({ ok: true });
    });

    await initializeApiSecurity();
    assert.equal(csrfRequests, 1);

    await apiClient("https://example.test/unsafe", {
        method: "POST",
    });

    // No additional CSRF request on the unsafe call.
    assert.equal(csrfRequests, 1);

    // Subsequent unsafe call also reuses the cached token.
    await apiClient("https://example.test/unsafe", {
        method: "DELETE",
    });
    assert.equal(csrfRequests, 1);
});

test("apiClient does not request CSRF for safe methods", async () => {
    clearCsrfToken();
    let csrfRequests = 0;
    installFetch(async (input) => {
        const url = typeof input === "string"
            ? input
            : input instanceof URL
                ? input.toString()
                : input.url;
        if (url.endsWith("/api/security/csrf")) {
            csrfRequests += 1;
            return new Response(JSON.stringify({
                headerName: "X-XSRF-TOKEN",
                token: "csrf-token",
            }), { status: 200 });
        }
        return makeJsonResponse({ ok: true });
    });

    await apiClient("https://example.test/safe", { method: "GET" });
    assert.equal(csrfRequests, 0);
});

test("clearCsrfToken forces a new CSRF request on the next unsafe call", async () => {
    clearCsrfToken();
    let csrfRequests = 0;
    installFetch(async (input) => {
        const url = typeof input === "string"
            ? input
            : input instanceof URL
                ? input.toString()
                : input.url;
        if (url.endsWith("/api/security/csrf")) {
            csrfRequests += 1;
            return new Response(JSON.stringify({
                headerName: "X-XSRF-TOKEN",
                token: "csrf-token",
            }), { status: 200 });
        }
        return makeJsonResponse({ ok: true });
    });

    await initializeApiSecurity();
    assert.equal(csrfRequests, 1);

    clearCsrfToken();

    await apiClient("https://example.test/unsafe", { method: "POST" });
    assert.equal(csrfRequests, 2);
});

test("ApiRequestError exposes the underlying ErrorResponse", () => {
    const details: ErrorResponse = {
        status: 500,
        type: "Boom",
        message: "kaboom",
        path: "/api/test",
        severity: Severity.SYSTEM,
    };
    const error = new ApiRequestError(details);

    assert.equal(error.name, "ApiRequestError");
    assert.equal(error.details, details);
    assert.equal(error.message, "kaboom");
    assert.equal(error.response, undefined);
});

test("Severity enum exposes the expected wire values", () => {
    assert.equal(Severity.EXPECTED, "EXPECTED");
    assert.equal(Severity.USER, "USER");
    assert.equal(Severity.SYSTEM, "SYSTEM");
});
