<script setup lang="ts">
import {
  PromptSection,
  PromptTemplate,
  REASONING_EFFORT,
  REASONING_EFFORT_IDs,
  ReasoningEffortId
} from "@/domain/Prompts";
import ShortTextBox from "@/components/utils/primitives/ShortTextBox.vue";
import NumberSlider from "@/components/utils/primitives/NumberSlider.vue";
import BooleanToggle from "@/components/utils/primitives/BooleanToggle.vue";
import SingleEnumInput from "@/components/utils/primitives/SingleEnumInput.vue";
import {computed} from "vue";
import FieldEditorWrapper from "@/components/utils/FieldEditorWrapper.vue";
import PromptSectionEditor from "@/components/prompts/PromptSectionEditor.vue";
import {computedAsync} from "@vueuse/core";
import {LLMConnection, LLMConnectionData, LLMConnectionKeys} from "@/domain/Connection";
import {fetch_all} from "@/frameworks/ABSEntity";
import {EntityTypes} from "@/domain/EntityTypes";

const model = defineModel<PromptTemplate>({required:true, type:PromptTemplate});
const reasoningEffortIDtoName = computed<ReadonlyMap<ReasoningEffortId, string>>(() => {
  const map = new Map<ReasoningEffortId, string>()

  Object.values(REASONING_EFFORT).forEach(obj => {
    map.set(obj.id, obj.name)
  })

  return map
})
const sections = computedAsync<PromptSection[]>(async () => await model.value.getSections(), [])

const connections = computedAsync<LLMConnection[]>(async () => await fetch_all<LLMConnectionKeys, LLMConnectionData, LLMConnection>(
            EntityTypes.LLM,
            LLMConnection
        ), [])
const connectionIDs = computed<number[]>(() => connections.value.map(connection => connection.get("id")))
const connectionIDtoName = computed<ReadonlyMap<number, string>>(() => {
  const map = new Map<number, string>()

  connections.value.forEach(connection => {
    map.set(connection.get("id"), connection.get("name")!)
  })

  return map
})

const currentConnection = computed<number | null>({
  get(){
    const con = connections.value!.find(con => con.get('id') === model.value.get('connection_id'));
    if (!con) return null;
    return con.get('id');
  },
  set(newCon:number | null){
    if (!newCon) return;
    model.value.update('connection_id', newCon);
  }
})
async function createSection(){
  const name = window.prompt("Enter new section name");
  if (!name) return;
  sections.value.push(await model.value.createSection(name));
}
const orderedSections = computed<PromptSection[]>(() =>
    [...sections.value].sort((a, b) => a.get("position") - b.get("position"))
);

async function moveSection(section: PromptSection, direction: -1 | 1): Promise<void> {
  const ordered = orderedSections.value;
  const index = ordered.indexOf(section);
  const other = ordered[index + direction];

  if (!other) return;

  const sectionPosition = section.get("position");
  const otherPosition = other.get("position");

  await section.update("position", otherPosition);
  await other.update("position", sectionPosition);

  sections.value = [...sections.value].sort(
      (a, b) => a.get("position") - b.get("position")
  );
}
async function reload(){

}
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
      info="If this prompt runs, this will be the connection it'll hit"
    >
      <SingleEnumInput
          :value="currentConnection ? currentConnection : null"
          :possible_values="connectionIDs"
          :labels="connectionIDtoName"
          @edit="value => currentConnection = value"
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
    <button
      type="button"
      @click="createSection"
    >NEW</button>
    <PromptSectionEditor
        v-for = "section in orderedSections"
        :can-move-down="true"
        :can-move-up="true"
        :section="section"
        @move-down="section => moveSection(section, -1)"
        @move-up="section => moveSection(section, 1)"
    />
  </div>
</template>

<style scoped>

</style>