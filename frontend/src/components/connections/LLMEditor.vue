<script setup lang="ts">
import {
  LLMBackends,
  LLMConnection,
  type LLMBackend,
  getLLMModels,
  BackendLLMModel,
  getBackendFromID,
  ApiKey,
} from "@/domain/Connection";

import { computed, ref } from "vue";
import FieldEditorWrapper from "@/components/utils/FieldEditorWrapper.vue";
import SingleEnumInput from "@/components/utils/field-editors/SingleEnumInput.vue";
import { computedAsync } from "@vueuse/core";
import ShortTextBox from "@/components/utils/field-editors/ShortTextBox.vue";

const model = defineModel<LLMConnection>({
  required: true,
  type: LLMConnection,
});

const editingName = computed<string>({
  get() {
    return model.value.get("name")!;
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

const host_url = computed<string>({
  get() {
    return getBackendFromID(model.value.get("host_id")).host;
  },
  set() {},
});

const llmBackendValues = Object.values(LLMBackends) as readonly LLMBackend[];

const selectedBackend = computed<LLMBackend>(() => {
  return (
      llmBackendValues.find((backend) => backend.id === connectionType.value) ??
      LLMBackends.NANOGPT
  );
});

const llmBackendIDs = computed<number[]>(() => {
  return llmBackendValues.map((backend) => backend.id);
});

const llmBackendNames = computed<Record<number, string>>(() => {
  return Object.fromEntries(
      llmBackendValues.map((backend) => [backend.id, backend.name])
  ) as Record<number, string>;
});

const llm_model = computed<string | null>({
  get() {
    return model.value.get('modelID') ?? null;
  },
  set(value: string | null) {
    model.value.update("modelID", value);
  },
});

const modelOptions = computedAsync<BackendLLMModel[]>(
    async () => await getLLMModels(selectedBackend.value),
    []
);

const modelNames = computed<string[]>(() => modelOptions.value.map((i) => i.id));

const api_key = ref<string | null>(null);

async function createKey(key: string) {
  await model.value.assignNewKey(key);
  api_key.value = "";
}

const testingConnection = ref(false);
const connectionTestResult = ref<boolean | null>(null);

async function testConnection(): Promise<void> {
  testingConnection.value = true;
  connectionTestResult.value = null;

  try {
    connectionTestResult.value = await model.value.testConnection();
  } catch {
    connectionTestResult.value = false;
  } finally {
    testingConnection.value = false;
  }
}
</script>

<template>
  <div class="background-edit-box">
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
            :possible_values="modelNames"
            @edit="i => llm_model = i"
        />
      </FieldEditorWrapper>

      <FieldEditorWrapper
          field-name="api_key"
          info="Will be hidden as soon as its inputted, copy paste it"
      >
        <ShortTextBox
            :model-value="api_key"
            @edit="createKey"
        />
      </FieldEditorWrapper>
    </div>

    <div class="connection-test-row">
      <button
          type="button"
          :disabled="testingConnection"
          @click="testConnection"
      >
        {{ testingConnection ? "Testing..." : "Test connection" }}
      </button>

      <span v-if="connectionTestResult === true" class="connection-ok">
        Connection OK
      </span>

      <span v-else-if="connectionTestResult === false" class="connection-failed">
        Connection failed
      </span>
    </div>
  </div>
</template>

<style scoped>
.connection-test-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-top: 1rem;
}

.connection-ok {
  color: #15803d;
}

.connection-failed {
  color: #b91c1c;
}
</style>