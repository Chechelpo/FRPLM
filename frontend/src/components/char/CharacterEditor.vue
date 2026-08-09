<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";

import { Character } from "@/domain/Characters";
import { Lorebook } from "@/domain/Lorebook";
import { Tag } from "@/domain/Tag";

import ShortTextBox from "@/components/primitive-editors/ShortTextBox.vue";
import BooleanToggle from "@/components/primitive-editors/BooleanToggle.vue";
import LongTextBox from "@/components/primitive-editors/LongTextBox.vue";
import LorebookEditor from "@/components/lorebooks/LorebookEditor.vue";
import FieldEditorWrapper from "@/components/utils/FieldEditorWrapper.vue";
import Expandable from "@/components/utils/panels/Expandable.vue";

// -----------------------------------------------------------------------------
// Model and properties
// -----------------------------------------------------------------------------

const model = defineModel<Character>({
  required: true,
});

const props = withDefaults(
    defineProps<{
      showBackButton?: boolean;
    }>(),
    {
      showBackButton: false,
    },
);

const emit = defineEmits<{
  back: [];
}>();

// -----------------------------------------------------------------------------
// Character data
// -----------------------------------------------------------------------------

const embedLorebook = ref<Lorebook>();
const characterTags = ref<Tag[]>([]);
const isLoadingCharacter = ref(false);

// -----------------------------------------------------------------------------
// Loading
// -----------------------------------------------------------------------------

async function loadCharacter(character: Character): Promise<void> {
  isLoadingCharacter.value = true;

  try {
    console.info(`Editing character ${character}`);

    const [tags, lorebook] = await Promise.all([
      character.getTags(),
      character.getLorebook(),
    ]);

    /*
     * Ignore results belonging to a character that is no longer selected.
     */
    if (!model.value.equals(character)) {
      return;
    }

    characterTags.value = tags;
    embedLorebook.value = lorebook;

    console.debug("Loaded character editor data:", {
      character,
      tags,
      lorebook,
    });
  } finally {
    if (model.value.equals(character)) {
      isLoadingCharacter.value = false;
    }
  }
}

onMounted(async () => {
  await loadCharacter(model.value);
});

watch(model, async newValue => {
  embedLorebook.value = undefined;
  characterTags.value = [];

  await loadCharacter(newValue);
});

// -----------------------------------------------------------------------------
// Editable attributes
// -----------------------------------------------------------------------------

const name = computed<string>({
  get() {
    return model.value.get("name");
  },

  set(value: string) {
    model.value.update("name", value);
  },
});

const description = computed<string>({
  get() {
    return model.value.get("description");
  },

  set(value: string) {
    model.value.update("description", value);
  },
});

const canBeUser = computed<boolean>({
  get() {
    return model.value.get("can_be_user");
  },

  set(value: boolean) {
    model.value.update("can_be_user", value);
  },
});

// -----------------------------------------------------------------------------
// Tags
// -----------------------------------------------------------------------------

async function handleNewTag(tag: Tag): Promise<void> {
  console.debug(`Adding tag ${tag} to character ${model.value}`);

  await model.value.addTag(tag);

  const exists = characterTags.value.some(existingTag =>
      existingTag.equals(tag),
  );

  if (!exists) {
    characterTags.value = await model.value.getTags();
  }
}

async function handleRemoveTag(tag: Tag): Promise<void> {
  const character = model.value;

  console.debug(`Removing tag ${tag} from character ${character}`);

  await character.removeTag(tag);

  characterTags.value = await character.getTags();
}
</script>

<template>
  <article class="edit-box edit-box--accent character-editor">
    <button
        v-if="props.showBackButton"
        type="button"
        class="character-editor__back"
        @click="emit('back')"
    >
      <span aria-hidden="true">\u2190</span>
      Back to location
    </button>

    <!-- Character header -->
    <header class="edit-box__header">
      <div class="edit-box__header-icon">
        <svg
            viewBox="0 0 24 24"
            aria-hidden="true"
        >
          <circle
              cx="12"
              cy="8"
              r="4"
          />

          <path d="M4 21a8 8 0 0 1 16 0" />
        </svg>
      </div>

      <div class="edit-box__header-main">
        <span class="edit-box__eyebrow">
          Character configuration
        </span>

        <div class="edit-box__title-row">
          <h2 class="edit-box__title">
            {{ name || "Unnamed character" }}
          </h2>

          <span
              class="edit-box__badge"
              :class="{
              'edit-box__badge--success': canBeUser,
              'edit-box__badge--neutral': !canBeUser,
            }"
          >
            {{ canBeUser ? "Playable" : "NPC only" }}
          </span>
        </div>

        <p class="edit-box__description">
          Configure the character identity, description, player availability,
          opening message, and embedded lore.
        </p>
      </div>
    </header>

    <div class="edit-box__body character-editor__body">
      <!-- Primary attributes -->
      <section class="edit-box__section edit-box__section--accent">
        <header class="edit-box__section-header">
          <div class="edit-box__section-heading">
            <h3 class="edit-box__section-title">
              Identity and behavior
            </h3>

            <p class="edit-box__section-description">
              Core attributes used when constructing prompts and sessions.
            </p>
          </div>
        </header>

        <div class="character-editor__field-grid">
          <div class="character-editor__field character-editor__field--name">
            <FieldEditorWrapper
                field-name="Name"
                info="Character's name, included in the generated prompt"
            >
              <ShortTextBox
                  v-model="name"
                  @edit="value => (name = value)"
              />
            </FieldEditorWrapper>
          </div>

          <div class="character-editor__field character-editor__field--toggle">
            <FieldEditorWrapper
                field-name="Can be user"
                info="Determines whether the user may play this character"
            >
              <BooleanToggle
                  :model-value="canBeUser"
                  @edit="value => (canBeUser = value)"
              />
            </FieldEditorWrapper>
          </div>

          <div
              class="
              character-editor__field
              character-editor__field--full
              character-editor__description-field
            "
          >
            <FieldEditorWrapper
                field-name="Description"
                info="Describes the character's identity, appearance, personality, and behavior"
                :vertical="true"
            >
              <LongTextBox
                  :model-value="description"
                  @edit="value => (description = value)"
                  tokenize
                  :tokenization-started="true"
              />
            </FieldEditorWrapper>
          </div>

          <Transition name="character-field">
            <div
                v-if="canBeUser"
                class="
                character-editor__field
                character-editor__field--full
                character-editor__first-message
              "
            >
              <FieldEditorWrapper
                  field-name="First message"
                  info="Initial message shown when a session starts with this character"
                  :vertical="true"
              >
                <LongTextBox
                    :model-value="model.get('firstMessage')"
                    @edit="payload => model.update('firstMessage', payload)"
                    tokenize
                    :tokenization-started="true"
                />
              </FieldEditorWrapper>
            </div>
          </Transition>
        </div>
      </section>

      <!-- Character lorebook -->
      <section class="edit-box__section character-editor__expandable-section">
        <Expandable
            title="Lorebook"
            info="Activated whenever this character is present in the current location"
            :initially-open="false"
        >
          <div class="character-editor__expanded-content">
            <LorebookEditor
                v-if="embedLorebook"
                v-model="embedLorebook"
            />

            <div
                v-else-if="isLoadingCharacter"
                class="edit-box__state"
                aria-live="polite"
            >
              <span class="edit-box__spinner" />

              <div class="edit-box__state-content">
                <strong class="edit-box__state-title">
                  Loading character lorebook
                </strong>

                <p class="edit-box__state-description">
                  Retrieving the embedded lore entries for this character.
                </p>
              </div>
            </div>

            <div
                v-else
                class="edit-box__state edit-box__state--vertical"
            >
              <div class="edit-box__state-icon">
                <svg
                    viewBox="0 0 24 24"
                    aria-hidden="true"
                >
                  <path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H20v16H6.5A2.5 2.5 0 0 0 4 21.5Z" />
                  <path d="M4 5.5v16" />
                </svg>
              </div>

              <div class="edit-box__state-content">
                <strong class="edit-box__state-title">
                  No lorebook available
                </strong>

                <p class="edit-box__state-description">
                  This character does not currently have an embedded lorebook.
                </p>
              </div>
            </div>
          </div>
        </Expandable>
      </section>

    </div>
  </article>
</template>

<style scoped>
.character-editor {
  width: 100%;
  min-width: 0;
}

.character-editor__back {
  align-self: flex-start;
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-3);
  padding: var(--space-2) var(--space-3);
  border: 1px solid rgb(var(--c-border));
  border-radius: var(--radius-md);
  background: rgb(var(--c-surface-raised));
  color: rgb(var(--c-fg));
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}

.character-editor__back:hover {
  border-color: rgb(var(--c-accent));
  color: rgb(var(--c-fg-strong));
}

.character-editor__body {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

/* -------------------------------------------------------------------------- */
/* Primary field layout                                                       */
/* -------------------------------------------------------------------------- */

.character-editor__field-grid {
  display: grid;
  grid-template-columns:
    minmax(16rem, 2fr)
    minmax(12rem, 1fr);

  align-items: start;
  gap: var(--space-3);

  min-width: 0;
}

.character-editor__field {
  min-width: 0;
  height: 100%;

  padding: var(--space-3);

  background:
      linear-gradient(
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

.character-editor__field:hover {
  border-color: rgb(var(--c-primary) / 0.27);
}

.character-editor__field:focus-within {
  background: rgb(var(--c-surface-raised) / 0.62);
  border-color: rgb(var(--c-accent) / 0.42);

  box-shadow:
      0 0 0 3px rgb(var(--c-accent) / 0.09),
      inset 0 1px 0 rgb(255 255 255 / 0.32);
}

.character-editor__field--toggle {
  display: flex;
  align-items: center;
}

.character-editor__field--full {
  grid-column: 1 / -1;
}

.character-editor__description-field {
  background:
      linear-gradient(
          145deg,
          rgb(var(--c-primary) / 0.04),
          rgb(var(--c-surface-raised) / 0.48)
      );
}

.character-editor__first-message {
  border-color: rgb(var(--c-accent) / 0.2);

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-accent) / 0.055),
          rgb(var(--c-surface-raised) / 0.5)
      );
}

/* Ensure primitive editors use the full available width. */
.character-editor__field :deep(input),
.character-editor__field :deep(textarea) {
  width: 100%;
  min-width: 0;
  box-sizing: border-box;
}

.character-editor__field :deep(textarea) {
  min-height: 8rem;
  resize: vertical;
}

/* -------------------------------------------------------------------------- */
/* Expandable sections                                                        */
/* -------------------------------------------------------------------------- */

.character-editor__expandable-section {
  padding: 0;
  overflow: hidden;
}

.character-editor__expanded-content {
  min-width: 0;
  padding: var(--space-3);
}

.character-editor__expandable-section :deep(> div) {
  min-width: 0;
}

.character-editor__expandable-section :deep(button) {
  font-family: var(--font-primary);
}

.character-editor__expandable-section :deep(input),
.character-editor__expandable-section :deep(textarea),
.character-editor__expandable-section :deep(select) {
  max-width: 100%;
  box-sizing: border-box;
}

/* -------------------------------------------------------------------------- */
/* Conditional field transition                                               */
/* -------------------------------------------------------------------------- */

.character-field-enter-active,
.character-field-leave-active {
  transition:
      opacity var(--duration-normal) var(--ease-standard),
      transform var(--duration-normal) var(--ease-standard);
}

.character-field-enter-from,
.character-field-leave-to {
  opacity: 0;
  transform: translateY(-0.4rem);
}

/* -------------------------------------------------------------------------- */
/* Responsive layout                                                          */
/* -------------------------------------------------------------------------- */

@media (max-width: 760px) {
  .character-editor__field-grid {
    grid-template-columns: 1fr;
  }

  .character-editor__field--full {
    grid-column: auto;
  }
}

@media (max-width: 480px) {
  .character-editor__field {
    padding: var(--space-2);
  }

  .character-editor__expanded-content {
    padding: var(--space-2);
  }
}

@media (prefers-reduced-motion: reduce) {
  .character-editor__field,
  .character-field-enter-active,
  .character-field-leave-active {
    transition: none;
  }
}
</style>
