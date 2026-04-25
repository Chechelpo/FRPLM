<script setup lang="ts">
import { ref, watch, onBeforeUnmount } from "vue";

const EDIT_EMIT_MS = 250;

const props = defineProps<{
  read_only: boolean;
  modelValue?: boolean;
}>();

const emit = defineEmits<{
  (e: "update:modelValue", value: boolean): void;
  (e: "edit", payload: { value: boolean }): void;
}>();

let timer: number | null = null;

const value = ref<boolean>(props.modelValue ?? false);
const lastEmittedValue = ref<boolean>(value.value);

function scheduleEditEmit(): void {
  if (props.read_only) return;

  if (timer !== null) {
    window.clearTimeout(timer);
  }

  timer = window.setTimeout(() => {
    timer = null;

    if (value.value !== lastEmittedValue.value) {
      emit("edit", { value: value.value });
      lastEmittedValue.value = value.value;
    }
  }, EDIT_EMIT_MS);
}

function onToggle(e: Event): void {
  const checked = (e.target as HTMLInputElement).checked;
  value.value = checked;
  emit("update:modelValue", checked);
  scheduleEditEmit();
}

watch(
    () => props.modelValue,
    (newValue) => {
      value.value = newValue ?? false;
    }
);

onBeforeUnmount(() => {
  if (timer !== null) {
    window.clearTimeout(timer);
  }
});
</script>

<template>
  <div>
    <label>
      <input
          type="checkbox"
          class="sr-only peer"
          :checked="value"
          :disabled="read_only"
          @change="onToggle"
      />
    </label>
  </div>
</template>

<style scoped>
/* no additional styles */
</style>