TODO: Keywords dont get updated in between entries on addition/removal. They need a global state regarding them.
<script setup lang="ts">
import {ActivationStrategy, Entry, Lorebook, LorebookData, LorebookKey} from "@/domain/Lorebook";
import LongTextBox from "@/components/utils/primitiveEditors/LongTextBox.vue";
import {computed, onMounted, ref, watch} from "vue";
import ShortTextBox from "@/components/utils/primitiveEditors/ShortTextBox.vue";
import SingleEnumInput from "@/components/utils/primitiveEditors/SingleEnumInput.vue";
import AutoCompleteBox from "@/components/utils/autocomplete/AutoCompleteBox.vue";
import SingleAutoComplete from "@/components/utils/autocomplete/SingleAutoComplete.vue";
import FieldEditorWrapper from "@/components/utils/FieldEditorWrapper.vue";
import NumberInput from "@/components/utils/primitiveEditors/NumberInput.vue";
import BooleanToggle from "@/components/utils/primitiveEditors/BooleanToggle.vue";
import WindowPrompt from "@/components/utils/prompts/WindowPrompt.vue";
import {fetch_all} from "@/frameworks/ABSEntity";
import {EntityTypes} from "@/domain/EntityTypes";
import IconButton from "@/components/utils/buttons/IconButton.vue";

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
  <!-- TITLE Name + expand entry bar -->
  <div>
    <div
        class="entry-header"
        :class="{ 'entry-header--expanded': isExpanded }"
    >
      <button
          type="button"
          class="entry-header__expand"
          :class="{ 'entry-header__expand--expanded': isExpanded }"
          :aria-expanded="isExpanded"
          :title="isExpanded ? 'Collapse entry' : 'Expand entry'"
          @click="openExpanded"
      >
        <span class="entry-header__arrow" aria-hidden="true">▶</span>
      </button>

      <div class="entry-header__enabled">
        <BooleanToggle
            :model-value="props.entry.get('enabled')"
            @edit="value => props.entry.update('enabled', value)"
        />
      </div>

      <ShortTextBox
          class="entry-header__name"
          :disabled="!isExpanded"
          :model-value="props.entry.get('name')"
          @edit="txt => props.entry.update('name', txt)"
      />
      <IconButton
          title="Move entry to another lorebook"
          variant="accent"
          @click="exportEntryStart"
      >
        <svg
            viewBox="0 0 24 24"
            aria-hidden="true"
        >
          <path d="M14 3h7v7"/>
          <path d="M10 14 21 3"/>
          <path d="M21 14v5a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5"/>
        </svg>
      </IconButton>
      <IconButton
          title="Delete entry"
          variant="danger"
          @click="onDelete"
      >
        ×
      </IconButton>
    </div>

    <!-- Expanded editor -->
    <div
        v-if="isExpanded"
        class="expandedTop"
    >
      <!-- Row 1: Keywords + probability -->
      <div
          v-if="strategy !== ActivationStrategy.CONSTANT"
          class="editor-row editor-row--keywords"
      >
        <div class="editor-field editor-field--grow">
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
            class="editor-field editor-field--probability"
            field-name="Probability"
            info="The probability of this entry being injected after successful activation"
        >
          <NumberInput
              :model-value="props.entry.get('probability')"
              @edit="payload => props.entry.update('probability', payload!)"
          />
        </FieldEditorWrapper>
      </div>

      <!-- Row 2: Injection strategy + outlet -->
      <div class="editor-row">
        <FieldEditorWrapper
            class="editor-field"
            field-name="Injection Strategy"
            info="Normal = keyword activation. Embedding = keywords plus embedding text. Constant = always active."
        >
          <SingleEnumInput
              :value="strategy"
              :possible_values="activationStrategyValues"
              :labels="activationStrategyLabels"
              @edit="option => strategy = option"
          />
        </FieldEditorWrapper>

        <FieldEditorWrapper
            class="editor-field editor-field--grow"
            field-name="Outlet"
            info="Place in the prompt where this entry will be inserted"
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

      <!-- Row 3: Toggles + scan depth -->
      <div class="editor-row editor-row--controls">
        <FieldEditorWrapper
            class="editor-field"
            field-name="Prevent further recursion"
        >
          <BooleanToggle
              :model-value="props.entry.get('prevent_further_recursion')"
              @edit="value => {
              props.entry.update('prevent_further_recursion', value);
              if (value)  props.entry.update('non_recursable', false);
        }"
          />
        </FieldEditorWrapper>

        <FieldEditorWrapper
            v-if="strategy != ActivationStrategy.CONSTANT"
            class="editor-field"
            field-name="Non-Recursable"
        >
          <BooleanToggle
              :model-value="props.entry.get('non_recursable')"
              @edit="value => props.entry.update('non_recursable', value)"
          />
        </FieldEditorWrapper>

        <FieldEditorWrapper
            v-if="strategy != ActivationStrategy.CONSTANT"
            class="editor-field editor-field--scan-depth"
            field-name="Scan depth"
        >
          <NumberInput
              :model-value="props.entry.get('scan_depth')"
              :nullable="true"
              @edit="payload => props.entry.update('scan_depth', payload)"
          />
        </FieldEditorWrapper>
      </div>

      <!-- Row 4: Content -->
      <div class="editor-row editor-row--content">
        <FieldEditorWrapper
            v-if="isEmbeddingStrategy"
            class="editor-field editor-field--grow"
            field-name="Embedding Text"
            info="Text used as the embedded representation. If empty, the content is used instead."
            :vertical="true"
        >
          <LongTextBox
              v-model="embed_text"
              @edit="txt => embed_text = txt"
          />
        </FieldEditorWrapper>

        <FieldEditorWrapper
            class="editor-field editor-field--grow"
            field-name="Content"
            info="Text that will be injected when the entry activates"
            :vertical="true"
        >
          <LongTextBox
              :model-value="props.entry.get('content')"
              @edit="txt => props.entry.update('content', txt)"
          />
        </FieldEditorWrapper>
      </div>
    </div>
    <WindowPrompt
        v-if="isExchanging"
        title="Export entry"
        @close="isExchanging = false"
    >
      <template #default>
        <FieldEditorWrapper field-name="Destination">
          <SingleEnumInput
              v-if="allLorebooks"
              :value="destinationLorebook? destinationLorebook.get('name') : '' "
              :possible_values="allLorebooks.map(lor => lor.get('name'))"
              @edit="value => destinationLorebook = allLorebooks.find(lor => lor.get('name') == value)!"
          />
        </FieldEditorWrapper>
      </template>
      <template #footer>
        <button
            type="button"
            @click="confirmEntryExport()"
        > submit
        </button>
      </template>
    </WindowPrompt>
  </div>
</template>

<style scoped>
.entry-header {
  display: flex;
  align-items: center;
  width: 100%;
  min-width: 0;
  gap: 0.5rem;

  padding: 0.4rem 0.5rem;
  border: 1px solid color-mix(in srgb, currentColor 20%, transparent);
  border-radius: 0.4rem;

  background: color-mix(in srgb, currentColor 4%, transparent);
}.expandedTop {
   box-sizing: border-box;
   max-width: 100%;
 }

.entry-header--expanded {
  border-bottom-left-radius: 0;
  border-bottom-right-radius: 0;
}

.entry-header__expand,
.entry-header__export,
.entry-header__delete {
  display: inline-flex;
  align-items: center;
  justify-content: center;

  flex: 0 0 auto;
  width: 2rem;
  height: 2rem;
  padding: 0;

  color: inherit;
  background: transparent;

  border: 1px solid transparent;
  border-radius: 0.35rem;

  cursor: pointer;

  transition: color 120ms ease,
  background-color 120ms ease,
  border-color 120ms ease,
  transform 120ms ease;
}

.entry-header__expand:hover,
.entry-header__delete:hover {
  background: color-mix(in srgb, currentColor 10%, transparent);
}

.entry-header__expand:focus-visible,
.entry-header__export:focus-visible,
.entry-header__delete:focus-visible {
  outline: 2px solid var(--primary-accent, currentColor);
  outline-offset: 2px;
}

.entry-header__arrow {
  display: inline-block;
  font-size: 0.75rem;
  transition: transform 150ms ease;
  transform-origin: center;
}

.entry-header__expand--expanded .entry-header__arrow {
  transform: rotate(90deg);
}

.entry-header__enabled {
  display: flex;
  align-items: center;
  flex: 0 0 auto;
}

.entry-header__name {
  flex: 1 1 auto;
  min-width: 0;
}

.entry-header__delete {
  font-size: 1.35rem;
  line-height: 1;
}

.entry-header__delete:hover {
  color: #d33;
}

.expandedTop {
  display: flex;
  flex-direction: column;
  width: 100%;
  min-width: 0;

  gap: 0.75rem;
  padding: 0.75rem;

  border: 1px solid color-mix(in srgb, currentColor 20%, transparent);
  border-top: none;
  border-bottom-left-radius: 0.4rem;
  border-bottom-right-radius: 0.4rem;
}

.editor-row {
  display: flex;
  align-items: flex-end;
  width: 100%;
  min-width: 0;
  gap: 1rem;
}

.editor-field {
  min-width: 0;
  max-width: 100%;
}

.editor-field--grow {
  flex: 1 1 0;
}

.editor-field--probability {
  flex: 0 1 12rem;
  min-width: 0;
}

.editor-field--scan-depth {
  flex: 0 1 10rem;
  min-width: 0;
  margin-left: auto;
}

.editor-row--controls {
  align-items: center;
}

.editor-row--content {
  align-items: stretch;
}

.outlet-autocomplete {
  width: 100%;
  min-width: 12rem;
}

@media (max-width: 800px) {
  .editor-row {
    flex-direction: column;
    align-items: stretch;
  }

  .editor-field,
  .editor-field--probability,
  .editor-field--scan-depth {
    width: 100%;
    flex: 1 1 auto;
    margin-left: 0;
  }
}
</style>