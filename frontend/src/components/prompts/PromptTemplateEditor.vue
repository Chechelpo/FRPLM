<script setup lang="ts">
import {
  PromptSection,
  PromptTemplate,
  REASONING_EFFORT,
  REASONING_EFFORT_IDs,
  ReasoningEffortId
} from "@/domain/Prompts";
import ShortTextBox from "@/components/utils/primitiveEditors/ShortTextBox.vue";
import NumberSlider from "@/components/utils/primitiveEditors/NumberSlider.vue";
import BooleanToggle from "@/components/utils/primitiveEditors/BooleanToggle.vue";
import SingleEnumInput from "@/components/utils/primitiveEditors/SingleEnumInput.vue";
import { computed, onMounted, ref } from "vue";
import FieldEditorWrapper from "@/components/utils/FieldEditorWrapper.vue";
import PromptSectionEditor from "@/components/prompts/PromptSectionEditor.vue";
import { computedAsync } from "@vueuse/core";
import { LLMConnection, LLMConnectionData, LLMConnectionKeys } from "@/domain/Connection";
import { fetch_all } from "@/frameworks/ABSEntity";
import { EntityTypes } from "@/domain/EntityTypes";
import Expandable from "@/components/utils/panels/Expandable.vue";

const model = defineModel<PromptTemplate>({
  required: true,
  type: PromptTemplate,
});

const reasoningEffortIDtoName = computed<ReadonlyMap<ReasoningEffortId, string>>(() => {
  const map = new Map<ReasoningEffortId, string>();

  Object.values(REASONING_EFFORT).forEach(obj => {
    map.set(obj.id, obj.name);
  });

  return map;
});

const connections = computedAsync<LLMConnection[]>(
    async () =>
        await fetch_all<LLMConnectionKeys, LLMConnectionData, LLMConnection>(
            EntityTypes.LLM,
            LLMConnection
        ),
    []
);

const connectionIDs = computed<number[]>(() =>
    connections.value.map(connection => connection.get("id"))
);

const connectionIDtoName = computed<ReadonlyMap<number, string>>(() => {
  const map = new Map<number, string>();

  connections.value.forEach(connection => {
    map.set(connection.get("id"), connection.get("name")!);
  });

  return map;
});

const currentConnection = computed<number | null>({
  get() {
    const currentId = model.value.get("connection_id");
    currentConnectionObject.value = connections.value.find(con => con.get("id") === currentId)!;

    return currentConnectionObject.value?.get("id") ?? null;
  },
  set(newCon: number | null) {
    if (newCon == null) return;

    model.value.update("connection_id", newCon);
  },
});
const currentConnectionObject = ref<LLMConnection | null>(null);

const sections = ref<PromptSection[]>([]);

const orderedSections = computed<PromptSection[]>(() =>
    [...sections.value].sort((a, b) => a.get("position") - b.get("position"))
);

onMounted(() => {
  reload();
});

async function reload(): Promise<void> {
  sections.value = await model.value.getSections();
  console.log(sections);
}

async function createSection(): Promise<void> {
  const name = window.prompt("Enter new section name");
  if (!name) return;

  await model.value.createSection(name);

  //Acts up if not done this way.
  await reload()
}

async function moveSection(section: PromptSection, direction: -1 | 1): Promise<void> {
  const ordered = orderedSections.value;
  const index = ordered.indexOf(section);

  if (index < 0) return;

  const other = ordered[index + direction];

  if (!other) return;

  const succeeded = await PromptSection.exchange(model.value, section, other)
  if (succeeded) sections.value = [...sections.value];
}

function sectionKey(section: PromptSection): string {
  return `${section.get("prompt_id")}:${section.get("section_id")}`;
}

const max_tokens = computed<number>({
  get(){
    return model.value.get("max_tokens");
  },
  set(value: number) {
    model.value.update("max_tokens", value);
  }
})

</script>
<template>
  <div class="background-edit-box">
    <FieldEditorWrapper field-name="Name">
      <ShortTextBox
          :model-value="model.get('name')"
          @edit="value => model.update('name', value)"
      />
    </FieldEditorWrapper>

    <FieldEditorWrapper
      field-name="Connection"
      info="If this prompt runs, this will be the backend it'll hit"
    >
      <SingleEnumInput
          :value="currentConnection ? currentConnection : null"
          :possible_values="connectionIDs"
          :labels="connectionIDtoName"
          @edit="value => currentConnection = value"
      />
    </FieldEditorWrapper>
    <FieldEditorWrapper v-if="currentConnectionObject != null" field-name="Max tokens">
      <NumberSlider
          :model-value="max_tokens"
          :max="currentConnectionObject.get('max_tokens')"
          :step="8192"
          @edit="payload => max_tokens = payload"
      />
    </FieldEditorWrapper>

    <FieldEditorWrapper field-name="streaming">
      <BooleanToggle
        :model-value="model.get('streaming')"
        @edit = "value => model.update('streaming', value)"
      />
    </FieldEditorWrapper>

    <FieldEditorWrapper field-name="Temperature">
      <NumberSlider
          :max="2"
          :step="0.1"
          :model-value="model.get('temperature')"
          @edit="payload => model.update('temperature', payload)"
      />
    </FieldEditorWrapper>

    <FieldEditorWrapper field-name="Top P">
      <NumberSlider
          :max="2"
          :step="0.1"
          :model-value="model.get('top_p')"
          @edit="payload => model.update('top_p', payload)"
      />
    </FieldEditorWrapper>

    <FieldEditorWrapper field-name="Presence penalty">
      <NumberSlider
          :max="2"
          :step="0.1"
          :model-value="model.get('presence_penalty')"
          @edit="payload => model.update('presence_penalty', payload)"
      />
    </FieldEditorWrapper>

    <FieldEditorWrapper field-name="Repetition penalty">
      <NumberSlider
          :max="2"
          :step="0.1"
          :model-value="model.get('repetition_penalty')"
          @edit="payload => model.update('repetition_penalty', payload)"
      />
    </FieldEditorWrapper>

    <FieldEditorWrapper field-name="Top K">
      <NumberSlider
          :max="2"
          :step="0.1"
          :model-value="model.get('top_k')"
          @edit="payload => model.update('top_k', payload)"
      />
    </FieldEditorWrapper>

    <FieldEditorWrapper field-name="Exclude reasoning">
      <BooleanToggle
          :model-value="model.get('exclude_reasoning')"
          @edit="payload => model.update('exclude_reasoning', payload)"
      />
    </FieldEditorWrapper>

    <FieldEditorWrapper field-name="Reasoning effort">
      <SingleEnumInput
          :value="model.get('reasoning_effort')"
          :possible_values="REASONING_EFFORT_IDs"
          :labels="reasoningEffortIDtoName"
          @edit = "value => model.update('reasoning_effort', value)"
      />
    </FieldEditorWrapper>
    <Expandable title="Sections">
      <button
        type="button"
        @click="createSection"
      >
        NEW
      </button>
      <PromptSectionEditor
          class="section_style"
          v-for="(section, index) in orderedSections"
          :key="sectionKey(section)"
          :section="section"
          :index="index"
          :can-move-up="index > 0"
          :can-move-down="index < orderedSections.length - 1"
          @move-up="section => moveSection(section, -1)"
          @move-down="section => moveSection(section, 1)"
      />
    </Expandable>
  </div>
</template>

<style scoped>
.section_style{
  overflow-x: clip;
}
</style>