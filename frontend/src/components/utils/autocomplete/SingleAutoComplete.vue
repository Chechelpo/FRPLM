<script setup lang="ts">
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
  watch,
  type CSSProperties,
} from "vue";

interface Props {
  suggestions: string[];
  modelValue?: string | null;
  placeholder?: string;
  allowCustom?: boolean;
  disabled?: boolean;
  clearable?: boolean;
}

type MenuItem = {
  type: "suggestion" | "custom";
  value: string;
};

type MenuPlacement = "above" | "below" | "overlay";

const props = withDefaults(defineProps<Props>(), {
  modelValue: null,
  placeholder: "Type to select…",
  allowCustom: false,
  disabled: false,
  clearable: true,
});

const emit = defineEmits<{
  (event: "update:modelValue", value: string | null): void;
  (event: "select", value: string): void;
  (event: "clear"): void;
}>();

const inputText = ref("");
const isFocused = ref(false);
const highlightedIndex = ref(0);

const controlRef = ref<HTMLElement | null>(null);
const inputRef = ref<HTMLInputElement | null>(null);
const menuRef = ref<HTMLElement | null>(null);

const menuPlacement = ref<MenuPlacement>("below");
const menuStyle = ref<CSSProperties>({});

const instanceId = `autocomplete-${Math.random()
    .toString(36)
    .slice(2, 10)}`;

const listboxId = `${instanceId}-listbox`;

const normalizedQuery = computed(() =>
    inputText.value.trim().toLocaleLowerCase(),
);

const filteredSuggestions = computed<string[]>(() => {
  const query = normalizedQuery.value;

  const availableSuggestions = props.suggestions.filter(
      value => value !== props.modelValue,
  );

  if (!query) {
    return availableSuggestions.slice(0, 8);
  }

  return availableSuggestions
      .filter(value =>
          value.toLocaleLowerCase().includes(query),
      )
      .slice(0, 8);
});

const exactSuggestion = computed<string | null>(() => {
  const query = normalizedQuery.value;

  if (!query) {
    return null;
  }

  return (
      props.suggestions.find(
          value =>
              value.toLocaleLowerCase() === query,
      ) ?? null
  );
});

const canApplyCustom = computed(() => {
  const value = inputText.value.trim();

  return (
      props.allowCustom &&
      value.length > 0 &&
      exactSuggestion.value === null
  );
});

const menuItems = computed<MenuItem[]>(() => {
  const items = filteredSuggestions.value.map<MenuItem>(
      value => ({
        type: "suggestion",
        value,
      }),
  );

  if (canApplyCustom.value) {
    items.push({
      type: "custom",
      value: inputText.value.trim(),
    });
  }

  return items;
});

const menuOpen = computed(() => {
  if (props.disabled) {
    return false;
  }

  if (!isFocused.value) {
    return false;
  }

  if (props.modelValue) {
    return false;
  }

  return menuItems.value.length > 0;
});

const activeDescendant = computed<
    string | undefined
>(() => {
  if (!menuOpen.value) {
    return undefined;
  }

  const activeItem =
      menuItems.value[highlightedIndex.value];

  if (!activeItem) {
    return undefined;
  }

  return `${instanceId}-option-${highlightedIndex.value}`;
});

watch(
    () => props.modelValue,
    value => {
      if (value !== null && value !== undefined) {
        inputText.value = "";
      }
    },
);

watch(menuItems, items => {
  if (items.length === 0) {
    highlightedIndex.value = 0;
    return;
  }

  if (highlightedIndex.value >= items.length) {
    highlightedIndex.value = items.length - 1;
  }

  if (menuOpen.value) {
    void nextTick(() => {
      updateMenuPosition();
      scrollHighlightedIntoView();
    });
  }
});

watch(menuOpen, async open => {
  if (!open) {
    return;
  }

  await nextTick();

  updateMenuPosition();
  scrollHighlightedIntoView();
});

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

function updateMenuPosition(): void {
  const control = controlRef.value;

  if (!control || !menuOpen.value) {
    return;
  }

  const viewport = window.visualViewport;

  const viewportWidth =
      viewport?.width ?? window.innerWidth;

  const viewportHeight =
      viewport?.height ?? window.innerHeight;

  const viewportLeft =
      viewport?.offsetLeft ?? 0;

  const viewportTop =
      viewport?.offsetTop ?? 0;

  const viewportRight =
      viewportLeft + viewportWidth;

  const viewportBottom =
      viewportTop + viewportHeight;

  const rect = control.getBoundingClientRect();

  const viewportMargin = 12;
  const controlGap = 6;
  const preferredMaximumHeight = 320;
  const minimumUsefulHeight = 96;

  const availableWidth = Math.max(
      0,
      viewportWidth - viewportMargin * 2,
  );

  const menuWidth = Math.min(
      Math.max(rect.width, 240),
      availableWidth,
  );

  const menuLeft = clamp(
      rect.left,
      viewportLeft + viewportMargin,
      viewportRight - viewportMargin - menuWidth,
  );

  const spaceBelow =
      viewportBottom -
      rect.bottom -
      controlGap -
      viewportMargin;

  const spaceAbove =
      rect.top -
      viewportTop -
      controlGap -
      viewportMargin;

  if (
      spaceBelow < minimumUsefulHeight &&
      spaceAbove < minimumUsefulHeight
  ) {
    menuPlacement.value = "overlay";

    menuStyle.value = {
      top: `${viewportTop + viewportMargin}px`,
      right: "auto",
      bottom: "auto",
      left: `${menuLeft}px`,
      width: `${menuWidth}px`,
      maxHeight: `${Math.max(
          0,
          viewportHeight - viewportMargin * 2,
      )}px`,
    };

    return;
  }

  const shouldOpenBelow =
      spaceBelow >= minimumUsefulHeight ||
      spaceBelow >= spaceAbove;

  if (shouldOpenBelow) {
    menuPlacement.value = "below";

    menuStyle.value = {
      top: `${Math.max(
          viewportTop + viewportMargin,
          rect.bottom + controlGap,
      )}px`,
      right: "auto",
      bottom: "auto",
      left: `${menuLeft}px`,
      width: `${menuWidth}px`,
      maxHeight: `${Math.max(
          minimumUsefulHeight,
          Math.min(
              preferredMaximumHeight,
              spaceBelow,
          ),
      )}px`,
    };

    return;
  }

  const maximumHeight = Math.max(
      minimumUsefulHeight,
      Math.min(
          preferredMaximumHeight,
          spaceAbove,
      ),
  );

  menuPlacement.value = "above";

  menuStyle.value = {
    top: "auto",
    right: "auto",
    bottom: `${Math.max(
        viewportMargin,
        viewportBottom - rect.top + controlGap,
    )}px`,
    left: `${menuLeft}px`,
    width: `${menuWidth}px`,
    maxHeight: `${maximumHeight}px`,
  };
}

function scrollHighlightedIntoView(): void {
  void nextTick(() => {
    const activeOption =
        menuRef.value?.querySelector<HTMLElement>(
            `[data-option-index="${highlightedIndex.value}"]`,
        );

    activeOption?.scrollIntoView({
      block: "nearest",
    });
  });
}

function ensureControlIsVisible(): void {
  controlRef.value?.scrollIntoView({
    block: "nearest",
    inline: "nearest",
  });
}

function focusInput(): void {
  if (props.disabled) {
    return;
  }

  inputRef.value?.focus();
}

function setValue(
    value: string,
    blurAfterSelection = true,
): void {
  const normalized = value.trim();

  if (!normalized || props.disabled) {
    return;
  }

  emit("update:modelValue", normalized);
  emit("select", normalized);

  inputText.value = "";
  highlightedIndex.value = 0;

  if (blurAfterSelection) {
    void nextTick(() => {
      inputRef.value?.blur();
    });
  }
}

function clearValue(): void {
  if (props.disabled) {
    return;
  }

  emit("update:modelValue", null);
  emit("clear");

  inputText.value = "";
  highlightedIndex.value = 0;

  void nextTick(() => {
    inputRef.value?.focus();
  });
}

function applyCurrent(
    blurAfterSelection = true,
): void {
  if (props.disabled) {
    return;
  }

  const item =
      menuItems.value[highlightedIndex.value];

  if (!item) {
    return;
  }

  setValue(item.value, blurAfterSelection);
}

function moveHighlight(delta: number): void {
  if (!menuOpen.value) {
    return;
  }

  const itemCount = menuItems.value.length;

  if (itemCount === 0) {
    return;
  }

  highlightedIndex.value =
      (
          highlightedIndex.value +
          delta +
          itemCount
      ) % itemCount;

  scrollHighlightedIntoView();
}

function handleFocus(): void {
  isFocused.value = true;

  void nextTick(() => {
    ensureControlIsVisible();
    updateMenuPosition();
  });
}

function handleBlur(): void {
  window.setTimeout(() => {
    isFocused.value = false;
    highlightedIndex.value = 0;
  }, 120);
}

function handleInput(): void {
  highlightedIndex.value = 0;

  void nextTick(updateMenuPosition);
}

function handleKeydown(
    event: KeyboardEvent,
): void {
  if (event.key === "ArrowDown") {
    event.preventDefault();
    moveHighlight(1);
    return;
  }

  if (event.key === "ArrowUp") {
    event.preventDefault();
    moveHighlight(-1);
    return;
  }

  if (event.key === "Home" && menuOpen.value) {
    event.preventDefault();

    highlightedIndex.value = 0;
    scrollHighlightedIntoView();
    return;
  }

  if (event.key === "End" && menuOpen.value) {
    event.preventDefault();

    highlightedIndex.value = Math.max(
        0,
        menuItems.value.length - 1,
    );

    scrollHighlightedIntoView();
    return;
  }

  if (event.key === "Enter") {
    if (menuOpen.value) {
      event.preventDefault();
      applyCurrent();
    }

    return;
  }

  if (event.key === "Tab") {
    if (menuOpen.value) {
      applyCurrent(false);
    }

    return;
  }

  if (event.key === "Escape") {
    /*
     * Prevent the enclosing WindowPrompt or modal from also receiving
     * Escape and closing.
     */
    event.preventDefault();
    event.stopPropagation();

    isFocused.value = false;
    inputRef.value?.blur();
    return;
  }

  if (
      event.key === "Backspace" &&
      props.modelValue &&
      props.clearable
  ) {
    event.preventDefault();
    clearValue();
  }
}

let resizeObserver: ResizeObserver | null = null;

onMounted(() => {
  window.addEventListener(
      "resize",
      updateMenuPosition,
  );

  window.addEventListener(
      "scroll",
      updateMenuPosition,
      true,
  );

  window.visualViewport?.addEventListener(
      "resize",
      updateMenuPosition,
  );

  window.visualViewport?.addEventListener(
      "scroll",
      updateMenuPosition,
  );

  if (
      typeof ResizeObserver !== "undefined" &&
      controlRef.value
  ) {
    resizeObserver = new ResizeObserver(
        updateMenuPosition,
    );

    resizeObserver.observe(controlRef.value);
  }
});

onBeforeUnmount(() => {
  window.removeEventListener(
      "resize",
      updateMenuPosition,
  );

  window.removeEventListener(
      "scroll",
      updateMenuPosition,
      true,
  );

  window.visualViewport?.removeEventListener(
      "resize",
      updateMenuPosition,
  );

  window.visualViewport?.removeEventListener(
      "scroll",
      updateMenuPosition,
  );

  resizeObserver?.disconnect();
});
</script>

<template>
  <div
      class="single-autocomplete"
      :class="{
      'single-autocomplete--open': menuOpen,
      'single-autocomplete--disabled': disabled,
    }"
  >
    <div
        ref="controlRef"
        class="single-autocomplete__control"
        :class="{
        'single-autocomplete__control--focused':
          isFocused,

        'single-autocomplete__control--selected':
          Boolean(modelValue),
      }"
        @click="focusInput"
    >
      <svg
          class="single-autocomplete__icon"
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

      <span
          v-if="modelValue"
          class="single-autocomplete__chip"
          :title="modelValue"
      >
        <span class="single-autocomplete__chip-text">
          {{ modelValue }}
        </span>

        <button
            v-if="clearable"
            type="button"
            class="single-autocomplete__clear"
            aria-label="Clear selected value"
            :disabled="disabled"
            @click.stop="clearValue"
        >
          <svg
              viewBox="0 0 24 24"
              aria-hidden="true"
          >
            <path d="M7 7l10 10" />
            <path d="M17 7 7 17" />
          </svg>
        </button>
      </span>

      <input
          ref="inputRef"
          v-model="inputText"
          type="text"
          class="single-autocomplete__input"
          :class="{
          'single-autocomplete__input--selected':
            Boolean(modelValue),
        }"
          :placeholder="modelValue ? '' : placeholder"
          :disabled="disabled"
          :aria-label="
          modelValue
            ? `Selected value: ${modelValue}`
            : placeholder
        "
          role="combobox"
          aria-autocomplete="list"
          aria-haspopup="listbox"
          :aria-expanded="menuOpen"
          :aria-controls="
          menuOpen
            ? listboxId
            : undefined
        "
          :aria-activedescendant="activeDescendant"
          autocomplete="off"
          spellcheck="false"
          @focus="handleFocus"
          @blur="handleBlur"
          @input="handleInput"
          @keydown="handleKeydown"
      />

      <svg
          class="single-autocomplete__chevron"
          :class="{
          'single-autocomplete__chevron--open':
            menuOpen,
        }"
          viewBox="0 0 24 24"
          aria-hidden="true"
      >
        <path d="m7 10 5 5 5-5" />
      </svg>
    </div>

    <Teleport to="body">
      <Transition name="autocomplete-menu">
        <div
            v-if="menuOpen"
            :id="listboxId"
            ref="menuRef"
            class="single-autocomplete__menu"
            :class="{
            'single-autocomplete__menu--above':
              menuPlacement === 'above',

            'single-autocomplete__menu--below':
              menuPlacement === 'below',

            'single-autocomplete__menu--overlay':
              menuPlacement === 'overlay',
          }"
            :style="menuStyle"
            role="listbox"
            :aria-label="placeholder"
            @keydown.esc.stop.prevent
        >
          <button
              v-for="(item, index) in menuItems"
              :id="`${instanceId}-option-${index}`"
              :key="`${item.type}:${item.value}`"
              :data-option-index="index"
              type="button"
              class="single-autocomplete__option"
              :class="{
              'single-autocomplete__option--active':
                index === highlightedIndex,

              'single-autocomplete__option--custom':
                item.type === 'custom',
            }"
              role="option"
              :aria-selected="
              index === highlightedIndex
            "
              @mouseenter="highlightedIndex = index"
              @mousedown.prevent="setValue(item.value)"
          >
            <svg
                class="single-autocomplete__option-icon"
                viewBox="0 0 24 24"
                aria-hidden="true"
            >
              <template v-if="item.type === 'custom'">
                <path d="M12 5v14" />
                <path d="M5 12h14" />
              </template>

              <path
                  v-else
                  d="M20 6 9 17l-5-5"
              />
            </svg>

            <span
                v-if="item.type === 'custom'"
                class="single-autocomplete__option-text"
            >
              Create
              <strong>“{{ item.value }}”</strong>
            </span>

            <span
                v-else
                class="single-autocomplete__option-text"
            >
              {{ item.value }}
            </span>
          </button>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.single-autocomplete {
  position: relative;

  width: 100%;
  min-width: 0;

  color: rgb(var(--c-fg));
  font-family: var(--font-primary);
}

/* -------------------------------------------------------------------------- */
/* Control                                                                    */
/* -------------------------------------------------------------------------- */

.single-autocomplete__control {
  width: 100%;
  min-width: 0;
  min-height: 2.5rem;
  box-sizing: border-box;

  display: flex;
  align-items: center;
  gap: var(--space-2);

  padding: 0.35rem 0.6rem;

  color: rgb(var(--c-fg));

  background: rgb(var(--c-surface-raised) / 0.62);
  border: 1px solid rgb(var(--c-border) / 0.34);
  border-radius: var(--radius-sm);

  box-shadow:
      inset 0 1px 0 rgb(var(--c-surface-raised) / 0.45),
      0 2px 7px rgb(var(--c-shadow) / 0.045);

  cursor: text;

  transition:
      background-color var(--duration-fast) var(--ease-standard),
      border-color var(--duration-fast) var(--ease-standard),
      box-shadow var(--duration-fast) var(--ease-standard);
}

.single-autocomplete__control:hover {
  background: rgb(var(--c-surface-hover) / 0.72);
  border-color: rgb(var(--c-primary) / 0.46);
}

.single-autocomplete__control--focused {
  background: rgb(var(--c-surface-raised) / 0.9);
  border-color: rgb(var(--c-accent) / 0.72);

  box-shadow:
      0 0 0 var(--focus-ring-width)
      rgb(var(--focus-ring-color) / 0.16),
      0 4px 12px rgb(var(--c-shadow) / 0.07);
}

.single-autocomplete__control--selected {
  border-color: rgb(var(--c-primary) / 0.46);
}

.single-autocomplete--disabled {
  opacity: 0.55;
}

.single-autocomplete--disabled
.single-autocomplete__control {
  cursor: not-allowed;
}

/* -------------------------------------------------------------------------- */
/* Icons                                                                      */
/* -------------------------------------------------------------------------- */

.single-autocomplete__icon,
.single-autocomplete__chevron {
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

.single-autocomplete__control--focused
.single-autocomplete__icon {
  stroke: rgb(var(--c-primary));
}

.single-autocomplete__chevron--open {
  stroke: rgb(var(--c-primary));
  transform: rotate(180deg);
}

/* -------------------------------------------------------------------------- */
/* Input                                                                      */
/* -------------------------------------------------------------------------- */

.single-autocomplete__input {
  flex: 1 1 auto;

  width: 100%;
  min-width: 0;
  height: 1.8rem;
  box-sizing: border-box;

  padding: 0;

  color: rgb(var(--c-fg));

  background: transparent;
  border: 0;
  outline: 0;

  font: inherit;
  font-size: 0.85rem;
}

.single-autocomplete__input::placeholder {
  color: rgb(var(--c-muted) / 0.72);
}

.single-autocomplete__input--selected {
  width: 1px;
  flex: 0 0 1px;

  opacity: 0;
}

/* -------------------------------------------------------------------------- */
/* Selected chip                                                              */
/* -------------------------------------------------------------------------- */

.single-autocomplete__chip {
  flex: 1 1 auto;
  min-width: 0;
  max-width: 100%;

  display: inline-flex;
  align-items: center;
  gap: var(--space-1);

  padding: 0.22rem 0.3rem 0.22rem 0.55rem;

  color: rgb(var(--c-on-accent));

  background: rgb(var(--c-accent) / 0.2);
  border: 1px solid rgb(var(--c-accent) / 0.42);
  border-radius: var(--radius-round);

  font-size: 0.82rem;
  font-weight: 700;
  line-height: 1.3;
}

.single-autocomplete__chip-text {
  min-width: 0;

  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.single-autocomplete__clear {
  width: 1.35rem;
  height: 1.35rem;
  flex: 0 0 auto;

  display: grid;
  place-items: center;

  padding: 0;

  color: currentColor;

  background: transparent;
  border: 0;
  border-radius: 50%;

  cursor: pointer;

  transition:
      color var(--duration-fast) var(--ease-standard),
      background-color var(--duration-fast) var(--ease-standard),
      transform var(--duration-fast) var(--ease-standard);
}

.single-autocomplete__clear:hover {
  color: rgb(var(--c-danger-strong));
  background: rgb(var(--c-danger) / 0.13);
}

.single-autocomplete__clear:active {
  transform: scale(0.88);
}

.single-autocomplete__clear:focus-visible {
  outline: 2px solid
  rgb(var(--focus-ring-color) / 0.5);

  outline-offset: 1px;
}

.single-autocomplete__clear:disabled {
  cursor: not-allowed;
}

.single-autocomplete__clear svg {
  width: 0.85rem;
  height: 0.85rem;

  fill: none;
  stroke: currentColor;
  stroke-width: 2.2;
  stroke-linecap: round;
}

/* -------------------------------------------------------------------------- */
/* Teleported viewport-aware menu                                             */
/* -------------------------------------------------------------------------- */

.single-autocomplete__menu {
  position: fixed;

  /*
   * Must remain above:
   *
   * PromptSectionEditor: 30000
   * WindowPrompt:        31000+
   * Expanded text modal: 50000
   */
  z-index: var(--z-critical-modal, 60000);

  box-sizing: border-box;

  display: flex;
  flex-direction: column;
  gap: 0.15rem;

  padding: var(--space-1);

  overflow-x: hidden;
  overflow-y: auto;
  overscroll-behavior: contain;

  color: rgb(var(--c-fg));

  background: rgb(var(--c-surface-raised));
  border: 1px solid rgb(var(--c-border-strong) / 0.55);
  border-radius: var(--radius-sm);

  box-shadow:
      0 20px 45px rgb(var(--c-shadow-strong) / 0.24),
      0 7px 18px rgb(var(--c-shadow) / 0.16);

  backdrop-filter: blur(14px);

  scrollbar-width: thin;
  scrollbar-color:
      rgb(var(--c-primary) / 0.48)
      transparent;

  contain: layout paint;
  isolation: isolate;
}

.single-autocomplete__menu--below {
  transform-origin: top;
}

.single-autocomplete__menu--above {
  transform-origin: bottom;
}

.single-autocomplete__menu--overlay {
  transform-origin: center;

  border-color: rgb(var(--c-accent) / 0.48);

  box-shadow:
      0 24px 60px rgb(var(--c-shadow-strong) / 0.28),
      0 0 0 1px rgb(var(--c-accent) / 0.12);
}

.single-autocomplete__menu::-webkit-scrollbar {
  width: 0.55rem;
}

.single-autocomplete__menu::-webkit-scrollbar-track {
  background: transparent;
}

.single-autocomplete__menu::-webkit-scrollbar-thumb {
  background: rgb(var(--c-primary) / 0.42);
  border: 2px solid transparent;
  border-radius: var(--radius-round);
  background-clip: padding-box;
}

/* -------------------------------------------------------------------------- */
/* Options                                                                    */
/* -------------------------------------------------------------------------- */

.single-autocomplete__option {
  width: 100%;
  min-width: 0;
  min-height: 2.35rem;

  display: flex;
  align-items: center;
  gap: var(--space-2);

  padding: 0.48rem 0.6rem;

  color: rgb(var(--c-fg-strong));

  background: rgb(var(--c-surface-hover) / 0.58);
  border: 1px solid rgb(var(--c-border-strong) / 0.28);
  border-radius: var(--radius-xs);

  text-align: left;
  font: inherit;
  font-size: 0.82rem;
  font-weight: 650;

  cursor: pointer;

  box-shadow:
      inset 0 1px 0 rgb(255 255 255 / 0.035);

  transition:
      color var(--duration-fast) var(--ease-standard),
      background-color var(--duration-fast) var(--ease-standard),
      border-color var(--duration-fast) var(--ease-standard),
      box-shadow var(--duration-fast) var(--ease-standard),
      transform var(--duration-fast) var(--ease-standard);
}

.single-autocomplete__option
+ .single-autocomplete__option {
  margin-top: 0.1rem;
}

.single-autocomplete__option:hover,
.single-autocomplete__option--active {
  color: rgb(var(--c-on-accent));

  background: rgb(var(--c-accent) / 0.28);
  border-color: rgb(var(--c-accent) / 0.52);

  box-shadow:
      0 2px 8px rgb(var(--c-shadow) / 0.1),
      inset 0 1px 0 rgb(255 255 255 / 0.08);

  transform: translateX(2px);
}

.single-autocomplete__option-icon {
  width: 0.95rem;
  height: 0.95rem;
  flex: 0 0 auto;

  fill: none;
  stroke: rgb(var(--c-fg-strong) / 0.72);
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.single-autocomplete__option:hover
.single-autocomplete__option-icon,
.single-autocomplete__option--active
.single-autocomplete__option-icon {
  stroke: currentColor;
}

.single-autocomplete__option-text {
  min-width: 0;

  color: inherit;

  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.single-autocomplete__option--custom {
  margin-top: var(--space-1);

  color: rgb(var(--c-accent));

  background: rgb(var(--c-accent) / 0.08);
  border-color: rgb(var(--c-accent) / 0.24);

  font-weight: 650;
}

.single-autocomplete__option--custom:hover,
.single-autocomplete__option--custom.single-autocomplete__option--active {
  color: rgb(var(--c-on-accent));
  background: rgb(var(--c-accent) / 0.3);
  border-color: rgb(var(--c-accent) / 0.58);
}

/* -------------------------------------------------------------------------- */
/* Transition                                                                 */
/* -------------------------------------------------------------------------- */

.autocomplete-menu-enter-active,
.autocomplete-menu-leave-active {
  transition:
      opacity var(--duration-fast) var(--ease-standard),
      transform var(--duration-fast) var(--ease-standard);
}

.autocomplete-menu-enter-from,
.autocomplete-menu-leave-to {
  opacity: 0;
  transform: scale(0.97);
}

.single-autocomplete__menu--below.autocomplete-menu-enter-from,
.single-autocomplete__menu--below.autocomplete-menu-leave-to {
  transform: translateY(-0.25rem) scale(0.98);
}

.single-autocomplete__menu--above.autocomplete-menu-enter-from,
.single-autocomplete__menu--above.autocomplete-menu-leave-to {
  transform: translateY(0.25rem) scale(0.98);
}

@media (prefers-reduced-motion: reduce) {
  .single-autocomplete__control,
  .single-autocomplete__icon,
  .single-autocomplete__chevron,
  .single-autocomplete__clear,
  .single-autocomplete__option,
  .autocomplete-menu-enter-active,
  .autocomplete-menu-leave-active {
    transition: none;
  }
}
</style>