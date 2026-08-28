<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from "vue";

/**
 * Debounce interval for emitting edit events in milliseconds.
 */
const EDIT_EMIT_MS = 250;

const model = defineModel<string | null>({
  required: true,
});

const props = withDefaults(
    defineProps<{
      disabled?: boolean;
    }>(),
    {
      disabled: false,
    },
);

const emit = defineEmits<{
  (event: "edit", payload: string): void;
}>();

const text = ref(model.value ?? "");
const lastEmitted = ref(text.value);

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
      const normalizedValue = value ?? "";

      if (normalizedValue !== text.value) {
        text.value = normalizedValue;
        lastEmitted.value = normalizedValue;
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
      class="short-text-box"
      type="text"
      :disabled="props.disabled"
      @input="scheduleEditEmit"
  />
</template>

<style scoped>
.short-text-box {
  width: 100%;
  min-width: 0;
  min-height: 2.5rem;
  box-sizing: border-box;

  padding:
      var(--space-2)
      var(--space-3);

  color: rgb(var(--c-fg));
  caret-color: rgb(var(--c-accent-2));

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-surface-raised) / 0.58),
          rgb(var(--c-surface-2) / 0.36)
      );

  border: 1px solid rgb(var(--c-border) / 0.32);
  border-radius: var(--radius-md);
  outline: 0;

  font-family: var(--font-primary);
  font-size: 1rem;
  font-weight: 450;
  line-height: 1.4;

  box-shadow:
      inset 0 1px 0 rgb(255 255 255 / 0.34),
      0 3px 10px rgb(var(--c-shadow) / 0.045);

  transition:
      background-color var(--duration-normal) var(--ease-standard),
      border-color var(--duration-normal) var(--ease-standard),
      box-shadow var(--duration-normal) var(--ease-standard);
}

.short-text-box::placeholder {
  color: rgb(var(--c-muted) / 0.72);
}

.short-text-box:hover:not(:disabled) {
  border-color: rgb(var(--c-primary) / 0.42);
}

.short-text-box:focus {
  border-color: rgb(var(--c-accent) / 0.72);

  box-shadow:
      0 0 0 var(--focus-ring-width)
      rgb(var(--focus-ring-color) / 0.15),
      inset 0 1px 0 rgb(255 255 255 / 0.38),
      0 5px 14px rgb(var(--c-shadow) / 0.065);
}

.short-text-box:disabled {
  color: rgb(var(--c-muted));

  background: rgb(var(--c-surface-2) / 0.32);
  border-color: rgb(var(--c-border) / 0.2);

  opacity: 0.62;
  cursor: not-allowed;
}

.short-text-box::selection {
  color: rgb(var(--c-on-accent));
  background: rgb(var(--c-accent) / 0.46);
}

@media (prefers-reduced-motion: reduce) {
  .short-text-box {
    transition: none;
  }
}
</style>