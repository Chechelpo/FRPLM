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

const props = withDefaults(
    defineProps<{
      suggestions: string[];
      modelValue?: string[];
      placeholder?: string;
      allowCustom?: boolean;
    }>(),
    {
      modelValue: () => [],
      placeholder: "Type and press Tab to add…",
      allowCustom: false,
    },
);

const emit = defineEmits<{
  (event: "update:modelValue", tags: string[]): void;
  (event: "add", newValue: string): void;
  (event: "remove", stringToRemove: string): void;
}>();

const internalTags = ref<string[]>([
  ...props.modelValue,
]);

const inputText = ref("");
const isFocused = ref(false);

const controlRef = ref<HTMLElement | null>(null);
const inputRef = ref<HTMLInputElement | null>(null);

const suggestionPlacement = ref<"above" | "below">(
    "above",
);

const suggestionStyle = ref<CSSProperties>({});

const suggestionId = `tag-suggestion-${Math.random()
    .toString(36)
    .slice(2, 10)}`;

let resizeObserver: ResizeObserver | null = null;

watch(
    () => props.modelValue,
    newValue => {
      internalTags.value = [...(newValue ?? [])];
    },
);

const normalizedTags = computed<Set<string>>(() => {
  return new Set(
      internalTags.value.map(tag =>
          tag.trim().toLocaleLowerCase(),
      ),
  );
});

const showSuggestion = computed<string | null>(() => {
  const query = inputText.value
      .trim()
      .toLocaleLowerCase();

  if (!query) {
    return null;
  }

  const match = props.suggestions.find(suggestion => {
    return suggestion
        .toLocaleLowerCase()
        .startsWith(query);
  });

  if (!match) {
    return null;
  }

  if (
      normalizedTags.value.has(
          match.trim().toLocaleLowerCase(),
      )
  ) {
    return null;
  }

  return match;
});

const canApplyCustom = computed<boolean>(() => {
  const value = inputText.value.trim();

  if (!props.allowCustom || !value) {
    return false;
  }

  return !normalizedTags.value.has(
      value.toLocaleLowerCase(),
  );
});

const suggestionVisible = computed<boolean>(() => {
  return (
      isFocused.value &&
      showSuggestion.value !== null
  );
});

watch(
    [showSuggestion, isFocused],
    async ([suggestion, focused]) => {
      if (!suggestion || !focused) {
        return;
      }

      await nextTick();
      updateSuggestionPosition();
    },
);

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

function updateSuggestionPosition(): void {
  const control = controlRef.value;

  if (!control || !suggestionVisible.value) {
    return;
  }

  const rect = control.getBoundingClientRect();
  const visualViewport = window.visualViewport;

  const viewportWidth =
      visualViewport?.width ?? window.innerWidth;

  const viewportHeight =
      visualViewport?.height ?? window.innerHeight;

  const viewportLeft =
      visualViewport?.offsetLeft ?? 0;

  const viewportTop =
      visualViewport?.offsetTop ?? 0;

  const controlLeft = rect.left - viewportLeft;
  const controlTop = rect.top - viewportTop;
  const controlBottom = rect.bottom - viewportTop;

  const viewportMargin = 12;
  const controlGap = 6;
  const estimatedSuggestionHeight = 46;

  const availableWidth = Math.max(
      0,
      viewportWidth - viewportMargin * 2,
  );

  const suggestionWidth = Math.min(
      Math.max(rect.width, 220),
      Math.min(448, availableWidth),
  );

  const left = clamp(
      controlLeft,
      viewportMargin,
      viewportWidth -
      viewportMargin -
      suggestionWidth,
  );

  const spaceAbove =
      controlTop -
      viewportMargin -
      controlGap;

  const spaceBelow =
      viewportHeight -
      controlBottom -
      viewportMargin -
      controlGap;

  const openBelow =
      spaceBelow >= estimatedSuggestionHeight ||
      spaceBelow > spaceAbove;

  if (openBelow) {
    suggestionPlacement.value = "below";

    suggestionStyle.value = {
      top: `${controlBottom + controlGap}px`,
      bottom: "auto",
      left: `${left}px`,
      width: `${suggestionWidth}px`,
      maxWidth: `${availableWidth}px`,
    };

    return;
  }

  suggestionPlacement.value = "above";

  suggestionStyle.value = {
    top: "auto",
    bottom: `${
        viewportHeight -
        controlTop +
        controlGap
    }px`,
    left: `${left}px`,
    width: `${suggestionWidth}px`,
    maxWidth: `${availableWidth}px`,
  };
}

function setTags(tags: string[]): void {
  internalTags.value = tags;

  emit("update:modelValue", tags);
}

function addTag(tag: string): void {
  const normalized = tag.trim();

  if (!normalized) {
    return;
  }

  const alreadyExists = normalizedTags.value.has(
      normalized.toLocaleLowerCase(),
  );

  if (alreadyExists) {
    return;
  }

  const updated = [
    ...internalTags.value,
    normalized,
  ];

  setTags(updated);
  emit("add", normalized);
}

function removeTag(index: number): void {
  const removed = internalTags.value[index];

  if (removed === undefined) {
    return;
  }

  const updated = [...internalTags.value];

  updated.splice(index, 1);

  setTags(updated);
  emit("remove", removed);
}

function applySuggestion(): void {
  const text = inputText.value.trim();

  if (!text) {
    return;
  }

  if (showSuggestion.value) {
    addTag(showSuggestion.value);
  } else if (canApplyCustom.value) {
    addTag(text);
  } else {
    return;
  }

  inputText.value = "";

  nextTick(() => {
    inputRef.value?.focus();
  });
}

function handleBackspace(
    event: KeyboardEvent,
): void {
  if (
      inputText.value === "" &&
      internalTags.value.length > 0
  ) {
    event.preventDefault();

    removeTag(
        internalTags.value.length - 1,
    );
  }
}

function handleTab(event: KeyboardEvent): void {
  if (
      showSuggestion.value ||
      canApplyCustom.value
  ) {
    event.preventDefault();
    applySuggestion();
  }
}

function handleEnter(
    event: KeyboardEvent,
): void {
  if (
      showSuggestion.value ||
      canApplyCustom.value
  ) {
    event.preventDefault();
    applySuggestion();
  }
}

function handleFocus(): void {
  isFocused.value = true;

  nextTick(() => {
    controlRef.value?.scrollIntoView({
      block: "nearest",
      inline: "nearest",
    });

    updateSuggestionPosition();
  });
}

function handleBlur(): void {
  /*
   * The delay allows a teleported suggestion button to process its
   * pointer interaction before the suggestion disappears.
   */
  window.setTimeout(() => {
    isFocused.value = false;
  }, 100);
}

onMounted(() => {
  window.addEventListener(
      "resize",
      updateSuggestionPosition,
  );

  window.addEventListener(
      "scroll",
      updateSuggestionPosition,
      true,
  );

  window.visualViewport?.addEventListener(
      "resize",
      updateSuggestionPosition,
  );

  window.visualViewport?.addEventListener(
      "scroll",
      updateSuggestionPosition,
  );

  if (
      typeof ResizeObserver !== "undefined" &&
      controlRef.value
  ) {
    resizeObserver = new ResizeObserver(
        updateSuggestionPosition,
    );

    resizeObserver.observe(controlRef.value);
  }
});

onBeforeUnmount(() => {
  window.removeEventListener(
      "resize",
      updateSuggestionPosition,
  );

  window.removeEventListener(
      "scroll",
      updateSuggestionPosition,
      true,
  );

  window.visualViewport?.removeEventListener(
      "resize",
      updateSuggestionPosition,
  );

  window.visualViewport?.removeEventListener(
      "scroll",
      updateSuggestionPosition,
  );

  resizeObserver?.disconnect();
});
</script>

<template>
  <div
      class="tag-autocomplete"
      :class="{
      'tag-autocomplete--focused': isFocused,
    }"
  >
    <div
        ref="controlRef"
        class="tag-autocomplete__control"
        @click="inputRef?.focus()"
    >
      <span
          v-for="(tag, index) in internalTags"
          :key="tag"
          class="tag-autocomplete__chip"
      >
        <span class="tag-autocomplete__chip-label">
          {{ tag }}
        </span>

        <button
            type="button"
            class="tag-autocomplete__remove"
            :aria-label="`Remove ${tag}`"
            @click.stop="removeTag(index)"
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
          class="tag-autocomplete__input"
          type="text"
          role="combobox"
          aria-autocomplete="both"
          aria-haspopup="listbox"
          :placeholder="
          internalTags.length
            ? ''
            : placeholder
        "
          :aria-label="placeholder"
          :aria-expanded="suggestionVisible"
          :aria-controls="
          suggestionVisible
            ? suggestionId
            : undefined
        "
          autocomplete="off"
          spellcheck="false"
          @keydown.tab="handleTab"
          @keydown.enter="handleEnter"
          @keydown.backspace="handleBackspace"
          @focus="handleFocus"
          @blur="handleBlur"
      />
    </div>

    <p
        v-if="isFocused"
        class="tag-autocomplete__hint"
    >
      Press Enter or Tab to add. Press Backspace to remove the last tag.
    </p>

    <Teleport to="body">
      <Transition name="suggestion">
        <button
            v-if="suggestionVisible"
            :id="suggestionId"
            class="tag-autocomplete__suggestion"
            :class="{
            'tag-autocomplete__suggestion--above':
              suggestionPlacement === 'above',

            'tag-autocomplete__suggestion--below':
              suggestionPlacement === 'below',
          }"
            :style="suggestionStyle"
            type="button"
            role="option"
            aria-selected="true"
            tabindex="-1"
            @mousedown.prevent
            @click="applySuggestion"
        >
          <svg
              class="tag-autocomplete__suggestion-icon"
              viewBox="0 0 24 24"
              aria-hidden="true"
          >
            <path d="M12 5v14" />
            <path d="M5 12h14" />
          </svg>

          <span class="tag-autocomplete__suggestion-text">
            {{ showSuggestion }}
          </span>

          <kbd>Tab</kbd>
        </button>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.tag-autocomplete {
  position: relative;

  width: 100%;
  min-width: 0;

  color: rgb(var(--c-fg));
  font-family: var(--font-primary);
}

/* -------------------------------------------------------------------------- */
/* Input surface                                                              */
/* -------------------------------------------------------------------------- */

.tag-autocomplete__control {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-2);

  width: 100%;
  min-width: 0;
  min-height: 2.75rem;
  box-sizing: border-box;

  padding: 0.4rem 0.55rem;

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-surface-raised) / 0.68),
          rgb(var(--c-surface-2) / 0.36)
      );

  border: 1px solid rgb(var(--c-border) / 0.35);
  border-radius: var(--radius-md);

  box-shadow:
      inset 0 1px 0 rgb(255 255 255 / 0.32),
      0 3px 10px rgb(var(--c-shadow) / 0.04);

  cursor: text;

  transition:
      background-color var(--duration-normal) var(--ease-standard),
      border-color var(--duration-normal) var(--ease-standard),
      box-shadow var(--duration-normal) var(--ease-standard);
}

.tag-autocomplete__control:hover {
  border-color: rgb(var(--c-primary) / 0.52);
}

.tag-autocomplete--focused
.tag-autocomplete__control {
  background:
      linear-gradient(
          145deg,
          rgb(var(--c-surface-raised) / 0.84),
          rgb(var(--c-surface-hover) / 0.48)
      );

  border-color: rgb(var(--c-accent) / 0.75);

  box-shadow:
      0 0 0 var(--focus-ring-width)
      rgb(var(--focus-ring-color) / 0.15),
      0 5px 15px rgb(var(--c-shadow) / 0.065),
      inset 0 1px 0 rgb(255 255 255 / 0.38);
}

/* -------------------------------------------------------------------------- */
/* Tags                                                                       */
/* -------------------------------------------------------------------------- */

.tag-autocomplete__chip {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;

  max-width: 100%;
  min-height: 1.75rem;
  box-sizing: border-box;

  padding: 0.18rem 0.25rem 0.18rem 0.55rem;

  color: rgb(var(--c-primary-strong));

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-accent) / 0.2),
          rgb(var(--c-primary) / 0.11)
      );

  border: 1px solid rgb(var(--c-accent) / 0.34);
  border-radius: var(--radius-round);

  font-size: 0.8rem;
  font-weight: 700;
  line-height: 1;

  box-shadow:
      inset 0 1px 0 rgb(255 255 255 / 0.25);
}

.tag-autocomplete__chip-label {
  min-width: 0;

  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tag-autocomplete__remove {
  width: 1.35rem;
  height: 1.35rem;
  flex: 0 0 auto;

  display: grid;
  place-items: center;

  padding: 0;

  color: rgb(var(--c-muted));

  background: transparent;
  border: 0;
  border-radius: 50%;

  cursor: pointer;

  transition:
      color var(--duration-fast) var(--ease-standard),
      background-color var(--duration-fast) var(--ease-standard),
      transform var(--duration-fast) var(--ease-standard);
}

.tag-autocomplete__remove:hover {
  color: rgb(var(--c-on-danger));
  background: rgb(var(--c-danger) / 0.82);
}

.tag-autocomplete__remove:active {
  transform: scale(0.9);
}

.tag-autocomplete__remove:focus-visible {
  outline: 2px solid
  rgb(var(--focus-ring-color) / 0.72);

  outline-offset: 1px;
}

.tag-autocomplete__remove svg {
  width: 0.8rem;
  height: 0.8rem;

  fill: none;
  stroke: currentColor;
  stroke-width: 2.2;
  stroke-linecap: round;
}

/* -------------------------------------------------------------------------- */
/* Text input                                                                 */
/* -------------------------------------------------------------------------- */

.tag-autocomplete__input {
  flex: 1 1 9rem;

  min-width: 5rem;
  height: 1.9rem;

  padding: 0 0.15rem;

  color: rgb(var(--c-fg));

  background: transparent;
  border: 0;
  outline: 0;

  font-family: var(--font-primary);
  font-size: 0.88rem;
  font-weight: 500;
}

.tag-autocomplete__input::placeholder {
  color: rgb(var(--c-muted));
  opacity: 0.78;
}

/* -------------------------------------------------------------------------- */
/* Teleported suggestion                                                      */
/* -------------------------------------------------------------------------- */

.tag-autocomplete__suggestion {
  position: fixed;

  /*
   * This remains above PromptSectionEditor, WindowPrompt and the expanded
   * LongTextBox editor.
   */
  z-index: var(--z-critical-popover, 51000);

  min-width: 0;
  min-height: 2.35rem;
  box-sizing: border-box;

  display: flex;
  align-items: center;
  gap: var(--space-2);

  padding: 0.48rem 0.65rem;

  color: rgb(var(--c-fg-strong));

  background: rgb(var(--c-surface-raised));
  border: 1px solid rgb(var(--c-accent) / 0.58);
  border-radius: var(--radius-sm);

  box-shadow:
      0 18px 42px rgb(var(--c-shadow-strong) / 0.24),
      0 6px 16px rgb(var(--c-shadow) / 0.16),
      inset 0 1px 0 rgb(255 255 255 / 0.34);

  font-family: var(--font-primary);
  font-size: 0.82rem;
  font-weight: 750;
  text-align: left;

  cursor: pointer;
  backdrop-filter: blur(14px);

  overflow: hidden;

  transition:
      color var(--duration-fast) var(--ease-standard),
      background-color var(--duration-fast) var(--ease-standard),
      border-color var(--duration-fast) var(--ease-standard),
      transform var(--duration-fast) var(--ease-standard);
}

.tag-autocomplete__suggestion--above {
  transform-origin: bottom;
}

.tag-autocomplete__suggestion--below {
  transform-origin: top;
}

.tag-autocomplete__suggestion:hover {
  color: rgb(var(--c-on-accent));

  background: rgb(var(--c-accent) / 0.3);
  border-color: rgb(var(--c-accent) / 0.78);

  transform: translateY(-1px);
}

.tag-autocomplete__suggestion-text {
  min-width: 0;
  flex: 1 1 auto;

  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tag-autocomplete__suggestion-icon {
  width: 1rem;
  height: 1rem;
  flex: 0 0 auto;

  fill: none;
  stroke: rgb(var(--c-primary));
  stroke-width: 2;
  stroke-linecap: round;
}

.tag-autocomplete__suggestion:hover
.tag-autocomplete__suggestion-icon {
  stroke: currentColor;
}

.tag-autocomplete__suggestion kbd {
  flex: 0 0 auto;

  margin-left: auto;
  padding: 0.18rem 0.38rem;

  color: rgb(var(--c-muted));

  background: rgb(var(--c-surface) / 0.76);
  border: 1px solid rgb(var(--c-border) / 0.36);
  border-radius: var(--radius-xs);

  font-family: var(--font-monospace);
  font-size: 0.65rem;
  font-weight: 700;
}

.tag-autocomplete__suggestion:hover kbd {
  color: rgb(var(--c-on-accent));

  background: rgb(var(--c-surface-raised) / 0.2);
  border-color: rgb(var(--c-accent) / 0.38);
}

/* -------------------------------------------------------------------------- */
/* Helper text                                                                */
/* -------------------------------------------------------------------------- */

.tag-autocomplete__hint {
  margin: 0.35rem 0 0;
  padding-left: 0.2rem;

  color: rgb(var(--c-muted));

  font-size: 0.68rem;
  line-height: 1.4;
}

/* -------------------------------------------------------------------------- */
/* Transition                                                                 */
/* -------------------------------------------------------------------------- */

.suggestion-enter-active,
.suggestion-leave-active {
  transition:
      opacity var(--duration-fast) var(--ease-standard),
      transform var(--duration-fast) var(--ease-standard);
}

.suggestion-enter-from,
.suggestion-leave-to {
  opacity: 0;
  transform: scale(0.96);
}

.tag-autocomplete__suggestion--above.suggestion-enter-from,
.tag-autocomplete__suggestion--above.suggestion-leave-to {
  transform:
      translateY(0.3rem)
      scale(0.97);
}

.tag-autocomplete__suggestion--below.suggestion-enter-from,
.tag-autocomplete__suggestion--below.suggestion-leave-to {
  transform:
      translateY(-0.3rem)
      scale(0.97);
}

@media (max-width: 480px) {
  .tag-autocomplete__control {
    padding: 0.35rem;
  }

  .tag-autocomplete__suggestion kbd {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .tag-autocomplete__control,
  .tag-autocomplete__remove,
  .tag-autocomplete__suggestion,
  .suggestion-enter-active,
  .suggestion-leave-active {
    transition: none;
  }
}
</style>