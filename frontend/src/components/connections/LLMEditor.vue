<script setup lang="ts">
import {
  LLMBackends,
  LLMConnection,
  type LLMBackend, getModelNames,
} from "@/domain/entities/Connection";

import { computed } from "vue";
import FieldEditorWrapper from "@/components/utils/FieldEditorWrapper.vue";
import SingleEnumInput from "@/components/utils/field-editors/SingleEnumInput.vue";
import {computedAsync} from "@vueuse/core";
import {st} from "vue-router/dist/router-CWoNjPRp";
import ShortTextBox from "@/components/utils/field-editors/ShortTextBox.vue";

const model = defineModel<LLMConnection>({
  required: true,
  type: LLMConnection,
});

const editingName = computed<string>({
  get() {
    return model.value.get("name");
  },
  set(value: string) {
    model.value.update("name", value);
  },
});

const connectionType = computed<number>({
  get() {
    return model.value.get("type") ?? LLMBackends.NANOGPT.id;
  },
  set(value: number) {
    model.value.update("type", value);
  },
});

const llm_model = computed<string | null>({
  get() {
    return model.value.get("model") ?? null;
  },
  set(value: string | null) {
    model.value.update("model", value);
  },
});
const llmBackendValues = Object.values(LLMBackends) as readonly LLMBackend[];

const llmBackendIDs = computed<number[]>(() => {
  return llmBackendValues.map((backend) => backend.id);
});

const llmBackendNames = computed<Record<number, string>>(() => {
  return Object.fromEntries(
      llmBackendValues.map((backend) => [backend.id, backend.name])
  ) as Record<number, string>;
});

function canEditHostByID(typeID: number): boolean {
  return typeID !== LLMBackends.NANOGPT.id;
}
const selectedBackend = computed<LLMBackend>(() => {
  return (
      llmBackendValues.find((backend) => backend.id === connectionType.value) ??
      LLMBackends.NANOGPT
  );
});

const modelOptions = computedAsync<string[]>(
    async () => getModelNames(selectedBackend.value),
    []
);


</script>

<template>
  <div class = background-edit-box>
    <FieldEditorWrapper field-name="name">
      <div>{{ editingName }}</div>
    </FieldEditorWrapper>

    <FieldEditorWrapper field-name="type" info="Connection type">
      <SingleEnumInput
          :value="connectionType"
          :possible_values="llmBackendIDs"
          :labels="llmBackendNames"
          @edit="connectionType = $event"
      />
    </FieldEditorWrapper>
    <div class="flex flex-row">
    <FieldEditorWrapper field-name="model">
      <SingleEnumInput
          :value="llm_model"
          :possible_values="modelOptions"
          @edit="i => llm_model = i"
      />
    </FieldEditorWrapper>
      <ShortTextBox
          model-value=""
    </div>
  </div>
</template>

<style scoped>
</style>