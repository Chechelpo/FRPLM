<script setup lang="ts" generic="T extends string | number | symbol">
import { computed } from "vue";

const props = defineProps<{
  value: T | null;
  possible_values: T[];
  labels?: Record<T, string>;
}>();

const emit = defineEmits<{
  (e: "edit", value: T): void;
}>();

const selectedIndex = computed(() => {
  if (props.value === null) return "";

  const index = props.possible_values.findIndex(
      value => Object.is(value, props.value)
  );

  return index === -1 ? "" : String(index);
});

function labelOf(value: T): string {
  return props.labels?.[value] ?? String(value);
}

function handleChange(event: Event): void {
  const target = event.target as HTMLSelectElement;
  const index = Number(target.value);
  const selected = props.possible_values[index];

  if (selected !== undefined) {
    emit("edit", selected);
  }
}
</script>

<template>
  <select
      :value="selectedIndex"
      @change="handleChange"
  >
    <option
        v-for="(option, index) in possible_values"
        :key="option"
        :value="index"
    >
      {{ labelOf(option) }}
    </option>
  </select>
</template>