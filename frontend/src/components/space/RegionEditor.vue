<script setup lang="ts">
import { computed, ref } from "vue";

import {
  Location,
  LocationData,
  LocationKey,
  Region,
  RegionData,
  RegionKey,
} from "@/domain/World";
import { EntityTypes } from "@/domain/EntityTypes";
import { Lorebook } from "@/domain/Lorebook";

import { createEntity, deleteEntity } from "@/core/ABSEntity";

import Expandable from "@/components/utils/panels/Expandable.vue";
import LorebookEditor from "@/components/lorebooks/LorebookEditor.vue";
import List from "@/components/utils/list/List.vue";
import LocationEditor from "@/components/space/LocationEditor.vue";
import SplitPanel from "@/components/utils/panels/SplitPanel.vue";
import IconButton from "@/components/utils/buttons/IconButton.vue";
import SearchBar from "@/components/utils/SearchBar.vue";
import LongTextBox from "@/components/primitive-editors/LongTextBox.vue";
import ShortTextBox from "@/components/primitive-editors/ShortTextBox.vue";

const props = defineProps<{
  region: Region;
}>();

defineEmits<{
  (event: "delete", region: Region): void;
}>();

const childrenRegions = ref<Region[]>([]);
const childrenLocations = ref<Location[]>([]);
const lorebook = ref<Lorebook>();

const regionFilteringTerm = ref("");
const locationFilteringTerm = ref("");

const filteredRegions = computed<Region[]>(() => {
  const query = regionFilteringTerm.value
      .trim()
      .toLocaleLowerCase();

  if (!query) {
    return childrenRegions.value as Region[];
  }

  return childrenRegions.value.filter((region) => {
    const name = String(region.get("name") ?? "");

    return name
        .toLocaleLowerCase()
        .includes(query);
  }) as Region[];
});

const filteredLocations = computed<Location[]>(() => {
  const query = locationFilteringTerm.value
      .trim()
      .toLocaleLowerCase();

  if (!query) {
    return childrenLocations.value as Location[];
  }

  return childrenLocations.value.filter((location) => {
    const name = String(location.get("name") ?? "");

    return name
        .toLocaleLowerCase()
        .includes(query);
  }) as Location[];
});

const hasExpanded = ref(false);
const isLoading = ref(false);

async function expand(isOpen: boolean): Promise<void> {
  if (!isOpen || hasExpanded.value || isLoading.value) {
    return;
  }

  isLoading.value = true;

  try {
    const [
      loadedLocations,
      loadedRegions,
      loadedLorebook,
    ] = await Promise.all([
      props.region.getLocations(),
      props.region.getFirstChildren(),
      props.region.getLorebook(),
    ]);

    childrenLocations.value = loadedLocations;
    childrenRegions.value = loadedRegions;
    lorebook.value = loadedLorebook;

    hasExpanded.value = true;
  } finally {
    isLoading.value = false;
  }
}

async function onCreateLocation(): Promise<void> {
  const name = window.prompt("Enter location name:");

  if (!name?.trim()) {
    return;
  }

  const newLocation = await createEntity<
      LocationKey,
      LocationData,
      Location
  >(
      {
        worldID: props.region.get("world_id"),
      },
      {
        region_id: props.region.get("id"),
        name: name.trim(),
      },
      EntityTypes.LOCATIONS,
      Location,
  );

  if (newLocation) {
    childrenLocations.value.push(newLocation);
  }
}

const locationToEdit = ref<Location | null>(null);

function onEditLocation(location: Location): void {
  if (
      locationToEdit.value &&
      locationToEdit.value.equals(location)
  ) {
    locationToEdit.value = null;
    return;
  }

  locationToEdit.value = location;
}

async function deleteLocation(
    location: Location,
): Promise<void> {
  const success = await deleteEntity<LocationKey>(
      location.key,
      EntityTypes.LOCATIONS,
  );

  if (!success) {
    return;
  }

  childrenLocations.value =
      childrenLocations.value.filter(
          other => !other.equals(location),
      );

  if (
      locationToEdit.value &&
      locationToEdit.value.equals(location)
  ) {
    locationToEdit.value = null;
  }
}

async function createNewRegion(): Promise<void> {
  const name = window.prompt("Enter new region name:");

  if (!name?.trim()) {
    return;
  }

  const newRegion = await createEntity<
      RegionKey,
      RegionData,
      Region
  >(
      {
        world_id: props.region.get("world_id"),
      },
      {
        name: name.trim(),
        parent_region_id: props.region.get("id"),
      },
      EntityTypes.REGIONS,
      Region,
  );

  if (newRegion) {
    childrenRegions.value.push(newRegion);
  }
}
</script>

<template>
  <article
      class="
      region-node
      edit-box
      edit-box--primary
      edit-box--compact
    "
  >
    <Expandable
        class="region-node__expandable"
        :title="region.get('name')"
        @status-change="expand"
    >
      <div v-if="isLoading" class="edit-box__body">
        <div class="edit-box__state" aria-live="polite">
          <span class="edit-box__spinner" />

          <div class="edit-box__state-content">
            <strong class="edit-box__state-title">
              Loading region
            </strong>

            <p class="edit-box__state-description">
              Retrieving locations, child regions and lorebook data.
            </p>
          </div>
        </div>
      </div>

      <div
          v-else
          class="edit-box__body edit-box__stack region-node__content"
      >
        <!-- Basic information -->
        <section class="edit-box__section region-section region-section--identity">
          <header class="edit-box__section-header">
            <div class="edit-box__section-heading">
              <h3 class="edit-box__section-title">
                Basic information
              </h3>

              <p class="edit-box__section-description">
                Define the region name and contextual description used in
                generated prompts.
              </p>
            </div>
          </header>

          <div class="region-section__body region-identity">
            <div class="region-identity__field">
              <FieldEditorWrapper
                  field-name="Name"
                  info="The region's display name."
                  :vertical="true"
              >
                <ShortTextBox
                    :model-value="region.get('name')"
                    @edit="payload => region.update('name', payload)"
                />
              </FieldEditorWrapper>
            </div>

            <div class="region-identity__field region-identity__field--description">
              <FieldEditorWrapper
                  field-name="Description"
                  info="Context applied while characters are inside this region."
                  :vertical="true"
              >
                <LongTextBox
                    :model-value="region.get('description')"
                    @edit="payload => region.update('description', payload)"
                    tokenize
                    :tokenization-started="true"
                />
              </FieldEditorWrapper>
            </div>
          </div>
        </section>

        <!-- Lorebook -->
        <section class="edit-box__section region-section">
          <Expandable title="Lorebook" variant="compact">
            <div class="region-section__body">
              <LorebookEditor
                  v-if="hasExpanded && lorebook"
                  :model-value="lorebook"
              />

              <div
                  v-else-if="hasExpanded"
                  class="edit-box__state edit-box__state--vertical region-section__empty"
              >
                <div class="edit-box__state-icon">
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H11v17H6.5A2.5 2.5 0 0 0 4 22V5.5Z" />
                    <path d="M20 5.5A2.5 2.5 0 0 0 17.5 3H13v17h4.5A2.5 2.5 0 0 1 20 22V5.5Z" />
                  </svg>
                </div>

                <div class="edit-box__state-content">
                  <strong class="edit-box__state-title">
                    No lorebook
                  </strong>

                  <p class="edit-box__state-description">
                    This region does not currently have a lorebook.
                  </p>
                </div>
              </div>
            </div>
          </Expandable>
        </section>

        <!-- Locations -->
        <section class="edit-box__section region-section">
          <Expandable title="Locations" variant="compact">
            <div v-if="hasExpanded" class="locations-layout">
              <div class="edit-box__toolbar">
                <div class="edit-box__toolbar-main">
                  <SearchBar
                      v-model:search="locationFilteringTerm"
                      placeholder="Filter locations by name"
                      aria-label="Filter locations by name"
                  />
                </div>

                <div class="edit-box__toolbar-actions">
                  <span class="edit-box__count">
                    {{ filteredLocations.length }}
                  </span>
                </div>
              </div>

              <SplitPanel
                  class="locations-layout__split-panel"
                  storage-key="WorldEdit"
              >
                <template #left>
                  <div class="locations-layout__list">
                    <List
                        :elements="filteredLocations"
                        @create="onCreateLocation"
                        @edit="element => onEditLocation(element as Location)"
                        @remove="element => deleteLocation(element as Location)"
                    />
                  </div>
                </template>

                <template #right>
                  <div class="locations-layout__editor">
                    <LocationEditor
                        v-if="locationToEdit"
                        :key="locationToEdit.hashKey()"
                        :model-value="{
                        location: locationToEdit as Location,
                        all_locations: childrenLocations as Location[],
                      }"
                    />

                    <div
                        v-else
                        class="edit-box__state edit-box__state--vertical locations-layout__placeholder"
                    >
                      <div class="edit-box__state-icon">
                        <svg viewBox="0 0 24 24" aria-hidden="true">
                          <path d="M20 10c0 5-8 11-8 11S4 15 4 10a8 8 0 1 1 16 0Z" />
                          <circle cx="12" cy="10" r="2.5" />
                        </svg>
                      </div>

                      <div class="edit-box__state-content">
                        <strong class="edit-box__state-title">
                          Select a location
                        </strong>

                        <p class="edit-box__state-description">
                          Choose a location from the list to edit its details.
                        </p>
                      </div>
                    </div>
                  </div>
                </template>
              </SplitPanel>
            </div>
          </Expandable>
        </section>

        <!-- Child regions -->
        <section
            v-if="hasExpanded"
            class="
            edit-box__section
            edit-box__section--accent
            region-section
            region-section--children
          "
        >
          <Expandable title="Child regions">
            <div class="children-regions">
              <header class="edit-box__toolbar children-regions__toolbar">
                <div class="children-regions__heading">
                  <div class="children-regions__summary">
                    <span class="edit-box__eyebrow">
                      Nested regions
                    </span>

                    <span class="edit-box__count">
                      {{ filteredRegions.length }}
                    </span>
                  </div>

                  <SearchBar
                      v-model:search="regionFilteringTerm"
                      placeholder="Filter child regions by name"
                      aria-label="Filter child regions by name"
                  />
                </div>

                <div class="edit-box__toolbar-actions">
                  <IconButton
                      class="children-regions__create"
                      title="Create new region"
                      variant="accent"
                      @click="createNewRegion"
                  >
                    <svg viewBox="0 0 24 24" aria-hidden="true">
                      <path d="M12 5v14" />
                      <path d="M5 12h14" />
                    </svg>
                  </IconButton>
                </div>
              </header>

              <ul
                  v-if="filteredRegions.length"
                  class="region-tree"
                  role="tree"
                  :aria-label="`Child regions of ${region.get('name')}`"
              >
                <li
                    v-for="childRegion in filteredRegions"
                    :key="childRegion.hashKey()"
                    class="region-tree__item"
                    role="treeitem"
                >
                  <div class="region-tree__connector" aria-hidden="true" />

                  <div class="region-tree__child">
                    <RegionEditor :region="childRegion" />
                  </div>
                </li>
              </ul>

              <div
                  v-else-if="regionFilteringTerm.trim()"
                  class="edit-box__state edit-box__state--vertical region-filter-empty"
              >
                <div class="edit-box__state-icon">
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <circle cx="11" cy="11" r="7" />
                    <path d="m20 20-4-4" />
                  </svg>
                </div>

                <div class="edit-box__state-content">
                  <strong class="edit-box__state-title">
                    No matching regions
                  </strong>

                  <p class="edit-box__state-description">
                    No child region contains
                    <span class="region-filter-empty__query">
                      “{{ regionFilteringTerm.trim() }}”
                    </span>
                    in its name.
                  </p>
                </div>

                <button
                    class="edit-box__action edit-box__action--accent"
                    type="button"
                    @click="regionFilteringTerm = ''"
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
                    <path d="M12 3v6" />
                    <path d="M6 21v-6" />
                    <path d="M18 21v-6" />
                    <path d="M6 15h12" />
                    <path d="M12 9v3a3 3 0 0 1-3 3H6" />
                    <path d="M12 12a3 3 0 0 0 3 3h3" />
                  </svg>
                </div>

                <div class="edit-box__state-content">
                  <strong class="edit-box__state-title">
                    No child regions
                  </strong>

                  <p class="edit-box__state-description">
                    Create a nested region to extend this part of the
                    hierarchy.
                  </p>
                </div>

                <button
                    class="edit-box__action edit-box__action--accent"
                    type="button"
                    @click="createNewRegion"
                >
                  Create child region
                </button>
              </div>
            </div>
          </Expandable>
        </section>
      </div>
    </Expandable>
  </article>
</template>

<style scoped>
.region-node {
  position: relative;
  width: 100%;
  min-width: 0;
  box-sizing: border-box;
  color: rgb(var(--c-fg));
  font-family: var(--font-primary);
}

.region-node__expandable {
  width: 100%;
  min-width: 0;
}

.region-node__content {
  gap: var(--space-3);
}

.region-section {
  padding: 0;
  overflow: hidden;
}

.region-section__body {
  min-width: 0;
  padding: var(--space-3);
}

.region-section__empty {
  min-height: 8rem;
}

/* -------------------------------------------------------------------------- */
/* Basic information                                                          */
/* -------------------------------------------------------------------------- */

.region-identity {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  min-width: 0;
}

.region-identity__field {
  min-width: 0;
  padding: var(--space-3);

  background: linear-gradient(
      145deg,
      rgb(var(--c-surface-raised) / 0.48),
      rgb(var(--c-surface-2) / 0.24)
  );

  border: 1px solid rgb(var(--c-border) / 0.19);
  border-radius: var(--radius-md);

  box-shadow:
      inset 0 1px 0 rgb(255 255 255 / 0.28),
      0 3px 9px rgb(var(--c-shadow) / 0.035);

  transition:
      background-color var(--duration-normal) var(--ease-standard),
      border-color var(--duration-normal) var(--ease-standard),
      box-shadow var(--duration-normal) var(--ease-standard);
}

.region-identity__field:hover {
  border-color: rgb(var(--c-primary) / 0.27);
}

.region-identity__field:focus-within {
  background: rgb(var(--c-surface-raised) / 0.62);
  border-color: rgb(var(--c-primary) / 0.42);

  box-shadow:
      0 0 0 3px rgb(var(--c-primary) / 0.09),
      inset 0 1px 0 rgb(255 255 255 / 0.32);
}

.region-identity__field--description {
  border-color: rgb(var(--c-primary) / 0.2);

  background: linear-gradient(
      145deg,
      rgb(var(--c-primary) / 0.055),
      rgb(var(--c-surface-raised) / 0.5)
  );
}

.region-identity__field :deep(input),
.region-identity__field :deep(textarea) {
  width: 100%;
  min-width: 0;
  box-sizing: border-box;
}

.region-identity__field :deep(textarea) {
  min-height: 8rem;
  resize: vertical;
}

/* -------------------------------------------------------------------------- */
/* Locations                                                                  */
/* -------------------------------------------------------------------------- */

.locations-layout {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  min-width: 0;
  padding: var(--space-3);
}

.locations-layout__split-panel {
  min-height: 20rem;

  background: linear-gradient(
      145deg,
      rgb(var(--c-surface-raised) / 0.4),
      rgb(var(--c-surface-2) / 0.24)
  );

  border: 1px solid rgb(var(--c-border) / 0.22);
  border-radius: var(--radius-md);

  box-shadow:
      inset 0 1px 0 rgb(255 255 255 / 0.26),
      0 4px 14px rgb(var(--c-shadow) / 0.045);

  overflow: hidden;
}

.locations-layout__list,
.locations-layout__editor {
  width: 100%;
  min-width: 0;
  height: 100%;
  box-sizing: border-box;
}

.locations-layout__list {
  padding: var(--space-2);
  background: rgb(var(--c-surface) / 0.18);
}

.locations-layout__editor {
  padding: var(--space-3);

  background: linear-gradient(
      145deg,
      rgb(var(--c-surface-raised) / 0.32),
      rgb(var(--c-surface-2) / 0.2)
  );
}

.locations-layout__placeholder {
  min-height: 16rem;
}

/* -------------------------------------------------------------------------- */
/* Children                                                                   */
/* -------------------------------------------------------------------------- */

.children-regions {
  min-width: 0;
  padding: var(--space-3);
}

.children-regions__toolbar {
  align-items: flex-end;
  margin-bottom: var(--space-3);
}

.children-regions__heading {
  display: flex;
  flex: 1 1 22rem;
  flex-direction: column;
  gap: var(--space-2);
  min-width: 0;
}

.children-regions__summary {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.children-regions__summary .edit-box__eyebrow {
  margin: 0;
}

.children-regions__create {
  flex: 0 0 auto;
}

/* -------------------------------------------------------------------------- */
/* Recursive region tree                                                      */
/* -------------------------------------------------------------------------- */

.region-tree {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  min-width: 0;
  margin: 0;
  padding: 0 0 0 1.6rem;
  list-style: none;
}

.region-tree::before {
  content: "";
  position: absolute;
  top: 0;
  bottom: 1.25rem;
  left: 0.5rem;
  width: 2px;

  background: linear-gradient(
      to bottom,
      rgb(var(--c-primary) / 0.66),
      rgb(var(--c-primary) / 0.1)
  );

  border-radius: var(--radius-round);
}

.region-tree__item {
  position: relative;
  min-width: 0;
}

.region-tree__connector {
  position: absolute;
  top: 1.4rem;
  left: -1.1rem;
  width: 1.1rem;
  height: 2px;
  background: rgb(var(--c-primary) / 0.56);
}

.region-tree__connector::before {
  content: "";
  position: absolute;
  top: 50%;
  right: -0.28rem;
  width: 0.55rem;
  height: 0.55rem;
  box-sizing: border-box;
  background: rgb(var(--c-accent));
  border: 2px solid rgb(var(--c-primary));
  border-radius: 50%;
  box-shadow: 0 0 0 3px rgb(var(--c-accent) / 0.12);
  transform: translateY(-50%);
}

.region-tree__child {
  min-width: 0;
  padding: var(--space-1);

  background: linear-gradient(
      145deg,
      rgb(var(--c-surface-raised) / 0.28),
      rgb(var(--c-surface-2) / 0.16)
  );

  border: 1px solid rgb(var(--c-primary) / 0.17);
  border-radius: var(--radius-md);

  transition:
      background-color var(--duration-normal) var(--ease-standard),
      border-color var(--duration-normal) var(--ease-standard),
      box-shadow var(--duration-normal) var(--ease-standard),
      transform var(--duration-normal) var(--ease-standard);
}

.region-tree__child:hover {
  background: linear-gradient(
      145deg,
      rgb(var(--c-surface-raised) / 0.46),
      rgb(var(--c-surface-hover) / 0.28)
  );

  border-color: rgb(var(--c-primary) / 0.34);
  box-shadow: 0 6px 18px rgb(var(--c-shadow) / 0.075);
  transform: translateY(-1px);
}

/* -------------------------------------------------------------------------- */
/* Filter state                                                               */
/* -------------------------------------------------------------------------- */

.region-filter-empty {
  min-height: 10rem;
}

.region-filter-empty__query {
  color: rgb(var(--c-primary-strong));
  font-weight: 800;
}

/* -------------------------------------------------------------------------- */
/* Responsive                                                                 */
/* -------------------------------------------------------------------------- */

@media (max-width: 720px) {
  .region-node__content {
    padding: var(--space-2);
  }

  .locations-layout,
  .children-regions,
  .region-section__body {
    padding: var(--space-2);
  }

  .region-identity__field {
    padding: var(--space-2);
  }

  .children-regions__toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .children-regions__heading {
    flex-basis: auto;
    width: 100%;
  }

  .children-regions__create {
    align-self: flex-end;
  }

  .region-tree {
    padding-left: 1.2rem;
  }

  .region-tree::before {
    left: 0.34rem;
  }

  .region-tree__connector {
    left: -0.86rem;
    width: 0.86rem;
  }
}

@media (max-width: 480px) {
  .locations-layout__split-panel {
    min-height: 16rem;
  }

  .locations-layout__editor {
    padding: var(--space-2);
  }

  .region-tree {
    padding-left: 0.9rem;
  }

  .region-tree::before {
    left: 0.22rem;
  }

  .region-tree__connector {
    left: -0.68rem;
    width: 0.68rem;
  }
}

@media (prefers-reduced-motion: reduce) {
  .region-identity__field,
  .region-tree__child {
    transition: none;
  }
}
</style>