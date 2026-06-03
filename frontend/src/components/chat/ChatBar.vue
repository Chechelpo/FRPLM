<script setup lang="ts">
import BooleanToggle from "@/components/utils/primitives/BooleanToggle.vue";
import { computed, ref } from "vue";
import { NewMessageOrder } from "@/domain/Session";

/**
 * In charge of making new user generated messages, as well as triggering new generations.
 */

const emit = defineEmits<{
  (e: "send", payload: NewMessageOrder): void;
}>();

const debugPrompt = ref<boolean>(false);
const message = ref<string>("");

const canSend = computed<boolean>(() => message.value.trim().length > 0);

function onSend(): void {
  if (!canSend.value) return;

  emit("send", {
    debugPrompt: debugPrompt.value,
    message: message.value,
  });

  message.value = "";
}

function onDebugPromptEdit(value: boolean): void {
  debugPrompt.value = value;
}

function onMessageInput(event: Event): void {
  message.value = (event.target as HTMLTextAreaElement).value;
}
</script>

<template>
  <section class="chat-bar">
    <div class="chat-bar-options">
      <label class="chat-bar-debug-option">
        <span class="chat-bar-debug-label">
          Debug prompt
        </span>

        <BooleanToggle
            :model-value="debugPrompt"
            @edit="onDebugPromptEdit"
        />
      </label>
    </div>

    <div class="chat-bar-composer">
      <textarea
          :value="message"
          class="chat-bar-input"
          placeholder="Write..."
          spellcheck="true"
          rows="2"
          aria-label="New message"
          @input="onMessageInput"
          @keydown.enter.exact.prevent="onSend"
      />

      <button
          type="button"
          class="chat-bar-send-button"
          :disabled="!canSend"
          @click="onSend"
      >
        Send
      </button>
    </div>
  </section>
</template>

<style scoped>
.chat-bar {
  width: 100%;
  overflow-x: clip;

  display: flex;
  flex-direction: column;
  gap: 0.35rem;

  padding: 0.45rem 0.55rem;

  border: 1px solid color-mix(
      in srgb,
      var(--primary-accent, #f59e0b) 55%,
      transparent
  );
  border-radius: 0.55rem;

  background: var(--primary-background, #1c1917);
  color: var(--primary-text, #e2e8f0);

  box-shadow:
      0 4px 8px rgb(0 0 0 / 0.14),
      inset 0 1px 0 rgb(255 255 255 / 0.04);
}

.chat-bar-options {
  display: flex;
  justify-content: flex-end;
}

.chat-bar-debug-option {
  display: flex;
  align-items: center;
  gap: 0.35rem;

  color: var(--muted-text, #94a3b8);

  font-size: 0.75rem;
  line-height: 1.2;
}

.chat-bar-debug-label {
  user-select: none;
}

.chat-bar-composer {
  display: flex;
  align-items: flex-end;
  gap: 0.4rem;
}

.chat-bar-input {
  flex: 1;
  min-width: 0;
  min-height: 2.6rem;
  max-height: 7rem;

  resize: vertical;

  padding: 0.45rem 0.55rem;

  border: 1px solid color-mix(
      in srgb,
      var(--primary-accent, #f59e0b) 35%,
      transparent
  );
  border-radius: 0.45rem;
  outline: none;

  background: color-mix(
      in srgb,
      var(--primary-background, #1c1917) 90%,
      black 10%
  );

  color: var(--primary-text, #e2e8f0);

  font: inherit;
  font-size: 0.85rem;
  line-height: 1.35;
}

.chat-bar-input:focus {
  border-color: var(--primary-accent, #f59e0b);
  box-shadow: inset 0 0 0 1px var(--primary-accent, #f59e0b);
}

.chat-bar-input::placeholder {
  color: var(--muted-text, #94a3b8);
  opacity: 0.7;
}

.chat-bar-send-button {
  flex: 0 0 auto;

  padding: 0.45rem 0.7rem;

  border: 1px solid var(--primary-accent, #f59e0b);
  border-radius: 0.45rem;

  background: transparent;
  color: var(--primary-text, #e2e8f0);

  font: inherit;
  font-size: 0.8rem;
  font-weight: 700;
  line-height: 1.1;

  cursor: pointer;
}

.chat-bar-send-button:hover:not(:disabled) {
  background: color-mix(
      in srgb,
      var(--primary-accent, #f59e0b) 14%,
      transparent
  );
}

.chat-bar-send-button:disabled {
  opacity: 0.45;
  cursor: default;
}
</style>