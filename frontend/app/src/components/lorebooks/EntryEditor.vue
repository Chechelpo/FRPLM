<script setup lang="ts">
import {     LongTextBox,
    ShortTextBox,
    SingleEnumInput,
    AutoCompleteBox,
    SingleAutoComplete,
    NumberInput,
    BooleanToggle,
    BooleanTickBox } from "@frplm/ui";

import {     ActivationStrategy,
    Entry,
    Lorebook,
    LorebookData,
    LorebookKey,
    fetch_all,
    EntityTypes } from "@frplm/host-sdk";

import {computed, onMounted, ref, watch} from "vue";
import FieldEditorWrapper from "@components/utils/FieldEditorWrapper.vue";
import WindowPrompt from "@components/utils/prompts/WindowPrompt.vue";
import IconButton from "@components/utils/buttons/IconButton.vue";
const props = defineProps<{
  entry: Entry;
  outlets: string[];
  keywords: string[];
  moveEntry: (entry: Entry, toLorebookId: number) => Promise<void>;
}>();

const emit = defineEmits<{
  (event: "delete", payload: Entry): void;
  (event: "newKeyword", name: string): void;
  (event: "newOutlet", name: string): void;
}>();

const entryKeywords = ref<string[]>([]);
const outlet = ref("");
const isExpanded = ref(false);
const isLoading = ref(false);

const strategy = computed<ActivationStrategy>({
  get() {
    return props.entry.get("strategy");
  },
  set(value: ActivationStrategy) {
    void props.entry.update("strategy", value);
  },
});

const isEmbeddingStrategy = computed<boolean>(() => false);

const embedText = computed<string | null>({
  get() {
    if (!isEmbeddingStrategy.value) {
      return null;
    }

    return props.entry.get("embed_text");
  },
  set(value: string | null) {
    void props.entry.update("embed_text", value);
  },
});

const activationStrategyValues = Object.values(ActivationStrategy)
    .filter((value): value is ActivationStrategy => typeof value === "number");

const activationStrategyLabels: Record<ActivationStrategy, string> = {
  [ActivationStrategy.CONSTANT]: "Always",
  [ActivationStrategy.COMMON]: "Keywords",
};

function toggleExpanded(): void {
  isExpanded.value = !isExpanded.value;
}

function onDelete(): void {
  emit("delete", props.entry);
}

async function handleNewKeyword(name: string): Promise<void> {
  if (await props.entry.addKeyword(name)) {
    emit("newKeyword", name);
  }
}

async function handleRemoveKeyword(name: string): Promise<void> {
  await props.entry.removeKeyword(name);
}

async function handleOutletChange(value: string): Promise<void> {
  await props.entry.updateOutlet(value);
  outlet.value = value;
}

async function clearOutlet(): Promise<void> {
  await props.entry.clearOutlet();
  outlet.value = "";
}

async function load(): Promise<void> {
  isLoading.value = true;

  try {
    const [keywords, outletName] = await Promise.all([
      props.entry.keywords(),
      props.entry.getOutletName(),
    ]);

    entryKeywords.value = keywords;
    outlet.value = outletName ?? "";
  } finally {
    isLoading.value = false;
  }
}

watch(
    () => props.entry,
    () => {
      void load();
    },
);

onMounted(() => {
  void load();
});

const isExchanging = ref(false);
const destinationLorebook = ref<Lorebook | null>(null);
const allLorebooks = ref<Lorebook[]>([]);

async function exportEntryStart(): Promise<void> {
  isExchanging.value = true;
  destinationLorebook.value = null;

  const lorebooks = await fetch_all<LorebookKey, LorebookData, Lorebook>(
      EntityTypes.LOREBOOKS,
      Lorebook,
  );

  allLorebooks.value = lorebooks.filter(
      other => other.get("id") !== props.entry.get("lorebook_id"),
  );
}

async function confirmEntryExport(): Promise<void> {
  const destination = destinationLorebook.value;

  if (destination === null) {
    return;
  }

  await props.moveEntry(props.entry, destination.get("id"));
  isExchanging.value = false;
  destinationLorebook.value = null;
}
</script>

<template>
  <article class="entry-editor edit-box edit-box--compact edit-box--flat" :class="{'entry-editor--expanded': isExpanded, 'entry-editor--loading': isLoading}">
    <header class="entry-header">
      <button type="button" class="entry-header__expand" :class="{'entry-header__expand--expanded': isExpanded}" :aria-expanded="isExpanded" :title="isExpanded ? 'Collapse entry' : 'Expand entry'" @click="toggleExpanded">
        <svg class="entry-header__arrow" viewBox="0 0 24 24" aria-hidden="true">
          <path d="m9 18 6-6-6-6" />
        </svg>
      </button>

      <BooleanToggle class="entry-header__enabled" :model-value="props.entry.get('enabled')" @edit="value => props.entry.update('enabled', value)" />

      <ShortTextBox class="entry-header__name" :disabled="!isExpanded" :model-value="props.entry.get('name')" aria-label="Entry name" @edit="value => props.entry.update('name', value)" />

      <span class="entry-header__strategy" :title="`Activation mode: ${activationStrategyLabels[strategy]}`">
        {{ activationStrategyLabels[strategy] }}
      </span>

      <div class="entry-header__actions">
        <IconButton title="Move entry" variant="accent" @click="exportEntryStart">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M14 3h7v7" />
            <path d="M10 14 21 3" />
            <path d="M21 14v5a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5" />
          </svg>
        </IconButton>

        <IconButton title="Delete entry" variant="danger" @click="onDelete">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M3 6h18" />
            <path d="M8 6V4h8v2" />
            <path d="M19 6 18 21H6L5 6" />
            <path d="M10 11v6" />
            <path d="M14 11v6" />
          </svg>
        </IconButton>
      </div>
    </header>

    <div v-if="isExpanded" class="entry-editor__body">
      <section class="entry-panel">
        <header class="entry-panel__header">Activation</header>

        <div class="activation-grid">
          <AutoCompleteBox v-if="strategy !== ActivationStrategy.CONSTANT" class="activation-grid__keywords" placeholder="Activation keywords" :model-value="entryKeywords" :suggestions="props.keywords" :allow-custom="true" @add="handleNewKeyword" @remove="handleRemoveKeyword" />

          <FieldEditorWrapper v-if="strategy !== ActivationStrategy.CONSTANT" field-name="Chance" info="Probability of injection after successful activation.">
            <NumberInput :model-value="props.entry.get('probability')" @edit="value => props.entry.update('probability', value!)" />
          </FieldEditorWrapper>

          <FieldEditorWrapper field-name="Mode" info="Keywords activates through matching terms. Always remains active.">
            <SingleEnumInput :value="strategy" :possible_values="activationStrategyValues" :labels="activationStrategyLabels" @edit="value => strategy = value" />
          </FieldEditorWrapper>

          <FieldEditorWrapper class="activation-grid__outlet" field-name="Outlet" info="Prompt destination where this entry will be inserted.">
            <SingleAutoComplete class="editor-control" :model-value="outlet" :suggestions="props.outlets" :allow-custom="true" :clearable="true" @select="handleOutletChange" @clear="clearOutlet" />
          </FieldEditorWrapper>
        </div>
      </section>

      <section class="entry-panel">
        <header class="entry-panel__header">Recursion</header>

        <div class="processing-grid">
          <FieldEditorWrapper field-name="Stop scan" info="Prevents this entry from causing another recursive scan.">
            <BooleanTickBox :model-value="props.entry.get('prevent_further_recursion')" @edit="value => { props.entry.update('prevent_further_recursion', value); if (value) props.entry.update('non_recursable', false); }" />
          </FieldEditorWrapper>

          <FieldEditorWrapper v-if="strategy !== ActivationStrategy.CONSTANT" field-name="No recurse" info="Prevents recursive scans from activating this entry.">
            <BooleanTickBox :model-value="props.entry.get('non_recursable')" @edit="value => props.entry.update('non_recursable', value)" />
          </FieldEditorWrapper>

          <FieldEditorWrapper v-if="strategy !== ActivationStrategy.CONSTANT" field-name="Depth" info="Maximum recursive scan depth. Empty uses the default.">
            <NumberInput :model-value="props.entry.get('scan_depth')" :nullable="true" @edit="value => props.entry.update('scan_depth', value)" />
          </FieldEditorWrapper>
        </div>
      </section>

      <section class="entry-panel entry-panel--content">
        <header class="entry-panel__header">Content</header>

        <div class="content-grid" :class="{'content-grid--with-embedding': isEmbeddingStrategy}">
          <FieldEditorWrapper v-if="isEmbeddingStrategy" field-name="Embedding" info="Text used for embedding. Content is used when this is empty." :vertical="true">
            <LongTextBox v-model="embedText" tokenize @edit="value => embedText = value" />
          </FieldEditorWrapper>

          <FieldEditorWrapper field-name="Prompt" info="Text inserted when this entry activates." :vertical="true">
            <LongTextBox :model-value="props.entry.get('content')" :tokenization-started="isExpanded" tokenize @edit="value => props.entry.update('content', value)" />
          </FieldEditorWrapper>
        </div>
      </section>
    </div>

    <WindowPrompt v-if="isExchanging" title="Move entry" @close="isExchanging = false">
      <div class="entry-move-dialog">
        <p class="entry-move-dialog__description">Select the destination lorebook.</p>

        <FieldEditorWrapper field-name="Destination">
          <SingleEnumInput :value="destinationLorebook?.get('name') ?? ''" :possible_values="allLorebooks.map(lorebook => lorebook.get('name'))" @edit="value => destinationLorebook = allLorebooks.find(lorebook => lorebook.get('name') === value) ?? null" />
        </FieldEditorWrapper>
      </div>

      <template #footer>
        <button type="button" class="edit-box__action edit-box__action--accent" :disabled="destinationLorebook === null" @click="confirmEntryExport">Move</button>
      </template>
    </WindowPrompt>
  </article>
</template>

<style scoped>
.entry-editor {
  --entry-gap: var(--space-1);
  --entry-padding: 0.42rem;

  width: 100%;
  min-width: 0;
  overflow: visible;
}

.entry-editor::before {
  height: 2px;
  opacity: 0.5;
}

.entry-editor--expanded {
  border-color: rgb(var(--c-accent) / 0.38);
  box-shadow: 0 0 0 1px rgb(var(--c-accent) / 0.06), 0 5px 16px rgb(var(--c-shadow) / 0.07);
}

.entry-editor--loading {
  opacity: 0.72;
  pointer-events: none;
}

.entry-header {
  display: grid;
  grid-template-columns: auto auto minmax(0, 1fr) auto auto;
  align-items: center;
  gap: var(--space-1);

  width: 100%;
  min-width: 0;
  box-sizing: border-box;
  padding: 0.3rem 0.4rem;

  background: linear-gradient(135deg, rgb(var(--c-surface-raised) / 0.64), rgb(var(--c-surface-2) / 0.34));
  border-radius: inherit;
}

.entry-editor--expanded .entry-header {
  border-bottom: 1px solid rgb(var(--c-border) / 0.18);
  border-bottom-right-radius: 0;
  border-bottom-left-radius: 0;
}

.entry-header__expand {
  width: 1.65rem;
  height: 1.65rem;
  display: grid;
  place-items: center;
  padding: 0;

  color: rgb(var(--c-muted));
  background: transparent;
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  cursor: pointer;

  transition: color var(--duration-fast) var(--ease-standard), background-color var(--duration-fast) var(--ease-standard), border-color var(--duration-fast) var(--ease-standard);
}

.entry-header__expand:hover {
  color: rgb(var(--c-primary-strong));
  background: rgb(var(--c-accent) / 0.1);
  border-color: rgb(var(--c-accent) / 0.24);
}

.entry-header__expand:focus-visible {
  outline: var(--focus-ring-width) solid rgb(var(--focus-ring-color) / 0.3);
  outline-offset: 1px;
}

.entry-header__arrow {
  width: 0.76rem;
  height: 0.76rem;
  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
  transition: transform var(--duration-normal) var(--ease-standard);
}

.entry-header__expand--expanded .entry-header__arrow {
  transform: rotate(90deg);
}

.entry-header__enabled,
.entry-header__name {
  min-width: 0;
}

.entry-header__name {
  display: block;
  width: 100%;
}

.entry-header__name :deep(input) {
  width: 100%;
  min-width: 0;
  min-height: 1.65rem;
  box-sizing: border-box;
  padding-block: 0.18rem;

  color: rgb(var(--c-fg-strong));
  font-size: 0.79rem;
  font-weight: 750;
}

.entry-header__strategy {
  display: inline-flex;
  align-items: center;
  padding: 0.12rem 0.34rem;

  color: rgb(var(--c-primary-strong));
  background: rgb(var(--c-accent) / 0.1);
  border: 1px solid rgb(var(--c-accent) / 0.2);
  border-radius: var(--radius-round);

  font-size: 0.56rem;
  font-weight: 800;
  line-height: 1;
  white-space: nowrap;
}

.entry-header__actions {
  display: flex;
  align-items: center;
  gap: 0.18rem;
}

.entry-editor__body {
  display: flex;
  flex-direction: column;
  gap: var(--entry-gap);

  width: 100%;
  min-width: 0;
  box-sizing: border-box;
  padding: var(--entry-padding);

  background: linear-gradient(145deg, rgb(var(--c-surface) / 0.42), rgb(var(--c-surface-2) / 0.18));
  border-bottom-right-radius: inherit;
  border-bottom-left-radius: inherit;
}

.entry-panel {
  min-width: 0;
  box-sizing: border-box;
  padding: var(--entry-padding);

  background: linear-gradient(145deg, rgb(var(--c-surface-raised) / 0.4), rgb(var(--c-surface-2) / 0.2));
  border: 1px solid rgb(var(--c-border) / 0.16);
  border-radius: var(--radius-sm);
}

.entry-panel--content {
  border-color: rgb(var(--c-accent) / 0.2);
  background: linear-gradient(145deg, rgb(var(--c-accent) / 0.045), rgb(var(--c-surface-raised) / 0.38));
}

.entry-panel__header {
  margin-bottom: var(--space-1);
  padding-bottom: 0.24rem;

  color: rgb(var(--c-primary-strong));
  border-bottom: 1px solid rgb(var(--c-border) / 0.13);

  font-size: 0.6rem;
  font-weight: 800;
  line-height: 1;
  text-transform: uppercase;
  letter-spacing: 0.065em;
}

.activation-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(6.5rem, 0.72fr);
  align-items: end;
  gap: var(--space-1);
  min-width: 0;
}

.activation-grid__keywords,
.activation-grid__outlet {
  grid-column: 1 / -1;
}

.processing-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  align-items: end;
  gap: var(--space-1);
  min-width: 0;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: var(--space-1);
  min-width: 0;
}

.content-grid--with-embedding {
  grid-template-columns: minmax(0, 1fr);
}

.editor-control {
  width: 100%;
  min-width: 0;
  max-width: 100%;
}

.content-grid :deep(textarea) {
  width: 100%;
  min-width: 0;
  min-height: 8rem;
  max-height: 28rem;
  box-sizing: border-box;
}

.entry-move-dialog {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  width: min(22rem, 100%);
  min-width: 0;
}

.entry-move-dialog__description {
  margin: 0;
  color: rgb(var(--c-muted));
  font-size: 0.79rem;
  line-height: 1.45;
}

@media (max-width: 430px) {
  .entry-header {
    grid-template-columns: auto auto minmax(0, 1fr) auto;
  }

  .entry-header__strategy {
    display: none;
  }

  .processing-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 350px) {
  .entry-header {
    grid-template-columns: auto auto minmax(0, 1fr);
  }

  .entry-header__actions {
    grid-column: 1 / -1;
    justify-content: flex-end;
    padding-top: var(--space-1);
    border-top: 1px solid rgb(var(--c-border) / 0.13);
  }

  .activation-grid,
  .processing-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .activation-grid__keywords,
  .activation-grid__outlet {
    grid-column: auto;
  }
}

@media (prefers-reduced-motion: reduce) {
  .entry-header__expand,
  .entry-header__arrow,
  .entry-editor {
    transition: none;
  }
}
</style>
