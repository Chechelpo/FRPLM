<script setup lang="ts">
import {     ShortTextBox } from "@frplm/ui";

import {     Character,
    EntityTypes } from "@frplm/host-sdk";

import {
  computed,
  ref,
  shallowRef,
  watch,
} from "vue";

import {
  World,
  type WorldData,
  type WorldKey,
} from "@frplm/host-sdk";

import {
  fetch_all,
} from "@frplm/host-sdk";

import WindowPrompt from "@components/utils/prompts/WindowPrompt.vue";
import FieldEditorWrapper from "@components/utils/FieldEditorWrapper.vue";
import SearchBar from "@components/utils/SearchBar.vue";

const model = defineModel<boolean>({
  required: true,
});

const emit = defineEmits<{
  (
      event: "createNewSession",
      payload: {
        name: string;
        world: World;
        character: Character;
      },
  ): void;

  (event: "close"): void;
}>();

const newSessionName = ref("");

const worlds = ref<World[]>([]);
const characters = ref<Character[]>([]);

const selectedWorld =
    shallowRef<World | null>(null);

const selectedCharacter =
    shallowRef<Character | null>(null);

const worldSearch = ref("");
const characterSearch = ref("");

const loadingWorlds = ref(false);
const loadingCharacters = ref(false);

const worldLoadError =
    ref<string | null>(null);

const characterLoadError =
    ref<string | null>(null);

const submitAttempted = ref(false);

let characterRequestId = 0;

function normalize(value: unknown): string {
  return String(value ?? "")
      .trim()
      .toLocaleLowerCase()
      .normalize("NFKD")
      .replace(/\p{Diacritic}/gu, "");
}

function getWorldName(world: World): string {
  const name = String(
      world.get("name") ?? "",
  ).trim();

  return name || "Unnamed world";
}

function getCharacterName(
    character: Character,
): string {
  const name = String(
      character.get("name") ?? "",
  ).trim();

  return name || "Unnamed character";
}

function getEntityId(
    entity: World | Character,
): string {
  return String(entity.get("id") ?? "—");
}

function isSelectedWorld(
    world: World,
): boolean {
  return (
      selectedWorld.value?.get("id") ===
      world.get("id")
  );
}

function isSelectedCharacter(
    character: Character,
): boolean {
  return (
      selectedCharacter.value?.get("id") ===
      character.get("id")
  );
}

const filteredWorlds = computed<World[]>(
    () => {
      const query = normalize(
          worldSearch.value,
      );

      return [...worlds.value]
          .filter((world) => {
            if (!query) {
              return true;
            }

            return (
                normalize(
                    getWorldName(world),
                ).includes(query) ||
                normalize(
                    getEntityId(world),
                ).includes(query)
            );
          })
          .sort((first, second) =>
              getWorldName(first).localeCompare(
                  getWorldName(second),
              ),
          );
    },
);

const filteredCharacters = computed<
    Character[]
>(() => {
  const query = normalize(
      characterSearch.value,
  );

  return [...characters.value]
      .filter((character) => {
        if (!query) {
          return true;
        }

        return (
            normalize(
                getCharacterName(character),
            ).includes(query) ||
            normalize(
                getEntityId(character),
            ).includes(query)
        );
      })
      .sort((first, second) =>
          getCharacterName(
              first,
          ).localeCompare(
              getCharacterName(second),
          ),
      );
});

const validationIssues = computed<
    string[]
>(() => {
  const issues: string[] = [];

  if (!newSessionName.value.trim()) {
    issues.push(
        "Enter a name for the session.",
    );
  }

  if (!selectedWorld.value) {
    issues.push("Select a world.");
  }

  if (!selectedCharacter.value) {
    issues.push(
        "Select a user character.",
    );
  }

  return issues;
});

const canCreate = computed(
    () =>
        validationIssues.value.length === 0 &&
        !loadingWorlds.value &&
        !loadingCharacters.value,
);

function resetForm(): void {
  newSessionName.value = "";

  selectedWorld.value = null;
  selectedCharacter.value = null;

  characters.value = [];

  worldSearch.value = "";
  characterSearch.value = "";

  worldLoadError.value = null;
  characterLoadError.value = null;

  submitAttempted.value = false;

  characterRequestId += 1;
}

async function loadWorlds(): Promise<void> {
  loadingWorlds.value = true;
  worldLoadError.value = null;

  try {
    worlds.value = await fetch_all<
        WorldKey,
        WorldData,
        World
    >(
        EntityTypes.WORLDS,
        World,
    );
  } catch (error) {
    console.error(
        "Could not load worlds",
        error,
    );

    worlds.value = [];

    worldLoadError.value =
        "The available worlds could not be loaded.";
  } finally {
    loadingWorlds.value = false;
  }
}

async function loadCharacters(
    world: World,
): Promise<void> {
  const requestId =
      ++characterRequestId;

  loadingCharacters.value = true;
  characterLoadError.value = null;

  characters.value = [];
  selectedCharacter.value = null;

  try {
    const loadedCharacters =
        await Character.getStartingAt(world);

    if (
        requestId !== characterRequestId
    ) {
      return;
    }

    characters.value =
        loadedCharacters.filter(
            (character) =>
                Boolean(
                    character.get(
                        "can_be_user",
                    ),
                ),
        );
  } catch (error) {
    if (
        requestId !== characterRequestId
    ) {
      return;
    }

    console.error(
        "Could not load user characters",
        error,
    );

    characterLoadError.value =
        "The available user characters could not be loaded.";
  } finally {
    if (
        requestId === characterRequestId
    ) {
      loadingCharacters.value = false;
    }
  }
}

function selectWorld(world: World): void {
  if (isSelectedWorld(world)) {
    return;
  }

  selectedWorld.value = world;
  selectedCharacter.value = null;

  characterSearch.value = "";
  submitAttempted.value = false;

  void loadCharacters(world);
}

function selectCharacter(
    character: Character,
): void {
  selectedCharacter.value = character;
  submitAttempted.value = false;
}

function closePopup(): void {
  model.value = false;
  emit("close");
}

function createNewSession(): void {
  submitAttempted.value = true;

  if (
      !canCreate.value ||
      !selectedWorld.value ||
      !selectedCharacter.value
  ) {
    return;
  }

  emit("createNewSession", {
    name: newSessionName.value.trim(),
    world: selectedWorld.value,
    character:
    selectedCharacter.value,
  });

  closePopup();
}

watch(
    () => model.value,
    (isOpen) => {
      if (!isOpen) {
        return;
      }

      resetForm();
      void loadWorlds();
    },
);
</script>

<template>
  <WindowPrompt
      v-if="model"
      title="New session"
      @close="closePopup"
  >
    <div class="new-session">
      <header class="new-session__intro">
        <div
            class="new-session__intro-icon"
            aria-hidden="true"
        >
          <svg viewBox="0 0 24 24">
            <path
                d="M4 5h16v11H8l-4 4V5Z"
            />

            <path d="M12 8v5" />
            <path d="M9.5 10.5h5" />
          </svg>
        </div>

        <div class="new-session__intro-main">
          <span class="edit-box__eyebrow">
            Conversation setup
          </span>

          <h2 class="new-session__intro-title">
            Create a narrative session
          </h2>

          <p
              class="
              new-session__intro-description
            "
          >
            Choose the world and user character
            that will provide the initial context
            for the conversation.
          </p>
        </div>
      </header>

      <div
          class="
          edit-box__stack
          new-session__fields
        "
      >
        <section
            class="
            edit-box__section
            new-session__section
          "
        >
          <FieldEditorWrapper
              field-name="Session name"
              info="A recognizable name for this conversation."
              :vertical="true"
          >
            <ShortTextBox
                :model-value="newSessionName"
                aria-label="Session name"
                @edit="
                payload => {
                  newSessionName = payload;
                  submitAttempted = false;
                }
              "
            />
          </FieldEditorWrapper>
        </section>

        <!-- World selection -->
        <section
            class="
            edit-box__section
            new-session__section
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
              <span class="edit-box__eyebrow">
                Step 1
              </span>

              <h3
                  class="
                  edit-box__section-title
                "
              >
                Select a world
              </h3>

              <p
                  class="
                  edit-box__section-description
                "
              >
                The session will use this world's
                locations, regions, and lore.
              </p>
            </div>

            <span class="edit-box__count">
              {{ worlds.length }}
            </span>
          </header>

          <div
              v-if="
              worlds.length > 5 ||
              worldSearch.trim()
            "
              class="
              new-session__selection-search
            "
          >
            <SearchBar
                v-model:search="worldSearch"
                placeholder="Search worlds by name or ID"
                aria-label="Search worlds"
            />
          </div>

          <div
              v-if="loadingWorlds"
              class="
              edit-box__state
              new-session__selection-state
            "
              role="status"
              aria-live="polite"
          >
            <span
                class="edit-box__spinner"
                aria-hidden="true"
            />

            <div
                class="edit-box__state-content"
            >
              <strong
                  class="edit-box__state-title"
              >
                Loading worlds
              </strong>

              <p
                  class="
                  edit-box__state-description
                "
              >
                Retrieving available narrative
                worlds.
              </p>
            </div>
          </div>

          <div
              v-else-if="worldLoadError"
              class="
              edit-box__state
              edit-box__state--error
              edit-box__state--vertical
              new-session__selection-state
            "
              role="alert"
          >
            <div
                class="edit-box__state-content"
            >
              <strong
                  class="edit-box__state-title"
              >
                Could not load worlds
              </strong>

              <p
                  class="
                  edit-box__state-description
                "
              >
                {{ worldLoadError }}
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
              class="new-session__options"
              role="listbox"
              aria-label="Available worlds"
          >
            <button
                v-for="world in filteredWorlds"
                :key="world.hashKey()"
                type="button"
                class="new-session__option"
                :class="{
                'new-session__option--selected':
                  isSelectedWorld(world),
              }"
                role="option"
                :aria-selected="
                isSelectedWorld(world)
              "
                @click="selectWorld(world)"
            >
              <span
                  class="
                  new-session__option-icon
                "
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

              <span
                  class="
                  new-session__option-main
                "
              >
                <span
                    class="
                    new-session__option-name
                  "
                >
                  {{ getWorldName(world) }}
                </span>

                <span
                    class="
                    new-session__option-id
                  "
                >
                  World ID
                  {{ getEntityId(world) }}
                </span>
              </span>

              <span
                  v-if="isSelectedWorld(world)"
                  class="
                  new-session__option-check
                "
                  aria-hidden="true"
              >
                <svg viewBox="0 0 24 24">
                  <path d="m5 12 4 4L19 6" />
                </svg>
              </span>
            </button>
          </div>

          <div
              v-else-if="
              worlds.length &&
              worldSearch.trim()
            "
              class="
              edit-box__state
              edit-box__state--vertical
              new-session__selection-state
            "
          >
            <div
                class="edit-box__state-content"
            >
              <strong
                  class="edit-box__state-title"
              >
                No matching worlds
              </strong>

              <p
                  class="
                  edit-box__state-description
                "
              >
                No world matches
                “{{ worldSearch.trim() }}”.
              </p>
            </div>

            <button
                type="button"
                class="edit-box__action"
                @click="worldSearch = ''"
            >
              Clear search
            </button>
          </div>

          <div
              v-else
              class="
              edit-box__state
              edit-box__state--vertical
              new-session__selection-state
            "
          >
            <div
                class="edit-box__state-content"
            >
              <strong
                  class="edit-box__state-title"
              >
                No worlds available
              </strong>

              <p
                  class="
                  edit-box__state-description
                "
              >
                Create or import a world before
                starting a session.
              </p>
            </div>
          </div>
        </section>

        <!-- Character selection -->
        <section
            class="
            edit-box__section
            edit-box__section--accent
            new-session__section
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
              <span class="edit-box__eyebrow">
                Step 2
              </span>

              <h3
                  class="
                  edit-box__section-title
                "
              >
                Select your character
              </h3>

              <p
                  class="
                  edit-box__section-description
                "
              >
                Only characters marked as usable
                by the player are displayed.
              </p>
            </div>

            <span class="edit-box__count">
              {{ characters.length }}
            </span>
          </header>

          <template v-if="selectedWorld">
            <div
                class="
                new-session__selected-world
              "
            >
              <span>World</span>

              <strong>
                {{
                  getWorldName(
                      selectedWorld,
                  )
                }}
              </strong>
            </div>

            <div
                v-if="
                characters.length > 5 ||
                characterSearch.trim()
              "
                class="
                new-session__selection-search
              "
            >
              <SearchBar
                  v-model:search="
                  characterSearch
                "
                  placeholder="Search characters by name or ID"
                  aria-label="Search characters"
              />
            </div>

            <div
                v-if="loadingCharacters"
                class="
                edit-box__state
                new-session__selection-state
              "
                role="status"
                aria-live="polite"
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
                  Loading characters
                </strong>

                <p
                    class="
                    edit-box__state-description
                  "
                >
                  Finding eligible starting
                  characters in this world.
                </p>
              </div>
            </div>

            <div
                v-else-if="
                characterLoadError
              "
                class="
                edit-box__state
                edit-box__state--error
                edit-box__state--vertical
                new-session__selection-state
              "
                role="alert"
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
                  Could not load characters
                </strong>

                <p
                    class="
                    edit-box__state-description
                  "
                >
                  {{ characterLoadError }}
                </p>
              </div>

              <button
                  type="button"
                  class="
                  edit-box__action
                  edit-box__action--accent
                "
                  @click="
                  loadCharacters(
                    selectedWorld,
                  )
                "
              >
                Retry
              </button>
            </div>

            <div
                v-else-if="
                filteredCharacters.length
              "
                class="new-session__options"
                role="listbox"
                aria-label="Available user characters"
            >
              <button
                  v-for="
                  character in
                  filteredCharacters
                "
                  :key="character.hashKey()"
                  type="button"
                  class="new-session__option"
                  :class="{
                  'new-session__option--selected':
                    isSelectedCharacter(
                      character,
                    ),
                }"
                  role="option"
                  :aria-selected="
                  isSelectedCharacter(
                    character,
                  )
                "
                  @click="
                  selectCharacter(character)
                "
              >
                <span
                    class="
                    new-session__option-icon
                  "
                    aria-hidden="true"
                >
                  <svg
                      viewBox="0 0 24 24"
                  >
                    <circle
                        cx="12"
                        cy="8"
                        r="4"
                    />

                    <path
                        d="M4 21a8 8 0 0 1 16 0"
                    />
                  </svg>
                </span>

                <span
                    class="
                    new-session__option-main
                  "
                >
                  <span
                      class="
                      new-session__option-name
                    "
                  >
                    {{
                      getCharacterName(
                          character,
                      )
                    }}
                  </span>

                  <span
                      class="
                      new-session__option-id
                    "
                  >
                    Character ID
                    {{
                      getEntityId(
                          character,
                      )
                    }}
                  </span>
                </span>

                <span
                    v-if="
                    isSelectedCharacter(
                      character,
                    )
                  "
                    class="
                    new-session__option-check
                  "
                    aria-hidden="true"
                >
                  <svg
                      viewBox="0 0 24 24"
                  >
                    <path
                        d="m5 12 4 4L19 6"
                    />
                  </svg>
                </span>
              </button>
            </div>

            <div
                v-else-if="
                characters.length &&
                characterSearch.trim()
              "
                class="
                edit-box__state
                edit-box__state--vertical
                new-session__selection-state
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
                  No matching characters
                </strong>

                <p
                    class="
                    edit-box__state-description
                  "
                >
                  No character matches
                  “{{
                    characterSearch.trim()
                  }}”.
                </p>
              </div>

              <button
                  type="button"
                  class="edit-box__action"
                  @click="
                  characterSearch = ''
                "
              >
                Clear search
              </button>
            </div>

            <div
                v-else
                class="
                edit-box__state
                edit-box__state--vertical
                new-session__selection-state
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
                  No eligible characters
                </strong>

                <p
                    class="
                    edit-box__state-description
                  "
                >
                  This world has no starting
                  characters marked as usable by
                  the player.
                </p>
              </div>
            </div>
          </template>

          <div
              v-else
              class="
              edit-box__state
              edit-box__state--vertical
              new-session__selection-state
            "
          >
            <div class="edit-box__state-icon">
              <svg
                  viewBox="0 0 24 24"
                  aria-hidden="true"
              >
                <circle
                    cx="12"
                    cy="8"
                    r="4"
                />

                <path
                    d="M4 21a8 8 0 0 1 16 0"
                />
              </svg>
            </div>

            <div
                class="edit-box__state-content"
            >
              <strong
                  class="edit-box__state-title"
              >
                Select a world first
              </strong>

              <p
                  class="
                  edit-box__state-description
                "
              >
                Character availability depends on
                the selected world.
              </p>
            </div>
          </div>
        </section>

        <div
            v-if="
            submitAttempted &&
            validationIssues.length
          "
            class="
            edit-box__state
            edit-box__state--error
            new-session__validation
          "
            role="alert"
        >
          <div class="edit-box__state-content">
            <strong class="edit-box__state-title">
              Complete the session setup
            </strong>

            <ul
                class="
                new-session__validation-list
              "
            >
              <li
                  v-for="
                  issue in validationIssues
                "
                  :key="issue"
              >
                {{ issue }}
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="new-session__footer">
        <button
            type="button"
            class="edit-box__action"
            @click="closePopup"
        >
          Cancel
        </button>

        <button
            type="button"
            class="
            edit-box__action
            edit-box__action--accent
          "
            :disabled="
            loadingWorlds ||
            loadingCharacters
          "
            @click="createNewSession"
        >
          <svg
              viewBox="0 0 24 24"
              aria-hidden="true"
          >
            <path
                d="M4 5h16v11H8l-4 4V5Z"
            />

            <path d="M12 8v5" />
            <path d="M9.5 10.5h5" />
          </svg>

          Create session
        </button>
      </div>
    </template>
  </WindowPrompt>
</template>

<style scoped>
.new-session {
  width: min(100%, 46rem);
  min-width: 0;

  display: flex;
  flex-direction: column;
  gap: var(--space-4);

  color: rgb(var(--c-fg));
  font-family: var(--font-primary);
}

/* -------------------------------------------------------------------------- */
/* Introduction                                                               */
/* -------------------------------------------------------------------------- */

.new-session__intro {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);

  min-width: 0;

  padding:
      var(--space-2)
      var(--space-1)
      var(--space-3);

  border-bottom:
      1px solid
      rgb(var(--c-border) / 0.22);
}

.new-session__intro-icon {
  width: 2.7rem;
  height: 2.7rem;
  flex: 0 0 auto;

  display: grid;
  place-items: center;

  color: rgb(var(--c-primary-strong));

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-accent) / 0.22),
          rgb(var(--c-primary) / 0.1)
      );

  border:
      1px solid
      rgb(var(--c-accent) / 0.34);
  border-radius: var(--radius-md);

  box-shadow:
      inset 0 1px 0
      rgb(255 255 255 / 0.32);
}

.new-session__intro-icon svg {
  width: 1.35rem;
  height: 1.35rem;

  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.new-session__intro-main {
  min-width: 0;
}

.new-session__intro-main
.edit-box__eyebrow {
  margin: 0 0 var(--space-1);
}

.new-session__intro-title {
  margin: 0;

  color: rgb(var(--c-fg-strong));

  font-size: 1.05rem;
  font-weight: 850;
  line-height: 1.3;
}

.new-session__intro-description {
  max-width: 38rem;

  margin:
      var(--space-1)
      0
      0;

  color: rgb(var(--c-muted));

  font-size: 0.78rem;
  line-height: 1.5;
}

/* -------------------------------------------------------------------------- */
/* Fields                                                                     */
/* -------------------------------------------------------------------------- */

.new-session__fields {
  gap: var(--space-3);
}

.new-session__section {
  min-width: 0;
}

.new-session__selection-search {
  margin-bottom: var(--space-2);
}

.new-session__selection-state {
  min-height: 8rem;
}

.new-session__selected-world {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);

  margin-bottom: var(--space-2);
  padding:
      var(--space-2)
      var(--space-3);

  color: rgb(var(--c-muted));

  background:
      rgb(var(--c-accent) / 0.07);

  border:
      1px solid
      rgb(var(--c-accent) / 0.2);
  border-radius: var(--radius-sm);

  font-size: 0.72rem;
  line-height: 1.35;
}

.new-session__selected-world strong {
  min-width: 0;

  overflow: hidden;

  color: rgb(var(--c-primary-strong));

  font-size: 0.76rem;
  font-weight: 800;

  text-overflow: ellipsis;
  white-space: nowrap;
}

/* -------------------------------------------------------------------------- */
/* Selection options                                                          */
/* -------------------------------------------------------------------------- */

.new-session__options {
  display: grid;
  grid-template-columns:
    repeat(
      auto-fit,
      minmax(13rem, 1fr)
    );

  gap: var(--space-2);

  max-height: 13.5rem;

  overflow-y: auto;
  overscroll-behavior: contain;

  padding-right: var(--space-1);

  scrollbar-width: thin;
  scrollbar-color:
      rgb(var(--c-primary) / 0.42)
      transparent;
}

.new-session__options::-webkit-scrollbar {
  width: 0.65rem;
}

.new-session__options::-webkit-scrollbar-track {
  background: transparent;
}

.new-session__options::-webkit-scrollbar-thumb {
  background:
      rgb(var(--c-primary) / 0.36);

  border: 2px solid transparent;
  border-radius: var(--radius-round);
  background-clip: padding-box;
}

.new-session__option {
  min-width: 0;

  display: flex;
  align-items: center;
  gap: var(--space-2);

  padding: var(--space-2);

  color: rgb(var(--c-fg));

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-surface-raised) / 0.56),
          rgb(var(--c-surface-2) / 0.3)
      );

  border:
      1px solid
      rgb(var(--c-border) / 0.24);
  border-radius: var(--radius-sm);
  outline: 0;

  font: inherit;
  text-align: left;

  cursor: pointer;

  box-shadow:
      inset 0 1px 0
      rgb(255 255 255 / 0.24);

  transition:
      background-color
      var(--duration-fast)
      var(--ease-standard),
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

.new-session__option:hover {
  background:
      linear-gradient(
          145deg,
          rgb(var(--c-surface-hover) / 0.82),
          rgb(var(--c-accent) / 0.08)
      );

  border-color:
      rgb(var(--c-primary) / 0.44);

  transform: translateY(-1px);
}

.new-session__option:focus-visible {
  border-color:
      rgb(var(--c-accent) / 0.68);

  box-shadow:
      0 0 0
      var(--focus-ring-width)
      rgb(var(--focus-ring-color) / 0.18);
}

.new-session__option--selected {
  background:
      linear-gradient(
          145deg,
          rgb(var(--c-accent) / 0.18),
          rgb(var(--c-primary) / 0.08)
      );

  border-color:
      rgb(var(--c-accent) / 0.62);

  box-shadow:
      0 0 0
      1px
      rgb(var(--c-accent) / 0.08),
      inset 0 1px 0
      rgb(255 255 255 / 0.3);
}

.new-session__option-icon {
  width: 2.15rem;
  height: 2.15rem;
  flex: 0 0 auto;

  display: grid;
  place-items: center;

  color: rgb(var(--c-primary-strong));

  background:
      rgb(var(--c-accent) / 0.1);

  border:
      1px solid
      rgb(var(--c-accent) / 0.18);
  border-radius: var(--radius-sm);
}

.new-session__option-icon svg,
.new-session__option-check svg {
  width: 1.05rem;
  height: 1.05rem;

  fill: none;
  stroke: currentColor;
  stroke-width: 1.9;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.new-session__option-main {
  flex: 1 1 auto;
  min-width: 0;

  display: flex;
  flex-direction: column;
  gap: 0.12rem;
}

.new-session__option-name {
  overflow: hidden;

  color: rgb(var(--c-fg-strong));

  font-size: 0.8rem;
  font-weight: 800;
  line-height: 1.3;

  text-overflow: ellipsis;
  white-space: nowrap;
}

.new-session__option-id {
  color: rgb(var(--c-muted));

  font-family: var(--font-monospace);
  font-size: 0.63rem;
  font-weight: 600;
  line-height: 1.3;
}

.new-session__option-check {
  width: 1.6rem;
  height: 1.6rem;
  flex: 0 0 auto;

  display: grid;
  place-items: center;

  color: rgb(var(--c-on-accent));

  background: rgb(var(--c-accent));

  border-radius: var(--radius-round);
}

/* -------------------------------------------------------------------------- */
/* Validation                                                                 */
/* -------------------------------------------------------------------------- */

.new-session__validation {
  min-height: auto;
  justify-content: flex-start;

  padding: var(--space-3);

  text-align: left;
}

.new-session__validation-list {
  margin:
      var(--space-1)
      0
      0;
  padding-left: var(--space-5);

  color: rgb(var(--c-muted));

  font-size: 0.75rem;
  line-height: 1.5;
}

/* -------------------------------------------------------------------------- */
/* Footer                                                                     */
/* -------------------------------------------------------------------------- */

.new-session__footer {
  width: 100%;

  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-2);
}

.new-session__footer
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

/* -------------------------------------------------------------------------- */
/* Responsive                                                                 */
/* -------------------------------------------------------------------------- */

@media (max-width: 620px) {
  .new-session__options {
    grid-template-columns: 1fr;
    max-height: 12rem;
  }
}

@media (max-width: 440px) {
  .new-session__intro {
    align-items: center;
  }

  .new-session__intro-description {
    display: none;
  }

  .new-session__footer {
    align-items: stretch;
    flex-direction: column-reverse;
  }

  .new-session__footer
  .edit-box__action {
    width: 100%;
  }
}

@media (prefers-reduced-motion: reduce) {
  .new-session__option {
    transition: none;
  }

  .new-session__option:hover {
    transform: none;
  }
}
</style>