<script setup lang="ts">
import {     ShortTextBox,
    BooleanToggle,
    LongTextBox,
    SingleEnumInput } from "@frplm/ui";

import {     PromptSection,
    ChatCompletionRole } from "@frplm/host-sdk";

import {
  computed,
  nextTick,
  onBeforeUnmount,
  ref,
  watch,
} from "vue";


import FieldEditorWrapper from "@components/utils/FieldEditorWrapper.vue";

const props = defineProps<{
  section: PromptSection;
  index: number;
  canMoveUp: boolean;
  canMoveDown: boolean;
}>();

const emit = defineEmits<{
  (event: "move-up", section: PromptSection): void;
  (event: "move-down", section: PromptSection): void;
}>();

const edit = ref(false);

const dialogRef = ref<HTMLElement | null>(null);
const closeButtonRef = ref<HTMLButtonElement | null>(null);

let previousBodyOverflow = "";
let previouslyFocusedElement: HTMLElement | null = null;

const name = computed<string>({
  get() {
    return props.section.get("name");
  },

  set(value: string) {
    props.section.update("name", value);
  },
});

const content = computed<string>({
  get() {
    return props.section.get("content");
  },

  set(value: string) {
    props.section.update("content", value);
  },
});

const role = computed<ChatCompletionRole>({
  get() {
    return props.section.get("role");
  },

  set(value: ChatCompletionRole) {
    props.section.update("role", value);
  },
});

const active = computed<boolean>({
  get() {
    return props.section.get("active");
  },

  set(value: boolean) {
    props.section.update("active", value);
  },
});

const contentPreview = computed<string>(() => {
  const normalized = content.value
      .trim()
      .replace(/\s+/g, " ");

  if (!normalized) {
    return "Empty section";
  }

  if (normalized.length <= 140) {
    return normalized;
  }

  return `${normalized.slice(0, 140)}…`;
});

const roles = [
  ChatCompletionRole.USER,
  ChatCompletionRole.ASSISTANT,
  ChatCompletionRole.SYSTEM,
] satisfies ChatCompletionRole[];

const dialogTitleId = computed(
    () =>
        `prompt-section-${props.section.get("prompt_id")}-${props.section.get("section_id")}-title`,
);

function openEditor(): void {
  edit.value = true;
}

function closeEditor(): void {
  edit.value = false;
}

function moveUp(): void {
  if (props.canMoveUp) {
    emit("move-up", props.section);
  }
}

function moveDown(): void {
  if (props.canMoveDown) {
    emit("move-down", props.section);
  }
}

function getFocusableElements(): HTMLElement[] {
  if (!dialogRef.value) {
    return [];
  }

  const selector = [
    "button:not([disabled])",
    "input:not([disabled])",
    "textarea:not([disabled])",
    "select:not([disabled])",
    "[href]",
    '[tabindex]:not([tabindex="-1"])',
  ].join(",");

  return Array.from(
      dialogRef.value.querySelectorAll<HTMLElement>(selector),
  ).filter(element => {
    return (
        element.offsetWidth > 0 ||
        element.offsetHeight > 0
    );
  });
}

function trapDialogFocus(event: KeyboardEvent): void {
  if (event.key !== "Tab") {
    return;
  }

  const focusableElements = getFocusableElements();

  if (!focusableElements.length) {
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
      activeElement === firstElement
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

function handleDialogKeydown(
    event: KeyboardEvent,
): void {
  if (event.key === "Escape") {
    event.preventDefault();
    closeEditor();
    return;
  }

  trapDialogFocus(event);
}

watch(edit, async isOpen => {
  if (isOpen) {
    previouslyFocusedElement =
        document.activeElement instanceof HTMLElement
            ? document.activeElement
            : null;

    previousBodyOverflow =
        document.body.style.overflow;

    document.body.style.overflow = "hidden";

    await nextTick();

    closeButtonRef.value?.focus();
    return;
  }

  document.body.style.overflow =
      previousBodyOverflow;

  await nextTick();

  previouslyFocusedElement?.focus();
  previouslyFocusedElement = null;
});

onBeforeUnmount(() => {
  document.body.style.overflow =
      previousBodyOverflow;
});
</script>

<template>
  <article
      class="section-card"
      :class="{
      'section-card--inactive': !active,
    }"
  >
    <div
        class="section-card__order"
        aria-label="Section ordering controls"
    >
      <button
          type="button"
          class="section-card__arrow"
          :disabled="!canMoveUp"
          aria-label="Move section up"
          title="Move section up"
          @click.stop="moveUp"
      >
        <svg
            viewBox="0 0 24 24"
            aria-hidden="true"
        >
          <path d="m7 14 5-5 5 5" />
        </svg>
      </button>

      <button
          type="button"
          class="section-card__arrow"
          :disabled="!canMoveDown"
          aria-label="Move section down"
          title="Move section down"
          @click.stop="moveDown"
      >
        <svg
            viewBox="0 0 24 24"
            aria-hidden="true"
        >
          <path d="m7 10 5 5 5-5" />
        </svg>
      </button>
    </div>

    <button
        type="button"
        class="section-card__main"
        :aria-label="`Edit prompt section ${name}`"
        @click="openEditor"
    >
      <span class="section-card__meta">
        <span class="section-card__index">
          #{{ index + 1 }}
        </span>

        <span
            class="section-card__role"
            :data-role="role"
        >
          {{ role }}
        </span>

        <span
            class="section-card__state"
            :class="{
            'section-card__state--inactive': !active,
          }"
        >
          {{ active ? "Active" : "Inactive" }}
        </span>
      </span>

      <strong class="section-card__name">
        {{ name || "Untitled section" }}
      </strong>

      <span
          class="section-card__preview"
          :class="{
          'section-card__preview--empty':
            !content.trim(),
        }"
      >
        {{ contentPreview }}
      </span>
    </button>

    <div class="section-card__actions">
      <BooleanToggle
          :model-value="active"
          @edit="value => active = value"
      />

      <button
          type="button"
          class="
          edit-box__action
          edit-box__action--accent
          section-card__edit
        "
          @click.stop="openEditor"
      >
        <svg
            viewBox="0 0 24 24"
            aria-hidden="true"
        >
          <path d="M12 20h9" />
          <path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L8 18l-4 1 1-4Z" />
        </svg>

        Edit
      </button>
    </div>

    <Teleport to="body">
      <Transition name="section-editor">
        <div
            v-if="edit"
            class="section-editor__backdrop"
            @mousedown.self="closeEditor"
            @keydown="handleDialogKeydown"
        >
          <section
              ref="dialogRef"
              class="
              section-editor__window
              edit-box
              edit-box--accent
            "
              role="dialog"
              aria-modal="true"
              :aria-labelledby="dialogTitleId"
              tabindex="-1"
          >
            <header class="edit-box__header">
              <div class="edit-box__header-icon">
                <svg
                    viewBox="0 0 24 24"
                    aria-hidden="true"
                >
                  <path d="M4 5h16" />
                  <path d="M4 12h16" />
                  <path d="M4 19h10" />
                  <path d="m16 17 2 2 4-4" />
                </svg>
              </div>

              <div class="edit-box__header-main">
                <span class="edit-box__eyebrow">
                  Prompt section
                </span>

                <div class="edit-box__title-row">
                  <h2
                      :id="dialogTitleId"
                      class="edit-box__title"
                  >
                    {{ name || "Untitled section" }}
                  </h2>

                  <span
                      class="section-editor__role"
                      :data-role="role"
                  >
                    {{ role }}
                  </span>

                  <span
                      class="edit-box__badge"
                      :class="{
                      'edit-box__badge--success': active,
                      'edit-box__badge--neutral': !active,
                    }"
                  >
                    {{ active ? "Active" : "Inactive" }}
                  </span>
                </div>

                <p class="edit-box__description">
                  Configure the identity, role and content of this
                  prompt section.
                </p>
              </div>

              <div class="edit-box__actions">
                <button
                    ref="closeButtonRef"
                    type="button"
                    class="section-editor__close"
                    aria-label="Close section editor"
                    title="Close"
                    @click="closeEditor"
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
                edit-box__body
                edit-box__body--scrollable
                section-editor__body
              "
            >
              <div class="section-editor__fields">
                <FieldEditorWrapper
                    class="section-editor__field"
                    field-name="Name"
                >
                  <ShortTextBox
                      :model-value="name"
                      @edit="value => name = value"
                  />
                </FieldEditorWrapper>

                <FieldEditorWrapper
                    class="section-editor__field"
                    field-name="Role"
                >
                  <SingleEnumInput
                      :value="role"
                      :possible_values="roles"
                      placeholder="Select a role"
                      @edit="value => role = value"
                  />
                </FieldEditorWrapper>

                <FieldEditorWrapper
                    class="section-editor__field"
                    field-name="Active"
                    info="Inactive sections remain saved but are excluded from the generated prompt."
                >
                  <BooleanToggle
                      :model-value="active"
                      @edit="value => active = value"
                  />
                </FieldEditorWrapper>

                <FieldEditorWrapper
                    class="
                    section-editor__field
                    section-editor__field--content
                  "
                    field-name="Content"
                >
                  <LongTextBox
                      :model-value="content"
                      tokenize
                      :tokenization-started="edit"
                      @edit="value => content = value"
                  />
                </FieldEditorWrapper>
              </div>
            </main>

            <footer class="edit-box__footer">
              <span class="section-editor__footer-status">
                Changes are saved automatically.
              </span>

              <button
                  type="button"
                  class="
                  edit-box__action
                  edit-box__action--accent
                "
                  @click="closeEditor"
              >
                Done
              </button>
            </footer>
          </section>
        </div>
      </Transition>
    </Teleport>
  </article>
</template>

<style scoped>
.section-card {
  position: relative;

  width: 100%;
  min-width: 0;
  min-height: 5.25rem;
  box-sizing: border-box;

  display: grid;
  grid-template-columns:
    auto
    minmax(0, 1fr)
    auto;

  align-items: center;
  gap: var(--space-3);

  padding: var(--space-3);

  color: rgb(var(--c-fg));

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-surface-raised) / 0.54),
          rgb(var(--c-surface-2) / 0.28)
      );

  border: 1px solid rgb(var(--c-border) / 0.3);
  border-radius: var(--radius-md);

  box-shadow:
      0 4px 13px rgb(var(--c-shadow) / 0.055),
      inset 0 1px 0 rgb(var(--c-surface-raised) / 0.4);

  transition:
      background-color var(--duration-normal) var(--ease-standard),
      border-color var(--duration-normal) var(--ease-standard),
      box-shadow var(--duration-normal) var(--ease-standard),
      opacity var(--duration-normal) var(--ease-standard),
      transform var(--duration-normal) var(--ease-standard);
}

.section-card::before {
  content: "";

  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;

  width: 3px;

  background:
      linear-gradient(
          to bottom,
          rgb(var(--c-accent)),
          rgb(var(--c-primary))
      );

  border-radius:
      var(--radius-md)
      0
      0
      var(--radius-md);
}

.section-card:hover {
  background:
      linear-gradient(
          145deg,
          rgb(var(--c-surface-raised) / 0.7),
          rgb(var(--c-surface-hover) / 0.4)
      );

  border-color: rgb(var(--c-accent) / 0.42);

  box-shadow:
      0 7px 19px rgb(var(--c-shadow) / 0.085),
      inset 0 1px 0 rgb(var(--c-surface-raised) / 0.46);

  transform: translateY(-1px);
}

.section-card--inactive {
  opacity: 0.64;
}

.section-card--inactive:hover {
  opacity: 0.82;
}

/* -------------------------------------------------------------------------- */
/* Ordering controls                                                          */
/* -------------------------------------------------------------------------- */

.section-card__order {
  display: inline-flex;
  flex-direction: column;
  gap: var(--space-1);
}

.section-card__arrow {
  width: 1.85rem;
  height: 1.55rem;
  box-sizing: border-box;

  display: grid;
  place-items: center;

  padding: 0;

  color: rgb(var(--c-fg));

  background: rgb(var(--c-surface-raised) / 0.48);
  border: 1px solid rgb(var(--c-border) / 0.32);
  border-radius: var(--radius-xs);

  cursor: pointer;

  transition:
      color var(--duration-fast) var(--ease-standard),
      background-color var(--duration-fast) var(--ease-standard),
      border-color var(--duration-fast) var(--ease-standard),
      transform var(--duration-fast) var(--ease-standard);
}

.section-card__arrow svg {
  width: 0.9rem;
  height: 0.9rem;

  fill: none;
  stroke: currentColor;
  stroke-width: 2.2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.section-card__arrow:not(:disabled):hover {
  color: rgb(var(--c-on-accent));

  background: rgb(var(--c-accent) / 0.24);
  border-color: rgb(var(--c-accent) / 0.48);
}

.section-card__arrow:not(:disabled):active {
  transform: scale(0.91);
}

.section-card__arrow:focus-visible {
  outline: 2px solid
  rgb(var(--focus-ring-color) / 0.48);

  outline-offset: 2px;
}

.section-card__arrow:disabled {
  opacity: 0.28;
  cursor: not-allowed;
}

/* -------------------------------------------------------------------------- */
/* Main section information                                                   */
/* -------------------------------------------------------------------------- */

.section-card__main {
  min-width: 0;

  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 0.32rem;

  padding: var(--space-1);

  color: inherit;

  background: transparent;
  border: 0;
  border-radius: var(--radius-sm);

  text-align: left;
  cursor: pointer;
}

.section-card__main:focus-visible {
  outline: 2px solid
  rgb(var(--focus-ring-color) / 0.52);

  outline-offset: 3px;
}

.section-card__meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.35rem;

  min-width: 0;
}

.section-card__index,
.section-card__role,
.section-card__state,
.section-editor__role {
  min-height: 1.35rem;
  box-sizing: border-box;

  display: inline-flex;
  align-items: center;
  justify-content: center;

  padding: 0.18rem 0.45rem;

  border-radius: var(--radius-round);

  font-size: 0.66rem;
  font-weight: 800;
  line-height: 1;

  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.section-card__index {
  color: rgb(var(--c-primary-strong));

  background: rgb(var(--c-primary) / 0.11);
  border: 1px solid rgb(var(--c-primary) / 0.24);
}

.section-card__role,
.section-editor__role {
  color: rgb(var(--c-fg-strong));

  background: rgb(var(--c-surface-raised) / 0.48);
  border: 1px solid rgb(var(--c-border) / 0.32);
}

.section-card__role[data-role="system"],
.section-editor__role[data-role="system"] {
  color: rgb(var(--c-warning-strong));

  background: rgb(var(--c-warning) / 0.12);
  border-color: rgb(var(--c-warning) / 0.3);
}

.section-card__role[data-role="assistant"],
.section-editor__role[data-role="assistant"] {
  color: rgb(var(--c-info-strong));

  background: rgb(var(--c-info) / 0.11);
  border-color: rgb(var(--c-info) / 0.28);
}

.section-card__role[data-role="user"],
.section-editor__role[data-role="user"] {
  color: rgb(var(--c-success-strong));

  background: rgb(var(--c-success) / 0.1);
  border-color: rgb(var(--c-success) / 0.28);
}

.section-card__state {
  color: rgb(var(--c-success-strong));

  background: rgb(var(--c-success) / 0.1);
  border: 1px solid rgb(var(--c-success) / 0.24);
}

.section-card__state--inactive {
  color: rgb(var(--c-danger-strong));

  background: rgb(var(--c-danger) / 0.09);
  border-color: rgb(var(--c-danger) / 0.22);
}

.section-card__name {
  min-width: 0;

  color: rgb(var(--c-fg-strong));

  font-size: 0.98rem;
  font-weight: 750;
  line-height: 1.35;

  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.section-card__preview {
  min-width: 0;

  color: rgb(var(--c-muted));

  font-size: 0.8rem;
  line-height: 1.45;

  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.section-card__preview--empty {
  font-style: italic;
  opacity: 0.75;
}

/* -------------------------------------------------------------------------- */
/* Card actions                                                               */
/* -------------------------------------------------------------------------- */

.section-card__actions {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-2);
}

.section-card__edit {
  white-space: nowrap;
}

.section-card__edit svg {
  width: 0.95rem;
  height: 0.95rem;

  fill: none;
  stroke: currentColor;
  stroke-width: 1.9;
  stroke-linecap: round;
  stroke-linejoin: round;
}

/* -------------------------------------------------------------------------- */
/* Editor modal                                                               */
/* -------------------------------------------------------------------------- */

.section-editor__backdrop {
  position: fixed;
  inset: 0;
  z-index: 30000;

  display: grid;
  place-items: center;

  box-sizing: border-box;
  padding: clamp(0.5rem, 2vw, 1.25rem);

  overflow: auto;
  overscroll-behavior: contain;

  background: rgb(var(--c-shadow-strong) / 0.6);
  backdrop-filter: blur(6px);
}

.section-editor__window {
  width: min(100%, 48rem);
  min-width: 0;

  /*
   * The complete modal shell always remains inside the viewport.
   * Only the body scrolls when the content is taller.
   */
  max-height: calc(100dvh - 2.5rem);

  display: flex;
  flex-direction: column;

  margin: auto;

  overflow: hidden;
}

.section-editor__window > .edit-box__header,
.section-editor__window > .edit-box__footer {
  flex: 0 0 auto;
}

.section-editor__body {
  flex: 1 1 auto;
  min-height: 0;

  overflow-x: hidden;
  overflow-y: auto;

  overscroll-behavior: contain;
  scrollbar-gutter: stable;
}

.section-editor__fields {
  display: grid;
  grid-template-columns:
    repeat(
      2,
      minmax(0, 1fr)
    );

  align-items: start;
  gap: var(--space-4);

  min-width: 0;
}

.section-editor__field {
  min-width: 0;
}

.section-editor__field--content {
  grid-column: 1 / -1;
}

.section-editor__field--content :deep(textarea) {
  min-height: clamp(12rem, 35dvh, 24rem);
  max-height: none;
  resize: vertical;
}

.section-editor__close {
  width: 2.25rem;
  height: 2.25rem;
  flex: 0 0 auto;

  display: grid;
  place-items: center;

  padding: 0;

  color: rgb(var(--c-muted));

  background: rgb(var(--c-surface-raised) / 0.42);
  border: 1px solid rgb(var(--c-border) / 0.28);
  border-radius: var(--radius-sm);

  cursor: pointer;

  transition:
      color var(--duration-fast) var(--ease-standard),
      background-color var(--duration-fast) var(--ease-standard),
      border-color var(--duration-fast) var(--ease-standard),
      transform var(--duration-fast) var(--ease-standard);
}

.section-editor__close:hover {
  color: rgb(var(--c-on-danger));

  background: rgb(var(--c-danger) / 0.86);
  border-color: rgb(var(--c-danger));
}

.section-editor__close:active {
  transform: scale(0.92);
}

.section-editor__close:focus-visible {
  outline: var(--focus-ring-width) solid
  rgb(var(--focus-ring-color) / 0.35);

  outline-offset: 2px;
}

.section-editor__close svg {
  width: 1rem;
  height: 1rem;

  fill: none;
  stroke: currentColor;
  stroke-width: 2.2;
  stroke-linecap: round;
}

.section-editor__footer-status {
  margin-right: auto;

  color: rgb(var(--c-muted));

  font-size: 0.74rem;
  line-height: 1.4;
}

/* -------------------------------------------------------------------------- */
/* Modal transition                                                           */
/* -------------------------------------------------------------------------- */

.section-editor-enter-active,
.section-editor-leave-active {
  transition:
      opacity var(--duration-normal) var(--ease-standard);
}

.section-editor-enter-active
.section-editor__window,
.section-editor-leave-active
.section-editor__window {
  transition:
      opacity var(--duration-normal) var(--ease-standard),
      transform var(--duration-normal) var(--ease-standard);
}

.section-editor-enter-from,
.section-editor-leave-to {
  opacity: 0;
}

.section-editor-enter-from
.section-editor__window,
.section-editor-leave-to
.section-editor__window {
  opacity: 0;
  transform: translateY(0.75rem) scale(0.98);
}

/* -------------------------------------------------------------------------- */
/* Responsive                                                                 */
/* -------------------------------------------------------------------------- */

@media (max-width: 720px) {
  .section-card {
    grid-template-columns:
      auto
      minmax(0, 1fr);

    gap: var(--space-2);
  }

  .section-card__actions {
    grid-column: 1 / -1;

    justify-content: flex-end;

    padding-top: var(--space-2);

    border-top: 1px solid
    rgb(var(--c-border) / 0.18);
  }

  .section-editor__fields {
    grid-template-columns: 1fr;
  }

  .section-editor__field--content {
    grid-column: auto;
  }
}

@media (max-width: 480px) {
  .section-card {
    padding: var(--space-2);
  }

  .section-card__order {
    align-self: start;
  }

  .section-card__meta {
    gap: 0.25rem;
  }

  .section-editor__backdrop {
    place-items: start center;
    padding: 0.5rem;
  }

  .section-editor__window {
    width: 100%;
    max-height: calc(100dvh - 1rem);

    border-radius: var(--radius-md);
  }

  .section-editor__window > .edit-box__header {
    padding: var(--space-3);
  }

  .section-editor__body {
    padding: var(--space-3);
  }

  .section-editor__window > .edit-box__footer {
    align-items: stretch;
    flex-direction: column;
  }

  .section-editor__footer-status {
    margin-right: 0;
    text-align: center;
  }

  .section-editor__window
  > .edit-box__footer
  .edit-box__action {
    width: 100%;
  }
}

@media (prefers-reduced-motion: reduce) {
  .section-card,
  .section-card__arrow,
  .section-editor__close,
  .section-editor-enter-active,
  .section-editor-leave-active,
  .section-editor-enter-active
  .section-editor__window,
  .section-editor-leave-active
  .section-editor__window {
    transition: none;
  }
}
</style>