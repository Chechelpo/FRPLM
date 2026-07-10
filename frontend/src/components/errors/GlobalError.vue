<script setup lang="ts">
import {
  currentGlobalError,
  dismissGlobalError,
} from "@/core/GlobalError";

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

                <p
                    id="global-error-message"
                    class="edit-box__state-description"
                >
                  {{ currentGlobalError.message }}
                </p>
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
  .global-error__close {
    transition: none;
  }
}
</style>