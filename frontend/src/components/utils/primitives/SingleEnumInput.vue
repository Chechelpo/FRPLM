<script setup lang="ts" generic="T extends string | number | symbol">
import { computed } from "vue"

type LabelSource<T extends string | number | symbol> =
    | ReadonlyMap<T, string>
    | Partial<Record<T, string>>

const props = defineProps<{
  value: T | null
  possible_values: readonly T[]
  labels?: LabelSource<T>
}>()

const emit = defineEmits<{
  edit: [value: T]
}>()

const selectedIndex = computed<string>(() => {
  if (props.value === null) return ""

  const index = props.possible_values.findIndex(value =>
      Object.is(value, props.value)
  )

  return index === -1 ? "" : String(index)
})

function isMapLabels(
    labels: LabelSource<T>
): labels is ReadonlyMap<T, string> {
  return typeof (labels as ReadonlyMap<T, string>).get === "function"
}

function labelOf(value: T): string {
  const labels = props.labels

  if (!labels) {
    return String(value)
  }

  if (isMapLabels(labels)) {
    return labels.get(value) ?? String(value)
  }

  return labels[value] ?? String(value)
}

function handleChange(event: Event): void {
  const target = event.currentTarget as HTMLSelectElement
  const index = Number(target.value)
  const selected = props.possible_values[index]

  if (selected !== undefined) {
    emit("edit", selected)
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
        :key="String(option)"
        :value="index"
    >
      {{ labelOf(option) }}
    </option>
  </select>
</template>