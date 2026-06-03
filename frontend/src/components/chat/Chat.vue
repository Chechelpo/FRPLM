<script setup lang="ts">
import {Message, NewMessageOrder, Session} from "@/domain/Session";
import {computed, nextTick, onMounted, ref, shallowRef, watch} from "vue";
import {Location, World} from "@/domain/World";
import {Character} from "@/domain/Characters";
import ChatMessage from "@/components/chat/ChatMessage.vue";
import {computedAsync} from "@vueuse/core";
import ChatBar from "@/components/chat/ChatBar.vue";
import {ChatCompletionRequest, ChatCompletionRole} from "@/types/ChatCompletions";
import SplitPanel from "@/components/utils/panels/SplitPanel.vue";
import ConfigSidebar from "@/components/chat/ConfigSidebar.vue";
import WindowPrompt from "@/components/utils/prompts/WindowPrompt.vue";
import PromptDebug from "@/components/chat/PromptDebug.vue";

const model = defineModel<Session>({
  required: true,
  type: Session,
});

const emit = defineEmits<{
  (e: "close"): void;
}>();

const world = computedAsync<World>(async () => await model.value.getWorld());
const last_location = shallowRef<Location>();
const user_character = computedAsync<Character>(
    async () => await model.value.getUserCharacter()
);

const messages = ref<Message[]>([]);
const messageScrollElement = ref<HTMLElement | null>(null);

const loading = ref<boolean>(false);
const loadError = ref<string | null>(null);

const cachedPrompt = ref<ChatCompletionRequest | null>(null);
const debuggingPrompt = ref<boolean>(false);

const world_name = computed<string>(() => {
  if (!world.value) return "Loading world";
  return String(world.value.get("name") ?? "Unnamed world");
});

async function scrollMessagesToBottom(): Promise<void> {
  await nextTick();

  requestAnimationFrame(() => {
    requestAnimationFrame(() => {
      const element = messageScrollElement.value;
      if (!element) return;

      element.scrollTop = element.scrollHeight;
    });
  });
}

async function loadChat(): Promise<void> {
  loading.value = true;
  loadError.value = null;

  try {
    world.value = await model.value.getWorld();
    user_character.value = await model.value.getUserCharacter();
    messages.value = await model.value.getMessages();

    if (messages.value.length > 0) {
      last_location.value = await messages.value.at(-1)!.getLocation();
    }
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : String(error);
  } finally {
    loading.value = false;
    await scrollMessagesToBottom();
  }
}

onMounted(loadChat);

watch(
    () => model.value,
    () => {
      void loadChat();
    }
);

watch(
    () => messages.value.length,
    () => {
      void scrollMessagesToBottom();
    },
    {flush: "post"}
);

async function onNewMessage(order: NewMessageOrder): Promise<void> {
  if (order.message != null) {
    console.log("Sending new user message: ", order.message);

    const previousMessage = messages.value.at(-1);

    if (!previousMessage) {
      throw new Error("Cannot create a new message without a previous message.");
    }

    const newUserMessage = await model.value.newUserMessage(
        previousMessage as Message,
        order.message
    );

    messages.value.push(newUserMessage);
    await scrollMessagesToBottom();

    messages.value.push(await model.value.generateNewMessage())
  }

  debuggingPrompt.value = order.debugPrompt;
  cachedPrompt.value = await model.value.getNewPrompt(order);
}

async function onDeleteMessage(message: Message) {
  if (!model.value.isLastMessage(message)) {
    console.debug("This is not the last message");
    return;
  }
  const confirmation = window.confirm("Are you sure you want to delete the last message?")
  if (!confirmation) return;

  let deleted = await model.value.deleteMessage(message);
  if (!deleted) {
    console.error(`Could not delete ${message}`)
    return;
  }

  console.debug(`${message} Deleted Filtering messages`)
  messages.value = messages.value.filter(other => !other.equals(message));
  void scrollMessagesToBottom();
}

function onRegenerateMessage(message: Message): void {
  console.debug("Regenerate message", message);
}

</script>

<template>
  <section class="chat-view">
    <header class="chat-header">
      <div class="chat-title">
        {{ model.get("name") }}
      </div>

      <button
          type="button"
          class="chat-close-button"
          aria-label="Close chat"
          @click="emit('close')"
      >
        X
      </button>
    </header>

    <main class="chat-body">
      <div
          v-if="loading"
          class="chat-state"
      >
        Loading chat…
      </div>

      <div
          v-else-if="loadError"
          class="chat-state chat-state-error"
      >
        {{ loadError }}
      </div>

      <SplitPanel
          v-else
          storage-key="main-chat"
          class="split-panel"
      >
        <template #left>
          <ConfigSidebar :model-value="model"/>
        </template>

        <template #right>
          <div class="chat-right-panel">
            <div
                ref="messageScrollElement"
                class="chat-message-scroll"
            >
              <div class="chat-message-stack">
                <ChatMessage
                    v-for="message in messages"
                    :key="String(message.get('tick_num'))"
                    :message="message as Message"
                    @delete="onDeleteMessage"
                    :title="message.get('role') === ChatCompletionRole.USER
                    ? String(user_character?.get('name') ?? 'User')
                    : String(world?.get('name') ?? world_name)
                  "
                />
              </div>
            </div>

            <ChatBar @send="payload => onNewMessage(payload)"/>
          </div>
        </template>
      </SplitPanel>
    </main>
  </section>

  <WindowPrompt
      v-if="cachedPrompt"
      title="Prompt"
      @close="debuggingPrompt = false; cachedPrompt = null"
  >
    <PromptDebug :model-value="cachedPrompt"/>
  </WindowPrompt>
</template>

<style scoped>
.chat-view {
  width: 100%;
  height: 100dvh;
  min-height: 0;

  display: flex;
  flex-direction: column;

  overflow: hidden;
}

.chat-header {
  flex: 0 0 auto;

  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 0.35rem;

  padding: 0.9rem 1rem;

  background: var(--secondary-background, #44403c);
}

.chat-title {
  flex: 1;
  min-width: 0;

  overflow: hidden;

  font-size: 1.15rem;
  font-weight: 700;
  line-height: 1.3;

  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-close-button {
  margin-left: auto;

  padding: 0;
  border: none;
  background: transparent;

  color: inherit;
  font: inherit;
  line-height: 1;

  cursor: pointer;
}

.chat-body {
  flex: 1 1 auto;
  min-height: 0;

  display: flex;
  flex-direction: column;

  overflow: hidden;

  padding: 1rem;
}

.split-panel {
  flex: 1 1 auto;
  min-height: 0;
  height: 100%;

  overflow: hidden;
}

.chat-right-panel {
  width: 100%;
  height: 100%;
  min-height: 0;

  display: flex;
  flex-direction: column;
  gap: 1rem;

  overflow: hidden;
}

.chat-message-scroll {
  flex: 1 1 0;
  min-height: 0;

  overflow-y: auto;
  overflow-x: hidden;

  overscroll-behavior: contain;
}

.chat-message-stack {
  min-height: 100%;

  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  gap: 1rem;
}

.chat-state {
  padding: 1rem;

  border: 1px solid color-mix(
      in srgb,
      var(--primary-accent, #f59e0b) 45%,
      transparent
  );
  border-radius: 0.75rem;

  color: var(--muted-text, #94a3b8);

  font-size: 0.95rem;
}

.chat-state-error {
  color: #fecaca;
  border-color: #b91c1c;
}
</style>