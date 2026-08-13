<script setup lang="ts">
import {computed, onBeforeUnmount, onMounted, ref, shallowRef, watch} from "vue";

import type {LocationData} from "@/domain/World";
import {Location} from "@/domain/World";
import {Lorebook} from "@/domain/Lorebook";

import LorebookEditor from "@/components/lorebooks/LorebookEditor.vue";
import LongTextBox from "@/components/primitive-editors/LongTextBox.vue";
import ShortTextBox from "@/components/primitive-editors/ShortTextBox.vue";
import FieldEditorWrapper from "@/components/utils/FieldEditorWrapper.vue";
import Expandable from "@/components/utils/panels/Expandable.vue";
import LocationStartingCharactersEditor from "@/components/char/LocationStartingCharactersEditor.vue";

const props = defineProps<{
  location: Location;
}>();

const lorebook = shallowRef<Lorebook | null>(null);
const backgroundInput = ref<HTMLInputElement | null>(null);
const backgroundUrl = ref<string | null>(null);
const loadingBackground = ref(false);
const savingBackground = ref(false);
const backgroundError = ref<string | null>(null);
const loadingLorebook = ref(false);
const loadError = ref<string | null>(null);
const saveError = ref<string | null>(null);
const savingFields = ref<ReadonlySet<keyof LocationData>>(new Set());
let loadRequestId = 0;
let backgroundRequestId = 0;

const locationName = computed(() => {
  const name = String(props.location.get("name") ?? "").trim();
  return name || "Unnamed location";
});

const isSaving = computed(() => savingFields.value.size > 0);
const backgroundBusy = computed(() => loadingBackground.value || savingBackground.value);

function replaceBackgroundUrl(blob: Blob | null): void {
  if (backgroundUrl.value) URL.revokeObjectURL(backgroundUrl.value);
  backgroundUrl.value = blob ? URL.createObjectURL(blob) : null;
}

async function loadBackground(): Promise<void> {
  const requestId = ++backgroundRequestId;
  loadingBackground.value = true;
  backgroundError.value = null;

  try {
    const background = await props.location.fetchBackground();
    if (requestId === backgroundRequestId) replaceBackgroundUrl(background);
  } catch (error) {
    if (requestId !== backgroundRequestId) return;
    console.debug("Could not load location background");
    replaceBackgroundUrl(null);
  } finally {
    if (requestId === backgroundRequestId) loadingBackground.value = false;
  }
}

function openBackgroundPicker(): void {
  if (!backgroundBusy.value) backgroundInput.value?.click();
}

async function onBackgroundSelected(event: Event): Promise<void> {
  const input = event.currentTarget as HTMLInputElement;
  const file = input.files?.[0];
  input.value = "";

  if (!file) return;
  if (!file.type.startsWith("image/")) {
    backgroundError.value = "Choose a valid image file.";
    return;
  }

  const requestId = ++backgroundRequestId;
  savingBackground.value = true;
  backgroundError.value = null;

  try {
    await props.location.saveBackground(file, true);
    if (requestId === backgroundRequestId) replaceBackgroundUrl(file);
  } catch (error) {
    if (requestId !== backgroundRequestId) return;
    console.error("Could not save location background", error);
    backgroundError.value = "The location background could not be saved.";
  } finally {
    if (requestId === backgroundRequestId) savingBackground.value = false;
  }
}

async function removeBackground(): Promise<void> {
  if (!backgroundUrl.value || backgroundBusy.value) return;

  const requestId = ++backgroundRequestId;
  savingBackground.value = true;
  backgroundError.value = null;

  try {
    await props.location.deleteBackground();
    if (requestId === backgroundRequestId) replaceBackgroundUrl(null);
  } catch (error) {
    if (requestId !== backgroundRequestId) return;
    console.error("Could not delete location background", error);
    backgroundError.value = "The location background could not be removed.";
  } finally {
    if (requestId === backgroundRequestId) savingBackground.value = false;
  }
}

function markFieldSaving(field: keyof LocationData, saving: boolean): void {
  const next = new Set(savingFields.value);
  if (saving) next.add(field);
  else next.delete(field);
  savingFields.value = next;
}

async function updateField<K extends keyof LocationData>(field: K, value: LocationData[K]): Promise<void> {
  if (savingFields.value.has(field) || props.location.get(field) === value) return;

  saveError.value = null;
  markFieldSaving(field, true);

  try {
    const updated = await props.location.update(field, value);
    if (updated === false) throw new Error("Location update returned false");
  } catch (error) {
    console.error(`Could not update location field ${String(field)}`, error);
    saveError.value = "The location information could not be saved.";
  } finally {
    markFieldSaving(field, false);
  }
}

async function loadLorebook(): Promise<void> {
  const requestId = ++loadRequestId;
  lorebook.value = null;
  loadingLorebook.value = true;
  loadError.value = null;

  try {
    const loaded = await props.location.getLorebook();
    if (requestId === loadRequestId) lorebook.value = loaded;
  } catch (error) {
    if (requestId !== loadRequestId) return;
    console.error("Could not load location lorebook", error);
    loadError.value = "The location lorebook could not be loaded.";
  } finally {
    if (requestId === loadRequestId) loadingLorebook.value = false;
  }
}

onMounted(() => {
  void loadBackground();
  void loadLorebook();
});

onBeforeUnmount(() => {
  ++backgroundRequestId;
  replaceBackgroundUrl(null);
});

watch(
    [
      () => props.location.get("worldID"),
      () => props.location.get("id"),
    ],
    () => {
      ++backgroundRequestId;
      replaceBackgroundUrl(null);
      savingBackground.value = false;
      backgroundError.value = null;
      savingFields.value = new Set();
      saveError.value = null;
      void loadBackground();
      void loadLorebook();
    },
);
</script>

<template>
  <article class="location-editor" :aria-busy="isSaving || loadingLorebook || backgroundBusy">
    <header class="location-editor__identity">
      <div class="location-editor__background-editor">
        <input
            ref="backgroundInput"
            class="location-editor__file-input"
            type="file"
            accept="image/*"
            :disabled="backgroundBusy"
            aria-label="Choose location background"
            @change="onBackgroundSelected"
        />

        <div class="location-editor__background-viewport">
          <button
              type="button"
              class="location-editor__background"
              :class="{'location-editor__background--empty': !backgroundUrl}"
              :disabled="backgroundBusy"
              :aria-label="backgroundUrl ? 'Replace location background' : 'Add location background'"
              @click="openBackgroundPicker"
          >
            <img
                v-if="backgroundUrl"
                :src="backgroundUrl"
                :alt="`${locationName} background`"
            />

            <span v-else class="location-editor__background-placeholder" aria-hidden="true">
                            <svg viewBox="0 0 24 24">
                                <path d="M4 16.5 8.5 12l3 3 2-2 6.5 6.5" />
                                <circle cx="15.5" cy="8.5" r="2" />
                                <rect x="3" y="3" width="18" height="18" rx="4" />
                            </svg>
                            <span>{{ loadingBackground ? "Loading…" : "Add background" }}</span>
                        </span>

            <span v-if="savingBackground" class="location-editor__background-busy" role="status">
                            Saving…
                        </span>
          </button>

          <div class="location-editor__background-actions">
            <button
                type="button"
                :disabled="backgroundBusy"
                :title="backgroundUrl ? 'Replace background' : 'Upload background'"
                @click="openBackgroundPicker"
            >
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M12 16V4" />
                <path d="m7 9 5-5 5 5" />
                <path d="M5 20h14" />
              </svg>
              <span>{{ backgroundUrl ? "Replace" : "Upload" }}</span>
            </button>
            <button
                v-if="backgroundUrl"
                type="button"
                class="location-editor__remove-background"
                :disabled="backgroundBusy"
                title="Delete background"
                @click="removeBackground"
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
        </div>
      </div>

      <div class="location-editor__name">
        <ShortTextBox
            :model-value="location.get('name')"
            aria-label="Location name"
            :disabled="savingFields.has('name')"
            @edit="value => updateField('name', value)"
        />
      </div>
    </header>

    <div v-if="backgroundError" class="location-editor__error" role="alert">
      <span>{{ backgroundError }}</span>
      <div class="location-editor__error-actions">
        <button type="button" @click="loadBackground">Retry</button>
        <button type="button" @click="backgroundError = null">Dismiss</button>
      </div>
    </div>

    <div v-if="saveError" class="location-editor__error" role="alert">
      <span>{{ saveError }}</span>
      <button type="button" @click="saveError = null">Dismiss</button>
    </div>

    <section class="location-editor__section">
      <FieldEditorWrapper field-name="Description" info="Narrative and semantic context associated with the location." :vertical="true">
        <LongTextBox
            :model-value="location.get('description')"
            aria-label="Location description"
            tokenize
            :tokenization-started="true"
            @edit="value => updateField('description', value)"
        />
      </FieldEditorWrapper>
    </section>

    <section class="location-editor__section location-editor__section--accent">
      <Expandable title="Location lorebook" info="Lore scoped to this location." :initially-open="false">
        <LorebookEditor v-if="lorebook" :model-value="lorebook" />

        <div v-else-if="loadingLorebook" class="location-editor__state" role="status">
          Loading location lorebook…
        </div>

        <div v-else-if="loadError" class="location-editor__state location-editor__state--error" role="alert">
          <span>{{ loadError }}</span>
          <button type="button" @click="loadLorebook">Retry</button>
        </div>

        <div v-else class="location-editor__state">
          This location does not currently expose a lorebook.
        </div>
      </Expandable>
    </section>

    <section class="location-editor__section location-editor__section--accent">
      <Expandable
          title="Starting characters"
          info="Characters that may begin a session at this location."
          :initially-open="false"
      >
        <LocationStartingCharactersEditor :location="location" />
      </Expandable>
    </section>
  </article>
</template>

<style scoped>
.location-editor {
  display: grid;
  gap: var(--space-5);
  min-width: 0;
  color: rgb(var(--c-fg));
}

.location-editor__identity {
  display: grid;
  justify-items: stretch;
  gap: var(--space-3);
  min-width: 0;
  container-type: inline-size;
  text-align: center;
}

.location-editor__background-editor {
  width: 100%;
  min-width: 0;
}

.location-editor__file-input {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.location-editor__background-viewport {
  position: relative;
  width: 100%;
  min-width: 0;
}

.location-editor__background {
  display: grid;
  place-items: center;
  width: 100%;
  min-width: 0;
  height: clamp(9rem, 43.75cqi, 22rem);
  padding: 0;
  overflow: hidden;
  border: 2px solid rgb(var(--c-accent-2) / 0.7) !important;
  border-radius: var(--radius-lg) !important;
  background: rgb(var(--c-surface-2));
  box-shadow: 0 10px 28px rgb(var(--c-shadow) / 0.14);
}

.location-editor__background:hover:not(:disabled),
.location-editor__background:focus-visible {
  border-color: rgb(var(--c-accent)) !important;
  box-shadow:
      0 0 0 var(--focus-ring-width) rgb(var(--focus-ring-color) / 0.16),
      0 12px 32px rgb(var(--c-shadow) / 0.18);
}

.location-editor__background:focus-visible {
  outline: 0;
}

.location-editor__background > img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.location-editor__background-placeholder {
  display: grid;
  justify-items: center;
  gap: var(--space-2);
  color: rgb(var(--c-muted));
  font-size: 0.8rem;
  font-weight: 750;
}

.location-editor__background-placeholder svg {
  width: 2.75rem;
  height: 2.75rem;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.5;
}

.location-editor__background-busy {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  padding: var(--space-3);
  background: rgb(0 0 0 / 0.5);
  color: white;
  font-size: 0.85rem;
  font-weight: 800;
}

.location-editor__error-actions {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.location-editor__background-actions {
  position: absolute;
  top: var(--space-2);
  right: var(--space-2);
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-2);
  max-width: calc(100% - (2 * var(--space-2)));
}

.location-editor__background-actions button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-1);
  min-height: 2rem;
  padding: var(--space-1) var(--space-2);
  border-color: rgb(255 255 255 / 0.42);
  background: rgb(16 18 22 / 0.78);
  box-shadow: 0 4px 12px rgb(0 0 0 / 0.24);
  color: white;
  font-size: 0.75rem;
  font-weight: 800;
  backdrop-filter: blur(6px);
}

.location-editor__background-actions button:hover:not(:disabled) {
  border-color: rgb(255 255 255 / 0.78);
  background: rgb(16 18 22 / 0.94);
}

.location-editor__background-actions svg {
  width: 1rem;
  height: 1rem;
  flex: 0 0 auto;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.8;
}

.location-editor__background-actions .location-editor__remove-background {
  border-color: rgb(var(--c-danger) / 0.7);
  background: rgb(var(--c-danger-strong) / 0.84);
  color: white;
}

.location-editor__name {
  width: min(100%, 24rem);
  justify-self: center;
}

.location-editor__eyebrow {
  justify-self: center;
  color: rgb(var(--c-muted));
  font-size: 0.72rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.location-editor__name :deep(.short-text-box) {
  text-align: center;
  font-size: 1.25rem;
  font-weight: 750;
}

.location-editor__description {
  justify-self: center;
  max-width: 42rem;
  margin: calc(-1 * var(--space-1)) 0 0;
  color: rgb(var(--c-muted));
  font-size: 0.9rem;
  line-height: 1.5;
}

.location-editor__status {
  flex: 0 0 auto;
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-round);
  background: rgb(var(--c-info-soft));
  color: rgb(var(--c-info-strong));
  font-size: 0.75rem;
  font-weight: 800;
}

.location-editor__section {
  display: grid;
  gap: var(--space-4);
  min-width: 0;
  padding: var(--space-4);
  border: 1px solid rgb(var(--c-border));
  border-radius: var(--radius-lg);
  background: rgb(var(--c-surface-raised));
}

.location-editor__section--accent {
  border-color: rgb(var(--c-accent-2) / 0.6);
}

.location-editor__error,
.location-editor__state--error {
  border-color: rgb(var(--c-danger) / 0.5);
  background: rgb(var(--c-danger-soft));
  color: rgb(var(--c-danger-strong));
}

.location-editor__error,
.location-editor__state {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding: var(--space-3);
  border: 1px solid rgb(var(--c-border));
  border-radius: var(--radius-md);
}

.location-editor button {
  border: 1px solid currentColor;
  border-radius: var(--radius-sm);
  background: transparent;
  color: inherit;
  cursor: pointer;
}

.location-editor button:disabled {
  opacity: 0.55;
  cursor: wait;
}

@media (prefers-reduced-motion: reduce) {
  .location-editor__background-actions button {
    backdrop-filter: none;
  }
}
</style>
