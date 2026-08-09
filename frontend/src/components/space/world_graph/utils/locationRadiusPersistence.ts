import type {Location} from "@/domain/World";
import {
    appendIDParams,
    getEntityController,
} from "@/core/ABSEntity";
import {EntityTypes} from "@/domain/EntityTypes";
import {fetchApi} from "@/services/apiClient";
import {
    executeLocationRadiusPatch,
    type LocationRadiusFetcher,
} from "./locationRadiusProtocol";

export function buildLocationRadiusEndpoint(location: Location): URL {
    const url = getEntityController(EntityTypes.LOCATIONS);
    url.pathname = `${url.pathname.replace(/\/$/, "")}/entity`;
    appendIDParams(url, {
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
