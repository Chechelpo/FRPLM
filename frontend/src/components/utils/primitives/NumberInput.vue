<script setup lang="ts">
import {onBeforeUnmount, ref} from "vue";
/**
 * Debounce interval for emitting edit events (ms).
 */
const EDIT_EMIT_MS = 250;

const model = defineModel<number>({required:true});

const lastEmitted = ref(model.value);
const value = ref(lastEmitted.value);
let timer: number | null = null;

const emit = defineEmits<{
  (e: "edit", payload: number): void;
}>();

function scheduleEditEmit(): void {
  if (timer !== null) window.clearTimeout(timer);

  timer = window.setTimeout(() => {
    timer = null;

    if (value.value !== lastEmitted.value) {
      emit("edit", value.value);
      lastEmitted.value = value.value;
    }
  }, EDIT_EMIT_MS);
}

function onInput(e: Event): void {
  const raw = (e.target as HTMLInputElement).value;
  const next = raw === "" ? 0 : Number(raw);

  value.value = next;
  emit("edit", next);

  scheduleEditEmit();
}

onBeforeUnmount(() => {
  if (timer !== null) window.clearTimeout(timer);
});
</script>

<template>
  <div class="">
    <!-- input -->
    <input
        type="number"
        :value="value ?? ''"
        @input="onInput"
        class=""
    />
  </div>
</template>