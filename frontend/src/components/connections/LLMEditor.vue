<script setup lang="ts">
import {
  LLMBackends,
  LLMConnection,
  type LLMBackend,
  getBackendFromID,
  ModelResponse,
} from "@/domain/Connection";

import {computed, onMounted, ref} from "vue";
import FieldEditorWrapper from "@/components/utils/FieldEditorWrapper.vue";
import SingleEnumInput from "@/components/utils/primitiveEditors/SingleEnumInput.vue";
import { computedAsync } from "@vueuse/core";
import ShortTextBox from "@/components/utils/primitiveEditors/ShortTextBox.vue";

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
    const modelResponse = modelOptions.value.find(model => model.id === value)!;
    model.value.update("modelID", modelResponse.id);
    model.value.update("max_tokens", modelResponse.context_length ? modelResponse.context_length : 52000);
  },
});

const modelOptions = ref<ModelResponse[]>([]);

const modelNames = computed<string[]>(() => modelOptions.value.map((i) => i.id));

async function loadModels() : Promise<void> {
  modelOptions.value = await model.value.getModels();
}

const api_key = ref<string | null>(null);

async function createKey(key: string) {
  await model.value.assignNewKey(key);
  api_key.value = "";
  await loadModels() //Some providers change the available model list based on the key
}

const testingConnection = ref(false);
const connectionTestResult = ref<boolean | null>(null);

async function testConnection(): Promise<void> {
  testingConnection.value = true;
  connectionTestResult.value = null;

  try {
    console.debug("Testing connection");
    const response = await model.value.testConnection();
    console.log(`Connection test result: ${response}`);
    connectionTestResult.value = response;
  } catch {
    connectionTestResult.value = false;
  } finally {
    testingConnection.value = false;
  }
}
onMounted( () => {loadModels()})
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