<script setup lang="ts" generic="T extends string | number | symbol">
import { onMounted, onUnmounted } from "vue";
import SingleEnumInput from "@/components/utils/field-editors/SingleEnumInput.vue";

const props = defineProps<{
  message: string;
  options: T[];
  labels?: Record<T, string>;
}>();

const emit = defineEmits<{
  (e: "select", option: T): void;
  (e: "close"): void;
}>();

function close() {
  emit("close");
}

function select(option: T) {
  emit("select", option);
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === "Escape") {
    close();
  }
}

onMounted(() => {
  window.addEventListener("keydown", onKeydown);
});

onUnmounted(() => {
  window.removeEventListener("keydown", onKeydown);
});
</script>

<template>
  <Teleport to="body">
    <div
        class="prompt-window-backdrop"
        @click.self="close"
    >
      <section
          class="prompt-window"
          role="dialog"
          aria-modal="true"
      >
        <header class="prompt-window-header">
          <div class="prompt-window-message">
            {{ message }}
          </div>

          <button
              type="button"
              class="prompt-window-close"
              aria-label="Close"
              @click="close"
          >
            ×
          </button>
        </header>

        <main class="prompt-window-body">
          <SingleEnumInput
              :value="null"
              :possible_values="options"
              :labels="labels"
              @edit="select"
          />
        </main>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.prompt-window-backdrop {
  position: fixed;
  inset: 0;
  z-index: 9999;

  display: flex;
  align-items: center;
  justify-content: center;

  padding: 1rem;
  background: rgb(0 0 0 / 0.45);
}

.prompt-window {
  width: min(420px, 100%);
  max-height: min(80vh, 640px);

  display: flex;
  flex-direction: column;

  background: var(--primary-background);
  color: #111827;

  border-radius: 0.75rem;
  box-shadow:
      0 20px 25px rgb(0 0 0 / 0.1),
      0 8px 10px rgb(0 0 0 / 0.1);

  overflow: hidden;
}

.prompt-window-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;

  padding: 1rem;
  border-bottom: 1px solid #e5e7eb;
}

.prompt-window-message {
  font-size: 1rem;
  font-weight: 600;
  line-height: 1.4;
}

.prompt-window-close {
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

.prompt-window-close:hover {
  background: #f3f4f6;
}

.prompt-window-body {
  padding: 1rem;
  overflow: auto;
}
</style>