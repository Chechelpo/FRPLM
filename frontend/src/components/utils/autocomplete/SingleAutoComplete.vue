<script setup lang="ts">
import { computed, nextTick, ref, watch } from "vue";

interface Props {
  suggestions: string[];
  modelValue?: string | null;
  placeholder?: string;
  allowCustom?: boolean;
  disabled?: boolean;
  clearable?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: null,
  placeholder: "Type to select…",
  allowCustom: false,
  disabled: false,
  clearable: true,
});

const emit = defineEmits<{
  (e: "update:modelValue", value: string | null): void;
  (e: "select", value: string): void;
  (e: "clear"): void;
}>();

const inputText = ref("");
const isFocused = ref(false);
const highlightedIndex = ref(0);
const inputRef = ref<HTMLInputElement | null>(null);

watch(
    () => props.modelValue,
    value => {
      if (value !== null && value !== undefined) {
        inputText.value = "";
      }
    }
);

const normalizedQuery = computed<string>(() =>
    inputText.value.trim().toLowerCase()
);

const filteredSuggestions = computed<string[]>(() => {
  const query = normalizedQuery.value;

  if (!query) {
    return props.suggestions.filter(value => value !== props.modelValue).slice(0, 8);
  }

  return props.suggestions
      .filter(value => value !== props.modelValue)
      .filter(value => value.toLowerCase().includes(query))
      .slice(0, 8);
});

const exactSuggestion = computed<string | null>(() => {
  const query = normalizedQuery.value;
  if (!query) return null;

  return (
      props.suggestions.find(value => value.toLowerCase() === query) ?? null
  );
});

const canApplyCustom = computed<boolean>(() => {
  const value = inputText.value.trim();

  return (
      props.allowCustom &&
      value.length > 0 &&
      exactSuggestion.value === null
  );
});

const menuOpen = computed<boolean>(() => {
  if (props.disabled) return false;
  if (!isFocused.value) return false;
  if (props.modelValue) return false;

  return filteredSuggestions.value.length > 0 || canApplyCustom.value;
});

function setValue(value: string): void {
  const normalized = value.trim();
  if (!normalized || props.disabled) return;

  emit("update:modelValue", normalized);
  emit("select", normalized);

  inputText.value = "";
  highlightedIndex.value = 0;

  nextTick(() => inputRef.value?.blur());
}

function clearValue(): void {
  if (props.disabled) return;

  emit("update:modelValue", null);
  emit("clear");

  inputText.value = "";
  highlightedIndex.value = 0;

  nextTick(() => inputRef.value?.focus());
}

function applyCurrent(): void {
  if (props.disabled) return;

  const suggestion = filteredSuggestions.value[highlightedIndex.value];

  if (suggestion !== undefined) {
    setValue(suggestion);
    return;
  }

  if (canApplyCustom.value) {
    setValue(inputText.value);
  }
}

function moveHighlight(delta: number): void {
  if (!menuOpen.value) return;

  const maxIndex = filteredSuggestions.value.length - 1;
  if (maxIndex < 0) return;

  highlightedIndex.value =
      (highlightedIndex.value + delta + filteredSuggestions.value.length) %
      filteredSuggestions.value.length;
}

function handleFocus(): void {
  isFocused.value = true;
}

function handleBlur(): void {
  window.setTimeout(() => {
    isFocused.value = false;
    highlightedIndex.value = 0;
  }, 120);
}

function handleInput(): void {
  highlightedIndex.value = 0;
}

function handleKeydown(event: KeyboardEvent): void {
  if (event.key === "ArrowDown") {
    event.preventDefault();
    moveHighlight(1);
    return;
  }

  if (event.key === "ArrowUp") {
    event.preventDefault();
    moveHighlight(-1);
    return;
  }

  if (event.key === "Enter" || event.key === "Tab") {
    if (menuOpen.value || canApplyCustom.value) {
      event.preventDefault();
      applyCurrent();
    }
    return;
  }

  if (event.key === "Escape") {
    isFocused.value = false;
    inputRef.value?.blur();
    return;
  }

  if (event.key === "Backspace" && props.modelValue && props.clearable) {
    event.preventDefault();
    clearValue();
  }
}
</script>

<template>
  <div class="single-autocomplete">
    <div
        class="single-autocomplete-box"
        :class="{
        'single-autocomplete-box--focused': isFocused,
        'single-autocomplete-box--disabled': disabled,
      }"
        @click="inputRef?.focus()"
    >
      <span
          v-if="modelValue"
          class="single-autocomplete-chip"
      >
        {{ modelValue }}

        <button
            v-if="clearable"
            type="button"
            class="single-autocomplete-chip-remove"
            aria-label="Clear selected value"
            :disabled="disabled"
            @click.stop="clearValue"
        >
          ×
        </button>
      </span>

      <input
          v-else
          ref="inputRef"
          v-model="inputText"
          type="text"
          class="single-autocomplete-input"
          :placeholder="placeholder"
          :disabled="disabled"
          autocomplete="off"
          @focus="handleFocus"
          @blur="handleBlur"
          @input="handleInput"
          @keydown="handleKeydown"
      />
    </div>

    <div
        v-if="menuOpen"
        class="single-autocomplete-menu"
    >
      <button
          v-for="(suggestion, index) in filteredSuggestions"
          :key="suggestion"
          type="button"
          class="single-autocomplete-option"
          :class="{ 'single-autocomplete-option--active': index === highlightedIndex }"
          @mousedown.prevent="setValue(suggestion)"
      >
        {{ suggestion }}
      </button>

      <button
          v-if="canApplyCustom"
          type="button"
          class="single-autocomplete-option single-autocomplete-option--custom"
          @mousedown.prevent="setValue(inputText)"
      >
        Create "{{ inputText.trim() }}"
      </button>
    </div>
  </div>
</template>

<style scoped>
.single-autocomplete {
  position: relative;
  width: 100%;
}

.single-autocomplete-box {
  min-height: 2.25rem;
  width: 100%;

  display: flex;
  align-items: center;
  gap: 0.35rem;

  padding: 0.25rem 0.45rem;

  border: 1px solid var(--primary-accent, #ffc600);
  border-radius: 0.5rem;

  background-color: color-mix(
      in srgb,
      var(--secondary-background, #b88f5a) 70%,
      transparent
  );

  color: var(--primary-text, #e2e8f0);
  cursor: text;
}

.single-autocomplete-box--focused {
  outline: 2px solid color-mix(
      in srgb,
      var(--primary-accent, #ffc600) 55%,
      transparent
  );
  outline-offset: 2px;
}

.single-autocomplete-box--disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.single-autocomplete-input {
  width: 100%;
  min-width: 0;

  border: none;
  outline: none;

  background: transparent;
  color: inherit;

  font: inherit;
}

.single-autocomplete-input::placeholder {
  color: color-mix(in srgb, currentColor 55%, transparent);
}

.single-autocomplete-chip {
  max-width: 100%;

  display: inline-flex;
  align-items: center;
  gap: 0.35rem;

  padding: 0.15rem 0.45rem;

  border: 1px solid var(--primary-accent, #ffc600);
  border-radius: 999px;

  background-color: var(--primary-background, #dfae7c);
  color: #1c1917;

  font-size: 0.9rem;
  line-height: 1.4;
}

.single-autocomplete-chip-remove {
  width: 1.1rem;
  height: 1.1rem;

  display: inline-flex;
  align-items: center;
  justify-content: center;

  border: none;
  border-radius: 999px;

  background: transparent;
  color: inherit;

  font-size: 1rem;
  line-height: 1;

  cursor: pointer;
}

.single-autocomplete-chip-remove:hover {
  background-color: color-mix(in srgb, var(--primary-accent, #ffc600) 45%, transparent);
}

.single-autocomplete-menu {
  position: absolute;
  z-index: 50;

  top: calc(100% + 0.25rem);
  left: 0;
  right: 0;

  max-height: 14rem;
  overflow-y: auto;

  padding: 0.25rem;

  border: 1px solid var(--primary-accent, #ffc600);
  border-radius: 0.5rem;

  background: var(--secondary-background, #b88f5a);
  box-shadow:
      0 20px 25px rgb(0 0 0 / 0.12),
      0 8px 10px rgb(0 0 0 / 0.10);
}

.single-autocomplete-option {
  width: 100%;

  display: block;

  padding: 0.45rem 0.55rem;

  border: none;
  border-radius: 0.375rem;

  background: transparent;
  color: inherit;

  text-align: left;
  font: inherit;

  cursor: pointer;
}

.single-autocomplete-option:hover,
.single-autocomplete-option--active {
  background-color: var(--primary-accent, #ffc600);
  color: #1c1917;
}

.single-autocomplete-option--custom {
  font-style: italic;
}
</style>