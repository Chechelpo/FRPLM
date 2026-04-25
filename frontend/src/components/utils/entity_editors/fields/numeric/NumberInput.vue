<script setup lang="ts">
import {onBeforeUnmount, ref} from "vue";
/**
 * Debounce interval for emitting edit events (ms).
 */
const EDIT_EMIT_MS = 250;

const props = defineProps<{
  read_only?:boolean
  initial_value:number
}>();

const lastEmitted = ref(props.initial_value);
const value = ref(lastEmitted.value);
let timer: number | null = null;

const emit = defineEmits<{
  (e: "edit", payload: {
    value: number;
  }): void;
}>();

function scheduleEditEmit(): void {
  if (props.read_only) return;

  if (timer !== null) window.clearTimeout(timer);

  timer = window.setTimeout(() => {
    timer = null;

    if (value.value !== lastEmitted.value) {
      emit("edit", {
        value: value.value,
      });
      lastEmitted.value = value.value;
    }
  }, EDIT_EMIT_MS);
}

function onInput(e: Event): void {
  const raw = (e.target as HTMLInputElement).value;
  const next = raw === "" ? 0 : Number(raw);

  value.value = next;
  emit("edit", {
    value:next
  });

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
        :disabled="props.read_only"
        @input="onInput"
        class=""
    />
  </div>
</template>