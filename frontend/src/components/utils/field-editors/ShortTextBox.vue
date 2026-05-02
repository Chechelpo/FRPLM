<script setup lang="ts">
import { ref, watch, onBeforeUnmount } from "vue";

/**
 * Debounce interval for emitting edit events (ms).
 */
const EDIT_EMIT_MS = 250;

// ---- model / emit -------------------------------------------------------
const model = defineModel<string | null>({
  required: true,
});

const emit = defineEmits<{
  (e: "edit", payload: string): void;
}>();

// ---- state -------------------------------------------------------
const text = ref<string>(model.value ?? "");
const lastEmitted = ref<string>(text.value);

let timer: number | null = null;

function scheduleEditEmit(): void {
  model.value = text.value;

  if (timer !== null) {
    window.clearTimeout(timer);
  }

  timer = window.setTimeout(() => {
    timer = null;

    if (text.value !== lastEmitted.value) {
      emit("edit", text.value);
      lastEmitted.value = text.value;
    }
  }, EDIT_EMIT_MS);
}

watch(
    () => model.value,
    (value) => {
      const normalized = value ?? "";

      if (normalized !== text.value) {
        text.value = normalized;
        lastEmitted.value = normalized;
      }
    },
);

onBeforeUnmount(() => {
  if (timer !== null) {
    window.clearTimeout(timer);
    timer = null;
  }
});
</script>

<template>
  <input
      v-model="text"
      type="text"
      class="noBackground"
      @input="scheduleEditEmit"
  />
</template>

<style scoped>
.noBackground {
  background: transparent;
  color: inherit;
  font-size: 16px;
  border: none;
  outline: none;
}
</style>