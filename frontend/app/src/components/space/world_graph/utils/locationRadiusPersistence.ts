import {     EntityTypes,
    fetchApi } from "@frplm/host-sdk";
import type {Location} from "@frplm/host-sdk";
import {
    appendQueryParams,
    getEntityController,
} from "@frplm/host-sdk";
import {
    executeLocationRadiusPatch,
    type LocationRadiusFetcher,
} from "./locationRadiusProtocol.js";

export function buildLocationRadiusEndpoint(location: Location): URL {
    const url = getEntityController(EntityTypes.LOCATIONS);
    url.pathname = `${url.pathname.replace(/\/$/, "")}/entity`;
    appendQueryParams(url, {
        worldID: location.get("worldID"),
        id: location.get("id"),
    });
    return url;
}

export async function requestLocationRadiusUpdate(
    location: Location,
    requestedRadius: number | null,
    fetcher: LocationRadiusFetcher = (input, init) => fetchApi(input, init),
): Promise<number | null> {
    return executeLocationRadiusPatch(
        buildLocationRadiusEndpoint(location),
        requestedRadius,
        fetcher,
    );
}
