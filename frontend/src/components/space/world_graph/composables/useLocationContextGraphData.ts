import {
    shallowRef,
    watch,
    type ComputedRef,
} from "vue";
import {
    Location,
    Region,
    type LocationData,
    type LocationKey,
    type RegionData,
    type RegionKey,
} from "@/domain/World";
import {fetchOne} from "@/core/ABSEntity";
import {EntityTypes} from "@/domain/EntityTypes";
import type {
    LocationContextProjection,
} from "../types";
import {
    locationEntityKey,
} from "../utils/geometry";
import {
    assertLocationContextProjection,
} from "../utils/locationContext";

async function fetchRegion(
    worldId: number,
    regionId: number,
): Promise<Region> {
    return fetchOne<RegionKey, RegionData, Region>(
        {
            world_id: worldId,
            id: regionId,
        },
        EntityTypes.REGIONS,
        Region,
    );
}

async function fetchLocation(
    worldId: number,
    locationId: number,
): Promise<Location> {
    return fetchOne<LocationKey, LocationData, Location>(
        {
            worldID: worldId,
            id: locationId,
        },
        EntityTypes.LOCATIONS,
        Location,
    );
}

async function loadRequiredRegionChains(
    worldId: number,
    regionIds: readonly number[],
): Promise<readonly Region[]> {
    const resolved = new Map<number, Region>();
    const pending = new Map<number, Promise<Region>>();

    async function resolveRegion(regionId: number): Promise<Region> {
        const existing = resolved.get(regionId);
        if (existing !== undefined) return existing;

        const existingPending = pending.get(regionId);
        if (existingPending !== undefined) return existingPending;

        const request = fetchRegion(worldId, regionId)
            .then(region => {
                if (region.get("world_id") !== worldId) {
                    throw new Error("Region hierarchy crosses world boundaries");
                }
                resolved.set(regionId, region);
                return region;
            })
            .finally(() => {
                pending.delete(regionId);
            });

        pending.set(regionId, request);
        return request;
    }

    async function resolveChain(startId: number): Promise<void> {
        let currentId: number | null = startId;
        const visited = new Set<number>();

        while (currentId !== null) {
            if (visited.has(currentId)) {
                throw new Error("Cycle detected in region hierarchy");
            }
            visited.add(currentId);

            const region = await resolveRegion(currentId);
            currentId = region.get("parent_region_id");
        }
    }

    await Promise.all(
        [...new Set(regionIds)].map(resolveChain),
    );

    return [...resolved.values()];
}

async function loadDegree(
    location: Location,
): Promise<number> {
    const [outgoing, incoming] = await Promise.all([
        location.getOutEdges(),
        location.getInEdges(),
    ]);
    return outgoing.length + incoming.length;
}

/**
 * Loads only the current location, its outgoing destinations, and the region
 * ancestry required to express those nodes relative to the current region.
 */
export async function loadLocationContextProjection(
    currentLocation: Location,
): Promise<LocationContextProjection> {
    const worldId = currentLocation.get("worldID");
    const currentRegionId = currentLocation.get("region_id");

    if (currentRegionId === null) {
        throw new Error("The current location has no containing region");
    }

    const [outgoingEdges, incomingEdges] = await Promise.all([
        currentLocation.getOutEdges(),
        currentLocation.getInEdges(),
    ]);
    const currentDegree = outgoingEdges.length + incomingEdges.length;

    const destinationIds = new Set<number>();
    for (const edge of outgoingEdges) {
        if (edge.get("world_id") !== worldId) {
            throw new Error("Outgoing edge belongs to another world");
        }
        if (edge.get("from_id") !== currentLocation.get("id")) {
            throw new Error("Outgoing edge has an unexpected source");
        }
        if (edge.get("to_id") === currentLocation.get("id")) {
            throw new Error("A location context edge cannot target itself");
        }
        if (destinationIds.has(edge.get("to_id"))) {
            throw new Error("Duplicate outgoing destination");
        }
        destinationIds.add(edge.get("to_id"));
    }

    const destinations = await Promise.all(
        outgoingEdges.map(edge => fetchLocation(worldId, edge.get("to_id"))),
    );

    const requiredRegionIds = [
        currentRegionId,
        ...destinations
            .map(destination => destination.get("region_id"))
            .filter((id): id is number => id !== null),
    ];
    const regions = await loadRequiredRegionChains(
        worldId,
        requiredRegionIds,
    );
    const currentRegion = regions.find(
        region => region.get("id") === currentRegionId,
    );
    if (currentRegion === undefined) {
        throw new Error("The current region could not be resolved");
    }

    const destinationDegrees = await Promise.all(
        destinations.map(destination => loadDegree(destination)),
    );
    const degreeIndex = new Map<string, number>([
        [locationEntityKey(currentLocation), currentDegree],
    ]);
    destinations.forEach((destination, index) => {
        degreeIndex.set(
            locationEntityKey(destination),
            destinationDegrees[index] ?? 0,
        );
    });

    const projection: LocationContextProjection = {
        worldId,
        currentLocation,
        currentRegion,
        regions,
        neighbours: outgoingEdges.map((edge, index) => {
            const destination = destinations[index];
            if (destination === undefined) {
                throw new Error("Outgoing destination could not be resolved");
            }
            return {
                edge,
                destination,
            };
        }),
        degreeIndex,
    };

    assertLocationContextProjection(projection);
    return projection;
}

export function useLocationContextGraphData(
    location: ComputedRef<Location>,
) {
    const projection = shallowRef<LocationContextProjection | null>(null);
    const isLoading = shallowRef(false);
    const loadError = shallowRef<string | null>(null);
    let generation = 0;

    async function load(): Promise<void> {
        const activeGeneration = ++generation;
        const requestedLocation = location.value;
        isLoading.value = true;
        loadError.value = null;
        projection.value = null;

        try {
            const loaded = await loadLocationContextProjection(requestedLocation);
            if (activeGeneration !== generation) return;
            if (!location.value.equals(requestedLocation)) return;
            projection.value = loaded;
        } catch (error) {
            if (activeGeneration !== generation) return;
            console.error("Unable to load the location context graph", error);
            loadError.value = requestedLocation.get("region_id") === null
                ? "This location is not placed inside a region."
                : "The location context could not be loaded.";
        } finally {
            if (activeGeneration === generation) {
                isLoading.value = false;
            }
        }
    }

    watch(
        () => [
            location.value.get("worldID"),
            location.value.get("id"),
            location.value.get("region_id"),
        ] as const,
        () => {
            void load();
        },
        {immediate: true},
    );

    return {
        projection,
        isLoading,
        loadError,
        reload: load,
    };
}
