<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { Message, Session } from "@/domain/Session";
import { ChatCompletionRole } from "@/types/ChatCompletions";

const props = defineProps<{
  session: Session;
}>();

const emit = defineEmits<{
  (e: "open", value: Session): void;
  (e: "delete", value: Session): void;
}>();

const loading = ref<boolean>(false);
const lastMessage = ref<Message | null>(null);
const loadError = ref<string | null>(null);

const tickLabel = computed<string>(() => {
  const tick = lastMessage.value?.get("tick_num");

  if (tick == null) return "No ticks";

  return `Tick ${tick}`;
});

const roleLabel = computed<string>(() => {
  const role = lastMessage.value?.get("role");

  switch (role) {
    case ChatCompletionRole.USER:
      return "User";
    case ChatCompletionRole.ASSISTANT:
      return "Assistant";
    case ChatCompletionRole.SYSTEM:
      return "System";
    default:
      return "Message";
  }
});

const messageTease = computed<string>(() => {
  if (loading.value) return "Loading last message…";
  if (loadError.value) return loadError.value;
  if (!lastMessage.value) return "No messages yet.";

  const content = String(lastMessage.value.get("content") ?? "")
      .replace(/\s+/g, " ")
      .trim();

  if (content.length === 0) return "Empty message.";

  const maxLength = 120;

  return content.length > maxLength
      ? `${content.slice(0, maxLength).trim()}…`
      : content;
});

async function loadLastMessage(): Promise<void> {
  loading.value = true;
  loadError.value = null;

  try {
    const messages = await props.session.getMessages();

    if (messages.length === 0) {
      lastMessage.value = null;
      return;
    }

    const orderedMessages = [...messages].sort(
        (a, b) => Number(a.get("tick_num") ?? 0) - Number(b.get("tick_num") ?? 0)
    );

    lastMessage.value = orderedMessages.at(-1) ?? null;
  } catch (error) {
    lastMessage.value = null;
    loadError.value = error instanceof Error ? error.message : String(error);
  } finally {
    loading.value = false;
  }
}

function onOpen(): void {
  emit("open", props.session);
}

function onDelete(): void {
  emit("delete", props.session);
}

watch(
    () => props.session,
    () => {
      void loadLastMessage();
    },
    { immediate: true }
);
</script>

<template>
  <article class="session-tease-box">
    <button
        type="button"
        class="session-tease-main"
        @click="onOpen"
    >
      <div class="session-tease-header">
        <span class="session-tease-name">
          {{ props.session.get("name") }}
        </span>

        <span class="session-tease-tick">
          {{ tickLabel }}
        </span>
      </div>

      <div class="session-tease-meta">
        {{ roleLabel }}
      </div>

      <p class="session-tease-content">
        {{ messageTease }}
      </p>
    </button>

    <button
        type="button"
        class="session-tease-delete"
        aria-label="Delete session"
        title="Delete session"
        @click.stop="onDelete"
    >
      X
    </button>
  </article>
</template>

<style scoped>
.session-tease-box {
  width: 100%;
  min-width: 0;

  display: flex;
  align-items: stretch;

  overflow: hidden;

  border: 1px solid var(--primary-accent, #f59e0b);
  border-radius: 0.75rem;

  background: var(--primary-background, #1c1917);
  color: var(--primary-text, #e2e8f0);

  box-shadow:
      0 10px 15px rgb(0 0 0 / 0.18),
      inset 0 1px 0 rgb(255 255 255 / 0.06);
}

.session-tease-main {
  flex: 1;
  min-width: 0;

  display: flex;
  flex-direction: column;
  gap: 0.35rem;

  padding: 0.75rem 0.9rem;

  border: none;
  background: transparent;
  color: inherit;

  font: inherit;
  text-align: left;

  cursor: pointer;
}

.session-tease-main:hover {
  background: color-mix(
      in srgb,
      var(--primary-accent, #f59e0b) 10%,
      transparent
  );
}

.session-tease-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;

  min-width: 0;
}

.session-tease-name {
  flex: 1;
  min-width: 0;

  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;

  font-size: 0.95rem;
  font-weight: 700;
  line-height: 1.3;
}

.session-tease-tick {
  flex: 0 0 auto;

  color: var(--muted-text, #94a3b8);

  font-size: 0.75rem;
  line-height: 1.2;
}

.session-tease-meta {
  color: var(--primary-accent, #f59e0b);

  font-size: 0.75rem;
  font-weight: 700;
  line-height: 1.2;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.session-tease-content {
  margin: 0;

  overflow: hidden;

  color: var(--muted-text, #94a3b8);

  font-size: 0.85rem;
  line-height: 1.4;

  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.session-tease-delete {
  flex: 0 0 auto;

  width: 2.4rem;

  display: flex;
  align-items: center;
  justify-content: center;

  border: none;
  border-left: 1px solid color-mix(
      in srgb,
      var(--primary-accent, #f59e0b) 45%,
      transparent
  );

  background: transparent;
  color: var(--muted-text, #94a3b8);

  font: inherit;
  font-weight: 700;
  line-height: 1;

  cursor: pointer;
}

.session-tease-delete:hover {
  color: #fecaca;
  background: rgb(185 28 28 / 0.18);
}
</style>