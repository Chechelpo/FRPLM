<script setup lang="ts">
import {
  computed,
  onMounted,
  ref,
} from "vue";
import {computedAsync} from "@vueuse/core";

import {
  World,
  Region,
  type RegionData,
  type RegionKey,
} from "@/domain/World";
import {Lorebook} from "@/domain/Lorebook";
import {EntityTypes} from "@/domain/EntityTypes";

import {API_BASE} from "@/config";
import {
  createEntity,

} from "@/core/ABSEntity";

import ShortTextBox from "@/components/utils/primitiveEditors/ShortTextBox.vue";
import LorebookEditor from "@/components/lorebooks/LorebookEditor.vue";
import Expandable from "@/components/utils/panels/Expandable.vue";
import IconButton from "@/components/utils/buttons/IconButton.vue";
import SearchBar from "@/components/utils/SearchBar.vue";
import FieldEditorWrapper from "@/components/utils/FieldEditorWrapper.vue";
import RegionEditor from "@/components/space/RegionEditor.vue";
import WorldLocationGraph from "@/components/space/WorldLocationGraph.vue";
import LongTextBox from "@/components/utils/primitiveEditors/LongTextBox.vue";
import {fetchApi} from "@/services/apiClient";

const model = defineModel<World>({
  required: true,
  type: World,
});

/* -------------------------------------------------------------------------- */
/* World information                                                         */
/* -------------------------------------------------------------------------- */

const lorebook = computedAsync<Lorebook | undefined>(
    async () => model.value.getLorebook(),
    undefined,
);

const worldName = computed(() =>
    String(
        model.value.get("name") ??
        "Unnamed world",
    ),
);

const worldId = computed(() =>
    String(model.value.get("id") ?? "—"),
);

const worldLorebookId = computed(() =>
    String(
        model.value.get("lorebook_id") ??
        "—",
    ),
);

const exportingWorld = ref(false);
const exportError = ref<string | null>(null);

async function onExportWorld(): Promise<void> {
  exportingWorld.value = true;
  exportError.value = null;

  try {
    const response = await fetchApi(
        `api/import/world?worldId=${model.value.get("id")}`,
        {
          method: "GET",
          headers: {
            accept: "application/json",
          },
        },
    );

    if (!response.ok) {
      throw new Error(
          `Export failed with status ${response.status}`,
      );
    }

    const json = await response.json();

    const blob = new Blob(
        [JSON.stringify(json, null, 2)],
        {
          type: "application/json",
        },
    );

    const url = URL.createObjectURL(blob);
    const link =
        document.createElement("a");

    const safeName = worldName.value
        .replace(/[<>:"/\\|?*\u0000-\u001F]/g, "_")
        .trim();

    link.href = url;
    link.download =
        `${safeName || "world"}.json`;

    document.body.appendChild(link);
    link.click();
    link.remove();

    URL.revokeObjectURL(url);
  } catch (error) {
    console.error(
        "Failed to export world:",
        error,
    );

    exportError.value =
        "The world could not be exported.";
  } finally {
    exportingWorld.value = false;
  }
}

/* -------------------------------------------------------------------------- */
/* Location graph                                                             */
/* -------------------------------------------------------------------------- */

const graphOpen = ref(false);

function onGraphStatusChange(
    isOpen: boolean,
): void {
  graphOpen.value = isOpen;
}

/* -------------------------------------------------------------------------- */
/* Regions                                                                    */
/* -------------------------------------------------------------------------- */

const regions = ref<Region[]>([]);
const isLoadingRegions = ref(false);
const creatingRegion = ref(false);
const regionLoadError =
    ref<string | null>(null);

const regionFilteringTerm = ref("");

const filteredRegions = computed<
    Region[]
>(() => {
  const query =
      regionFilteringTerm.value
          .trim()
          .toLocaleLowerCase();

  if (!query) {
    return regions.value as Region[];
  }

  return regions.value.filter(
      (region) => {
        const name = String(
            region.get("name") ?? "",
        );

        return name
            .toLocaleLowerCase()
            .includes(query);
      },
  ) as Region[];
});

const regionCountLabel = computed(() => {
  if (
      !regionFilteringTerm.value.trim()
  ) {
    return String(regions.value.length);
  }

  return (
      `${filteredRegions.value.length}` +
      ` of ${regions.value.length}`
  );
});

async function createRootRegion(): Promise<void> {
  const name = window.prompt(
      "Enter new region name:",
  );

  if (!name?.trim()) {
    return;
  }

  creatingRegion.value = true;

  try {
    const newRegion =
        await createEntity<
            RegionKey,
            RegionData,
            Region
        >(
            {
              world_id:
                  model.value.get("id"),
            },
            {
              name: name.trim(),
            },
            EntityTypes.REGIONS,
            Region,
        );

    if (!newRegion) {
      return;
    }

    regions.value = [
      ...regions.value,
      newRegion,
    ];
  } finally {
    creatingRegion.value = false;
  }
}

function clearRegionFilter(): void {
  regionFilteringTerm.value = "";
}

async function loadRegions(): Promise<void> {
  isLoadingRegions.value = true;
  regionLoadError.value = null;

  try {
    regions.value =
        await model.value.getRootRegions();
  } catch (error) {
    console.error(
        "Failed to load root regions:",
        error,
    );

    regionLoadError.value =
        error instanceof Error
            ? error.message
            : "The root regions could not be loaded.";
  } finally {
    isLoadingRegions.value = false;
  }
}

onMounted(() => {
  void loadRegions();
});
</script>

<template>
  <main class="world-editor">
    <div class="world-editor__content">
      <!-- World information -->
      <section class="edit-box edit-box--primary world-information">
        <header class="edit-box__header">
          <div class="edit-box__header-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24">
              <circle cx="12" cy="12" r="9"/>
              <path d="M3 12h18"/>
              <path d="M12 3a15 15 0 0 1 0 18"/>
              <path d="M12 3a15 15 0 0 0 0 18"/>
            </svg>
          </div>

          <div class="edit-box__header-main">
            <span class="edit-box__eyebrow">
              World information
            </span>

            <div class="edit-box__title-row">
              <h1 class="edit-box__title">
                {{ worldName }}
              </h1>

              <span class="edit-box__badge edit-box__badge--neutral">
                ID {{ worldId }}
              </span>
            </div>

            <p class="edit-box__description">
              Edit global world metadata and context shared across every region
              and location.
            </p>
          </div>

          <div class="edit-box__actions">
            <button
                type="button"
                class="edit-box__action edit-box__action--accent"
                :disabled="exportingWorld"
                @click="onExportWorld"
            >
              <span
                  v-if="exportingWorld"
                  class="edit-box__spinner"
                  aria-hidden="true"
              />

              <svg v-else viewBox="0 0 24 24" aria-hidden="true">
                <path d="M12 3v12"/>
                <path d="m7 10 5 5 5-5"/>
                <path d="M5 21h14"/>
              </svg>

              {{ exportingWorld ? "Exporting..." : "Export JSON" }}
            </button>
          </div>
        </header>

        <div class="edit-box__body edit-box__stack">
          <div
              v-if="exportError"
              class="edit-box__state edit-box__state--error world-information__error"
              role="alert"
          >
            <div class="edit-box__state-content">
              <strong class="edit-box__state-title">
                Export failed
              </strong>

              <p class="edit-box__state-description">
                {{ exportError }}
              </p>
            </div>

            <button
                type="button"
                class="edit-box__action"
                @click="exportError = null"
            >
              Dismiss
            </button>
          </div>

          <section class="edit-box__section world-information__identity">
            <header class="edit-box__section-header">
              <div class="edit-box__section-heading">
                <h2 class="edit-box__section-title">
                  Identity
                </h2>

                <p class="edit-box__section-description">
                  Define the world's name and global description used
                  throughout generated prompts and exported data.
                </p>
              </div>
            </header>

            <div class="world-information__identity-fields">
              <div class="world-information__field">
                <FieldEditorWrapper
                    field-name="Name"
                    info="The world's display name."
                    :vertical="true"
                >
                  <ShortTextBox
                      :model-value="model.get('name')"
                      aria-label="World name"
                      @edit="payload => model.update('name', payload)"
                  />
                </FieldEditorWrapper>
              </div>

              <div class="world-information__field world-information__field--description">
                <FieldEditorWrapper
                    field-name="Description"
                    info="Global world context available throughout generated prompts."
                    :vertical="true"
                >
                  <LongTextBox
                      :model-value="model.get('description')"
                      aria-label="World description"
                      @edit="payload => model.update('description', payload)"
                      tokenize
                      :tokenization-started="true"
                  />
                </FieldEditorWrapper>
              </div>
            </div>

          </section>

          <section
              class="edit-box__section edit-box__section--accent world-information__lorebook-section"
          >
            <Expandable
                title="World lorebook"
                info="Lore available throughout the entire world, independently of the active region."
                :initially-open="false"
            >
              <div class="world-information__lorebook-body">
                <LorebookEditor
                    v-if="lorebook"
                    class="world-information__lorebook"
                    :model-value="lorebook"
                />

                <div
                    v-else
                    class="edit-box__state"
                    aria-live="polite"
                >
                  <span class="edit-box__spinner" aria-hidden="true"/>

                  <div class="edit-box__state-content">
                    <strong class="edit-box__state-title">
                      Loading world lorebook
                    </strong>

                    <p class="edit-box__state-description">
                      Retrieving the lorebook associated with this world.
                    </p>
                  </div>
                </div>
              </div>
            </Expandable>
          </section>
        </div>
      </section>

      <!-- Experimental location graph -->
      <section class="edit-box edit-box--info world-editor__graph-section">
        <Expandable
            title="Location graph"
            info="Experimental read-only visualization of all discovered location connections."
            :initially-open="false"
            @status-change="onGraphStatusChange"
        >
          <div class="world-editor__graph-body">
            <WorldLocationGraph
                v-if="graphOpen"
                :world="model"
            />

            <div
                v-else
                class="edit-box__state edit-box__state--vertical world-editor__graph-placeholder"
            >
              <div class="edit-box__state-icon">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <circle cx="5" cy="6" r="2"/>
                  <circle cx="19" cy="7" r="2"/>
                  <circle cx="8" cy="18" r="2"/>
                  <circle cx="18" cy="17" r="2"/>
                  <path d="m7 7 10 0"/>
                  <path d="m6 8 2 8"/>
                  <path d="m10 17 6 0"/>
                  <path d="m18 9 0 6"/>
                </svg>
              </div>

              <div class="edit-box__state-content">
                <strong class="edit-box__state-title">
                  Location graph is closed
                </strong>

                <p class="edit-box__state-description">
                  Expand this section to discover and visualize location
                  connections.
                </p>
              </div>
            </div>
          </div>
        </Expandable>
      </section>

      <!-- Region hierarchy -->
      <section class="edit-box edit-box--accent">
        <header class="edit-box__header">
          <div class="edit-box__header-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24">
              <path d="M12 3v6"/>
              <path d="M6 21v-4"/>
              <path d="M18 21v-4"/>
              <path d="M6 13h12"/>
              <path d="M6 13a2 2 0 1 0 0 4"/>
              <path d="M18 13a2 2 0 1 1 0 4"/>
              <path d="M12 9a2 2 0 1 0 0 4"/>
            </svg>
          </div>

          <div class="edit-box__header-main">
            <span class="edit-box__eyebrow">
              Spatial hierarchy
            </span>

            <div class="edit-box__title-row">
              <h2 class="edit-box__title">
                Regions
              </h2>

              <span class="edit-box__count">
                {{ regionCountLabel }}
              </span>
            </div>

            <p class="edit-box__description">
              Root regions form the top level of the world's spatial tree.
              Locations are managed inside their corresponding regions.
            </p>
          </div>

          <div class="edit-box__actions">
            <IconButton
                title="Create root region"
                variant="accent"
                :disabled="creatingRegion"
                @click="createRootRegion"
            >
              <span
                  v-if="creatingRegion"
                  class="edit-box__spinner"
                  aria-hidden="true"
              />

              <svg v-else viewBox="0 0 24 24" aria-hidden="true">
                <path d="M12 5v14"/>
                <path d="M5 12h14"/>
              </svg>
            </IconButton>
          </div>
        </header>

        <div class="edit-box__body">
          <div class="edit-box__toolbar">
            <div class="edit-box__toolbar-main">
              <SearchBar
                  v-model:search="regionFilteringTerm"
                  placeholder="Filter root regions by name"
                  aria-label="Filter root regions by name"
              />
            </div>

            <div class="edit-box__toolbar-actions">
              <button
                  v-if="regionFilteringTerm.trim()"
                  class="edit-box__action"
                  type="button"
                  @click="clearRegionFilter"
              >
                Clear filter
              </button>

              <button
                  class="edit-box__action"
                  type="button"
                  :disabled="isLoadingRegions"
                  @click="loadRegions"
              >
                Refresh
              </button>
            </div>
          </div>

          <div
              v-if="isLoadingRegions"
              class="edit-box__state"
              aria-live="polite"
          >
            <span class="edit-box__spinner" aria-hidden="true"/>

            <div class="edit-box__state-content">
              <strong class="edit-box__state-title">
                Loading regions
              </strong>

              <p class="edit-box__state-description">
                Retrieving the world region hierarchy.
              </p>
            </div>
          </div>

          <div
              v-else-if="regionLoadError"
              class="edit-box__state edit-box__state--error edit-box__state--vertical"
              role="alert"
          >
            <div class="edit-box__state-icon">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M12 9v4"/>
                <path d="M12 17h.01"/>
                <path
                    d="M10.3 3.8 2.2 18a2 2 0 0 0 1.8 3h16a2 2 0 0 0 1.8-3L13.7 3.8a2 2 0 0 0-3.4 0Z"
                />
              </svg>
            </div>

            <div class="edit-box__state-content">
              <strong class="edit-box__state-title">
                Unable to load regions
              </strong>

              <p class="edit-box__state-description">
                {{ regionLoadError }}
              </p>
            </div>

            <button
                class="edit-box__action edit-box__action--danger"
                type="button"
                @click="loadRegions"
            >
              Retry
            </button>
          </div>

          <ul
              v-else-if="filteredRegions.length"
              class="region-tree"
              role="tree"
              aria-label="World regions"
          >
            <li
                v-for="region in filteredRegions"
                :key="region.hashKey()"
                class="region-tree__item"
                role="treeitem"
            >
              <div class="region-tree__marker" aria-hidden="true"/>

              <div class="region-tree__node">
                <RegionEditor :region="region"/>
              </div>
            </li>
          </ul>

          <div
              v-else-if="regionFilteringTerm.trim()"
              class="edit-box__state edit-box__state--vertical"
          >
            <div class="edit-box__state-icon">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <circle cx="11" cy="11" r="7"/>
                <path d="m20 20-4-4"/>
              </svg>
            </div>

            <div class="edit-box__state-content">
              <strong class="edit-box__state-title">
                No matching regions
              </strong>

              <p class="edit-box__state-description">
                No root region contains
                <strong class="world-editor__query">
                  “{{ regionFilteringTerm.trim() }}”
                </strong>
                in its name.
              </p>
            </div>

            <button
                class="edit-box__action"
                type="button"
                @click="clearRegionFilter"
            >
              Clear filter
            </button>
          </div>

          <div
              v-else
              class="edit-box__state edit-box__state--vertical"
          >
            <div class="edit-box__state-icon">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M4 19V8l8-4 8 4v11"/>
                <path d="M8 19v-5h8v5"/>
              </svg>
            </div>

            <div class="edit-box__state-content">
              <strong class="edit-box__state-title">
                No root regions
              </strong>

              <p class="edit-box__state-description">
                Create a root region to begin constructing the world's spatial
                hierarchy.
              </p>
            </div>

            <button
                class="edit-box__action edit-box__action--accent"
                type="button"
                :disabled="creatingRegion"
                @click="createRootRegion"
            >
              {{ creatingRegion ? "Creating..." : "Create first region" }}
            </button>
          </div>
        </div>
      </section>
    </div>
  </main>
</template>

<style scoped>
.world-editor {
  width: 100%;
  min-width: 0;
  min-height: 100%;
  box-sizing: border-box;
  padding: var(--space-3);
  color: rgb(var(--c-fg));
  font-family: var(--font-primary);
}

.world-editor__content {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  width: 100%;
  min-width: 0;
}

.world-editor .edit-box__action svg {
  width: 1rem;
  height: 1rem;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.9;
  stroke-linecap: round;
  stroke-linejoin: round;
}

/* -------------------------------------------------------------------------- */
/* World information                                                         */
/* -------------------------------------------------------------------------- */

.world-information {
  width: 100%;
  min-width: 0;
}

.world-information__error {
  min-height: auto;
  justify-content: flex-start;
  padding: var(--space-3);
  text-align: left;
}

.world-information__identity {
  min-width: 0;
}

.world-information__identity-grid {
  display: grid;
  grid-template-columns: minmax(20rem, 1.6fr) minmax(14rem, 1fr);
  align-items: start;
  gap: var(--space-4);
  min-width: 0;
}

.world-information__identity-fields {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  min-width: 0;
}

.world-information__field {
  min-width: 0;
  padding: var(--space-3);

  background: linear-gradient(
      145deg,
      rgb(var(--c-surface-raised) / 0.48),
      rgb(var(--c-surface-2) / 0.24)
  );

  border: 1px solid rgb(var(--c-border) / 0.19);
  border-radius: var(--radius-md);

  box-shadow: inset 0 1px 0 rgb(255 255 255 / 0.28),
  0 3px 9px rgb(var(--c-shadow) / 0.035);

  transition: background-color var(--duration-normal) var(--ease-standard),
  border-color var(--duration-normal) var(--ease-standard),
  box-shadow var(--duration-normal) var(--ease-standard);
}

.world-information__field:hover {
  border-color: rgb(var(--c-primary) / 0.27);
}

.world-information__field:focus-within {
  background: rgb(var(--c-surface-raised) / 0.62);
  border-color: rgb(var(--c-primary) / 0.42);

  box-shadow: 0 0 0 3px rgb(var(--c-primary) / 0.09),
  inset 0 1px 0 rgb(255 255 255 / 0.32);
}

.world-information__field--description {
  border-color: rgb(var(--c-primary) / 0.2);

  background: linear-gradient(
      145deg,
      rgb(var(--c-primary) / 0.055),
      rgb(var(--c-surface-raised) / 0.5)
  );
}

.world-information__field :deep(input),
.world-information__field :deep(textarea) {
  width: 100%;
  min-width: 0;
  box-sizing: border-box;
}

.world-information__field :deep(textarea) {
  min-height: 8rem;
  resize: vertical;
}

.world-information__metadata {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(6.5rem, 1fr));
  gap: var(--space-2);
  min-width: 0;
  margin: 0;
}

.world-information__metadata-item {
  min-width: 0;
  padding: var(--space-3);

  background: linear-gradient(
      145deg,
      rgb(var(--c-surface-raised) / 0.56),
      rgb(var(--c-surface-2) / 0.28)
  );

  border: 1px solid rgb(var(--c-border) / 0.22);
  border-radius: var(--radius-sm);
  box-shadow: inset 0 1px 0 rgb(255 255 255 / 0.26);
}

.world-information__metadata-item dt {
  margin: 0 0 var(--space-1);
  color: rgb(var(--c-muted));
  font-size: 0.64rem;
  font-weight: 800;
  line-height: 1.2;
  text-transform: uppercase;
  letter-spacing: 0.055em;
}

.world-information__metadata-item dd {
  min-width: 0;
  margin: 0;
  overflow: hidden;
  color: rgb(var(--c-fg-strong));
  font-family: var(--font-monospace);
  font-size: 0.82rem;
  font-weight: 750;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.world-information__lorebook-section {
  padding: 0;
  overflow: hidden;
}

.world-information__lorebook-body {
  min-width: 0;
  padding: var(--space-3);
}

.world-information__lorebook {
  width: 100%;
  min-width: 0;
}

/* -------------------------------------------------------------------------- */
/* Location graph                                                             */
/* -------------------------------------------------------------------------- */

.world-editor__graph-section {
  width: 100%;
  min-width: 0;
  overflow: hidden;
}

.world-editor__graph-body {
  min-width: 0;
  padding: var(--space-3);
}

.world-editor__graph-placeholder {
  min-height: 10rem;
}

/* -------------------------------------------------------------------------- */
/* Region tree                                                                */
/* -------------------------------------------------------------------------- */

.world-editor__query {
  color: rgb(var(--c-primary-strong));
  font-weight: 800;
}

.region-tree {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  min-width: 0;
  margin: 0;
  padding: var(--space-1) 0 var(--space-1) 1.75rem;
  list-style: none;
}

.region-tree::before {
  content: "";
  position: absolute;
  top: var(--space-1);
  bottom: 1.6rem;
  left: 0.55rem;
  width: 2px;

  background: linear-gradient(
      to bottom,
      rgb(var(--c-primary) / 0.68),
      rgb(var(--c-primary) / 0.1)
  );

  border-radius: var(--radius-round);
}

.region-tree__item {
  position: relative;
  min-width: 0;
}

.region-tree__item::before {
  content: "";
  position: absolute;
  top: 1.45rem;
  left: -1.2rem;
  width: 1.2rem;
  height: 2px;
  background: rgb(var(--c-primary) / 0.55);
}

.region-tree__marker {
  position: absolute;
  top: 1.15rem;
  left: -0.22rem;
  z-index: 1;
  width: 0.55rem;
  height: 0.55rem;
  box-sizing: border-box;
  background: rgb(var(--c-accent));
  border: 2px solid rgb(var(--c-primary));
  border-radius: 50%;
  box-shadow: 0 0 0 3px rgb(var(--c-accent) / 0.13);
  transform: translateX(-100%);
}

.region-tree__node {
  min-width: 0;

  transition: transform var(--duration-normal) var(--ease-standard);
}

.region-tree__node:hover {
  transform: translateY(-1px);
}

/* -------------------------------------------------------------------------- */
/* Responsive                                                                 */
/* -------------------------------------------------------------------------- */

@media (max-width: 800px) {
  .world-information__identity-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 700px) {
  .world-editor {
    padding: var(--space-2);
  }

  .world-editor__graph-body,
  .world-information__lorebook-body {
    padding: var(--space-2);
  }

  .world-information__field {
    padding: var(--space-2);
  }

  .region-tree {
    padding-left: 1.3rem;
  }

  .region-tree::before {
    left: 0.35rem;
  }

  .region-tree__item::before {
    left: -0.95rem;
    width: 0.95rem;
  }

  .region-tree__marker {
    left: -0.03rem;
  }
}

@media (max-width: 480px) {
  .world-information__metadata {
    grid-template-columns: 1fr;
  }

  .world-editor__graph-body {
    padding: var(--space-2) var(--space-1);
  }
}

@media (prefers-reduced-motion: reduce) {
  .world-information__field,
  .region-tree__node {
    transition: none;
  }

  .region-tree__node:hover {
    transform: none;
  }
}
</style>