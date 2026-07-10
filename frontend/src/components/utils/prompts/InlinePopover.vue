<script setup lang="ts">
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
} from "vue";

type PopoverPlacement =
    | "top-start"
    | "top-end"
    | "bottom-start"
    | "bottom-end";

const props = withDefaults(
    defineProps<{
      title?: string;
      buttonLabel?: string;
      placement?: PopoverPlacement;
      disabled?: boolean;
    }>(),
    {
      title: "",
      buttonLabel: "Open menu",
      placement: "bottom-end",
      disabled: false,
    },
);

const emit = defineEmits<{
  (event: "open"): void;
  (event: "close"): void;
}>();

const root = ref<HTMLElement | null>(null);
const trigger = ref<HTMLButtonElement | null>(
    null,
);
const panel = ref<HTMLElement | null>(null);

const open = ref(false);

const panelTop = ref(0);
const panelLeft = ref(0);
const panelReady = ref(false);

const panelId =
    `inline-popover-${Math.random()
        .toString(36)
        .slice(2)}`;

const panelStyle = computed(() => ({
  top: `${panelTop.value}px`,
  left: `${panelLeft.value}px`,
  visibility: panelReady.value
      ? "visible"
      : "hidden",
}));

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

function updatePosition(): void {
  const triggerElement = trigger.value;
  const panelElement = panel.value;

  if (
      !triggerElement ||
      !panelElement ||
      !open.value
  ) {
    return;
  }

  const triggerRect =
      triggerElement.getBoundingClientRect();

  const panelRect =
      panelElement.getBoundingClientRect();

  const viewportPadding = 8;
  const gap = 8;

  const wantsTop =
      props.placement.startsWith("top");

  const alignStart =
      props.placement.endsWith("start");

  const spaceAbove =
      triggerRect.top -
      viewportPadding;

  const spaceBelow =
      window.innerHeight -
      triggerRect.bottom -
      viewportPadding;

  let placeAbove = wantsTop;

  if (
      wantsTop &&
      panelRect.height + gap > spaceAbove &&
      spaceBelow > spaceAbove
  ) {
    placeAbove = false;
  }

  if (
      !wantsTop &&
      panelRect.height + gap > spaceBelow &&
      spaceAbove > spaceBelow
  ) {
    placeAbove = true;
  }

  let top = placeAbove
      ? triggerRect.top -
      panelRect.height -
      gap
      : triggerRect.bottom + gap;

  let left = alignStart
      ? triggerRect.left
      : triggerRect.right -
      panelRect.width;

  top = clamp(
      top,
      viewportPadding,
      Math.max(
          viewportPadding,
          window.innerHeight -
          panelRect.height -
          viewportPadding,
      ),
  );

  left = clamp(
      left,
      viewportPadding,
      Math.max(
          viewportPadding,
          window.innerWidth -
          panelRect.width -
          viewportPadding,
      ),
  );

  panelTop.value = top;
  panelLeft.value = left;
  panelReady.value = true;
}

async function openPopover(): Promise<void> {
  if (props.disabled || open.value) {
    return;
  }

  open.value = true;
  panelReady.value = false;

  emit("open");

  await nextTick();

  updatePosition();
  panel.value?.focus();
}

function closePopover(): void {
  if (!open.value) {
    return;
  }

  open.value = false;
  panelReady.value = false;

  emit("close");
}

function togglePopover(): void {
  if (open.value) {
    closePopover();
    return;
  }

  void openPopover();
}

function isInsidePopover(
    target: EventTarget | null,
): boolean {
  if (!(target instanceof Node)) {
    return false;
  }

  return Boolean(
      root.value?.contains(target) ||
      panel.value?.contains(target),
  );
}

function onDocumentPointerDown(
    event: PointerEvent,
): void {
  if (
      open.value &&
      !isInsidePopover(event.target)
  ) {
    closePopover();
  }
}

function onDocumentKeyDown(
    event: KeyboardEvent,
): void {
  if (
      open.value &&
      event.key === "Escape"
  ) {
    closePopover();
    trigger.value?.focus();
  }
}

function onViewportChange(): void {
  if (open.value) {
    closePopover();
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
      onDocumentKeyDown,
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
      onDocumentKeyDown,
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

defineExpose({
  open: openPopover,
  close: closePopover,
  toggle: togglePopover,
});
</script>

<template>
  <div
      ref="root"
      class="inline-popover"
  >
    <slot
        name="trigger"
        :open="open"
        :toggle="togglePopover"
    >
      <button
          ref="trigger"
          type="button"
          class="inline-popover__trigger"
          :class="{
          'inline-popover__trigger--active':
            open,
        }"
          :disabled="props.disabled"
          :aria-label="props.buttonLabel"
          :title="props.buttonLabel"
          :aria-expanded="open"
          :aria-controls="panelId"
          aria-haspopup="dialog"
          @click="togglePopover"
      >
        <svg
            viewBox="0 0 24 24"
            aria-hidden="true"
        >
          <path d="M4 7h16" />
          <path d="M4 12h16" />
          <path d="M4 17h16" />
        </svg>
      </button>
    </slot>

    <Teleport to="body">
      <Transition name="inline-popover-panel">
        <section
            v-if="open"
            :id="panelId"
            ref="panel"
            class="inline-popover__panel"
            :style="panelStyle"
            role="dialog"
            tabindex="-1"
            :aria-label="
            props.title ||
            props.buttonLabel
          "
        >
          <header
              v-if="
              props.title ||
              $slots.header
            "
              class="inline-popover__header"
          >
            <slot
                name="header"
                :close="closePopover"
            >
              <strong
                  class="
                  inline-popover__title
                "
              >
                {{ props.title }}
              </strong>

              <button
                  type="button"
                  class="
                  inline-popover__close
                "
                  aria-label="Close menu"
                  title="Close"
                  @click="closePopover"
              >
                <svg
                    viewBox="0 0 24 24"
                    aria-hidden="true"
                >
                  <path d="m6 6 12 12" />
                  <path d="m18 6-12 12" />
                </svg>
              </button>
            </slot>
          </header>

          <div class="inline-popover__body">
            <slot
                :close="closePopover"
            />
          </div>
        </section>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.inline-popover {
  display: inline-flex;
  flex: 0 0 auto;

  min-width: 0;

  color: rgb(var(--c-fg));
  font-family: var(--font-primary);
}

.inline-popover__trigger {
  width: 2.75rem;
  height: 2.75rem;
  flex: 0 0 auto;

  display: grid;
  place-items: center;

  padding: 0;

  color: rgb(var(--c-fg));

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-surface-raised) / 0.82),
          rgb(var(--c-surface-2) / 0.62)
      );

  border:
      1px solid
      rgb(var(--c-border) / 0.4);
  border-radius: var(--radius-md);

  box-shadow:
      inset 0 1px 0
      rgb(255 255 255 / 0.3),
      0 3px 9px
      rgb(var(--c-shadow) / 0.06);

  cursor: pointer;

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

.inline-popover__trigger:hover:not(:disabled),
.inline-popover__trigger--active {
  color:
      rgb(var(--c-primary-strong));

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-accent) / 0.22),
          rgb(var(--c-primary) / 0.13)
      );

  border-color:
      rgb(var(--c-accent) / 0.56);

  box-shadow:
      inset 0 1px 0
      rgb(255 255 255 / 0.34),
      0 4px 12px
      rgb(var(--c-shadow) / 0.09);
}

.inline-popover__trigger:active:not(
    :disabled
  ) {
  transform: scale(0.95);
}

.inline-popover__trigger:focus-visible {
  outline:
      var(--focus-ring-width)
      solid
      rgb(var(--focus-ring-color) / 0.32);

  outline-offset: 2px;
}

.inline-popover__trigger:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.inline-popover__trigger svg {
  width: 1.2rem;
  height: 1.2rem;

  fill: none;
  stroke: currentColor;
  stroke-width: 2.2;
  stroke-linecap: round;
}

/* Teleported panel */

.inline-popover__panel {
  position: fixed;
  z-index: 10000;

  width: min(
      20rem,
      calc(100vw - 1rem)
  );
  min-width: 16rem;
  max-height: min(30rem, 75dvh);
  box-sizing: border-box;

  overflow-x: hidden;
  overflow-y: auto;
  overscroll-behavior: contain;

  color: rgb(var(--c-fg));
  font-family: var(--font-primary);

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-surface-raised) / 0.99),
          rgb(var(--c-surface-2) / 0.98)
      );

  border:
      1px solid
      rgb(var(--c-accent) / 0.4);
  border-radius: var(--radius-md);

  box-shadow:
      0 20px 48px
      rgb(var(--c-shadow) / 0.28),
      0 6px 16px
      rgb(var(--c-shadow) / 0.14),
      inset 0 1px 0
      rgb(255 255 255 / 0.34);

  outline: 0;

  backdrop-filter: blur(18px);

  scrollbar-width: thin;
  scrollbar-color:
      rgb(var(--c-primary) / 0.44)
      transparent;
}

.inline-popover__header {
  position: sticky;
  top: 0;
  z-index: 1;

  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);

  padding:
      var(--space-3);

  background:
      rgb(var(--c-surface-raised) / 0.96);

  border-bottom:
      1px solid
      rgb(var(--c-border) / 0.28);

  backdrop-filter: blur(14px);
}

.inline-popover__title {
  min-width: 0;

  color: rgb(var(--c-fg-strong));

  font-size: 0.82rem;
  font-weight: 800;
  line-height: 1.3;

  overflow-wrap: anywhere;
}

.inline-popover__close {
  width: 1.9rem;
  height: 1.9rem;
  flex: 0 0 auto;

  display: grid;
  place-items: center;

  padding: 0;

  color: rgb(var(--c-muted));

  background: transparent;
  border: 1px solid transparent;
  border-radius: var(--radius-sm);

  cursor: pointer;
}

.inline-popover__close:hover {
  color: rgb(var(--c-fg-strong));

  background:
      rgb(var(--c-surface-hover) / 0.78);

  border-color:
      rgb(var(--c-border) / 0.32);
}

.inline-popover__close:focus-visible {
  outline:
      var(--focus-ring-width)
      solid
      rgb(var(--focus-ring-color) / 0.28);

  outline-offset: 1px;
}

.inline-popover__close svg {
  width: 0.95rem;
  height: 0.95rem;

  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
}

.inline-popover__body {
  min-width: 0;
  padding: var(--space-3);
}

/* Transition */

.inline-popover-panel-enter-active,
.inline-popover-panel-leave-active {
  transition:
      opacity
      var(--duration-fast)
      var(--ease-standard),
      transform
      var(--duration-fast)
      var(--ease-standard);
}

.inline-popover-panel-enter-from,
.inline-popover-panel-leave-to {
  opacity: 0;

  transform:
      translateY(0.3rem)
      scale(0.98);
}

@media (prefers-reduced-motion: reduce) {
  .inline-popover__trigger,
  .inline-popover-panel-enter-active,
  .inline-popover-panel-leave-active {
    transition: none;
  }
}
</style>