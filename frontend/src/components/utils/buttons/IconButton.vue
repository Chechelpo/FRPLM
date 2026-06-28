<script setup lang="ts">
withDefaults(
    defineProps<{
      title: string;
      disabled?: boolean;
      loading?: boolean;
      type?: "button" | "submit" | "reset";
      variant?: "default" | "accent" | "danger";
    }>(),
    {
      disabled: false,
      loading: false,
      type: "button",
      variant: "default",
    },
);

const emit = defineEmits<{
  click: [event: MouseEvent];
}>();
</script>

<template>
  <button
      :type="type"
      class="icon-button"
      :class="`icon-button--${variant}`"
      :disabled="disabled || loading"
      :title="title"
      :aria-label="title"
      :aria-busy="loading"
      @click="emit('click', $event)"
  >
    <span
        v-if="loading"
        class="icon-button__spinner"
        aria-hidden="true"
    />

    <span
        v-else
        class="icon-button__content"
        aria-hidden="true"
    >
      <slot />
    </span>
  </button>
</template>

<style scoped>
.icon-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;

  flex: 0 0 auto;
  width: 2rem;
  height: 2rem;
  padding: 0;

  color: inherit;
  background: transparent;

  border: 1px solid transparent;
  border-radius: 0.35rem;

  font: inherit;
  cursor: pointer;

  transition:
      color 120ms ease,
      background-color 120ms ease,
      border-color 120ms ease,
      opacity 120ms ease,
      transform 120ms ease;
}

.icon-button:hover:not(:disabled) {
  background: color-mix(
      in srgb,
      currentColor 10%,
      transparent
  );

  transform: translateY(-1px);
}

.icon-button:active:not(:disabled) {
  transform: translateY(0);
}

.icon-button:focus-visible {
  outline: 2px solid var(--primary-accent, currentColor);
  outline-offset: 2px;
}

.icon-button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.icon-button--accent {
  color: var(--primary-accent, #f59e0b);

  background: color-mix(
      in srgb,
      var(--primary-accent, #f59e0b) 8%,
      transparent
  );

  border-color: color-mix(
      in srgb,
      var(--primary-accent, #f59e0b) 30%,
      transparent
  );
}

.icon-button--accent:hover:not(:disabled) {
  color: var(--primary-text, #ffffff);

  background: color-mix(
      in srgb,
      var(--primary-accent, #f59e0b) 75%,
      transparent
  );

  border-color: var(--primary-accent, #f59e0b);
}

.icon-button--danger {
  color: var(--danger-color, #dc2626);
}

.icon-button--danger:hover:not(:disabled) {
  color: var(--danger-text, #ffffff);
  background: var(--danger-color, #dc2626);
  border-color: var(--danger-color, #dc2626);
}

.icon-button__content {
  display: inline-flex;
  align-items: center;
  justify-content: center;

  width: 1.1rem;
  height: 1.1rem;
}

.icon-button__content :deep(svg) {
  width: 100%;
  height: 100%;

  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.icon-button__spinner {
  width: 1rem;
  height: 1rem;

  border: 2px solid currentColor;
  border-right-color: transparent;
  border-radius: 50%;

  animation: icon-button-spin 650ms linear infinite;
}

@keyframes icon-button-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>