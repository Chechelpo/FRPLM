<script setup lang="ts">
import {     ShortTextBox,
    BooleanToggle,
    LongTextBox } from "@frplm/ui";

import {     Character,
    Lorebook } from "@frplm/host-sdk";

import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";


import LorebookEditor from "@components/lorebooks/LorebookEditor.vue";
import FieldEditorWrapper from "@components/utils/FieldEditorWrapper.vue";
import Expandable from "@components/utils/panels/Expandable.vue";

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
const isLoadingCharacter = ref(false);
const lorebookError = ref<string | null>(null);
const avatarInput = ref<HTMLInputElement | null>(null);
const avatarUrl = ref<string | null>(null);
const loadingAvatar = ref(false);
const savingAvatar = ref(false);
const avatarError = ref<string | null>(null);
let avatarRequestId = 0;

const avatarBusy = computed(
    () => loadingAvatar.value || savingAvatar.value,
);

const characterName = computed(() => {
  const value = String(model.value.get("name") ?? "").trim();
  return value || "Unnamed character";
});

// -----------------------------------------------------------------------------
// Loading
// -----------------------------------------------------------------------------

async function loadCharacter(character: Character): Promise<void> {
  isLoadingCharacter.value = true;
  lorebookError.value = null;

  try {
    console.info(`Editing character ${character}`);

    const lorebook = await character.getLorebook();

    /*
     * Ignore results belonging to a character that is no longer selected.
     */
    if (!model.value.equals(character)) {
      return;
    }

    embedLorebook.value = lorebook;

    console.debug("Loaded character editor data:", {
      character,
      lorebook,
    });
  } catch (error) {
    if (!model.value.equals(character)) {
      return;
    }

    console.error("Could not load character lorebook", error);
    embedLorebook.value = undefined;
    lorebookError.value = "The character lorebook could not be loaded.";
  } finally {
    if (model.value.equals(character)) {
      isLoadingCharacter.value = false;
    }
  }
}

function replaceAvatarUrl(blob: Blob | null): void {
  if (avatarUrl.value) {
    URL.revokeObjectURL(avatarUrl.value);
  }

  avatarUrl.value = blob ? URL.createObjectURL(blob) : null;
}

async function loadAvatar(character: Character): Promise<void> {
  const requestId = ++avatarRequestId;
  loadingAvatar.value = true;
  avatarError.value = null;

  try {
    const avatar = await character.fetchAvatar();

    if (requestId === avatarRequestId) {
      replaceAvatarUrl(avatar);
    }
  } catch (error) {
    if (requestId !== avatarRequestId) {
      return;
    }

    console.error("Could not load character avatar", error);
    replaceAvatarUrl(null);
    avatarError.value = "The character avatar could not be loaded.";
  } finally {
    if (requestId === avatarRequestId) {
      loadingAvatar.value = false;
    }
  }
}

function openAvatarPicker(): void {
  if (!avatarBusy.value) {
    avatarInput.value?.click();
  }
}

async function onAvatarSelected(event: Event): Promise<void> {
  const input = event.currentTarget as HTMLInputElement;
  const file = input.files?.[0];
  input.value = "";

  if (!file) {
    return;
  }

  if (!file.type.startsWith("image/")) {
    avatarError.value = "Choose a valid image file.";
    return;
  }

  const character = model.value;
  const requestId = ++avatarRequestId;
  savingAvatar.value = true;
  avatarError.value = null;

  try {
    await character.saveAvatar(file, true);

    if (requestId === avatarRequestId) {
      replaceAvatarUrl(file);
    }
  } catch (error) {
    if (requestId !== avatarRequestId) {
      return;
    }

    console.error("Could not save character avatar", error);
    avatarError.value = "The character avatar could not be saved.";
  } finally {
    if (requestId === avatarRequestId) {
      savingAvatar.value = false;
    }
  }
}

async function removeAvatar(): Promise<void> {
  if (!avatarUrl.value || avatarBusy.value) {
    return;
  }

  const character = model.value;
  const requestId = ++avatarRequestId;
  savingAvatar.value = true;
  avatarError.value = null;

  try {
    await character.deleteAvatar();

    if (requestId === avatarRequestId) {
      replaceAvatarUrl(null);
    }
  } catch (error) {
    if (requestId !== avatarRequestId) {
      return;
    }

    console.error("Could not delete character avatar", error);
    avatarError.value = "The character avatar could not be removed.";
  } finally {
    if (requestId === avatarRequestId) {
      savingAvatar.value = false;
    }
  }
}

onMounted(() => {
  const character = model.value;
  void loadCharacter(character);
  void loadAvatar(character);
});

watch(model, async newValue => {
  embedLorebook.value = undefined;
  lorebookError.value = null;
  ++avatarRequestId;
  replaceAvatarUrl(null);
  loadingAvatar.value = false;
  savingAvatar.value = false;
  avatarError.value = null;

  await Promise.all([
    loadCharacter(newValue),
    loadAvatar(newValue),
  ]);
});

onBeforeUnmount(() => {
  ++avatarRequestId;
  replaceAvatarUrl(null);
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

const welcomeMessage = computed<string | null>({
  get() {
    return model.value.get("welcome_message");
  },

  set(value: string | null) {
    const normalizedValue = value?.trim() ? value : null;
    model.value.update("welcome_message", normalizedValue);
  },
});

const firstMessage = computed<string>({
  get() {
    return model.value.get("welcome_message") ?? "";
  },

  set(value: string) {
    model.value.update("welcome_message", value);
  },
});

</script>

<template>
  <article class="edit-box edit-box--accent character-editor">
    <nav
        v-if="props.showBackButton"
        class="character-editor__navigation"
        aria-label="Character editor navigation"
    >
      <button
          type="button"
          class="character-editor__back"
          aria-label="Back to location editor"
          @click="emit('back')"
      >
        <span class="character-editor__back-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24">
            <path d="m14.5 5-7 7 7 7" />
          </svg>
        </span>

        <span class="character-editor__back-copy">
          <span class="character-editor__back-kicker">Return to</span>
          <strong>Location editor</strong>
        </span>
      </button>

      <span class="character-editor__navigation-current">
        Editing character
      </span>
    </nav>

    <header class="character-editor__identity-card">
      <input
          ref="avatarInput"
          class="character-editor__file-input"
          type="file"
          accept="image/*"
          :disabled="avatarBusy"
          aria-label="Choose character avatar"
          @change="onAvatarSelected"
      />

      <button
          type="button"
          class="character-editor__avatar"
          :class="{
            'character-editor__avatar--empty': !avatarUrl,
            'character-editor__avatar--busy': avatarBusy,
          }"
          :disabled="avatarBusy"
          :aria-label="avatarUrl ? 'Replace character avatar' : 'Add character avatar'"
          @click="openAvatarPicker"
      >
        <img
            v-if="avatarUrl"
            :src="avatarUrl"
            :alt="`${characterName} avatar`"
        />

        <span
            v-else
            class="character-editor__avatar-placeholder"
            aria-hidden="true"
        >
          <svg viewBox="0 0 24 24">
            <circle cx="12" cy="8" r="4" />
            <path d="M4 21a8 8 0 0 1 16 0" />
          </svg>
          <span>{{ loadingAvatar ? "Loading…" : "Add avatar" }}</span>
        </span>

        <span
            v-if="savingAvatar"
            class="character-editor__avatar-saving"
            role="status"
        >
          Saving…
        </span>
      </button>

      <div class="character-editor__identity-main">
        <div class="character-editor__identity-heading">
          <span class="character-editor__identity-eyebrow">
            Character profile
          </span>

          <span
              class="edit-box__badge character-editor__playable-badge"
              :class="{
                'edit-box__badge--success': canBeUser,
                'edit-box__badge--neutral': !canBeUser,
              }"
          >
            {{ canBeUser ? "Playable" : "NPC only" }}
          </span>
        </div>

        <div
            class="character-editor__avatar-actions"
            aria-label="Avatar controls"
        >
          <button
              type="button"
              :disabled="avatarBusy"
              :title="avatarUrl ? 'Replace avatar' : 'Upload avatar'"
              @click="openAvatarPicker"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M12 16V4" />
              <path d="m7 9 5-5 5 5" />
              <path d="M5 20h14" />
            </svg>
            <span>{{ avatarUrl ? "Replace" : "Upload" }}</span>
          </button>

          <button
              v-if="avatarUrl"
              type="button"
              class="character-editor__remove-avatar"
              :disabled="avatarBusy"
              title="Delete avatar"
              @click="removeAvatar"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M4 7h16" />
              <path d="m9 7 .8-3h4.4L15 7" />
              <path d="m6.5 7 .8 13h9.4l.8-13" />
              <path d="M10 11v5M14 11v5" />
            </svg>
            <span>Delete</span>
          </button>

        </div>

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

      <p
          v-if="avatarError"
          class="character-editor__avatar-error"
          role="alert"
      >
        {{ avatarError }}
      </p>
    </header>

    <div class="edit-box__body character-editor__body">
      <!-- Identity and availability -->
      <section class="edit-box__section edit-box__section--accent">
        <header class="edit-box__section-header">
          <div class="edit-box__section-heading">
            <h3 class="edit-box__section-title">
              Identity and availability
            </h3>

            <p class="edit-box__section-description">
              Define the character and whether it can represent the user.
            </p>
          </div>
        </header>

        <div class="character-editor__field-grid">
          <div
              class="
                character-editor__field
                character-editor__field--toggle
                character-editor__field--full
              "
          >
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
        </div>
      </section>

      <!-- Session opening text -->
      <section class="edit-box__section character-editor__messages-section">
        <header class="edit-box__section-header">
          <div class="edit-box__section-heading">
            <h3 class="edit-box__section-title">
              Conversation opening
            </h3>

            <p class="edit-box__section-description">
              Configure the text used when a session begins.
            </p>
          </div>
        </header>

        <div class="character-editor__field-grid">
          <Transition name="character-field">
            <div
                v-if="canBeUser"
                class="
                  character-editor__field
                  character-editor__field--full
                  character-editor__welcome-message
                "
            >
              <FieldEditorWrapper
                  field-name="Welcome message"
                  info="Optional message shown when this playable character starts a session"
                  :vertical="true"
              >
                <LongTextBox
                    v-model="welcomeMessage"
                    @edit="value => (welcomeMessage = value)"
                    tokenize
                    :tokenization-started="true"
                />
              </FieldEditorWrapper>

              <p class="character-editor__field-note">
                Optional. Leaving this empty stores no welcome message.
              </p>
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
                v-else-if="lorebookError"
                class="edit-box__state edit-box__state--error"
                role="alert"
            >
              <div class="edit-box__state-icon">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <circle cx="12" cy="12" r="9" />
                  <path d="M12 7v6M12 17h.01" />
                </svg>
              </div>

              <div class="edit-box__state-content">
                <strong class="edit-box__state-title">
                  Lorebook unavailable
                </strong>

                <p class="edit-box__state-description">
                  {{ lorebookError }}
                </p>
              </div>
            </div>

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
  container-type: inline-size;
}

.character-editor__navigation {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  min-width: 0;
  padding: var(--space-3) var(--space-4);
  background:
      linear-gradient(
          90deg,
          rgb(var(--c-surface-raised) / 0.82),
          rgb(var(--c-surface-2) / 0.44)
      );
  border-bottom: 1px solid rgb(var(--c-border) / 0.24);
}

.character-editor__back {
  display: inline-flex;
  align-items: center;
  gap: var(--space-3);
  min-width: 0;
  padding: var(--space-2) var(--space-3) var(--space-2) var(--space-2);
  border: 1px solid rgb(var(--c-border) / 0.34);
  border-radius: var(--radius-md);
  background: rgb(var(--c-surface-raised) / 0.76);
  color: rgb(var(--c-fg));
  font: inherit;
  text-align: left;
  cursor: pointer;

  box-shadow:
      inset 0 1px 0 rgb(255 255 255 / 0.38),
      0 2px 7px rgb(var(--c-shadow) / 0.05);

  transition:
      color var(--duration-fast) var(--ease-standard),
      background-color var(--duration-fast) var(--ease-standard),
      border-color var(--duration-fast) var(--ease-standard),
      transform var(--duration-fast) var(--ease-standard);
}

.character-editor__back:hover {
  border-color: rgb(var(--c-accent) / 0.62);
  background: rgb(var(--c-accent) / 0.1);
  color: rgb(var(--c-fg-strong));
  transform: translateX(-2px);
}

.character-editor__back:focus-visible {
  outline: var(--focus-ring-width) solid rgb(var(--focus-ring-color) / 0.28);
  outline-offset: 2px;
}

.character-editor__back-icon {
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  width: 2rem;
  height: 2rem;
  color: rgb(var(--c-primary-strong));
  background: rgb(var(--c-accent) / 0.16);
  border: 1px solid rgb(var(--c-accent) / 0.25);
  border-radius: var(--radius-sm);
}

.character-editor__back-icon svg {
  width: 1.15rem;
  height: 1.15rem;
  fill: none;
  stroke: currentColor;
  stroke-width: 2.1;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.character-editor__back-copy {
  display: flex;
  flex-direction: column;
  min-width: 0;
  line-height: 1.12;
}

.character-editor__back-copy strong {
  font-size: 0.84rem;
  font-weight: 780;
  white-space: nowrap;
}

.character-editor__back-kicker,
.character-editor__navigation-current {
  color: rgb(var(--c-muted));
  font-size: 0.65rem;
  font-weight: 750;
  text-transform: uppercase;
  letter-spacing: 0.075em;
}

.character-editor__navigation-current {
  flex: 0 1 auto;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* -------------------------------------------------------------------------- */
/* Responsive identity card                                                   */
/* -------------------------------------------------------------------------- */

.character-editor__identity-card {
  display: grid;
  grid-template-columns: clamp(6.5rem, 20cqi, 9rem) minmax(0, 1fr);
  align-items: center;
  gap: clamp(var(--space-3), 3cqi, var(--space-5));

  min-width: 0;
  margin: var(--space-4) var(--space-4) 0;
  padding: clamp(var(--space-3), 3cqi, var(--space-5));

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-primary) / 0.09),
          rgb(var(--c-surface-raised) / 0.68)
      );

  border: 1px solid rgb(var(--c-primary) / 0.25);
  border-radius: var(--radius-lg);

  box-shadow:
      inset 0 1px 0 rgb(255 255 255 / 0.34),
      0 10px 28px rgb(var(--c-shadow) / 0.07);
}

.character-editor__file-input {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.character-editor__avatar {
  position: relative;
  display: block;
  width: 100%;
  aspect-ratio: 1;
  padding: 0;
  overflow: hidden;

  color: rgb(var(--c-muted));
  background: rgb(var(--c-surface-2) / 0.72);

  border: 2px solid rgb(var(--c-primary) / 0.36);
  border-radius: 50%;
  outline: 0;

  box-shadow:
      0 0 0 5px rgb(var(--c-surface-raised) / 0.54),
      0 8px 22px rgb(var(--c-shadow) / 0.13);

  cursor: pointer;

  transition:
      border-color var(--duration-normal) var(--ease-standard),
      box-shadow var(--duration-normal) var(--ease-standard),
      transform var(--duration-normal) var(--ease-standard);
}

.character-editor__avatar:hover:not(:disabled) {
  border-color: rgb(var(--c-accent) / 0.78);
  transform: translateY(-2px);
}

.character-editor__avatar:focus-visible {
  border-color: rgb(var(--c-accent));
  box-shadow:
      0 0 0 5px rgb(var(--c-accent) / 0.15),
      0 8px 22px rgb(var(--c-shadow) / 0.13);
}

.character-editor__avatar:disabled {
  cursor: progress;
}

.character-editor__avatar img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.character-editor__avatar-placeholder {
  position: absolute;
  inset: 0;

  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-1);

  padding: var(--space-2);
  text-align: center;
  font-size: clamp(0.65rem, 2.2cqi, 0.78rem);
  font-weight: 700;
}

.character-editor__avatar-placeholder svg {
  width: clamp(2rem, 7cqi, 3rem);
  fill: none;
  stroke: currentColor;
  stroke-width: 1.7;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.character-editor__avatar-saving {
  position: absolute;
  inset: auto 0 0;
  padding: var(--space-2);

  color: rgb(var(--c-on-accent));
  background: rgb(var(--c-accent) / 0.88);

  font-size: 0.75rem;
  font-weight: 800;
  text-align: center;
}

.character-editor__identity-main {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  min-width: 0;
}

.character-editor__identity-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
  min-width: 0;
}

.character-editor__identity-eyebrow {
  color: rgb(var(--c-primary-strong));
  font-size: 0.68rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.085em;
}

.character-editor__avatar-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-2);
  min-width: 0;
}

.character-editor__avatar-actions button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);

  min-height: 2.25rem;
  padding: var(--space-2) var(--space-3);

  color: rgb(var(--c-fg));
  background: rgb(var(--c-surface-raised) / 0.9);

  border: 1px solid rgb(var(--c-border) / 0.45);
  border-radius: var(--radius-md);

  font: inherit;
  font-size: 0.82rem;
  font-weight: 750;
  cursor: pointer;

  transition:
      background-color var(--duration-normal) var(--ease-standard),
      border-color var(--duration-normal) var(--ease-standard),
      color var(--duration-normal) var(--ease-standard);
}

.character-editor__avatar-actions button:hover:not(:disabled) {
  color: rgb(var(--c-fg-strong));
  background: rgb(var(--c-primary) / 0.12);
  border-color: rgb(var(--c-primary) / 0.52);
}

.character-editor__avatar-actions button:focus-visible {
  outline: var(--focus-ring-width) solid rgb(var(--focus-ring-color) / 0.72);
  outline-offset: 2px;
}

.character-editor__avatar-actions button:disabled {
  opacity: 0.58;
  cursor: progress;
}

.character-editor__avatar-actions button svg {
  flex: 0 0 auto;
  width: 1rem;
  height: 1rem;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.9;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.character-editor__avatar-actions .character-editor__remove-avatar {
  color: rgb(var(--c-danger));
  border-color: rgb(var(--c-danger) / 0.3);
}

.character-editor__avatar-actions .character-editor__remove-avatar:hover:not(:disabled) {
  color: rgb(var(--c-on-danger));
  background: rgb(var(--c-danger) / 0.9);
  border-color: rgb(var(--c-danger));
}

.character-editor__playable-badge {
  flex: 0 0 auto;
}

.character-editor__avatar-error {
  grid-column: 1 / -1;
  margin: 0;
  padding: var(--space-2) var(--space-3);

  color: rgb(var(--c-danger));
  background: rgb(var(--c-danger) / 0.08);
  border: 1px solid rgb(var(--c-danger) / 0.22);
  border-radius: var(--radius-md);

  font-size: 0.86rem;
  font-weight: 650;
}

.character-editor__body {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.character-editor__messages-section {
  border-color: rgb(var(--c-primary) / 0.22);
}

/* -------------------------------------------------------------------------- */
/* Primary field layout                                                       */
/* -------------------------------------------------------------------------- */

.character-editor__field-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);

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

.character-editor__welcome-message {
  border-color: rgb(var(--c-success) / 0.2);

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-success) / 0.045),
          rgb(var(--c-surface-raised) / 0.5)
      );
}

.character-editor__field-note {
  margin: var(--space-2) 0 0;
  color: rgb(var(--c-muted));
  font-size: 0.72rem;
  line-height: 1.45;
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

@container (max-width: 32rem) {
  .character-editor__identity-card {
    grid-template-columns: clamp(5rem, 22cqi, 6.5rem) minmax(0, 1fr);
    gap: var(--space-3);
    padding: var(--space-3);
  }

  .character-editor__avatar-actions button {
    width: 2.25rem;
    padding: var(--space-2);
  }

  .character-editor__avatar-actions button span {
    position: absolute;
    width: 1px;
    height: 1px;
    padding: 0;
    margin: -1px;
    overflow: hidden;
    clip: rect(0, 0, 0, 0);
    white-space: nowrap;
    border: 0;
  }

}

@container (max-width: 22rem) {
  .character-editor__identity-card {
    grid-template-columns: 4.5rem minmax(0, 1fr);
    align-items: start;
  }

  .character-editor__avatar-placeholder span {
    position: absolute;
    width: 1px;
    height: 1px;
    overflow: hidden;
    clip: rect(0, 0, 0, 0);
    white-space: nowrap;
  }

  .character-editor__playable-badge {
    display: none;
  }
}

@media (max-width: 480px) {
  .character-editor__navigation {
    padding: var(--space-2) var(--space-3);
  }

  .character-editor__navigation-current {
    display: none;
  }

  .character-editor__identity-card {
    margin: var(--space-3) var(--space-3) 0;
  }

  .character-editor__field {
    padding: var(--space-2);
  }

  .character-editor__expanded-content {
    padding: var(--space-2);
  }
}

@media (prefers-reduced-motion: reduce) {
  .character-editor__field,
  .character-editor__avatar,
  .character-field-enter-active,
  .character-field-leave-active {
    transition: none;
  }
}
</style>
