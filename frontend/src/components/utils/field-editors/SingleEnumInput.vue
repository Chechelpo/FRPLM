<script setup lang="ts" generic = "T extends string|number|symbol">
const props = defineProps<{
  value: T;
  possible_values: T[];
  labels?: Record<T, string>;
}>();

const emit = defineEmits<{
  (e: "edit", value: number): void;
}>();

function labelOf(value: T): string {
  return props.labels?.[value] ?? String(value);
}

function handleChange(event: Event): void {
  const target = event.target as HTMLSelectElement;
  emit("edit", Number(target.value));
}
</script>

<template>
  <select
      :value="value"
      @change="handleChange"
  >
    <option
        v-for="option in possible_values"
        :key="option"
        :value="option"
    >
      {{ labelOf(option) }}
    </option>
  </select>
</template>

<style scoped>
select {
  background: transparent;
  color: inherit;
  font: inherit;
}
</style>