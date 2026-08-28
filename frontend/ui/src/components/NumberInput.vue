<script setup lang="ts">
import {
  computed,
  onBeforeUnmount,
  ref,
  watch,
} from "vue";

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

      /**
       * Displays the integrated decrement and
       * increment controls.
       */
      showControls?: boolean;

      ariaLabel?: string;
    }>(),
    {
      nullable: false,
      placeholder: "",
      step: 1,
      disabled: false,
      showControls: false,
      ariaLabel: "Number input",
    },
);

const emit = defineEmits<{
  (
      event: "edit",
      payload: number | null,
  ): void;
}>();

const value = ref<number | null>(
    model.value,
);

const draft = ref(
    formatValue(model.value),
);

const focused = ref(false);

const lastEmitted = ref<number | null>(
    model.value,
);

let timer:
    | ReturnType<typeof setTimeout>
    | null = null;

const normalizedStep = computed(() => {
  return props.step > 0
      ? props.step
      : 1;
});

const inputInvalid = computed(() => {
  const raw = draft.value.trim();

  if (!raw) {
    return !props.nullable;
  }

  const parsed = Number(raw);

  if (!Number.isFinite(parsed)) {
    return true;
  }

  if (
      props.min !== undefined &&
      parsed < props.min
  ) {
    return true;
  }

  if (
      props.max !== undefined &&
      parsed > props.max
  ) {
    return true;
  }

  return false;
});

const decrementDisabled = computed(() => {
  if (props.disabled) {
    return true;
  }

  return (
      value.value !== null &&
      props.min !== undefined &&
      value.value <= props.min
  );
});

const incrementDisabled = computed(() => {
  if (props.disabled) {
    return true;
  }

  return (
      value.value !== null &&
      props.max !== undefined &&
      value.value >= props.max
  );
});

function formatValue(
    currentValue: number | null,
): string {
  return currentValue === null
      ? ""
      : String(currentValue);
}

function decimalPlaces(
    number: number,
): number {
  const text = String(number);

  if (/e-/i.test(text)) {
    const exponent = Number(
        text.split(/e-/i)[1],
    );

    return Number.isFinite(exponent)
        ? exponent
        : 0;
  }

  const decimalIndex =
      text.indexOf(".");

  return decimalIndex === -1
      ? 0
      : text.length - decimalIndex - 1;
}

function normalizeValue(
    nextValue: number,
): number {
  let normalized = nextValue;

  if (props.min !== undefined) {
    normalized = Math.max(
        props.min,
        normalized,
    );
  }

  if (props.max !== undefined) {
    normalized = Math.min(
        props.max,
        normalized,
    );
  }

  const step = normalizedStep.value;
  const base = props.min ?? 0;

  normalized =
      base +
      Math.round(
          (normalized - base) / step,
      ) *
      step;

  if (props.min !== undefined) {
    normalized = Math.max(
        props.min,
        normalized,
    );
  }

  if (props.max !== undefined) {
    normalized = Math.min(
        props.max,
        normalized,
    );
  }

  const precision = Math.max(
      decimalPlaces(step),
      decimalPlaces(base),
      decimalPlaces(normalized),
  );

  return Number(
      normalized.toFixed(
          Math.min(precision, 12),
      ),
  );
}

function clearScheduledEmit(): void {
  if (timer === null) {
    return;
  }

  clearTimeout(timer);
  timer = null;
}

function emitEditImmediately(
    nextValue: number | null,
): void {
  clearScheduledEmit();

  if (
      nextValue === lastEmitted.value
  ) {
    return;
  }

  lastEmitted.value = nextValue;
  emit("edit", nextValue);
}

function scheduleEditEmit(
    nextValue: number | null,
): void {
  clearScheduledEmit();

  timer = setTimeout(() => {
    timer = null;

    if (
        nextValue === lastEmitted.value
    ) {
      return;
    }

    lastEmitted.value = nextValue;
    emit("edit", nextValue);
  }, EDIT_EMIT_MS);
}

function applyValue(
    nextValue: number | null,
    immediate = false,
): void {
  value.value = nextValue;
  model.value = nextValue;

  if (immediate) {
    emitEditImmediately(nextValue);
  } else {
    scheduleEditEmit(nextValue);
  }
}

function onInput(event: Event): void {
  const input =
      event.currentTarget as HTMLInputElement;

  draft.value = input.value;

  const raw = draft.value.trim();

  if (!raw) {
    if (props.nullable) {
      applyValue(null);
    }

    return;
  }

  const parsed = Number(raw);

  if (!Number.isFinite(parsed)) {
    return;
  }

  /*
   * Do not clamp while typing. This allows
   * intermediate values such as "-" or "1"
   * while entering "-10" or "100".
   */
  applyValue(parsed);
}

function commitDraft(): void {
  const raw = draft.value.trim();

  if (!raw) {
    if (props.nullable) {
      draft.value = "";
      applyValue(null, true);
      return;
    }

    const fallback =
        value.value ??
        props.min ??
        0;

    const normalized =
        normalizeValue(fallback);

    draft.value =
        formatValue(normalized);

    applyValue(normalized, true);
    return;
  }

  const parsed = Number(raw);

  if (!Number.isFinite(parsed)) {
    draft.value =
        formatValue(value.value);

    return;
  }

  const normalized =
      normalizeValue(parsed);

  draft.value =
      formatValue(normalized);

  applyValue(normalized, true);
}

function stepValue(
    direction: -1 | 1,
): void {
  if (props.disabled) {
    return;
  }

  const baseValue =
      value.value ??
      props.min ??
      0;

  const nextValue = normalizeValue(
      baseValue +
      normalizedStep.value *
      direction,
  );

  draft.value =
      formatValue(nextValue);

  applyValue(nextValue, true);
}

function onFocus(): void {
  focused.value = true;
}

function onBlur(): void {
  focused.value = false;
  commitDraft();
}

function onKeydown(
    event: KeyboardEvent,
): void {
  if (event.key === "Enter") {
    event.preventDefault();

    commitDraft();

    (
        event.currentTarget as
            HTMLInputElement
    ).blur();

    return;
  }

  if (event.key === "Escape") {
    event.preventDefault();

    clearScheduledEmit();

    draft.value =
        formatValue(value.value);

    (
        event.currentTarget as
            HTMLInputElement
    ).blur();
  }
}

watch(
    () => model.value,
    newValue => {
      /*
       * Ignore model writes originating from this
       * component. External model changes still
       * update the visible value.
       */
      if (newValue === value.value) {
        return;
      }

      clearScheduledEmit();

      value.value = newValue;
      lastEmitted.value = newValue;
      draft.value =
          formatValue(newValue);
    },
);

onBeforeUnmount(() => {
  clearScheduledEmit();
});
</script>

<template>
  <div
      class="number-input"
      :class="{
      'number-input--focused': focused,
      'number-input--invalid':
        inputInvalid,
      'number-input--disabled':
        props.disabled,
      'number-input--with-controls':
        props.showControls,
    }"
  >
    <button
        v-if="props.showControls"
        type="button"
        class="
        number-input__step
        number-input__step--decrement
      "
        :disabled="decrementDisabled"
        :aria-label="
        `Decrease ${props.ariaLabel}`
      "
        tabindex="-1"
        @click="stepValue(-1)"
    >
      <svg
          viewBox="0 0 16 16"
          aria-hidden="true"
      >
        <path d="M3 8h10" />
      </svg>
    </button>

    <input
        class="number-input__control"
        type="number"
        inputmode="decimal"
        :value="draft"
        :placeholder="props.placeholder"
        :min="props.min"
        :max="props.max"
        :step="normalizedStep"
        :disabled="props.disabled"
        :aria-label="props.ariaLabel"
        :aria-invalid="inputInvalid"
        @input="onInput"
        @focus="onFocus"
        @blur="onBlur"
        @keydown="onKeydown"
    />

    <button
        v-if="props.showControls"
        type="button"
        class="
        number-input__step
        number-input__step--increment
      "
        :disabled="incrementDisabled"
        :aria-label="
        `Increase ${props.ariaLabel}`
      "
        tabindex="-1"
        @click="stepValue(1)"
    >
      <svg
          viewBox="0 0 16 16"
          aria-hidden="true"
      >
        <path d="M3 8h10" />
        <path d="M8 3v10" />
      </svg>
    </button>
  </div>
</template>

<style scoped>
.number-input {
  width: 100%;
  min-width: 0;
  min-height: 2.4rem;
  box-sizing: border-box;

  display: flex;
  align-items: stretch;

  color: rgb(var(--c-fg));

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-surface-raised) / 0.78),
          rgb(var(--c-surface-2) / 0.56)
      );

  border:
      1px solid
      rgb(var(--c-border) / 0.4);
  border-radius: var(--radius-sm);

  box-shadow:
      inset 0 1px 0
      rgb(255 255 255 / 0.26),
      0 2px 7px
      rgb(var(--c-shadow) / 0.04);

  overflow: hidden;

  transition:
      background-color
      var(--duration-fast)
      var(--ease-standard),
      border-color
      var(--duration-fast)
      var(--ease-standard),
      box-shadow
      var(--duration-fast)
      var(--ease-standard),
      opacity
      var(--duration-fast)
      var(--ease-standard);
}

.number-input:hover:not(
    .number-input--disabled
  ) {
  border-color:
      rgb(var(--c-primary) / 0.52);
}

.number-input--focused {
  background:
      rgb(var(--c-surface-raised));

  border-color:
      rgb(var(--c-accent) / 0.78);

  box-shadow:
      0 0 0
      var(--focus-ring-width)
      rgb(var(--focus-ring-color) / 0.16),
      inset 0 1px 0
      rgb(255 255 255 / 0.32),
      0 3px 9px
      rgb(var(--c-shadow) / 0.06);
}

.number-input--invalid:not(
    .number-input--focused
  ) {
  border-color:
      rgb(var(--c-danger) / 0.62);
}

.number-input--invalid.number-input--focused {
  border-color:
      rgb(var(--c-danger));

  box-shadow:
      0 0 0
      var(--focus-ring-width)
      rgb(var(--c-danger) / 0.15);
}

.number-input--disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* -------------------------------------------------------------------------- */
/* Input                                                                      */
/* -------------------------------------------------------------------------- */

.number-input__control {
  flex: 1 1 auto;

  display: block;

  width: 100%;
  min-width: 0;
  max-width: 100%;
  min-height: 2.4rem;
  box-sizing: border-box;

  padding:
      var(--space-2)
      var(--space-3);

  color: rgb(var(--c-fg-strong));
  caret-color: rgb(var(--c-accent));

  background: transparent;
  border: 0;
  outline: 0;

  font: inherit;
  font-size: 0.8rem;
  font-weight: 700;
  line-height: 1.2;
  text-align: right;

  font-variant-numeric: tabular-nums;

  appearance: textfield;
  -moz-appearance: textfield;
}

.number-input__control::placeholder {
  color: rgb(var(--c-muted) / 0.68);
  font-weight: 500;
}

.number-input__control:disabled {
  color: rgb(var(--c-muted));

  cursor: not-allowed;
}

.number-input__control::-webkit-inner-spin-button,
.number-input__control::-webkit-outer-spin-button {
  margin: 0;

  appearance: none;
  -webkit-appearance: none;
}

/* -------------------------------------------------------------------------- */
/* Step controls                                                              */
/* -------------------------------------------------------------------------- */

.number-input__step {
  width: 2.25rem;
  min-height: 2.4rem;
  flex: 0 0 auto;

  display: grid;
  place-items: center;

  padding: 0;

  color: rgb(var(--c-muted));

  background:
      rgb(var(--c-surface-2) / 0.38);

  border: 0;
  outline: 0;

  cursor: pointer;

  transition:
      color
      var(--duration-fast)
      var(--ease-standard),
      background-color
      var(--duration-fast)
      var(--ease-standard),
      box-shadow
      var(--duration-fast)
      var(--ease-standard);
}

.number-input__step--decrement {
  border-right:
      1px solid
      rgb(var(--c-border) / 0.28);
}

.number-input__step--increment {
  border-left:
      1px solid
      rgb(var(--c-border) / 0.28);
}

.number-input__step:hover:not(:disabled) {
  color: rgb(var(--c-on-accent));

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-primary)),
          rgb(var(--c-primary-strong))
      );
}

.number-input__step:active:not(:disabled) {
  background:
      rgb(var(--c-primary-strong));
}

.number-input__step:focus-visible {
  position: relative;
  z-index: 1;

  box-shadow:
      inset 0 0 0
      var(--focus-ring-width)
      rgb(var(--focus-ring-color) / 0.38);
}

.number-input__step:disabled {
  color: rgb(var(--c-muted-soft));

  background:
      rgb(var(--c-surface-2) / 0.18);

  opacity: 0.48;
  cursor: not-allowed;
}

.number-input__step svg {
  width: 0.85rem;
  height: 0.85rem;

  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
}

/* -------------------------------------------------------------------------- */
/* Without custom controls                                                    */
/* -------------------------------------------------------------------------- */

.number-input:not(
    .number-input--with-controls
  )
.number-input__control {
  text-align: left;
}

/* -------------------------------------------------------------------------- */
/* Responsive                                                                 */
/* -------------------------------------------------------------------------- */

@media (max-width: 420px) {
  .number-input__step {
    width: 2rem;
  }

  .number-input__control {
    padding-inline: var(--space-2);
  }
}

@media (prefers-reduced-motion: reduce) {
  .number-input,
  .number-input__step {
    transition: none;
  }
}
</style>