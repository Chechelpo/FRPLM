<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from "vue";

const EDIT_EMIT_MS = 250;

const model = defineModel<number | null>({
  required: true,
});

const props = withDefaults(
    defineProps<{
      nullable?: boolean;
      placeholder?: string;
      min?: number;
      max?: number;
      step?: number;
      disabled?: boolean;
    }>(),
    {
      nullable: false,
      placeholder: "",
      step: 1,
      disabled: false,
    },
);

const emit = defineEmits<{
  (e: "edit", payload: number | null): void;
}>();

const value = ref<number | null>(model.value);
const lastEmitted = ref<number | null>(model.value);

let timer: ReturnType<typeof setTimeout> | null = null;

function scheduleEditEmit(): void {
  if (timer !== null) {
    clearTimeout(timer);
  }

  timer = setTimeout(() => {
    timer = null;

    if (value.value !== lastEmitted.value) {
      emit("edit", value.value);
      lastEmitted.value = value.value;
    }
  }, EDIT_EMIT_MS);
}

function onInput(event: Event): void {
  const input = event.target as HTMLInputElement;
  const raw = input.value.trim();

  let next: number | null;

  if (raw === "") {
    next = props.nullable ? null : 0;
  } else {
    const parsed = Number(raw);

    if (!Number.isFinite(parsed)) {
      return;
    }

    next = parsed;
  }

  value.value = next;
  model.value = next;

  scheduleEditEmit();
}

watch(
    () => model.value,
    newValue => {
      if (newValue !== value.value) {
        value.value = newValue;
        lastEmitted.value = newValue;
      }
    },
);

onBeforeUnmount(() => {
  if (timer !== null) {
    clearTimeout(timer);
  }
});
</script>

<template>
  <div class="number-input">
    <input
        class="number-input__control"
        type="number"
        inputmode="decimal"
        :value="value ?? ''"
        :placeholder="placeholder"
        :min="min"
        :max="max"
        :step="step"
        :disabled="disabled"
        @input="onInput"
    />
  </div>
</template>

<style scoped>
.number-input {
  width: 100%;
  min-width: 0;
}

.number-input__control {
  display: block;

  width: 100%;
  min-width: 0;
  max-width: 100%;
  height: 2.25rem;

  box-sizing: border-box;
  padding: 0.4rem 0.65rem;

  border: 1px solid var(--primary-accent, #ffc600);
  border-radius: 0.4rem;

  background: var(--primary-background, transparent);
  color: inherit;

  font: inherit;
  line-height: 1.2;

  outline: none;

  /* Firefox: remove number arrows */
  appearance: textfield;
  -moz-appearance: textfield;
}

.number-input__control:focus {
  border-color: var(--primary-accent, #ffc600);
  box-shadow: 0 0 0 2px
  color-mix(
      in srgb,
      var(--primary-accent, #ffc600) 35%,
      transparent
  );
}

.number-input__control:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

/* Chromium, Safari, Edge: remove number arrows */
.number-input__control::-webkit-inner-spin-button,
.number-input__control::-webkit-outer-spin-button {
  margin: 0;
  appearance: none;
  -webkit-appearance: none;
}
</style>