<script setup lang="ts">
// LongTextBox.vue
import {nextTick, onBeforeUnmount, onMounted, ref, watch} from "vue";
import {tokenize as countTokens} from "@/services/tokenizer";

/**
 * Debounce interval for emitting edit events in milliseconds.
 */
const EDIT_EMIT_MS = 250;

const model = defineModel<string | null>({
  required: true,
});
const options = withDefaults(
    defineProps<{
      tokenize?: boolean
      tokenizationStarted?: boolean
    }>(),
    {
      tokenize: false,
      tokenizationStarted: true
    }
)

const emit = defineEmits<{
  (event: "edit", payload: string): void;
}>();

const text = ref<string>(model.value ?? "");
const lastEmitted = ref(text.value);
const isExpanded = ref(false);

const tokenCount = ref<number | null>(null);
const tokenCountPending = ref(false);

const TOKENIZE_DEBOUNCE_MS = 400;

let tokenizationTimer: number | null = null;
let tokenizationSequence = 0;

const inlineRef = ref<HTMLTextAreaElement | null>(null);
const expandedRef = ref<HTMLTextAreaElement | null>(null);

let timer: number | null = null;

function canTokenize(): boolean {
  return options.tokenize && options.tokenizationStarted && text.value.length > 0;
}
function schedule_edit_emit(): void {
  model.value = text.value;
  scheduleTokenCount();

  if (timer !== null) {
    window.clearTimeout(timer);
  }

  timer = window.setTimeout(() => {
    timer = null;

    if (text.value !== lastEmitted.value) {
      emit("edit", text.value);
      lastEmitted.value = text.value;
    }
  }, EDIT_EMIT_MS);
}

function openExpanded(): void {
  isExpanded.value = true;

  void nextTick(() => {
    expandedRef.value?.focus();
  });
}

function closeExpanded(): void {
  isExpanded.value = false;

  void nextTick(() => {
    inlineRef.value?.focus();
  });
}

function scheduleTokenCount(): void {
  if (!canTokenize()) {
    tokenCount.value = null;
    tokenCountPending.value = false;
    return;
  }

  if (tokenizationTimer !== null) {
    window.clearTimeout(tokenizationTimer);
  }

  tokenizationTimer = window.setTimeout(() => {
    tokenizationTimer = null;
    void updateTokenCount();
  }, TOKENIZE_DEBOUNCE_MS);
}

async function updateTokenCount(): Promise<void> {
  if (!canTokenize()) {
    tokenCount.value = null;
    tokenCountPending.value = false;
    return;
  }

  const sequence = ++tokenizationSequence;
  const value = text.value;

  tokenCountPending.value = true;

  try {
    const result = await countTokens(value);

    /*
     * Ignore responses belonging to an older version of the text.
     */
    if (sequence === tokenizationSequence) {
      tokenCount.value = result;
    }
  } catch (error) {
    if (sequence === tokenizationSequence) {
      tokenCount.value = null;
    }

    console.warn("Could not count text tokens", error);
  } finally {
    if (sequence === tokenizationSequence) {
      tokenCountPending.value = false;
    }
  }
}

watch(
    () => [options.tokenize, options.tokenizationStarted] as const,
    ([tokenizeEnabled, tokenizationStarted]) => {
      if (tokenizeEnabled && tokenizationStarted) {
        scheduleTokenCount();
        return;
      }

      tokenizationSequence++;
      tokenCount.value = null;
      tokenCountPending.value = false;

      if (tokenizationTimer !== null) {
        window.clearTimeout(tokenizationTimer);
        tokenizationTimer = null;
      }
    },
);

onMounted(() => {
  if (canTokenize()) {
    void updateTokenCount();
  }
});
onBeforeUnmount(() => {
  if (timer !== null) {
    window.clearTimeout(timer);
  }

  if (tokenizationTimer !== null) {
    window.clearTimeout(tokenizationTimer);
  }

  /*
   * Prevent an outstanding response from updating unmounted state.
   */
  tokenizationSequence++;
});
</script>

<template>
  <div class="long-text-editor">
    <div
        v-if="options.tokenize"
        class="
          long-text-editor__token-count
          long-text-editor__token-count--inline
        "
        role="status"
        aria-live="polite"
    >
      token count:
      <span>
        {{ tokenCountPending ? "…" : (tokenCount ?? "—") }}
      </span>
    </div>

    <div class="long-text-editor__inline">
      <textarea
          ref="inlineRef"
          v-model="text"
          class="
          long-text-editor__textarea
          long-text-editor__textarea--inline
        "
          aria-label="Text editor"
          @input="schedule_edit_emit"
      />

      <button
          class="
          edit-box__action
          long-text-editor__icon-button
          long-text-editor__expand-button
        "
          type="button"
          title="Open expanded editor"
          aria-label="Open expanded editor"
          @click="openExpanded"
      >
        <svg
            viewBox="0 0 24 24"
            aria-hidden="true"
        >
          <path d="M8 3H3v5"/>
          <path d="m3 3 6 6"/>
          <path d="M16 3h5v5"/>
          <path d="m21 3-6 6"/>
          <path d="M8 21H3v-5"/>
          <path d="m3 21 6-6"/>
          <path d="M16 21h5v-5"/>
          <path d="m21 21-6-6"/>
        </svg>
      </button>
    </div>

    <Teleport to="body">
      <Transition name="text-editor-modal">
        <div
            v-if="isExpanded"
            class="modal-backdrop"
            role="presentation"
            @click.self="closeExpanded"
            @keydown.esc.stop.prevent="closeExpanded"
        >
          <section
              class="
              modal-window
              edit-box
              edit-box--accent
            "
              role="dialog"
              aria-modal="true"
              aria-labelledby="expanded-text-editor-title"
          >
            <header class="edit-box__header modal-toolbar">
              <div class="edit-box__header-main">
                <span class="edit-box__eyebrow">
                  Text editor
                </span>

                <h2
                    id="expanded-text-editor-title"
                    class="edit-box__title"
                >
                  Expanded editor
                </h2>
              </div>

              <div
                  v-if="options.tokenize"
                  class="
                      long-text-editor__token-count
                      long-text-editor__token-count--modal
                    "
                  role="status"
                  aria-live="polite"
              >
                Tokens:
                <span>
                  {{ tokenCountPending ? "…" : (tokenCount ?? "—") }}
                </span>
              </div>

              <div class="edit-box__actions">
                <button
                    class="
                    edit-box__action
                    long-text-editor__icon-button
                    modal-toolbar__close
                  "
                    type="button"
                    title="Close expanded editor"
                    aria-label="Close expanded editor"
                    @click="closeExpanded"
                >
                  <svg
                      viewBox="0 0 24 24"
                      aria-hidden="true"
                  >
                    <path d="M6 6l12 12"/>
                    <path d="M18 6 6 18"/>
                  </svg>
                </button>
              </div>
            </header>

            <div class="edit-box__body modal-window__body">
              <textarea
                  ref="expandedRef"
                  v-model="text"
                  class="
                  long-text-editor__textarea
                  long-text-editor__textarea--expanded
                "
                  aria-label="Expanded text editor"
                  @input="schedule_edit_emit"
                  @keydown.esc.stop.prevent="closeExpanded"
              />
            </div>
          </section>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.long-text-editor {
  width: 100%;
  min-width: 0;

  color: rgb(var(--c-fg));
  font-family: var(--font-primary);
}

/* -------------------------------------------------------------------------- */
/* Inline editor                                                              */
/* -------------------------------------------------------------------------- */

.long-text-editor__inline {
  position: relative;

  width: 100%;
  min-width: 0;
  box-sizing: border-box;

  background: linear-gradient(
      145deg,
      rgb(var(--c-surface-raised) / 0.58),
      rgb(var(--c-surface-2) / 0.36)
  );

  border: 1px solid rgb(var(--c-border) / 0.32);
  border-radius: var(--radius-md);

  box-shadow: inset 0 1px 0 rgb(255 255 255 / 0.34),
  0 4px 14px rgb(var(--c-shadow) / 0.055);

  transition: background-color var(--duration-normal) var(--ease-standard),
  border-color var(--duration-normal) var(--ease-standard),
  box-shadow var(--duration-normal) var(--ease-standard);
}

.long-text-editor__inline:hover {
  border-color: rgb(var(--c-primary) / 0.42);
}

.long-text-editor__inline:focus-within {
  border-color: rgb(var(--c-accent) / 0.72);

  box-shadow: 0 0 0 var(--focus-ring-width) rgb(var(--focus-ring-color) / 0.15),
  inset 0 1px 0 rgb(255 255 255 / 0.38),
  0 6px 18px rgb(var(--c-shadow) / 0.075);
}

/* -------------------------------------------------------------------------- */
/* Text areas                                                                 */
/* -------------------------------------------------------------------------- */

.long-text-editor__textarea {
  display: block;

  width: 100%;
  min-width: 0;
  box-sizing: border-box;

  padding: var(--space-3);

  color: rgb(var(--c-fg));
  caret-color: rgb(var(--c-accent-2));

  background: transparent;
  border: 0;
  border-radius: inherit;
  outline: 0;

  font-family: var(--font-primary);
  font-size: 1rem;
  font-weight: 450;
  line-height: 1.65;

  scrollbar-width: thin;
  scrollbar-color: rgb(var(--c-primary) / 0.48) transparent;
}

.long-text-editor__textarea::placeholder {
  color: rgb(var(--c-muted) / 0.72);
}

.long-text-editor__textarea::selection {
  color: rgb(var(--c-on-accent));
  background: rgb(var(--c-accent) / 0.46);
}

.long-text-editor__textarea::-webkit-scrollbar {
  width: 0.65rem;
  height: 0.65rem;
}

.long-text-editor__textarea::-webkit-scrollbar-track {
  background: transparent;
}

.long-text-editor__textarea::-webkit-scrollbar-thumb {
  background: rgb(var(--c-primary) / 0.38);
  border: 2px solid transparent;
  border-radius: var(--radius-round);
  background-clip: padding-box;
}

.long-text-editor__textarea::-webkit-scrollbar-thumb:hover {
  background: rgb(var(--c-primary) / 0.58);
  border: 2px solid transparent;
  background-clip: padding-box;
}

.long-text-editor__textarea--inline {
  min-height: 12.5rem;

  padding-right: 3.5rem;

  resize: vertical;
}

.long-text-editor__textarea--expanded {
  flex: 1;

  min-height: 0;
  height: 100%;

  padding: var(--space-4);

  background: linear-gradient(
      145deg,
      rgb(var(--c-surface-raised) / 0.55),
      rgb(var(--c-surface) / 0.42)
  );

  border: 1px solid rgb(var(--c-border) / 0.28);
  border-radius: var(--radius-md);

  font-size: 1.08rem;

  resize: none;

  transition: border-color var(--duration-normal) var(--ease-standard),
  box-shadow var(--duration-normal) var(--ease-standard);
}

.long-text-editor__textarea--expanded:focus {
  border-color: rgb(var(--c-accent) / 0.65);

  box-shadow: 0 0 0 var(--focus-ring-width) rgb(var(--focus-ring-color) / 0.13);
}

/* -------------------------------------------------------------------------- */
/* Buttons                                                                    */
/* -------------------------------------------------------------------------- */

.long-text-editor__icon-button {
  width: 2.35rem;
  height: 2.35rem;
  min-height: 0;
  flex: 0 0 auto;

  padding: 0;
}

.long-text-editor__icon-button svg {
  width: 1.05rem;
  height: 1.05rem;

  fill: none;
  stroke: currentColor;
  stroke-width: 1.9;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.long-text-editor__expand-button {
  position: absolute;
  top: var(--space-2);
  right: var(--space-2);

  color: rgb(var(--c-primary-strong));

  background: rgb(var(--c-surface-raised) / 0.78);
  border-color: rgb(var(--c-primary) / 0.28);

  backdrop-filter: blur(8px);
}

.long-text-editor__expand-button:hover {
  color: rgb(var(--c-on-accent));

  background: rgb(var(--c-accent) / 0.26);
  border-color: rgb(var(--c-accent) / 0.58);
}

.modal-toolbar__close {
  color: rgb(var(--c-danger-strong));

  background: rgb(var(--c-danger) / 0.08);
  border-color: rgb(var(--c-danger) / 0.22);
}

.modal-toolbar__close:hover {
  color: rgb(var(--c-on-danger));

  background: rgb(var(--c-danger) / 0.88);
  border-color: rgb(var(--c-danger));
}

/* -------------------------------------------------------------------------- */
/* Expanded modal                                                             */
/* -------------------------------------------------------------------------- */

.modal-backdrop {
  position: fixed;
  inset: 0;

  /*
   * Must remain above PromptSectionEditor and WindowPrompt.
   *
   * PromptSectionEditor: approximately 30000
   * WindowPrompt: approximately 31000+
   */
  z-index: var(--z-critical-modal, 50000);

  display: grid;
  place-items: center;

  box-sizing: border-box;
  padding: var(--space-4);

  overflow: auto;
  overscroll-behavior: contain;

  background: rgb(var(--c-shadow-strong) / 0.68);

  backdrop-filter: blur(7px);
  isolation: isolate;
}

.modal-window {
  width: min(92vw, 75rem);
  height: min(88dvh, 56rem);
  min-width: 0;
  max-height: calc(100dvh - (var(--space-4) * 2));

  display: flex;
  flex-direction: column;

  margin: auto;
  overflow: hidden;

  background: linear-gradient(
      145deg,
      rgb(var(--c-surface-raised) / 0.98),
      rgb(var(--c-surface) / 0.97)
  );

  box-shadow: 0 28px 90px rgb(var(--c-shadow-strong) / 0.34),
  0 10px 30px rgb(var(--c-shadow) / 0.2),
  inset 0 1px 0 rgb(255 255 255 / 0.42);
}

.modal-toolbar {
  flex: 0 0 auto;
  align-items: center;
}

.modal-window__body {
  flex: 1 1 auto;
  min-height: 0;

  display: flex;

  padding: var(--space-3);
  overflow: hidden;
}

/* -------------------------------------------------------------------------- */
/* Modal transition                                                           */
/* -------------------------------------------------------------------------- */

.text-editor-modal-enter-active,
.text-editor-modal-leave-active {
  transition: opacity var(--duration-normal) var(--ease-standard);
}

.text-editor-modal-enter-active .modal-window,
.text-editor-modal-leave-active .modal-window {
  transition: opacity var(--duration-normal) var(--ease-standard),
  transform var(--duration-normal) var(--ease-standard);
}

.text-editor-modal-enter-from,
.text-editor-modal-leave-to {
  opacity: 0;
}

.text-editor-modal-enter-from .modal-window,
.text-editor-modal-leave-to .modal-window {
  opacity: 0;

  transform: translateY(1rem) scale(0.98);
}

.long-text-editor__token-count {
  width: max-content;
  max-width: 100%;

  padding: 0.18rem 0.5rem;

  color: rgb(var(--c-muted));

  background: rgb(var(--c-surface-raised) / 0.78);
  border: 1px solid rgb(var(--c-border) / 0.28);
  border-radius: var(--radius-round);

  font-family: var(--font-monospace);
  font-size: 0.66rem;
  font-weight: 650;
  line-height: 1.25;

  pointer-events: none;
  backdrop-filter: blur(8px);
}

.long-text-editor__token-count span {
  color: rgb(var(--c-primary-strong));
  font-weight: 800;
}

.long-text-editor__token-count--inline {
  margin: 0 auto var(--space-2);
}

.long-text-editor__token-count--modal {
  position: absolute;
  top: 50%;
  left: 50%;
  z-index: 1;

  flex: 0 0 auto;

  /*
   * Center relative to the modal header rather than participating in
   * normal toolbar layout.
   */
  transform: translate(-50%, -50%);
}

.modal-toolbar {
  position: relative;

  flex: 0 0 auto;
  align-items: center;
}
/* -------------------------------------------------------------------------- */
/* Responsive                                                                 */
/* -------------------------------------------------------------------------- */

@media (max-width: 700px) {
  .modal-backdrop {
    padding: var(--space-2);
  }

  .modal-window {
    width: 100%;
    height: calc(
        100dvh -
        (var(--space-2) * 2)
    );

    max-height: calc(
        100dvh -
        (var(--space-2) * 2)
    );
  }

  .long-text-editor__textarea--expanded {
    padding: var(--space-3);
    font-size: 1rem;
  }
}

@media (max-width: 480px) {
  .modal-window__body {
    padding: var(--space-2);
  }

  .modal-toolbar {
    padding: calc(var(--space-3) + 0.15rem) var(--space-3) var(--space-3);
  }
}

@media (prefers-reduced-motion: reduce) {
  .long-text-editor__inline,
  .long-text-editor__textarea--expanded,
  .text-editor-modal-enter-active,
  .text-editor-modal-leave-active,
  .text-editor-modal-enter-active .modal-window,
  .text-editor-modal-leave-active .modal-window {
    transition: none;
  }
}
</style>