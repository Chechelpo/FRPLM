<script setup lang="ts">
import {
  computed,
  ref,
  watch,
  type CSSProperties,
} from "vue";

const model = defineModel<number>({
  required: true,
  type: Number,
});

const props = withDefaults(
    defineProps<{
      min?: number;
      max?: number;
      step?: number;
      read_only?: boolean;
      label?: string;
    }>(),
    {
      min: 0,
      max: 100,
      step: 1,
      read_only: false,
      label: "Numeric value",
    },
);

const emit = defineEmits<{
  (event: "edit", payload: number): void;
}>();

const numberDraft = ref(
    String(model.value),
);

const minimum = computed(() =>
    Math.min(props.min, props.max),
);

const maximum = computed(() =>
    Math.max(props.min, props.max),
);

const validStep = computed(() =>
    props.step > 0 ? props.step : 1,
);

const progress = computed(() => {
  const range =
      maximum.value - minimum.value;

  if (range <= 0) {
    return 0;
  }

  const value = Math.min(
      maximum.value,
      Math.max(minimum.value, model.value),
  );

  return (
      ((value - minimum.value) / range) *
      100
  );
});

const rangeStyle = computed<CSSProperties>(
    () =>
        ({
          "--slider-progress":
              `${progress.value}%`,
        }) as CSSProperties,
);

watch(
    () => model.value,
    (value) => {
      numberDraft.value = String(value);
    },
);

function decimalPlaces(
    value: number,
): number {
  const text = String(value);

  if (
      text.includes("e-") ||
      text.includes("E-")
  ) {
    const exponent = Number(
        text.split(/e-/i)[1],
    );

    return Number.isFinite(exponent)
        ? exponent
        : 0;
  }

  return text.includes(".")
      ? text.split(".")[1].length
      : 0;
}

function normalizeValue(
    value: number,
): number {
  const clamped = Math.min(
      maximum.value,
      Math.max(minimum.value, value),
  );

  const steps =
      Math.round(
          (clamped - minimum.value) /
          validStep.value,
      );

  const stepped =
      minimum.value +
      steps * validStep.value;

  const precision = Math.max(
      decimalPlaces(minimum.value),
      decimalPlaces(maximum.value),
      decimalPlaces(validStep.value),
  );

  return Number(
      Math.min(
          maximum.value,
          Math.max(minimum.value, stepped),
      ).toFixed(precision),
  );
}

function commitValue(value: number): void {
  if (
      props.read_only ||
      !Number.isFinite(value)
  ) {
    return;
  }

  const normalized =
      normalizeValue(value);

  numberDraft.value =
      String(normalized);

  if (normalized === model.value) {
    return;
  }

  model.value = normalized;
  emit("edit", normalized);
}

function onRangeInput(
    event: Event,
): void {
  const input =
      event.currentTarget as HTMLInputElement;

  commitValue(Number(input.value));
}

function onNumberInput(
    event: Event,
): void {
  const input =
      event.currentTarget as HTMLInputElement;

  numberDraft.value = input.value;
}

function commitNumberDraft(): void {
  const parsed = Number(
      numberDraft.value,
  );

  if (
      !numberDraft.value.trim() ||
      !Number.isFinite(parsed)
  ) {
    numberDraft.value =
        String(model.value);

    return;
  }

  commitValue(parsed);
}

function onNumberKeydown(
    event: KeyboardEvent,
): void {
  const input =
      event.currentTarget as HTMLInputElement;

  if (event.key === "Enter") {
    event.preventDefault();
    commitNumberDraft();
    input.blur();
  }

  if (event.key === "Escape") {
    event.preventDefault();

    numberDraft.value =
        String(model.value);

    input.blur();
  }
}
</script>

<template>
  <div
      class="slider"
      :class="{
      'slider--disabled':
        props.read_only,
    }"
  >
    <div class="slider__range-container">
      <input
          class="slider__range"
          type="range"
          :min="minimum"
          :max="maximum"
          :step="validStep"
          :value="model"
          :style="rangeStyle"
          :disabled="props.read_only"
          :aria-label="props.label"
          @input="onRangeInput"
      />
    </div>

    <div class="slider__number-container">
      <input
          class="slider__number"
          type="number"
          inputmode="decimal"
          :min="minimum"
          :max="maximum"
          :step="validStep"
          :value="numberDraft"
          :disabled="props.read_only"
          :aria-label="
          `${props.label}, direct input`
        "
          @input="onNumberInput"
          @change="commitNumberDraft"
          @blur="commitNumberDraft"
          @keydown="onNumberKeydown"
      />
    </div>
  </div>
</template>

<style scoped>
.slider {
  width: 100%;
  min-width: 0;

  display: flex;
  align-items: center;
  gap: var(--space-3);

  color: rgb(var(--c-fg));
  font-family: var(--font-primary);
}

/* -------------------------------------------------------------------------- */
/* Range                                                                      */
/* -------------------------------------------------------------------------- */

.slider__range-container {
  flex: 1 1 auto;
  min-width: 5rem;

  display: flex;
  align-items: center;

  min-height: 2.25rem;
}

.slider__range {
  --slider-progress: 0%;

  width: 100%;
  height: 0.5rem;

  appearance: none;

  margin: 0;
  padding: 0;

  background:
      linear-gradient(
          to right,
          rgb(var(--c-primary))
          0%,
          rgb(var(--c-primary))
          var(--slider-progress),
          rgb(var(--c-surface-3))
          var(--slider-progress),
          rgb(var(--c-surface-3))
          100%
      );

  border:
      1px solid
      rgb(var(--c-border) / 0.36);
  border-radius: var(--radius-round);
  outline: 0;

  box-shadow:
      inset 0 2px 4px
      rgb(var(--c-shadow) / 0.1),
      inset 0 1px 0
      rgb(255 255 255 / 0.16);

  cursor: pointer;
}

/* Chrome, Edge, Safari */

.slider__range::-webkit-slider-thumb {
  width: 1.15rem;
  height: 1.15rem;

  appearance: none;

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-accent)),
          rgb(var(--c-primary-strong))
      );

  border:
      2px solid
      rgb(var(--c-on-accent) / 0.86);
  border-radius: 50%;

  box-shadow:
      0 3px 8px
      rgb(var(--c-shadow) / 0.25),
      0 0 0 1px
      rgb(var(--c-primary) / 0.22);

  cursor: grab;

  transition:
      box-shadow
      var(--duration-fast)
      var(--ease-standard),
      transform
      var(--duration-fast)
      var(--ease-standard);
}

.slider__range:hover:not(:disabled)
::-webkit-slider-thumb {
  box-shadow:
      0 4px 10px
      rgb(var(--c-shadow) / 0.3),
      0 0 0 4px
      rgb(var(--c-accent) / 0.13);
}

.slider__range:active:not(:disabled)
::-webkit-slider-thumb {
  cursor: grabbing;
  transform: scale(1.08);
}

.slider__range:focus-visible
::-webkit-slider-thumb {
  box-shadow:
      0 0 0
      var(--focus-ring-width)
      rgb(var(--focus-ring-color) / 0.34),
      0 3px 8px
      rgb(var(--c-shadow) / 0.25);
}

/* Firefox */

.slider__range::-moz-range-track {
  height: 0.5rem;

  background:
      rgb(var(--c-surface-3));

  border:
      1px solid
      rgb(var(--c-border) / 0.36);
  border-radius: var(--radius-round);

  box-shadow:
      inset 0 2px 4px
      rgb(var(--c-shadow) / 0.1);
}

.slider__range::-moz-range-progress {
  height: 0.5rem;

  background:
      linear-gradient(
          to right,
          rgb(var(--c-primary)),
          rgb(var(--c-primary-strong))
      );

  border-radius: var(--radius-round);
}

.slider__range::-moz-range-thumb {
  width: 1.15rem;
  height: 1.15rem;

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-accent)),
          rgb(var(--c-primary-strong))
      );

  border:
      2px solid
      rgb(var(--c-on-accent) / 0.86);
  border-radius: 50%;

  box-shadow:
      0 3px 8px
      rgb(var(--c-shadow) / 0.25);

  cursor: grab;
}

.slider__range:focus-visible
::-moz-range-thumb {
  box-shadow:
      0 0 0
      var(--focus-ring-width)
      rgb(var(--focus-ring-color) / 0.34),
      0 3px 8px
      rgb(var(--c-shadow) / 0.25);
}

/* -------------------------------------------------------------------------- */
/* Direct input                                                               */
/* -------------------------------------------------------------------------- */

.slider__number-container {
  position: relative;

  width: 5.5rem;
  flex: 0 0 auto;
}

.slider__number {
  width: 100%;
  min-height: 2.25rem;
  box-sizing: border-box;
  appearance: textfield;
  -moz-appearance: textfield;

  padding:
      var(--space-1)
      var(--space-2);

  color: rgb(var(--c-fg-strong));

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-surface-raised) / 0.84),
          rgb(var(--c-surface-2) / 0.64)
      );

  border:
      1px solid
      rgb(var(--c-border) / 0.38);
  border-radius: var(--radius-sm);
  outline: 0;

  box-shadow:
      inset 0 1px 0
      rgb(255 255 255 / 0.28);

  font: inherit;
  font-family: var(--font-monospace);
  font-size: 0.76rem;
  font-weight: 750;
  line-height: 1.2;
  text-align: right;

  font-variant-numeric: tabular-nums;

  transition:
      background-color
      var(--duration-fast)
      var(--ease-standard),
      border-color
      var(--duration-fast)
      var(--ease-standard),
      box-shadow
      var(--duration-fast)
      var(--ease-standard);
}

.slider__number:hover:not(:disabled) {
  border-color:
      rgb(var(--c-primary) / 0.54);
}

.slider__number:focus {
  background:
      rgb(var(--c-surface-raised));

  border-color:
      rgb(var(--c-accent) / 0.74);

  box-shadow:
      0 0 0
      var(--focus-ring-width)
      rgb(var(--focus-ring-color) / 0.16),
      inset 0 1px 0
      rgb(255 255 255 / 0.34);
}

/* -------------------------------------------------------------------------- */
/* Disabled                                                                   */
/* -------------------------------------------------------------------------- */

.slider--disabled {
  opacity: 0.5;
}

.slider--disabled
.slider__range,
.slider--disabled
.slider__number {
  cursor: not-allowed;
}

/* -------------------------------------------------------------------------- */
/* Responsive                                                                 */
/* -------------------------------------------------------------------------- */

@media (max-width: 420px) {
  .slider {
    align-items: stretch;
    flex-direction: column;
    gap: var(--space-1);
  }

  .slider__number-container {
    width: 100%;
  }

  .slider__number {
    text-align: left;
  }
}

@media (prefers-reduced-motion: reduce) {
  .slider__range::-webkit-slider-thumb,
  .slider__number {
    transition: none;
  }
}
</style>