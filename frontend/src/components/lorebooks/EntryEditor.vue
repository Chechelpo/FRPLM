<script setup lang="ts">
import {ActivationStrategy, Entry, Lorebook, LorebookData, LorebookKey} from "@/domain/Lorebook";
import LongTextBox from "@/components/primitive-editors/LongTextBox.vue";
import {computed, onMounted, ref, watch} from "vue";
import ShortTextBox from "@/components/primitive-editors/ShortTextBox.vue";
import SingleEnumInput from "@/components/primitive-editors/SingleEnumInput.vue";
import AutoCompleteBox from "@/components/primitive-editors/AutoCompleteBox.vue";
import SingleAutoComplete from "@/components/primitive-editors/SingleAutoComplete.vue";
import FieldEditorWrapper from "@/components/utils/FieldEditorWrapper.vue";
import NumberInput from "@/components/primitive-editors/NumberInput.vue";
import BooleanToggle from "@/components/primitive-editors/BooleanToggle.vue";
import WindowPrompt from "@/components/utils/prompts/WindowPrompt.vue";
import {fetch_all} from "@/core/ABSEntity";
import {EntityTypes} from "@/domain/EntityTypes";
import IconButton from "@/components/utils/buttons/IconButton.vue";
import BooleanTickBox from "@/components/primitive-editors/BooleanTickBox.vue";

// ---- model / emit -------------------------------------------------------
const props = defineProps<{
  entry: Entry;
  outlets: string[];
  keywords: string[];
  moveEntry: (entry: Entry, toLorebookId: number) => Promise<void>
}>();
const emit = defineEmits<{
  (e: 'delete', payload: Entry): void;
  (e: 'newKeyword', name: string): void;
  (e: 'newOutlet', name: string): void;
}>();

// ---- attributes -------------------------------------------------------
const entryKeywords = ref<string[]>([]);
const strategy = computed<ActivationStrategy>({
  get() {
    return props.entry.get('strategy')
  },
  set(value: ActivationStrategy) {
    props.entry.update('strategy', value)
  }
})
const isEmbeddingStrategy = computed<boolean>(() => {
  return false;
  //return strategy.value === ActivationStrategy.EMBEDDING;
});
const embed_text = computed<string | null>({
  get() {
    if (isEmbeddingStrategy.value)
      return null
    return props.entry.get('embed_text')
  },
  set(value: string | null) {
    props.entry.update('embed_text', value);
  }
})
const outlet = ref<string>('')


// ---- Render state -------------------------------------------------
const isExpanded = ref<boolean>(false);

function openExpanded() {
  if (!isExpanded.value) console.info(`Editing entry ${props.entry}`)
  isExpanded.value = !isExpanded.value;
}

function onDelete() {
  emit('delete', props.entry);
}

const activationStrategyValues = Object.values(ActivationStrategy)
    .filter((value): value is ActivationStrategy => typeof value === "number");

const activationStrategyLabels: Record<ActivationStrategy, string> = {
  [ActivationStrategy.CONSTANT]: "CONSTANT",
  [ActivationStrategy.COMMON]: "COMMON",
  //[ActivationStrategy.EMBEDDING]: "EMBEDDING",
};

// ---- Edit handlers -------------------------------------------------------
async function handleNewKeyword(name: string): Promise<void> {
  if (await props.entry.addKeyword(name)) emit("newKeyword", name);
}

async function handleRemoveKeyword(name: string): Promise<void> {
  await props.entry.removeKeyword(name)
}

async function handleOutletChange(value: string) {
  await props.entry.updateOutlet(value);
  console.debug(`New outlet value: ${await props.entry.getOutletName()}`);
  outlet.value = value;
}

async function clearOutlet() {
  await props.entry.clearOutlet();
  outlet.value = '';
}

// ---- values -------------------------------------------------------
watch(
    () => props.entry,
    () => {
      load();
    }
)
onMounted(async () => {
  await load()
})

async function load() {
  entryKeywords.value = await props.entry.keywords();

  const string = await props.entry.getOutletName();
  if (string != null) outlet.value = string;
}

// -- Entry exchange --------------------------------------------------
const isExchanging = ref<boolean>(false);
const destinationLorebook = ref<Lorebook | null>(null);
const allLorebooks = ref<Lorebook[]>([]);

async function exportEntryStart() {
  isExchanging.value = true;
  allLorebooks.value = await fetch_all<LorebookKey, LorebookData, Lorebook>(EntityTypes.LOREBOOKS, Lorebook);
  allLorebooks.value = allLorebooks.value.filter(other => other.get('id') != props.entry.get('lorebook_id'))
}

async function confirmEntryExport() {
  if (destinationLorebook.value == null) return;
  await props.moveEntry(props.entry, destinationLorebook.value.get('id'))
}
</script>

<template>
  <article
      class="entry-editor edit-box edit-box--compact edit-box--flat"
      :class="{ 'entry-editor--expanded': isExpanded }"
  >
    <header class="entry-header">
      <button
          type="button"
          class="entry-header__expand"
          :class="{ 'entry-header__expand--expanded': isExpanded }"
          :aria-expanded="isExpanded"
          :title="isExpanded ? 'Collapse entry' : 'Expand entry'"
          @click="openExpanded"
      >
        <svg
            class="entry-header__arrow"
            viewBox="0 0 24 24"
            aria-hidden="true"
        >
          <path d="m9 18 6-6-6-6" />
        </svg>
      </button>

      <BooleanToggle
          class="entry-header__enabled"
          :model-value="props.entry.get('enabled')"
          @edit="value => props.entry.update('enabled', value)"
      />

      <div class="entry-header__identity">
        <ShortTextBox
            class="entry-header__name"
            :disabled="!isExpanded"
            :model-value="props.entry.get('name')"
            aria-label="Entry name"
            @edit="txt => props.entry.update('name', txt)"
        />
      </div>

      <span
          class="entry-header__strategy"
          :title="`Activation strategy: ${activationStrategyLabels[strategy]}`"
      >
        {{ activationStrategyLabels[strategy] }}
      </span>

      <div class="entry-header__actions">
        <IconButton
            title="Move entry to another lorebook"
            variant="accent"
            @click="exportEntryStart"
        >
          <svg
              viewBox="0 0 24 24"
              aria-hidden="true"
          >
            <path d="M14 3h7v7" />
            <path d="M10 14 21 3" />
            <path d="M21 14v5a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5" />
          </svg>
        </IconButton>

        <IconButton
            title="Delete entry"
            variant="danger"
            @click="onDelete"
        >
          <svg
              viewBox="0 0 24 24"
              aria-hidden="true"
          >
            <path d="M3 6h18" />
            <path d="M8 6V4h8v2" />
            <path d="M19 6 18 21H6L5 6" />
            <path d="M10 11v6" />
            <path d="M14 11v6" />
          </svg>
        </IconButton>
      </div>
    </header>

    <div
        v-if="isExpanded"
        class="entry-editor__body"
    >
      <div class="entry-editor__settings">
        <!-- Activation -->
        <section class="entry-panel entry-panel--activation">
          <header class="entry-panel__header">
            <span class="entry-panel__eyebrow">
              Activation
            </span>

            <span class="entry-panel__summary">
              Conditions and prompt destination
            </span>
          </header>

          <div class="activation-grid">
            <div
                v-if="strategy !== ActivationStrategy.CONSTANT"
                class="editor-field activation-grid__keywords"
            >
              <AutoCompleteBox
                  placeholder="Activation keywords"
                  :model-value="entryKeywords"
                  :suggestions="props.keywords"
                  :allow-custom="true"
                  @add="handleNewKeyword"
                  @remove="handleRemoveKeyword"
              />
            </div>

            <FieldEditorWrapper
                v-if="strategy !== ActivationStrategy.CONSTANT"
                class="editor-field activation-grid__probability"
                field-name="Probability"
                info="Probability of injection after successful activation"
            >
              <NumberInput
                  :model-value="props.entry.get('probability')"
                  @edit="
                  payload =>
                    props.entry.update('probability', payload!)
                "
              />
            </FieldEditorWrapper>

            <FieldEditorWrapper
                class="editor-field activation-grid__strategy"
                field-name="Strategy"
                info="Common activates through keywords. Constant remains active."
            >
              <SingleEnumInput
                  :value="strategy"
                  :possible_values="activationStrategyValues"
                  :labels="activationStrategyLabels"
                  @edit="option => strategy = option"
              />
            </FieldEditorWrapper>

            <FieldEditorWrapper
                class="editor-field activation-grid__outlet"
                field-name="Outlet"
                info="Prompt location where this entry will be inserted"
            >
              <SingleAutoComplete
                  class="outlet-autocomplete"
                  :model-value="outlet"
                  :suggestions="props.outlets"
                  :allow-custom="true"
                  :clearable="true"
                  @select="handleOutletChange"
                  @clear="clearOutlet"
              />
            </FieldEditorWrapper>
          </div>
        </section>

        <!-- Processing -->
        <section class="entry-panel entry-panel--processing">
          <header class="entry-panel__header">
            <span class="entry-panel__eyebrow">
              Processing
            </span>

            <span class="entry-panel__summary">
              Recursion controls
            </span>
          </header>

          <div class="processing-grid">
            <FieldEditorWrapper
                class="editor-field"
                field-name="Stop recursion"
                info="Prevents this entry from causing another recursive scan"
            >
              <BooleanTickBox
                  :model-value="
                  props.entry.get('prevent_further_recursion')
                "
                  @edit="
                  value => {
                    props.entry.update(
                      'prevent_further_recursion',
                      value
                    );

                    if (value) {
                      props.entry.update('non_recursable', false);
                    }
                  }
                "
              />
            </FieldEditorWrapper>

            <FieldEditorWrapper
                v-if="strategy !== ActivationStrategy.CONSTANT"
                class="editor-field"
                field-name="Non-recursable"
            >
              <BooleanTickBox
                  :model-value="props.entry.get('non_recursable')"
                  @edit="
                  value =>
                    props.entry.update('non_recursable', value)
                "
              />
            </FieldEditorWrapper>

            <FieldEditorWrapper
                v-if="strategy !== ActivationStrategy.CONSTANT"
                class="editor-field"
                field-name="Scan depth"
            >
              <NumberInput
                  :model-value="props.entry.get('scan_depth')"
                  :nullable="true"
                  @edit="
                  payload =>
                    props.entry.update('scan_depth', payload)
                "
              />
            </FieldEditorWrapper>
          </div>
        </section>
      </div>

      <!-- Content -->
      <section class="entry-panel entry-panel--content">
        <header class="entry-panel__header">
          <span class="entry-panel__eyebrow">
            Prompt content
          </span>

          <span class="entry-panel__summary">
            Text inserted when the entry activates
          </span>
        </header>

        <div
            class="content-grid"
            :class="{
            'content-grid--with-embedding': isEmbeddingStrategy
          }"
        >
          <FieldEditorWrapper
              v-if="isEmbeddingStrategy"
              class="editor-field content-grid__embedding"
              field-name="Embedding text"
              info="Text used as the embedded representation. Content is used when empty."
              :vertical="true"
          >
            <LongTextBox
                v-model="embed_text"
                tokenize
                @edit="txt => embed_text = txt"
            />
          </FieldEditorWrapper>

          <FieldEditorWrapper
              class="editor-field content-grid__content"
              field-name="Content"
              info="Text injected when this entry activates"
              :vertical="true"
          >
            <LongTextBox
                :model-value="props.entry.get('content')"
                :tokenization-started="isExpanded"
                tokenize
                @edit="txt => props.entry.update('content', txt)"
            />
          </FieldEditorWrapper>
        </div>
      </section>
    </div>

    <WindowPrompt
        v-if="isExchanging"
        title="Move entry"
        @close="isExchanging = false"
    >
      <template #default>
        <div class="entry-move-dialog">
          <p class="entry-move-dialog__description">
            Select the lorebook that should receive this entry.
          </p>

          <FieldEditorWrapper field-name="Destination">
            <SingleEnumInput
                :value="destinationLorebook?.get('name') ?? ''"
                :possible_values="
                allLorebooks.map(lorebook => lorebook.get('name'))
              "
                @edit="
                value =>
                  destinationLorebook =
                    allLorebooks.find(
                      lorebook => lorebook.get('name') === value
                    ) ?? null
              "
            />
          </FieldEditorWrapper>
        </div>
      </template>

      <template #footer>
        <button
            type="button"
            class="edit-box__action edit-box__action--accent"
            :disabled="destinationLorebook === null"
            @click="confirmEntryExport"
        >
          Move entry
        </button>
      </template>
    </WindowPrompt>
  </article>
</template>

<style scoped>
.entry-editor {
  --entry-gap: var(--space-2);
  --entry-panel-padding: var(--space-2);

  width: 100%;
  min-width: 0;

  overflow: visible;
}

.entry-editor::before {
  height: 2px;
  opacity: 0.55;
}

.entry-editor--expanded {
  border-color: rgb(var(--c-accent) / 0.4);

  box-shadow:
      0 0 0 1px rgb(var(--c-accent) / 0.07),
      0 7px 20px rgb(var(--c-shadow) / 0.08),
      inset 0 1px 0 rgb(255 255 255 / 0.4);
}

/* Header */

.entry-header {
  display: grid;
  grid-template-columns:
    auto
    auto
    minmax(8rem, 1fr)
    auto
    auto;
  align-items: center;
  gap: var(--space-2);

  width: 100%;
  min-width: 0;
  box-sizing: border-box;

  padding: 0.38rem var(--space-2);

  background:
      linear-gradient(
          135deg,
          rgb(var(--c-surface-raised) / 0.64),
          rgb(var(--c-surface-2) / 0.36)
      );

  border-radius: inherit;
}

.entry-editor--expanded .entry-header {
  border-bottom: 1px solid rgb(var(--c-border) / 0.2);
  border-bottom-right-radius: 0;
  border-bottom-left-radius: 0;
}

.entry-header__expand {
  width: 1.85rem;
  height: 1.85rem;

  display: grid;
  place-items: center;

  padding: 0;

  color: rgb(var(--c-muted));

  background: transparent;
  border: 1px solid transparent;
  border-radius: var(--radius-sm);

  cursor: pointer;

  transition:
      color var(--duration-fast) var(--ease-standard),
      background-color var(--duration-fast) var(--ease-standard),
      border-color var(--duration-fast) var(--ease-standard);
}

.entry-header__expand:hover {
  color: rgb(var(--c-primary-strong));

  background: rgb(var(--c-accent) / 0.1);
  border-color: rgb(var(--c-accent) / 0.25);
}

.entry-header__expand:focus-visible {
  outline:
      var(--focus-ring-width)
      solid
      rgb(var(--focus-ring-color) / 0.3);

  outline-offset: 1px;
}

.entry-header__arrow {
  width: 0.82rem;
  height: 0.82rem;

  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;

  transition:
      transform var(--duration-normal) var(--ease-standard);
}

.entry-header__expand--expanded .entry-header__arrow {
  transform: rotate(90deg);
}

.entry-header__enabled {
  min-width: 0;
}

.entry-header__identity {
  min-width: 0;
}

.entry-header__name {
  display: block;

  width: 100%;
  min-width: 0;
}

.entry-header__name :deep(input) {
  width: 100%;
  min-width: 0;
  min-height: 1.85rem;
  box-sizing: border-box;

  padding-block: 0.25rem;

  color: rgb(var(--c-fg-strong));

  font-size: 0.82rem;
  font-weight: 750;
}

.entry-header__strategy {
  min-height: 1.3rem;

  display: inline-flex;
  align-items: center;

  padding: 0.14rem 0.42rem;

  color: rgb(var(--c-primary-strong));

  background: rgb(var(--c-accent) / 0.11);
  border: 1px solid rgb(var(--c-accent) / 0.22);
  border-radius: var(--radius-round);

  font-size: 0.58rem;
  font-weight: 800;
  line-height: 1;

  letter-spacing: 0.035em;
  white-space: nowrap;
}

.entry-header__actions {
  display: flex;
  align-items: center;
  gap: var(--space-1);
}

/* Expanded body */

.entry-editor__body {
  display: flex;
  flex-direction: column;
  gap: var(--entry-gap);

  width: 100%;
  min-width: 0;
  box-sizing: border-box;

  padding: var(--space-2);

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-surface) / 0.44),
          rgb(var(--c-surface-2) / 0.2)
      );

  border-bottom-right-radius: inherit;
  border-bottom-left-radius: inherit;
}

.entry-editor__settings {
  display: grid;
  grid-template-columns:
    minmax(0, 2fr)
    minmax(14rem, 0.8fr);
  align-items: stretch;
  gap: var(--entry-gap);

  min-width: 0;
}

/* Local compact panels avoid the large global edit-box__section padding. */

.entry-panel {
  min-width: 0;
  box-sizing: border-box;

  padding: var(--entry-panel-padding);

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-surface-raised) / 0.42),
          rgb(var(--c-surface-2) / 0.22)
      );

  border: 1px solid rgb(var(--c-border) / 0.18);
  border-radius: var(--radius-sm);

  box-shadow: inset 0 1px 0 rgb(255 255 255 / 0.25);
}

.entry-panel--content {
  border-color: rgb(var(--c-accent) / 0.22);

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-accent) / 0.055),
          rgb(var(--c-surface-raised) / 0.4)
      );
}

.entry-panel__header {
  display: flex;
  align-items: baseline;
  gap: var(--space-2);

  min-width: 0;

  margin-bottom: var(--space-2);
  padding-bottom: var(--space-1);

  border-bottom: 1px solid rgb(var(--c-border) / 0.14);
}

.entry-panel__eyebrow {
  flex: 0 0 auto;

  color: rgb(var(--c-primary-strong));

  font-size: 0.62rem;
  font-weight: 800;
  line-height: 1;

  text-transform: uppercase;
  letter-spacing: 0.07em;
}

.entry-panel__summary {
  min-width: 0;

  color: rgb(var(--c-muted));

  font-size: 0.68rem;
  line-height: 1.2;

  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Activation */

.activation-grid {
  display: grid;
  grid-template-columns:
    minmax(10rem, 2fr)
    minmax(5.5rem, 0.65fr)
    minmax(8rem, 0.9fr);
  align-items: end;
  gap: var(--space-2);

  min-width: 0;
}

.activation-grid__keywords {
  grid-column: 1 / 3;
}

.activation-grid__probability {
  grid-column: 3;
}

.activation-grid__strategy {
  grid-column: 1;
}

.activation-grid__outlet {
  grid-column: 2 / 4;
}

/* Processing */

.processing-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  align-items: end;
  gap: var(--space-2);

  min-width: 0;
}

.processing-grid > :first-child {
  grid-column: 1 / -1;
}

/* Content */

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: var(--space-2);

  min-width: 0;
}

.content-grid--with-embedding {
  grid-template-columns:
    minmax(12rem, 0.7fr)
    minmax(18rem, 1.3fr);
}

.editor-field,
.outlet-autocomplete {
  width: 100%;
  min-width: 0;
  max-width: 100%;
}

.content-grid__embedding :deep(textarea) {
  min-height: 6rem;
  max-height: 22rem;
}

.content-grid__content :deep(textarea) {
  min-height: 12rem;
  max-height: 36rem;
}


/* Move dialog */

.entry-move-dialog {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);

  min-width: min(24rem, 100%);
}

.entry-move-dialog__description {
  margin: 0;

  color: rgb(var(--c-muted));

  font-size: 0.82rem;
  line-height: 1.5;
}

/* Responsive */

@media (max-width: 960px) {
  .entry-editor__settings {
    grid-template-columns: minmax(0, 1fr);
  }

  .processing-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .processing-grid > :first-child {
    grid-column: auto;
  }
}

@media (max-width: 720px) {
  .entry-header {
    grid-template-columns:
      auto
      auto
      minmax(0, 1fr)
      auto;
  }

  .entry-header__strategy {
    display: none;
  }

  .activation-grid {
    grid-template-columns:
      minmax(0, 1fr)
      minmax(7rem, 0.55fr);
  }

  .activation-grid__keywords {
    grid-column: 1;
  }

  .activation-grid__probability {
    grid-column: 2;
  }

  .activation-grid__strategy {
    grid-column: 1;
  }

  .activation-grid__outlet {
    grid-column: 2;
  }

  .processing-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .processing-grid > :first-child {
    grid-column: 1 / -1;
  }

  .content-grid--with-embedding {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 520px) {
  .entry-header {
    grid-template-columns:
      auto
      auto
      minmax(0, 1fr);
  }

  .entry-header__actions {
    grid-column: 1 / -1;

    justify-content: flex-end;

    margin-top: 0.1rem;
    padding-top: var(--space-1);

    border-top: 1px solid rgb(var(--c-border) / 0.14);
  }

  .entry-editor__body {
    padding: var(--space-1);
  }

  .activation-grid,
  .processing-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .activation-grid__keywords,
  .activation-grid__probability,
  .activation-grid__strategy,
  .activation-grid__outlet,
  .processing-grid > :first-child {
    grid-column: auto;
  }

  .entry-panel__summary {
    display: none;
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