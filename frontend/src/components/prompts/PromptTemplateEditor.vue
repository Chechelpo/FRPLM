<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { computedAsync } from "@vueuse/core";

import {
  PromptSection,
  PromptTemplate,
  REASONING_EFFORT,
  REASONING_EFFORT_IDs,
  type ReasoningEffortId,
} from "@/domain/Prompts";
import {
  LLMConnection,
  type LLMConnectionData,
  type LLMConnectionKeys,
} from "@/domain/Connection";
import { EntityTypes } from "@/domain/EntityTypes";
import { fetch_all } from "@/core/ABSEntity";

import ShortTextBox from "@/components/primitive-editors/ShortTextBox.vue";
import NumberSlider from "@/components/primitive-editors/NumberSlider.vue";
import BooleanToggle from "@/components/primitive-editors/BooleanToggle.vue";
import SingleEnumInput from "@/components/primitive-editors/SingleEnumInput.vue";
import FieldEditorWrapper from "@/components/utils/FieldEditorWrapper.vue";
import PromptSectionEditor from "@/components/prompts/PromptSectionEditor.vue";
import Expandable from "@/components/utils/panels/Expandable.vue";

const model = defineModel<PromptTemplate>({
  required: true,
  type: PromptTemplate,
});

// -----------------------------------------------------------------------------
// Reasoning effort
// -----------------------------------------------------------------------------

const reasoningEffortIDtoName = computed<
    ReadonlyMap<ReasoningEffortId, string>
>(() => {
  const labels = new Map<ReasoningEffortId, string>();

  Object.values(REASONING_EFFORT).forEach(effort => {
    labels.set(effort.id, effort.name);
  });

  return labels;
});

// -----------------------------------------------------------------------------
// Connections
// -----------------------------------------------------------------------------

const connections = computedAsync<LLMConnection[]>(
    async () => {
      return await fetch_all<
          LLMConnectionKeys,
          LLMConnectionData,
          LLMConnection
      >(
          EntityTypes.LLM,
          LLMConnection,
      );
    },
    [],
);

const connectionIDs = computed<number[]>(() => {
  return connections.value.map(connection =>
      connection.get("id"),
  );
});

const connectionIDtoName = computed<ReadonlyMap<number, string>>(
    () => {
      const labels = new Map<number, string>();

      connections.value.forEach(connection => {
        labels.set(
            connection.get("id"),
            connection.get("name") ?? `Connection ${connection.get("id")}`,
        );
      });

      return labels;
    },
);

const currentConnection = computed<number | null>({
  get() {
    return model.value.get("connection_id") ?? null;
  },

  set(connectionId: number | null) {
    if (connectionId === null) {
      return;
    }

    model.value.update("connection_id", connectionId);
  },
});

const currentConnectionObject = computed<LLMConnection | null>(
    () => {
      const connectionId = currentConnection.value;

      if (connectionId === null) {
        return null;
      }

      return (
          connections.value.find(
              connection =>
                  connection.get("id") === connectionId,
          ) ?? null
      );
    },
);

const maximumTokensForConnection = computed<number>(() => {
  return (
      currentConnectionObject.value?.get("max_tokens") ??
      model.value.get("max_tokens")
  );
});

const maxTokens = computed<number>({
  get() {
    return model.value.get("max_tokens");
  },

  set(value: number) {
    model.value.update("max_tokens", value);
  },
});
const lorebookBudget = computed<number>({
  get() {
    return model.value.get("lorebooks_budget");
  },
  set(value: number) {
    const clamped = Math.min(
        value,
        1 - chatHistoryBudget.value,
    );

    model.value.update("lorebooks_budget", clamped);
  },
});

const chatHistoryBudget = computed<number>({
  get() {
    return model.value.get("chat_history_budget");
  },
  set(value: number) {
    const clamped = Math.min(
        value,
        1 - lorebookBudget.value,
    );

    model.value.update("chat_history_budget", clamped);
  },
});
// -----------------------------------------------------------------------------
// Sections
// -----------------------------------------------------------------------------

const sections = ref<PromptSection[]>([]);
const loadingSections = ref(false);

const orderedSections = computed<PromptSection[]>(() => {
  return [...sections.value].sort(
      (first, second) =>
          first.get("position") - second.get("position"),
  );
});

onMounted(reload);

async function reload(): Promise<void> {
  loadingSections.value = true;

  try {
    sections.value = await model.value.getSections();
  } finally {
    loadingSections.value = false;
  }
}

async function createSection(): Promise<void> {
  const name = window.prompt("Enter new section name:");

  if (!name?.trim()) {
    return;
  }

  await model.value.createSection(name.trim());

  /*
   * Reloading is intentional because section positions are assigned by the
   * backend.
   */
  await reload();
}

async function moveSection(
    section: PromptSection,
    direction: -1 | 1,
): Promise<void> {
  const ordered = orderedSections.value;
  const index = ordered.indexOf(section);

  if (index < 0) {
    return;
  }

  const adjacentSection = ordered[index + direction];

  if (!adjacentSection) {
    return;
  }

  const succeeded = await PromptSection.exchange(
      model.value,
      section,
      adjacentSection,
  );

  if (succeeded) {
    sections.value = [...sections.value];
  }
}

function sectionKey(section: PromptSection): string {
  return `${section.get("prompt_id")}:${section.get("section_id")}`;
}
</script>

<template>
  <section class="prompt-editor edit-box edit-box--accent">
    <header class="edit-box__header">
      <div class="edit-box__header-icon">
        <svg
            viewBox="0 0 24 24"
            aria-hidden="true"
        >
          <path d="M4 5h16" />
          <path d="M4 12h16" />
          <path d="M4 19h10" />
          <path d="m17 17 2 2 4-4" />
        </svg>
      </div>

      <div class="edit-box__header-main">
        <span class="edit-box__eyebrow">
          Prompt configuration
        </span>

        <div class="edit-box__title-row">
          <h1 class="edit-box__title">
            {{ model.get("name") || "Untitled prompt" }}
          </h1>

          <span class="edit-box__count">
            {{ orderedSections.length }}
            {{ orderedSections.length === 1 ? "section" : "sections" }}
          </span>
        </div>

        <p class="edit-box__description">
          Configure the model connection, generation parameters and ordered
          prompt sections.
        </p>
      </div>
    </header>

    <div class="edit-box__body prompt-editor__body">
      <!-- General configuration -->
      <section class="edit-box__section">
        <header class="edit-box__section-header">
          <div class="edit-box__section-heading">
            <h2 class="edit-box__section-title">
              General
            </h2>

            <p class="edit-box__section-description">
              Basic identity and model connection.
            </p>
          </div>
        </header>

        <div class="prompt-editor__fields">
          <FieldEditorWrapper field-name="Name">
            <ShortTextBox
                :model-value="model.get('name')"
                @edit="value => model.update('name', value)"
            />
          </FieldEditorWrapper>

          <FieldEditorWrapper
              field-name="Connection"
              info="The backend connection used when this prompt is executed."
          >
            <SingleEnumInput
                :value="currentConnection"
                :possible_values="connectionIDs"
                :labels="connectionIDtoName"
                placeholder="Select a connection"
                @edit="value => currentConnection = value"
            />
          </FieldEditorWrapper>

          <FieldEditorWrapper
              v-if="currentConnectionObject"
              field-name="Max tokens"
              info="Maximum output token count supported by the selected connection."
          >
            <NumberSlider
                :model-value="maxTokens"
                :max="maximumTokensForConnection"
                :step="8192"
                @edit="value => maxTokens = value"
            />
          </FieldEditorWrapper>

          <div
              v-else-if="connections.length === 0"
              class="prompt-editor__connection-state"
          >
            <span class="edit-box__spinner" />

            <span>Loading available connections...</span>
          </div>
        </div>
      </section>

      <!-- Budgeting configuration -->
      <section class="edit-box__section">
        <header class="edit-box__section-header">
          <div class="edit-box__section-heading">
            <h2 class="edit-box__section-title">
              Context budgeting
            </h2>

            <p class="edit-box__section-description">
              Allocate the available context budget between chat history and
              lorebook content.
            </p>
          </div>
        </header>

        <div class="prompt-editor__parameter-grid">
          <FieldEditorWrapper
              field-name="Chat history budget"
              info="Maximum proportion of the context budget allocated to chat history."
          >
            <NumberSlider
                :max="1 - lorebookBudget"
                :step="0.01"
                :model-value="chatHistoryBudget"
                @edit="
                  value => chatHistoryBudget = value
                "
            />
          </FieldEditorWrapper>

          <FieldEditorWrapper
              field-name="Lorebooks budget"
              info="Maximum proportion of the context budget allocated to lorebook content."
          >
            <NumberSlider
                :max="1 - chatHistoryBudget"
                :step="0.01"
                :model-value="lorebookBudget"
                @edit="
                  value => lorebookBudget = value
                "
            />
          </FieldEditorWrapper>
        </div>
      </section>

      <!-- Sampling parameters -->
      <section class="edit-box__section">
        <header class="edit-box__section-header">
          <div class="edit-box__section-heading">
            <h2 class="edit-box__section-title">
              Sampling
            </h2>

            <p class="edit-box__section-description">
              Control randomness, token selection and repetition behavior.
            </p>
          </div>
        </header>

        <div class="prompt-editor__parameter-grid">
          <FieldEditorWrapper field-name="Temperature">
            <NumberSlider
                :max="2"
                :step="0.1"
                :model-value="model.get('temperature')"
                @edit="value => model.update('temperature', value)"
            />
          </FieldEditorWrapper>

          <FieldEditorWrapper field-name="Top P">
            <NumberSlider
                :max="2"
                :step="0.1"
                :model-value="model.get('top_p')"
                @edit="value => model.update('top_p', value)"
            />
          </FieldEditorWrapper>

          <FieldEditorWrapper field-name="Top K">
            <NumberSlider
                :max="2"
                :step="0.1"
                :model-value="model.get('top_k')"
                @edit="value => model.update('top_k', value)"
            />
          </FieldEditorWrapper>

          <FieldEditorWrapper field-name="Presence penalty">
            <NumberSlider
                :max="2"
                :step="0.1"
                :model-value="model.get('presence_penalty')"
                @edit="value => model.update('presence_penalty', value)"
            />
          </FieldEditorWrapper>

          <FieldEditorWrapper field-name="Repetition penalty">
            <NumberSlider
                :max="2"
                :step="0.1"
                :model-value="model.get('repetition_penalty')"
                @edit="value => model.update('repetition_penalty', value)"
            />
          </FieldEditorWrapper>
        </div>
      </section>

      <!-- Execution and reasoning -->
      <section class="edit-box__section">
        <header class="edit-box__section-header">
          <div class="edit-box__section-heading">
            <h2 class="edit-box__section-title">
              Execution and reasoning
            </h2>

            <p class="edit-box__section-description">
              Configure streaming and model reasoning behavior.
            </p>
          </div>
        </header>

        <div class="prompt-editor__parameter-grid">
          <FieldEditorWrapper
              field-name="Streaming"
              info="Return generated content incrementally as it becomes available."
          >
            <BooleanToggle
                :model-value="model.get('streaming')"
                @edit="value => model.update('streaming', value)"
            />
          </FieldEditorWrapper>

          <FieldEditorWrapper
              field-name="Exclude reasoning"
              info="Prevent reasoning content from being included in the final response."
          >
            <BooleanToggle
                :model-value="model.get('exclude_reasoning')"
                @edit="value => model.update('exclude_reasoning', value)"
            />
          </FieldEditorWrapper>

          <FieldEditorWrapper field-name="Reasoning effort">
            <SingleEnumInput
                :value="model.get('reasoning_effort')"
                :possible_values="REASONING_EFFORT_IDs"
                :labels="reasoningEffortIDtoName"
                placeholder="Select reasoning effort"
                @edit="value => model.update('reasoning_effort', value)"
            />
          </FieldEditorWrapper>
        </div>
      </section>

      <!-- Prompt sections -->
      <section class="edit-box__section edit-box__section--accent">
        <Expandable title="Sections" variant="compact">
          <div class="prompt-sections">
            <header class="edit-box__toolbar prompt-sections__toolbar">
              <div class="prompt-sections__heading">
                <span class="edit-box__eyebrow">
                  Ordered content
                </span>

                <div class="prompt-sections__title-row">
                  <strong class="prompt-sections__title">
                    Prompt sections
                  </strong>

                  <span class="edit-box__count">
                    {{ orderedSections.length }}
                  </span>
                </div>
              </div>

              <div class="edit-box__toolbar-actions">
                <button
                    class="edit-box__action edit-box__action--accent"
                    type="button"
                    @click="createSection"
                >
                  <svg
                      viewBox="0 0 24 24"
                      aria-hidden="true"
                  >
                    <path d="M12 5v14" />
                    <path d="M5 12h14" />
                  </svg>

                  New section
                </button>
              </div>
            </header>

            <div
                v-if="loadingSections"
                class="edit-box__state"
                aria-live="polite"
            >
              <span class="edit-box__spinner" />

              <div class="edit-box__state-content">
                <strong class="edit-box__state-title">
                  Loading sections
                </strong>

                <p class="edit-box__state-description">
                  Retrieving the ordered prompt structure.
                </p>
              </div>
            </div>

            <div
                v-else-if="orderedSections.length"
                class="prompt-sections__list"
            >
              <article
                  v-for="(section, index) in orderedSections"
                  :key="sectionKey(section)"
                  class="prompt-sections__item"
              >
                <div
                    class="prompt-sections__position"
                    :title="`Section position ${index + 1}`"
                >
                  {{ index + 1 }}
                </div>

                <PromptSectionEditor
                    class="prompt-sections__editor"
                    :section="section"
                    :index="index"
                    :can-move-up="index > 0"
                    :can-move-down="index < orderedSections.length - 1"
                    @move-up="target => moveSection(target, -1)"
                    @move-down="target => moveSection(target, 1)"
                />
              </article>
            </div>

            <div
                v-else
                class="edit-box__state edit-box__state--vertical"
            >
              <div class="edit-box__state-icon">
                <svg
                    viewBox="0 0 24 24"
                    aria-hidden="true"
                >
                  <path d="M4 5h16" />
                  <path d="M4 12h16" />
                  <path d="M4 19h10" />
                </svg>
              </div>

              <div class="edit-box__state-content">
                <strong class="edit-box__state-title">
                  No prompt sections
                </strong>

                <p class="edit-box__state-description">
                  Create a section to begin constructing this prompt.
                </p>
              </div>

              <button
                  class="edit-box__action edit-box__action--accent"
                  type="button"
                  @click="createSection"
              >
                Create first section
              </button>
            </div>
          </div>
        </Expandable>
      </section>
    </div>
  </section>
</template>

<style scoped>
.prompt-editor {
  width: 100%;
  min-width: 0;
}

.prompt-editor__body {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.prompt-editor__fields {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);

  min-width: 0;
}

.prompt-editor__parameter-grid {
  display: grid;
  grid-template-columns:
    repeat(
      auto-fit,
      minmax(min(100%, 18rem), 1fr)
    );

  align-items: start;
  gap: var(--space-3);

  min-width: 0;
}

.prompt-editor__connection-state {
  min-height: 2.75rem;

  display: flex;
  align-items: center;
  gap: var(--space-2);

  padding: var(--space-3);

  color: rgb(var(--c-muted));

  background: rgb(var(--c-surface-raised) / 0.34);
  border: 1px dashed rgb(var(--c-border) / 0.32);
  border-radius: var(--radius-sm);

  font-size: 0.8rem;
}

/* -------------------------------------------------------------------------- */
/* Section toolbar                                                            */
/* -------------------------------------------------------------------------- */

.prompt-sections {
  min-width: 0;
  padding: var(--space-3);
}

.prompt-sections__toolbar {
  align-items: center;
}

.prompt-sections__heading {
  flex: 1 1 auto;
  min-width: 0;
}

.prompt-sections__heading .edit-box__eyebrow {
  margin-bottom: var(--space-1);
}

.prompt-sections__title-row {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.prompt-sections__title {
  color: rgb(var(--c-fg-strong));

  font-size: 0.92rem;
  line-height: 1.3;
}

.prompt-sections__toolbar svg,
.edit-box__action svg {
  width: 1rem;
  height: 1rem;

  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

/* -------------------------------------------------------------------------- */
/* Section list                                                               */
/* -------------------------------------------------------------------------- */

.prompt-sections__list {
  position: relative;

  display: flex;
  flex-direction: column;
  gap: var(--space-3);

  min-width: 0;

  margin: 0;
  padding: 0 0 0 2rem;
}

.prompt-sections__list::before {
  content: "";

  position: absolute;
  top: 1rem;
  bottom: 1rem;
  left: 0.72rem;

  width: 2px;

  background:
      linear-gradient(
          to bottom,
          rgb(var(--c-accent) / 0.66),
          rgb(var(--c-primary) / 0.12)
      );

  border-radius: var(--radius-round);
}

.prompt-sections__item {
  position: relative;

  min-width: 0;

  padding: var(--space-1);

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-surface-raised) / 0.36),
          rgb(var(--c-surface-2) / 0.18)
      );

  border: 1px solid rgb(var(--c-primary) / 0.18);
  border-radius: var(--radius-md);

  box-shadow:
      0 5px 16px rgb(var(--c-shadow) / 0.055),
      inset 0 1px 0 rgb(var(--c-surface-raised) / 0.3);

  transition:
      background-color var(--duration-normal) var(--ease-standard),
      border-color var(--duration-normal) var(--ease-standard),
      box-shadow var(--duration-normal) var(--ease-standard);
}

.prompt-sections__item::before {
  content: "";

  position: absolute;
  top: 1.35rem;
  left: -1.3rem;

  width: 1.3rem;
  height: 2px;

  background: rgb(var(--c-primary) / 0.52);
}

.prompt-sections__item:hover {
  background:
      linear-gradient(
          145deg,
          rgb(var(--c-surface-raised) / 0.5),
          rgb(var(--c-surface-hover) / 0.3)
      );

  border-color: rgb(var(--c-primary) / 0.34);

  box-shadow:
      0 8px 20px rgb(var(--c-shadow) / 0.075),
      inset 0 1px 0 rgb(var(--c-surface-raised) / 0.36);
}

.prompt-sections__position {
  position: absolute;
  top: 0.8rem;
  left: -2rem;
  z-index: 1;

  width: 1.45rem;
  height: 1.45rem;
  box-sizing: border-box;

  display: grid;
  place-items: center;

  color: rgb(var(--c-on-accent));

  background: rgb(var(--c-accent));
  border: 2px solid rgb(var(--c-primary));
  border-radius: 50%;

  box-shadow:
      0 0 0 3px rgb(var(--c-accent) / 0.13);

  font-size: 0.66rem;
  font-weight: 800;
}

.prompt-sections__editor {
  min-width: 0;
  overflow-x: clip;
}

/* -------------------------------------------------------------------------- */
/* Responsive                                                                 */
/* -------------------------------------------------------------------------- */

@media (max-width: 700px) {
  .prompt-editor__parameter-grid {
    grid-template-columns: 1fr;
  }

  .prompt-sections {
    padding: var(--space-2);
  }

  .prompt-sections__toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .prompt-sections__toolbar-actions {
    align-self: flex-end;
  }

  .prompt-sections__list {
    padding-left: 1.55rem;
  }

  .prompt-sections__list::before {
    left: 0.52rem;
  }

  .prompt-sections__item::before {
    left: -1.03rem;
    width: 1.03rem;
  }

  .prompt-sections__position {
    left: -1.55rem;
  }
}

@media (max-width: 480px) {
  .prompt-sections__toolbar-actions,
  .prompt-sections__toolbar-actions .edit-box__action {
    width: 100%;
  }

  .prompt-sections__list {
    padding-left: 0;
  }

  .prompt-sections__list::before,
  .prompt-sections__item::before,
  .prompt-sections__position {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .prompt-sections__item {
    transition: none;
  }
}
</style>