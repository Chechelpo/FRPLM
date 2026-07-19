<script setup lang="ts">
import {
  computed,
  onMounted,
  ref,
  shallowRef,
} from "vue";

import {
  World,
  type WorldData,
  type WorldKey,
} from "@/domain/World";
import { EntityTypes } from "@/domain/EntityTypes";

import {
  createEntity,
  deleteEntity,
  fetch_all,

} from "@/core/ABSEntity";
import { API_BASE } from "@/config";

import WorldEdit from "@/components/space/WorldEdit.vue";
import SearchBar from "@/components/utils/SearchBar.vue";
import {fetchApi} from "@/services/apiClient";

interface WorldSummary {
  loading: boolean;
  locationCount: number | null;
  rootRegionCount: number | null;
  freeLocationCount: number | null;
  hasError: boolean;
}

const worlds = ref<World[]>([]);
const worldSummaries = ref<
    Record<string, WorldSummary>
>({});

const editingWorld =
    shallowRef<World | null>(null);

const searchTerm = ref("");

const loadingWorlds = ref(false);
const creatingWorld = ref(false);
const importing = ref(false);
const deletingWorldId =
    ref<number | null>(null);

const loadError = ref<string | null>(null);
const operationError =
    ref<string | null>(null);

const importInput =
    ref<HTMLInputElement | null>(null);

function getWorldKey(world: World): string {
  const id = world.get("id");

  return id == null
      ? String(world.hashKey())
      : String(id);
}

function getWorldName(world: World): string {
  const name = String(
      world.get("name") ?? "",
  ).trim();

  return name || "Unnamed world";
}

function getWorldId(world: World): string {
  const id = world.get("id");

  return id == null
      ? String(world.hashKey())
      : String(id);
}

function normalize(value: unknown): string {
  return String(value ?? "")
      .trim()
      .toLocaleLowerCase()
      .normalize("NFKD")
      .replace(/\p{Diacritic}/gu, "");
}

const filteredWorlds = computed<World[]>(
    () => {
      const query = normalize(searchTerm.value);

      return [...worlds.value]
          .filter((world) => {
            if (!query) {
              return true;
            }

            const name = normalize(
                getWorldName(world as World),
            );

            const id = normalize(
                getWorldId(world as World),
            );

            return (
                name.includes(query) ||
                id.includes(query)
            );
          })
          .sort((first, second) =>
              getWorldName(first as World).localeCompare(
                  getWorldName(second as World),
              ),
          ) as World[];
    },
);

const isFiltering = computed(
    () => searchTerm.value.trim().length > 0,
);

function getSummary(
    world: World,
): WorldSummary | undefined {
  return worldSummaries.value[
      getWorldKey(world)
      ];
}

function updateWorldSummary(
    world: World,
    summary: WorldSummary,
): void {
  worldSummaries.value = {
    ...worldSummaries.value,
    [getWorldKey(world)]: summary,
  };
}

async function loadWorldSummary(
    world: World,
): Promise<void> {
  updateWorldSummary(world, {
    loading: true,
    locationCount: null,
    rootRegionCount: null,
    freeLocationCount: null,
    hasError: false,
  });

  const [
    locationsResult,
    rootRegionsResult,
    freeLocationsResult,
  ] = await Promise.allSettled([
    world.getLocations(),
    world.getRootRegions(),
    world.getFreeLocations(),
  ]);

  updateWorldSummary(world, {
    loading: false,

    locationCount:
        locationsResult.status ===
        "fulfilled"
            ? locationsResult.value.length
            : null,

    rootRegionCount:
        rootRegionsResult.status ===
        "fulfilled"
            ? rootRegionsResult.value.length
            : null,

    freeLocationCount:
        freeLocationsResult.status ===
        "fulfilled"
            ? freeLocationsResult.value.length
            : null,

    hasError:
        locationsResult.status ===
        "rejected" ||
        rootRegionsResult.status ===
        "rejected" ||
        freeLocationsResult.status ===
        "rejected",
  });
}

async function loadWorlds(): Promise<void> {
  loadingWorlds.value = true;
  loadError.value = null;
  operationError.value = null;

  try {
    worlds.value = await fetch_all<
        WorldKey,
        WorldData,
        World
    >(
        EntityTypes.WORLDS,
        World,
    );

    await Promise.allSettled(
        worlds.value.map((world) =>
            loadWorldSummary(world as World),
        ),
    );
  } catch (error) {
    console.error(
        "Could not load worlds",
        error,
    );

    loadError.value =
        "The world collection could not be loaded.";
  } finally {
    loadingWorlds.value = false;
  }
}

async function onCreate(): Promise<void> {
  const inputName = window.prompt(
      "Enter new world name:",
  );

  if (!inputName?.trim()) {
    return;
  }

  creatingWorld.value = true;
  operationError.value = null;

  try {
    const newWorld = await createEntity<
        WorldKey,
        WorldData,
        World
    >(
        null,
        {
          name: inputName.trim(),
        },
        EntityTypes.WORLDS,
        World,
    );

    worlds.value = [
      ...worlds.value,
      newWorld,
    ];

    await loadWorldSummary(newWorld);

    openWorld(newWorld);
  } catch (error) {
    console.error(
        "Could not create world",
        error,
    );

    operationError.value =
        "The new world could not be created.";
  } finally {
    creatingWorld.value = false;
  }
}

function openWorld(world: World): void {
  editingWorld.value = world;
  operationError.value = null;
}

function closeWorldEditor(): void {
  editingWorld.value = null;
  operationError.value = null;
}

function openImportPicker(): void {
  operationError.value = null;
  importInput.value?.click();
}

async function onImportFileSelected(
    event: Event,
): Promise<void> {
  const input =
      event.target as HTMLInputElement;

  const file = input.files?.[0];

  if (!file) {
    return;
  }

  importing.value = true;
  operationError.value = null;

  try {
    const text = await file.text();

    // Validate the selected file before sending it.
    JSON.parse(text);

    const response = await fetchApi(
        `${API_BASE}/import/world`,
        {
          method: "POST",
          headers: {
            "Content-Type":
                "application/json",
          },
          body: text,
        },
    );

    if (!response.ok) {
      throw new Error(
          `Import failed with status ${response.status}`,
      );
    }

    const dto = await response.json();

    const importedWorld = new World(
        dto,
        EntityTypes.WORLDS,
    );

    worlds.value = [
      ...worlds.value,
      importedWorld,
    ];

    await loadWorldSummary(importedWorld);

    openWorld(importedWorld);
  } catch (error) {
    console.error(
        "Could not import world",
        error,
    );

    operationError.value =
        error instanceof SyntaxError
            ? "The selected file is not valid JSON."
            : "The world could not be imported.";
  } finally {
    importing.value = false;

    // Permit selecting the same file again.
    input.value = "";
  }
}

async function deleteWorld(
    world: World,
): Promise<void> {
  const worldName = getWorldName(world);

  const confirmed = window.confirm(
      `Delete ${worldName}? This action cannot be undone.`,
  );

  if (!confirmed) {
    return;
  }

  const worldId = world.get("id");

  if (worldId == null) {
    operationError.value =
        "This world does not have a valid identifier.";

    return;
  }

  deletingWorldId.value = worldId;
  operationError.value = null;

  try {
    const success =
        await deleteEntity<WorldKey>(
            world.key,
            EntityTypes.WORLDS,
        );

    if (!success) {
      operationError.value =
          `${worldName} could not be deleted.`;

      return;
    }

    worlds.value = worlds.value.filter(
        (candidate) =>
            !candidate.equals(world),
    );

    const nextSummaries = {
      ...worldSummaries.value,
    };

    delete nextSummaries[
        getWorldKey(world)
        ];

    worldSummaries.value =
        nextSummaries;

    if (
        editingWorld.value?.equals(world)
    ) {
      closeWorldEditor();
    }
  } catch (error) {
    console.error(
        "Could not delete world",
        error,
    );

    operationError.value =
        `${worldName} could not be deleted.`;
  } finally {
    deletingWorldId.value = null;
  }
}

function isDeleting(
    world: World,
): boolean {
  return (
      deletingWorldId.value !== null &&
      deletingWorldId.value ===
      world.get("id")
  );
}

onMounted(() => {
  void loadWorlds();
});
</script>

<template>
  <section
      class="world-manager"
      :aria-busy="
      loadingWorlds ||
      importing ||
      creatingWorld
    "
  >
    <input
        ref="importInput"
        type="file"
        class="world-manager__file-input"
        accept=".json,application/json"
        @change="onImportFileSelected"
    />

    <!-- World editor -->
    <template v-if="editingWorld">
      <header
          class="
          edit-box
          edit-box--primary
          edit-box--compact
          world-manager__editor-header
        "
      >
        <div class="edit-box__header">
          <div
              class="edit-box__header-icon"
              aria-hidden="true"
          >
            <svg viewBox="0 0 24 24">
              <circle
                  cx="12"
                  cy="12"
                  r="9"
              />

              <path
                  d="M3 12h18"
              />

              <path
                  d="M12 3a15 15 0 0 1 0 18"
              />

              <path
                  d="M12 3a15 15 0 0 0 0 18"
              />
            </svg>
          </div>

          <div class="edit-box__header-main">
            <span class="edit-box__eyebrow">
              World editor
            </span>

            <div class="edit-box__title-row">
              <h1 class="edit-box__title">
                {{ getWorldName(editingWorld) }}
              </h1>

              <span
                  class="
                  edit-box__badge
                  edit-box__badge--neutral
                "
              >
                ID {{ getWorldId(editingWorld) }}
              </span>
            </div>

            <p class="edit-box__description">
              Edit regions, locations, lore, and
              world structure.
            </p>
          </div>

          <div class="edit-box__actions">
            <button
                type="button"
                class="edit-box__action"
                @click="closeWorldEditor"
            >
              <svg
                  viewBox="0 0 24 24"
                  aria-hidden="true"
              >
                <path d="m15 18-6-6 6-6" />
              </svg>

              Worlds
            </button>

            <button
                type="button"
                class="
                edit-box__action
                edit-box__action--danger
              "
                :disabled="
                isDeleting(editingWorld)
              "
                @click="
                deleteWorld(editingWorld)
              "
            >
              <span
                  v-if="
                  isDeleting(editingWorld)
                "
                  class="edit-box__spinner"
                  aria-hidden="true"
              />

              <svg
                  v-else
                  viewBox="0 0 24 24"
                  aria-hidden="true"
              >
                <path d="M3 6h18" />
                <path d="M8 6V4h8v2" />
                <path
                    d="m19 6-1 14H6L5 6"
                />
                <path d="M10 11v5" />
                <path d="M14 11v5" />
              </svg>

              {{
                isDeleting(editingWorld)
                    ? "Deleting..."
                    : "Delete"
              }}
            </button>
          </div>
        </div>
      </header>

      <div
          v-if="operationError"
          class="
          edit-box__state
          edit-box__state--error
          world-manager__operation-error
        "
          role="alert"
      >
        <div class="edit-box__state-content">
          <strong class="edit-box__state-title">
            World operation failed
          </strong>

          <p class="edit-box__state-description">
            {{ operationError }}
          </p>
        </div>
      </div>

      <div class="world-manager__editor">
        <WorldEdit
            :model-value="editingWorld"
            @delete="
            deleteWorld(editingWorld)
          "
            @stop-editing="closeWorldEditor"
        />
      </div>
    </template>

    <!-- World library -->
    <template v-else>
      <header
          class="
          edit-box
          edit-box--accent
          world-manager__header
        "
      >
        <div class="edit-box__header">
          <div
              class="edit-box__header-icon"
              aria-hidden="true"
          >
            <svg viewBox="0 0 24 24">
              <circle
                  cx="12"
                  cy="12"
                  r="9"
              />

              <path d="M3 12h18" />

              <path
                  d="M12 3a15 15 0 0 1 0 18"
              />

              <path
                  d="M12 3a15 15 0 0 0 0 18"
              />
            </svg>
          </div>

          <div class="edit-box__header-main">
            <span class="edit-box__eyebrow">
              World library
            </span>

            <div class="edit-box__title-row">
              <h1 class="edit-box__title">
                Worlds
              </h1>

              <span class="edit-box__count">
                {{ worlds.length }}
              </span>
            </div>

            <p class="edit-box__description">
              Create, import, inspect, and edit
              narrative worlds.
            </p>
          </div>
        </div>

        <div
            class="
            edit-box__body
            world-manager__controls
          "
        >
          <div class="world-manager__search">
            <SearchBar
                v-model:search="searchTerm"
                placeholder="Search worlds by name or ID"
                aria-label="Search worlds by name or ID"
            />
          </div>

          <div
              class="
              world-manager__control-actions
            "
          >
            <button
                type="button"
                class="edit-box__action"
                :disabled="loadingWorlds"
                @click="loadWorlds"
            >
              <svg
                  viewBox="0 0 24 24"
                  aria-hidden="true"
              >
                <path
                    d="M20 11a8.1 8.1 0 0 0-15.5-2"
                />

                <path d="M4 4v5h5" />

                <path
                    d="M4 13a8.1 8.1 0 0 0 15.5 2"
                />

                <path d="M20 20v-5h-5" />
              </svg>

              Refresh
            </button>

            <button
                type="button"
                class="edit-box__action"
                :disabled="importing"
                @click="openImportPicker"
            >
              <span
                  v-if="importing"
                  class="edit-box__spinner"
                  aria-hidden="true"
              />

              <svg
                  v-else
                  viewBox="0 0 24 24"
                  aria-hidden="true"
              >
                <path d="M12 3v12" />
                <path d="m7 10 5 5 5-5" />
                <path
                    d="M5 21h14a2 2 0 0 0 2-2v-3"
                />
                <path
                    d="M3 16v3a2 2 0 0 0 2 2"
                />
              </svg>

              {{
                importing
                    ? "Importing..."
                    : "Import"
              }}
            </button>

            <button
                type="button"
                class="
                edit-box__action
                edit-box__action--accent
              "
                :disabled="creatingWorld"
                @click="onCreate"
            >
              <span
                  v-if="creatingWorld"
                  class="edit-box__spinner"
                  aria-hidden="true"
              />

              <svg
                  v-else
                  viewBox="0 0 24 24"
                  aria-hidden="true"
              >
                <path d="M12 5v14" />
                <path d="M5 12h14" />
              </svg>

              {{
                creatingWorld
                    ? "Creating..."
                    : "New world"
              }}
            </button>
          </div>
        </div>
      </header>

      <div
          v-if="operationError"
          class="
          edit-box__state
          edit-box__state--error
          world-manager__operation-error
        "
          role="alert"
      >
        <div class="edit-box__state-content">
          <strong class="edit-box__state-title">
            World operation failed
          </strong>

          <p class="edit-box__state-description">
            {{ operationError }}
          </p>
        </div>

        <button
            type="button"
            class="edit-box__action"
            @click="operationError = null"
        >
          Dismiss
        </button>
      </div>

      <div
          v-if="loadingWorlds"
          class="
          edit-box__state
          world-manager__state
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
            Loading worlds
          </strong>

          <p class="edit-box__state-description">
            Retrieving worlds and structural
            summaries.
          </p>
        </div>
      </div>

      <div
          v-else-if="loadError"
          class="
          edit-box__state
          edit-box__state--error
          edit-box__state--vertical
          world-manager__state
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
            Could not load worlds
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
            @click="loadWorlds"
        >
          Retry
        </button>
      </div>

      <div
          v-else-if="filteredWorlds.length"
          class="world-grid"
          aria-label="Available worlds"
      >
        <article
            v-for="world in filteredWorlds"
            :key="world.hashKey()"
            class="
            edit-box
            edit-box--primary
            edit-box--compact
            world-card
          "
        >
          <button
              type="button"
              class="world-card__main"
              @click="openWorld(world)"
          >
            <span
                class="world-card__icon"
                aria-hidden="true"
            >
              <svg viewBox="0 0 24 24">
                <circle
                    cx="12"
                    cy="12"
                    r="9"
                />

                <path d="M3 12h18" />

                <path
                    d="M12 3a15 15 0 0 1 0 18"
                />

                <path
                    d="M12 3a15 15 0 0 0 0 18"
                />
              </svg>
            </span>

            <span class="world-card__identity">
              <span class="world-card__name">
                {{ getWorldName(world) }}
              </span>

              <span
                  class="
                  world-card__identifier
                "
              >
                World ID {{ getWorldId(world) }}
              </span>
            </span>

            <svg
                class="world-card__open-icon"
                viewBox="0 0 24 24"
                aria-hidden="true"
            >
              <path d="m9 18 6-6-6-6" />
            </svg>
          </button>

          <div class="world-card__stats">
            <template
                v-if="
                getSummary(world)?.loading
              "
            >
              <div
                  class="
                  world-card__summary-loading
                "
              >
                <span
                    class="edit-box__spinner"
                    aria-hidden="true"
                />

                Loading structure...
              </div>
            </template>

            <template v-else>
              <div class="world-card__stat">
                <span
                    class="world-card__stat-value"
                >
                  {{
                    getSummary(world)
                        ?.locationCount ?? "—"
                  }}
                </span>

                <span
                    class="world-card__stat-label"
                >
                  Locations
                </span>
              </div>

              <div class="world-card__stat">
                <span
                    class="world-card__stat-value"
                >
                  {{
                    getSummary(world)
                        ?.rootRegionCount ?? "—"
                  }}
                </span>

                <span
                    class="world-card__stat-label"
                >
                  Root regions
                </span>
              </div>

              <div class="world-card__stat">
                <span
                    class="world-card__stat-value"
                >
                  {{
                    getSummary(world)
                        ?.freeLocationCount ?? "—"
                  }}
                </span>

                <span
                    class="world-card__stat-label"
                >
                  Ungrouped
                </span>
              </div>
            </template>
          </div>

          <footer class="world-card__footer">
            <div class="world-card__badges">
              <span
                  class="
                  edit-box__badge
                  edit-box__badge--neutral
                "
              >
                Lorebook
                {{ world.get("lorebook_id") }}
              </span>

              <span
                  v-if="
                  getSummary(world)?.hasError
                "
                  class="
                  edit-box__badge
                  edit-box__badge--warning
                "
                  title="Some world statistics could not be loaded."
              >
                Partial data
              </span>
            </div>

            <div class="world-card__actions">
              <button
                  type="button"
                  class="edit-box__action"
                  @click="openWorld(world)"
              >
                Edit
              </button>

              <button
                  type="button"
                  class="
                  edit-box__action
                  edit-box__action--danger
                  world-card__delete
                "
                  :disabled="isDeleting(world)"
                  :aria-label="
                  `Delete ${getWorldName(world)}`
                "
                  :title="
                  `Delete ${getWorldName(world)}`
                "
                  @click="deleteWorld(world)"
              >
                <span
                    v-if="isDeleting(world)"
                    class="edit-box__spinner"
                    aria-hidden="true"
                />

                <svg
                    v-else
                    viewBox="0 0 24 24"
                    aria-hidden="true"
                >
                  <path d="M3 6h18" />
                  <path d="M8 6V4h8v2" />
                  <path
                      d="m19 6-1 14H6L5 6"
                  />
                  <path d="M10 11v5" />
                  <path d="M14 11v5" />
                </svg>
              </button>
            </div>
          </footer>
        </article>
      </div>

      <div
          v-else-if="isFiltering"
          class="
          edit-box__state
          edit-box__state--vertical
          world-manager__state
        "
      >
        <div class="edit-box__state-icon">
          <svg
              viewBox="0 0 24 24"
              aria-hidden="true"
          >
            <circle
                cx="11"
                cy="11"
                r="7"
            />

            <path d="m20 20-4-4" />
          </svg>
        </div>

        <div class="edit-box__state-content">
          <strong class="edit-box__state-title">
            No matching worlds
          </strong>

          <p class="edit-box__state-description">
            No world matches
            “{{ searchTerm.trim() }}”.
          </p>
        </div>

        <button
            type="button"
            class="edit-box__action"
            @click="searchTerm = ''"
        >
          Clear search
        </button>
      </div>

      <div
          v-else
          class="
          edit-box__state
          edit-box__state--vertical
          world-manager__state
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

            <path d="M3 12h18" />

            <path
                d="M12 3a15 15 0 0 1 0 18"
            />

            <path
                d="M12 3a15 15 0 0 0 0 18"
            />
          </svg>
        </div>

        <div class="edit-box__state-content">
          <strong class="edit-box__state-title">
            No worlds
          </strong>

          <p class="edit-box__state-description">
            Create a new world or import one from
            a JSON file.
          </p>
        </div>

        <div class="world-manager__empty-actions">
          <button
              type="button"
              class="edit-box__action"
              @click="openImportPicker"
          >
            Import world
          </button>

          <button
              type="button"
              class="
              edit-box__action
              edit-box__action--accent
            "
              @click="onCreate"
          >
            Create world
          </button>
        </div>
      </div>
    </template>
  </section>
</template>

<style scoped>
.world-manager {
  width: min(100%, 96rem);
  min-width: 0;
  min-height: 100%;
  box-sizing: border-box;

  display: flex;
  flex-direction: column;
  gap: var(--space-4);

  margin: 0 auto;
  padding: var(--space-4);

  color: rgb(var(--c-fg));
  font-family: var(--font-primary);
}

.world-manager__file-input {
  display: none;
}

/* -------------------------------------------------------------------------- */
/* Header and controls                                                        */
/* -------------------------------------------------------------------------- */

.world-manager__header,
.world-manager__editor-header {
  flex: 0 0 auto;
}

.world-manager__controls {
  display: flex;
  align-items: center;
  gap: var(--space-3);

  padding-top: var(--space-3);
}

.world-manager__search {
  flex: 1 1 22rem;
  min-width: 12rem;
}

.world-manager__control-actions {
  flex: 0 0 auto;

  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.world-manager__control-actions
.edit-box__action
svg,
.world-manager__editor-header
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

.world-manager__operation-error {
  min-height: auto;
  justify-content: flex-start;

  padding: var(--space-3);

  text-align: left;
}

.world-manager__state {
  min-height: 18rem;
}

.world-manager__empty-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  justify-content: center;
  gap: var(--space-2);
}

/* -------------------------------------------------------------------------- */
/* World grid                                                                 */
/* -------------------------------------------------------------------------- */

.world-grid {
  display: grid;
  grid-template-columns:
    repeat(
      auto-fill,
      minmax(min(100%, 19rem), 1fr)
    );

  gap: var(--space-4);

  min-width: 0;
}

.world-card {
  min-width: 0;

  display: flex;
  flex-direction: column;

  overflow: hidden;
}

.world-card__main {
  width: 100%;
  min-width: 0;

  display: flex;
  align-items: center;
  gap: var(--space-3);

  padding: var(--space-4);

  color: inherit;

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-surface-raised) / 0.54),
          rgb(var(--c-surface-2) / 0.28)
      );

  border: 0;
  border-bottom:
      1px solid
      rgb(var(--c-border) / 0.22);
  outline: 0;

  font: inherit;
  text-align: left;

  cursor: pointer;

  transition:
      background-color
      var(--duration-fast)
      var(--ease-standard);
}

.world-card__main:hover {
  background:
      linear-gradient(
          145deg,
          rgb(var(--c-surface-hover) / 0.82),
          rgb(var(--c-accent) / 0.08)
      );
}

.world-card__main:focus-visible {
  box-shadow:
      inset 0 0 0
      var(--focus-ring-width)
      rgb(var(--focus-ring-color) / 0.28);
}

.world-card__icon {
  width: 2.75rem;
  height: 2.75rem;
  flex: 0 0 auto;

  display: grid;
  place-items: center;

  color: rgb(var(--c-primary-strong));

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-accent) / 0.2),
          rgb(var(--c-primary) / 0.09)
      );

  border:
      1px solid
      rgb(var(--c-accent) / 0.3);
  border-radius: var(--radius-md);

  box-shadow:
      inset 0 1px 0
      rgb(255 255 255 / 0.34);
}

.world-card__icon svg {
  width: 1.4rem;
  height: 1.4rem;

  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.world-card__identity {
  flex: 1 1 auto;
  min-width: 0;

  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.world-card__name {
  overflow: hidden;

  color: rgb(var(--c-fg-strong));

  font-size: 0.95rem;
  font-weight: 800;
  line-height: 1.3;

  text-overflow: ellipsis;
  white-space: nowrap;
}

.world-card__identifier {
  color: rgb(var(--c-muted));

  font-family: var(--font-monospace);
  font-size: 0.68rem;
  font-weight: 600;
  line-height: 1.3;
}

.world-card__open-icon {
  width: 1rem;
  height: 1rem;
  flex: 0 0 auto;

  color: rgb(var(--c-muted));

  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

/* -------------------------------------------------------------------------- */
/* Statistics                                                                 */
/* -------------------------------------------------------------------------- */

.world-card__stats {
  display: grid;
  grid-template-columns:
    repeat(3, minmax(0, 1fr));

  min-height: 4.8rem;

  background:
      rgb(var(--c-surface) / 0.22);

  border-bottom:
      1px solid
      rgb(var(--c-border) / 0.2);
}

.world-card__stat {
  min-width: 0;

  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 0.15rem;

  padding: var(--space-2);

  text-align: center;
}

.world-card__stat,
+ .world-card__stat {
  border-left:
      1px solid
      rgb(var(--c-border) / 0.2);
}

.world-card__stat-value {
  color: rgb(var(--c-fg-strong));

  font-size: 1rem;
  font-weight: 850;
  line-height: 1.2;

  font-variant-numeric: tabular-nums;
}

.world-card__stat-label {
  color: rgb(var(--c-muted));

  font-size: 0.64rem;
  font-weight: 700;
  line-height: 1.25;

  text-transform: uppercase;
  letter-spacing: 0.045em;
}

.world-card__summary-loading {
  grid-column: 1 / -1;

  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);

  color: rgb(var(--c-muted));

  font-size: 0.75rem;
  line-height: 1.4;
}

/* -------------------------------------------------------------------------- */
/* Card footer                                                                */
/* -------------------------------------------------------------------------- */

.world-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);

  margin-top: auto;
  padding: var(--space-2);

  background:
      rgb(var(--c-surface-2) / 0.24);
}

.world-card__badges {
  min-width: 0;

  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-1);
}

.world-card__actions {
  flex: 0 0 auto;

  display: flex;
  align-items: center;
  gap: var(--space-1);
}

.world-card__actions
.edit-box__action {
  min-height: 2rem;

  padding:
      0.35rem
      0.6rem;
}

.world-card__delete {
  width: 2rem;
  padding: 0;
}

.world-card__delete svg {
  width: 0.95rem;
  height: 0.95rem;

  fill: none;
  stroke: currentColor;
  stroke-width: 1.9;
  stroke-linecap: round;
  stroke-linejoin: round;
}

/* -------------------------------------------------------------------------- */
/* Editor                                                                     */
/* -------------------------------------------------------------------------- */

.world-manager__editor {
  width: 100%;
  min-width: 0;
  box-sizing: border-box;
}

/* -------------------------------------------------------------------------- */
/* Responsive                                                                 */
/* -------------------------------------------------------------------------- */

@media (max-width: 760px) {
  .world-manager {
    padding: var(--space-3);
  }

  .world-manager__controls {
    align-items: stretch;
    flex-direction: column;
  }

  .world-manager__search {
    flex-basis: auto;
    width: 100%;
  }

  .world-manager__control-actions {
    width: 100%;
  }
}

@media (max-width: 520px) {
  .world-manager {
    padding: var(--space-2);
  }

  .world-manager__control-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .world-manager__control-actions
  .edit-box__action {
    width: 100%;
  }

  .world-card__footer {
    align-items: stretch;
    flex-direction: column;
  }

  .world-card__actions {
    justify-content: flex-end;
  }
}

@media (prefers-reduced-motion: reduce) {
  .world-card__main {
    transition: none;
  }
}
</style>