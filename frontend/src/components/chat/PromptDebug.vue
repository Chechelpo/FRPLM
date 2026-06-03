<script setup lang="ts">
import {
  ChatCompletionRequest,
  ChatCompletionRole,
} from "@/types/ChatCompletions";

const props = defineProps<{
  modelValue: ChatCompletionRequest;
}>();

function roleLabel(role: ChatCompletionRole): string {
  switch (role) {
    case ChatCompletionRole.USER:
      return "User";
    case ChatCompletionRole.ASSISTANT:
      return "Assistant";
    case ChatCompletionRole.SYSTEM:
      return "System";
    default:
      return String(role);
  }
}

function roleClass(role: ChatCompletionRole): string {
  return `chat-request-message-role-${role}`;
}
</script>

<template>
  <section class="chat-request-viewer">
    <header class="chat-request-header">
      <div class="chat-request-title">
        Chat Completion Request
      </div>

      <div class="chat-request-model">
        Model: {{ props.modelValue.modelId }}
      </div>
    </header>

    <div class="chat-request-messages">
      <article
          v-for="(message, index) in props.modelValue.messages"
          :key="`${message.role}-${index}`"
          class="chat-request-message"
      >
        <header class="chat-request-message-header">
          <span
              class="chat-request-message-role"
              :class="roleClass(message.role)"
          >
            {{ roleLabel(message.role) }}
          </span>

          <span class="chat-request-message-index">
            #{{ index + 1 }}
          </span>
        </header>

        <pre class="chat-request-message-content">{{ message.content }}</pre>
      </article>
    </div>
  </section>
</template>

<style scoped>
.chat-request-viewer {
  width: 100%;

  display: flex;
  flex-direction: column;
  gap: 0.75rem;

  padding: 0.9rem;

  border: 1px solid var(--primary-accent, #f59e0b);
  border-radius: 0.75rem;

  background: var(--primary-background, #1c1917);
  color: var(--primary-text, #e2e8f0);

  box-shadow:
      0 10px 15px rgb(0 0 0 / 0.18),
      inset 0 1px 0 rgb(255 255 255 / 0.06);
}

.chat-request-header {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;

  padding-bottom: 0.65rem;

  border-bottom: 1px solid color-mix(
      in srgb,
      var(--primary-accent, #f59e0b) 45%,
      transparent
  );
}

.chat-request-title {
  font-size: 1rem;
  font-weight: 700;
  line-height: 1.3;
}

.chat-request-model {
  color: var(--muted-text, #94a3b8);

  font-size: 0.8rem;
  line-height: 1.35;
}

.chat-request-messages {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.chat-request-message {
  overflow: hidden;

  border: 1px solid color-mix(
      in srgb,
      var(--primary-accent, #f59e0b) 35%,
      transparent
  );
  border-radius: 0.65rem;

  background: color-mix(
      in srgb,
      var(--primary-background, #1c1917) 88%,
      black 12%
  );
}

.chat-request-message-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;

  padding: 0.45rem 0.65rem;

  background: var(--secondary-background, #44403c);
  border-bottom: 1px solid color-mix(
      in srgb,
      var(--primary-accent, #f59e0b) 35%,
      transparent
  );
}

.chat-request-message-role {
  font-size: 0.75rem;
  font-weight: 700;
  line-height: 1.2;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.chat-request-message-role-user {
  color: #93c5fd;
}

.chat-request-message-role-assistant {
  color: #86efac;
}

.chat-request-message-role-system {
  color: #fca5a5;
}

.chat-request-message-index {
  margin-left: auto;

  color: var(--muted-text, #94a3b8);

  font-size: 0.75rem;
  line-height: 1.2;
}

.chat-request-message-content {
  margin: 0;
  padding: 0.65rem;

  white-space: pre-wrap;
  overflow-wrap: anywhere;

  color: var(--primary-text, #e2e8f0);

  font: inherit;
  font-size: 0.85rem;
  line-height: 1.45;
}
</style>