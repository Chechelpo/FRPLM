import type {LocationEdge} from "@frplm/host-sdk";

export const LOCATION_EDGE_TRAVERSABILITY_EVENT = "simulith:location-edge-traversability";

export type LocationEdgeTraversabilityChange = {
    worldId: number;
    fromId: number;
    toId: number;
    isTraversable: boolean;
};

export function edgeMatchesTraversabilityChange(
    edge: LocationEdge,
    change: LocationEdgeTraversabilityChange,
): boolean {
    return edge.get("world_id") === change.worldId &&
        edge.get("from_id") === change.fromId &&
        edge.get("to_id") === change.toId;
}

export function synchronizeEdgeTraversability(
    edges: readonly LocationEdge[],
    change: LocationEdgeTraversabilityChange,
): boolean {
    const edge = edges.find(candidate => edgeMatchesTraversabilityChange(candidate, change));
    if (edge === undefined) return false;
    if (edge.get("is_traversable") === change.isTraversable) return true;
    edge.dataMap.is_traversable = change.isTraversable;
    return true;
}

export function publishEdgeTraversability(edge: LocationEdge): void {
    if (typeof window === "undefined") return;
    window.dispatchEvent(new CustomEvent<LocationEdgeTraversabilityChange>(
        LOCATION_EDGE_TRAVERSABILITY_EVENT,
        {
            detail: {
                worldId: edge.get("world_id"),
                fromId: edge.get("from_id"),
                toId: edge.get("to_id"),
                isTraversable: edge.get("is_traversable"),
            },
        },
    ));
}

export function subscribeEdgeTraversability(
    listener: (change: LocationEdgeTraversabilityChange) => void,
): () => void {
    if (typeof window === "undefined") return () => undefined;
    const handler = (event: Event): void => {
        listener((event as CustomEvent<LocationEdgeTraversabilityChange>).detail);
    };
    window.addEventListener(LOCATION_EDGE_TRAVERSABILITY_EVENT, handler);
    return () => window.removeEventListener(LOCATION_EDGE_TRAVERSABILITY_EVENT, handler);
}
