<!-- PlainSlider.vue -->
<template>
  <div class="slider">
    <input
        class="slider__input"
        type="range"
        :min="min"
        :max="max"
        :step="step"
        :value="modelValue"
        @input="onInput"
    />

    <span class="slider__value">
      {{ modelValue }}
    </span>
  </div>
</template>

<script setup lang="ts">
const model = defineModel<number>({required:true, type:Number})
const props = withDefaults(
    defineProps<{
      min?: number
      max?: number
      step?: number
    }>(),
    {
      min: 0,
      max: 100,
      step: 1,
    }
)

const emit = defineEmits<{
  (e: "edit", payload: number): void;
}>();
function onInput(event: Event): void {
  const input = event.target as HTMLInputElement
  emit('edit', Number(input.value))
}
</script>

<style scoped>
.slider {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  width: 100%;
}

.slider__input {
  width: 100%;
  appearance: none;
  height: 0.4rem;
  border-radius: 999px;
  background: #d1d5db;
  outline: none;
  cursor: pointer;
}

/* Chrome, Edge, Safari */
.slider__input::-webkit-slider-thumb {
  appearance: none;
  width: 1rem;
  height: 1rem;
  border-radius: 50%;
  background: #111827;
  cursor: pointer;
}

/* Firefox */
.slider__input::-moz-range-thumb {
  width: 1rem;
  height: 1rem;
  border: none;
  border-radius: 50%;
  background: #111827;
  cursor: pointer;
}

.slider__value {
  min-width: 2.5rem;
  font-variant-numeric: tabular-nums;
  text-align: right;
}
</style>