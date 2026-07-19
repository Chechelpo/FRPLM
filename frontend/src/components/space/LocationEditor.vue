<script setup lang="ts">
import {computed, onMounted, ref, shallowRef, watch,} from "vue";

import {Location} from "@/domain/World";
import {Character, type CharacterData, type CharacterKey} from "@/domain/Characters";
import {Lorebook} from "@/domain/Lorebook";
import {EntityTypes} from "@/domain/EntityTypes";
import type {DTO} from "@/types/DTOs";

import {createEntity, deleteEntity, } from "@/core/ABSEntity";
import {API_BASE} from "@/config";

import ShortTextBox from "@/components/utils/primitiveEditors/ShortTextBox.vue";
import FieldEditorWrapper from "@/components/utils/FieldEditorWrapper.vue";
import Expandable from "@/components/utils/panels/Expandable.vue";
import LorebookEditor from "@/components/lorebooks/LorebookEditor.vue";
import LocationEdgesEditor from "@/components/space/LocationEdgesEditor.vue";
import List from "@/components/utils/list/List.vue";
import SplitPanel from "@/components/utils/panels/SplitPanel.vue";
import CharacterEditor from "@/components/char/CharacterEditor.vue";
import LongTextBox from "@/components/utils/primitiveEditors/LongTextBox.vue";
import {fetchApi} from "@/services/apiClient";

const model = defineModel<{
  location: Location;
  all_locations: Location[];
}>({
  required: true,
});

const lorebook = shallowRef<Lorebook | null>(null);
const charactersHere = ref<Character[]>([]);
const editingCharacter =
    shallowRef<Character | null>(null);

const loadingLorebook = ref(false);
const loadingCharacters = ref(false);
const creatingCharacter = ref(false);
const deletingCharacterId =
    ref<number | string | null>(null);
const loadError = ref<string | null>(null);

let loadRequestId = 0;

const locationName = computed(() =>
    String(
        model.value.location.get("name") ??
        "Unnamed location",
    ),
);

const characterCount = computed(
    () => charactersHere.value.length,
);

const isLoading = computed(
    () =>
        loadingLorebook.value ||
        loadingCharacters.value,
);

async function loadLocationData(): Promise<void> {
  const requestId = ++loadRequestId;
  const currentLocation = model.value.location;

  editingCharacter.value = null;
  lorebook.value = null;
  charactersHere.value = [];

  loadingLorebook.value = true;
  loadingCharacters.value = true;
  loadError.value = null;

  try {
    const [
      loadedLorebook,
      characterResponse,
    ] = await Promise.all([
      currentLocation.getLorebook(),
      currentLocation.getStartingHere()
    ]);


    if (requestId !== loadRequestId) {
      return;
    }
    lorebook.value = loadedLorebook;
    charactersHere.value = characterResponse;
  } catch (error) {
    if (requestId !== loadRequestId) {
      return;
    }

    console.error(
        "Could not load location data",
        error,
    );

    loadError.value =
        "The location data could not be loaded.";
  } finally {
    if (requestId === loadRequestId) {
      loadingLorebook.value = false;
      loadingCharacters.value = false;
    }
  }
}

async function createCharacterInLocation(): Promise<void> {
  const name = window.prompt(
      "Enter new character name:",
  );

  if (!name?.trim()) {
    return;
  }

  creatingCharacter.value = true;

  try {
    const newCharacter = await createEntity<
        CharacterKey,
        CharacterData,
        Character
    >(
        null,
        {
          name: name.trim(),
        },
        EntityTypes.CHARACTERS,
        Character,
    );

    await newCharacter.markStartingAt(
        model.value.location,
    );

    charactersHere.value = [
      ...charactersHere.value,
      newCharacter,
    ];

    selectCharacter(newCharacter)
  } finally {
    creatingCharacter.value = false;
  }
}

function selectCharacter(
    character: Character,
): void {
  const selectedId =
      editingCharacter.value?.get("id");

  if (selectedId === character.get("id")) {
    editingCharacter.value = null;
    return;
  }

  editingCharacter.value = character;
}

async function onDeleteCharacter(
    character: Character,
): Promise<void> {
  const characterName = String(
      character.get("name") ?? "this character",
  );

  const confirmed = window.confirm(
      `Delete ${characterName}?`,
  );

  if (!confirmed) {
    return;
  }

  const characterId = character.get("id");
  deletingCharacterId.value = characterId;

  try {
    const success =
        await deleteEntity<CharacterKey>(
            character.key,
            EntityTypes.CHARACTERS,
        );

    if (!success) {
      console.error(
          "Could not delete character",
      );

      return;
    }

    charactersHere.value =
        charactersHere.value.filter(
            (candidate) =>
                candidate.get("id") !== characterId,
        );

    if (
        editingCharacter.value?.get("id") ===
        characterId
    ) {
      editingCharacter.value = null;
    }
  } finally {
    deletingCharacterId.value = null;
  }
}

onMounted(() => {
  void loadLocationData();
});

watch(
    () => model.value.location.get("id"),
    () => {
      void loadLocationData();
    },
);
</script>

<template>
  <article
      class="location-editor edit-box edit-box--primary"
      :aria-busy="isLoading"
  >
    <header class="edit-box__header location-editor__header">
      <div class="edit-box__header-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24">
          <path d="M20 10c0 5-8 11-8 11S4 15 4 10a8 8 0 1 1 16 0Z" />
          <circle cx="12" cy="10" r="2.5" />
        </svg>
      </div>

      <div class="edit-box__header-main">
        <span class="edit-box__eyebrow">
          Location
        </span>

        <div class="edit-box__title-row">
          <h2 class="edit-box__title">
            {{ locationName }}
          </h2>

          <span class="edit-box__count">
            {{ characterCount }}
            {{ characterCount === 1 ? "character" : "characters" }}
          </span>
        </div>

        <p class="edit-box__description">
          Configure the location description, lorebook, connections, and
          starting characters.
        </p>
      </div>
    </header>

    <div v-if="loadError" class="edit-box__body">
      <div
          class="edit-box__state edit-box__state--error edit-box__state--vertical"
          role="alert"
      >
        <div class="edit-box__state-content">
          <strong class="edit-box__state-title">
            Could not load location
          </strong>

          <p class="edit-box__state-description">
            {{ loadError }}
          </p>
        </div>

        <button
            type="button"
            class="edit-box__action edit-box__action--accent"
            @click="loadLocationData"
        >
          Retry
        </button>
      </div>
    </div>

    <div
        v-else
        class="edit-box__body edit-box__stack location-editor__body"
    >
      <!-- Basic information -->
      <section class="edit-box__section location-editor__section">
        <header class="edit-box__section-header">
          <div class="edit-box__section-heading">
            <h3 class="edit-box__section-title">
              Basic information
            </h3>

            <p class="edit-box__section-description">
              Define how this location is named and described in generated
              prompts.
            </p>
          </div>
        </header>

        <div class="location-editor__field-row">
          <div class="location-editor__field location-editor__field--name">
            <FieldEditorWrapper
                field-name="Name"
                info="The location's display name."
                :vertical="true"
            >
              <ShortTextBox
                  :model-value="model.location.get('name')"
                  @edit="payload => model.location.update('name', payload)"
              />
            </FieldEditorWrapper>
          </div>

          <div class="location-editor__field location-editor__field--description">
            <FieldEditorWrapper
                field-name="Description"
                info="Injected into the prompt while this is the current location."
                :vertical="true"
            >
              <LongTextBox
                  :model-value="model.location.get('description')"
                  @edit="payload => model.location.update('description', payload)"
                  tokenize
                  :tokenization-started="true"
              />
            </FieldEditorWrapper>
          </div>
        </div>
      </section>

      <!-- Lorebook -->
      <section
          class="edit-box__section location-editor__section location-editor__section--expandable"
      >
        <Expandable
            title="Location lorebook"
            :initially-open="false"
            info="Activated while characters are present at this location."
        >
          <div class="location-editor__section-body">
            <div
                v-if="loadingLorebook"
                class="edit-box__state"
                role="status"
                aria-live="polite"
            >
              <span class="edit-box__spinner" aria-hidden="true" />

              <div class="edit-box__state-content">
                <strong class="edit-box__state-title">
                  Loading lorebook
                </strong>

                <p class="edit-box__state-description">
                  Retrieving location-specific lore entries.
                </p>
              </div>
            </div>

            <LorebookEditor
                v-else-if="lorebook"
                :model-value="lorebook"
            />

            <div
                v-else
                class="edit-box__state edit-box__state--vertical"
            >
              <div class="edit-box__state-icon">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path
                      d="M4 5.5A2.5 2.5 0 0 1 6.5 3H11v17H6.5A2.5 2.5 0 0 0 4 22V5.5Z"
                  />
                  <path
                      d="M20 5.5A2.5 2.5 0 0 0 17.5 3H13v17h4.5A2.5 2.5 0 0 1 20 22V5.5Z"
                  />
                </svg>
              </div>

              <div class="edit-box__state-content">
                <strong class="edit-box__state-title">
                  No lorebook
                </strong>

                <p class="edit-box__state-description">
                  This location does not currently have a lorebook.
                </p>
              </div>
            </div>
          </div>
        </Expandable>
      </section>

      <!-- Connections -->
      <section
          class="edit-box__section location-editor__section location-editor__section--expandable"
      >
        <Expandable
            title="Connected locations"
            info="Configure the locations that can be reached from this location."
        >
          <div class="location-editor__section-body">
            <LocationEdgesEditor
                :model-value="{
                parentLocation: model.location,
                all_locations: model.all_locations,
              }"
            />
          </div>
        </Expandable>
      </section>

      <!-- Starting characters -->
      <section
          class="
          edit-box__section
          edit-box__section--accent
          location-editor__section
          location-editor__section--expandable
        "
      >
        <Expandable
            title="Characters starting here"
            info="Static starting locations, not current session positions."
        >
          <div class="location-characters location-editor__section-body">
            <header class="edit-box__toolbar location-characters__toolbar">
              <div class="edit-box__toolbar-main">
                <span class="edit-box__eyebrow">
                  Starting characters
                </span>

                <p class="location-characters__description">
                  Select a character to edit its configuration.
                </p>
              </div>

              <div class="edit-box__toolbar-actions">
                <span class="edit-box__count">
                  {{ characterCount }}
                </span>

                <button
                    type="button"
                    class="edit-box__action edit-box__action--accent"
                    :disabled="creatingCharacter"
                    @click="createCharacterInLocation"
                >
                  <span
                      v-if="creatingCharacter"
                      class="edit-box__spinner"
                      aria-hidden="true"
                  />

                  {{ creatingCharacter ? "Creating..." : "New character" }}
                </button>
              </div>
            </header>

            <div
                v-if="loadingCharacters"
                class="edit-box__state"
                role="status"
                aria-live="polite"
            >
              <span class="edit-box__spinner" aria-hidden="true" />

              <div class="edit-box__state-content">
                <strong class="edit-box__state-title">
                  Loading characters
                </strong>

                <p class="edit-box__state-description">
                  Retrieving characters that begin at this location.
                </p>
              </div>
            </div>

            <SplitPanel
                v-else
                class="location-characters__split-panel"
                storage-key="location-characters-editor"
            >
              <template #left>
                <div class="location-characters__list">
                  <List
                      :elements="charactersHere as Character[]"
                      @edit="value => selectCharacter(value as Character)"
                      @create="createCharacterInLocation"
                      @remove="value => onDeleteCharacter(value as Character)"
                  />

                  <div
                      v-if="charactersHere.length === 0"
                      class="edit-box__state edit-box__state--vertical location-characters__empty"
                  >
                    <div class="edit-box__state-icon">
                      <svg viewBox="0 0 24 24" aria-hidden="true">
                        <circle cx="12" cy="8" r="4" />
                        <path d="M4 21a8 8 0 0 1 16 0" />
                      </svg>
                    </div>

                    <div class="edit-box__state-content">
                      <strong class="edit-box__state-title">
                        No starting characters
                      </strong>

                      <p class="edit-box__state-description">
                        Create a character whose initial location is
                        {{ locationName }}.
                      </p>
                    </div>
                  </div>
                </div>
              </template>

              <template #right>
                <div class="location-characters__editor">
                  <CharacterEditor
                      v-if="editingCharacter"
                      :key="editingCharacter.get('id')"
                      :model-value="editingCharacter"
                      :edit-starting-locations="false"
                  />

                  <div
                      v-else
                      class="edit-box__state edit-box__state--vertical location-characters__placeholder"
                  >
                    <div class="edit-box__state-icon">
                      <svg viewBox="0 0 24 24" aria-hidden="true">
                        <circle cx="12" cy="8" r="4" />
                        <path d="M4 21a8 8 0 0 1 16 0" />
                        <path d="M18 3v4" />
                        <path d="M16 5h4" />
                      </svg>
                    </div>

                    <div class="edit-box__state-content">
                      <strong class="edit-box__state-title">
                        Select a character
                      </strong>

                      <p class="edit-box__state-description">
                        Choose a character from the list to edit its details.
                      </p>
                    </div>
                  </div>
                </div>
              </template>
            </SplitPanel>

            <p
                v-if="deletingCharacterId !== null"
                class="location-characters__operation"
                role="status"
                aria-live="polite"
            >
              Deleting character...
            </p>
          </div>
        </Expandable>
      </section>
    </div>
  </article>
</template>

<style scoped>
.location-editor {
  width: 100%;
  min-width: 0;
  color: rgb(var(--c-fg));
  font-family: var(--font-primary);
}

.location-editor__header {
  align-items: flex-start;
}

.location-editor__body {
  gap: var(--space-3);
}

.location-editor__section {
  min-width: 0;
}

.location-editor__section--expandable {
  padding: 0;
  overflow: hidden;
}

.location-editor__section-body {
  min-width: 0;
  padding: var(--space-3);
}

/* -------------------------------------------------------------------------- */
/* Basic information                                                          */
/* -------------------------------------------------------------------------- */

.location-editor__field-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: var(--space-3);
  min-width: 0;
}

.location-editor__field {
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

.location-editor__field:hover {
  border-color: rgb(var(--c-primary) / 0.27);
}

.location-editor__field:focus-within {
  background: rgb(var(--c-surface-raised) / 0.62);
  border-color: rgb(var(--c-primary) / 0.42);

  box-shadow:
      0 0 0 3px rgb(var(--c-primary) / 0.09),
      inset 0 1px 0 rgb(255 255 255 / 0.32);
}

.location-editor__field--description {
  border-color: rgb(var(--c-primary) / 0.2);

  background: linear-gradient(
      145deg,
      rgb(var(--c-primary) / 0.055),
      rgb(var(--c-surface-raised) / 0.5)
  );
}

.location-editor__field :deep(input),
.location-editor__field :deep(textarea) {
  width: 100%;
  min-width: 0;
  box-sizing: border-box;
}

.location-editor__field :deep(textarea) {
  min-height: 8rem;
  resize: vertical;
}

/* -------------------------------------------------------------------------- */
/* Starting characters                                                        */
/* -------------------------------------------------------------------------- */

.location-characters {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  min-width: 0;
}

.location-characters__toolbar {
  align-items: flex-end;
  margin-bottom: 0;
}

.location-characters__toolbar .edit-box__eyebrow {
  margin: 0;
}

.location-characters__description {
  margin: var(--space-1) 0 0;
  color: rgb(var(--c-muted));
  font-size: 0.76rem;
  line-height: 1.45;
}

.location-characters__split-panel {
  width: 100%;
  min-width: 0;
  height: clamp(24rem, 58dvh, 44rem);

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

.location-characters__list,
.location-characters__editor {
  width: 100%;
  min-width: 0;
  height: 100%;
  box-sizing: border-box;
}

.location-characters__list {
  padding: var(--space-2);
  overflow: auto;
  background: rgb(var(--c-surface) / 0.18);
  scrollbar-width: thin;
  scrollbar-color: rgb(var(--c-primary) / 0.44) transparent;
}

.location-characters__editor {
  padding: var(--space-3);
  overflow: auto;

  background: linear-gradient(
      145deg,
      rgb(var(--c-surface-raised) / 0.32),
      rgb(var(--c-surface-2) / 0.2)
  );

  scrollbar-width: thin;
  scrollbar-color: rgb(var(--c-primary) / 0.44) transparent;
}

.location-characters__list::-webkit-scrollbar,
.location-characters__editor::-webkit-scrollbar {
  width: 0.65rem;
  height: 0.65rem;
}

.location-characters__list::-webkit-scrollbar-track,
.location-characters__editor::-webkit-scrollbar-track {
  background: transparent;
}

.location-characters__list::-webkit-scrollbar-thumb,
.location-characters__editor::-webkit-scrollbar-thumb {
  background: rgb(var(--c-primary) / 0.38);
  border: 2px solid transparent;
  border-radius: var(--radius-round);
  background-clip: padding-box;
}

.location-characters__empty {
  min-height: 12rem;
  margin-top: var(--space-2);
}

.location-characters__placeholder {
  min-height: 100%;
}

.location-characters__operation {
  margin: 0;
  color: rgb(var(--c-muted));
  font-size: 0.75rem;
  line-height: 1.4;
  text-align: right;
}

/* -------------------------------------------------------------------------- */
/* Responsive                                                                 */
/* -------------------------------------------------------------------------- */

@media (max-width: 760px) {
  .location-editor__body,
  .location-editor__section-body {
    padding: var(--space-2);
  }

  .location-editor__field {
    padding: var(--space-2);
  }

  .location-characters__toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .location-characters__toolbar .edit-box__toolbar-actions {
    width: 100%;
    justify-content: space-between;
  }

  .location-characters__split-panel {
    height: 36rem;
  }

  .location-characters__editor {
    padding: var(--space-2);
  }
}

@media (max-width: 480px) {
  .location-characters__split-panel {
    min-height: 22rem;
    height: 32rem;
  }

  .location-characters__toolbar .edit-box__toolbar-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .location-characters__toolbar .edit-box__action {
    width: 100%;
  }
}

@media (prefers-reduced-motion: reduce) {
  .location-editor__field,
  .location-characters__list,
  .location-characters__editor {
    transition: none;
    scroll-behavior: auto;
  }
}
</style>