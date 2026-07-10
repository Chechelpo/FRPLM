<script setup lang="ts">
const model = defineModel<boolean>({
  required: true,
});

const props = withDefaults(
    defineProps<{
      read_only?: boolean;
      label?: string;
    }>(),
    {
      read_only: false,
      label: "Toggle option",
    },
);

const emit = defineEmits<{
  (event: "edit", value: boolean): void;
}>();

function onChange(event: Event): void {
  if (props.read_only) {
    return;
  }

  const checked = (
      event.currentTarget as HTMLInputElement
  ).checked;

  model.value = checked;
  emit("edit", checked);
}
</script>

<template>
  <label
      class="boolean-checkbox"
      :class="{
      'boolean-checkbox--checked': model,
      'boolean-checkbox--disabled':
        props.read_only,
    }"
      :title="props.label"
  >
    <input
        class="boolean-checkbox__input"
        type="checkbox"
        :checked="model"
        :disabled="props.read_only"
        :aria-label="props.label"
        @change="onChange"
    />

    <span
        class="boolean-checkbox__box"
        aria-hidden="true"
    >
      <svg viewBox="0 0 12 12">
        <path d="m2.25 6.1 2.35 2.35 5.15-5.15" />
      </svg>
    </span>
  </label>
</template>

<style scoped>
.boolean-checkbox {
  position: relative;

  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;

  width: 1.15rem;
  height: 1.15rem;

  cursor: pointer;
  user-select: none;
}

.boolean-checkbox__input {
  position: absolute;

  width: 1px;
  height: 1px;

  margin: 0;
  padding: 0;

  opacity: 0;
  pointer-events: none;
}

.boolean-checkbox__box {
  width: 1rem;
  height: 1rem;
  box-sizing: border-box;

  display: grid;
  place-items: center;

  color: transparent;

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-surface-raised) / 0.8),
          rgb(var(--c-surface-2) / 0.62)
      );

  border:
      1px solid
      rgb(var(--c-border) / 0.52);
  border-radius: var(--radius-xs);

  box-shadow:
      inset 0 1px 0
      rgb(255 255 255 / 0.24);

  transition:
      color
      var(--duration-fast)
      var(--ease-standard),
      background-color
      var(--duration-fast)
      var(--ease-standard),
      border-color
      var(--duration-fast)
      var(--ease-standard),
      box-shadow
      var(--duration-fast)
      var(--ease-standard),
      transform
      var(--duration-fast)
      var(--ease-standard);
}

.boolean-checkbox__box svg {
  width: 0.7rem;
  height: 0.7rem;

  fill: none;
  stroke: currentColor;
  stroke-width: 2.2;
  stroke-linecap: round;
  stroke-linejoin: round;

  opacity: 0;
  transform: scale(0.65);

  transition:
      opacity
      var(--duration-fast)
      var(--ease-standard),
      transform
      var(--duration-fast)
      var(--ease-standard);
}

/* Checked */

.boolean-checkbox--checked
.boolean-checkbox__box {
  color: rgb(var(--c-on-accent));

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-primary)),
          rgb(var(--c-primary-strong))
      );

  border-color:
      rgb(var(--c-accent) / 0.82);

  box-shadow:
      inset 0 1px 0
      rgb(255 255 255 / 0.32),
      0 1px 4px
      rgb(var(--c-shadow) / 0.16);
}

.boolean-checkbox--checked
.boolean-checkbox__box svg {
  opacity: 1;
  transform: scale(1);
}

/* Interaction */

.boolean-checkbox:hover:not(
    .boolean-checkbox--disabled
  )
.boolean-checkbox__box {
  border-color:
      rgb(var(--c-accent) / 0.72);

  background:
      rgb(var(--c-surface-hover));
}

.boolean-checkbox--checked:hover:not(
    .boolean-checkbox--disabled
  )
.boolean-checkbox__box {
  background:
      linear-gradient(
          145deg,
          rgb(var(--c-accent)),
          rgb(var(--c-primary-strong))
      );
}

.boolean-checkbox__input:focus-visible
+ .boolean-checkbox__box {
  outline:
      var(--focus-ring-width)
      solid
      rgb(var(--focus-ring-color) / 0.34);

  outline-offset: 2px;
}

.boolean-checkbox:active:not(
    .boolean-checkbox--disabled
  )
.boolean-checkbox__box {
  transform: scale(0.9);
}

/* Disabled */

.boolean-checkbox--disabled {
  opacity: 0.46;
  cursor: not-allowed;
}

@media (prefers-reduced-motion: reduce) {
  .boolean-checkbox__box,
  .boolean-checkbox__box svg {
    transition: none;
  }
}
</style>