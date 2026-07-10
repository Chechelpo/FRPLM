<script lang="ts">
const WINDOW_PROMPT_BASE_Z_INDEX = 31_000;
const WINDOW_PROMPT_LAYER_STEP = 10;

let promptSequence = 0;
const openPromptStack: number[] = [];

let bodyLockCount = 0;
let originalBodyOverflow = "";

function registerPrompt(instanceId: number): void {
  openPromptStack.push(instanceId);
}

function unregisterPrompt(instanceId: number): void {
  const index = openPromptStack.lastIndexOf(instanceId);

  if (index >= 0) {
    openPromptStack.splice(index, 1);
  }
}

function isTopPrompt(instanceId: number): boolean {
  return (
      openPromptStack[openPromptStack.length - 1] ===
      instanceId
  );
}

function lockBodyScroll(): void {
  if (typeof document === "undefined") {
    return;
  }

  if (bodyLockCount === 0) {
    originalBodyOverflow =
        document.body.style.overflow;

    document.body.style.overflow = "hidden";
  }

  bodyLockCount += 1;
}

function unlockBodyScroll(): void {
  if (typeof document === "undefined") {
    return;
  }

  bodyLockCount = Math.max(0, bodyLockCount - 1);

  if (bodyLockCount === 0) {
    document.body.style.overflow =
        originalBodyOverflow;

    originalBodyOverflow = "";
  }
}
</script>

<script setup lang="ts">
import {
  computed,
  nextTick,
  onMounted,
  onUnmounted,
  ref,
  type CSSProperties,
} from "vue";

type ResizeDirection =
    | "n"
    | "s"
    | "e"
    | "w"
    | "ne"
    | "nw"
    | "se"
    | "sw";

interface ResizeState {
  direction: ResizeDirection;
  startPointerX: number;
  startPointerY: number;
  startLeft: number;
  startTop: number;
  startWidth: number;
  startHeight: number;
}

const props = withDefaults(
    defineProps<{
      title?: string;
      info?: string;
      closeOnBackdrop?: boolean;
      closeOnEscape?: boolean;
      showCloseButton?: boolean;
      resizable?: boolean;
      minWidth?: number;
      minHeight?: number;
      zIndex?: number;
    }>(),
    {
      closeOnBackdrop: true,
      closeOnEscape: true,
      showCloseButton: true,
      resizable: true,
      minWidth: 320,
      minHeight: 220,
      zIndex: undefined,
    },
);

const emit = defineEmits<{
  close: [];
}>();

const resizeDirections: readonly ResizeDirection[] = [
  "n",
  "s",
  "e",
  "w",
  "ne",
  "nw",
  "se",
  "sw",
];

const instanceId = ++promptSequence;

const generatedZIndex =
    WINDOW_PROMPT_BASE_Z_INDEX +
    instanceId * WINDOW_PROMPT_LAYER_STEP;

const resolvedZIndex = computed<number>(() => {
  return props.zIndex ?? generatedZIndex;
});

const dialogRef = ref<HTMLElement | null>(null);
const closeButtonRef =
    ref<HTMLButtonElement | null>(null);

const windowStyle = ref<CSSProperties>({});
const hasManualSize = ref(false);
const isResizing = ref(false);

let resizeState: ResizeState | null = null;
let previouslyFocusedElement: HTMLElement | null = null;

let previousUserSelect = "";
let previousCursor = "";

const titleId = `window-prompt-title-${instanceId}`;
const informationId = `window-prompt-info-${instanceId}`;

const resizeCursor: Record<ResizeDirection, string> = {
  n: "ns-resize",
  s: "ns-resize",
  e: "ew-resize",
  w: "ew-resize",
  ne: "nesw-resize",
  sw: "nesw-resize",
  nw: "nwse-resize",
  se: "nwse-resize",
};

function close(): void {
  emit("close");
}

function onBackdropClick(): void {
  if (
      props.closeOnBackdrop &&
      isTopPrompt(instanceId) &&
      !isResizing.value
  ) {
    close();
  }
}

function clamp(
    value: number,
    minimum: number,
    maximum: number,
): number {
  return Math.min(
      Math.max(value, minimum),
      Math.max(minimum, maximum),
  );
}

function includesNorth(
    direction: ResizeDirection,
): boolean {
  return direction.includes("n");
}

function includesSouth(
    direction: ResizeDirection,
): boolean {
  return direction.includes("s");
}

function includesEast(
    direction: ResizeDirection,
): boolean {
  return direction.includes("e");
}

function includesWest(
    direction: ResizeDirection,
): boolean {
  return direction.includes("w");
}

function getViewportBounds(): {
  width: number;
  height: number;
  margin: number;
} {
  return {
    width: window.innerWidth,
    height: window.innerHeight,
    margin: 8,
  };
}

function startResize(
    event: PointerEvent,
    direction: ResizeDirection,
): void {
  if (!props.resizable) {
    return;
  }

  const dialog = dialogRef.value;

  if (!dialog) {
    return;
  }

  event.preventDefault();
  event.stopPropagation();

  const rect = dialog.getBoundingClientRect();

  resizeState = {
    direction,
    startPointerX: event.clientX,
    startPointerY: event.clientY,
    startLeft: rect.left,
    startTop: rect.top,
    startWidth: rect.width,
    startHeight: rect.height,
  };

  hasManualSize.value = true;
  isResizing.value = true;

  windowStyle.value = {
    position: "fixed",
    left: `${rect.left}px`,
    top: `${rect.top}px`,
    width: `${rect.width}px`,
    height: `${rect.height}px`,
    maxWidth: "none",
    maxHeight: "none",
    margin: "0",
  };

  previousUserSelect = document.body.style.userSelect;
  previousCursor = document.body.style.cursor;

  document.body.style.userSelect = "none";
  document.body.style.cursor = resizeCursor[direction];

  window.addEventListener(
      "pointermove",
      handleResize,
      true,
  );

  window.addEventListener(
      "pointerup",
      stopResize,
      true,
  );

  window.addEventListener(
      "pointercancel",
      stopResize,
      true,
  );
}

function handleResize(event: PointerEvent): void {
  if (!resizeState) {
    return;
  }

  event.preventDefault();

  const {
    direction,
    startPointerX,
    startPointerY,
    startLeft,
    startTop,
    startWidth,
    startHeight,
  } = resizeState;

  const {
    width: viewportWidth,
    height: viewportHeight,
    margin,
  } = getViewportBounds();

  const deltaX = event.clientX - startPointerX;
  const deltaY = event.clientY - startPointerY;

  const minimumWidth = Math.min(
      props.minWidth,
      viewportWidth - margin * 2,
  );

  const minimumHeight = Math.min(
      props.minHeight,
      viewportHeight - margin * 2,
  );

  let left = startLeft;
  let top = startTop;
  let width = startWidth;
  let height = startHeight;

  if (includesEast(direction)) {
    const maximumWidth =
        viewportWidth - margin - startLeft;

    width = clamp(
        startWidth + deltaX,
        minimumWidth,
        maximumWidth,
    );
  }

  if (includesWest(direction)) {
    const right = startLeft + startWidth;
    const maximumWidth = right - margin;

    width = clamp(
        startWidth - deltaX,
        minimumWidth,
        maximumWidth,
    );

    left = right - width;
  }

  if (includesSouth(direction)) {
    const maximumHeight =
        viewportHeight - margin - startTop;

    height = clamp(
        startHeight + deltaY,
        minimumHeight,
        maximumHeight,
    );
  }

  if (includesNorth(direction)) {
    const bottom = startTop + startHeight;
    const maximumHeight = bottom - margin;

    height = clamp(
        startHeight - deltaY,
        minimumHeight,
        maximumHeight,
    );

    top = bottom - height;
  }

  left = clamp(
      left,
      margin,
      viewportWidth - margin - width,
  );

  top = clamp(
      top,
      margin,
      viewportHeight - margin - height,
  );

  windowStyle.value = {
    position: "fixed",
    left: `${left}px`,
    top: `${top}px`,
    width: `${width}px`,
    height: `${height}px`,
    maxWidth: "none",
    maxHeight: "none",
    margin: "0",
  };
}

function stopResize(): void {
  if (!isResizing.value && !resizeState) {
    return;
  }

  resizeState = null;
  isResizing.value = false;

  document.body.style.userSelect =
      previousUserSelect;

  document.body.style.cursor =
      previousCursor;

  previousUserSelect = "";
  previousCursor = "";

  window.removeEventListener(
      "pointermove",
      handleResize,
      true,
  );

  window.removeEventListener(
      "pointerup",
      stopResize,
      true,
  );

  window.removeEventListener(
      "pointercancel",
      stopResize,
      true,
  );
}

function resetWindowSize(): void {
  stopResize();

  hasManualSize.value = false;
  windowStyle.value = {};
}

function constrainWindowToViewport(): void {
  if (!hasManualSize.value) {
    return;
  }

  const dialog = dialogRef.value;

  if (!dialog) {
    return;
  }

  const rect = dialog.getBoundingClientRect();

  const {
    width: viewportWidth,
    height: viewportHeight,
    margin,
  } = getViewportBounds();

  const maximumWidth =
      viewportWidth - margin * 2;

  const maximumHeight =
      viewportHeight - margin * 2;

  const width = clamp(
      rect.width,
      Math.min(props.minWidth, maximumWidth),
      maximumWidth,
  );

  const height = clamp(
      rect.height,
      Math.min(props.minHeight, maximumHeight),
      maximumHeight,
  );

  const left = clamp(
      rect.left,
      margin,
      viewportWidth - margin - width,
  );

  const top = clamp(
      rect.top,
      margin,
      viewportHeight - margin - height,
  );

  windowStyle.value = {
    position: "fixed",
    left: `${left}px`,
    top: `${top}px`,
    width: `${width}px`,
    height: `${height}px`,
    maxWidth: "none",
    maxHeight: "none",
    margin: "0",
  };
}

function getFocusableElements(): HTMLElement[] {
  const dialog = dialogRef.value;

  if (!dialog) {
    return [];
  }

  const selector = [
    "button:not([disabled])",
    "input:not([disabled])",
    "textarea:not([disabled])",
    "select:not([disabled])",
    "a[href]",
    "[contenteditable='true']",
    "[tabindex]:not([tabindex='-1'])",
  ].join(",");

  return Array.from(
      dialog.querySelectorAll<HTMLElement>(selector),
  ).filter(element => {
    const style = window.getComputedStyle(element);

    return (
        style.display !== "none" &&
        style.visibility !== "hidden" &&
        !element.hasAttribute("hidden")
    );
  });
}

function trapFocus(event: KeyboardEvent): void {
  if (event.key !== "Tab") {
    return;
  }

  const focusableElements = getFocusableElements();

  if (focusableElements.length === 0) {
    event.preventDefault();
    dialogRef.value?.focus();
    return;
  }

  const firstElement = focusableElements[0];
  const lastElement =
      focusableElements[focusableElements.length - 1];

  const activeElement = document.activeElement;

  if (
      event.shiftKey &&
      (
          activeElement === firstElement ||
          activeElement === dialogRef.value
      )
  ) {
    event.preventDefault();
    lastElement.focus();
    return;
  }

  if (
      !event.shiftKey &&
      activeElement === lastElement
  ) {
    event.preventDefault();
    firstElement.focus();
  }
}

function onKeydown(event: KeyboardEvent): void {
  if (!isTopPrompt(instanceId)) {
    return;
  }

  if (
      props.closeOnEscape &&
      event.key === "Escape"
  ) {
    event.preventDefault();
    event.stopImmediatePropagation();

    close();
    return;
  }

  trapFocus(event);
}

onMounted(async () => {
  previouslyFocusedElement =
      document.activeElement instanceof HTMLElement
          ? document.activeElement
          : null;

  registerPrompt(instanceId);
  lockBodyScroll();

  window.addEventListener(
      "keydown",
      onKeydown,
  );

  window.addEventListener(
      "resize",
      constrainWindowToViewport,
  );

  window.visualViewport?.addEventListener(
      "resize",
      constrainWindowToViewport,
  );

  await nextTick();

  if (props.showCloseButton) {
    closeButtonRef.value?.focus();
  } else {
    dialogRef.value?.focus();
  }
});

onUnmounted(() => {
  stopResize();

  window.removeEventListener(
      "keydown",
      onKeydown,
  );

  window.removeEventListener(
      "resize",
      constrainWindowToViewport,
  );

  window.visualViewport?.removeEventListener(
      "resize",
      constrainWindowToViewport,
  );

  unregisterPrompt(instanceId);
  unlockBodyScroll();

  if (previouslyFocusedElement?.isConnected) {
    previouslyFocusedElement.focus();
  }

  previouslyFocusedElement = null;
});
</script>

<template>
  <Teleport to="body">
    <Transition name="window-prompt-transition">
      <div
          class="window-prompt-backdrop"
          :style="{
          zIndex: resolvedZIndex,
        }"
          @mousedown.self="onBackdropClick"
      >
        <section
            ref="dialogRef"
            class="window-prompt"
            :class="{
            'window-prompt--resizable': resizable,
            'window-prompt--manual-size': hasManualSize,
            'window-prompt--resizing': isResizing,
          }"
            :style="windowStyle"
            role="dialog"
            aria-modal="true"
            :aria-labelledby="
            title
              ? titleId
              : undefined
          "
            :aria-describedby="
            info
              ? informationId
              : undefined
          "
            :aria-label="
            !title
              ? 'Dialog'
              : undefined
          "
            tabindex="-1"
        >
          <div
              class="
              window-prompt__shell
              edit-box
              edit-box--accent
            "
          >
            <header
                v-if="
                $slots.header ||
                title ||
                info ||
                showCloseButton
              "
                class="
                window-prompt-header
                edit-box__header
              "
            >
              <slot
                  name="header"
                  :close="close"
              >
                <div class="edit-box__header-main">
                  <span class="edit-box__eyebrow">
                    Dialog
                  </span>

                  <h2
                      v-if="title"
                      :id="titleId"
                      class="edit-box__title"
                  >
                    {{ title }}
                  </h2>

                  <p
                      v-if="info"
                      :id="informationId"
                      class="edit-box__description"
                  >
                    {{ info }}
                  </p>
                </div>
              </slot>

              <div
                  v-if="showCloseButton"
                  class="edit-box__actions"
              >
                <button
                    ref="closeButtonRef"
                    type="button"
                    class="window-prompt-close"
                    aria-label="Close dialog"
                    title="Close"
                    @click="close"
                >
                  <svg
                      viewBox="0 0 24 24"
                      aria-hidden="true"
                  >
                    <path d="M6 6l12 12" />
                    <path d="M18 6 6 18" />
                  </svg>
                </button>
              </div>
            </header>

            <main
                class="
                window-prompt-body
                edit-box__body
              "
            >
              <slot :close="close" />
            </main>

            <footer
                v-if="$slots.footer"
                class="
                window-prompt-footer
                edit-box__footer
              "
            >
              <slot
                  name="footer"
                  :close="close"
              />
            </footer>
          </div>

          <template v-if="resizable">
            <div
                v-for="direction in resizeDirections"
                :key="direction"
                class="window-prompt-resize-handle"
                :class="
                `window-prompt-resize-handle--${direction}`
              "
                role="presentation"
                :title="
                direction === 'se'
                  ? 'Drag to resize. Double-click to reset.'
                  : undefined
              "
                @pointerdown="
                event => startResize(event, direction)
              "
                @dblclick.stop="resetWindowSize"
            />
          </template>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.window-prompt-backdrop {
  position: fixed;
  inset: 0;

  display: grid;
  place-items: center;

  box-sizing: border-box;
  padding: clamp(
      0.5rem,
      2vw,
      1.25rem
  );

  overflow: hidden;
  overscroll-behavior: contain;

  background:
      rgb(var(--c-shadow-strong) / 0.66);

  backdrop-filter: blur(7px);
}

.window-prompt {
  position: relative;

  width: min(100%, 45rem);
  min-width: min(
      20rem,
      calc(100dvw - 1rem)
  );

  min-height: min(
      13.75rem,
      calc(100dvh - 1rem)
  );

  max-width: calc(100dvw - 2.5rem);
  max-height: calc(100dvh - 2.5rem);

  display: flex;
  flex-direction: column;

  margin: auto;

  overflow: visible;

  color: rgb(var(--c-fg));
}

.window-prompt__shell {
  flex: 1 1 auto;

  width: 100%;
  min-width: 0;
  min-height: 0;
  box-sizing: border-box;

  display: flex;
  flex-direction: column;

  overflow: hidden;
}

.window-prompt--resizing {
  transition: none !important;
}

.window-prompt--resizing
.window-prompt__shell {
  box-shadow:
      0 0 0 2px
      rgb(var(--c-accent) / 0.5),
      0 24px 70px
      rgb(var(--c-shadow-strong) / 0.34);
}

.window-prompt-header,
.window-prompt-footer {
  flex: 0 0 auto;
}

.window-prompt-body {
  flex: 1 1 auto;

  min-width: 0;
  min-height: 0;

  overflow: auto;

  overscroll-behavior: contain;
  scrollbar-gutter: stable;

  scrollbar-width: thin;
  scrollbar-color:
      rgb(var(--c-primary) / 0.5)
      transparent;
}

.window-prompt-body::-webkit-scrollbar {
  width: 0.65rem;
  height: 0.65rem;
}

.window-prompt-body::-webkit-scrollbar-track {
  background: transparent;
}

.window-prompt-body::-webkit-scrollbar-thumb {
  background: rgb(var(--c-primary) / 0.44);

  border: 2px solid transparent;
  border-radius: var(--radius-round);

  background-clip: padding-box;
}

.window-prompt-body::-webkit-scrollbar-thumb:hover {
  background: rgb(var(--c-primary) / 0.65);

  border: 2px solid transparent;
  background-clip: padding-box;
}

.window-prompt-close {
  width: 2.25rem;
  height: 2.25rem;
  flex: 0 0 auto;

  display: grid;
  place-items: center;

  padding: 0;

  color: rgb(var(--c-muted));

  background:
      rgb(var(--c-surface-raised) / 0.45);

  border:
      1px solid rgb(var(--c-border) / 0.3);

  border-radius: var(--radius-sm);

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
      transform
      var(--duration-fast)
      var(--ease-standard);
}

.window-prompt-close:hover {
  color: rgb(var(--c-on-danger));

  background: rgb(var(--c-danger) / 0.88);
  border-color: rgb(var(--c-danger));
}

.window-prompt-close:active {
  transform: scale(0.92);
}

.window-prompt-close:focus-visible {
  outline:
      var(--focus-ring-width)
      solid
      rgb(var(--focus-ring-color) / 0.36);

  outline-offset: 2px;
}

.window-prompt-close svg {
  width: 1rem;
  height: 1rem;

  fill: none;
  stroke: currentColor;
  stroke-width: 2.2;
  stroke-linecap: round;
}

.window-prompt-footer {
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-2);
}

/* -------------------------------------------------------------------------- */
/* Resize handles                                                             */
/* -------------------------------------------------------------------------- */

.window-prompt-resize-handle {
  position: absolute;
  z-index: 20;

  touch-action: none;
  user-select: none;
}

.window-prompt-resize-handle::before {
  content: "";

  position: absolute;
  inset: 0;

  border-radius: var(--radius-xs);

  transition:
      background-color
      var(--duration-fast)
      var(--ease-standard);
}

.window-prompt-resize-handle:hover::before {
  background:
      rgb(var(--c-accent) / 0.16);
}

/* Horizontal edges */

.window-prompt-resize-handle--n,
.window-prompt-resize-handle--s {
  right: 1rem;
  left: 1rem;

  height: 0.75rem;

  cursor: ns-resize;
}

.window-prompt-resize-handle--n {
  top: -0.375rem;
}

.window-prompt-resize-handle--s {
  bottom: -0.375rem;
}

/* Vertical edges */

.window-prompt-resize-handle--e,
.window-prompt-resize-handle--w {
  top: 1rem;
  bottom: 1rem;

  width: 0.75rem;

  cursor: ew-resize;
}

.window-prompt-resize-handle--e {
  right: -0.375rem;
}

.window-prompt-resize-handle--w {
  left: -0.375rem;
}

/* Corners */

.window-prompt-resize-handle--ne,
.window-prompt-resize-handle--nw,
.window-prompt-resize-handle--se,
.window-prompt-resize-handle--sw {
  width: 1.25rem;
  height: 1.25rem;
}

.window-prompt-resize-handle--ne {
  top: -0.45rem;
  right: -0.45rem;

  cursor: nesw-resize;
}

.window-prompt-resize-handle--nw {
  top: -0.45rem;
  left: -0.45rem;

  cursor: nwse-resize;
}

.window-prompt-resize-handle--se {
  right: -0.45rem;
  bottom: -0.45rem;

  cursor: nwse-resize;
}

.window-prompt-resize-handle--sw {
  bottom: -0.45rem;
  left: -0.45rem;

  cursor: nesw-resize;
}

/*
 * Always-visible bottom-right grip.
 */
.window-prompt-resize-handle--se::after {
  content: "";

  position: absolute;
  right: 0.26rem;
  bottom: 0.26rem;

  width: 0.9rem;
  height: 0.9rem;

  opacity: 0.82;

  background:
      linear-gradient(
          135deg,
          transparent 0 35%,
          rgb(var(--c-accent) / 0.75) 36% 43%,
          transparent 44% 55%,
          rgb(var(--c-accent) / 0.75) 56% 63%,
          transparent 64% 75%,
          rgb(var(--c-accent) / 0.75) 76% 83%,
          transparent 84%
      );

  pointer-events: none;
}

/* -------------------------------------------------------------------------- */
/* Transition                                                                 */
/* -------------------------------------------------------------------------- */

.window-prompt-transition-enter-active,
.window-prompt-transition-leave-active {
  transition:
      opacity
      var(--duration-normal)
      var(--ease-standard);
}

.window-prompt-transition-enter-active
.window-prompt,
.window-prompt-transition-leave-active
.window-prompt {
  transition:
      opacity
      var(--duration-normal)
      var(--ease-standard),
      transform
      var(--duration-normal)
      var(--ease-standard);
}

.window-prompt-transition-enter-from,
.window-prompt-transition-leave-to {
  opacity: 0;
}

.window-prompt-transition-enter-from
.window-prompt,
.window-prompt-transition-leave-to
.window-prompt {
  opacity: 0;

  transform:
      translateY(0.75rem)
      scale(0.98);
}

/* -------------------------------------------------------------------------- */
/* Responsive                                                                 */
/* -------------------------------------------------------------------------- */

@media (max-width: 600px) {
  .window-prompt-backdrop {
    place-items: start center;
    padding: 0.5rem;
  }

  .window-prompt {
    width: 100%;
    min-width: 0;

    max-width: calc(100dvw - 1rem);
    max-height: calc(100dvh - 1rem);
  }

  .window-prompt-header {
    padding: var(--space-3);
  }

  .window-prompt-body {
    padding: var(--space-3);
  }

  .window-prompt-footer {
    padding: var(--space-3);
  }

  .window-prompt-resize-handle {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .window-prompt-close,
  .window-prompt-resize-handle::before,
  .window-prompt-transition-enter-active,
  .window-prompt-transition-leave-active,
  .window-prompt-transition-enter-active
  .window-prompt,
  .window-prompt-transition-leave-active
  .window-prompt {
    transition: none;
  }
}
</style>