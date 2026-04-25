<script setup lang="ts" >
import { ref, watch, onBeforeUnmount } from "vue";

/**
 * Debounce interval for emitting edit events (ms).
 */
const EDIT_EMIT_MS = 250;

const props = defineProps<{
  read_only: boolean,
  initial_value?: string
}>();

const emit = defineEmits<{
  (e: "edit", payload: {value: string }): void;
}>();

const text = ref(props.initial_value ?? "");
const lastEmitted = ref(text.value);

let timer: number | null = null;

function scheduleEditEmit(): void {
  if (timer !== null) window.clearTimeout(timer);

  timer = window.setTimeout(() => {
    timer = null;

    if (text.value !== lastEmitted.value) {
      emit("edit", {
        value: text.value,
      });
      lastEmitted.value = text.value;
    }
  }, EDIT_EMIT_MS);
}

watch(
    () => props.initial_value,
    (v) => {
      if (v !== undefined && v !== text.value) {
        text.value = v;
        lastEmitted.value = v;
      }
    }
);

onBeforeUnmount(() => {
  if (timer !== null) window.clearTimeout(timer);
});
</script>


<template>
  <input
      v-model="text"
      type="text"
      class=""
  />
</template>


<style scoped>

</style>