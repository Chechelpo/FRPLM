<script setup lang="ts">
import { computed, ref, watch } from "vue";
import {
  currentGlobalError,
  dismissGlobalError,
} from "@/core/GlobalError";

/**
 * Character threshold above which the message is considered "long" and
 * collapsed by default. Stack traces sent from the frontend for debugging
 * will typically far exceed this.
 */
const LONG_MESSAGE_THRESHOLD = 280;

/**
 * Number of characters to show in the collapsed preview.
 */
const COLLAPSED_PREVIEW_LENGTH = 260;

const isExpanded = ref(false);

const isLongMessage = computed<boolean>(() => {
  const message = currentGlobalError.value?.message ?? "";
  return (
      message.length > LONG_MESSAGE_THRESHOLD ||
      message.includes("\n")
  );
});

const truncatedMessage = computed<string>(() => {
  const message = currentGlobalError.value?.message ?? "";

  if (message.length <= COLLAPSED_PREVIEW_LENGTH) {
    return message;
  }

  /*
   * Try to cut on a whitespace boundary so we don't split a word in half.
   */
  const cut = message.slice(0, COLLAPSED_PREVIEW_LENGTH);
  const lastSpace = cut.lastIndexOf(" ");

  return `${lastSpace > 0 ? cut.slice(0, lastSpace) : cut}…`;
});

/*
 * Collapse the view whenever a new error is shown.
 */
watch(
    () => currentGlobalError.value?.id,
    () => {
      isExpanded.value = false;
    },
);

function toggleExpanded(): void {
  isExpanded.value = !isExpanded.value;
}

function dismiss(): void {
  const error = currentGlobalError.value;

  if (error) {
    dismissGlobalError(error.id);
  }
}
</script>

<template>
  <Teleport to="body">
    <Transition name="global-error">
      <aside
          v-if="currentGlobalError"
          class="global-error"
          role="alertdialog"
          aria-live="assertive"
          aria-atomic="true"
          aria-labelledby="global-error-title"
          aria-describedby="global-error-message"
      >
        <section class="edit-box edit-box--danger global-error__box">
          <header class="edit-box__header">
            <div class="edit-box__header-icon">
              <svg
                  viewBox="0 0 24 24"
                  aria-hidden="true"
              >
                <path d="M12 9v4" />
                <path d="M12 17h.01" />
                <path d="M10.3 3.8 2.2 18a2 2 0 0 0 1.8 3h16a2 2 0 0 0 1.8-3L13.7 3.8a2 2 0 0 0-3.4 0Z" />
              </svg>
            </div>

            <div class="edit-box__header-main">
              <span class="edit-box__eyebrow">
                Request failed
              </span>

              <div class="edit-box__title-row">
                <h2
                    id="global-error-title"
                    class="edit-box__title"
                >
                  {{ currentGlobalError.type }}
                </h2>

                <span class="edit-box__badge edit-box__badge--danger">
                  {{
                    currentGlobalError.status === 0
                        ? "Network"
                        : `HTTP ${currentGlobalError.status}`
                  }}
                </span>
              </div>
            </div>

            <div class="edit-box__actions">
              <button
                  class="global-error__close"
                  type="button"
                  aria-label="Dismiss error"
                  title="Dismiss error"
                  @click="dismiss"
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

          <div class="edit-box__body">
            <div class="edit-box__state edit-box__state--error">
              <div class="edit-box__state-content">
                <strong class="edit-box__state-title">
                  The operation could not be completed
                </strong>

                <!-- Collapsed preview for long messages -->
                <p
                    v-if="isLongMessage && !isExpanded"
                    id="global-error-message"
                    class="edit-box__state-description"
                >
                  {{ truncatedMessage }}
                </p>

                <!-- Full short message (no expand needed) -->
                <p
                    v-else-if="!isLongMessage"
                    id="global-error-message"
                    class="edit-box__state-description"
                >
                  {{ currentGlobalError.message }}
                </p>

                <!-- Expanded scrollable block for long messages -->
                <pre
                    v-if="isLongMessage && isExpanded"
                    id="global-error-message"
                    class="global-error__full-message"
                ><code>{{ currentGlobalError.message }}</code></pre>

                <button
                    v-if="isLongMessage"
                    class="global-error__toggle"
                    type="button"
                    :aria-expanded="isExpanded"
                    aria-controls="global-error-message"
                    @click="toggleExpanded"
                >
                  {{ isExpanded ? "Collapse" : "Show full message" }}
                </button>
              </div>
            </div>

            <dl class="global-error__details">
              <div class="global-error__detail">
                <dt>Status</dt>

                <dd>
                  {{ currentGlobalError.status }}
                </dd>
              </div>

              <div class="global-error__detail">
                <dt>Type</dt>

                <dd>
                  {{ currentGlobalError.type }}
                </dd>
              </div>

              <div class="global-error__detail global-error__detail--path">
                <dt>Path</dt>

                <dd>
                  <code>{{ currentGlobalError.path }}</code>
                </dd>
              </div>
            </dl>
          </div>

          <footer class="edit-box__footer">
            <button
                class="edit-box__action edit-box__action--danger"
                type="button"
                @click="dismiss"
            >
              Dismiss
            </button>
          </footer>
        </section>
      </aside>
    </Transition>
  </Teleport>
</template>

<style scoped>
.global-error {
  position: fixed;
  right: 1rem;
  bottom: 1rem;
  z-index: 10000;

  width: min(
      calc(100vw - 2rem),
      34rem
  );

  max-height: calc(100dvh - 2rem);

  overflow: auto;
  overscroll-behavior: contain;
}

.global-error__box {
  box-shadow:
      0 24px 70px rgb(var(--c-shadow-strong) / 0.25),
      0 8px 24px rgb(var(--c-shadow) / 0.18),
      inset 0 1px 0 rgb(255 255 255 / 0.45);
}

.global-error__close {
  width: 2.25rem;
  height: 2.25rem;

  display: grid;
  place-items: center;

  padding: 0;

  color: rgb(var(--c-danger-strong));

  background: rgb(var(--c-danger) / 0.08);
  border: 1px solid rgb(var(--c-danger) / 0.2);
  border-radius: var(--radius-sm);

  cursor: pointer;

  transition:
      color var(--duration-fast) var(--ease-standard),
      background-color var(--duration-fast) var(--ease-standard),
      border-color var(--duration-fast) var(--ease-standard),
      transform var(--duration-fast) var(--ease-standard);
}

.global-error__close:hover {
  color: rgb(var(--c-on-danger));

  background: rgb(var(--c-danger) / 0.9);
  border-color: rgb(var(--c-danger));
}

.global-error__close:active {
  transform: scale(0.94);
}

.global-error__close:focus-visible {
  outline: var(--focus-ring-width) solid
  rgb(var(--focus-ring-color) / 0.35);

  outline-offset: 2px;
}

.global-error__close svg {
  width: 1rem;
  height: 1rem;

  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
}

.global-error__full-message {
  max-height: min(50dvh, 24rem);
  min-width: 0;
  box-sizing: border-box;

  margin: var(--space-2) 0 0;
  padding: var(--space-3);

  overflow: auto;
  overscroll-behavior: contain;

  background: rgb(var(--c-surface-raised) / 0.5);
  border: 1px solid rgb(var(--c-border) / 0.26);
  border-radius: var(--radius-sm);

  font-family: var(--font-monospace);
  font-size: 0.76rem;
  font-weight: 450;
  line-height: 1.55;

  white-space: pre-wrap;
  overflow-wrap: anywhere;
  tab-size: 4;

  scrollbar-width: thin;
  scrollbar-color: rgb(var(--c-danger) / 0.5) transparent;
}

.global-error__full-message::-webkit-scrollbar {
  width: 0.6rem;
  height: 0.6rem;
}

.global-error__full-message::-webkit-scrollbar-track {
  background: transparent;
}

.global-error__full-message::-webkit-scrollbar-thumb {
  background: rgb(var(--c-danger) / 0.4);
  border: 2px solid transparent;
  border-radius: var(--radius-round);
  background-clip: padding-box;
}

.global-error__full-message::-webkit-scrollbar-thumb:hover {
  background: rgb(var(--c-danger) / 0.6);
  border: 2px solid transparent;
  background-clip: padding-box;
}

.global-error__full-message code {
  font-family: inherit;
  font-size: inherit;

  white-space: inherit;
  overflow-wrap: inherit;
}

.global-error__toggle {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);

  margin-top: var(--space-2);
  padding: 0.35rem 0.7rem;

  color: rgb(var(--c-danger-strong));

  background: rgb(var(--c-danger) / 0.1);
  border: 1px solid rgb(var(--c-danger) / 0.25);
  border-radius: var(--radius-sm);

  font-family: var(--font-primary);
  font-size: 0.74rem;
  font-weight: 750;
  line-height: 1;

  cursor: pointer;

  transition:
      color var(--duration-fast) var(--ease-standard),
      background-color var(--duration-fast) var(--ease-standard),
      border-color var(--duration-fast) var(--ease-standard),
      transform var(--duration-fast) var(--ease-standard);
}

.global-error__toggle:hover {
  color: rgb(var(--c-on-danger));

  background: rgb(var(--c-danger) / 0.85);
  border-color: rgb(var(--c-danger));
}

.global-error__toggle:active {
  transform: scale(0.96);
}

.global-error__toggle:focus-visible {
  outline: var(--focus-ring-width) solid
  rgb(var(--focus-ring-color) / 0.35);

  outline-offset: 2px;
}

.global-error__details {
  display: grid;
  grid-template-columns:
    repeat(
      2,
      minmax(0, 1fr)
    );

  gap: var(--space-2);

  margin: var(--space-3) 0 0;
}

.global-error__detail {
  min-width: 0;

  padding: var(--space-3);

  background: rgb(var(--c-surface-raised) / 0.4);
  border: 1px solid rgb(var(--c-border) / 0.22);
  border-radius: var(--radius-sm);
}

.global-error__detail--path {
  grid-column: 1 / -1;
}

.global-error__detail dt {
  margin-bottom: var(--space-1);

  color: rgb(var(--c-muted));

  font-size: 0.66rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.07em;
}

.global-error__detail dd {
  min-width: 0;
  margin: 0;

  color: rgb(var(--c-fg));

  font-size: 0.8rem;
  font-weight: 650;

  overflow-wrap: anywhere;
}

.global-error__detail code {
  font-family: var(--font-monospace);
  font-size: 0.75rem;
}

.global-error-enter-active,
.global-error-leave-active {
  transition:
      opacity var(--duration-normal) var(--ease-standard),
      transform var(--duration-normal) var(--ease-standard);
}

.global-error-enter-from,
.global-error-leave-to {
  opacity: 0;
  transform: translateY(1rem) scale(0.98);
}

@media (max-width: 600px) {
  .global-error {
    right: 0.5rem;
    bottom: 0.5rem;

    width: calc(100vw - 1rem);
    max-height: calc(100dvh - 1rem);
  }

  .global-error__details {
    grid-template-columns: 1fr;
  }

  .global-error__detail--path {
    grid-column: auto;
  }
}

@media (prefers-reduced-motion: reduce) {
  .global-error-enter-active,
  .global-error-leave-active,
  .global-error__close,
  .global-error__toggle {
    transition: none;
  }
}
</style>