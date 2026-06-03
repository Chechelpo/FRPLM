TODO: Keywords dont get updated in between entries on addition/removal. They need a global state regarding them.
<script setup lang="ts">
import {ActivationStrategy, Entry, Outlet} from "@/domain/Lorebook";
import LongTextBox from "@/components/utils/primitives/LongTextBox.vue";
import {computed, onMounted, ref, watch} from "vue";
import ShortTextBox from "@/components/utils/primitives/ShortTextBox.vue";
import SingleEnumInput from "@/components/utils/primitives/SingleEnumInput.vue";
import AutoCompleteBox from "@/components/utils/autocomplete/AutoCompleteBox.vue";
import {EntityTypes} from "@/domain/EntityTypes";
import {createEntity} from "@/frameworks/ABSEntity";
import SingleAutoComplete from "@/components/utils/autocomplete/SingleAutoComplete.vue";
import FieldEditorWrapper from "@/components/utils/FieldEditorWrapper.vue";
import NumberInput from "@/components/utils/primitives/NumberInput.vue";
import BooleanToggle from "@/components/utils/primitives/BooleanToggle.vue";

// ---- model / emit -------------------------------------------------------
const props = defineProps<{
  entry: Entry;
  outlets: string[];
  keywords: string[];
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
  return strategy.value === ActivationStrategy.EMBEDDING;
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
const expanded = ref<boolean>(false);

function openExpanded() {
  if (!expanded.value) console.info(`Editing entry ${props.entry}`)
  expanded.value = !expanded.value;
}

function onDelete() {
  emit('delete', props.entry);
}

const activationStrategyValues = Object.values(ActivationStrategy)
    .filter((value): value is ActivationStrategy => typeof value === "number");

const activationStrategyLabels: Record<ActivationStrategy, string> = {
  [ActivationStrategy.CONSTANT]: "CONSTANT",
  [ActivationStrategy.COMMON]: "COMMON",
  [ActivationStrategy.EMBEDDING]: "EMBEDDING",
};

// ---- Edit handlers -------------------------------------------------------
async function handleNewKeyword(name: string): Promise<void> {
  if (await props.entry.addKeyword(name)) emit("newKeyword", name);
}

async function handleRemoveKeyword(name: string): Promise<void> {
  await props.entry.removeKeyword(name)
}
async function handleOutletChange(value:string){
  await props.entry.updateOutlet(value);
  console.debug(`New outlet value: ${await props.entry.getOutletName()}`);
  outlet.value = value;
}
async function clearOutlet(){
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
  if ( string != null ) outlet.value = string;
}
</script>

<template>
  <!-- TITLE Name + expand entry bar -->
  <div class="flexColumnBar">
    <BooleanToggle
        :model-value="props.entry.get('enabled')"
        @edit="value => props.entry.update('enabled', value)"
    />
    <ShortTextBox
        :model-value="props.entry.get('name')"
        @edit="txt => props.entry.update('name', txt)"
    ></ShortTextBox>
    <button
        class="expandButton"
        type="button"
        @click="openExpanded"
        title="expand"
    >⤢
    </button>
    <button
        type="button"
        @click="onDelete"
    >D
    </button>
  </div>

  <!-- Expanded editor -->
  <div class="expandedTop" v-if="expanded">
    <!-- Keywords -->
    <AutoCompleteBox
        v-if         = "!(strategy.valueOf() == ActivationStrategy.CONSTANT)"
        placeholder  = "Activation keywords"
        :model-value = "entryKeywords"
        :suggestions = "props.keywords"
        @add         = "handleNewKeyword"
        @remove      = "handleRemoveKeyword"
        :allow-custom= "true"
    />
    <!-- probability -->
    <FieldEditorWrapper
        v-if = "!(strategy.valueOf() == ActivationStrategy.CONSTANT)"
        field-name="probability"
        info="The probability of this entry being injected on successful activation"
    >
      <NumberInput
          :model-value="props.entry.get('probability')"
          @edit="payload => props.entry.update('probability', payload)"
      />
    </FieldEditorWrapper>

    <!--Injection strategy -->
    <div class=flexColumnBar>
      <FieldEditorWrapper
          field-name="Injection Strategy"
          info = "Normal = keyword activation. Embedding = Keywords + embedding text. Constant = Always active"
      >
        <SingleEnumInput
            :value="strategy!"
            :possible_values="activationStrategyValues"
            :labels="activationStrategyLabels"
            @edit="option => strategy = option"
        ></SingleEnumInput>
      </FieldEditorWrapper>
      <!--Outlet -->
      <div class = "outlet-row">
        <FieldEditorWrapper
            field-name="Outlet"
            info="Place in the prompt where this entry will be inserted"
        >
          <SingleAutoComplete
              class="outlet-autocomplete"
              :model-value="outlet"
              :suggestions="outlets"
              :allow-custom="true"
              :clearable="true"
              @select="txt => handleOutletChange(txt)"
              @clear = "clearOutlet()"
          />
        </FieldEditorWrapper>
      </div>
    </div>

    <!-- Content -->
    <div class="flex-row">
      <div v-if="isEmbeddingStrategy">
        <FieldEditorWrapper
          field-name="EmbeddingText"
          info="Text to be used as the embedded representation of the entry. If empty it'll use the content itself instead"
          :vertical="true"
        >
          <LongTextBox
              v-if="isEmbeddingStrategy"
              v-model="embed_text"
              @edit="txt => embed_text = txt"
          />
        </FieldEditorWrapper>
      </div>
      <div>
        <FieldEditorWrapper
          field-name="Content"
          info="Text that will be injected on entry activation"
          :vertical="true"
        >
          <LongTextBox
              :model-value="props.entry.get('content')"
              @edit="txt => props.entry.update('content', txt)"
          />
        </FieldEditorWrapper>
      </div>
    </div>
  </div>
</template>

<style scoped>
.outlet-row {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 0.5rem;
  min-width: 0;
}

.outlet-label {
  white-space: nowrap;
}

.outlet-autocomplete {
  flex: 1;
  min-width: 12rem;
}

.flexColumnBar {
  display: flex;
  flex-direction: row;

  width: 100%;
}

.expandedTop {
  display: flex;
  flex-direction: column;
  width: 100%;
}

.expandButton {
  margin-left: auto;
}
</style>