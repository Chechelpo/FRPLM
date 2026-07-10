<!-- Updated from the supplied graph component. :contentReference[oaicite:0]{index=0} -->
<script setup lang="ts">
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
  watch,
} from "vue";

import {
  Location,
  LocationEdge,
  World,
} from "@/domain/World";

import SearchBar from "@/components/utils/SearchBar.vue";
import SplitPanel from "@/components/utils/panels/SplitPanel.vue";

const props = defineProps<{
  world: World;
}>();

interface GraphNode {
  id: number;
  location: Location;
  name: string;

  x: number;
  y: number;

  velocityX: number;
  velocityY: number;

  inDegree: number;
  outDegree: number;
  degree: number;

  component: number;
}

interface GraphEdge {
  id: string;

  sourceId: number;
  targetId: number;

  entity: LocationEdge;

  description: string;

  showDestinationName: boolean;
  showDestinationDescription: boolean;
  traversable: boolean;

  reciprocal: boolean;
}

interface RenderedEdge extends GraphEdge {
  source: GraphNode;
  target: GraphNode;

  path: string;

  labelX: number;
  labelY: number;
}

interface NodeConnection {
  edge: GraphEdge;
  other: GraphNode;
  direction: "incoming" | "outgoing";
}

interface SavedNodePosition {
  x: number;
  y: number;
}

interface SavedGraphLayout {
  version: 1;

  nodes: Record<
      string,
      SavedNodePosition
  >;

  viewport: {
    zoom: number;
    panX: number;
    panY: number;
  };
}

const VIEW_WIDTH = 1200;
const VIEW_HEIGHT = 760;

const NODE_RADIUS_MIN = 17;
const NODE_RADIUS_MAX = 29;

const GRAPH_LAYOUT_STORAGE_PREFIX =
    "world-location-directed-graph-layout:v1:";

const GRAPH_LAYOUT_SAVE_DELAY = 250;

const graphSvg =
    ref<SVGSVGElement | null>(null);

const nodes = ref<GraphNode[]>([]);
const edges = ref<GraphEdge[]>([]);

const loading = ref(false);
const loadError = ref<string | null>(null);
const layoutRunning = ref(false);

const externalEdgeCount = ref(0);
const invalidLocationCount = ref(0);
const invalidEdgeCount = ref(0);
const selfLoopCount = ref(0);

const selectedNodeId =
    ref<number | null>(null);

const selectedEdgeId =
    ref<string | null>(null);

const searchTerm = ref("");

const showNodeLabels = ref(true);
const showEdgeLabels = ref(true);

const zoom = ref(1);
const panX = ref(0);
const panY = ref(0);

const draggingNodeId =
    ref<number | null>(null);

const panning = ref(false);

const pointerStartX = ref(0);
const pointerStartY = ref(0);

const panStartX = ref(0);
const panStartY = ref(0);

const loadedWorldStorageId =
    ref<string | null>(null);

let graphLayoutSaveTimer:
    | ReturnType<typeof setTimeout>
    | null = null;

let layoutFrame: number | null = null;
let loadRequestId = 0;

const worldName = computed(() =>
    String(
        props.world.get("name") ??
        "Unnamed world",
    ),
);

const graphIdentifier = computed(() => {
  const rawId = String(
      props.world.get("id") ??
      "unknown",
  );

  return rawId.replace(
      /[^a-zA-Z0-9_-]/g,
      "-",
  );
});

const normalArrowMarkerId = computed(
    () =>
        `location-graph-arrow-${graphIdentifier.value}`,
);

const selectedArrowMarkerId = computed(
    () =>
        `location-graph-arrow-selected-${graphIdentifier.value}`,
);

const blockedArrowMarkerId = computed(
    () =>
        `location-graph-arrow-blocked-${graphIdentifier.value}`,
);

const nodeMap = computed(() => {
  const map = new Map<number, GraphNode>();

  for (const node of nodes.value) {
    map.set(node.id, node);
  }

  return map;
});

const edgeMap = computed(() => {
  const map = new Map<string, GraphEdge>();

  for (const edge of edges.value) {
    map.set(edge.id, edge);
  }

  return map;
});

const renderedEdges = computed<RenderedEdge[]>(
    () =>
        edges.value.flatMap((edge) => {
          const source =
              nodeMap.value.get(
                  edge.sourceId,
              );

          const target =
              nodeMap.value.get(
                  edge.targetId,
              );

          if (!source || !target) {
            return [];
          }

          const geometry =
              edgeGeometry(
                  edge,
                  source,
                  target,
              );

          return [
            {
              ...edge,
              source,
              target,
              ...geometry,
            },
          ];
        }),
);

const selectedNode = computed<
    GraphNode | null
>(() => {
  if (selectedNodeId.value == null) {
    return null;
  }

  return (
      nodeMap.value.get(
          selectedNodeId.value,
      ) ?? null
  );
});

const selectedEdge = computed<
    GraphEdge | null
>(() => {
  if (!selectedEdgeId.value) {
    return null;
  }

  return (
      edgeMap.value.get(
          selectedEdgeId.value,
      ) ?? null
  );
});

const selectedEdgeSource = computed<
    GraphNode | null
>(() => {
  if (!selectedEdge.value) {
    return null;
  }

  return (
      nodeMap.value.get(
          selectedEdge.value.sourceId,
      ) ?? null
  );
});

const selectedEdgeTarget = computed<
    GraphNode | null
>(() => {
  if (!selectedEdge.value) {
    return null;
  }

  return (
      nodeMap.value.get(
          selectedEdge.value.targetId,
      ) ?? null
  );
});

const selectedReverseEdge = computed<
    GraphEdge | null
>(() => {
  const edge = selectedEdge.value;

  if (!edge) {
    return null;
  }

  return (
      edgeMap.value.get(
          edgeIdentifier(
              edge.targetId,
              edge.sourceId,
          ),
      ) ?? null
  );
});

const selectedConnections = computed<
    NodeConnection[]
>(() => {
  const selected = selectedNode.value;

  if (!selected) {
    return [];
  }

  const result: NodeConnection[] = [];

  for (const edge of edges.value) {
    if (edge.sourceId === selected.id) {
      const other =
          nodeMap.value.get(
              edge.targetId,
          );

      if (other) {
        result.push({
          edge,
          other,
          direction: "outgoing",
        });
      }
    }

    if (edge.targetId === selected.id) {
      const other =
          nodeMap.value.get(
              edge.sourceId,
          );

      if (other) {
        result.push({
          edge,
          other,
          direction: "incoming",
        });
      }
    }
  }

  return result.sort(
      (first, second) => {
        const nameComparison =
            first.other.name.localeCompare(
                second.other.name,
            );

        if (nameComparison !== 0) {
          return nameComparison;
        }

        return first.direction.localeCompare(
            second.direction,
        );
      },
  );
});

const selectedNeighbours = computed<
    GraphNode[]
>(() => {
  const unique = new Map<
      number,
      GraphNode
  >();

  for (
      const connection of
      selectedConnections.value
      ) {
    unique.set(
        connection.other.id,
        connection.other,
    );
  }

  return [...unique.values()].sort(
      (first, second) =>
          first.name.localeCompare(
              second.name,
          ),
  );
});

const componentCount = computed(() => {
  if (!nodes.value.length) {
    return 0;
  }

  return (
      Math.max(
          ...nodes.value.map(
              (node) => node.component,
          ),
      ) + 1
  );
});

const isolatedNodeCount = computed(
    () =>
        nodes.value.filter(
            (node) =>
                node.degree === 0,
        ).length,
);

const traversableEdgeCount = computed(
    () =>
        edges.value.filter(
            (edge) => edge.traversable,
        ).length,
);

const blockedEdgeCount = computed(
    () =>
        edges.value.filter(
            (edge) => !edge.traversable,
        ).length,
);

const reciprocalPairCount = computed(
    () => {
      const pairs = new Set<string>();

      for (const edge of edges.value) {
        if (!edge.reciprocal) {
          continue;
        }

        pairs.add(
            undirectedEdgeIdentifier(
                edge.sourceId,
                edge.targetId,
            ),
        );
      }

      return pairs.size;
    },
);

const highestDegree = computed(() => {
  if (!nodes.value.length) {
    return 0;
  }

  return Math.max(
      ...nodes.value.map(
          (node) => node.degree,
      ),
  );
});

const normalizedSearchTerm = computed(
    () => normalize(searchTerm.value),
);

const matchingNodeIds = computed(() => {
  const query =
      normalizedSearchTerm.value;

  if (!query) {
    return new Set<number>();
  }

  return new Set(
      nodes.value
          .filter(
              (node) =>
                  normalize(node.name)
                      .includes(query) ||
                  String(node.id)
                      .includes(query),
          )
          .map((node) => node.id),
  );
});

const matchingEdgeIds = computed(() => {
  const query =
      normalizedSearchTerm.value;

  if (!query) {
    return new Set<string>();
  }

  return new Set(
      edges.value
          .filter((edge) => {
            const source =
                nodeMap.value.get(
                    edge.sourceId,
                );

            const target =
                nodeMap.value.get(
                    edge.targetId,
                );

            return (
                normalize(
                    edge.description,
                ).includes(query) ||
                normalize(
                    source?.name,
                ).includes(query) ||
                normalize(
                    target?.name,
                ).includes(query) ||
                edge.id.includes(query)
            );
          })
          .map((edge) => edge.id),
  );
});

const graphTransform = computed(
    () =>
        `translate(${panX.value} ${panY.value}) ` +
        `scale(${zoom.value})`,
);

const hasDiagnostics = computed(
    () =>
        componentCount.value > 1 ||
        isolatedNodeCount.value > 0 ||
        externalEdgeCount.value > 0 ||
        invalidLocationCount.value > 0 ||
        invalidEdgeCount.value > 0 ||
        selfLoopCount.value > 0,
);

function normalize(
    value: unknown,
): string {
  return String(value ?? "")
      .trim()
      .toLocaleLowerCase()
      .normalize("NFKD")
      .replace(/\p{Diacritic}/gu, "");
}

function getLocationId(
    location: Location,
): number | null {
  const id = Number(
      location.get("id"),
  );

  return Number.isFinite(id)
      ? id
      : null;
}

function getLocationName(
    location: Location,
): string {
  const name = String(
      location.get("name") ?? "",
  ).trim();

  return name || "Unnamed location";
}

function edgeIdentifier(
    sourceId: number,
    targetId: number,
): string {
  return `${sourceId}:${targetId}`;
}

function undirectedEdgeIdentifier(
    firstId: number,
    secondId: number,
): string {
  return [
    Math.min(firstId, secondId),
    Math.max(firstId, secondId),
  ].join(":");
}

function nodeRadius(
    node: GraphNode,
): number {
  if (highestDegree.value === 0) {
    return NODE_RADIUS_MIN;
  }

  const normalizedDegree =
      node.degree /
      highestDegree.value;

  return (
      NODE_RADIUS_MIN +
      normalizedDegree *
      (
          NODE_RADIUS_MAX -
          NODE_RADIUS_MIN
      )
  );
}

function edgeGeometry(
    edge: GraphEdge,
    source: GraphNode,
    target: GraphNode,
): {
  path: string;
  labelX: number;
  labelY: number;
} {
  const deltaX =
      target.x - source.x;

  const deltaY =
      target.y - source.y;

  const distance =
      Math.sqrt(
          deltaX * deltaX +
          deltaY * deltaY,
      ) || 1;

  const directionX =
      deltaX / distance;

  const directionY =
      deltaY / distance;

  const sourcePadding =
      nodeRadius(source) + 5;

  const targetPadding =
      nodeRadius(target) + 11;

  const startX =
      source.x +
      directionX * sourcePadding;

  const startY =
      source.y +
      directionY * sourcePadding;

  const endX =
      target.x -
      directionX * targetPadding;

  const endY =
      target.y -
      directionY * targetPadding;

  if (!edge.reciprocal) {
    return {
      path:
          `M ${startX} ${startY} ` +
          `L ${endX} ${endY}`,

      labelX:
          (startX + endX) / 2,

      labelY:
          (startY + endY) / 2,
    };
  }

  /*
   * The normal reverses naturally for the
   * reverse edge, so both edges curve toward
   * opposite sides without another sign.
   */
  const normalX = -directionY;
  const normalY = directionX;

  const curveDistance = 38;

  const controlX =
      (startX + endX) / 2 +
      normalX * curveDistance;

  const controlY =
      (startY + endY) / 2 +
      normalY * curveDistance;

  /*
   * Quadratic Bézier position at t = 0.5:
   *
   * 0.25P0 + 0.5P1 + 0.25P2
   */
  const labelX =
      startX * 0.25 +
      controlX * 0.5 +
      endX * 0.25;

  const labelY =
      startY * 0.25 +
      controlY * 0.5 +
      endY * 0.25;

  return {
    path:
        `M ${startX} ${startY} ` +
        `Q ${controlX} ${controlY} ` +
        `${endX} ${endY}`,

    labelX,
    labelY,
  };
}

function edgeLabel(
    edge: GraphEdge,
): string {
  const description =
      edge.description.trim();

  if (description) {
    return description.length > 28
        ? `${description.slice(0, 26)}…`
        : description;
  }

  return edge.traversable
      ? "Traversable"
      : "Blocked";
}

function edgeLabelWidth(
    edge: GraphEdge,
): number {
  return Math.min(
      190,
      Math.max(
          76,
          edgeLabel(edge).length * 6.2 +
          24,
      ),
  );
}

function booleanLabel(
    value: boolean,
): string {
  return value
      ? "Enabled"
      : "Disabled";
}

function edgeMarker(
    edge: GraphEdge,
): string {
  if (
      selectedEdgeId.value === edge.id
  ) {
    return (
        `url(#${selectedArrowMarkerId.value})`
    );
  }

  if (!edge.traversable) {
    return (
        `url(#${blockedArrowMarkerId.value})`
    );
  }

  return (
      `url(#${normalArrowMarkerId.value})`
  );
}

function isSelectedOrRelatedEdge(
    edge: GraphEdge,
): boolean {
  if (
      selectedEdgeId.value === edge.id
  ) {
    return true;
  }

  const selectedId =
      selectedNodeId.value;

  if (selectedId == null) {
    return false;
  }

  return (
      edge.sourceId === selectedId ||
      edge.targetId === selectedId
  );
}

function isEdgeDimmed(
    edge: GraphEdge,
): boolean {
  if (selectedEdgeId.value) {
    return (
        selectedEdgeId.value !==
        edge.id
    );
  }

  if (selectedNodeId.value != null) {
    return (
        edge.sourceId !==
        selectedNodeId.value &&
        edge.targetId !==
        selectedNodeId.value
    );
  }

  const query =
      normalizedSearchTerm.value;

  if (!query) {
    return false;
  }

  return (
      !matchingEdgeIds.value.has(edge.id) &&
      !matchingNodeIds.value.has(
          edge.sourceId,
      ) &&
      !matchingNodeIds.value.has(
          edge.targetId,
      )
  );
}

function isNodeDimmed(
    node: GraphNode,
): boolean {
  const query =
      normalizedSearchTerm.value;

  if (
      query &&
      !matchingNodeIds.value.has(node.id)
  ) {
    const connectedMatchingEdge =
        edges.value.some(
            (edge) =>
                matchingEdgeIds.value.has(
                    edge.id,
                ) &&
                (
                    edge.sourceId === node.id ||
                    edge.targetId === node.id
                ),
        );

    if (!connectedMatchingEdge) {
      return true;
    }
  }

  const selectedGraphEdge =
      selectedEdge.value;

  if (selectedGraphEdge) {
    return (
        node.id !==
        selectedGraphEdge.sourceId &&
        node.id !==
        selectedGraphEdge.targetId
    );
  }

  const selectedId =
      selectedNodeId.value;

  if (selectedId == null) {
    return false;
  }

  if (node.id === selectedId) {
    return false;
  }

  return !selectedNeighbours.value.some(
      (neighbour) =>
          neighbour.id === node.id,
  );
}

function assignConnectedComponents(): void {
  const adjacency = new Map<
      number,
      Set<number>
  >();

  for (const node of nodes.value) {
    adjacency.set(
        node.id,
        new Set(),
    );
  }

  /*
   * Components are intentionally calculated as
   * weakly connected components. Direction does
   * not affect graph layout grouping.
   */
  for (const edge of edges.value) {
    adjacency
        .get(edge.sourceId)
        ?.add(edge.targetId);

    adjacency
        .get(edge.targetId)
        ?.add(edge.sourceId);
  }

  const visited = new Set<number>();
  let component = 0;

  for (const node of nodes.value) {
    if (visited.has(node.id)) {
      continue;
    }

    const queue = [node.id];
    visited.add(node.id);

    while (queue.length) {
      const currentId =
          queue.shift();

      if (currentId == null) {
        continue;
      }

      const currentNode =
          nodeMap.value.get(
              currentId,
          );

      if (currentNode) {
        currentNode.component =
            component;
      }

      for (
          const neighbourId of
      adjacency.get(currentId) ?? []
          ) {
        if (
            visited.has(neighbourId)
        ) {
          continue;
        }

        visited.add(neighbourId);
        queue.push(neighbourId);
      }
    }

    component += 1;
  }
}

function getWorldStorageId():
    string | null {
  const id = String(
      props.world.get("id") ?? "",
  ).trim();

  return id || null;
}

function graphLayoutStorageKey(
    worldId: string | null =
    loadedWorldStorageId.value,
): string | null {
  if (!worldId) {
    return null;
  }

  return (
      GRAPH_LAYOUT_STORAGE_PREFIX +
      worldId
  );
}

function clampNumber(
    value: number,
    minimum: number,
    maximum: number,
): number {
  return Math.min(
      Math.max(value, minimum),
      maximum,
  );
}

function isFiniteNumber(
    value: unknown,
): value is number {
  return (
      typeof value === "number" &&
      Number.isFinite(value)
  );
}

function clearScheduledGraphLayoutSave():
    void {
  if (graphLayoutSaveTimer === null) {
    return;
  }

  clearTimeout(
      graphLayoutSaveTimer,
  );

  graphLayoutSaveTimer = null;
}

function saveGraphLayout(): void {
  clearScheduledGraphLayoutSave();

  if (
      typeof localStorage ===
      "undefined" ||
      !nodes.value.length
  ) {
    return;
  }

  const storageKey =
      graphLayoutStorageKey();

  if (!storageKey) {
    return;
  }

  const savedNodes: Record<
      string,
      SavedNodePosition
  > = {};

  for (const node of nodes.value) {
    if (
        !Number.isFinite(node.x) ||
        !Number.isFinite(node.y)
    ) {
      continue;
    }

    savedNodes[String(node.id)] = {
      x: node.x,
      y: node.y,
    };
  }

  const layout: SavedGraphLayout = {
    version: 1,

    nodes: savedNodes,

    viewport: {
      zoom: zoom.value,
      panX: panX.value,
      panY: panY.value,
    },
  };

  try {
    localStorage.setItem(
        storageKey,
        JSON.stringify(layout),
    );
  } catch (error) {
    console.error(
        "Could not save graph layout",
        error,
    );
  }
}

function scheduleGraphLayoutSave(): void {
  clearScheduledGraphLayoutSave();

  graphLayoutSaveTimer =
      setTimeout(
          () => {
            graphLayoutSaveTimer = null;
            saveGraphLayout();
          },
          GRAPH_LAYOUT_SAVE_DELAY,
      );
}

function restoreGraphLayout(): boolean {
  if (
      typeof localStorage ===
      "undefined"
  ) {
    return false;
  }

  const storageKey =
      graphLayoutStorageKey();

  if (!storageKey) {
    return false;
  }

  let savedLayout: SavedGraphLayout;

  try {
    const source =
        localStorage.getItem(
            storageKey,
        );

    if (!source) {
      return false;
    }

    const parsed =
        JSON.parse(source) as
            Partial<SavedGraphLayout>;

    if (
        parsed.version !== 1 ||
        !parsed.nodes ||
        typeof parsed.nodes !==
        "object"
    ) {
      return false;
    }

    savedLayout =
        parsed as SavedGraphLayout;
  } catch (error) {
    console.error(
        "Could not read graph layout",
        error,
    );

    return false;
  }

  initializeNodePositions();

  let restoredNodeCount = 0;

  for (const node of nodes.value) {
    const savedNode =
        savedLayout.nodes[
            String(node.id)
            ];

    if (
        !savedNode ||
        !isFiniteNumber(savedNode.x) ||
        !isFiniteNumber(savedNode.y)
    ) {
      continue;
    }

    node.x = clampNumber(
        savedNode.x,
        45,
        VIEW_WIDTH - 45,
    );

    node.y = clampNumber(
        savedNode.y,
        45,
        VIEW_HEIGHT - 45,
    );

    node.velocityX = 0;
    node.velocityY = 0;

    restoredNodeCount += 1;
  }

  if (!restoredNodeCount) {
    return false;
  }

  const viewport =
      savedLayout.viewport;

  const validViewport =
      viewport &&
      isFiniteNumber(viewport.zoom) &&
      isFiniteNumber(viewport.panX) &&
      isFiniteNumber(viewport.panY);

  if (validViewport) {
    zoom.value = clampNumber(
        viewport.zoom,
        0.35,
        2.75,
    );

    panX.value = viewport.panX;
    panY.value = viewport.panY;
  } else {
    fitGraph(false);
  }

  return true;
}

function clearSavedGraphLayout(): void {
  clearScheduledGraphLayoutSave();

  const storageKey =
      graphLayoutStorageKey();

  if (
      storageKey &&
      typeof localStorage !==
      "undefined"
  ) {
    localStorage.removeItem(
        storageKey,
    );
  }

  runLayout(true);
}

function initializeNodePositions(): void {
  const total = nodes.value.length;

  if (!total) {
    return;
  }

  const radius = Math.min(
      VIEW_WIDTH,
      VIEW_HEIGHT,
  ) * 0.34;

  const centerX = VIEW_WIDTH / 2;
  const centerY = VIEW_HEIGHT / 2;

  const sortedNodes =
      [...nodes.value].sort(
          (first, second) =>
              first.id - second.id,
      );

  sortedNodes.forEach(
      (node, index) => {
        const angle =
            (
                Math.PI *
                2 *
                index
            ) / total -
            Math.PI / 2;

        node.x =
            centerX +
            Math.cos(angle) *
            radius;

        node.y =
            centerY +
            Math.sin(angle) *
            radius;

        node.velocityX = 0;
        node.velocityY = 0;
      },
  );
}

function componentAnchors(): Map<
    number,
    {
      x: number;
      y: number;
    }
> {
  const count = Math.max(
      componentCount.value,
      1,
  );

  const anchors = new Map<
      number,
      {
        x: number;
        y: number;
      }
  >();

  if (count === 1) {
    anchors.set(0, {
      x: VIEW_WIDTH / 2,
      y: VIEW_HEIGHT / 2,
    });

    return anchors;
  }

  const radius = Math.min(
      VIEW_WIDTH,
      VIEW_HEIGHT,
  ) * 0.28;

  for (
      let component = 0;
      component < count;
      component += 1
  ) {
    const angle =
        (
            Math.PI *
            2 *
            component
        ) / count -
        Math.PI / 2;

    anchors.set(component, {
      x:
          VIEW_WIDTH / 2 +
          Math.cos(angle) *
          radius,

      y:
          VIEW_HEIGHT / 2 +
          Math.sin(angle) *
          radius,
    });
  }

  return anchors;
}

function layoutSpringPairs(): Array<{
  sourceId: number;
  targetId: number;
}> {
  const pairs = new Map<
      string,
      {
        sourceId: number;
        targetId: number;
      }
  >();

  for (const edge of edges.value) {
    const sourceId = Math.min(
        edge.sourceId,
        edge.targetId,
    );

    const targetId = Math.max(
        edge.sourceId,
        edge.targetId,
    );

    const id =
        undirectedEdgeIdentifier(
            sourceId,
            targetId,
        );

    if (!pairs.has(id)) {
      pairs.set(id, {
        sourceId,
        targetId,
      });
    }
  }

  return [...pairs.values()];
}

function runLayout(
    resetPositions = true,
): void {
  stopLayout();

  if (!nodes.value.length) {
    return;
  }

  if (resetPositions) {
    initializeNodePositions();
  }

  layoutRunning.value = true;

  const anchors =
      componentAnchors();

  const springPairs =
      layoutSpringPairs();

  let iteration = 0;

  const maxIterations =
      nodes.value.length > 150
          ? 90
          : 180;

  function simulateStep(): void {
    const graphNodes =
        nodes.value;

    const repulsionStrength =
        graphNodes.length > 100
            ? 2400
            : 5200;

    const springLength = 135;
    const springStrength = 0.012;
    const anchorStrength = 0.004;
    const damping = 0.84;

    for (
        let firstIndex = 0;
        firstIndex <
        graphNodes.length;
        firstIndex += 1
    ) {
      const first =
          graphNodes[firstIndex];

      for (
          let secondIndex =
              firstIndex + 1;
          secondIndex <
          graphNodes.length;
          secondIndex += 1
      ) {
        const second =
            graphNodes[secondIndex];

        let deltaX =
            second.x - first.x;

        let deltaY =
            second.y - first.y;

        let distanceSquared =
            deltaX * deltaX +
            deltaY * deltaY;

        if (distanceSquared < 1) {
          distanceSquared = 1;
          deltaX = 1;
          deltaY = 0;
        }

        const distance =
            Math.sqrt(
                distanceSquared,
            );

        const force =
            repulsionStrength /
            distanceSquared;

        const forceX =
            (
                deltaX /
                distance
            ) * force;

        const forceY =
            (
                deltaY /
                distance
            ) * force;

        first.velocityX -= forceX;
        first.velocityY -= forceY;

        second.velocityX += forceX;
        second.velocityY += forceY;
      }
    }

    for (
        const pair of springPairs
        ) {
      const source =
          nodeMap.value.get(
              pair.sourceId,
          );

      const target =
          nodeMap.value.get(
              pair.targetId,
          );

      if (!source || !target) {
        continue;
      }

      const deltaX =
          target.x - source.x;

      const deltaY =
          target.y - source.y;

      const distance =
          Math.sqrt(
              deltaX * deltaX +
              deltaY * deltaY,
          ) || 1;

      const displacement =
          distance - springLength;

      const force =
          displacement *
          springStrength;

      const forceX =
          (
              deltaX /
              distance
          ) * force;

      const forceY =
          (
              deltaY /
              distance
          ) * force;

      source.velocityX += forceX;
      source.velocityY += forceY;

      target.velocityX -= forceX;
      target.velocityY -= forceY;
    }

    for (const node of graphNodes) {
      const anchor =
          anchors.get(
              node.component,
          ) ?? {
            x: VIEW_WIDTH / 2,
            y: VIEW_HEIGHT / 2,
          };

      node.velocityX +=
          (
              anchor.x -
              node.x
          ) * anchorStrength;

      node.velocityY +=
          (
              anchor.y -
              node.y
          ) * anchorStrength;

      node.velocityX *= damping;
      node.velocityY *= damping;

      node.x += node.velocityX;
      node.y += node.velocityY;

      node.x = Math.max(
          45,
          Math.min(
              VIEW_WIDTH - 45,
              node.x,
          ),
      );

      node.y = Math.max(
          45,
          Math.min(
              VIEW_HEIGHT - 45,
              node.y,
          ),
      );
    }
  }

  function frame(): void {
    for (
        let step = 0;
        step < 3;
        step += 1
    ) {
      simulateStep();
      iteration += 1;
    }

    if (
        iteration <
        maxIterations
    ) {
      layoutFrame =
          window.requestAnimationFrame(
              frame,
          );

      return;
    }

    layoutFrame = null;
    layoutRunning.value = false;

    void nextTick(() => {
      fitGraph(false);
      saveGraphLayout();
    });
  }

  layoutFrame =
      window.requestAnimationFrame(
          frame,
      );
}

function stopLayout(): void {
  if (layoutFrame !== null) {
    window.cancelAnimationFrame(
        layoutFrame,
    );

    layoutFrame = null;
  }

  layoutRunning.value = false;
}

function fitGraph(
    persist = true,
): void {
  if (!nodes.value.length) {
    zoom.value = 1;
    panX.value = 0;
    panY.value = 0;

    if (persist) {
      scheduleGraphLayoutSave();
    }

    return;
  }

  const padding = 90;

  const minimumX = Math.min(
      ...nodes.value.map(
          (node) => node.x,
      ),
  );

  const maximumX = Math.max(
      ...nodes.value.map(
          (node) => node.x,
      ),
  );

  const minimumY = Math.min(
      ...nodes.value.map(
          (node) => node.y,
      ),
  );

  const maximumY = Math.max(
      ...nodes.value.map(
          (node) => node.y,
      ),
  );

  const width = Math.max(
      maximumX - minimumX,
      1,
  );

  const height = Math.max(
      maximumY - minimumY,
      1,
  );

  const nextZoom = Math.min(
      (
          VIEW_WIDTH -
          padding * 2
      ) / width,

      (
          VIEW_HEIGHT -
          padding * 2
      ) / height,

      1.75,
  );

  zoom.value = Math.max(
      0.4,
      nextZoom,
  );

  panX.value =
      VIEW_WIDTH / 2 -
      (
          (
              minimumX +
              maximumX
          ) / 2
      ) * zoom.value;

  panY.value =
      VIEW_HEIGHT / 2 -
      (
          (
              minimumY +
              maximumY
          ) / 2
      ) * zoom.value;

  if (persist) {
    scheduleGraphLayoutSave();
  }
}

async function loadGraph(): Promise<void> {
  const requestId = ++loadRequestId;

  saveGraphLayout();
  stopLayout();

  loading.value = true;
  loadError.value = null;

  selectedNodeId.value = null;
  selectedEdgeId.value = null;

  externalEdgeCount.value = 0;
  invalidLocationCount.value = 0;
  invalidEdgeCount.value = 0;
  selfLoopCount.value = 0;

  try {
    const locations =
        await props.world.getLocations();

    if (requestId !== loadRequestId) {
      return;
    }

    const locationMap = new Map<
        number,
        Location
    >();

    for (const location of locations) {
      const id =
          getLocationId(location);

      if (id == null) {
        invalidLocationCount.value += 1;
        continue;
      }

      locationMap.set(id, location);
    }

    const graphNodes: GraphNode[] =
        [...locationMap.entries()]
            .map(
                ([id, location]) => ({
                  id,
                  location,

                  name:
                      getLocationName(
                          location,
                      ),

                  x: VIEW_WIDTH / 2,
                  y: VIEW_HEIGHT / 2,

                  velocityX: 0,
                  velocityY: 0,

                  inDegree: 0,
                  outDegree: 0,
                  degree: 0,

                  component: 0,
                }),
            )
            .sort(
                (first, second) =>
                    first.name.localeCompare(
                        second.name,
                    ),
            );

    /*
     * Location.getOutEdges() returns the actual
     * directional LocationEdge entities, unlike
     * getNeighbours(), which loses direction and
     * edge metadata.
     */
    const edgeResults =
        await Promise.allSettled(
            graphNodes.map(
                async (node) => ({
                  ownerId: node.id,
                  edges:
                      await node.location
                          .getOutEdges(),
                }),
            ),
        );

    if (requestId !== loadRequestId) {
      return;
    }

    const graphEdgeMap = new Map<
        string,
        GraphEdge
    >();

    let outsideWorld = 0;
    let invalidEdges = 0;
    let selfLoops = 0;

    for (const result of edgeResults) {
      if (
          result.status ===
          "rejected"
      ) {
        console.error(
            "Could not retrieve outgoing location edges",
            result.reason,
        );

        continue;
      }

      for (
          const edge of
          result.value.edges
          ) {
        const sourceId = Number(
            edge.get("from_id"),
        );

        const targetId = Number(
            edge.get("to_id"),
        );

        if (
            !Number.isFinite(sourceId) ||
            !Number.isFinite(targetId)
        ) {
          invalidEdges += 1;
          continue;
        }

        if (sourceId === targetId) {
          selfLoops += 1;
          continue;
        }

        if (
            !locationMap.has(sourceId) ||
            !locationMap.has(targetId)
        ) {
          outsideWorld += 1;
          continue;
        }

        const id =
            edgeIdentifier(
                sourceId,
                targetId,
            );

        graphEdgeMap.set(id, {
          id,

          sourceId,
          targetId,

          entity: edge,

          description: String(
              edge.get(
                  "edge_description",
              ) ?? "",
          ),

          showDestinationName:
              edge.get(
                  "show_destination_name",
              ) ?? false,

          showDestinationDescription:
              edge.get(
                  "show_destination_description",
              ) ?? false,

          traversable:
              edge.get(
                  "is_traversable",
              ) ?? false,

          reciprocal: false,
        });
      }
    }

    for (
        const edge of
        graphEdgeMap.values()
        ) {
      edge.reciprocal =
          graphEdgeMap.has(
              edgeIdentifier(
                  edge.targetId,
                  edge.sourceId,
              ),
          );
    }

    const graphEdges = [
      ...graphEdgeMap.values(),
    ].sort(
        (first, second) =>
            first.id.localeCompare(
                second.id,
            ),
    );

    const inDegreeMap =
        new Map<number, number>();

    const outDegreeMap =
        new Map<number, number>();

    for (const node of graphNodes) {
      inDegreeMap.set(node.id, 0);
      outDegreeMap.set(node.id, 0);
    }

    for (const edge of graphEdges) {
      outDegreeMap.set(
          edge.sourceId,
          (
              outDegreeMap.get(
                  edge.sourceId,
              ) ?? 0
          ) + 1,
      );

      inDegreeMap.set(
          edge.targetId,
          (
              inDegreeMap.get(
                  edge.targetId,
              ) ?? 0
          ) + 1,
      );
    }

    for (const node of graphNodes) {
      node.inDegree =
          inDegreeMap.get(node.id) ?? 0;

      node.outDegree =
          outDegreeMap.get(node.id) ?? 0;

      node.degree =
          node.inDegree +
          node.outDegree;
    }

    nodes.value = graphNodes;
    edges.value = graphEdges;

    externalEdgeCount.value =
        outsideWorld;

    invalidEdgeCount.value =
        invalidEdges;

    selfLoopCount.value =
        selfLoops;

    assignConnectedComponents();

    loadedWorldStorageId.value =
        getWorldStorageId();

    await nextTick();

    const restored =
        restoreGraphLayout();

    if (!restored) {
      runLayout(true);
    }
  } catch (error) {
    console.error(
        "Could not construct directed location graph",
        error,
    );

    nodes.value = [];
    edges.value = [];

    loadError.value =
        "The directed location graph could not be constructed.";
  } finally {
    if (requestId === loadRequestId) {
      loading.value = false;
    }
  }
}

function selectNode(
    nodeId: number,
): void {
  selectedEdgeId.value = null;

  selectedNodeId.value =
      selectedNodeId.value === nodeId
          ? null
          : nodeId;
}

function selectEdge(
    edgeId: string,
): void {
  selectedNodeId.value = null;

  selectedEdgeId.value =
      selectedEdgeId.value === edgeId
          ? null
          : edgeId;
}

function clearSelection(): void {
  selectedNodeId.value = null;
  selectedEdgeId.value = null;
}

function onWindowKeyDown(
    event: KeyboardEvent,
): void {
  if (event.key !== "Escape") {
    return;
  }

  clearSelection();
}

function selectAndCenterNode(
    node: GraphNode,
): void {
  selectedEdgeId.value = null;
  selectedNodeId.value = node.id;

  zoom.value = Math.max(
      zoom.value,
      1.15,
  );

  panX.value =
      VIEW_WIDTH / 2 -
      node.x * zoom.value;

  panY.value =
      VIEW_HEIGHT / 2 -
      node.y * zoom.value;

  scheduleGraphLayoutSave();
}

function selectConnection(
    connection: NodeConnection,
): void {
  selectedNodeId.value = null;

  selectedEdgeId.value =
      connection.edge.id;
}

function svgCoordinates(
    event:
        | PointerEvent
        | WheelEvent,
): {
  x: number;
  y: number;
} {
  const svg = graphSvg.value;

  if (!svg) {
    return {
      x: 0,
      y: 0,
    };
  }

  const screenMatrix =
      svg.getScreenCTM();

  if (!screenMatrix) {
    return {
      x: 0,
      y: 0,
    };
  }

  const screenPoint =
      svg.createSVGPoint();

  screenPoint.x = event.clientX;
  screenPoint.y = event.clientY;

  const svgPoint =
      screenPoint.matrixTransform(
          screenMatrix.inverse(),
      );

  return {
    x: svgPoint.x,
    y: svgPoint.y,
  };
}

function graphCoordinates(
    event: PointerEvent,
): {
  x: number;
  y: number;
} {
  const point =
      svgCoordinates(event);

  return {
    x:
        (
            point.x -
            panX.value
        ) / zoom.value,

    y:
        (
            point.y -
            panY.value
        ) / zoom.value,
  };
}

function onNodePointerDown(
    event: PointerEvent,
    node: GraphNode,
): void {
  stopLayout();

  draggingNodeId.value = node.id;

  (
      event.currentTarget as
          SVGElement
  ).setPointerCapture(
      event.pointerId,
  );

  event.stopPropagation();
}

function onGraphPointerDown(
    event: PointerEvent,
): void {
  const point =
      svgCoordinates(event);

  panning.value = true;

  pointerStartX.value = point.x;
  pointerStartY.value = point.y;

  panStartX.value = panX.value;
  panStartY.value = panY.value;

  graphSvg.value?.setPointerCapture(
      event.pointerId,
  );
}

function onGraphPointerMove(
    event: PointerEvent,
): void {
  if (
      draggingNodeId.value !== null
  ) {
    const node =
        nodeMap.value.get(
            draggingNodeId.value,
        );

    if (!node) {
      return;
    }

    const point =
        graphCoordinates(event);

    node.x = point.x;
    node.y = point.y;

    node.velocityX = 0;
    node.velocityY = 0;

    return;
  }

  if (!panning.value) {
    return;
  }

  const point =
      svgCoordinates(event);

  panX.value =
      panStartX.value +
      point.x -
      pointerStartX.value;

  panY.value =
      panStartY.value +
      point.y -
      pointerStartY.value;
}

function onGraphPointerUp(): void {
  const graphChanged =
      draggingNodeId.value !== null ||
      panning.value;

  draggingNodeId.value = null;
  panning.value = false;

  if (graphChanged) {
    scheduleGraphLayoutSave();
  }
}

function onWheel(
    event: WheelEvent,
): void {
  event.preventDefault();

  const cursor =
      svgCoordinates(event);

  const previousZoom =
      zoom.value;

  const nextZoom = Math.max(
      0.35,
      Math.min(
          2.75,
          previousZoom *
          (
              event.deltaY < 0
                  ? 1.12
                  : 0.89
          ),
      ),
  );

  const graphX =
      (
          cursor.x -
          panX.value
      ) / previousZoom;

  const graphY =
      (
          cursor.y -
          panY.value
      ) / previousZoom;

  zoom.value = nextZoom;

  panX.value =
      cursor.x -
      graphX * nextZoom;

  panY.value =
      cursor.y -
      graphY * nextZoom;

  scheduleGraphLayoutSave();
}

onMounted(() => {
  window.addEventListener(
      "keydown",
      onWindowKeyDown,
  );

  void loadGraph();
});

watch(
    () => props.world.get("id"),
    () => {
      void loadGraph();
    },
);

onBeforeUnmount(() => {
  window.removeEventListener(
      "keydown",
      onWindowKeyDown,
  );

  stopLayout();
  saveGraphLayout();
});
</script>

<template>
  <article
      class="
      world-location-graph
      edit-box
      edit-box--info
    "
      :aria-busy="loading"
  >
    <header class="edit-box__header">
      <div
          class="edit-box__header-icon"
          aria-hidden="true"
      >
        <svg viewBox="0 0 24 24">
          <circle
              cx="5"
              cy="6"
              r="2"
          />

          <circle
              cx="19"
              cy="7"
              r="2"
          />

          <circle
              cx="8"
              cy="18"
              r="2"
          />

          <circle
              cx="18"
              cy="17"
              r="2"
          />

          <path d="M7 7h9"/>
          <path d="m14 4 3 3-3 3"/>

          <path d="m6 8 2 7"/>
          <path d="m5 13 3 3 2-4"/>

          <path d="M10 17h5"/>
          <path d="m13 14 3 3-3 3"/>
        </svg>
      </div>

      <div class="edit-box__header-main">
        <span class="edit-box__eyebrow">
          Directed graph viewer
        </span>

        <div class="edit-box__title-row">
          <h2 class="edit-box__title">
            Location graph
          </h2>

          <span class="edit-box__badge">
            Read only
          </span>

          <span
              class="
              edit-box__badge
              edit-box__badge--success
            "
          >
            Directed edges
          </span>
        </div>

        <p class="edit-box__description">
          Visualize directional location edges in
          {{ worldName }}. Arrowheads indicate
          travel direction, curved pairs indicate
          reciprocal edges, and dashed red edges
          are not traversable.
        </p>
      </div>

      <div class="edit-box__actions">
        <button
            type="button"
            class="edit-box__action"
            :disabled="
            loading ||
            layoutRunning
          "
            @click="runLayout(true)"
        >
          <svg
              viewBox="0 0 24 24"
              aria-hidden="true"
          >
            <path d="M4 7h10"/>
            <path d="m11 4 3 3-3 3"/>

            <path d="M20 17H10"/>
            <path d="m13 14-3 3 3 3"/>
          </svg>

          {{
            layoutRunning
                ? "Arranging..."
                : "Re-layout"
          }}
        </button>

        <button
            type="button"
            class="edit-box__action"
            :disabled="
            loading ||
            !nodes.length
          "
            @click="fitGraph"
        >
          Fit
        </button>

        <button
            type="button"
            class="edit-box__action"
            :disabled="
            loading ||
            layoutRunning
          "
            @click="loadGraph"
        >
          Refresh
        </button>

        <button
            type="button"
            class="edit-box__action"
            :disabled="
            loading ||
            layoutRunning ||
            !nodes.length
          "
            @click="
            clearSavedGraphLayout
          "
        >
          Reset positions
        </button>
      </div>
    </header>

    <div
        v-if="loadError"
        class="edit-box__body"
    >
      <div
          class="
          edit-box__state
          edit-box__state--error
          edit-box__state--vertical
          world-location-graph__state
        "
          role="alert"
      >
        <div class="edit-box__state-icon">
          <svg
              viewBox="0 0 24 24"
              aria-hidden="true"
          >
            <path d="M12 9v4"/>
            <path d="M12 17h.01"/>

            <path
                d="M10.3 3.8 2.2 18a2 2 0 0 0 1.8 3h16a2 2 0 0 0 1.8-3L13.7 3.8a2 2 0 0 0-3.4 0Z"
            />
          </svg>
        </div>

        <div class="edit-box__state-content">
          <strong class="edit-box__state-title">
            Could not construct graph
          </strong>

          <p class="edit-box__state-description">
            {{ loadError }}
          </p>
        </div>

        <button
            type="button"
            class="
            edit-box__action
            edit-box__action--accent
          "
            @click="loadGraph"
        >
          Retry
        </button>
      </div>
    </div>

    <div
        v-else
        class="
        edit-box__body
        world-location-graph__body
      "
    >
      <div class="edit-box__toolbar">
        <div class="edit-box__toolbar-main">
          <SearchBar
              v-model:search="searchTerm"
              placeholder="Find location, edge description, or ID"
              aria-label="Find location, edge description, or ID"
          />
        </div>

        <div class="edit-box__toolbar-actions">
          <button
              type="button"
              class="edit-box__action"
              :aria-pressed="
              showNodeLabels
            "
              @click="
              showNodeLabels =
                !showNodeLabels
            "
          >
            {{
              showNodeLabels
                  ? "Hide node labels"
                  : "Show node labels"
            }}
          </button>

          <button
              type="button"
              class="edit-box__action"
              :aria-pressed="
              showEdgeLabels
            "
              @click="
              showEdgeLabels =
                !showEdgeLabels
            "
          >
            {{
              showEdgeLabels
                  ? "Hide edge labels"
                  : "Show edge labels"
            }}
          </button>

          <span class="edit-box__count">
            {{ nodes.length }} nodes
          </span>

          <span class="edit-box__count">
            {{ edges.length }}
            directed edges
          </span>
        </div>
      </div>

      <div
          class="
          world-location-graph__statistics
        "
          aria-label="Directed graph statistics"
      >
        <span
            class="
            edit-box__badge
            edit-box__badge--success
          "
        >
          {{ traversableEdgeCount }}
          traversable
        </span>

        <span
            v-if="blockedEdgeCount > 0"
            class="
            edit-box__badge
            edit-box__badge--danger
          "
        >
          {{ blockedEdgeCount }}
          blocked
        </span>

        <span class="edit-box__badge">
          {{ reciprocalPairCount }}
          reciprocal pairs
        </span>
      </div>

      <div
          v-if="hasDiagnostics"
          class="
          world-location-graph__diagnostics
        "
          aria-label="Graph diagnostics"
      >
        <span
            v-if="componentCount > 1"
            class="
            edit-box__badge
            edit-box__badge--warning
          "
        >
          {{ componentCount }}
          disconnected components
        </span>

        <span
            v-if="isolatedNodeCount > 0"
            class="
            edit-box__badge
            edit-box__badge--warning
          "
        >
          {{ isolatedNodeCount }}
          isolated
        </span>

        <span
            v-if="externalEdgeCount > 0"
            class="
            edit-box__badge
            edit-box__badge--danger
          "
            title="Edges referenced locations not returned by this world."
        >
          {{ externalEdgeCount }}
          external edges
        </span>

        <span
            v-if="invalidLocationCount > 0"
            class="
            edit-box__badge
            edit-box__badge--danger
          "
        >
          {{ invalidLocationCount }}
          invalid location IDs
        </span>

        <span
            v-if="invalidEdgeCount > 0"
            class="
            edit-box__badge
            edit-box__badge--danger
          "
        >
          {{ invalidEdgeCount }}
          invalid edges
        </span>

        <span
            v-if="selfLoopCount > 0"
            class="
            edit-box__badge
            edit-box__badge--warning
          "
            title="Self-referencing edges are currently omitted from the visual graph."
        >
          {{ selfLoopCount }}
          self loops omitted
        </span>
      </div>

      <div
          v-if="loading"
          class="
          edit-box__state
          world-location-graph__state
        "
          role="status"
          aria-live="polite"
      >
        <span
            class="edit-box__spinner"
            aria-hidden="true"
        />

        <div class="edit-box__state-content">
          <strong class="edit-box__state-title">
            Discovering directed edges
          </strong>

          <p class="edit-box__state-description">
            Retrieving locations, outgoing edge
            entities, and edge metadata.
          </p>
        </div>
      </div>

      <SplitPanel
          v-else-if="nodes.length"
          class="
          world-location-graph__split-panel
        "
          storage-key="WorldDirectedLocationGraph"
      >
        <template #left>
          <div
              class="
              world-location-graph__viewport
            "
          >
            <svg
                ref="graphSvg"
                class="
                world-location-graph__svg
              "
                :viewBox="
                `0 0 ${VIEW_WIDTH} ${VIEW_HEIGHT}`
              "
                role="img"
                :aria-label="
                `Directed location graph for ${worldName}`
              "
                @pointerdown="
                onGraphPointerDown
              "
                @pointermove="
                onGraphPointerMove
              "
                @pointerup="
                onGraphPointerUp
              "
                @pointercancel="
                onGraphPointerUp
              "
                @pointerleave="
                onGraphPointerUp
              "
                @wheel="onWheel"
            >
              <defs>
                <pattern
                    :id="
                    `location-graph-grid-${graphIdentifier}`
                  "
                    width="32"
                    height="32"
                    patternUnits="userSpaceOnUse"
                >
                  <path
                      d="M32 0H0V32"
                      class="
                      world-location-graph__grid-line
                    "
                  />
                </pattern>

                <marker
                    :id="normalArrowMarkerId"
                    viewBox="0 0 10 10"
                    refX="9"
                    refY="5"
                    markerWidth="7"
                    markerHeight="7"
                    orient="auto"
                    markerUnits="strokeWidth"
                >
                  <path
                      d="M0 0 10 5 0 10Z"
                      class="
                      world-location-graph__arrow
                    "
                  />
                </marker>

                <marker
                    :id="selectedArrowMarkerId"
                    viewBox="0 0 10 10"
                    refX="9"
                    refY="5"
                    markerWidth="7"
                    markerHeight="7"
                    orient="auto"
                    markerUnits="strokeWidth"
                >
                  <path
                      d="M0 0 10 5 0 10Z"
                      class="
                      world-location-graph__arrow
                      world-location-graph__arrow--selected
                    "
                  />
                </marker>

                <marker
                    :id="blockedArrowMarkerId"
                    viewBox="0 0 10 10"
                    refX="9"
                    refY="5"
                    markerWidth="7"
                    markerHeight="7"
                    orient="auto"
                    markerUnits="strokeWidth"
                >
                  <path
                      d="M0 0 10 5 0 10Z"
                      class="
                      world-location-graph__arrow
                      world-location-graph__arrow--blocked
                    "
                  />
                </marker>
              </defs>

              <rect
                  width="100%"
                  height="100%"
                  :fill="
                  `url(#location-graph-grid-${graphIdentifier})`
                "
              />

              <g :transform="graphTransform">
                <g
                    class="
                    world-location-graph__edges
                  "
                >
                  <g
                      v-for="
                      edge in renderedEdges
                    "
                      :key="edge.id"
                      class="
                      world-location-graph__edge-group
                    "
                      :class="{
                      'world-location-graph__edge-group--dimmed':
                        isEdgeDimmed(edge),
                    }"
                      role="button"
                      tabindex="0"
                      :aria-label="
                      `${edge.source.name} to ${edge.target.name}. ${edge.traversable ? 'Traversable' : 'Not traversable'}.`
                    "
                      @pointerdown.stop
                      @click.stop="
                      selectEdge(edge.id)
                    "
                      @keydown.enter.prevent="
                      selectEdge(edge.id)
                    "
                      @keydown.space.prevent="
                      selectEdge(edge.id)
                    "
                  >
                    <path
                        :d="edge.path"
                        class="
                        world-location-graph__edge-hit
                      "
                        vector-effect="
                        non-scaling-stroke
                      "
                    />

                    <path
                        :d="edge.path"
                        class="
                        world-location-graph__edge
                      "
                        :class="{
                        'world-location-graph__edge--selected':
                          selectedEdgeId ===
                          edge.id,

                        'world-location-graph__edge--related':
                          isSelectedOrRelatedEdge(
                            edge,
                          ),

                        'world-location-graph__edge--blocked':
                          !edge.traversable,

                        'world-location-graph__edge--reciprocal':
                          edge.reciprocal,
                      }"
                        :marker-end="
                        edgeMarker(edge)
                      "
                        vector-effect="
                        non-scaling-stroke
                      "
                    />

                    <g
                        v-if="
                        showEdgeLabels ||
                        selectedEdgeId ===
                        edge.id
                      "
                        class="
                        world-location-graph__edge-label
                      "
                        :class="{
                        'world-location-graph__edge-label--selected':
                          selectedEdgeId ===
                          edge.id,

                        'world-location-graph__edge-label--blocked':
                          !edge.traversable,
                      }"
                        :transform="
                        `translate(${edge.labelX} ${edge.labelY})`
                      "
                        pointer-events="none"
                    >
                      <rect
                          :x="
                          -edgeLabelWidth(edge) /
                          2
                        "
                          y="-11"
                          :width="
                          edgeLabelWidth(edge)
                        "
                          height="22"
                          rx="11"
                      />

                      <text
                          text-anchor="middle"
                          dominant-baseline="central"
                      >
                        {{ edgeLabel(edge) }}
                      </text>
                    </g>
                  </g>
                </g>

                <g
                    v-for="node in nodes"
                    :key="node.id"
                    class="
                    world-location-graph__node
                  "
                    :class="{
                    'world-location-graph__node--selected':
                      selectedNodeId ===
                      node.id,

                    'world-location-graph__node--edge-endpoint':
                      selectedEdge &&
                      (
                        selectedEdge.sourceId ===
                        node.id ||
                        selectedEdge.targetId ===
                        node.id
                      ),

                    'world-location-graph__node--matching':
                      matchingNodeIds.has(
                        node.id,
                      ),

                    'world-location-graph__node--dimmed':
                      isNodeDimmed(node),

                    'world-location-graph__node--isolated':
                      node.degree === 0,
                  }"
                    :transform="
                    `translate(${node.x} ${node.y})`
                  "
                    role="button"
                    tabindex="0"
                    :aria-label="
                    `${node.name}, ${node.outDegree} outgoing and ${node.inDegree} incoming edges`
                  "
                    @pointerdown="
                    event =>
                      onNodePointerDown(
                        event,
                        node,
                      )
                  "
                    @click.stop="
                    selectNode(node.id)
                  "
                    @keydown.enter.prevent="
                    selectNode(node.id)
                  "
                    @keydown.space.prevent="
                    selectNode(node.id)
                  "
                >
                  <circle
                      class="
                      world-location-graph__node-halo
                    "
                      :r="
                      nodeRadius(node) + 7
                    "
                  />

                  <circle
                      class="
                      world-location-graph__node-circle
                    "
                      :r="nodeRadius(node)"
                  />

                  <text
                      class="
                      world-location-graph__node-degree
                    "
                      text-anchor="middle"
                      dominant-baseline="central"
                  >
                    {{ node.degree }}
                  </text>

                  <text
                      v-if="showNodeLabels"
                      class="
                      world-location-graph__node-label
                    "
                      text-anchor="middle"
                      :y="
                      nodeRadius(node) + 18
                    "
                  >
                    {{
                      node.name.length > 24
                          ? `${node.name.slice(
                              0,
                              22,
                          )}…`
                          : node.name
                    }}
                  </text>
                </g>
              </g>
            </svg>

            <div
                class="
                world-location-graph__legend
              "
                aria-label="Graph legend"
            >
              <span
                  class="
                  world-location-graph__legend-item
                "
              >
                <span
                    class="
                    world-location-graph__legend-line
                  "
                />

                Traversable
              </span>

              <span
                  class="
                  world-location-graph__legend-item
                "
              >
                <span
                    class="
                    world-location-graph__legend-line
                    world-location-graph__legend-line--blocked
                  "
                />

                Blocked
              </span>

              <span
                  class="
                  world-location-graph__legend-item
                "
              >
                <span
                    class="
                    world-location-graph__legend-curve
                  "
                />

                Reciprocal pair
              </span>
            </div>

            <div
                class="
                world-location-graph__viewport-help
              "
            >
              Select arrows for edge info · Drag
              nodes · Drag background to pan ·
              Scroll to zoom
            </div>
          </div>
        </template>

        <template #right>
          <aside
              class="
              world-location-graph__inspector
            "
          >
            <template
                v-if="
                selectedEdge &&
                selectedEdgeSource &&
                selectedEdgeTarget
              "
            >
              <header
                  class="
                  world-location-graph__inspector-header
                "
              >
                <span class="edit-box__eyebrow">
                  Selected directed edge
                </span>

                <h3
                    class="
                    world-location-graph__inspector-title
                    world-location-graph__edge-title
                  "
                >
                  <button
                      type="button"
                      @click="
                      selectAndCenterNode(
                        selectedEdgeSource,
                      )
                    "
                  >
                    {{
                      selectedEdgeSource.name
                    }}
                  </button>

                  <span aria-hidden="true">
                    →
                  </span>

                  <button
                      type="button"
                      @click="
                      selectAndCenterNode(
                        selectedEdgeTarget,
                      )
                    "
                  >
                    {{
                      selectedEdgeTarget.name
                    }}
                  </button>
                </h3>

                <span
                    class="
                    world-location-graph__identifier
                  "
                >
                  {{ selectedEdge.id }}
                </span>
              </header>

              <div
                  class="
                  world-location-graph__edge-status
                "
              >
                <span
                    class="edit-box__badge"
                    :class="
                    selectedEdge.traversable
                      ? 'edit-box__badge--success'
                      : 'edit-box__badge--danger'
                  "
                >
                  {{
                    selectedEdge.traversable
                        ? "Traversable"
                        : "Blocked"
                  }}
                </span>

                <span
                    v-if="
                    selectedEdge.reciprocal
                  "
                    class="
                    edit-box__badge
                    edit-box__badge--success
                  "
                >
                  Reciprocal
                </span>

                <span
                    v-else
                    class="
                    edit-box__badge
                    edit-box__badge--neutral
                  "
                >
                  One way
                </span>
              </div>

              <dl
                  class="
                  world-location-graph__details
                "
              >
                <div
                    class="
                    world-location-graph__detail
                  "
                >
                  <dt>Source ID</dt>

                  <dd>
                    {{
                      selectedEdge.sourceId
                    }}
                  </dd>
                </div>

                <div
                    class="
                    world-location-graph__detail
                  "
                >
                  <dt>Destination ID</dt>

                  <dd>
                    {{
                      selectedEdge.targetId
                    }}
                  </dd>
                </div>

                <div
                    class="
                    world-location-graph__detail
                  "
                >
                  <dt>Direction</dt>

                  <dd>
                    Outgoing
                  </dd>
                </div>

                <div
                    class="
                    world-location-graph__detail
                  "
                >
                  <dt>Reverse edge</dt>

                  <dd>
                    {{
                      selectedReverseEdge
                          ? "Present"
                          : "Absent"
                    }}
                  </dd>
                </div>
              </dl>

              <section
                  class="
                  edit-box__section
                  world-location-graph__edge-description
                "
              >
                <header
                    class="
                    edit-box__section-header
                  "
                >
                  <div
                      class="
                      edit-box__section-heading
                    "
                  >
                    <h4
                        class="
                        edit-box__section-title
                      "
                    >
                      Edge description
                    </h4>

                    <p
                        class="
                        edit-box__section-description
                      "
                    >
                      Direction-specific context
                      associated with this edge.
                    </p>
                  </div>
                </header>

                <p
                    v-if="
                    selectedEdge.description.trim()
                  "
                    class="
                    world-location-graph__description-text
                  "
                >
                  {{
                    selectedEdge.description
                  }}
                </p>

                <div
                    v-else
                    class="
                    edit-box__state
                    world-location-graph__empty-description
                  "
                >
                  No edge description.
                </div>
              </section>

              <section
                  class="
                  edit-box__section
                  world-location-graph__edge-flags
                "
              >
                <header
                    class="
                    edit-box__section-header
                  "
                >
                  <div
                      class="
                      edit-box__section-heading
                    "
                  >
                    <h4
                        class="
                        edit-box__section-title
                      "
                    >
                      Edge flags
                    </h4>

                    <p
                        class="
                        edit-box__section-description
                      "
                    >
                      Metadata applied when this
                      directional edge is used.
                    </p>
                  </div>
                </header>

                <dl
                    class="
                    world-location-graph__flag-list
                  "
                >
                  <div
                      class="
                      world-location-graph__flag
                    "
                  >
                    <dt>
                      Traversable
                    </dt>

                    <dd
                        :class="{
                        'world-location-graph__flag-value--enabled':
                          selectedEdge.traversable,

                        'world-location-graph__flag-value--disabled':
                          !selectedEdge.traversable,
                      }"
                    >
                      {{
                        booleanLabel(
                            selectedEdge.traversable,
                        )
                      }}
                    </dd>
                  </div>

                  <div
                      class="
                      world-location-graph__flag
                    "
                  >
                    <dt>
                      Show destination name
                    </dt>

                    <dd
                        :class="{
                        'world-location-graph__flag-value--enabled':
                          selectedEdge.showDestinationName,

                        'world-location-graph__flag-value--disabled':
                          !selectedEdge.showDestinationName,
                      }"
                    >
                      {{
                        booleanLabel(
                            selectedEdge.showDestinationName,
                        )
                      }}
                    </dd>
                  </div>

                  <div
                      class="
                      world-location-graph__flag
                    "
                  >
                    <dt>
                      Show destination description
                    </dt>

                    <dd
                        :class="{
                        'world-location-graph__flag-value--enabled':
                          selectedEdge.showDestinationDescription,

                        'world-location-graph__flag-value--disabled':
                          !selectedEdge.showDestinationDescription,
                      }"
                    >
                      {{
                        booleanLabel(
                            selectedEdge.showDestinationDescription,
                        )
                      }}
                    </dd>
                  </div>
                </dl>
              </section>

              <section
                  v-if="
                  selectedReverseEdge
                "
                  class="
                  edit-box__section
                  edit-box__section--accent
                  world-location-graph__reverse-edge
                "
              >
                <header
                    class="
                    edit-box__section-header
                  "
                >
                  <div
                      class="
                      edit-box__section-heading
                    "
                  >
                    <h4
                        class="
                        edit-box__section-title
                      "
                    >
                      Reverse edge
                    </h4>

                    <p
                        class="
                        edit-box__section-description
                      "
                    >
                      A separate edge exists in the
                      opposite direction.
                    </p>
                  </div>
                </header>

                <button
                    type="button"
                    class="
                    world-location-graph__reverse-button
                  "
                    @click="
                    selectEdge(
                      selectedReverseEdge.id,
                    )
                  "
                >
                  <span>
                    {{
                      selectedEdgeTarget.name
                    }}
                  </span>

                  <span aria-hidden="true">
                    →
                  </span>

                  <span>
                    {{
                      selectedEdgeSource.name
                    }}
                  </span>
                </button>
              </section>
            </template>

            <template v-else-if="selectedNode">
              <header
                  class="
                  world-location-graph__inspector-header
                "
              >
                <span class="edit-box__eyebrow">
                  Selected location
                </span>

                <h3
                    class="
                    world-location-graph__inspector-title
                  "
                >
                  {{ selectedNode.name }}
                </h3>

                <span
                    class="
                    world-location-graph__identifier
                  "
                >
                  ID {{ selectedNode.id }}
                </span>
              </header>

              <dl
                  class="
                  world-location-graph__details
                "
              >
                <div
                    class="
                    world-location-graph__detail
                  "
                >
                  <dt>Total edges</dt>

                  <dd>
                    {{ selectedNode.degree }}
                  </dd>
                </div>

                <div
                    class="
                    world-location-graph__detail
                    world-location-graph__detail--outgoing
                  "
                >
                  <dt>Outgoing</dt>

                  <dd>
                    {{
                      selectedNode.outDegree
                    }}
                  </dd>
                </div>

                <div
                    class="
                    world-location-graph__detail
                    world-location-graph__detail--incoming
                  "
                >
                  <dt>Incoming</dt>

                  <dd>
                    {{
                      selectedNode.inDegree
                    }}
                  </dd>
                </div>

                <div
                    class="
                    world-location-graph__detail
                  "
                >
                  <dt>Component</dt>

                  <dd>
                    {{
                      selectedNode.component +
                      1
                    }}
                    of {{ componentCount }}
                  </dd>
                </div>
              </dl>

              <section
                  class="
                  edit-box__section
                  world-location-graph__connections
                "
              >
                <header
                    class="
                    edit-box__section-header
                  "
                >
                  <div
                      class="
                      edit-box__section-heading
                    "
                  >
                    <h4
                        class="
                        edit-box__section-title
                      "
                    >
                      Directional connections
                    </h4>

                    <p
                        class="
                        edit-box__section-description
                      "
                    >
                      Select an edge to inspect its
                      description and flags.
                    </p>
                  </div>

                  <span class="edit-box__count">
                    {{
                      selectedConnections.length
                    }}
                  </span>
                </header>

                <div
                    v-if="
                    selectedConnections.length
                  "
                    class="
                    world-location-graph__connection-list
                  "
                >
                  <button
                      v-for="
                      connection in
                      selectedConnections
                    "
                      :key="
                      connection.edge.id
                    "
                      type="button"
                      class="
                      world-location-graph__connection
                    "
                      :class="{
                      'world-location-graph__connection--outgoing':
                        connection.direction ===
                        'outgoing',

                      'world-location-graph__connection--incoming':
                        connection.direction ===
                        'incoming',

                      'world-location-graph__connection--blocked':
                        !connection.edge.traversable,
                    }"
                      @click="
                      selectConnection(
                        connection,
                      )
                    "
                  >
                    <span
                        class="
                        world-location-graph__connection-direction
                      "
                        aria-hidden="true"
                    >
                      {{
                        connection.direction ===
                        "outgoing"
                            ? "→"
                            : "←"
                      }}
                    </span>

                    <span
                        class="
                        world-location-graph__connection-main
                      "
                    >
                      <strong>
                        {{
                          connection.other.name
                        }}
                      </strong>

                      <span>
                        {{
                          connection.edge.description.trim()
                              ? connection.edge.description
                              : connection.edge.traversable
                                  ? "Traversable edge"
                                  : "Blocked edge"
                        }}
                      </span>
                    </span>

                    <span
                        class="
                        world-location-graph__connection-status
                      "
                    >
                      {{
                        connection.direction ===
                        "outgoing"
                            ? "Out"
                            : "In"
                      }}
                    </span>
                  </button>
                </div>

                <div
                    v-else
                    class="
                    edit-box__state
                    edit-box__state--warning
                    world-location-graph__empty-connections
                  "
                >
                  This location has no incoming or
                  outgoing edges.
                </div>
              </section>
            </template>

            <div
                v-else
                class="
                edit-box__state
                edit-box__state--vertical
                world-location-graph__inspector-placeholder
              "
            >
              <div class="edit-box__state-icon">
                <svg
                    viewBox="0 0 24 24"
                    aria-hidden="true"
                >
                  <circle
                      cx="6"
                      cy="12"
                      r="3"
                  />

                  <circle
                      cx="18"
                      cy="12"
                      r="3"
                  />

                  <path d="M9 12h6"/>
                  <path d="m12 9 3 3-3 3"/>
                </svg>
              </div>

              <div class="edit-box__state-content">
                <strong
                    class="edit-box__state-title"
                >
                  Select a location or edge
                </strong>

                <p
                    class="
                    edit-box__state-description
                  "
                >
                  Select a node to inspect its
                  incoming and outgoing edges, or
                  select an arrow to inspect the
                  edge description and flags.
                </p>
              </div>
            </div>
          </aside>
        </template>
      </SplitPanel>

      <div
          v-else
          class="
          edit-box__state
          edit-box__state--vertical
          world-location-graph__state
        "
      >
        <div class="edit-box__state-icon">
          <svg
              viewBox="0 0 24 24"
              aria-hidden="true"
          >
            <circle
                cx="12"
                cy="12"
                r="9"
            />

            <path d="M8 12h8"/>
          </svg>
        </div>

        <div class="edit-box__state-content">
          <strong class="edit-box__state-title">
            No locations
          </strong>

          <p class="edit-box__state-description">
            {{ worldName }} does not currently
            contain any locations to visualize.
          </p>
        </div>
      </div>
    </div>
  </article>
</template>

<style scoped>
.world-location-graph {
  width: 100%;
  min-width: 0;

  color: rgb(var(--c-fg));
  font-family: var(--font-primary);
}

.world-location-graph__body {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.world-location-graph
.edit-box__action
svg {
  width: 1rem;
  height: 1rem;

  fill: none;
  stroke: currentColor;
  stroke-width: 1.9;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.world-location-graph__statistics,
.world-location-graph__diagnostics {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-2);

  min-width: 0;

  padding: var(--space-2);

  border-radius: var(--radius-sm);
}

.world-location-graph__statistics {
  background:
      rgb(var(--c-info) / 0.035);

  border:
      1px solid
      rgb(var(--c-info) / 0.15);
}

.world-location-graph__diagnostics {
  background:
      rgb(var(--c-warning) / 0.045);

  border:
      1px solid
      rgb(var(--c-warning) / 0.2);
}

.world-location-graph__state {
  min-height: 20rem;
}

/* -------------------------------------------------------------------------- */
/* Main layout                                                                */
/* -------------------------------------------------------------------------- */

.world-location-graph__split-panel {
  width: 100%;
  min-width: 0;
  height: clamp(34rem, 72dvh, 58rem);

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-surface-raised) / 0.4),
          rgb(var(--c-surface-2) / 0.22)
      );

  border:
      1px solid
      rgb(var(--c-border) / 0.24);
  border-radius: var(--radius-md);

  box-shadow:
      inset 0 1px 0
      rgb(255 255 255 / 0.28),
      0 5px 16px
      rgb(var(--c-shadow) / 0.055);

  overflow: hidden;
}

/* -------------------------------------------------------------------------- */
/* Viewport                                                                   */
/* -------------------------------------------------------------------------- */

.world-location-graph__viewport {
  position: relative;

  width: 100%;
  min-width: 0;
  height: 100%;

  overflow: hidden;

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-page) / 0.36),
          rgb(var(--c-surface-2) / 0.22)
      );

  touch-action: none;
  user-select: none;
}

.world-location-graph__svg {
  display: block;

  width: 100%;
  height: 100%;

  cursor: grab;
}

.world-location-graph__svg:active {
  cursor: grabbing;
}

.world-location-graph__grid-line {
  fill: none;

  stroke:
      rgb(var(--c-border) / 0.15);
  stroke-width: 1;
}

.world-location-graph__viewport-help {
  position: absolute;
  right: var(--space-2);
  bottom: var(--space-2);

  max-width: calc(100% - 1rem);

  padding:
      0.28rem
      0.5rem;

  color: rgb(var(--c-muted));

  background:
      rgb(var(--c-surface-raised) / 0.82);

  border:
      1px solid
      rgb(var(--c-border) / 0.22);
  border-radius: var(--radius-round);

  font-size: 0.63rem;
  font-weight: 650;
  line-height: 1.25;
  text-align: center;

  pointer-events: none;
  backdrop-filter: blur(8px);
}

.world-location-graph__legend {
  position: absolute;
  top: var(--space-2);
  left: var(--space-2);

  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-2);

  max-width: calc(100% - 1rem);

  padding:
      0.35rem
      0.55rem;

  color: rgb(var(--c-muted));

  background:
      rgb(var(--c-surface-raised) / 0.82);

  border:
      1px solid
      rgb(var(--c-border) / 0.22);
  border-radius: var(--radius-md);

  font-size: 0.62rem;
  font-weight: 700;

  pointer-events: none;
  backdrop-filter: blur(8px);
}

.world-location-graph__legend-item {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
}

.world-location-graph__legend-line {
  position: relative;

  width: 1.5rem;
  height: 2px;

  background:
      rgb(var(--c-border-strong));
}

.world-location-graph__legend-line::after {
  content: "";

  position: absolute;
  top: 50%;
  right: -0.08rem;

  width: 0;
  height: 0;

  border-top:
      0.22rem solid transparent;
  border-bottom:
      0.22rem solid transparent;
  border-left:
      0.36rem solid
      rgb(var(--c-border-strong));

  transform: translateY(-50%);
}

.world-location-graph__legend-line--blocked {
  background:
      repeating-linear-gradient(
          to right,
          rgb(var(--c-danger)),
          rgb(var(--c-danger)) 0.25rem,
          transparent 0.25rem,
          transparent 0.42rem
      );
}

.world-location-graph__legend-line--blocked::after {
  border-left-color:
      rgb(var(--c-danger));
}

.world-location-graph__legend-curve {
  width: 1.55rem;
  height: 0.7rem;

  border-top:
      2px solid
      rgb(var(--c-primary));
  border-radius: 50%;
}

/* -------------------------------------------------------------------------- */
/* Arrow markers                                                              */
/* -------------------------------------------------------------------------- */

.world-location-graph__arrow {
  fill:
      rgb(var(--c-border-strong));
}

.world-location-graph__arrow--selected {
  fill:
      rgb(var(--c-accent));
}

.world-location-graph__arrow--blocked {
  fill:
      rgb(var(--c-danger));
}

/* -------------------------------------------------------------------------- */
/* Directed edges                                                             */
/* -------------------------------------------------------------------------- */

.world-location-graph__edge-group {
  cursor: pointer;

  outline: 0;

  transition:
      opacity
      var(--duration-fast)
      var(--ease-standard);
}

.world-location-graph__edge-group--dimmed {
  opacity: 0.17;
}

.world-location-graph__edge-hit {
  fill: none;

  stroke: transparent;
  stroke-width: 14;

  pointer-events: stroke;
}

.world-location-graph__edge {
  fill: none;

  stroke:
      rgb(var(--c-border-strong) / 0.58);
  stroke-width: 1.9;

  pointer-events: none;

  transition:
      stroke
      var(--duration-fast)
      var(--ease-standard),
      stroke-width
      var(--duration-fast)
      var(--ease-standard),
      opacity
      var(--duration-fast)
      var(--ease-standard);
}

.world-location-graph__edge--reciprocal {
  stroke:
      rgb(var(--c-primary) / 0.67);
}

.world-location-graph__edge--blocked {
  stroke:
      rgb(var(--c-danger) / 0.8);
  stroke-dasharray: 6 4;
}

.world-location-graph__edge--related {
  stroke:
      rgb(var(--c-accent-2) / 0.88);
  stroke-width: 2.8;
}

.world-location-graph__edge--selected {
  stroke:
      rgb(var(--c-accent));
  stroke-width: 4;
  stroke-dasharray: none;

  filter:
      drop-shadow(
          0 0 4px
          rgb(var(--c-accent) / 0.45)
      );
}

.world-location-graph__edge-group:hover
.world-location-graph__edge {
  stroke:
      rgb(var(--c-accent));
  stroke-width: 3.4;
}

.world-location-graph__edge-label rect {
  fill:
      rgb(var(--c-surface-raised) / 0.92);

  stroke:
      rgb(var(--c-border) / 0.4);
  stroke-width: 1;

  vector-effect: non-scaling-stroke;

  filter:
      drop-shadow(
          0 2px 3px
          rgb(var(--c-shadow) / 0.13)
      );
}

.world-location-graph__edge-label text {
  fill: rgb(var(--c-fg));

  font-family: var(--font-primary);
  font-size: 0.62rem;
  font-weight: 750;
}

.world-location-graph__edge-label--selected rect {
  fill:
      rgb(var(--c-accent-soft));

  stroke:
      rgb(var(--c-accent));
  stroke-width: 1.5;
}

.world-location-graph__edge-label--blocked rect {
  fill:
      rgb(var(--c-danger-soft));

  stroke:
      rgb(var(--c-danger) / 0.65);
}

.world-location-graph__edge-label--blocked text {
  fill:
      rgb(var(--c-danger-strong));
}

/* -------------------------------------------------------------------------- */
/* Nodes                                                                      */
/* -------------------------------------------------------------------------- */

.world-location-graph__node {
  cursor: pointer;

  outline: 0;

  transition:
      opacity
      var(--duration-fast)
      var(--ease-standard);
}

.world-location-graph__node-halo {
  fill: transparent;

  stroke:
      rgb(var(--c-accent) / 0);
  stroke-width: 4;

  transition:
      fill
      var(--duration-fast)
      var(--ease-standard),
      stroke
      var(--duration-fast)
      var(--ease-standard);
}

.world-location-graph__node-circle {
  fill:
      rgb(var(--c-surface-raised));

  stroke:
      rgb(var(--c-primary) / 0.74);
  stroke-width: 2;

  filter:
      drop-shadow(
          0 4px 6px
          rgb(var(--c-shadow) / 0.18)
      );

  transition:
      fill
      var(--duration-fast)
      var(--ease-standard),
      stroke
      var(--duration-fast)
      var(--ease-standard);
}

.world-location-graph__node:hover
.world-location-graph__node-circle {
  fill:
      rgb(var(--c-surface-hover));

  stroke:
      rgb(var(--c-accent));
}

.world-location-graph__node:hover
.world-location-graph__node-halo {
  fill:
      rgb(var(--c-accent) / 0.08);

  stroke:
      rgb(var(--c-accent) / 0.22);
}

.world-location-graph__node--selected
.world-location-graph__node-circle {
  fill:
      rgb(var(--c-accent-soft));

  stroke:
      rgb(var(--c-accent));
  stroke-width: 3;
}

.world-location-graph__node--selected
.world-location-graph__node-halo {
  fill:
      rgb(var(--c-accent) / 0.12);

  stroke:
      rgb(var(--c-accent) / 0.42);
}

.world-location-graph__node--edge-endpoint
.world-location-graph__node-circle {
  fill:
      rgb(var(--c-info-soft));

  stroke:
      rgb(var(--c-info));
  stroke-width: 3;
}

.world-location-graph__node--edge-endpoint
.world-location-graph__node-halo {
  fill:
      rgb(var(--c-info) / 0.09);

  stroke:
      rgb(var(--c-info) / 0.32);
}

.world-location-graph__node--matching
.world-location-graph__node-circle {
  stroke:
      rgb(var(--c-info));
  stroke-width: 4;
}

.world-location-graph__node--isolated
.world-location-graph__node-circle {
  fill:
      rgb(var(--c-warning-soft));

  stroke:
      rgb(var(--c-warning));
  stroke-dasharray: 4 3;
}

.world-location-graph__node--dimmed {
  opacity: 0.22;
}

.world-location-graph__node-degree {
  fill:
      rgb(var(--c-fg-strong));

  font-family:
      var(--font-monospace);
  font-size: 0.72rem;
  font-weight: 850;

  pointer-events: none;
}

.world-location-graph__node-label {
  fill:
      rgb(var(--c-fg-strong));

  paint-order: stroke;

  stroke:
      rgb(var(--c-surface-raised) / 0.92);
  stroke-width: 4px;
  stroke-linejoin: round;

  font-family:
      var(--font-primary);
  font-size: 0.72rem;
  font-weight: 750;

  pointer-events: none;
}

/* -------------------------------------------------------------------------- */
/* Inspector                                                                  */
/* -------------------------------------------------------------------------- */

.world-location-graph__inspector {
  width: 100%;
  min-width: 0;
  height: 100%;
  box-sizing: border-box;

  display: flex;
  flex-direction: column;
  gap: var(--space-3);

  padding: var(--space-3);

  overflow: auto;
  overscroll-behavior: contain;

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-surface-raised) / 0.4),
          rgb(var(--c-surface-2) / 0.24)
      );

  scrollbar-width: thin;
  scrollbar-color:
      rgb(var(--c-primary) / 0.42)
      transparent;
}

.world-location-graph__inspector-header {
  padding-bottom: var(--space-3);

  border-bottom:
      1px solid
      rgb(var(--c-border) / 0.22);
}

.world-location-graph__inspector-header
.edit-box__eyebrow {
  margin:
      0
      0
      var(--space-1);
}

.world-location-graph__inspector-title {
  margin: 0;

  color:
      rgb(var(--c-fg-strong));

  font-size: 1.05rem;
  font-weight: 850;
  line-height: 1.3;

  overflow-wrap: anywhere;
}

.world-location-graph__edge-title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.world-location-graph__edge-title button {
  min-width: 0;

  padding: 0;

  color: inherit;

  background: transparent;
  border: 0;

  font: inherit;
  text-align: left;

  cursor: pointer;

  overflow-wrap: anywhere;
}

.world-location-graph__edge-title button:hover {
  color:
      rgb(var(--c-primary-strong));

  text-decoration: underline;
}

.world-location-graph__identifier {
  display: block;

  margin-top: var(--space-1);

  color: rgb(var(--c-muted));

  font-family:
      var(--font-monospace);
  font-size: 0.68rem;
  font-weight: 650;
}

.world-location-graph__edge-status {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.world-location-graph__details {
  display: grid;
  grid-template-columns:
      repeat(
          auto-fit,
          minmax(6.5rem, 1fr)
      );

  gap: var(--space-2);

  margin: 0;
}

.world-location-graph__detail {
  padding: var(--space-2);

  background:
      rgb(var(--c-surface-raised) / 0.46);

  border:
      1px solid
      rgb(var(--c-border) / 0.2);
  border-radius: var(--radius-sm);
}

.world-location-graph__detail--outgoing {
  border-color:
      rgb(var(--c-info) / 0.35);

  background:
      rgb(var(--c-info) / 0.05);
}

.world-location-graph__detail--incoming {
  border-color:
      rgb(var(--c-success) / 0.35);

  background:
      rgb(var(--c-success) / 0.05);
}

.world-location-graph__detail dt {
  margin-bottom: var(--space-1);

  color: rgb(var(--c-muted));

  font-size: 0.63rem;
  font-weight: 800;
  line-height: 1.2;

  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.world-location-graph__detail dd {
  margin: 0;

  color:
      rgb(var(--c-fg-strong));

  font-size: 0.82rem;
  font-weight: 800;
  line-height: 1.3;

  overflow-wrap: anywhere;
}

.world-location-graph__edge-description,
.world-location-graph__edge-flags,
.world-location-graph__reverse-edge,
.world-location-graph__connections {
  padding: var(--space-3);
}

.world-location-graph__description-text {
  margin: 0;

  color: rgb(var(--c-fg));

  font-size: 0.78rem;
  line-height: 1.6;

  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.world-location-graph__empty-description,
.world-location-graph__empty-connections {
  min-height: 5rem;

  padding: var(--space-3);
}

.world-location-graph__flag-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);

  margin: 0;
}

.world-location-graph__flag {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);

  padding: var(--space-2);

  background:
      rgb(var(--c-surface-raised) / 0.42);

  border:
      1px solid
      rgb(var(--c-border) / 0.18);
  border-radius: var(--radius-sm);
}

.world-location-graph__flag dt {
  color:
      rgb(var(--c-fg-strong));

  font-size: 0.72rem;
  font-weight: 750;
}

.world-location-graph__flag dd {
  margin: 0;

  font-size: 0.68rem;
  font-weight: 800;
}

.world-location-graph__flag-value--enabled {
  color:
      rgb(var(--c-success-strong));
}

.world-location-graph__flag-value--disabled {
  color:
      rgb(var(--c-danger-strong));
}

.world-location-graph__reverse-button {
  width: 100%;
  min-width: 0;

  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);

  padding: var(--space-3);

  color:
      rgb(var(--c-fg-strong));

  background:
      rgb(var(--c-accent) / 0.1);

  border:
      1px solid
      rgb(var(--c-accent) / 0.28);
  border-radius: var(--radius-sm);

  font: inherit;
  font-size: 0.76rem;
  font-weight: 800;

  cursor: pointer;
}

.world-location-graph__reverse-button:hover {
  background:
      rgb(var(--c-accent) / 0.18);

  border-color:
      rgb(var(--c-accent) / 0.5);
}

/* -------------------------------------------------------------------------- */
/* Node connection list                                                       */
/* -------------------------------------------------------------------------- */

.world-location-graph__connection-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.world-location-graph__connection {
  width: 100%;
  min-width: 0;

  display: grid;
  grid-template-columns:
      2rem
      minmax(0, 1fr)
      auto;
  align-items: center;
  gap: var(--space-2);

  padding: var(--space-2);

  color: rgb(var(--c-fg));

  background:
      rgb(var(--c-surface-raised) / 0.52);

  border:
      1px solid
      rgb(var(--c-border) / 0.22);
  border-radius: var(--radius-sm);

  font: inherit;
  text-align: left;

  cursor: pointer;

  transition:
      background-color
      var(--duration-fast)
      var(--ease-standard),
      border-color
      var(--duration-fast)
      var(--ease-standard),
      transform
      var(--duration-fast)
      var(--ease-standard);
}

.world-location-graph__connection:hover {
  background:
      rgb(var(--c-surface-hover) / 0.82);

  border-color:
      rgb(var(--c-accent) / 0.4);

  transform: translateY(-1px);
}

.world-location-graph__connection--outgoing {
  border-left:
      3px solid
      rgb(var(--c-info) / 0.7);
}

.world-location-graph__connection--incoming {
  border-left:
      3px solid
      rgb(var(--c-success) / 0.7);
}

.world-location-graph__connection--blocked {
  border-color:
      rgb(var(--c-danger) / 0.35);

  background:
      rgb(var(--c-danger) / 0.04);
}

.world-location-graph__connection-direction {
  width: 1.8rem;
  height: 1.8rem;

  display: grid;
  place-items: center;

  color:
      rgb(var(--c-primary-strong));

  background:
      rgb(var(--c-accent) / 0.12);

  border:
      1px solid
      rgb(var(--c-accent) / 0.24);
  border-radius: var(--radius-sm);

  font-family:
      var(--font-monospace);
  font-size: 1rem;
  font-weight: 850;
}

.world-location-graph__connection-main {
  min-width: 0;

  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.world-location-graph__connection-main strong {
  overflow: hidden;

  color:
      rgb(var(--c-fg-strong));

  font-size: 0.76rem;
  font-weight: 800;

  text-overflow: ellipsis;
  white-space: nowrap;
}

.world-location-graph__connection-main span {
  display: -webkit-box;

  overflow: hidden;

  color: rgb(var(--c-muted));

  font-size: 0.66rem;
  line-height: 1.35;

  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.world-location-graph__connection-status {
  padding:
      0.18rem
      0.38rem;

  color:
      rgb(var(--c-primary-strong));

  background:
      rgb(var(--c-accent) / 0.11);

  border-radius: var(--radius-round);

  font-family:
      var(--font-monospace);
  font-size: 0.61rem;
  font-weight: 800;
}

.world-location-graph__inspector-placeholder {
  flex: 1 1 auto;

  min-height: 18rem;
}

/* -------------------------------------------------------------------------- */
/* Responsive                                                                 */
/* -------------------------------------------------------------------------- */

@media (max-width: 760px) {
  .world-location-graph__split-panel {
    height: 44rem;
  }

  .world-location-graph__inspector {
    padding: var(--space-2);
  }

  .world-location-graph__legend {
    right: var(--space-2);
  }
}

@media (max-width: 480px) {
  .world-location-graph__split-panel {
    height: 38rem;
  }

  .world-location-graph__viewport-help {
    display: none;
  }

  .world-location-graph__legend {
    font-size: 0.56rem;
  }

  .world-location-graph__edge-title {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (prefers-reduced-motion: reduce) {
  .world-location-graph__edge,
  .world-location-graph__edge-group,
  .world-location-graph__node,
  .world-location-graph__node-circle,
  .world-location-graph__node-halo,
  .world-location-graph__connection {
    transition: none;
  }

  .world-location-graph__connection:hover {
    transform: none;
  }
}
</style>

