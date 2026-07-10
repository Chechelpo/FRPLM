<script setup lang="ts">
import { computed, onMounted, ref } from "vue";

import {
  LLMBackendList,
  LLMBackends,
  type LLMBackend,
  LLMConnection,
  type ModelResponse,
  getBackendFromID,
} from "@/domain/Connection";
import { EntityTypes } from "@/domain/EntityTypes";

import { API_BASE } from "@/config";
import { fetchApi } from "@/core/ABSEntity";

import FieldEditorWrapper from "@/components/utils/FieldEditorWrapper.vue";
import ShortTextBox from "@/components/utils/primitiveEditors/ShortTextBox.vue";
import SingleEnumInput from "@/components/utils/primitiveEditors/SingleEnumInput.vue";

const model = defineModel<LLMConnection>({
  required: true,
});

interface HostDto {
  hostId: number;
  url: string;
}

const connectionType = computed<LLMBackend>(() =>
    getBackendFromID(model.value.get("host_id")),
);

const llmBackendNames = computed<string[]>(() =>
    LLMBackendList.map((backend) => backend.name),
);

const customHost = ref<HostDto | null>(null);
const apiKey = ref("");

const modelOptions = ref<ModelResponse[]>([]);
const modelNames = computed<string[]>(() =>
    modelOptions.value.map((option) => option.id),
);

const testingConnection = ref(false);
const connectionTestResult = ref<boolean | null>(null);

const llmModel = computed<string | null>({
  get() {
    return model.value.get("modelID") ?? null;
  },

  set(value) {
    if (!value) {
      model.value.update("modelID", "");
      return;
    }

    const selectedModel = modelOptions.value.find(
        (modelOption) => modelOption.id === value,
    );

    if (!selectedModel) {
      return;
    }

    model.value.update("modelID", selectedModel.id);
    model.value.update(
        "max_tokens",
        selectedModel.context_length ?? 52_000,
    );
  },
});

async function loadModels(): Promise<void> {
  modelOptions.value = await model.value.getModels();
}

async function updateBackend(typeName: string): Promise<void> {
  const newBackend = LLMBackendList.find(
      (backend) => backend.name === typeName,
  );

  if (!newBackend) {
    return;
  }

  connectionTestResult.value = null;

  if (newBackend !== LLMBackends.CUSTOM_OPENAI) {
    customHost.value = null;

    await model.value.update("host_id", newBackend.id);
    await model.value.update("modelID", "");
    await loadModels();

    return;
  }

  await assignCustomBackend("");
}

async function assignCustomBackend(hostUrl: string): Promise<void> {
  const encodedUrl = encodeURIComponent(hostUrl);

  const response = await fetchApi(
      `${API_BASE}/${EntityTypes.LLM}/${model.value.get("id")}/assignHost?url=${encodedUrl}`,
      {
        method: "PUT",
      },
  );

  const newHost = (await response.json()) as HostDto;

  await model.value.update("host_id", newHost.hostId);

  customHost.value = newHost;
  connectionTestResult.value = null;
}

async function getCustomHost(): Promise<void> {
  if (connectionType.value !== LLMBackends.CUSTOM_OPENAI) {
    customHost.value = null;
    return;
  }

  const response = await fetchApi(
      `${API_BASE}/${EntityTypes.LLM}/host/${model.value.get("host_id")}`,
      {
        method: "GET",
      },
  );

  customHost.value = (await response.json()) as HostDto;
}

async function createKey(key: string): Promise<void> {
  try {
    await model.value.assignNewKey(key);

    connectionTestResult.value = null;

    // Some providers change their model list based on the API key.
    await loadModels();
  } finally {
    apiKey.value = "";
  }
}

async function testConnection(): Promise<void> {
  testingConnection.value = true;
  connectionTestResult.value = null;

  try {
    connectionTestResult.value =
        await model.value.testConnection();
  } catch {
    connectionTestResult.value = false;
  } finally {
    testingConnection.value = false;
  }
}

onMounted(async () => {
  await Promise.all([
    loadModels(),
    getCustomHost(),
  ]);
});
</script>

<template>
  <section
      class="edit-box edit-box--accent connection-editor"
      :aria-busy="testingConnection"
  >
    <header class="edit-box__header">
      <div class="edit-box__header-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24">
          <rect x="3" y="4" width="18" height="6" rx="2" />
          <rect x="3" y="14" width="18" height="6" rx="2" />
          <path d="M7 7h.01" />
          <path d="M7 17h.01" />
          <path d="M11 7h6" />
          <path d="M11 17h6" />
        </svg>
      </div>

      <div class="edit-box__header-main">
        <span class="edit-box__eyebrow">
          Language model
        </span>

        <div class="edit-box__title-row">
          <h2 class="edit-box__title">
            Connection settings
          </h2>
        </div>

        <p class="edit-box__description">
          Configure the provider, model, credentials, and endpoint used
          by this LLM connection.
        </p>
      </div>

      <div class="edit-box__actions">
        <span class="edit-box__badge">
          {{ connectionType.name }}
        </span>
      </div>
    </header>

    <div class="edit-box__body">
      <div class="edit-box__stack">
        <section class="edit-box__section">
          <div class="edit-box__section-header">
            <div class="edit-box__section-heading">
              <h3 class="edit-box__section-title">
                Connection identity
              </h3>

              <p class="edit-box__section-description">
                Set the display name and select the backend provider.
              </p>
            </div>
          </div>

          <div class="edit-box__grid">
            <div class="connection-editor__field">
              <FieldEditorWrapper field-name="name">
                <div class="connection-editor__value">
                  {{ model.get("name") }}
                </div>
              </FieldEditorWrapper>
            </div>

            <div class="connection-editor__field">
              <FieldEditorWrapper
                  field-name="type"
                  info="Connection type"
              >
                <SingleEnumInput
                    :value="connectionType.name"
                    :possible_values="llmBackendNames"
                    @edit="updateBackend"
                />
              </FieldEditorWrapper>
            </div>
          </div>
        </section>

        <section class="edit-box__section edit-box__section--accent">
          <div class="edit-box__section-header">
            <div class="edit-box__section-heading">
              <h3 class="edit-box__section-title">
                Model configuration
              </h3>

              <p class="edit-box__section-description">
                Select the model and configure the endpoint when using
                an OpenAI-compatible custom provider.
              </p>
            </div>

            <span class="edit-box__count">
              {{ modelOptions.length }}
              {{ modelOptions.length === 1 ? "model" : "models" }}
            </span>
          </div>

          <div class="edit-box__grid">
            <div class="connection-editor__field">
              <FieldEditorWrapper field-name="model">
                <SingleEnumInput
                    :value="llmModel"
                    :possible_values="modelNames"
                    @edit="llmModel = $event"
                />
              </FieldEditorWrapper>
            </div>

            <div
                v-if="
                connectionType === LLMBackends.CUSTOM_OPENAI &&
                customHost
              "
                class="connection-editor__field"
            >
              <FieldEditorWrapper
                  field-name="Host"
                  info="Base URL of the OpenAI-compatible API"
              >
                <ShortTextBox
                    :model-value="customHost.url"
                    @edit="assignCustomBackend"
                />
              </FieldEditorWrapper>
            </div>
          </div>
        </section>

        <section class="edit-box__section">
          <div class="edit-box__section-header">
            <div class="edit-box__section-heading">
              <h3 class="edit-box__section-title">
                Authentication
              </h3>

              <p class="edit-box__section-description">
                Enter a new API key. The value is cleared from the
                interface immediately after it is stored.
              </p>
            </div>
          </div>

          <div class="connection-editor__field">
            <FieldEditorWrapper
                field-name="api_key"
                info="Paste the API key. It will be hidden after submission."
            >
              <ShortTextBox
                  :key="apiKey"
                  :model-value="apiKey"
                  @edit="createKey"
              />
            </FieldEditorWrapper>
          </div>
        </section>
      </div>
    </div>

    <footer class="edit-box__footer connection-editor__footer">
      <div
          class="connection-editor__status"
          role="status"
          aria-live="polite"
      >
        <span
            v-if="connectionTestResult === true"
            class="edit-box__badge edit-box__badge--success"
        >
          Connection OK
        </span>

        <span
            v-else-if="connectionTestResult === false"
            class="edit-box__badge edit-box__badge--danger"
        >
          Connection failed
        </span>
      </div>

      <button
          type="button"
          class="
          edit-box__action
          edit-box__action--accent
          connection-editor__test-button
        "
          :disabled="testingConnection"
          @click="testConnection"
      >
        <span
            v-if="testingConnection"
            class="edit-box__spinner"
            aria-hidden="true"
        />

        {{ testingConnection ? "Testing..." : "Test connection" }}
      </button>
    </footer>
  </section>
</template>

<style scoped>
.connection-editor__field {
  min-width: 0;
}

.connection-editor__value {
  min-width: 0;

  color: rgb(var(--c-fg-strong));

  font-weight: 700;
  line-height: 1.45;

  overflow-wrap: anywhere;
}

.connection-editor__footer {
  justify-content: space-between;
}

.connection-editor__status {
  min-width: 0;
  min-height: 1.45rem;

  display: flex;
  align-items: center;
}

.connection-editor__test-button {
  min-width: 9.75rem;
}

@media (max-width: 480px) {
  .connection-editor__footer {
    align-items: stretch;
    flex-direction: column;
  }

  .connection-editor__test-button {
    width: 100%;
  }
}
</style>