<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref} from "vue";

/**
 * Debounce interval for emitting edit events (ms).
 */
const EDIT_EMIT_MS = 250;

const props = defineProps<{
  read_only?: boolean
  initial_value?: string;
}>();

const emit = defineEmits<{
  (e: "edit", payload: {
    value: string;
  }): void;
}>();

const text = ref(props.initial_value? props.initial_value : "");
const lastEmitted = ref(text.value);
const isExpanded = ref(false);

const inlineRef = ref<HTMLTextAreaElement | null>(null);
const expandedRef = ref<HTMLTextAreaElement | null>(null);

let timer: number | null = null;

function schedule_edit_emit(): void {
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
  <div class="w-full h-full flex flex-col">
    <div>
      <textarea
          ref="inlineRef"
          v-model="text as string"
          @input="schedule_edit_emit"
      />

      <button
          type="button"
          @click="openExpanded"
          title="Expand"
      >
        ⤢
      </button>
    </div>

  </div>
</template>

