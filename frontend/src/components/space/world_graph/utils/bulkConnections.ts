import type {
    BulkConnectionDirection,
    BulkConnectionTopology,
} from "../types";

export type BulkConnectionNode = {
    key: string;
    x: number;
    y: number;
};

export type BulkConnectionPair = {
    sourceKey: string;
    destinationKey: string;
};

type UndirectedPair = readonly [string, string];

function squaredDistance(first: BulkConnectionNode, second: BulkConnectionNode): number {
    const dx = first.x - second.x;
    const dy = first.y - second.y;
    return dx * dx + dy * dy;
}

function allPairLinks(nodes: readonly BulkConnectionNode[]): UndirectedPair[] {
    const result: UndirectedPair[] = [];
    for (let first = 0; first < nodes.length; first += 1) {
        for (let second = first + 1; second < nodes.length; second += 1) {
            const source = nodes[first];
            const destination = nodes[second];
            if (source !== undefined && destination !== undefined) {
                result.push([source.key, destination.key]);
            }
        }
    }
    return result;
}

/** Prim MST with stable input-order tie breaking. */
export function minimumSpanningTree(nodes: readonly BulkConnectionNode[]): readonly UndirectedPair[] {
    if (nodes.length < 2) return [];

    const inTree = new Set<number>([0]);
    const result: UndirectedPair[] = [];

    while (inTree.size < nodes.length) {
        let bestFrom = -1;
        let bestTo = -1;
        let bestDistance = Number.POSITIVE_INFINITY;

        for (const from of inTree) {
            const source = nodes[from];
            if (source === undefined) continue;
            for (let to = 0; to < nodes.length; to += 1) {
                if (inTree.has(to)) continue;
                const destination = nodes[to];
                if (destination === undefined) continue;
                const distance = squaredDistance(source, destination);
                if (
                    distance < bestDistance ||
                    (distance === bestDistance && (bestTo < 0 || to < bestTo)) ||
                    (distance === bestDistance && to === bestTo && from < bestFrom)
                ) {
                    bestDistance = distance;
                    bestFrom = from;
                    bestTo = to;
                }
            }
        }

        if (bestFrom < 0 || bestTo < 0) break;
        const source = nodes[bestFrom];
        const destination = nodes[bestTo];
        if (source === undefined || destination === undefined) break;
        result.push([source.key, destination.key]);
        inTree.add(bestTo);
    }

    return result;
}

/** Orient an undirected connected network outwards from root using BFS. */
export function orientLinksFromRoot(
    links: readonly UndirectedPair[],
    rootKey: string,
): readonly BulkConnectionPair[] {
    const adjacency = new Map<string, string[]>();
    for (const [first, second] of links) {
        const firstNeighbours = adjacency.get(first) ?? [];
        firstNeighbours.push(second);
        adjacency.set(first, firstNeighbours);
        const secondNeighbours = adjacency.get(second) ?? [];
        secondNeighbours.push(first);
        adjacency.set(second, secondNeighbours);
    }

    const result: BulkConnectionPair[] = [];
    const visited = new Set<string>([rootKey]);
    const queue = [rootKey];
    for (let cursor = 0; cursor < queue.length; cursor += 1) {
        const source = queue[cursor];
        if (source === undefined) continue;
        for (const destination of adjacency.get(source) ?? []) {
            if (visited.has(destination)) continue;
            visited.add(destination);
            queue.push(destination);
            result.push({sourceKey: source, destinationKey: destination});
        }
    }
    return result;
}

export function planBulkConnections(
    nodes: readonly BulkConnectionNode[],
    topology: BulkConnectionTopology,
    direction: BulkConnectionDirection,
): readonly BulkConnectionPair[] {
    if (nodes.length < 2) return [];
    const links = topology === "all-pairs" ? allPairLinks(nodes) : minimumSpanningTree(nodes);
    const root = nodes[0];
    if (root === undefined) return [];
    const outward = topology === "all-pairs"
        ? links.map(([sourceKey, destinationKey]) => ({sourceKey, destinationKey}))
        : orientLinksFromRoot(links, root.key);

    if (direction === "one-way") return outward;
    return outward.flatMap(({sourceKey, destinationKey}) => [
        {sourceKey, destinationKey},
        {sourceKey: destinationKey, destinationKey: sourceKey},
    ]);
}
