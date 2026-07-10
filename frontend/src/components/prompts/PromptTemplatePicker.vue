<script setup lang="ts">
import {
  computed,
  onMounted,
  ref,
  shallowRef,
} from "vue";

import { Session } from "@/domain/Session";
import {
  PromptTemplate,
  type PromptTemplateData,
  type PromptTemplateKey,
} from "@/domain/Prompts";
import { EntityTypes } from "@/domain/EntityTypes";
import { createEntity } from "@/core/ABSEntity";

import PromptTemplateEditor from "@/components/prompts/PromptTemplateEditor.vue";
import SingleEnumInput from "@/components/utils/primitiveEditors/SingleEnumInput.vue";
import IconButton from "@/components/utils/buttons/IconButton.vue";

const props = defineProps<{
  session: Session;
}>();

const template = shallowRef<PromptTemplate | null>(null);
const allTemplates = ref<PromptTemplate[]>([]);

const loading = ref(true);
const creatingTemplate = ref(false);

const templateNames = computed<string[]>(() =>
    allTemplates.value
        .map((candidate) =>
            String(candidate.get("name") ?? ""),
        )
        .filter(Boolean),
);

const selectedTemplateName = computed<string>(() =>
    template.value
        ? String(template.value.get("name") ?? "")
        : "",
);

async function selectNewTemplate(
    selectedTemplate: PromptTemplate,
): Promise<void> {
  await props.session.update(
      "template_id",
      selectedTemplate.get("id"),
  );

  template.value = selectedTemplate;
}

async function selectTemplateByName(
    templateName: string,
): Promise<void> {
  const selectedTemplate = allTemplates.value.find(
      (candidate) =>
          String(candidate.get("name") ?? "") === templateName,
  );

  if (!selectedTemplate) {
    return;
  }

  await selectNewTemplate(selectedTemplate);
}

async function createTemplate(): Promise<void> {
  const name = window.prompt("New template name")?.trim();

  if (!name) {
    return;
  }

  creatingTemplate.value = true;

  try {
    const newTemplate = await createEntity<
        PromptTemplateKey,
        PromptTemplateData,
        PromptTemplate
    >(
        null,
        {
          name,
        },
        EntityTypes.TEMPLATES,
        PromptTemplate,
    );

    allTemplates.value.push(newTemplate);

    await selectNewTemplate(newTemplate);
  } finally {
    creatingTemplate.value = false;
  }
}

onMounted(async () => {
  loading.value = true;

  try {
    const [
      sessionTemplate,
      templates,
    ] = await Promise.all([
      props.session.getTemplate(),
      PromptTemplate.getAll(),
    ]);

    template.value = sessionTemplate;
    allTemplates.value = templates;
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <div
      class="session-prompt-config"
      :aria-busy="loading"
  >
    <div
        v-if="loading"
        class="
        edit-box__state
        edit-box__state--vertical
        session-prompt-config__state
      "
        role="status"
    >
      <span
          class="edit-box__spinner"
          aria-hidden="true"
      />

      <span>Loading prompt configuration...</span>
    </div>

    <template v-else>
      <div class="session-prompt-config__toolbar">
        <div class="session-prompt-config__toolbar-text">
          <span class="session-prompt-config__title">
            Prompt template
          </span>

          <span class="session-prompt-config__description">
            Select the template used to construct model requests.
          </span>
        </div>

        <IconButton
            title="Create new prompt template"
            variant="accent"
            :loading="creatingTemplate"
            @click="createTemplate"
        >
          <svg
              viewBox="0 0 24 24"
              aria-hidden="true"
          >
            <path d="M12 5v14" />
            <path d="M5 12h14" />
          </svg>
        </IconButton>
      </div>

      <SingleEnumInput
          :value="selectedTemplateName"
          :possible_values="templateNames"
          @edit="selectTemplateByName"
      />

      <div
          v-if="template"
          class="session-prompt-config__editor"
      >
        <PromptTemplateEditor
            :key="template.get('id')"
            :model-value="template"
        />
      </div>

      <div
          v-else
          class="
          edit-box__state
          edit-box__state--vertical
          session-prompt-config__state
        "
      >
        <div class="edit-box__state-content">
          <strong class="edit-box__state-title">
            No prompt template
          </strong>

          <p class="edit-box__state-description">
            Select an existing template or create a new one for
            this session.
          </p>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.session-prompt-config {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);

  min-width: 0;
  padding: var(--space-3);
}

.session-prompt-config__toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-3);

  min-width: 0;
}

.session-prompt-config__toolbar-text {
  flex: 1 1 0;
  min-width: 0;
}

.session-prompt-config__title {
  display: block;

  color: rgb(var(--c-fg-strong));

  font-size: 0.82rem;
  font-weight: 800;
  line-height: 1.3;
}

.session-prompt-config__description {
  display: block;

  margin-top: var(--space-1);

  color: rgb(var(--c-muted));

  font-size: 0.72rem;
  line-height: 1.45;
}

.session-prompt-config__editor {
  min-width: 0;
}

.session-prompt-config__state {
  min-height: 7rem;
  padding: var(--space-4);
}

@media (max-width: 420px) {
  .session-prompt-config__toolbar {
    align-items: center;
  }
}
</style>