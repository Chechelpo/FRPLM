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

function onToggle(event: Event): void {
  if (props.read_only) {
    return;
  }

  const input =
      event.currentTarget as HTMLInputElement;

  model.value = input.checked;
  emit("edit", input.checked);
}
</script>

<template>
  <label
      class="toggle"
      :class="{
      'toggle--checked': model,
      'toggle--disabled': props.read_only,
    }"
      :title="props.label"
  >
    <input
        class="toggle__input"
        type="checkbox"
        :checked="model"
        :disabled="props.read_only"
        :aria-label="props.label"
        @change="onToggle"
    />

    <span
        class="toggle__track"
        aria-hidden="true"
    >
      <span class="toggle__thumb">
        <svg
            class="
            toggle__state-icon
            toggle__state-icon--off
          "
            viewBox="0 0 12 12"
        >
          <path d="M3 3l6 6" />
          <path d="M9 3 3 9" />
        </svg>

        <svg
            class="
            toggle__state-icon
            toggle__state-icon--on
          "
            viewBox="0 0 12 12"
        >
          <path d="m2.5 6 2.2 2.2L9.5 3.5" />
        </svg>
      </span>
    </span>
  </label>
</template>

<style scoped>
.toggle {
  position: relative;

  display: inline-flex;
  align-items: center;
  flex: 0 0 auto;

  cursor: pointer;
  user-select: none;
}

.toggle__input {
  position: absolute;

  width: 1px;
  height: 1px;

  margin: 0;
  padding: 0;

  opacity: 0;
  pointer-events: none;
}

.toggle__track {
  position: relative;

  width: 2.7rem;
  height: 1.5rem;
  box-sizing: border-box;

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-surface-3) / 0.9),
          rgb(var(--c-surface-2) / 0.76)
      );

  border:
      1px solid
      rgb(var(--c-border) / 0.48);
  border-radius: var(--radius-round);

  box-shadow:
      inset 0 2px 4px
      rgb(var(--c-shadow) / 0.1),
      inset 0 1px 0
      rgb(255 255 255 / 0.16);

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

.toggle__thumb {
  position: absolute;
  top: 0.19rem;
  left: 0.19rem;

  width: 1rem;
  height: 1rem;

  display: grid;
  place-items: center;

  color: rgb(var(--c-muted));

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-surface-raised)),
          rgb(var(--c-surface-2))
      );

  border:
      1px solid
      rgb(var(--c-border) / 0.38);
  border-radius: 50%;

  box-shadow:
      0 2px 5px
      rgb(var(--c-shadow) / 0.2),
      inset 0 1px 0
      rgb(255 255 255 / 0.42);

  transform: translateX(0);

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
      transform
      var(--duration-normal)
      var(--ease-standard);
}

.toggle__state-icon {
  position: absolute;

  width: 0.55rem;
  height: 0.55rem;

  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;

  transition:
      opacity
      var(--duration-fast)
      var(--ease-standard),
      transform
      var(--duration-fast)
      var(--ease-standard);
}

.toggle__state-icon--on {
  opacity: 0;
  transform: scale(0.65);
}

.toggle__state-icon--off {
  opacity: 1;
  transform: scale(1);
}

/* Checked */

.toggle--checked .toggle__track {
  background:
      linear-gradient(
          145deg,
          rgb(var(--c-primary)),
          rgb(var(--c-primary-strong))
      );

  border-color:
      rgb(var(--c-accent) / 0.72);

  box-shadow:
      inset 0 2px 4px
      rgb(var(--c-shadow) / 0.16),
      0 0 0 1px
      rgb(var(--c-accent) / 0.08),
      0 0 12px
      rgb(var(--c-accent) / 0.1);
}

.toggle--checked .toggle__thumb {
  color: rgb(var(--c-primary-strong));

  background:
      rgb(var(--c-on-accent));

  border-color:
      rgb(255 255 255 / 0.48);

  transform: translateX(1.28rem);
}

.toggle--checked
.toggle__state-icon--off {
  opacity: 0;
  transform: scale(0.65);
}

.toggle--checked
.toggle__state-icon--on {
  opacity: 1;
  transform: scale(1);
}

/* Interaction */

.toggle:hover:not(.toggle--disabled)
.toggle__track {
  border-color:
      rgb(var(--c-primary) / 0.68);
}

.toggle:hover:not(.toggle--disabled)
.toggle__thumb {
  box-shadow:
      0 3px 7px
      rgb(var(--c-shadow) / 0.24),
      inset 0 1px 0
      rgb(255 255 255 / 0.46);
}

.toggle__input:focus-visible
+ .toggle__track {
  outline:
      var(--focus-ring-width)
      solid
      rgb(var(--focus-ring-color) / 0.34);

  outline-offset: 2px;
}

.toggle__input:active
+ .toggle__track
.toggle__thumb {
  width: 1.1rem;
}

.toggle--disabled {
  opacity: 0.48;
  cursor: not-allowed;
}

@media (prefers-reduced-motion: reduce) {
  .toggle__track,
  .toggle__thumb,
  .toggle__state-icon {
    transition: none;
  }
}
</style>