<script setup lang="ts" generic="T extends string | number | symbol">
import { computed, ref, watch } from "vue";

type LabelSource<T extends string | number | symbol> =
    | ReadonlyMap<T, string>
    | Partial<Record<T, string>>;

const props = defineProps<{
  value: T | null;
  possible_values: readonly T[];
  labels?: LabelSource<T>;
  placeholder?: string;
}>();

const emit = defineEmits<{
  edit: [value: T];
}>();

const open = ref(false);
const search = ref("");

function isMap(
    labels: LabelSource<T>,
): labels is ReadonlyMap<T, string> {
  return labels instanceof Map;
}

function labelOf(value: T): string {
  if (!props.labels) return String(value);

  if (isMap(props.labels)) {
    return props.labels.get(value) ?? String(value);
  }

  return props.labels[value] ?? String(value);
}

function normalize(value: string): string {
  return value
      .normalize("NFD")
      .replace(/\p{Diacritic}/gu, "")
      .toLowerCase()
      .trim();
}

const selectedLabel = computed(() =>
    props.value === null ? "" : labelOf(props.value),
);

const filteredValues = computed(() => {
  const terms = normalize(search.value)
      .split(/\s+/)
      .filter(Boolean);

  if (terms.length === 0) {
    return props.possible_values;
  }

  return props.possible_values.filter(value => {
    const text = normalize(
        `${labelOf(value)} ${String(value)}`,
    );

    return terms.every(term => text.includes(term));
  });
});

watch(
    selectedLabel,
    value => {
      if (!open.value) search.value = value;
    },
    { immediate: true },
);

function showOptions(): void {
  open.value = true;
  search.value = "";
}

function closeOptions(): void {
  open.value = false;
  search.value = selectedLabel.value;
}

function selectValue(value: T): void {
  emit("edit", value);
  open.value = false;
  search.value = labelOf(value);
}
</script>

<template>
  <div
      class="enumInput"
      @focusout="
      event => {
        const next = event.relatedTarget as Node | null;

        if (!next || !(event.currentTarget as HTMLElement).contains(next)) {
          closeOptions();
        }
      }
    "
  >
    <input
        v-model="search"
        type="text"
        autocomplete="off"
        :placeholder="placeholder ?? 'Select…'"
        @focus="showOptions"
        @keydown.esc="closeOptions"
    />

    <div
        v-if="open"
        class="options"
    >
      <button
          v-for="option in filteredValues"
          :key="String(option)"
          type="button"
          :class="{ selected: Object.is(option, value) }"
          @mousedown.prevent
          @click="selectValue(option)"
      >
        {{ labelOf(option) }}
      </button>

      <div
          v-if="filteredValues.length === 0"
          class="empty"
      >
        No matches
      </div>
    </div>
  </div>
</template>

<style scoped>
.enumInput {
  position: relative;
  width: 100%;
  font-family: var(--primary-text, system-ui, sans-serif);
}

input {
  width: 100%;
  box-sizing: border-box;
  padding: 7px 10px;

  font: inherit;
  font-size: 0.95rem;

  color:  #2f2418;
  background: rgba(184, 143, 90, 0.5);

  border: 1px solid var(--primary, #af8218);
  border-radius: 6px;
  outline: none;
}

input:focus {
  border-color: var(--primary-accent, #ffc600);
  box-shadow: 0 0 0 2px rgba(255, 198, 0, 0.2);
}

.options {
  position: absolute;
  z-index: 1000;
  top: calc(100% + 4px);

  width: 100%;
  max-height: 200px;
  overflow-y: auto;

  padding: 4px;
  box-sizing: border-box;

  background: var(--primary-background, #dfae7c);
  border: 1px solid var(--primary, #af8218);
  border-radius: 6px;

  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2);
}

.options button {
  display: block;
  width: 100%;
  padding: 2px 3px;

  font: inherit;
  text-align: left;

  color: var(--primary-text-color, #2f2418);
  background: transparent;

  border: 0;
  border-radius: 4px;
  cursor: pointer;
}

.options button:hover,
.options button.selected {
  background: rgba(255, 198, 0, 0.25);
}

.options button.selected {
  font-weight: 600;
}

.empty {
  padding: 8px;
  font-size: 0.875rem;
  color: var(--muted-text-color, #6b5138);
}
</style>