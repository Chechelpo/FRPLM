<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref } from "vue";

/**
 * Debounce interval for emitting edit events (ms).
 */
const EDIT_EMIT_MS = 250;

const model = defineModel<string | null>({ required: true });

const emit = defineEmits<{
  (e: "edit", payload: string): void;
}>();

const text = ref<string>(model.value != null ? model.value : "");
const lastEmitted = ref(text.value);
const isExpanded = ref(false);

const inlineRef = ref<HTMLTextAreaElement | null>(null);
const expandedRef = ref<HTMLTextAreaElement | null>(null);

let timer: number | null = null;

function schedule_edit_emit(): void {
  model.value = text.value;

  if (timer !== null) window.clearTimeout(timer);

  timer = window.setTimeout(() => {
    timer = null;

    if (text.value !== lastEmitted.value) {
      emit("edit", text.value);
      lastEmitted.value = text.value;
    }
  }, EDIT_EMIT_MS);
}

function openExpanded(): void {
  isExpanded.value = true;
  void nextTick(() => expandedRef.value?.focus());
}

function closeExpanded(): void {
  isExpanded.value = false;
  void nextTick(() => inlineRef.value?.focus());
}

onBeforeUnmount(() => {
  if (timer !== null) window.clearTimeout(timer);
});
</script>

<template>
  <div>
    <div class="inlineEditor">
      <textarea
          ref="inlineRef"
          v-model="text"
          class="textBox"
          @input="schedule_edit_emit"
      />

      <button
          type="button"
          title="Expand"
          @click="openExpanded"
      >
        ⤢
      </button>
    </div>

    <Teleport to="body">
      <div
          v-if="isExpanded"
          class="modalBackdrop"
          @click.self="closeExpanded"
      >
        <div class="modalWindow">
          <div class="modalToolbar">
            <button
                type="button"
                title="Close"
                @click="closeExpanded"
            >
              ✕
            </button>
          </div>

          <textarea
              ref="expandedRef"
              v-model="text"
              class="expandedTextBox"
              @input="schedule_edit_emit"
              @keydown.esc="closeExpanded"
          />
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.inlineEditor {
  display: flex;
  width: 100%;
}

.textBox {
  background-color: sandybrown;
  width: 100%;
  height: 100%;
}

.modalBackdrop {
  position: fixed;
  inset: 0;
  z-index: 9999;

  display: flex;
  align-items: center;
  justify-content: center;

  background: rgba(0, 0, 0, 0.45);
}

.modalWindow {
  width: min(90vw, 1200px);
  height: min(85vh, 900px);

  display: flex;
  flex-direction: column;

  background: var(--primary-background, white);
  border: 1px solid #777;
  border-radius: 8px;
  padding: 12px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.35);
}

.modalToolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 8px;
}

.expandedTextBox {
  flex: 1;
  width: 100%;
  min-height: 0;

  resize: none;
  background-color: sandybrown;
}
</style>