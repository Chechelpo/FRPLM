<script setup lang="ts">
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
} from "vue";

const props = withDefaults(
    defineProps<{
      fieldName: string | null;
      info?: string;
      vertical?: boolean;
    }>(),
    {
      info: "",
      vertical: false,
    },
);

const infoButton =
    ref<HTMLButtonElement | null>(null);

const tooltip =
    ref<HTMLElement | null>(null);

const tooltipVisible = ref(false);

const tooltipPosition = ref({
  top: 0,
  left: 0,
});

const tooltipPlacement = ref<
    "top" | "bottom"
>("bottom");

const tooltipId =
    `field-info-${Math.random()
        .toString(36)
        .slice(2)}`;

const hasInfo = computed(
    () => props.info.trim().length > 0,
);

function clamp(
    value: number,
    minimum: number,
    maximum: number,
): number {
  return Math.min(
      Math.max(value, minimum),
      maximum,
  );
}

async function updateTooltipPosition(): Promise<void> {
  const button = infoButton.value;

  if (!button) {
    return;
  }

  await nextTick();

  const tooltipElement = tooltip.value;

  if (!tooltipElement) {
    return;
  }

  const buttonRect =
      button.getBoundingClientRect();

  const tooltipRect =
      tooltipElement.getBoundingClientRect();

  const viewportPadding = 8;
  const gap = 8;

  const availableBelow =
      window.innerHeight -
      buttonRect.bottom -
      viewportPadding;

  const availableAbove =
      buttonRect.top -
      viewportPadding;

  const shouldPlaceAbove =
      tooltipRect.height + gap >
      availableBelow &&
      availableAbove > availableBelow;

  tooltipPlacement.value =
      shouldPlaceAbove
          ? "top"
          : "bottom";

  const preferredTop =
      shouldPlaceAbove
          ? buttonRect.top -
          tooltipRect.height -
          gap
          : buttonRect.bottom + gap;

  const preferredLeft =
      buttonRect.left +
      buttonRect.width / 2 -
      tooltipRect.width / 2;

  tooltipPosition.value = {
    top: clamp(
        preferredTop,
        viewportPadding,
        Math.max(
            viewportPadding,
            window.innerHeight -
            tooltipRect.height -
            viewportPadding,
        ),
    ),

    left: clamp(
        preferredLeft,
        viewportPadding,
        Math.max(
            viewportPadding,
            window.innerWidth -
            tooltipRect.width -
            viewportPadding,
        ),
    ),
  };
}

async function showTooltip(): Promise<void> {
  if (!hasInfo.value) {
    return;
  }

  tooltipVisible.value = true;

  await updateTooltipPosition();
}

function hideTooltip(): void {
  tooltipVisible.value = false;
}

function toggleTooltip(): void {
  if (tooltipVisible.value) {
    hideTooltip();
    return;
  }

  void showTooltip();
}

function onDocumentPointerDown(
    event: PointerEvent,
): void {
  if (!tooltipVisible.value) {
    return;
  }

  const target = event.target;

  if (
      target instanceof Node &&
      infoButton.value?.contains(target)
  ) {
    return;
  }

  hideTooltip();
}

function onEscape(
    event: KeyboardEvent,
): void {
  if (
      tooltipVisible.value &&
      event.key === "Escape"
  ) {
    hideTooltip();
    infoButton.value?.focus();
  }
}

function onViewportChange(): void {
  if (tooltipVisible.value) {
    hideTooltip();
  }
}

onMounted(() => {
  document.addEventListener(
      "pointerdown",
      onDocumentPointerDown,
      true,
  );

  document.addEventListener(
      "keydown",
      onEscape,
  );

  window.addEventListener(
      "resize",
      onViewportChange,
  );

  window.addEventListener(
      "scroll",
      onViewportChange,
      true,
  );
});

onBeforeUnmount(() => {
  document.removeEventListener(
      "pointerdown",
      onDocumentPointerDown,
      true,
  );

  document.removeEventListener(
      "keydown",
      onEscape,
  );

  window.removeEventListener(
      "resize",
      onViewportChange,
  );

  window.removeEventListener(
      "scroll",
      onViewportChange,
      true,
  );
});
</script>

<template>
  <div
      class="field-wrapper"
      :class="{
      'field-wrapper--vertical':
        props.vertical,
      'field-wrapper--without-label':
        !props.fieldName,
    }"
  >
    <div
        v-if="props.fieldName"
        class="field-wrapper__label-row"
    >
      <span class="field-wrapper__label">
        {{ props.fieldName }}
      </span>

      <button
          v-if="hasInfo"
          ref="infoButton"
          type="button"
          class="field-wrapper__info"
          aria-label="Field information"
          :aria-describedby="
          tooltipVisible
            ? tooltipId
            : undefined
        "
          :aria-expanded="tooltipVisible"
          @mouseenter="showTooltip"
          @mouseleave="hideTooltip"
          @focus="showTooltip"
          @blur="hideTooltip"
          @click.stop="toggleTooltip"
      >
        <svg
            viewBox="0 0 16 16"
            aria-hidden="true"
        >
          <circle
              cx="8"
              cy="8"
              r="6.25"
          />

          <path d="M8 7.25v4" />

          <path
              d="M8 4.6h.01"
          />
        </svg>
      </button>

      <span
          class="field-wrapper__separator"
          aria-hidden="true"
      >
        :
      </span>
    </div>

    <div
        v-if="$slots.default"
        class="field-wrapper__editor"
    >
      <slot />
    </div>

    <Teleport to="body">
      <Transition name="field-tooltip">
        <div
            v-if="
            tooltipVisible &&
            hasInfo
          "
            :id="tooltipId"
            ref="tooltip"
            class="field-tooltip"
            :class="{
            'field-tooltip--top':
              tooltipPlacement === 'top',
            'field-tooltip--bottom':
              tooltipPlacement ===
              'bottom',
          }"
            :style="{
            top:
              `${tooltipPosition.top}px`,
            left:
              `${tooltipPosition.left}px`,
          }"
            role="tooltip"
        >
          {{ props.info }}
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.field-wrapper {
  width: 100%;
  min-width: 0;

  display: grid;
  grid-template-columns:
    max-content
    minmax(0, 1fr);
  align-items: center;

  column-gap: var(--space-2);
  row-gap: var(--space-1);

  color: rgb(var(--c-fg));
  font-family: var(--font-primary);
}

.field-wrapper--vertical {
  grid-template-columns: minmax(0, 1fr);
  align-items: stretch;
}

.field-wrapper--without-label {
  grid-template-columns: minmax(0, 1fr);
}

/* -------------------------------------------------------------------------- */
/* Label                                                                      */
/* -------------------------------------------------------------------------- */

.field-wrapper__label-row {
  min-width: 0;

  display: inline-flex;
  align-items: center;
  gap: 0.22rem;

  white-space: nowrap;
}

.field-wrapper--vertical
.field-wrapper__label-row {
  margin-bottom: var(--space-1);
}

.field-wrapper__label {
  color: rgb(var(--c-fg-strong));

  font-size: 0.76rem;
  font-weight: 750;
  line-height: 1.35;
}

.field-wrapper__separator {
  color: rgb(var(--c-muted));

  font-size: 0.76rem;
  font-weight: 700;
  line-height: 1;
}

.field-wrapper__editor {
  min-width: 0;
}

/* -------------------------------------------------------------------------- */
/* Information button                                                         */
/* -------------------------------------------------------------------------- */

.field-wrapper__info {
  width: 1rem;
  height: 1rem;
  flex: 0 0 auto;

  display: grid;
  place-items: center;

  margin: 0;
  padding: 0;

  color: rgb(var(--c-primary-strong));

  background:
      rgb(var(--c-accent) / 0.08);

  border:
      1px solid
      rgb(var(--c-accent) / 0.2);
  border-radius: 50%;
  outline: 0;

  cursor: help;

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
      var(--duration-fast)
      var(--ease-standard);
}

.field-wrapper__info:hover,
.field-wrapper__info[aria-expanded="true"] {
  color: rgb(var(--c-on-accent));

  background:
      rgb(var(--c-primary));

  border-color:
      rgb(var(--c-accent));
}

.field-wrapper__info:active {
  transform: scale(0.92);
}

.field-wrapper__info:focus-visible {
  outline:
      var(--focus-ring-width)
      solid
      rgb(var(--focus-ring-color) / 0.32);

  outline-offset: 2px;
}

.field-wrapper__info svg {
  width: 0.72rem;
  height: 0.72rem;

  fill: none;
  stroke: currentColor;
  stroke-width: 1.55;
  stroke-linecap: round;
  stroke-linejoin: round;
}

/* -------------------------------------------------------------------------- */
/* Teleported tooltip                                                         */
/* -------------------------------------------------------------------------- */

.field-tooltip {
  position: fixed;
  z-index: 100000;

  width: max-content;
  max-width: min(
      18rem,
      calc(100vw - 1rem)
  );
  box-sizing: border-box;

  padding:
      var(--space-2)
      var(--space-3);

  color: rgb(var(--c-fg));

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-surface-raised) / 0.99),
          rgb(var(--c-surface-2) / 0.98)
      );

  border:
      1px solid
      rgb(var(--c-accent) / 0.38);
  border-radius: var(--radius-sm);

  box-shadow:
      0 10px 28px
      rgb(var(--c-shadow) / 0.22),
      0 3px 9px
      rgb(var(--c-shadow) / 0.1),
      inset 0 1px 0
      rgb(255 255 255 / 0.28);

  font-family: var(--font-primary);
  font-size: 0.74rem;
  font-weight: 500;
  line-height: 1.45;

  overflow-wrap: anywhere;
  white-space: normal;

  pointer-events: none;
  backdrop-filter: blur(14px);
}

.field-tooltip::after {
  content: "";

  position: absolute;
  left: 50%;

  width: 0.5rem;
  height: 0.5rem;

  background:
      rgb(var(--c-surface-raised));

  border:
      1px solid
      rgb(var(--c-accent) / 0.38);

  transform:
      translateX(-50%)
      rotate(45deg);
}

.field-tooltip--bottom::after {
  top: -0.3rem;

  border-right: 0;
  border-bottom: 0;
}

.field-tooltip--top::after {
  bottom: -0.3rem;

  border-top: 0;
  border-left: 0;
}

/* -------------------------------------------------------------------------- */
/* Transition                                                                 */
/* -------------------------------------------------------------------------- */

.field-tooltip-enter-active,
.field-tooltip-leave-active {
  transition:
      opacity
      var(--duration-fast)
      var(--ease-standard),
      transform
      var(--duration-fast)
      var(--ease-standard);
}

.field-tooltip-enter-from,
.field-tooltip-leave-to {
  opacity: 0;

  transform:
      translateY(-0.2rem)
      scale(0.98);
}

.field-tooltip--top.field-tooltip-enter-from,
.field-tooltip--top.field-tooltip-leave-to {
  transform:
      translateY(0.2rem)
      scale(0.98);
}

@media (max-width: 520px) {
  .field-wrapper:not(
      .field-wrapper--vertical
    ) {
    grid-template-columns:
      minmax(0, 1fr);
    align-items: stretch;
  }

  .field-wrapper__label-row {
    white-space: normal;
  }

  .field-wrapper__separator {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .field-wrapper__info,
  .field-tooltip-enter-active,
  .field-tooltip-leave-active {
    transition: none;
  }
}
</style>