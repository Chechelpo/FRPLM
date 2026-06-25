<script setup lang="ts">
import {
  LLMBackends,
  LLMConnection,
  type LLMBackend,
  getBackendFromID,
  ModelResponse, LLMBackendList,
} from "@/domain/Connection";

import {computed, onMounted, ref} from "vue";
import FieldEditorWrapper from "@/components/utils/FieldEditorWrapper.vue";
import SingleEnumInput from "@/components/utils/primitiveEditors/SingleEnumInput.vue";
import { computedAsync } from "@vueuse/core";
import ShortTextBox from "@/components/utils/primitiveEditors/ShortTextBox.vue";
import {fetchApi} from "@/frameworks/ABSEntity";
import {API_BASE} from "@/config";
import {EntityTypes} from "@/domain/EntityTypes";

const model = defineModel<LLMConnection>({
  required: true,
});

const connectionType = computed<LLMBackend>(() => getBackendFromID(model.value.get('host_id')))
const llmBackendNames = computed<string[]>(() => LLMBackendList.map(back => back.name))

async function updateBackend(typeName:string){
  const newCon = LLMBackendList.find(back => back.name == typeName)!;
  if (newCon != LLMBackends.CUSTOM_OPENAI){
    model.value.update('host_id', newCon.id!);
    model.value.update('modelID', "");
    await loadModels();
    return;
  }

  await assignCustomBackend("")
}

interface HostDto {
  hostId:number,
  url:string
}
async function assignCustomBackend(hostUrl:string){
  const newHost = await fetchApi(
      `${API_BASE}/${EntityTypes.LLM}/${model.value.get('id')}/assignHost?url=${hostUrl}`,
      {
        method:'PUT'
      }
  ).then(async response => await response.json() as HostDto)
  await model.value.update('host_id', newHost.hostId);
  customHost.value = newHost;
}
const customHost = ref<HostDto | null>(null);
async function getCustomHost(){
  if (connectionType.value != LLMBackends.CUSTOM_OPENAI){
    return;
  }
  customHost.value = await fetchApi(
      `${API_BASE}/${EntityTypes.LLM}/host/${model.value.get('host_id')}`,
      {
        method:'GET'
      }
  ).then(async response => await response.json() as HostDto)
}

const api_key = ref<string>("");
async function createKey(key: string) {
  try{
    await model.value.assignNewKey(key);
    await loadModels() //Some providers change the available model list based on the key
  } finally {
    api_key.value = "";
  }
}

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
onMounted( () => {
  loadModels()
  getCustomHost()
})
</script>

<template>
  <div class="background-edit-box">
    <FieldEditorWrapper field-name="name">
      <div>{{ model.get('name') }}</div>
    </FieldEditorWrapper>

    <FieldEditorWrapper field-name="type" info="Connection type">
      <SingleEnumInput
          :value="connectionType.name"
          :possible_values="llmBackendNames"
          @edit="value => updateBackend(value)"
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
      <FieldEditorWrapper v-if="connectionType == LLMBackends.CUSTOM_OPENAI && customHost" field-name="Host">
        <ShortTextBox
          :model-value="customHost.url"
          @edit="value => assignCustomBackend(value)"
        />
      </FieldEditorWrapper>
      <FieldEditorWrapper
          field-name="api_key"
          info="Will be hidden as soon as its inputted, copy paste it"
      >
        <ShortTextBox
            :key="api_key"
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