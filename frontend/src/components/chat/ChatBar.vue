<script setup lang="ts">
import {
  computed,
  ref,
} from "vue";
import { until } from "@vueuse/core";

import type {
  ChatCompletionRequest,
} from "@/types/ChatCompletions";
import type { PromptDTO } from "@/types/DTOs";

import BooleanToggle from "@/components/primitive-editors/BooleanToggle.vue";
import FieldEditorWrapper from "@/components/utils/FieldEditorWrapper.vue";
import InlinePopover from "@/components/utils/prompts/InlinePopover.vue";
import PromptDebug from "@/components/chat/PromptDebug.vue";
import WindowPrompt from "@/components/utils/prompts/WindowPrompt.vue";

const props = defineProps<{
  characterName: string;

  newUserMessage: (
      content: string,
  ) => Promise<boolean>;

  requestPrompt: () => Promise<PromptDTO>;

  generateNewMessage: (
      request: ChatCompletionRequest,
  ) => Promise<boolean>;
}>();

const debugPrompt = ref(false);
const autoReply = ref(true);
const message = ref("");

const waitingForMessage = ref(false);
const operationError =
    ref<string | null>(null);

const promptToDebug =
    ref<PromptDTO | null>(null);

const hasMessage = computed(
    () => message.value.trim().length > 0,
);

const canSubmit = computed(
    () =>
        !waitingForMessage.value &&
        (
            hasMessage.value ||
            autoReply.value ||
            debugPrompt.value
        ),
);

const sendLabel = computed(() => {
  if (waitingForMessage.value) {
    return "Working";
  }

  return hasMessage.value
      ? "Send"
      : "Generate";
});

const sendTitle = computed(() => {
  if (hasMessage.value) {
    if (
        autoReply.value &&
        debugPrompt.value
    ) {
      return "Send message, inspect prompt, and generate a response";
    }

    if (autoReply.value) {
      return "Send message and generate a response";
    }

    if (debugPrompt.value) {
      return "Send message and inspect the prompt";
    }

    return "Send message";
  }

  if (
      autoReply.value &&
      debugPrompt.value
  ) {
    return "Inspect the prompt and generate a response";
  }

  if (debugPrompt.value) {
    return "Inspect the generated prompt";
  }

  return "Generate a response";
});

async function onSend(): Promise<void> {
  if (!canSubmit.value) {
    return;
  }

  waitingForMessage.value = true;
  operationError.value = null;

  try {
    if (hasMessage.value) {
      const content =
          message.value.trim();

      const created =
          await props.newUserMessage(
              content,
          );

      if (!created) {
        operationError.value =
            "The message could not be sent.";

        return;
      }

      message.value = "";
    }

    if (
        !autoReply.value &&
        !debugPrompt.value
    ) {
      return;
    }

    const prompt =
        await props.requestPrompt();

    if (debugPrompt.value) {
      promptToDebug.value = prompt;

      await until(debugPrompt).toBe(
          false,
      );

      promptToDebug.value = null;
    }

    if (autoReply.value) {
      const generated =
          await props.generateNewMessage(
              prompt.rawRequest,
          );

      if (!generated) {
        operationError.value =
            "The response could not be generated.";
      }
    }
  } catch (error) {
    console.error(
        "Chat composer operation failed",
        error,
    );

    operationError.value =
        "The chat operation could not be completed.";
  } finally {
    waitingForMessage.value = false;
  }
}

function closePromptDebug(): void {
  debugPrompt.value = false;
  promptToDebug.value = null;
}
</script>

<template>
  <section
      class="chat-bar"
      :aria-busy="waitingForMessage"
  >
    <div class="chat-bar__composer">
      <!-- Left-side options menu -->
      <InlinePopover
          title="Generation options"
          button-label="Open generation options"
          placement="top-start"
          :disabled="waitingForMessage"
      >
        <div class="chat-bar__settings">
          <FieldEditorWrapper
              field-name="Auto reply"
              info="Generate an assistant response after sending a user message."
          >
            <BooleanToggle
                :model-value="autoReply"
                @edit="
                value =>
                  autoReply = value
              "
            />
          </FieldEditorWrapper>

          <FieldEditorWrapper
              field-name="Debug prompt"
              info="Inspect the generated prompt before model generation."
          >
            <BooleanToggle
                :model-value="debugPrompt"
                @edit="
                value =>
                  debugPrompt = value
              "
            />
          </FieldEditorWrapper>
        </div>
      </InlinePopover>

      <textarea
          v-model="message"
          class="chat-bar__input"
          :placeholder="
          `Message as ${props.characterName}…`
        "
          spellcheck="true"
          rows="2"
          aria-label="New message"
          :disabled="waitingForMessage"
          @keydown.enter.exact.prevent="onSend"
      />

      <button
          type="button"
          class="chat-bar__send"
          :class="{
          'chat-bar__send--generate':
            !hasMessage,
        }"
          :disabled="!canSubmit"
          :aria-label="sendTitle"
          :title="sendTitle"
          @click="onSend"
      >
        <span
            v-if="waitingForMessage"
            class="
            edit-box__spinner
            chat-bar__send-spinner
          "
            aria-hidden="true"
        />

        <svg
            v-else
            viewBox="0 0 24 24"
            aria-hidden="true"
        >
          <template v-if="hasMessage">
            <path
                d="m22 2-7 20-4-9-9-4Z"
            />

            <path d="M22 2 11 13" />
          </template>

          <template v-else>
            <path
                d="M12 3v3"
            />

            <path
                d="M12 18v3"
            />

            <path
                d="m4.22 4.22 2.12 2.12"
            />

            <path
                d="m17.66 17.66 2.12 2.12"
            />

            <path d="M3 12h3" />
            <path d="M18 12h3" />

            <path
                d="m4.22 19.78 2.12-2.12"
            />

            <path
                d="m17.66 6.34 2.12-2.12"
            />

            <circle
                cx="12"
                cy="12"
                r="3"
            />
          </template>
        </svg>

        <span>{{ sendLabel }}</span>
      </button>
    </div>

    <div
        v-if="operationError"
        class="chat-bar__error"
        role="alert"
    >
      <span>{{ operationError }}</span>

      <button
          type="button"
          class="chat-bar__error-dismiss"
          aria-label="Dismiss error"
          @click="operationError = null"
      >
        ×
      </button>
    </div>

    <WindowPrompt
        v-if="promptToDebug"
        title="Prompt"
        @close="closePromptDebug"
    >
      <PromptDebug
          :model-value="promptToDebug"
      />
    </WindowPrompt>
  </section>
</template>

<style scoped>
.chat-bar {
  width: 100%;
  min-width: 0;
  box-sizing: border-box;

  display: flex;
  flex-direction: column;
  gap: var(--space-1);

  color: rgb(var(--c-fg));
  font-family: var(--font-primary);
}

.chat-bar__composer {
  display: flex;
  align-items: flex-end;
  gap: var(--space-2);

  min-width: 0;
}

/* -------------------------------------------------------------------------- */
/* Input                                                                      */
/* -------------------------------------------------------------------------- */

.chat-bar__input {
  flex: 1 1 auto;

  width: 100%;
  min-width: 0;
  min-height: 2.75rem;
  max-height: 7rem;
  box-sizing: border-box;

  padding:
      var(--space-2)
      var(--space-3);

  color: rgb(var(--c-fg));
  caret-color: rgb(var(--c-accent-2));

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-surface-raised) / 0.82),
          rgb(var(--c-surface-2) / 0.58)
      );

  border:
      1px solid
      rgb(var(--c-border) / 0.38);
  border-radius: var(--radius-md);
  outline: 0;

  box-shadow:
      inset 0 1px 0
      rgb(255 255 255 / 0.3),
      0 3px 9px
      rgb(var(--c-shadow) / 0.045);

  font: inherit;
  font-size: 0.87rem;
  line-height: 1.4;

  resize: vertical;

  transition:
      border-color
      var(--duration-normal)
      var(--ease-standard),
      box-shadow
      var(--duration-normal)
      var(--ease-standard),
      background-color
      var(--duration-normal)
      var(--ease-standard);
}

.chat-bar__input:hover:not(:disabled) {
  border-color:
      rgb(var(--c-primary) / 0.5);
}

.chat-bar__input:focus {
  background:
      rgb(var(--c-surface-raised));

  border-color:
      rgb(var(--c-accent) / 0.76);

  box-shadow:
      0 0 0
      var(--focus-ring-width)
      rgb(var(--focus-ring-color) / 0.17),
      inset 0 1px 0
      rgb(255 255 255 / 0.34),
      0 4px 12px
      rgb(var(--c-shadow) / 0.07);
}

.chat-bar__input::placeholder {
  color: rgb(var(--c-muted) / 0.76);
}

.chat-bar__input:disabled {
  opacity: 0.58;
  cursor: not-allowed;
}

/* -------------------------------------------------------------------------- */
/* Primary action                                                             */
/* -------------------------------------------------------------------------- */

.chat-bar__send {
  min-width: 6.5rem;
  min-height: 2.75rem;
  flex: 0 0 auto;

  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);

  padding:
      var(--space-2)
      var(--space-3);

  color: rgb(var(--c-on-accent));

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-primary)),
          rgb(var(--c-primary-strong))
      );

  border:
      1px solid
      rgb(var(--c-accent) / 0.82);
  border-radius: var(--radius-md);

  box-shadow:
      inset 0 1px 0
      rgb(255 255 255 / 0.34),
      0 4px 12px
      rgb(var(--c-shadow) / 0.16),
      0 0 0 1px
      rgb(var(--c-primary) / 0.1);

  font: inherit;
  font-size: 0.78rem;
  font-weight: 850;
  line-height: 1;

  cursor: pointer;

  transition:
      filter
      var(--duration-fast)
      var(--ease-standard),
      box-shadow
      var(--duration-fast)
      var(--ease-standard),
      transform
      var(--duration-fast)
      var(--ease-standard);
}

.chat-bar__send--generate {
  background:
      linear-gradient(
          145deg,
          rgb(var(--c-accent)),
          rgb(var(--c-primary-strong))
      );

  border-color:
      rgb(var(--c-accent-2) / 0.9);

  box-shadow:
      inset 0 1px 0
      rgb(255 255 255 / 0.42),
      0 5px 15px
      rgb(var(--c-shadow) / 0.2),
      0 0 16px
      rgb(var(--c-accent) / 0.14);
}

.chat-bar__send:hover:not(:disabled) {
  filter: brightness(1.12);

  box-shadow:
      inset 0 1px 0
      rgb(255 255 255 / 0.42),
      0 7px 18px
      rgb(var(--c-shadow) / 0.22),
      0 0 20px
      rgb(var(--c-accent) / 0.16);

  transform: translateY(-1px);
}

.chat-bar__send:active:not(:disabled) {
  filter: brightness(0.98);
  transform: translateY(0) scale(0.98);
}

.chat-bar__send:focus-visible {
  outline:
      var(--focus-ring-width)
      solid
      rgb(var(--focus-ring-color) / 0.38);

  outline-offset: 2px;
}

.chat-bar__send:disabled {
  color:
      rgb(var(--c-muted) / 0.72);

  background:
      rgb(var(--c-surface-3) / 0.64);

  border-color:
      rgb(var(--c-border) / 0.25);

  box-shadow: none;

  opacity: 0.62;
  cursor: not-allowed;
}

.chat-bar__send svg {
  width: 1rem;
  height: 1rem;
  flex: 0 0 auto;

  fill: none;
  stroke: currentColor;
  stroke-width: 1.9;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.chat-bar__send-spinner {
  border-color:
      rgb(var(--c-on-accent) / 0.35);
  border-top-color:
      rgb(var(--c-on-accent));
}

/* -------------------------------------------------------------------------- */
/* Settings                                                                   */
/* -------------------------------------------------------------------------- */

.chat-bar__settings {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);

  min-width: 0;
}

/* -------------------------------------------------------------------------- */
/* Error                                                                      */
/* -------------------------------------------------------------------------- */

.chat-bar__error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);

  padding:
      var(--space-1)
      var(--space-2);

  color: rgb(var(--c-danger));

  background:
      rgb(var(--c-danger) / 0.08);

  border:
      1px solid
      rgb(var(--c-danger) / 0.25);
  border-radius: var(--radius-sm);

  font-size: 0.7rem;
  font-weight: 650;
  line-height: 1.35;
}

.chat-bar__error-dismiss {
  width: 1.4rem;
  height: 1.4rem;
  flex: 0 0 auto;

  display: grid;
  place-items: center;

  padding: 0;

  color: inherit;

  background: transparent;
  border: 0;
  border-radius: var(--radius-sm);

  font: inherit;
  cursor: pointer;
}

/* -------------------------------------------------------------------------- */
/* Responsive                                                                 */
/* -------------------------------------------------------------------------- */

@media (max-width: 560px) {
  .chat-bar__composer {
    display: grid;
    grid-template-columns:
      auto
      minmax(0, 1fr);
  }

  .chat-bar__input {
    grid-column: 1 / -1;
    grid-row: 1;
  }

  .chat-bar__send {
    width: 100%;
    min-width: 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .chat-bar__input,
  .chat-bar__send {
    transition: none;
  }

  .chat-bar__send:hover:not(:disabled) {
    transform: none;
  }
}
</style>