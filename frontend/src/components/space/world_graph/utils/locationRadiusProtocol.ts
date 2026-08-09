import {assertValidExplicitLocationRadius} from "./locationRadius";

export type LocationRadiusFetcher = (
    input: RequestInfo | URL,
    init?: RequestInit,
) => Promise<Response>;

function findRadiusCandidate(payload: unknown): {found: boolean; value: unknown} {
    if (payload === null || typeof payload !== "object") {
        return {found: false, value: undefined};
    }
    const record = payload as Record<string, unknown>;
    if (Object.prototype.hasOwnProperty.call(record, "radius")) {
        return {found: true, value: record.radius};
    }
    for (const key of ["payload", "data", "dataMap"] as const) {
        const nested = record[key];
        if (nested !== null && typeof nested === "object") {
            const nestedRecord = nested as Record<string, unknown>;
            if (Object.prototype.hasOwnProperty.call(nestedRecord, "radius")) {
                return {found: true, value: nestedRecord.radius};
            }
        }
    }
    return {found: false, value: undefined};
}

export function normalizeLocationRadiusResponse(
    payload: unknown,
    requestedRadius: number | null,
): number | null {
    const candidate = findRadiusCandidate(payload);
    if (!candidate.found) return requestedRadius;
    if (candidate.value === null) return null;
    assertValidExplicitLocationRadius(candidate.value);
    return candidate.value;
}

export async function executeLocationRadiusPatch(
    endpoint: RequestInfo | URL,
    requestedRadius: number | null,
    fetcher: LocationRadiusFetcher,
): Promise<number | null> {
    if (requestedRadius !== null) {
        assertValidExplicitLocationRadius(requestedRadius);
    }

    const response = await fetcher(
        endpoint,
        {
            method: "PATCH",
            headers: {
                "Content-Type": "application/json",
                Accept: "application/json",
            },
            body: JSON.stringify({radius: requestedRadius}),
        },
    );

    if (!response.ok) {
        throw new Error("Location radius persistence failed");
    }
    if (response.status === 204) return requestedRadius;

    const text = await response.text();
    if (text.trim().length === 0) return requestedRadius;

    try {
        return normalizeLocationRadiusResponse(
            JSON.parse(text) as unknown,
            requestedRadius,
        );
    } catch (error) {
        if (error instanceof SyntaxError) return requestedRadius;
        throw error;
    }
}
