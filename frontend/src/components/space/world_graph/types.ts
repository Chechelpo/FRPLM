import type {
    Location,
    LocationEdge,
    Position,
    Region,
    RegionGeometry,
    World,
} from "@/domain/World";

/** Complete entity set required by the authored world graph. */
export type WorldGraphData = {
    locations: Location[];
    regions: Region[];
    edges: LocationEdge[];
};

/** Center-based rectangle represented by its outer bounds. */
export type Bounds = {
    left: number;
    right: number;
    top: number;
    bottom: number;
};

export type WorldCanvasGeometry = {
    width: number;
    height: number;
};

export type SelectedGraphEntity =
    | {kind: "location"; location: Location}
    | {kind: "region"; region: Region};

/** Absolute preview geometry keyed by stable graph entity key. */
export type RegionPreviewMap = ReadonlyMap<string, RegionGeometry>;
export type LocationPreviewMap = ReadonlyMap<string, Position>;

export type LocationRadiusState = {
    activePreviewRadius: ReadonlyMap<string, number>;
    pendingRadius: ReadonlyMap<string, number>;
    };

/**
 * How a graph edit request should be consumed by an inspector host.
 *
 * Pointer/context-menu edit actions are ordinary open requests. The keyboard
 * E shortcut uses toggle-if-current so repeating the command for the entity
 * already shown in the inspector closes that inspector without suppressing
 * the edit event itself.
 */
export type GraphEntityEditIntent = "open" | "toggle-if-current";

export type LocationRadiusPersistResult =
    | {ok: true; radius: number | null}
    | {ok: false};

export type RegionPlacement = {
    region: Region;
    parent: Region | null;
    absoluteCenter: Position;
    localCenter: Position;
};

export type LocationPlacement = {
    location: Location;
    region: Region;
    absoluteCenter: Position;
    localCenter: Position;
};

export type MovementPlan = {
    worldId: number;
    regions: readonly RegionPlacement[];
    locations: readonly LocationPlacement[];
};

export type RegionScaleUpdate = {
    region: Region;
    localGeometry: RegionGeometry;
};

export type LocationScaleUpdate = {
    location: Location;
    localPosition: Position;
};

export type CanvasScalePlan = {
    worldId: number;
    world: World;
    worldCanvas: WorldCanvasGeometry | null;
    regions: readonly RegionScaleUpdate[];
    locations: readonly LocationScaleUpdate[];
};

export type MutationResult<T> = {
    succeeded: readonly T[];
    failed: readonly T[];
};

export type MovementPersistResult = {
    regions: MutationResult<RegionPlacement>;
    locations: MutationResult<LocationPlacement>;
};

export type CanvasScalePersistResult = {
    worldSucceeded: boolean | null;
    regions: MutationResult<RegionScaleUpdate>;
    locations: MutationResult<LocationScaleUpdate>;
};

export type ConnectionState = {
    forward: LocationEdge | null;
    reverse: LocationEdge | null;
};

export type BidirectionalCreateResult = {
    forward: LocationEdge | null;
    reverse: LocationEdge | null;
};

export type BidirectionalDeleteResult = {
    forwardDeleted: boolean;
    reverseDeleted: boolean;
};

export type BulkConnectionTopology = "all-pairs" | "nearest-network";
export type BulkConnectionDirection = "one-way" | "bidirectional";

export type BulkConnectionOptions = {
    topology: BulkConnectionTopology;
    direction: BulkConnectionDirection;
    isTraversable: boolean;
};

export type BulkConnectionRequest = {
    source: Location;
    destination: Location;
};

export type BulkConnectionResult = {
    created: readonly BulkConnectionRequest[];
    skipped: readonly BulkConnectionRequest[];
    failed: readonly BulkConnectionRequest[];
};

export type BackgroundTarget =
    | {kind: "world"; world: World}
    | {kind: "region"; region: Region};

/** Destination data that one directed edge permits the user-facing graph to expose. */
export type LocationContextDisclosure = {
    name: string | null;
    description: string | null;
};

/** One destination reached through an outgoing edge from the current location. */
export type LocationContextNeighbour = {
    edge: LocationEdge;
    destination: Location;
};

/** Read-only projection required by LocationContextGraph. */
export type LocationContextProjection = {
    worldId: number;
    currentLocation: Location;
    currentRegion: Region;
    regions: readonly Region[];
    neighbours: readonly LocationContextNeighbour[];
    degreeIndex: ReadonlyMap<string, number>;
};

export type LocationContextNodeRenderModel = {
    key: string;
    location: Location;
    position: Position;
    radius: number;
    displayName: string;
    description: string | null;
    accessibleLabel: string;
    current: boolean;
    outsideCurrentRegion: boolean;
};

export type LocationContextEdgeRenderModel = {
    key: string;
    edge: LocationEdge;
    source: Location;
    destination: Location;
    path: string;
    traversable: boolean;
    accessibleLabel: string;
};
