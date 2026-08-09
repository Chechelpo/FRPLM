import {
    computed,
    shallowRef,
    watch,
    type ComputedRef,
} from "vue";
import type {
    Location,
    LocationEdge,
    Position,
    Region,
    RegionGeometry,
    World,
} from "@/domain/World";
import type {
    BidirectionalCreateResult,
    BidirectionalDeleteResult,
    BulkConnectionRequest,
    BulkConnectionResult,
    CanvasScalePersistResult,
    ConnectionState,
    MovementPersistResult,
    MovementPlan,
    CanvasScalePlan,
    LocationRadiusPersistResult,
    WorldGraphData,
} from "../types";
import {
    buildRegionIndex,
    geometryToBounds,
    getAbsoluteLocationPosition,
    getAbsoluteRegionGeometry,
    isRegionDescendantOf,
    isFinitePosition,
    locationEntityKey,
    positionInsideBounds,
    regionEntityKey,
} from "../utils/geometry";
import {
    buildLocationDegreeIndex,
    buildLocationsByRegion,
    buildRegionsByParent,
    getDescendantRegions,
    regionCollapseTargets,
    getVisibleLocationKeys,
    getVisibleRegionKeys,
    loadWorldGraph,
    sortRegionsDeepestFirst,
    sortRegionsForRender,
    validateGraph,
} from "../utils/graph";
import {
    commitPersistedLocationRadius,
    isValidExplicitLocationRadius,
    radiusMutationStillApplies,
} from "../utils/locationRadius";
import {requestLocationRadiusUpdate} from "../utils/locationRadiusPersistence";

const EMPTY_GRAPH: WorldGraphData = {locations: [], regions: [], edges: []};

function findDirectedEdge(
    edges: readonly LocationEdge[],
    source: Location,
    destination: Location,
): LocationEdge | null {
    return edges.find((edge) => (
        edge.get("world_id") === source.get("worldID") &&
        edge.get("from_id") === source.get("id") &&
        edge.get("to_id") === destination.get("id")
    )) ?? null;
}

function wouldCreateCycle(
    region: Region,
    parent: Region | null,
    index: ReadonlyMap<number, Region>,
): boolean {
    if (parent === null) return false;
    const visited = new Set<number>();
    let current: Region | null = parent;
    while (current !== null) {
        const id = current.get("id");
        if (id === region.get("id") || visited.has(id)) return true;
        visited.add(id);
        const parentId: number | null = current.get("parent_region_id");
        current = parentId === null ? null : index.get(parentId) ?? null;
    }
    return false;
}

export function useWorldGraphData(world: ComputedRef<World>) {
    const graph = shallowRef<WorldGraphData>(EMPTY_GRAPH);
    const isLoading = shallowRef(false);
    const loadError = shallowRef<string | null>(null);
    const actionError = shallowRef<string | null>(null);
    let loadGeneration = 0;

    const regionIndex = computed(() => buildRegionIndex(graph.value.regions));
    const regionsByParent = computed(() => buildRegionsByParent(graph.value.regions));
    const locationsByRegion = computed(() => buildLocationsByRegion(graph.value.locations));
    const locationDegreeIndex = computed(() => buildLocationDegreeIndex(graph.value.edges));
    const regionsInRenderOrder = computed(() => sortRegionsForRender(graph.value.regions, regionIndex.value));
    const regionsDeepestFirst = computed(() => sortRegionsDeepestFirst(graph.value.regions, regionIndex.value));
    const visibleRegionKeys = computed(() => getVisibleRegionKeys(regionsInRenderOrder.value, regionIndex.value));
    const structurallyVisibleLocationKeys = computed(() => getVisibleLocationKeys(
        graph.value.locations,
        regionIndex.value,
        visibleRegionKeys.value,
    ));
    const visibleLocationKeys = computed<ReadonlySet<string>>(() => {
        const visible = new Set(structurallyVisibleLocationKeys.value);
        const expandedVisibleRegions = regionsInRenderOrder.value.filter((region) => (
            visibleRegionKeys.value.has(regionEntityKey(region)) &&
            !region.get("collapsed")
        ));

        for (const location of graph.value.locations) {
            const key = locationEntityKey(location);
            if (!visible.has(key)) continue;

            const ownerId = location.get("region_id");
            if (ownerId === null) continue;
            const owner = regionIndex.value.get(ownerId);
            if (owner === undefined) continue;

            const center = getAbsoluteLocationPosition(location, regionIndex.value);
            const occluded = expandedVisibleRegions.some((candidate) => (
                isRegionDescendantOf(candidate, owner, regionIndex.value) &&
                positionInsideBounds(
                    center,
                    geometryToBounds(getAbsoluteRegionGeometry(candidate, regionIndex.value)),
                )
            ));

            if (occluded) visible.delete(key);
        }

        return visible;
    });
    const visibleRegions = computed(() => regionsInRenderOrder.value.filter((region) => (
        visibleRegionKeys.value.has(regionEntityKey(region))
    )));
    const visibleLocations = computed(() => graph.value.locations.filter((location) => (
        visibleLocationKeys.value.has(locationEntityKey(location))
    )));

    function isActiveWorld(expectedWorldId: number): boolean {
        return world.value.get("id") === expectedWorldId;
    }

    function setActionError(expectedWorldId: number, message: string): void {
        if (isActiveWorld(expectedWorldId)) actionError.value = message;
    }

    function clearActionError(): void {
        actionError.value = null;
    }

    async function reload(preserveOnFailure = false): Promise<boolean> {
        const generation = ++loadGeneration;
        const expectedWorldId = world.value.get("id");
        const previous = graph.value;
        isLoading.value = true;
        loadError.value = null;
        actionError.value = null;

        try {
            const loaded = await loadWorldGraph(world.value);
            validateGraph(loaded, expectedWorldId);
            if (generation !== loadGeneration || !isActiveWorld(expectedWorldId)) return false;
            graph.value = loaded;
            return true;
        } catch (error) {
            console.error("Unable to load the world graph", error);
            if (generation !== loadGeneration || !isActiveWorld(expectedWorldId)) return false;
            if (!preserveOnFailure) graph.value = EMPTY_GRAPH;
            else graph.value = previous;
            loadError.value = "The graph could not be loaded. Retry when the connection is available.";
            return false;
        } finally {
            if (generation === loadGeneration && isActiveWorld(expectedWorldId)) {
                isLoading.value = false;
            }
        }
    }

    function validateParent(region: Region): boolean {
        const expectedWorldId = world.value.get("id");
        if (region.get("world_id") !== expectedWorldId) {
            actionError.value = "The selected region does not belong to the active world.";
            return false;
        }
        return true;
    }

    function validateLocationPair(source: Location, destination: Location): boolean {
        const expectedWorldId = world.value.get("id");
        if (source.get("worldID") !== expectedWorldId || destination.get("worldID") !== expectedWorldId) {
            actionError.value = "The selected locations do not belong to the active world.";
            return false;
        }
        if (source.equals(destination)) {
            actionError.value = "A location cannot connect to itself.";
            return false;
        }
        return true;
    }

    async function createRootRegion(name: string, geometry: RegionGeometry): Promise<Region | null> {
        clearActionError();
        const expectedWorldId = world.value.get("id");
        try {
            const created = await world.value.createRootRegion(name, geometry);
            if (!isActiveWorld(expectedWorldId)) return null;
            graph.value = {...graph.value, regions: [...graph.value.regions, created]};
            return created;
        } catch (error) {
            console.error("Unable to create a root region", error);
            setActionError(expectedWorldId, "The region could not be created.");
            return null;
        }
    }

    async function createSubRegion(
        parent: Region,
        name: string,
        geometry: RegionGeometry,
    ): Promise<Region | null> {
        clearActionError();
        const expectedWorldId = world.value.get("id");
        if (!validateParent(parent)) return null;
        if (parent.get("collapsed")) {
            actionError.value = "Expand the region before creating a sub-region.";
            return null;
        }
        try {
            const created = await parent.createSubRegion(name, geometry);
            if (!isActiveWorld(expectedWorldId)) return null;
            graph.value = {...graph.value, regions: [...graph.value.regions, created]};
            return created;
        } catch (error) {
            console.error("Unable to create a sub-region", error);
            setActionError(expectedWorldId, "The sub-region could not be created.");
            return null;
        }
    }

    async function createLocation(
        parent: Region,
        name: string,
        position: Position,
    ): Promise<Location | null> {
        clearActionError();
        const expectedWorldId = world.value.get("id");
        if (!validateParent(parent)) return null;
        if (parent.get("collapsed")) {
            actionError.value = "Expand the region before creating a location.";
            return null;
        }
        try {
            const created = await parent.createLocation(name, position);
            if (!isActiveWorld(expectedWorldId)) return null;
            graph.value = {...graph.value, locations: [...graph.value.locations, created]};
            return created;
        } catch (error) {
            console.error("Unable to create a location", error);
            setActionError(expectedWorldId, "The location could not be created.");
            return null;
        }
    }

    async function deleteLocation(location: Location): Promise<boolean> {
        clearActionError();
        const expectedWorldId = world.value.get("id");
        if (location.get("worldID") !== expectedWorldId) return false;
        try {
            const deleted = await world.value.deleteLocation(location);
            if (!deleted) {
                setActionError(expectedWorldId, "The location could not be deleted.");
                return false;
            }
            if (!isActiveWorld(expectedWorldId)) return true;
            const id = location.get("id");
            graph.value = {
                locations: graph.value.locations.filter((candidate) => !candidate.equals(location)),
                regions: graph.value.regions,
                edges: graph.value.edges.filter((edge) => (
                    edge.get("from_id") !== id && edge.get("to_id") !== id
                )),
            };
            return true;
        } catch (error) {
            console.error("Unable to delete a location", error);
            setActionError(expectedWorldId, "The location could not be deleted.");
            return false;
        }
    }

    async function deleteRegion(region: Region): Promise<boolean> {
        clearActionError();
        const expectedWorldId = world.value.get("id");
        if (!validateParent(region)) return false;
        const subtree = [region, ...getDescendantRegions(region, regionsByParent.value)];
        const removedRegionIds = new Set(subtree.map((item) => item.get("id")));
        const removedLocationIds = new Set(
            graph.value.locations
                .filter((location) => {
                    const regionId = location.get("region_id");
                    return regionId !== null && removedRegionIds.has(regionId);
                })
                .map((location) => location.get("id")),
        );

        try {
            const deleted = await world.value.deleteRegion(region);
            if (!deleted) {
                setActionError(expectedWorldId, "The region could not be deleted.");
                return false;
            }
            if (!isActiveWorld(expectedWorldId)) return true;
            graph.value = {
                regions: graph.value.regions.filter((candidate) => !removedRegionIds.has(candidate.get("id"))),
                locations: graph.value.locations.filter((location) => !removedLocationIds.has(location.get("id"))),
                edges: graph.value.edges.filter((edge) => (
                    !removedLocationIds.has(edge.get("from_id")) &&
                    !removedLocationIds.has(edge.get("to_id"))
                )),
            };
            return true;
        } catch (error) {
            console.error("Unable to delete a region", error);
            setActionError(expectedWorldId, "The region could not be deleted.");
            return false;
        }
    }
    async function setRegionCollapsed(
        region: Region,
        collapsed: boolean,
    ): Promise<boolean> {
        clearActionError();

        const expectedWorldId = world.value.get("id");

        if (!validateParent(region)) {
            return false;
        }

        try {
            /*
             * Collapsing is recursive by contract: descendants become
             * collapsed too. Expanding is deliberately NOT recursive;
             * child regions retain their own collapsed state until the
             * user (or search) explicitly expands them.
             *
             * Use the graph's live Region instances rather than fetching
             * children again so successful updates are immediately
             * reflected by the render/visibility computeds.
             */
            const targets = regionCollapseTargets(
                region,
                collapsed,
                regionsByParent.value,
            );

            const results = await Promise.all(
                targets.map(current => (
                    current.get("collapsed") === collapsed
                        ? Promise.resolve(true)
                        : current.update(
                            "collapsed",
                            collapsed,
                        )
                )),
            );

            const updated = results.every(Boolean);

            if (!updated) {
                setActionError(
                    expectedWorldId,
                    "The region collapse state could not be saved.",
                );
            }

            return updated;
        } catch (error) {
            console.error(
                "Unable to update region collapse state",
                error,
            );

            setActionError(
                expectedWorldId,
                "The region collapse state could not be saved.",
            );

            return false;
        }
    }

    async function createDirectedEdge(
        source: Location,
        destination: Location,
    ): Promise<LocationEdge | null> {
        clearActionError();
        const expectedWorldId = world.value.get("id");
        if (!validateLocationPair(source, destination)) return null;
        if (findDirectedEdge(graph.value.edges, source, destination) !== null) {
            actionError.value = "The directed connection already exists.";
            return null;
        }
        try {
            const created = await source.connect(destination, {});
            if (!isActiveWorld(expectedWorldId)) return null;
            graph.value = {...graph.value, edges: [...graph.value.edges, created]};
            return created;
        } catch (error) {
            console.error("Unable to create a directed connection", error);
            setActionError(expectedWorldId, "The directed connection could not be created.");
            return null;
        }
    }

    async function createBidirectionalConnection(
        first: Location,
        second: Location,
    ): Promise<BidirectionalCreateResult> {
        clearActionError();
        const expectedWorldId = world.value.get("id");
        if (!validateLocationPair(first, second)) return {forward: null, reverse: null};
        if (
            findDirectedEdge(graph.value.edges, first, second) !== null ||
            findDirectedEdge(graph.value.edges, second, first) !== null
        ) {
            actionError.value = "One or both directed connections already exist.";
            return {forward: null, reverse: null};
        }

        const settled = await Promise.allSettled([
            first.connect(second, {}),
            second.connect(first, {}),
        ]);
        const forward = settled[0].status === "fulfilled" ? settled[0].value : null;
        const reverse = settled[1].status === "fulfilled" ? settled[1].value : null;
        if (settled[0].status === "rejected") console.error("Unable to create one connection direction", settled[0].reason);
        if (settled[1].status === "rejected") console.error("Unable to create one connection direction", settled[1].reason);

        if (isActiveWorld(expectedWorldId)) {
            const created = [forward, reverse].filter((edge): edge is LocationEdge => edge !== null);
            if (created.length > 0) graph.value = {...graph.value, edges: [...graph.value.edges, ...created]};
        }
        if (forward === null || reverse === null) {
            setActionError(
                expectedWorldId,
                forward !== null || reverse !== null
                    ? "Only one connection direction was created; the other could not be saved."
                    : "The bidirectional connection could not be created.",
            );
        }
        return {forward, reverse};
    }

    async function createBulkConnections(
        requests: readonly BulkConnectionRequest[],
        isTraversable: boolean,
    ): Promise<BulkConnectionResult> {
        clearActionError();
        const expectedWorldId = world.value.get("id");
        const skipped: BulkConnectionRequest[] = [];
        const failed: BulkConnectionRequest[] = [];
        const pending: BulkConnectionRequest[] = [];
        const knownDirections = new Set(graph.value.edges.map((edge) => (
            `${edge.get("world_id")}:${edge.get("from_id")}:${edge.get("to_id")}`
        )));

        for (const request of requests) {
            if (
                request.source.get("worldID") !== expectedWorldId ||
                request.destination.get("worldID") !== expectedWorldId ||
                request.source.equals(request.destination)
            ) {
                failed.push(request);
                continue;
            }
            const identity = `${expectedWorldId}:${request.source.get("id")}:${request.destination.get("id")}`;
            if (knownDirections.has(identity)) {
                skipped.push(request);
                continue;
            }
            knownDirections.add(identity);
            pending.push(request);
        }

        const settled = await Promise.allSettled(pending.map((request) => (
            request.source.connect(request.destination, {is_traversable: isTraversable})
        )));
        const created: BulkConnectionRequest[] = [];
        const createdEdges: LocationEdge[] = [];

        settled.forEach((result, index) => {
            const request = pending[index];
            if (request === undefined) return;
            if (result.status === "fulfilled") {
                created.push(request);
                createdEdges.push(result.value);
            } else {
                console.error("Unable to create one bulk connection direction", result.reason);
                failed.push(request);
            }
        });

        if (isActiveWorld(expectedWorldId) && createdEdges.length > 0) {
            graph.value = {...graph.value, edges: [...graph.value.edges, ...createdEdges]};
        }
        if (failed.length > 0) {
            const skippedSuffix = skipped.length > 0
                ? ` ${skipped.length} existing directions were kept unchanged.`
                : "";
            setActionError(
                expectedWorldId,
                created.length > 0
                    ? `${created.length} bulk connection directions were created, but ${failed.length} could not be saved.${skippedSuffix}`
                    : `The bulk connections could not be created.${skippedSuffix}`,
            );
        }
        return {created, skipped, failed};
    }

    async function deleteDirectedEdge(source: Location, destination: Location): Promise<boolean> {
        clearActionError();
        const expectedWorldId = world.value.get("id");
        if (!validateLocationPair(source, destination)) return false;
        try {
            const deleted = await source.disconnect(destination);
            if (!deleted) {
                setActionError(expectedWorldId, "The directed connection could not be deleted.");
                return false;
            }
            if (isActiveWorld(expectedWorldId)) {
                graph.value = {
                    ...graph.value,
                    edges: graph.value.edges.filter((edge) => !(
                        edge.get("world_id") === expectedWorldId &&
                        edge.get("from_id") === source.get("id") &&
                        edge.get("to_id") === destination.get("id")
                    )),
                };
            }
            return true;
        } catch (error) {
            console.error("Unable to delete a directed connection", error);
            setActionError(expectedWorldId, "The directed connection could not be deleted.");
            return false;
        }
    }

    async function deleteBidirectionalConnection(
        first: Location,
        second: Location,
    ): Promise<BidirectionalDeleteResult> {
        clearActionError();
        const expectedWorldId = world.value.get("id");
        if (!validateLocationPair(first, second)) return {forwardDeleted: false, reverseDeleted: false};
        const state = getConnectionState(first, second);
        const directions: Array<{kind: "forward" | "reverse"; promise: Promise<boolean>}> = [];
        if (state.forward !== null) directions.push({kind: "forward", promise: first.disconnect(second)});
        if (state.reverse !== null) directions.push({kind: "reverse", promise: second.disconnect(first)});
        const settled = await Promise.allSettled(directions.map(({promise}) => promise));
        let forwardDeleted = state.forward === null;
        let reverseDeleted = state.reverse === null;

        settled.forEach((result, index) => {
            const direction = directions[index];
            if (direction === undefined) return;
            const deleted = result.status === "fulfilled" && result.value;
            if (result.status === "rejected") console.error("Unable to delete one connection direction", result.reason);
            if (direction.kind === "forward") forwardDeleted = deleted;
            else reverseDeleted = deleted;
        });

        if (isActiveWorld(expectedWorldId)) {
            graph.value = {
                ...graph.value,
                edges: graph.value.edges.filter((edge) => {
                    const isForward = edge.get("from_id") === first.get("id") && edge.get("to_id") === second.get("id");
                    const isReverse = edge.get("from_id") === second.get("id") && edge.get("to_id") === first.get("id");
                    return !(forwardDeleted && isForward) && !(reverseDeleted && isReverse);
                }),
            };
        }

        const failed = (!forwardDeleted && state.forward !== null) || (!reverseDeleted && state.reverse !== null);
        if (failed) {
            setActionError(
                expectedWorldId,
                forwardDeleted || reverseDeleted
                    ? "Only one connection direction was deleted; the other remains."
                    : "The connection directions could not be deleted.",
            );
        }
        return {forwardDeleted, reverseDeleted};
    }

    function getConnectionState(first: Location, second: Location): ConnectionState {
        return {
            forward: findDirectedEdge(graph.value.edges, first, second),
            reverse: findDirectedEdge(graph.value.edges, second, first),
        };
    }

    async function persistLocationRadius(
        location: Location,
        radius: number | null,
    ): Promise<LocationRadiusPersistResult> {
        clearActionError();
        const expectedWorldId = world.value.get("id");
        const key = locationEntityKey(location);
        const availableAtStart = graph.value.locations.some((candidate) => candidate.equals(location));

        if (
            location.get("worldID") !== expectedWorldId ||
            !availableAtStart ||
            (radius !== null && !isValidExplicitLocationRadius(radius))
        ) {
            setActionError(expectedWorldId, "The location size is not valid.");
            return {ok: false};
        }

        try {
            const confirmedRadius = await requestLocationRadiusUpdate(location, radius);
            const availableKeys = new Set(graph.value.locations.map(locationEntityKey));
            if (radiusMutationStillApplies(
                {worldId: expectedWorldId, locationKey: key},
                world.value.get("id"),
                availableKeys,
            )) {
                commitPersistedLocationRadius(location, confirmedRadius);
                graph.value = {...graph.value};
            }
            return {ok: true, radius: confirmedRadius};
        } catch (error) {
            console.error("Unable to persist the location radius", error);
            setActionError(expectedWorldId, "The location size could not be saved.");
            return {ok: false};
        }
    }

    async function persistMovement(plan: MovementPlan): Promise<MovementPersistResult> {
        clearActionError();
        const expectedWorldId = plan.worldId;
        const invalidRegion = plan.regions.some((update) => (
            update.region.get("world_id") !== expectedWorldId ||
            (update.parent !== null && update.parent.get("world_id") !== expectedWorldId) ||
            !isFinitePosition(update.localCenter) ||
            !isFinitePosition(update.absoluteCenter) ||
            wouldCreateCycle(update.region, update.parent, regionIndex.value)
        ));
        const invalidLocation = plan.locations.some((update) => (
            update.location.get("worldID") !== expectedWorldId ||
            update.region.get("world_id") !== expectedWorldId ||
            !isFinitePosition(update.localCenter) ||
            !isFinitePosition(update.absoluteCenter)
        ));
        if (invalidRegion || invalidLocation || !isActiveWorld(expectedWorldId)) {
            setActionError(expectedWorldId, "The selected entities have an invalid final placement.");
            return {
                regions: {succeeded: [], failed: [...plan.regions]},
                locations: {succeeded: [], failed: [...plan.locations]},
            };
        }

        const regionSettled = await Promise.allSettled(plan.regions.map(async (update) => ({
            update,
            persisted: await update.region.updateMany({
                parent_region_id: update.parent?.get("id") ?? null,
                x: update.localCenter.x,
                y: update.localCenter.y,
            }),
        })));
        const locationSettled = await Promise.allSettled(plan.locations.map(async (update) => ({
            update,
            persisted: await update.location.updateMany({
                region_id: update.region.get("id"),
                x: update.localCenter.x,
                y: update.localCenter.y,
            }),
        })));

        const succeededRegions = [] as typeof plan.regions[number][];
        const failedRegions = [] as typeof plan.regions[number][];
        regionSettled.forEach((result, index) => {
            const update = plan.regions[index];
            if (update === undefined) return;
            if (result.status === "fulfilled" && result.value.persisted) succeededRegions.push(update);
            else {
                if (result.status === "rejected") console.error("Unable to persist a region placement", result.reason);
                failedRegions.push(update);
            }
        });
        const succeededLocations = [] as typeof plan.locations[number][];
        const failedLocations = [] as typeof plan.locations[number][];
        locationSettled.forEach((result, index) => {
            const update = plan.locations[index];
            if (update === undefined) return;
            if (result.status === "fulfilled" && result.value.persisted) succeededLocations.push(update);
            else {
                if (result.status === "rejected") console.error("Unable to persist a location placement", result.reason);
                failedLocations.push(update);
            }
        });

        const failures = failedRegions.length + failedLocations.length;
        const successes = succeededRegions.length + succeededLocations.length;
        if (isActiveWorld(expectedWorldId)) {
            if (failures > 0 && successes > 0) await reload(true);
            else graph.value = {...graph.value};
        }
        if (failures > 0) {
            setActionError(
                expectedWorldId,
                successes > 0
                    ? "Some selected entities moved, but other placements could not be saved."
                    : "The selected entities could not be moved.",
            );
        }
        return {
            regions: {succeeded: succeededRegions, failed: failedRegions},
            locations: {succeeded: succeededLocations, failed: failedLocations},
        };
    }

    async function persistCanvasScale(plan: CanvasScalePlan): Promise<CanvasScalePersistResult> {
        clearActionError();
        const expectedWorldId = plan.worldId;
        if (!isActiveWorld(expectedWorldId)) {
            return {
                worldSucceeded: plan.worldCanvas === null ? null : false,
                regions: {succeeded: [], failed: [...plan.regions]},
                locations: {succeeded: [], failed: [...plan.locations]},
            };
        }

        const worldPromise = plan.worldCanvas === null
            ? null
            : plan.world.updateMany({
                background_x: 0,
                background_y: 0,
                background_width: plan.worldCanvas.width,
                background_height: plan.worldCanvas.height,
            });
        const regionPromises = plan.regions.map(async (update) => ({
            update,
            persisted: await update.region.updateGeometry(update.localGeometry),
        }));
        const locationPromises = plan.locations.map(async (update) => ({
            update,
            persisted: await update.location.updatePosition(update.localPosition),
        }));

        const [worldSettled, regionSettled, locationSettled] = await Promise.all([
            worldPromise === null ? Promise.resolve(null) : Promise.allSettled([worldPromise]).then(([result]) => result),
            Promise.allSettled(regionPromises),
            Promise.allSettled(locationPromises),
        ]);

        const worldSucceeded = worldSettled === null
            ? null
            : worldSettled.status === "fulfilled" && worldSettled.value;
        if (worldSettled !== null && worldSettled.status === "rejected") {
            console.error("Unable to persist world canvas scale", worldSettled.reason);
        }

        const succeededRegions = [] as typeof plan.regions[number][];
        const failedRegions = [] as typeof plan.regions[number][];
        regionSettled.forEach((result, index) => {
            const update = plan.regions[index];
            if (update === undefined) return;
            if (result.status === "fulfilled" && result.value.persisted) succeededRegions.push(update);
            else {
                if (result.status === "rejected") console.error("Unable to persist scaled region geometry", result.reason);
                failedRegions.push(update);
            }
        });
        const succeededLocations = [] as typeof plan.locations[number][];
        const failedLocations = [] as typeof plan.locations[number][];
        locationSettled.forEach((result, index) => {
            const update = plan.locations[index];
            if (update === undefined) return;
            if (result.status === "fulfilled" && result.value.persisted) succeededLocations.push(update);
            else {
                if (result.status === "rejected") console.error("Unable to persist scaled location position", result.reason);
                failedLocations.push(update);
            }
        });

        const failedCount = failedRegions.length + failedLocations.length + (worldSucceeded === false ? 1 : 0);
        const succeededCount = succeededRegions.length + succeededLocations.length + (worldSucceeded === true ? 1 : 0);
        if (isActiveWorld(expectedWorldId)) {
            if (failedCount > 0 && succeededCount > 0) await reload(true);
            else graph.value = {...graph.value};
        }
        if (failedCount > 0) {
            setActionError(
                expectedWorldId,
                succeededCount > 0
                    ? "The canvas scale was only partially saved. The graph was reconciled with confirmed changes."
                    : "The canvas scale could not be saved.",
            );
        }
        return {
            worldSucceeded,
            regions: {succeeded: succeededRegions, failed: failedRegions},
            locations: {succeeded: succeededLocations, failed: failedLocations},
        };
    }

    watch(
        () => world.value.get("id"),
        () => {
            graph.value = EMPTY_GRAPH;
            void reload();
        },
        {immediate: true},
    );

    return {
        graph,
        isLoading,
        loadError,
        actionError,
        regionIndex,
        regionsByParent,
        locationsByRegion,
        locationDegreeIndex,
        regionsInRenderOrder,
        regionsDeepestFirst,
        visibleRegionKeys,
        visibleLocationKeys,
        visibleRegions,
        visibleLocations,
        reload,
        createRootRegion,
        createSubRegion,
        createLocation,
        deleteLocation,
        deleteRegion,
        setRegionCollapsed,
        createDirectedEdge,
        createBidirectionalConnection,
        createBulkConnections,
        deleteDirectedEdge,
        deleteBidirectionalConnection,
        getConnectionState,
        persistLocationRadius,
        persistMovement,
        persistCanvasScale,
        clearActionError,
    };
}
