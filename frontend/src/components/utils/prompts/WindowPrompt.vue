<script setup lang="ts">
import { onMounted, onUnmounted } from "vue";

const props = withDefaults(
    defineProps<{
      title?: string;
      info?: string;
      closeOnBackdrop?: boolean;
      closeOnEscape?: boolean;
      showCloseButton?: boolean;
    }>(),
    {
      closeOnBackdrop: true,
      closeOnEscape: true,
      showCloseButton: true,
    }
);

const emit = defineEmits<{
  close: [];
}>();

function close(): void {
  emit("close");
}

function onBackdropClick(): void {
  if (props.closeOnBackdrop) {
    close();
  }
}

function onKeydown(event: KeyboardEvent): void {
  if (props.closeOnEscape && event.key === "Escape") {
    close();
  }
}

onMounted(() => {
  window.addEventListener("keydown", onKeydown);
});

onUnmounted(() => {
  window.removeEventListener("keydown", onKeydown);
});
</script>

<template>
  <Teleport to="body">
    <div
        class="window-prompt-backdrop"
        @click.self="onBackdropClick"
    >
      <section
          class="window-prompt"
          role="dialog"
          aria-modal="true"
          :aria-label="title"
      >
        <header
            v-if="$slots.header || title || showCloseButton"
            class="window-prompt-header"
        >
          <slot
              name="header"
              :close="close"
          >
            <div class="window-prompt-title-block">
              <div
                  v-if="title"
                  class="window-prompt-title"
              >
                {{ title }}
              </div>

              <div
                  v-if="info"
                  class="window-prompt-info"
              >
                {{ info }}
              </div>
            </div>
          </slot>

          <button
              v-if="showCloseButton"
              type="button"
              class="window-prompt-close"
              aria-label="Close"
              @click="close"
          >
            ×
          </button>
        </header>

        <main class="window-prompt-body">
          <slot :close="close" />
        </main>

        <footer
            v-if="$slots.footer"
            class="window-prompt-footer"
        >
          <slot
              name="footer"
              :close="close"
          />
        </footer>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.window-prompt-backdrop {
  position: fixed;
  inset: 0;
  z-index: 9999;

  display: flex;
  align-items: center;
  justify-content: center;

  padding: 1rem;

  background: rgb(0 0 0 / 0.45);
}

.window-prompt {
  width: min(720px, 100%);
  max-height: min(85vh, 720px);

  display: flex;
  flex-direction: column;

  overflow: hidden;

  background: var(--primary-background, #1c1917);
  color: var(--primary-text, #e2e8f0);

  border: 1px solid var(--primary-accent, #f59e0b);
  border-radius: 0.75rem;

  box-shadow:
      0 20px 25px rgb(0 0 0 / 0.25),
      0 8px 10px rgb(0 0 0 / 0.20);
}

.window-prompt-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;

  padding: 1rem;

  background: var(--secondary-background, #44403c);
  border-bottom: 1px solid var(--primary-accent, #f59e0b);
}

.window-prompt-title-block {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;

  min-width: 0;
}

.window-prompt-title {
  font-size: 1rem;
  font-weight: 600;
  line-height: 1.4;

  color: var(--primary-text, #e2e8f0);
}

.window-prompt-info {
  font-size: 0.875rem;
  line-height: 1.35;

  color: var(--muted-text, #94a3b8);
}

.window-prompt-close {
  width: 2rem;
  height: 2rem;

  flex: 0 0 auto;

  display: inline-flex;
  align-items: center;
  justify-content: center;

  border: none;
  border-radius: 0.375rem;

  background: transparent;
  color: var(--primary-text, #e2e8f0);

  font-size: 1.5rem;
  line-height: 1;

  cursor: pointer;
}

.window-prompt-close:hover {
  background: color-mix(
      in srgb,
      var(--primary-accent, #f59e0b) 18%,
      transparent
  );
}

.window-prompt-body {
  flex: 1;
  min-height: 0;

  padding: 1rem;

  overflow: auto;
}

.window-prompt-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;

  padding: 1rem;

  background: var(--secondary-background, #44403c);
  border-top: 1px solid var(--primary-accent, #f59e0b);
}
</style>