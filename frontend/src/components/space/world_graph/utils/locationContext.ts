import type {
    Location,
    LocationEdge,
    Position,
    RegionGeometry,
} from "@/domain/World";
import type {
    LocationContextDisclosure,
    LocationContextEdgeRenderModel,
    LocationContextNeighbour,
    LocationContextNodeRenderModel,
    LocationContextProjection,
} from "../types";
import {
    boundsToGeometry,
    buildRegionIndex,
    circleInsideGeometry,
    edgeEntityKey,
    geometryToBounds,
    getAbsoluteLocationPosition,
    getAbsoluteRegionCenter,
    locationEntityKey,
} from "./geometry";
import {locationRadius} from "./graph";
import {makeDirectedEdgePath} from "./rendering";

export const HIDDEN_LOCATION_LABEL = "Unknown location";

export function destinationDisclosure(
    edge: LocationEdge,
    destination: Location,
): LocationContextDisclosure {
    return {
        name: edge.get("show_destination_name")
            ? destination.get("name")
            : null,
        description: edge.get("show_destination_description")
            ? destination.get("description")
            : null,
    };
}

export function locationContextDisplayName(
    disclosure: LocationContextDisclosure,
): string {
    return disclosure.name?.trim() || HIDDEN_LOCATION_LABEL;
}

export function locationContextNodeAccessibleLabel(
    displayName: string,
    description: string | null,
    current: boolean,
): string {
    const prefix = current ? "Current location" : "Neighbour";
    const normalizedDescription = description?.trim() ?? "";
    return normalizedDescription.length > 0
        ? `${prefix}: ${displayName}. ${normalizedDescription}`
        : `${prefix}: ${displayName}`;
}

export function locationContextEdgeAccessibleLabel(
    edge: LocationEdge,
    disclosure: LocationContextDisclosure,
): string {
    const state = edge.get("is_traversable")
        ? "Traversable connection"
        : "Blocked connection";
    return `${state} to ${locationContextDisplayName(disclosure)}`;
}

export function assertLocationContextProjection(
    projection: LocationContextProjection,
): void {
    const current = projection.currentLocation;
    const region = projection.currentRegion;

    if (current.get("worldID") !== projection.worldId) {
        throw new Error("Current location belongs to another world");
    }
    if (region.get("world_id") !== projection.worldId) {
        throw new Error("Current region belongs to another world");
    }
    if (current.get("region_id") !== region.get("id")) {
        throw new Error("Current region does not contain the current location");
    }

    const destinationIds = new Set<number>();
    for (const neighbour of projection.neighbours) {
        const edge = neighbour.edge;
        const destination = neighbour.destination;

        if (edge.get("world_id") !== projection.worldId) {
            throw new Error("Context edge belongs to another world");
        }
        if (edge.get("from_id") !== current.get("id")) {
            throw new Error("Context edge is not outgoing from the current location");
        }
        if (edge.get("to_id") !== destination.get("id")) {
            throw new Error("Context edge destination does not match its location");
        }
        if (destination.get("worldID") !== projection.worldId) {
            throw new Error("Context destination belongs to another world");
        }
        if (destinationIds.has(destination.get("id"))) {
            throw new Error("Duplicate context destination");
        }
        destinationIds.add(destination.get("id"));
    }

    buildRegionIndex(projection.regions);
}

export function locationPositionRelativeToCurrentRegion(
    location: Location,
    projection: LocationContextProjection,
): Position {
    const regionIndex = buildRegionIndex(projection.regions);
    const currentRegionCenter = getAbsoluteRegionCenter(
        projection.currentRegion,
        regionIndex,
    );
    const absolute = getAbsoluteLocationPosition(location, regionIndex);
    return {
        x: absolute.x - currentRegionCenter.x,
        y: absolute.y - currentRegionCenter.y,
    };
}

export function buildLocationContextNodeModels(
    projection: LocationContextProjection,
): readonly LocationContextNodeRenderModel[] {
    assertLocationContextProjection(projection);
    const regionIndex = buildRegionIndex(projection.regions);
    const currentRegionCenter = getAbsoluteRegionCenter(
        projection.currentRegion,
        regionIndex,
    );
    const regionGeometry: RegionGeometry = {
        x: 0,
        y: 0,
        width: projection.currentRegion.get("width"),
        height: projection.currentRegion.get("height"),
    };

    function relativePosition(location: Location): Position {
        const absolute = getAbsoluteLocationPosition(location, regionIndex);
        return {
            x: absolute.x - currentRegionCenter.x,
            y: absolute.y - currentRegionCenter.y,
        };
    }

    const currentPosition = relativePosition(projection.currentLocation);
    const currentRadius = locationRadius(
        projection.currentLocation,
        projection.degreeIndex,
    );
    const result: LocationContextNodeRenderModel[] = [{
        key: locationEntityKey(projection.currentLocation),
        location: projection.currentLocation,
        position: currentPosition,
        radius: currentRadius,
        displayName: projection.currentLocation.get("name"),
        description: projection.currentLocation.get("description"),
        accessibleLabel: locationContextNodeAccessibleLabel(
            projection.currentLocation.get("name"),
            projection.currentLocation.get("description"),
            true,
        ),
        current: true,
        outsideCurrentRegion: !circleInsideGeometry(
            currentPosition,
            currentRadius,
            regionGeometry,
        ),
    }];

    for (const neighbour of projection.neighbours) {
        const position = relativePosition(neighbour.destination);
        const radius = locationRadius(
            neighbour.destination,
            projection.degreeIndex,
        );
        const disclosure = destinationDisclosure(
            neighbour.edge,
            neighbour.destination,
        );
        const displayName = locationContextDisplayName(disclosure);
        result.push({
            key: locationEntityKey(neighbour.destination),
            location: neighbour.destination,
            position,
            radius,
            displayName,
            description: disclosure.description,
            accessibleLabel: locationContextNodeAccessibleLabel(
                displayName,
                disclosure.description,
                false,
            ),
            current: false,
            outsideCurrentRegion: !circleInsideGeometry(
                position,
                radius,
                regionGeometry,
            ),
        });
    }

    return result;
}

export function buildLocationContextEdgeModels(
    projection: LocationContextProjection,
    nodes: readonly LocationContextNodeRenderModel[],
): readonly LocationContextEdgeRenderModel[] {
    const byId = new Map(
        nodes.map(node => [node.location.get("id"), node]),
    );
    const source = byId.get(projection.currentLocation.get("id"));
    if (source === undefined) {
        throw new Error("Current location render node is missing");
    }

    return projection.neighbours.map(neighbour => {
        const destination = byId.get(neighbour.destination.get("id"));
        if (destination === undefined) {
            throw new Error("Destination render node is missing");
        }
        return {
            key: edgeEntityKey(neighbour.edge),
            edge: neighbour.edge,
            source: source.location,
            destination: destination.location,
            path: makeDirectedEdgePath(
                source.position,
                destination.position,
                source.radius,
                destination.radius,
            ),
            traversable: neighbour.edge.get("is_traversable"),
            accessibleLabel: locationContextEdgeAccessibleLabel(
                neighbour.edge,
                destinationDisclosure(
                    neighbour.edge,
                    neighbour.destination,
                ),
            ),
        };
    });
}

export function locationContextSceneGeometry(
    region: Pick<RegionGeometry, "width" | "height">,
    nodes: readonly LocationContextNodeRenderModel[],
    padding = 36,
): RegionGeometry {
    if (!Number.isFinite(padding) || padding < 0) {
        throw new Error("Scene padding must be finite and non-negative");
    }

    let left = -region.width / 2;
    let right = region.width / 2;
    let top = -region.height / 2;
    let bottom = region.height / 2;

    for (const node of nodes) {
        left = Math.min(left, node.position.x - node.radius);
        right = Math.max(right, node.position.x + node.radius);
        top = Math.min(top, node.position.y - node.radius);
        bottom = Math.max(bottom, node.position.y + node.radius);
    }

    const bounds = geometryToBounds(boundsToGeometry({
        left,
        right,
        top,
        bottom,
    }));

    return boundsToGeometry({
        left: bounds.left - padding,
        right: bounds.right + padding,
        top: bounds.top - padding,
        bottom: bounds.bottom + padding,
    });
}

export function neighbourByLocation(
    neighbours: readonly LocationContextNeighbour[],
    location: Location,
): LocationContextNeighbour | null {
    return neighbours.find(neighbour => neighbour.destination.equals(location)) ?? null;
}
