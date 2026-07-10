<script setup lang="ts" generic="T extends string | number | symbol">
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
  watch,
  type CSSProperties,
} from "vue";

type LabelSource<T extends string | number | symbol> =
    | ReadonlyMap<T, string>
    | Partial<Record<T, string>>;

type MenuPlacement = "above" | "below" | "overlay";

interface MenuLayoutConfig {
  readonly viewportMarginPx: number;
  readonly controlGapPx: number;
  readonly preferredMaxHeightPx: number;
  readonly minimumUsefulHeightPx: number;
  readonly minimumWidthPx: number;
  readonly estimatedOptionHeightPx: number;
  readonly estimatedVerticalPaddingPx: number;
}

interface ViewportMetrics {
  readonly left: number;
  readonly top: number;
  readonly width: number;
  readonly height: number;
  readonly right: number;
  readonly bottom: number;
}

const MENU_LAYOUT = {
  viewportMarginPx: 12,
  controlGapPx: 6,
  preferredMaxHeightPx: 320,
  minimumUsefulHeightPx: 100,
  minimumWidthPx: 220,
  estimatedOptionHeightPx: 42,
  estimatedVerticalPaddingPx: 8,
} as const satisfies MenuLayoutConfig;

const props = defineProps<{
  value: T | null;
  possible_values: readonly T[];
  labels?: LabelSource<T>;
  placeholder?: string;
}>();

const emit = defineEmits<{
  edit: [value: T];
}>();

const rootRef = ref<HTMLElement | null>(null);
const controlRef = ref<HTMLElement | null>(null);
const inputRef = ref<HTMLInputElement | null>(null);
const menuRef = ref<HTMLElement | null>(null);

const open = ref(false);
const search = ref("");
const highlightedIndex = ref(0);

const menuPlacement = ref<MenuPlacement>("below");
const menuStyle = ref<CSSProperties>({});

const instanceId = createInstanceId();
const listboxId = `${instanceId}-listbox`;

let blurTimer: number | null = null;
let positionFrame: number | null = null;
let resizeObserver: ResizeObserver | null = null;

function createInstanceId(): string {
  if (
      typeof crypto !== "undefined" &&
      typeof crypto.randomUUID === "function"
  ) {
    return `enum-input-${crypto.randomUUID()}`;
  }

  return `enum-input-${Math.random()
      .toString(36)
      .slice(2, 10)}`;
}

function isMap(
    labels: LabelSource<T>,
): labels is ReadonlyMap<T, string> {
  return labels instanceof Map;
}

function labelOf(value: T): string {
  if (!props.labels) {
    return String(value);
  }

  if (isMap(props.labels)) {
    return props.labels.get(value) ?? String(value);
  }

  return props.labels[value] ?? String(value);
}

function normalize(value: string): string {
  return value
      .normalize("NFD")
      .replace(/\p{Diacritic}/gu, "")
      .toLocaleLowerCase()
      .trim();
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

function getViewportMetrics(): ViewportMetrics {
  const visualViewport = window.visualViewport;

  const left = visualViewport?.offsetLeft ?? 0;
  const top = visualViewport?.offsetTop ?? 0;
  const width = visualViewport?.width ?? window.innerWidth;
  const height = visualViewport?.height ?? window.innerHeight;

  return {
    left,
    top,
    width,
    height,
    right: left + width,
    bottom: top + height,
  };
}

const selectedLabel = computed<string>(() => {
  return props.value === null
      ? ""
      : labelOf(props.value);
});

const filteredValues = computed<readonly T[]>(() => {
  const terms = normalize(search.value)
      .split(/\s+/)
      .filter(Boolean);

  if (terms.length === 0) {
    return props.possible_values;
  }

  return props.possible_values.filter(value => {
    const searchableText = normalize(
        `${labelOf(value)} ${String(value)}`,
    );

    return terms.every(term =>
        searchableText.includes(term),
    );
  });
});

const activeDescendant = computed<string | undefined>(() => {
  if (
      !open.value ||
      filteredValues.value.length === 0
  ) {
    return undefined;
  }

  return `${instanceId}-option-${highlightedIndex.value}`;
});

watch(
    selectedLabel,
    value => {
      if (!open.value) {
        search.value = value;
      }
    },
    {
      immediate: true,
    },
);

watch(
    filteredValues,
    values => {
      if (values.length === 0) {
        highlightedIndex.value = 0;
      } else if (
          highlightedIndex.value >= values.length
      ) {
        highlightedIndex.value = values.length - 1;
      }

      if (open.value) {
        void nextTick(() => {
          scheduleMenuPositionUpdate();
          scrollHighlightedIntoView();
        });
      }
    },
);

watch(open, async isOpen => {
  if (!isOpen) {
    return;
  }

  await nextTick();

  scheduleMenuPositionUpdate();
  scrollHighlightedIntoView();
});

function getMenuContentHeight(): number {
  const measuredHeight = menuRef.value?.scrollHeight;

  if (
      measuredHeight !== undefined &&
      measuredHeight > 0
  ) {
    return measuredHeight;
  }

  const estimatedOptionsHeight =
      filteredValues.value.length *
      MENU_LAYOUT.estimatedOptionHeightPx;

  return Math.max(
      MENU_LAYOUT.minimumUsefulHeightPx,
      estimatedOptionsHeight +
      MENU_LAYOUT.estimatedVerticalPaddingPx,
  );
}

function scheduleMenuPositionUpdate(): void {
  if (!open.value) {
    return;
  }

  if (positionFrame !== null) {
    cancelAnimationFrame(positionFrame);
  }

  positionFrame = requestAnimationFrame(() => {
    positionFrame = null;
    updateMenuPosition();
  });
}

function updateMenuPosition(): void {
  const control = controlRef.value;

  if (!control || !open.value) {
    return;
  }

  const rect = control.getBoundingClientRect();
  const viewport = getViewportMetrics();

  const controlLeft = rect.left + viewport.left;
  const controlTop = rect.top + viewport.top;
  const controlBottom = rect.bottom + viewport.top;

  const availableViewportWidth = Math.max(
      0,
      viewport.width -
      MENU_LAYOUT.viewportMarginPx * 2,
  );

  const menuWidth = Math.min(
      Math.max(
          rect.width,
          MENU_LAYOUT.minimumWidthPx,
      ),
      availableViewportWidth,
  );

  const menuLeft = clamp(
      controlLeft,
      viewport.left + MENU_LAYOUT.viewportMarginPx,
      viewport.right -
      MENU_LAYOUT.viewportMarginPx -
      menuWidth,
  );

  const spaceBelow =
      viewport.bottom -
      controlBottom -
      MENU_LAYOUT.controlGapPx -
      MENU_LAYOUT.viewportMarginPx;

  const spaceAbove =
      controlTop -
      viewport.top -
      MENU_LAYOUT.controlGapPx -
      MENU_LAYOUT.viewportMarginPx;

  const neitherSideHasEnoughSpace =
      spaceBelow < MENU_LAYOUT.minimumUsefulHeightPx &&
      spaceAbove < MENU_LAYOUT.minimumUsefulHeightPx;

  if (neitherSideHasEnoughSpace) {
    const overlayHeight = Math.max(
        0,
        viewport.height -
        MENU_LAYOUT.viewportMarginPx * 2,
    );

    menuPlacement.value = "overlay";

    menuStyle.value = {
      top: `${
          viewport.top +
          MENU_LAYOUT.viewportMarginPx
      }px`,
      left: `${menuLeft}px`,
      width: `${menuWidth}px`,
      maxHeight: `${overlayHeight}px`,
    };

    return;
  }

  const shouldOpenBelow =
      spaceBelow >= MENU_LAYOUT.minimumUsefulHeightPx ||
      spaceBelow >= spaceAbove;

  if (shouldOpenBelow) {
    const maximumHeight = Math.max(
        0,
        Math.min(
            MENU_LAYOUT.preferredMaxHeightPx,
            spaceBelow,
        ),
    );

    menuPlacement.value = "below";

    menuStyle.value = {
      top: `${
          controlBottom +
          MENU_LAYOUT.controlGapPx
      }px`,
      left: `${menuLeft}px`,
      width: `${menuWidth}px`,
      maxHeight: `${maximumHeight}px`,
    };

    return;
  }

  const maximumHeight = Math.max(
      0,
      Math.min(
          MENU_LAYOUT.preferredMaxHeightPx,
          spaceAbove,
      ),
  );

  const renderedHeight = Math.min(
      getMenuContentHeight(),
      maximumHeight,
  );

  const menuTop =
      controlTop -
      MENU_LAYOUT.controlGapPx -
      renderedHeight;

  menuPlacement.value = "above";

  menuStyle.value = {
    top: `${Math.max(
        viewport.top + MENU_LAYOUT.viewportMarginPx,
        menuTop,
    )}px`,
    left: `${menuLeft}px`,
    width: `${menuWidth}px`,
    maxHeight: `${maximumHeight}px`,
  };
}

function scrollHighlightedIntoView(): void {
  void nextTick(() => {
    const selector =
        `[data-option-index="${highlightedIndex.value}"]`;

    const activeOption =
        menuRef.value?.querySelector<HTMLElement>(selector);

    activeOption?.scrollIntoView({
      block: "nearest",
      inline: "nearest",
    });
  });
}

function clearBlurTimer(): void {
  if (blurTimer === null) {
    return;
  }

  window.clearTimeout(blurTimer);
  blurTimer = null;
}

function showOptions(): void {
  clearBlurTimer();

  open.value = true;
  search.value = "";

  const selectedIndex =
      props.value === null
          ? -1
          : props.possible_values.findIndex(option =>
              Object.is(option, props.value),
          );

  highlightedIndex.value =
      selectedIndex >= 0
          ? selectedIndex
          : 0;

  void nextTick(() => {
    controlRef.value?.scrollIntoView({
      block: "nearest",
      inline: "nearest",
    });

    scheduleMenuPositionUpdate();
    scrollHighlightedIntoView();
  });
}

function closeOptions(): void {
  open.value = false;
  search.value = selectedLabel.value;
  highlightedIndex.value = 0;
}

function selectValue(value: T): void {
  emit("edit", value);

  open.value = false;
  search.value = labelOf(value);
  highlightedIndex.value = 0;

  void nextTick(() => {
    inputRef.value?.focus();
    inputRef.value?.select();
  });
}

function moveHighlight(delta: number): void {
  const optionCount = filteredValues.value.length;

  if (optionCount === 0) {
    return;
  }

  highlightedIndex.value =
      (
          highlightedIndex.value +
          delta +
          optionCount
      ) % optionCount;

  scrollHighlightedIntoView();
}

function selectHighlighted(): void {
  const value =
      filteredValues.value[highlightedIndex.value];

  if (value !== undefined) {
    selectValue(value);
  }
}

function handleBlur(): void {
  clearBlurTimer();

  blurTimer = window.setTimeout(() => {
    blurTimer = null;

    const activeElement = document.activeElement;

    const remainsInsideControl =
        activeElement !== null &&
        rootRef.value?.contains(activeElement);

    const remainsInsideMenu =
        activeElement !== null &&
        menuRef.value?.contains(activeElement);

    if (
        !remainsInsideControl &&
        !remainsInsideMenu
    ) {
      closeOptions();
    }
  }, 100);
}

function handleKeydown(event: KeyboardEvent): void {
  switch (event.key) {
    case "ArrowDown": {
      event.preventDefault();

      if (!open.value) {
        showOptions();
      } else {
        moveHighlight(1);
      }

      break;
    }

    case "ArrowUp": {
      event.preventDefault();

      if (!open.value) {
        showOptions();
      } else {
        moveHighlight(-1);
      }

      break;
    }

    case "Home": {
      if (!open.value) {
        break;
      }

      event.preventDefault();

      highlightedIndex.value = 0;
      scrollHighlightedIntoView();

      break;
    }

    case "End": {
      if (!open.value) {
        break;
      }

      event.preventDefault();

      highlightedIndex.value = Math.max(
          0,
          filteredValues.value.length - 1,
      );

      scrollHighlightedIntoView();

      break;
    }

    case "Enter": {
      if (!open.value) {
        break;
      }

      event.preventDefault();
      event.stopPropagation();

      selectHighlighted();

      break;
    }

    case "Escape": {
      if (!open.value) {
        break;
      }

      /*
       * Prevent the Escape event from reaching WindowPrompt or another
       * underlying modal. Only this options menu should close.
       */
      event.preventDefault();
      event.stopPropagation();

      closeOptions();
      inputRef.value?.focus();

      break;
    }

    case "Tab": {
      closeOptions();
      break;
    }
  }
}

function handleDocumentPointerDown(
    event: PointerEvent,
): void {
  const target = event.target;

  if (!(target instanceof Node)) {
    return;
  }

  const clickedInsideControl =
      rootRef.value?.contains(target) ?? false;

  const clickedInsideMenu =
      menuRef.value?.contains(target) ?? false;

  if (
      clickedInsideControl ||
      clickedInsideMenu
  ) {
    return;
  }

  closeOptions();
}

onMounted(() => {
  document.addEventListener(
      "pointerdown",
      handleDocumentPointerDown,
      true,
  );

  window.addEventListener(
      "resize",
      scheduleMenuPositionUpdate,
  );

  window.addEventListener(
      "scroll",
      scheduleMenuPositionUpdate,
      true,
  );

  window.visualViewport?.addEventListener(
      "resize",
      scheduleMenuPositionUpdate,
  );

  window.visualViewport?.addEventListener(
      "scroll",
      scheduleMenuPositionUpdate,
  );

  if (
      typeof ResizeObserver !== "undefined" &&
      controlRef.value
  ) {
    resizeObserver = new ResizeObserver(
        scheduleMenuPositionUpdate,
    );

    resizeObserver.observe(controlRef.value);
  }
});

onBeforeUnmount(() => {
  clearBlurTimer();

  if (positionFrame !== null) {
    cancelAnimationFrame(positionFrame);
    positionFrame = null;
  }

  document.removeEventListener(
      "pointerdown",
      handleDocumentPointerDown,
      true,
  );

  window.removeEventListener(
      "resize",
      scheduleMenuPositionUpdate,
  );

  window.removeEventListener(
      "scroll",
      scheduleMenuPositionUpdate,
      true,
  );

  window.visualViewport?.removeEventListener(
      "resize",
      scheduleMenuPositionUpdate,
  );

  window.visualViewport?.removeEventListener(
      "scroll",
      scheduleMenuPositionUpdate,
  );

  resizeObserver?.disconnect();
  resizeObserver = null;
});
</script>

<template>
  <div
      ref="rootRef"
      class="enum-input"
      :class="{
      'enum-input--open': open,
    }"
  >
    <div
        ref="controlRef"
        class="enum-input__control"
        :class="{
        'enum-input__control--open': open,
      }"
        @click="inputRef?.focus()"
    >
      <svg
          class="enum-input__search-icon"
          viewBox="0 0 24 24"
          aria-hidden="true"
      >
        <circle
            cx="11"
            cy="11"
            r="7"
        />

        <path d="m20 20-4-4" />
      </svg>

      <input
          ref="inputRef"
          v-model="search"
          class="enum-input__field"
          type="text"
          role="combobox"
          aria-autocomplete="list"
          aria-haspopup="listbox"
          :aria-expanded="open"
          :aria-controls="open ? listboxId : undefined"
          :aria-activedescendant="activeDescendant"
          autocomplete="off"
          spellcheck="false"
          :placeholder="placeholder ?? 'Select…'"
          @focus="showOptions"
          @blur="handleBlur"
          @keydown="handleKeydown"
      />

      <svg
          class="enum-input__chevron"
          :class="{
          'enum-input__chevron--open': open,
        }"
          viewBox="0 0 24 24"
          aria-hidden="true"
      >
        <path d="m7 10 5 5 5-5" />
      </svg>
    </div>

    <Teleport to="body">
      <Transition name="enum-menu">
        <div
            v-if="open"
            :id="listboxId"
            ref="menuRef"
            class="enum-input__options"
            :class="{
            'enum-input__options--above':
              menuPlacement === 'above',

            'enum-input__options--below':
              menuPlacement === 'below',

            'enum-input__options--overlay':
              menuPlacement === 'overlay',
          }"
            :style="menuStyle"
            role="listbox"
            :aria-label="placeholder ?? 'Select a value'"
        >
          <button
              v-for="(option, index) in filteredValues"
              :id="`${instanceId}-option-${index}`"
              :key="`${typeof option}:${String(option)}:${index}`"
              :data-option-index="index"
              type="button"
              class="enum-input__option"
              :class="{
              'enum-input__option--selected':
                Object.is(option, value),

              'enum-input__option--highlighted':
                index === highlightedIndex,
            }"
              role="option"
              :aria-selected="Object.is(option, value)"
              @mouseenter="highlightedIndex = index"
              @mousedown.prevent
              @click="selectValue(option)"
          >
            <span class="enum-input__option-indicator">
              <svg
                  v-if="Object.is(option, value)"
                  viewBox="0 0 24 24"
                  aria-hidden="true"
              >
                <path d="M20 6 9 17l-5-5" />
              </svg>
            </span>

            <span class="enum-input__option-label">
              {{ labelOf(option) }}
            </span>

            <span
                v-if="String(option) !== labelOf(option)"
                class="enum-input__option-value"
            >
              {{ String(option) }}
            </span>
          </button>

          <div
              v-if="filteredValues.length === 0"
              class="enum-input__empty"
              role="status"
          >
            <svg
                viewBox="0 0 24 24"
                aria-hidden="true"
            >
              <circle
                  cx="11"
                  cy="11"
                  r="7"
              />

              <path d="m20 20-4-4" />
            </svg>

            <div>
              <strong>No matches</strong>

              <span>
                No available value matches
                “{{ search.trim() }}”.
              </span>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.enum-input {
  position: relative;

  width: 100%;
  min-width: 0;

  color: rgb(var(--c-fg));
  font-family: var(--font-primary);
}

/* -------------------------------------------------------------------------- */
/* Input control                                                              */
/* -------------------------------------------------------------------------- */

.enum-input__control {
  width: 100%;
  min-width: 0;
  min-height: 2.5rem;
  box-sizing: border-box;

  display: flex;
  align-items: center;
  gap: var(--space-2);

  padding: 0.35rem 0.6rem;

  color: rgb(var(--c-fg));

  background: rgb(var(--c-surface-raised) / 0.68);
  border: 1px solid rgb(var(--c-border) / 0.38);
  border-radius: var(--radius-sm);

  box-shadow:
      inset 0 1px 0 rgb(var(--c-surface-raised) / 0.45),
      0 2px 7px rgb(var(--c-shadow) / 0.05);

  cursor: text;

  transition:
      background-color var(--duration-fast) var(--ease-standard),
      border-color var(--duration-fast) var(--ease-standard),
      box-shadow var(--duration-fast) var(--ease-standard);
}

.enum-input__control:hover {
  background: rgb(var(--c-surface-hover) / 0.78);
  border-color: rgb(var(--c-primary) / 0.5);
}

.enum-input__control:focus-within,
.enum-input__control--open {
  background: rgb(var(--c-surface-raised) / 0.95);
  border-color: rgb(var(--c-accent) / 0.76);

  box-shadow:
      0 0 0 var(--focus-ring-width)
      rgb(var(--focus-ring-color) / 0.16),
      0 4px 12px rgb(var(--c-shadow) / 0.08);
}

.enum-input__field {
  flex: 1 1 auto;

  width: 100%;
  min-width: 0;
  height: 1.8rem;
  box-sizing: border-box;

  padding: 0;

  color: rgb(var(--c-fg-strong));

  background: transparent;
  border: 0;
  outline: 0;

  font: inherit;
  font-size: 0.88rem;
  font-weight: 600;
}

.enum-input__field::placeholder {
  color: rgb(var(--c-muted) / 0.76);
  font-weight: 500;
}

.enum-input__search-icon,
.enum-input__chevron {
  width: 1.05rem;
  height: 1.05rem;
  flex: 0 0 auto;

  fill: none;
  stroke: rgb(var(--c-muted));
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;

  transition:
      stroke var(--duration-fast) var(--ease-standard),
      transform var(--duration-fast) var(--ease-standard);
}

.enum-input__control:focus-within
.enum-input__search-icon {
  stroke: rgb(var(--c-primary));
}

.enum-input__chevron--open {
  stroke: rgb(var(--c-primary));
  transform: rotate(180deg);
}

/* -------------------------------------------------------------------------- */
/* Viewport-aware options menu                                                */
/* -------------------------------------------------------------------------- */

.enum-input__options {
  position: fixed;

  /*
   * This menu is teleported to body and must appear above:
   *
   * - Prompt section modal: approximately 30000
   * - WindowPrompt: approximately 31000+
   * - Expanded LongTextBox: approximately 50000
   */
  z-index: var(--z-critical-popover, 60000);

  box-sizing: border-box;

  display: flex;
  flex-direction: column;
  gap: 0.2rem;

  padding: var(--space-1);

  overflow-x: hidden;
  overflow-y: auto;
  overscroll-behavior: contain;

  color: rgb(var(--c-fg-strong));

  background: rgb(var(--c-surface-raised));
  border: 1px solid rgb(var(--c-border-strong) / 0.58);
  border-radius: var(--radius-sm);

  box-shadow:
      0 20px 45px rgb(var(--c-shadow-strong) / 0.22),
      0 7px 18px rgb(var(--c-shadow) / 0.15),
      inset 0 1px 0 rgb(var(--c-surface-raised) / 0.5);

  backdrop-filter: blur(14px);

  scrollbar-width: thin;
  scrollbar-color:
      rgb(var(--c-primary) / 0.5)
      transparent;

  contain: layout paint;
  isolation: isolate;
}

.enum-input__options--below {
  transform-origin: top;
}

.enum-input__options--above {
  transform-origin: bottom;
}

.enum-input__options--overlay {
  transform-origin: center;

  border-color: rgb(var(--c-accent) / 0.58);

  box-shadow:
      0 24px 60px rgb(var(--c-shadow-strong) / 0.28),
      0 0 0 1px rgb(var(--c-accent) / 0.14);
}

.enum-input__options::-webkit-scrollbar {
  width: 0.55rem;
}

.enum-input__options::-webkit-scrollbar-track {
  background: transparent;
}

.enum-input__options::-webkit-scrollbar-thumb {
  background: rgb(var(--c-primary) / 0.46);
  border: 2px solid transparent;
  border-radius: var(--radius-round);
  background-clip: padding-box;
}

/* -------------------------------------------------------------------------- */
/* Options                                                                    */
/* -------------------------------------------------------------------------- */

.enum-input__option {
  width: 100%;
  min-width: 0;
  min-height: 2.5rem;
  box-sizing: border-box;

  display: grid;
  grid-template-columns:
    1.2rem
    minmax(0, 1fr)
    auto;

  align-items: center;
  gap: var(--space-2);

  padding: 0.5rem 0.65rem;

  color: rgb(var(--c-fg-strong));

  background: rgb(var(--c-surface-hover) / 0.72);
  border: 1px solid rgb(var(--c-border-strong) / 0.3);
  border-radius: var(--radius-xs);

  box-shadow:
      inset 0 1px 0 rgb(var(--c-surface-raised) / 0.22);

  text-align: left;
  font: inherit;
  font-size: 0.84rem;
  font-weight: 650;

  cursor: pointer;

  transition:
      color var(--duration-fast) var(--ease-standard),
      background-color var(--duration-fast) var(--ease-standard),
      border-color var(--duration-fast) var(--ease-standard),
      box-shadow var(--duration-fast) var(--ease-standard),
      transform var(--duration-fast) var(--ease-standard);
}

.enum-input__option:hover,
.enum-input__option--highlighted {
  color: rgb(var(--c-on-accent));

  background: rgb(var(--c-accent) / 0.28);
  border-color: rgb(var(--c-accent) / 0.55);

  box-shadow:
      0 3px 9px rgb(var(--c-shadow) / 0.11),
      inset 0 1px 0 rgb(var(--c-surface-raised) / 0.12);

  transform: translateX(2px);
}

.enum-input__option--selected {
  color: rgb(var(--c-primary-strong));

  background: rgb(var(--c-accent) / 0.16);
  border-color: rgb(var(--c-accent) / 0.4);

  font-weight: 800;
}

.enum-input__option--selected.enum-input__option--highlighted,
.enum-input__option--selected:hover {
  color: rgb(var(--c-on-accent));
  background: rgb(var(--c-accent) / 0.32);
  border-color: rgb(var(--c-accent) / 0.64);
}

.enum-input__option-indicator {
  width: 1rem;
  height: 1rem;

  display: grid;
  place-items: center;

  color: currentColor;
}

.enum-input__option-indicator svg {
  width: 0.95rem;
  height: 0.95rem;

  fill: none;
  stroke: currentColor;
  stroke-width: 2.3;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.enum-input__option-label {
  min-width: 0;

  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.enum-input__option-value {
  max-width: 10rem;

  padding: 0.18rem 0.4rem;

  color: rgb(var(--c-muted));

  background: rgb(var(--c-surface) / 0.7);
  border: 1px solid rgb(var(--c-border) / 0.24);
  border-radius: var(--radius-round);

  font-family: var(--font-monospace);
  font-size: 0.68rem;
  font-weight: 600;

  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.enum-input__option:hover
.enum-input__option-value,
.enum-input__option--highlighted
.enum-input__option-value {
  color: currentColor;

  background: rgb(var(--c-surface-raised) / 0.2);
  border-color: rgb(var(--c-accent) / 0.32);
}

/* -------------------------------------------------------------------------- */
/* Empty state                                                                */
/* -------------------------------------------------------------------------- */

.enum-input__empty {
  min-height: 5rem;

  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-3);

  padding: var(--space-4);

  color: rgb(var(--c-muted));

  background: rgb(var(--c-surface-hover) / 0.48);
  border: 1px dashed rgb(var(--c-border-strong) / 0.34);
  border-radius: var(--radius-xs);
}

.enum-input__empty svg {
  width: 1.35rem;
  height: 1.35rem;
  flex: 0 0 auto;

  fill: none;
  stroke: rgb(var(--c-primary));
  stroke-width: 1.9;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.enum-input__empty div {
  min-width: 0;

  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.enum-input__empty strong {
  color: rgb(var(--c-fg-strong));

  font-size: 0.82rem;
}

.enum-input__empty span {
  font-size: 0.74rem;
  line-height: 1.4;

  overflow-wrap: anywhere;
}

/* -------------------------------------------------------------------------- */
/* Transition                                                                 */
/* -------------------------------------------------------------------------- */

.enum-menu-enter-active,
.enum-menu-leave-active {
  transition:
      opacity var(--duration-fast) var(--ease-standard),
      transform var(--duration-fast) var(--ease-standard);
}

.enum-menu-enter-from,
.enum-menu-leave-to {
  opacity: 0;
  transform: scale(0.97);
}

.enum-input__options--below.enum-menu-enter-from,
.enum-input__options--below.enum-menu-leave-to {
  transform: translateY(-0.25rem) scale(0.98);
}

.enum-input__options--above.enum-menu-enter-from,
.enum-input__options--above.enum-menu-leave-to {
  transform: translateY(0.25rem) scale(0.98);
}

@media (max-width: 480px) {
  .enum-input__option {
    grid-template-columns:
      1.1rem
      minmax(0, 1fr);
  }

  .enum-input__option-value {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .enum-input__control,
  .enum-input__search-icon,
  .enum-input__chevron,
  .enum-input__option,
  .enum-menu-enter-active,
  .enum-menu-leave-active {
    transition: none;
  }
}
</style>