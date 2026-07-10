<script setup lang="ts">
import {
  computed,
  onMounted,
  ref,
  watch,
} from "vue";

import type {
  EdgeData,
} from "@/domain/World";

import {
  Location,
  LocationEdge,
} from "@/domain/World";

import SplitPanel from "@/components/utils/panels/SplitPanel.vue";
import FieldEditorWrapper from "@/components/utils/FieldEditorWrapper.vue";
import LongTextBox from "@/components/utils/primitiveEditors/LongTextBox.vue";
import BooleanTickBox from "@/components/utils/primitiveEditors/BooleanTickBox.vue";

import ConnectLocationPrompt, {
  type ConnectionDirection,
} from "@/components/space/ConnectLocationPrompt.vue";

type EdgeDirection =
    | "incoming"
    | "outgoing";

const model = defineModel<{
  parentLocation: Location;
  all_locations: Location[];
}>({
  required: true,
});

/*
 * A neighbour may have:
 *
 * parentLocation -> neighbour
 * neighbour -> parentLocation
 * both
 */
const neighbouringLocations =
    ref<Location[]>([]);

const outgoingEdgesByLocationId =
    ref<Map<number, LocationEdge>>(
        new Map(),
    );

const incomingEdgesByLocationId =
    ref<Map<number, LocationEdge>>(
        new Map(),
    );

const editingNeighbour =
    ref<Location | undefined>();

const outEdge =
    ref<LocationEdge | undefined>();

const inEdge =
    ref<LocationEdge | undefined>();

const creatingNeighbour = ref(false);
const editingBoth = ref(false);

const isLoading = ref(false);
const isMutating = ref(false);
const isSaving = ref(false);

const loadError = ref<string | null>(null);
const operationError = ref<string | null>(
    null,
);

let loadRequestId = 0;

const DEFAULT_EDGE_DATA: EdgeData = {
  edge_description: "",
  show_destination_name: true,
  show_destination_description: true,
  is_traversable: true,
};

function getLocationName(
    location: Location,
): string {
  const name = String(
      location.get("name") ?? "",
  ).trim();

  return name || "Unnamed location";
}

function getLocationId(
    location: Location,
): number | undefined {
  return location.get("id");
}

function getLocationIdLabel(
    location: Location,
): string {
  return String(
      location.get("id") ??
      location.hashKey(),
  );
}

function locationsEqual(
    first: Location,
    second: Location,
): boolean {
  return first.equals(second);
}

function sortLocations(
    locations: Location[],
): Location[] {
  return [...locations].sort(
      (first, second) =>
          getLocationName(first).localeCompare(
              getLocationName(second),
          ),
  );
}

function getOutgoingEdge(
    neighbour: Location,
): LocationEdge | undefined {
  const id = getLocationId(neighbour);

  if (id == null) {
    return undefined;
  }

  return outgoingEdgesByLocationId.value.get(
      id,
  );
}

function getIncomingEdge(
    neighbour: Location,
): LocationEdge | undefined {
  const id = getLocationId(neighbour);

  if (id == null) {
    return undefined;
  }

  return incomingEdgesByLocationId.value.get(
      id,
  );
}

function hasOutgoingEdge(
    neighbour: Location,
): boolean {
  return getOutgoingEdge(neighbour) != null;
}

function hasIncomingEdge(
    neighbour: Location,
): boolean {
  return getIncomingEdge(neighbour) != null;
}

function synchronizeSelectedEdges(): void {
  if (!editingNeighbour.value) {
    outEdge.value = undefined;
    inEdge.value = undefined;
    editingBoth.value = false;

    return;
  }

  outEdge.value = getOutgoingEdge(
      editingNeighbour.value,
  );

  inEdge.value = getIncomingEdge(
      editingNeighbour.value,
  );

  if (!outEdge.value || !inEdge.value) {
    editingBoth.value = false;
  }
}

function setOutgoingEdge(
    neighbour: Location,
    edge: LocationEdge | undefined,
): void {
  const id = getLocationId(neighbour);

  if (id == null) {
    return;
  }

  const edges = new Map(
      outgoingEdgesByLocationId.value,
  );

  if (edge) {
    edges.set(id, edge);
  } else {
    edges.delete(id);
  }

  outgoingEdgesByLocationId.value = edges;

  if (
      editingNeighbour.value?.equals(neighbour)
  ) {
    outEdge.value = edge;
  }
}

function setIncomingEdge(
    neighbour: Location,
    edge: LocationEdge | undefined,
): void {
  const id = getLocationId(neighbour);

  if (id == null) {
    return;
  }

  const edges = new Map(
      incomingEdgesByLocationId.value,
  );

  if (edge) {
    edges.set(id, edge);
  } else {
    edges.delete(id);
  }

  incomingEdgesByLocationId.value = edges;

  if (
      editingNeighbour.value?.equals(neighbour)
  ) {
    inEdge.value = edge;
  }
}

function ensureNeighbourListed(
    neighbour: Location,
): void {
  const alreadyListed =
      neighbouringLocations.value.some(
          (candidate) =>
              candidate.equals(neighbour),
      );

  if (alreadyListed) {
    return;
  }

  neighbouringLocations.value =
      sortLocations([
        ...neighbouringLocations.value,
        neighbour,
      ]);
}

function removeNeighbourIfDisconnected(
    neighbour: Location,
): void {
  if (
      hasOutgoingEdge(neighbour) ||
      hasIncomingEdge(neighbour)
  ) {
    return;
  }

  neighbouringLocations.value =
      neighbouringLocations.value.filter(
          (candidate) =>
              !candidate.equals(neighbour),
      );

  if (
      editingNeighbour.value?.equals(neighbour)
  ) {
    editingNeighbour.value = undefined;
    outEdge.value = undefined;
    inEdge.value = undefined;
    editingBoth.value = false;
  }
}

const parentLocationName = computed(
    () =>
        getLocationName(
            model.value.parentLocation,
        ),
);

const parentLocationId = computed(
    () =>
        getLocationIdLabel(
            model.value.parentLocation,
        ),
);

const connectableLocations = computed<
    Location[]
>(() =>
    sortLocations(
        model.value.all_locations.filter(
            (location) => {
              if (
                  location.equals(
                      model.value.parentLocation,
                  )
              ) {
                return false;
              }

              return !neighbouringLocations.value.some(
                  (neighbour) =>
                      neighbour.equals(location),
              );
            },
        ),
    ),
);

const canEditBoth = computed(
    () =>
        outEdge.value != null &&
        inEdge.value != null,
);

const neighbourCountLabel = computed(
    () => {
      const count =
          neighbouringLocations.value.length;

      return `${count} ${
          count === 1
              ? "neighbour"
              : "neighbours"
      }`;
    },
);

const outgoingCount = computed(
    () =>
        outgoingEdgesByLocationId.value
            .size,
);

const incomingCount = computed(
    () =>
        incomingEdgesByLocationId.value
            .size,
);

const bidirectionalCount = computed(
    () =>
        neighbouringLocations.value.filter(
            (neighbour) =>
                hasOutgoingEdge(neighbour) &&
                hasIncomingEdge(neighbour),
        ).length,
);

function editNeighbour(
    neighbour: Location,
): void {
  operationError.value = null;

  if (
      editingNeighbour.value?.equals(neighbour)
  ) {
    editingNeighbour.value = undefined;
    outEdge.value = undefined;
    inEdge.value = undefined;
    editingBoth.value = false;

    return;
  }

  editingNeighbour.value = neighbour;
  editingBoth.value = false;

  synchronizeSelectedEdges();
}

async function createNeighbour(
    other: Location,
    direction: ConnectionDirection,
): Promise<void> {
  creatingNeighbour.value = false;
  operationError.value = null;

  editingNeighbour.value = other;
  editingBoth.value = false;

  ensureNeighbourListed(other);
  synchronizeSelectedEdges();

  const operations: Promise<
      LocationEdge | undefined
  >[] = [];

  if (
      direction === "outgoing" ||
      direction === "both"
  ) {
    operations.push(createOutEdge());
  }

  if (
      direction === "incoming" ||
      direction === "both"
  ) {
    operations.push(createInEdge());
  }

  if (operations.length === 0) {
    return;
  }

  isMutating.value = true;

  try {
    const results =
        await Promise.allSettled(
            operations,
        );

    const failures = results.filter(
        (result) =>
            result.status === "rejected",
    );

    if (failures.length > 0) {
      for (const failure of failures) {
        if (
            failure.status === "rejected"
        ) {
          console.error(
              "Could not create location edge",
              failure.reason,
          );
        }
      }

      operationError.value =
          failures.length ===
          results.length
              ? "The connection could not be created."
              : "One direction was created, but the other direction failed.";
    }

    synchronizeSelectedEdges();

    if (
        direction === "both" &&
        outEdge.value &&
        inEdge.value
    ) {
      editingBoth.value = true;
    }

    removeNeighbourIfDisconnected(other);
  } finally {
    isMutating.value = false;
  }
}

async function createInEdge(): Promise<
    LocationEdge | undefined
> {
  const neighbour =
      editingNeighbour.value;

  if (!neighbour) {
    return undefined;
  }

  if (inEdge.value) {
    return inEdge.value;
  }

  const edge = await neighbour.connect(
      model.value.parentLocation,
      {
        ...DEFAULT_EDGE_DATA,
      },
  );

  ensureNeighbourListed(neighbour);
  setIncomingEdge(neighbour, edge);

  return edge;
}

async function createOutEdge(): Promise<
    LocationEdge | undefined
> {
  const neighbour =
      editingNeighbour.value;

  if (!neighbour) {
    return undefined;
  }

  if (outEdge.value) {
    return outEdge.value;
  }

  const edge =
      await model.value.parentLocation
          .connect(
              neighbour,
              {
                ...DEFAULT_EDGE_DATA,
              },
          );

  ensureNeighbourListed(neighbour);
  setOutgoingEdge(neighbour, edge);

  return edge;
}

async function handleCreateInEdge(): Promise<void> {
  operationError.value = null;
  isMutating.value = true;

  try {
    await createInEdge();
  } catch (error) {
    console.error(
        "Could not create incoming edge",
        error,
    );

    operationError.value =
        "The incoming edge could not be created.";
  } finally {
    isMutating.value = false;
  }
}

async function handleCreateOutEdge(): Promise<void> {
  operationError.value = null;
  isMutating.value = true;

  try {
    await createOutEdge();
  } catch (error) {
    console.error(
        "Could not create outgoing edge",
        error,
    );

    operationError.value =
        "The outgoing edge could not be created.";
  } finally {
    isMutating.value = false;
  }
}

async function deleteInEdge(): Promise<void> {
  const neighbour =
      editingNeighbour.value;

  if (!neighbour || !inEdge.value) {
    return;
  }

  const confirmed = window.confirm(
      `Delete the edge from ` +
      `${getLocationName(neighbour)} to ` +
      `${parentLocationName.value}?`,
  );

  if (!confirmed) {
    return;
  }

  operationError.value = null;
  isMutating.value = true;

  try {
    const success =
        await neighbour.disconnect(
            model.value.parentLocation,
        );

    if (!success) {
      throw new Error(
          "Incoming edge deletion returned false.",
      );
    }

    setIncomingEdge(
        neighbour,
        undefined,
    );

    editingBoth.value = false;

    removeNeighbourIfDisconnected(
        neighbour,
    );
  } catch (error) {
    console.error(
        "Could not delete incoming edge",
        error,
    );

    operationError.value =
        "The incoming edge could not be deleted.";
  } finally {
    isMutating.value = false;
  }
}

async function deleteOutEdge(): Promise<void> {
  const neighbour =
      editingNeighbour.value;

  if (!neighbour || !outEdge.value) {
    return;
  }

  const confirmed = window.confirm(
      `Delete the edge from ` +
      `${parentLocationName.value} to ` +
      `${getLocationName(neighbour)}?`,
  );

  if (!confirmed) {
    return;
  }

  operationError.value = null;
  isMutating.value = true;

  try {
    const success =
        await model.value.parentLocation
            .disconnect(neighbour);

    if (!success) {
      throw new Error(
          "Outgoing edge deletion returned false.",
      );
    }

    setOutgoingEdge(
        neighbour,
        undefined,
    );

    editingBoth.value = false;

    removeNeighbourIfDisconnected(
        neighbour,
    );
  } catch (error) {
    console.error(
        "Could not delete outgoing edge",
        error,
    );

    operationError.value =
        "The outgoing edge could not be deleted.";
  } finally {
    isMutating.value = false;
  }
}

async function deleteBothEdges(): Promise<void> {
  const neighbour =
      editingNeighbour.value;

  if (!neighbour) {
    return;
  }

  const confirmed = window.confirm(
      `Delete both directional edges between ` +
      `${parentLocationName.value} and ` +
      `${getLocationName(neighbour)}?`,
  );

  if (!confirmed) {
    return;
  }

  operationError.value = null;
  isMutating.value = true;

  try {
    const operations: Promise<boolean>[] =
        [];

    if (outEdge.value) {
      operations.push(
          model.value.parentLocation
              .disconnect(neighbour),
      );
    }

    if (inEdge.value) {
      operations.push(
          neighbour.disconnect(
              model.value.parentLocation,
          ),
      );
    }

    const results =
        await Promise.allSettled(
            operations,
        );

    const failures = results.filter(
        (result) =>
            result.status === "rejected" ||
            (
                result.status ===
                "fulfilled" &&
                !result.value
            ),
    );

    /*
     * Reload because one direction may have
     * succeeded while the other failed.
     */
    await load();

    if (failures.length > 0) {
      operationError.value =
          failures.length ===
          results.length
              ? "Neither directional edge could be deleted."
              : "One edge was deleted, but the other deletion failed.";
    }
  } finally {
    isMutating.value = false;
  }
}

async function updateEdgeField<
    K extends keyof EdgeData,
>(
    direction: EdgeDirection,
    field: K,
    value: EdgeData[K],
): Promise<void> {
  const selectedEdge =
      direction === "outgoing"
          ? outEdge.value
          : inEdge.value;

  if (!selectedEdge) {
    return;
  }

  const targets: LocationEdge[] =
      editingBoth.value &&
      outEdge.value &&
      inEdge.value
          ? [
            outEdge.value,
            inEdge.value,
          ]
          : [selectedEdge];

  operationError.value = null;
  isSaving.value = true;

  try {
    await Promise.all(
        targets.map(
            (edge) =>
                edge.update(
                    field,
                    value,
                ),
        ),
    );
  } catch (error) {
    console.error(
        `Could not update ${String(field)}`,
        error,
    );

    operationError.value =
        editingBoth.value
            ? "The field could not be updated on both edges."
            : "The edge field could not be updated.";
  } finally {
    isSaving.value = false;
  }
}

async function load(): Promise<void> {
  const requestId = ++loadRequestId;

  const selectedLocationId =
      editingNeighbour.value?.get("id");

  isLoading.value = true;
  loadError.value = null;

  try {
    const [
      neighbours,
      outgoingEdges,
      incomingEdges,
    ] = await Promise.all([
      model.value.parentLocation
          .getNeighbours(),

      model.value.parentLocation
          .getOutEdges(),

      model.value.parentLocation
          .getInEdges(),
    ]);

    if (requestId !== loadRequestId) {
      return;
    }

    neighbouringLocations.value =
        sortLocations(neighbours);

    outgoingEdgesByLocationId.value =
        new Map(
            outgoingEdges.map(
                (edge) => [
                  edge.get("to_id"),
                  edge,
                ],
            ),
        );

    incomingEdgesByLocationId.value =
        new Map(
            incomingEdges.map(
                (edge) => [
                  edge.get("from_id"),
                  edge,
                ],
            ),
        );

    if (selectedLocationId != null) {
      editingNeighbour.value =
          neighbouringLocations.value.find(
              (location) =>
                  location.get("id") ===
                  selectedLocationId,
          );
    } else {
      editingNeighbour.value = undefined;
    }

    synchronizeSelectedEdges();
  } catch (error) {
    if (requestId !== loadRequestId) {
      return;
    }

    console.error(
        "Could not load location neighbours",
        error,
    );

    neighbouringLocations.value = [];

    outgoingEdgesByLocationId.value =
        new Map();

    incomingEdgesByLocationId.value =
        new Map();

    editingNeighbour.value = undefined;
    outEdge.value = undefined;
    inEdge.value = undefined;

    loadError.value =
        "The neighbouring locations and their edges could not be loaded.";
  } finally {
    if (requestId === loadRequestId) {
      isLoading.value = false;
    }
  }
}

onMounted(() => {
  void load();
});

watch(
    [
      () =>
          model.value.parentLocation.get(
              "worldID",
          ),

      () =>
          model.value.parentLocation.get(
              "id",
          ),
    ],
    () => {
      editingNeighbour.value = undefined;
      outEdge.value = undefined;
      inEdge.value = undefined;
      editingBoth.value = false;

      void load();
    },
);

watch(
    canEditBoth,
    (available) => {
      if (!available) {
        editingBoth.value = false;
      }
    },
);
</script>

<template>
  <section
      class="
      edit-box
      edit-box--accent
      location-edges-editor
    "
      :aria-busy="
      isLoading ||
      isMutating ||
      isSaving
    "
  >
    <header class="edit-box__header">
      <div class="edit-box__header-main">
        <span class="edit-box__eyebrow">
          Location connections
        </span>

        <div class="edit-box__title-row">
          <h2 class="edit-box__title">
            Neighbours of
            {{ parentLocationName }}
          </h2>

          <span class="edit-box__count">
            {{ neighbourCountLabel }}
          </span>
        </div>

        <p class="edit-box__description">
          A neighbour has at least one directional
          edge connected to this location. Incoming
          and outgoing edges can contain different
          descriptions and traversal settings.
        </p>
      </div>

      <div class="edit-box__actions">
        <button
            type="button"
            class="edit-box__action"
            :disabled="
            isLoading ||
            isMutating
          "
            @click="load"
        >
          <span
              v-if="isLoading"
              class="edit-box__spinner"
              aria-hidden="true"
          />

          <svg
              v-else
              viewBox="0 0 24 24"
              aria-hidden="true"
          >
            <path
                d="M20 11a8 8 0 0 0-15.5-2"
            />

            <path d="M4 4v5h5" />

            <path
                d="M4 13a8 8 0 0 0 15.5 2"
            />

            <path d="M20 20v-5h-5" />
          </svg>

          Refresh
        </button>

        <button
            type="button"
            class="
            edit-box__action
            edit-box__action--accent
          "
            :disabled="
            isLoading ||
            isMutating ||
            connectableLocations.length === 0
          "
            :title="
            connectableLocations.length === 0
              ? 'Every available location is already a neighbour'
              : 'Connect another location'
          "
            @click="creatingNeighbour = true"
        >
          <svg
              viewBox="0 0 24 24"
              aria-hidden="true"
          >
            <path d="M12 5v14" />
            <path d="M5 12h14" />
          </svg>

          Connect
        </button>
      </div>
    </header>

    <div class="edit-box__body">
      <div
          v-if="operationError"
          class="
          edit-box__state
          edit-box__state--error
          location-edges-editor__message
        "
          role="alert"
      >
        <div class="edit-box__state-content">
          <strong class="edit-box__state-title">
            Edge operation failed
          </strong>

          <p class="edit-box__state-description">
            {{ operationError }}
          </p>
        </div>
      </div>

      <div
          v-if="loadError"
          class="
          edit-box__state
          edit-box__state--error
          edit-box__state--vertical
          location-edges-editor__load-error
        "
          role="alert"
      >
        <div class="edit-box__state-icon">
          <svg
              viewBox="0 0 24 24"
              aria-hidden="true"
          >
            <path d="M12 9v4" />
            <path d="M12 17h.01" />

            <path
                d="M10.3 3.8 2.2 18a2 2 0 0 0 1.8 3h16a2 2 0 0 0 1.8-3L13.7 3.8a2 2 0 0 0-3.4 0Z"
            />
          </svg>
        </div>

        <div class="edit-box__state-content">
          <strong class="edit-box__state-title">
            Connections unavailable
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
            @click="load"
        >
          Retry
        </button>
      </div>

      <template v-else>
        <div
            class="
            location-edges-editor__statistics
          "
        >
          <span class="edit-box__badge">
            {{ outgoingCount }}
            outgoing
          </span>

          <span class="edit-box__badge">
            {{ incomingCount }}
            incoming
          </span>

          <span
              class="
              edit-box__badge
              edit-box__badge--success
            "
          >
            {{ bidirectionalCount }}
            bidirectional
          </span>

          <span
              v-if="isSaving"
              class="
              edit-box__badge
              edit-box__badge--neutral
            "
          >
            Saving changes
          </span>
        </div>

        <SplitPanel
            class="
            location-edges-editor__split
          "
            storage-key="edge editor"
        >
          <template #left>
            <aside class="neighbour-browser">
              <div class="neighbour-browser__source">
                <span class="edit-box__eyebrow">
                  Current location
                </span>

                <strong
                    class="
                    neighbour-browser__source-name
                  "
                >
                  {{ parentLocationName }}
                </strong>

                <span
                    class="
                    neighbour-browser__identifier
                  "
                >
                  ID {{ parentLocationId }}
                </span>
              </div>

              <div
                  v-if="isLoading"
                  class="
                  edit-box__state
                  neighbour-browser__state
                "
                  role="status"
              >
                <span
                    class="edit-box__spinner"
                    aria-hidden="true"
                />

                <div
                    class="
                    edit-box__state-content
                  "
                >
                  <strong
                      class="
                      edit-box__state-title
                    "
                  >
                    Loading neighbours
                  </strong>

                  <p
                      class="
                      edit-box__state-description
                    "
                  >
                    Retrieving incoming and
                    outgoing edges.
                  </p>
                </div>
              </div>

              <div
                  v-else-if="
                  neighbouringLocations.length > 0
                "
                  class="neighbour-list"
                  role="list"
                  aria-label="Neighbouring locations"
              >
                <button
                    v-for="
                    neighbour in neighbouringLocations
                  "
                    :key="
                    `${neighbour.get('worldID')}:${neighbour.get('id')}`
                  "
                    type="button"
                    class="neighbour-list__item"
                    :class="{
                    'neighbour-list__item--selected':
                      editingNeighbour?.equals(
                        neighbour,
                      ),
                  }"
                    role="listitem"
                    :aria-pressed="
                    editingNeighbour?.equals(
                      neighbour,
                    ) ?? false
                  "
                    @click="
                    editNeighbour(neighbour)
                  "
                >
                  <span
                      class="
                      neighbour-list__direction
                    "
                      :class="{
                      'neighbour-list__direction--both':
                        hasOutgoingEdge(
                          neighbour,
                        ) &&
                        hasIncomingEdge(
                          neighbour,
                        ),
                    }"
                      aria-hidden="true"
                  >
                    <template
                        v-if="
                        hasOutgoingEdge(
                          neighbour,
                        ) &&
                        hasIncomingEdge(
                          neighbour,
                        )
                      "
                    >
                      ↔
                    </template>

                    <template
                        v-else-if="
                        hasOutgoingEdge(
                          neighbour,
                        )
                      "
                    >
                      →
                    </template>

                    <template v-else>
                      ←
                    </template>
                  </span>

                  <span
                      class="
                      neighbour-list__content
                    "
                  >
                    <span
                        class="
                        neighbour-list__heading
                      "
                    >
                      <strong
                          class="
                          neighbour-list__name
                        "
                      >
                        {{
                          getLocationName(
                              neighbour,
                          )
                        }}
                      </strong>

                      <span
                          class="
                          neighbour-browser__identifier
                        "
                      >
                        ID
                        {{
                          getLocationIdLabel(
                              neighbour,
                          )
                        }}
                      </span>
                    </span>

                    <span
                        class="
                        neighbour-list__badges
                      "
                    >
                      <span
                          v-if="
                          hasOutgoingEdge(
                            neighbour,
                          )
                        "
                          class="direction-badge"
                      >
                        Outgoing
                      </span>

                      <span
                          v-if="
                          hasIncomingEdge(
                            neighbour,
                          )
                        "
                          class="direction-badge"
                      >
                        Incoming
                      </span>
                    </span>
                  </span>
                </button>
              </div>

              <div
                  v-else
                  class="
                  edit-box__state
                  edit-box__state--vertical
                  neighbour-browser__state
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

                    <path d="M9 12h6" />
                  </svg>
                </div>

                <div
                    class="
                    edit-box__state-content
                  "
                >
                  <strong
                      class="
                      edit-box__state-title
                    "
                  >
                    No neighbours
                  </strong>

                  <p
                      class="
                      edit-box__state-description
                    "
                  >
                    Connect another location with
                    an incoming, outgoing, or
                    bidirectional edge.
                  </p>
                </div>

                <button
                    v-if="
                    connectableLocations.length > 0
                  "
                    type="button"
                    class="
                    edit-box__action
                    edit-box__action--accent
                  "
                    @click="
                    creatingNeighbour = true
                  "
                >
                  Connect a location
                </button>
              </div>
            </aside>
          </template>

          <template #right>
            <main class="edge-editor">
              <template v-if="editingNeighbour">
                <header class="edge-editor__header">
                  <div>
                    <span class="edit-box__eyebrow">
                      Editing neighbour
                    </span>

                    <h3 class="edge-editor__title">
                      {{
                        getLocationName(
                            editingNeighbour,
                        )
                      }}
                    </h3>

                    <span
                        class="
                        edge-editor__identifier
                      "
                    >
                      ID
                      {{
                        getLocationIdLabel(
                            editingNeighbour,
                        )
                      }}
                    </span>
                  </div>

                  <label
                      class="synchronization-toggle"
                      :class="{
                      'synchronization-toggle--disabled':
                        !canEditBoth,
                    }"
                      :title="
                      canEditBoth
                        ? 'Apply field edits to both directional edges'
                        : 'Both directional edges must exist'
                    "
                  >
                    <span
                        class="
                        synchronization-toggle__text
                      "
                    >
                      <strong>
                        Edit both
                      </strong>

                      <span>
                        Synchronize field changes
                      </span>
                    </span>

                    <input
                        v-model="editingBoth"
                        type="checkbox"
                        :disabled="
                        !canEditBoth ||
                        isMutating
                      "
                    />

                    <span
                        class="
                        synchronization-toggle__switch
                      "
                        aria-hidden="true"
                    />
                  </label>
                </header>

                <div
                    class="
                    edge-editor__directions
                  "
                >
                  <section
                      class="
                      edge-direction
                      edge-direction--outgoing
                    "
                  >
                    <header
                        class="
                        edge-direction__header
                      "
                    >
                      <div
                          class="
                          edge-direction__heading
                        "
                      >
                        <span
                            class="
                            edge-direction__icon
                          "
                            aria-hidden="true"
                        >
                          →
                        </span>

                        <div>
                          <span
                              class="
                              edit-box__eyebrow
                            "
                          >
                            Outgoing edge
                          </span>

                          <h4
                              class="
                              edge-direction__title
                            "
                          >
                            {{ parentLocationName }}
                            →
                            {{
                              getLocationName(
                                  editingNeighbour,
                              )
                            }}
                          </h4>
                        </div>
                      </div>

                      <button
                          v-if="outEdge"
                          type="button"
                          class="
                          edit-box__action
                          edit-box__action--danger
                        "
                          :disabled="isMutating"
                          @click="deleteOutEdge"
                      >
                        Delete
                      </button>
                    </header>

                    <div
                        v-if="outEdge"
                        class="
                        edge-direction__fields
                      "
                    >
                      <FieldEditorWrapper
                          field-name="Description"
                          info="Context used when travelling from the current location to this neighbour."
                          :vertical="true"
                      >
                        <LongTextBox
                            :model-value="
                            outEdge.get(
                              'edge_description',
                            )
                          "
                            @edit="
                            value =>
                              updateEdgeField(
                                'outgoing',
                                'edge_description',
                                value,
                              )
                          "
                        />
                      </FieldEditorWrapper>

                      <div
                          class="
                          edge-direction__options
                        "
                      >
                        <FieldEditorWrapper
                            field-name="Traversable"
                            info="Whether movement is allowed in this direction."
                            :vertical="true"
                        >
                          <BooleanTickBox
                              :model-value="
                              Boolean(
                                outEdge.get(
                                  'is_traversable',
                                ),
                              )
                            "
                              @edit="
                              value =>
                                updateEdgeField(
                                  'outgoing',
                                  'is_traversable',
                                  value,
                                )
                            "
                          />
                        </FieldEditorWrapper>

                        <FieldEditorWrapper
                            field-name="Show destination name"
                            info="Include the neighbour's name when this edge is used."
                            :vertical="true"
                        >
                          <BooleanTickBox
                              :model-value="
                              Boolean(
                                outEdge.get(
                                  'show_destination_name',
                                ),
                              )
                            "
                              @edit="
                              value =>
                                updateEdgeField(
                                  'outgoing',
                                  'show_destination_name',
                                  value,
                                )
                            "
                          />
                        </FieldEditorWrapper>

                        <FieldEditorWrapper
                            field-name="Show destination description"
                            info="Include the neighbour's description when this edge is used."
                            :vertical="true"
                        >
                          <BooleanTickBox
                              :model-value="
                              Boolean(
                                outEdge.get(
                                  'show_destination_description',
                                ),
                              )
                            "
                              @edit="
                              value =>
                                updateEdgeField(
                                  'outgoing',
                                  'show_destination_description',
                                  value,
                                )
                            "
                          />
                        </FieldEditorWrapper>
                      </div>
                    </div>

                    <div
                        v-else
                        class="
                        edit-box__state
                        edit-box__state--vertical
                        edge-direction__empty
                      "
                    >
                      <div
                          class="
                          edit-box__state-content
                        "
                      >
                        <strong
                            class="
                            edit-box__state-title
                          "
                        >
                          No outgoing edge
                        </strong>

                        <p
                            class="
                            edit-box__state-description
                          "
                        >
                          The current location
                          cannot currently travel
                          toward this neighbour.
                        </p>
                      </div>

                      <button
                          type="button"
                          class="
                          edit-box__action
                          edit-box__action--accent
                        "
                          :disabled="isMutating"
                          @click="
                          handleCreateOutEdge
                        "
                      >
                        Create outgoing edge
                      </button>
                    </div>
                  </section>

                  <section
                      class="
                      edge-direction
                      edge-direction--incoming
                    "
                  >
                    <header
                        class="
                        edge-direction__header
                      "
                    >
                      <div
                          class="
                          edge-direction__heading
                        "
                      >
                        <span
                            class="
                            edge-direction__icon
                          "
                            aria-hidden="true"
                        >
                          ←
                        </span>

                        <div>
                          <span
                              class="
                              edit-box__eyebrow
                            "
                          >
                            Incoming edge
                          </span>

                          <h4
                              class="
                              edge-direction__title
                            "
                          >
                            {{
                              getLocationName(
                                  editingNeighbour,
                              )
                            }}
                            →
                            {{ parentLocationName }}
                          </h4>
                        </div>
                      </div>

                      <button
                          v-if="inEdge"
                          type="button"
                          class="
                          edit-box__action
                          edit-box__action--danger
                        "
                          :disabled="isMutating"
                          @click="deleteInEdge"
                      >
                        Delete
                      </button>
                    </header>

                    <div
                        v-if="inEdge"
                        class="
                        edge-direction__fields
                      "
                    >
                      <FieldEditorWrapper
                          field-name="Description"
                          info="Context used when travelling from the neighbour to the current location."
                          :vertical="true"
                      >
                        <LongTextBox
                            :model-value="
                            inEdge.get(
                              'edge_description',
                            )
                          "
                            @edit="
                            value =>
                              updateEdgeField(
                                'incoming',
                                'edge_description',
                                value,
                              )
                          "
                        />
                      </FieldEditorWrapper>

                      <div
                          class="
                          edge-direction__options
                        "
                      >
                        <FieldEditorWrapper
                            field-name="Traversable"
                            info="Whether movement is allowed in this direction."
                            :vertical="true"
                        >
                          <BooleanTickBox
                              :model-value="
                              Boolean(
                                inEdge.get(
                                  'is_traversable',
                                ),
                              )
                            "
                              @edit="
                              value =>
                                updateEdgeField(
                                  'incoming',
                                  'is_traversable',
                                  value,
                                )
                            "
                          />
                        </FieldEditorWrapper>

                        <FieldEditorWrapper
                            field-name="Show destination name"
                            info="Include the current location's name when this edge is used."
                            :vertical="true"
                        >
                          <BooleanTickBox
                              :model-value="
                              Boolean(
                                inEdge.get(
                                  'show_destination_name',
                                ),
                              )
                            "
                              @edit="
                              value =>
                                updateEdgeField(
                                  'incoming',
                                  'show_destination_name',
                                  value,
                                )
                            "
                          />
                        </FieldEditorWrapper>

                        <FieldEditorWrapper
                            field-name="Show destination description"
                            info="Include the current location's description when this edge is used."
                            :vertical="true"
                        >
                          <BooleanTickBox
                              :model-value="
                              Boolean(
                                inEdge.get(
                                  'show_destination_description',
                                ),
                              )
                            "
                              @edit="
                              value =>
                                updateEdgeField(
                                  'incoming',
                                  'show_destination_description',
                                  value,
                                )
                            "
                          />
                        </FieldEditorWrapper>
                      </div>
                    </div>

                    <div
                        v-else
                        class="
                        edit-box__state
                        edit-box__state--vertical
                        edge-direction__empty
                      "
                    >
                      <div
                          class="
                          edit-box__state-content
                        "
                      >
                        <strong
                            class="
                            edit-box__state-title
                          "
                        >
                          No incoming edge
                        </strong>

                        <p
                            class="
                            edit-box__state-description
                          "
                        >
                          This neighbour cannot
                          currently travel toward
                          the current location.
                        </p>
                      </div>

                      <button
                          type="button"
                          class="
                          edit-box__action
                          edit-box__action--accent
                        "
                          :disabled="isMutating"
                          @click="
                          handleCreateInEdge
                        "
                      >
                        Create incoming edge
                      </button>
                    </div>
                  </section>
                </div>

                <footer
                    v-if="outEdge && inEdge"
                    class="edge-editor__footer"
                >
                  <button
                      type="button"
                      class="
                      edit-box__action
                      edit-box__action--danger
                    "
                      :disabled="isMutating"
                      @click="deleteBothEdges"
                  >
                    Delete both edges
                  </button>
                </footer>
              </template>

              <div
                  v-else
                  class="
                  edit-box__state
                  edit-box__state--vertical
                  edge-editor__placeholder
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

                    <path d="M9 12h6" />
                    <path d="m12 8 4 4-4 4" />
                  </svg>
                </div>

                <div
                    class="
                    edit-box__state-content
                  "
                >
                  <strong
                      class="
                      edit-box__state-title
                    "
                  >
                    Select a neighbour
                  </strong>

                  <p
                      class="
                      edit-box__state-description
                    "
                  >
                    Choose a neighbouring location
                    to inspect its incoming and
                    outgoing edges.
                  </p>
                </div>
              </div>
            </main>
          </template>
        </SplitPanel>
      </template>
    </div>

    <ConnectLocationPrompt
        v-if="creatingNeighbour"
        :parent-location="
        model.parentLocation
      "
        :connectable-locations="
        connectableLocations
      "
        @select="createNeighbour"
        @close="
        creatingNeighbour = false
      "
    />
  </section>
</template>

<style scoped>
.location-edges-editor {
  width: 100%;
  min-width: 0;
}

.location-edges-editor
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

.location-edges-editor__message {
  min-height: auto;
  justify-content: flex-start;

  margin-bottom: var(--space-3);
  padding: var(--space-3);

  text-align: left;
}

.location-edges-editor__load-error {
  min-height: 20rem;
}

.location-edges-editor__statistics {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-2);

  margin-bottom: var(--space-3);
}

.location-edges-editor__split {
  width: 100%;
  min-width: 0;
  height: clamp(34rem, 72dvh, 56rem);

  overflow: hidden;

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-surface-raised) / 0.44),
          rgb(var(--c-surface-2) / 0.24)
      );

  border:
      1px solid
      rgb(var(--c-border) / 0.26);
  border-radius: var(--radius-md);
}

/* Neighbour browser */

.neighbour-browser {
  width: 100%;
  min-width: 0;
  height: 100%;

  display: flex;
  flex-direction: column;
  gap: var(--space-3);

  padding: var(--space-3);

  overflow: auto;
  overscroll-behavior: contain;

  background:
      rgb(var(--c-surface) / 0.22);
}

.neighbour-browser__source {
  flex: 0 0 auto;

  padding: var(--space-3);

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-accent) / 0.13),
          rgb(var(--c-surface-raised) / 0.62)
      );

  border:
      1px solid
      rgb(var(--c-accent) / 0.32);
  border-radius: var(--radius-md);
}

.neighbour-browser__source
.edit-box__eyebrow {
  margin-bottom: var(--space-1);
}

.neighbour-browser__source-name {
  display: block;

  overflow: hidden;

  color: rgb(var(--c-fg-strong));

  font-size: 0.9rem;
  font-weight: 800;

  text-overflow: ellipsis;
  white-space: nowrap;
}

.neighbour-browser__identifier,
.edge-editor__identifier {
  color: rgb(var(--c-muted));

  font-family: var(--font-monospace);
  font-size: 0.65rem;
  font-weight: 650;
}

.neighbour-browser__state {
  flex: 1 1 auto;
  min-height: 14rem;
}

.neighbour-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.neighbour-list__item {
  width: 100%;
  min-width: 0;

  display: grid;
  grid-template-columns:
      2.5rem
      minmax(0, 1fr);

  padding: 0;

  color: inherit;

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-surface-raised) / 0.58),
          rgb(var(--c-surface-2) / 0.34)
      );

  border:
      1px solid
      rgb(var(--c-border) / 0.28);
  border-radius: var(--radius-md);

  text-align: left;

  overflow: hidden;
  cursor: pointer;

  transition:
      border-color
      var(--duration-fast)
      var(--ease-standard),
      box-shadow
      var(--duration-fast)
      var(--ease-standard),
      transform
      var(--duration-fast)
      var(--ease-standard);
}

.neighbour-list__item:hover {
  border-color:
      rgb(var(--c-primary) / 0.48);

  box-shadow:
      0 5px 14px
      rgb(var(--c-shadow) / 0.065);

  transform: translateY(-1px);
}

.neighbour-list__item--selected {
  border-color:
      rgb(var(--c-accent) / 0.76);

  box-shadow:
      0 0 0
      var(--focus-ring-width)
      rgb(var(--focus-ring-color) / 0.11),
      0 6px 16px
      rgb(var(--c-shadow) / 0.07);
}

.neighbour-list__direction {
  display: grid;
  place-items: center;

  color: rgb(var(--c-primary-strong));

  background:
      rgb(var(--c-accent) / 0.12);

  border-right:
      1px solid
      rgb(var(--c-border) / 0.2);

  font-family: var(--font-monospace);
  font-size: 1.15rem;
  font-weight: 800;
}

.neighbour-list__direction--both {
  color: rgb(var(--c-success-strong));

  background:
      rgb(var(--c-success) / 0.11);
}

.neighbour-list__content {
  min-width: 0;

  display: flex;
  flex-direction: column;
  gap: var(--space-2);

  padding: var(--space-3);
}

.neighbour-list__heading {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--space-2);

  min-width: 0;
}

.neighbour-list__name {
  min-width: 0;

  overflow: hidden;

  color: rgb(var(--c-fg-strong));

  font-size: 0.82rem;
  font-weight: 800;

  text-overflow: ellipsis;
  white-space: nowrap;
}

.neighbour-list__badges {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-1);
}

.direction-badge {
  padding:
      0.18rem
      0.42rem;

  color: rgb(var(--c-muted));

  background:
      rgb(var(--c-muted) / 0.07);

  border:
      1px solid
      rgb(var(--c-muted) / 0.14);
  border-radius: var(--radius-round);

  font-size: 0.6rem;
  font-weight: 700;
}

/* Edge editor */

.edge-editor {
  width: 100%;
  min-width: 0;
  height: 100%;

  display: flex;
  flex-direction: column;
  gap: var(--space-3);

  padding: var(--space-4);

  overflow: auto;
  overscroll-behavior: contain;

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-surface-raised) / 0.34),
          rgb(var(--c-surface-2) / 0.18)
      );
}

.edge-editor__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);

  padding-bottom: var(--space-3);

  border-bottom:
      1px solid
      rgb(var(--c-border) / 0.24);
}

.edge-editor__header
.edit-box__eyebrow {
  margin-bottom: var(--space-1);
}

.edge-editor__title {
  margin: 0;

  color: rgb(var(--c-fg-strong));

  font-size: 1.05rem;
  font-weight: 800;
}

.edge-editor__directions {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.edge-editor__footer {
  display: flex;
  justify-content: flex-end;

  padding-top: var(--space-3);

  border-top:
      1px solid
      rgb(var(--c-border) / 0.24);
}

.edge-editor__placeholder {
  flex: 1 1 auto;
  min-height: 20rem;
}

/* Synchronization control */

.synchronization-toggle {
  position: relative;

  display: flex;
  align-items: center;
  gap: var(--space-3);

  padding:
      var(--space-2)
      var(--space-3);

  background:
      rgb(var(--c-surface-raised) / 0.5);

  border:
      1px solid
      rgb(var(--c-border) / 0.24);
  border-radius: var(--radius-md);

  cursor: pointer;
}

.synchronization-toggle--disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.synchronization-toggle__text {
  display: flex;
  flex-direction: column;
}

.synchronization-toggle__text strong {
  color: rgb(var(--c-fg-strong));

  font-size: 0.72rem;
  font-weight: 800;
}

.synchronization-toggle__text span {
  color: rgb(var(--c-muted));

  font-size: 0.62rem;
}

.synchronization-toggle input {
  position: absolute;

  width: 1px;
  height: 1px;

  opacity: 0;
}

.synchronization-toggle__switch {
  position: relative;

  width: 2.4rem;
  height: 1.3rem;
  flex: 0 0 auto;

  background:
      rgb(var(--c-muted) / 0.2);

  border:
      1px solid
      rgb(var(--c-border) / 0.34);
  border-radius: var(--radius-round);
}

.synchronization-toggle__switch::after {
  content: "";

  position: absolute;
  top: 50%;
  left: 0.16rem;

  width: 0.86rem;
  height: 0.86rem;

  background:
      rgb(var(--c-surface-raised));

  border-radius: 50%;

  box-shadow:
      0 1px 4px
      rgb(var(--c-shadow) / 0.2);

  transform: translateY(-50%);

  transition:
      left
      var(--duration-fast)
      var(--ease-standard);
}

.synchronization-toggle
input:checked +
.synchronization-toggle__switch {
  background:
      rgb(var(--c-accent) / 0.72);

  border-color:
      rgb(var(--c-accent));
}

.synchronization-toggle
input:checked +
.synchronization-toggle__switch::after {
  left: 1.22rem;
}

/* Direction editor */

.edge-direction {
  min-width: 0;

  padding: var(--space-4);

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-surface-raised) / 0.5),
          rgb(var(--c-surface-2) / 0.26)
      );

  border:
      1px solid
      rgb(var(--c-border) / 0.24);
  border-radius: var(--radius-md);

  box-shadow:
      inset 0 1px 0
      rgb(255 255 255 / 0.28),
      0 4px 12px
      rgb(var(--c-shadow) / 0.04);
}

.edge-direction--outgoing {
  border-left:
      3px solid
      rgb(var(--c-info) / 0.7);
}

.edge-direction--incoming {
  border-left:
      3px solid
      rgb(var(--c-success) / 0.7);
}

.edge-direction__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-3);

  margin-bottom: var(--space-4);
}

.edge-direction__heading {
  display: flex;
  align-items: center;
  gap: var(--space-3);

  min-width: 0;
}

.edge-direction__heading
.edit-box__eyebrow {
  margin-bottom: var(--space-1);
}

.edge-direction__icon {
  width: 2.25rem;
  height: 2.25rem;
  flex: 0 0 auto;

  display: grid;
  place-items: center;

  color: rgb(var(--c-primary-strong));

  background:
      rgb(var(--c-accent) / 0.12);

  border:
      1px solid
      rgb(var(--c-accent) / 0.24);
  border-radius: var(--radius-md);

  font-family: var(--font-monospace);
  font-size: 1.1rem;
  font-weight: 800;
}

.edge-direction__title {
  margin: 0;

  color: rgb(var(--c-fg-strong));

  font-size: 0.84rem;
  font-weight: 800;

  overflow-wrap: anywhere;
}

.edge-direction__fields {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.edge-direction__options {
  display: grid;
  grid-template-columns:
      repeat(
          auto-fit,
          minmax(11rem, 1fr)
      );

  gap: var(--space-3);
}

.edge-direction__empty {
  min-height: 10rem;
}

/* Responsive */

@media (max-width: 720px) {
  .location-edges-editor__split {
    height: 44rem;
  }

  .edge-editor {
    padding: var(--space-3);
  }

  .edge-editor__header {
    flex-direction: column;
  }
}

@media (max-width: 480px) {
  .edge-direction {
    padding: var(--space-3);
  }

  .edge-direction__header {
    flex-direction: column;
  }

  .edge-direction__header
  .edit-box__action {
    width: 100%;
  }

  .neighbour-list__heading {
    align-items: flex-start;
    flex-direction: column;
    gap: 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .neighbour-list__item,
  .synchronization-toggle__switch::after {
    transition: none;
  }

  .neighbour-list__item:hover {
    transform: none;
  }
}
</style>