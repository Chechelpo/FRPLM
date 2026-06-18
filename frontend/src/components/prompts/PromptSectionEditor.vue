<script setup lang="ts">
import ShortTextBox from "@/components/utils/primitiveEditors/ShortTextBox.vue";
import BooleanToggle from "@/components/utils/primitiveEditors/BooleanToggle.vue";
import {PromptSection} from "@/domain/Prompts";
import {computed, ref} from "vue";
import LongTextBox from "@/components/utils/primitiveEditors/LongTextBox.vue";
import FieldEditorWrapper from "@/components/utils/FieldEditorWrapper.vue";
import SingleEnumInput from "@/components/utils/primitiveEditors/SingleEnumInput.vue";
import {ChatCompletionRole} from "@/types/ChatCompletions";

const props = defineProps<{
  section: PromptSection;
  index: number;
  canMoveUp: boolean;
  canMoveDown: boolean;
}>();

const emit = defineEmits<{
  (e: "move-up", section: PromptSection): void;
  (e: "move-down", section: PromptSection): void;
}>();

const edit = ref(false);

const name = computed<string>({
  get() {
    return props.section.get("name");
  },
  set(value: string) {
    props.section.update("name", value);
  },
});

const content = computed<string>({
  get() {
    return props.section.get("content");
  },
  set(value: string) {
    props.section.update("content", value);
  },
});

const role = computed<ChatCompletionRole>({
  get() {
    return props.section.get("role");
  },
  set(value: ChatCompletionRole) {
    props.section.update("role", value);
  },
});

const active = computed<boolean>({
  get() {
    return props.section.get('active');
  },
  set(value: boolean) {
    props.section.update('active', value);
  },
});

const contentPreview = computed<string>(() => {
  const normalized = content.value.trim().replace(/\s+/g, " ");
  if (normalized.length === 0) return "Empty section";
  if (normalized.length <= 140) return normalized;
  return `${normalized.slice(0, 140)}…`;
});

const roles = [ChatCompletionRole.USER, ChatCompletionRole.ASSISTANT, ChatCompletionRole.SYSTEM] satisfies ChatCompletionRole[];

function openEditor(): void {
  edit.value = true;
}

function closeEditor(): void {
  edit.value = false;
}
</script>

<template>
  <div>
    <article
        class="section-card"
        :class="{ 'section-card--inactive': !active }"
    >
      <div class="section-card-order">
        <button
            type="button"
            class="section-card-arrow"
            :disabled="!canMoveUp"
            aria-label="Move section up"
            @click.stop="emit('move-up', section)"
        >
          ▲
        </button>

        <button
            type="button"
            class="section-card-arrow"
            :disabled="!canMoveDown"
            aria-label="Move section down"
            @click.stop="emit('move-down', section)"
        >
          ▼
        </button>
      </div>

      <button
          type="button"
          class="section-card-main"
          @click="openEditor"
      >
        <div class="section-card-meta">
        <span class="section-card-index">
          #{{ index + 1 }}
        </span>

          <span
              class="section-card-role"
              :data-role="role"
          >
          {{ role }}
        </span>

          <span
              class="section-card-state"
              :class="{ 'section-card-state--inactive': !active }"
          >
          {{ active ? "active" : "inactive" }}
        </span>
        </div>

        <div class="section-card-name">
          {{ name }}
        </div>

        <p class="section-card-preview">
          {{ contentPreview }}
        </p>
      </button>

      <div class="section-card-actions">
        <BooleanToggle
            :model-value="active"
            @edit="value => active = value"
        />

        <button
            type="button"
            class="section-card-edit"
            @click.stop="openEditor"
        >
          Edit
        </button>
      </div>
    </article>

    <Teleport to="body">
      <div
          v-if="edit"
          class="section-editor-backdrop"
          @click.self="closeEditor"
      >
        <section
            class="section-editor-window"
            role="dialog"
            aria-modal="true"
        >
          <header class="section-editor-header">
            <div class="section-editor-title">
              Edit section
            </div>

            <button
                type="button"
                class="section-editor-close"
                aria-label="Close"
                @click="closeEditor"
            >
              ×
            </button>
          </header>

          <main class="section-editor-body">
            <FieldEditorWrapper field-name="name">
              <ShortTextBox
                  :model-value="name"
                  @edit="value => name = value"
              />
            </FieldEditorWrapper>

            <FieldEditorWrapper field-name="active">
              <BooleanToggle
                  :model-value="active"
                  @edit="value => active = value"
              />
            </FieldEditorWrapper>

            <FieldEditorWrapper field-name="role">
              <SingleEnumInput
                  :value="role"
                  :possible_values="roles"
                  @edit="value => role = value"
              />
            </FieldEditorWrapper>

            <FieldEditorWrapper field-name="content">
              <LongTextBox
                  :model-value="content"
                  @edit="value => content = value"
              />
            </FieldEditorWrapper>
          </main>
        </section>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.section-card {
  width: 100%;
  min-height: 76px;

  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.75rem;

  padding: 0.65rem 0.8rem;

  border: 1px solid var(--primary-accent);
  border-radius: 0.75rem;

  background: color-mix(
      in srgb,
      var(--primary-background, #dfae7c) 82%,
      black 18%
  );

  color: var(--primary-text, #111827);

  box-shadow: 0 1px 2px rgb(0 0 0 / 0.14),
  inset 0 1px 0 rgb(255 255 255 / 0.12);

  transition: border-color 140ms ease,
  background 140ms ease,
  opacity 140ms ease,
  transform 140ms ease;
}

.section-card:hover {
  border-color: var(--primary-accent);
  background: var(--secondary-background, #b88f5a);
  transform: translateY(-1px);
}

.section-card--inactive {
  opacity: 0.55;
}

.section-card-order {
  display: inline-flex;
  flex-direction: column;
  gap: 0.25rem;
}

.section-card-arrow,
.section-card-edit {
  border: 1px solid rgb(0 0 0 / 0.18);
  border-radius: 0.45rem;

  background: rgb(255 255 255 / 0.16);
  color: inherit;

  cursor: pointer;
}

.section-card-arrow {
  width: 1.75rem;
  height: 1.45rem;

  display: inline-flex;
  align-items: center;
  justify-content: center;

  font-size: 0.7rem;
  line-height: 1;
}

.section-card-arrow:disabled {
  opacity: 0.28;
  cursor: not-allowed;
}

.section-card-main {
  min-width: 0;

  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 0.25rem;

  padding: 0;

  border: none;
  background: transparent;
  color: inherit;

  text-align: left;
  cursor: pointer;
}

.section-card-meta {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  min-width: 0;
}

.section-card-index,
.section-card-role,
.section-card-state {
  display: inline-flex;
  align-items: center;

  min-height: 1.35rem;
  padding: 0.1rem 0.45rem;

  border-radius: 999px;

  font-size: 0.72rem;
  font-weight: 700;
  line-height: 1;
}

.section-card-index {
  background: rgb(0 0 0 / 0.16);
}

.section-card-role {
  background: rgb(255 255 255 / 0.18);
  text-transform: uppercase;
}

.section-card-role[data-role="system"] {
  border: 1px solid rgb(127 29 29 / 0.45);
}

.section-card-role[data-role="assistant"] {
  border: 1px solid rgb(30 64 175 / 0.45);
}

.section-card-role[data-role="user"] {
  border: 1px solid rgb(21 128 61 / 0.45);
}

.section-card-state {
  background: rgb(22 101 52 / 0.18);
}

.section-card-state--inactive {
  background: rgb(127 29 29 / 0.16);
}

.section-card-name {
  overflow: hidden;

  font-size: 1rem;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.section-card-preview {
  margin: 0;

  overflow: hidden;

  color: rgb(17 24 39 / 0.72);

  font-size: 0.82rem;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.section-card-actions {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
}

.section-card-edit {
  min-height: 2rem;
  padding: 0 0.7rem;

  font-size: 0.85rem;
  font-weight: 700;
}

.section-card-edit:hover,
.section-card-arrow:not(:disabled):hover {
  background: rgb(255 255 255 / 0.28);
}

/* Existing modal CSS can remain as-is. */
.section-editor-backdrop {
  position: fixed;
  inset: 0;
  z-index: 9999;

  display: flex;
  align-items: center;
  justify-content: center;

  padding: 1rem;
  background: rgb(0 0 0 / 0.45);
}

.section-editor-window {
  width: min(500px, 100%);
  max-height: min(90vh, 800px);

  display: flex;
  flex-direction: column;

  background: var(--primary-background, white);
  color: #111827;

  border-radius: 0.75rem;
  box-shadow: 0 20px 25px rgb(0 0 0 / 0.1),
  0 8px 10px rgb(0 0 0 / 0.1);

  overflow: hidden;
}

.section-editor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;

  padding: 1rem;
  border-bottom: 1px solid #e5e7eb;
}

.section-editor-title {
  font-size: 1rem;
  font-weight: 600;
  line-height: 1.4;
}

.section-editor-close {
  width: 2rem;
  height: 2rem;

  display: inline-flex;
  align-items: center;
  justify-content: center;

  border: none;
  border-radius: 0.375rem;

  background: transparent;
  color: #374151;

  font-size: 1.5rem;
  line-height: 1;

  cursor: pointer;
}

.section-editor-close:hover {
  background: #f3f4f6;
}

.section-editor-body {
  display: flex;
  flex-direction: column;
  gap: 1rem;

  padding: 1rem;
  overflow: auto;
}
</style>
