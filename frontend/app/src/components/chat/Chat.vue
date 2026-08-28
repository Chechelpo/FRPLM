<script setup lang="ts">
import {     Character } from "@frplm/host-sdk";

import {
  computed,
  nextTick,
  onMounted,
  ref,
  shallowRef,
  watch,
} from "vue";

import {
  Message,
  Session,
} from "@frplm/host-sdk";
import {
  Location,
  World,
} from "@frplm/host-sdk";
import {
  type ChatCompletionRequest,
  ChatCompletionRole,
} from "@frplm/host-sdk";

import ChatMessage from "@components/chat/ChatMessage.vue";
import ChatBar from "@components/chat/ChatBar.vue";
import ConfigSidebar from "@components/chat/ConfigSidebar.vue";
import SplitPanel from "@components/utils/panels/SplitPanel.vue";
import ExpandableSplitPanel from "@components/utils/panels/ExpandableSplitPanel.vue";

type ChatOperation =
    | "sending"
    | "generating"
    | "regenerating"
    | "deleting"
    | null;

const model = defineModel<Session>({
  required: true,
  type: Session,
});

const emit = defineEmits<{
  (event: "close"): void;
}>();

const world = shallowRef<World | null>(null);
const userCharacter =
    shallowRef<Character | null>(null);
const lastLocation =
    shallowRef<Location | null>(null);

const messages = ref<Message[]>([]);

const loading = ref(false);
const loadError = ref<string | null>(null);
const operationError =
    ref<string | null>(null);
const operation = ref<ChatOperation>(null);

const messageScrollElement =
    ref<HTMLElement | null>(null);

const nearBottom = ref(true);

let loadRequestId = 0;

const sessionName = computed(() => {
  const name = String(
      model.value.get("name") ?? "",
  ).trim();

  return name || "Unnamed session";
});

const sessionId = computed(() =>
    String(model.value.get("id") ?? "—"),
);

const worldName = computed(() => {
  if (!world.value) {
    return loading.value
        ? "Loading world"
        : "Unknown world";
  }

  return String(
      world.value.get("name") ??
      "Unnamed world",
  );
});

const characterName = computed(() => {
  if (!userCharacter.value) {
    return loading.value
        ? "Loading character"
        : "User";
  }

  return String(
      userCharacter.value.get("name") ??
      "User",
  );
});

const locationName = computed(() => {
  if (!lastLocation.value) {
    return messages.value.length
        ? "Unknown location"
        : "No location";
  }

  return String(
      lastLocation.value.get("name") ??
      "Unnamed location",
  );
});

const currentTick = computed(() =>
    Number(
        model.value.get("current_tick") ??
        0,
    ),
);

const messageCount = computed(
    () => messages.value.length,
);

const isBusy = computed(
    () => operation.value !== null,
);

const operationLabel = computed(() => {
  switch (operation.value) {
    case "sending":
      return "Sending message";

    case "generating":
      return "Generating response";

    case "regenerating":
      return "Regenerating response";

    case "deleting":
      return "Deleting message";

    default:
      return "";
  }
});

const showJumpToLatest = computed(
    () =>
        messages.value.length > 0 &&
        !nearBottom.value,
);

function sortMessages(
    source: Message[],
): Message[] {
  return [...source].sort(
      (first, second) =>
          Number(
              first.get("tick_num") ?? 0,
          ) -
          Number(
              second.get("tick_num") ?? 0,
          ),
  );
}

function messageTitle(
    message: Message,
): string {
  switch (message.get("role")) {
    case ChatCompletionRole.USER:
      return characterName.value;

    case ChatCompletionRole.ASSISTANT:
      return worldName.value;

    case ChatCompletionRole.SYSTEM:
      return "System";

    default:
      return "Message";
  }
}

function isLastMessage(
    message: Message,
): boolean {
  return model.value.isLastMessage(
      message,
  );
}

function updateScrollPosition(): void {
  const element =
      messageScrollElement.value;

  if (!element) {
    nearBottom.value = true;
    return;
  }

  const remainingDistance =
      element.scrollHeight -
      element.scrollTop -
      element.clientHeight;

  nearBottom.value =
      remainingDistance < 120;
}

async function scrollMessagesToBottom(
    smooth = false,
): Promise<void> {
  await nextTick();

  window.requestAnimationFrame(() => {
    const element =
        messageScrollElement.value;

    if (!element) {
      return;
    }

    element.scrollTo({
      top: element.scrollHeight,
      behavior: smooth
          ? "smooth"
          : "auto",
    });

    nearBottom.value = true;
  });
}

async function updateLastLocation(
    message: Message | undefined,
): Promise<void> {
  if (!message) {
    lastLocation.value = null;
    return;
  }

  try {
    lastLocation.value =
        await message.getLocation();
  } catch (error) {
    console.error(
        "Could not load the latest message location",
        error,
    );

    lastLocation.value = null;
  }
}

async function loadChat(): Promise<void> {
  const requestId = ++loadRequestId;

  loading.value = true;
  loadError.value = null;
  operationError.value = null;

  world.value = null;
  userCharacter.value = null;
  lastLocation.value = null;
  messages.value = [];

  try {
    const [
      loadedWorld,
      loadedCharacter,
      loadedMessages,
    ] = await Promise.all([
      model.value.getWorld(),
      model.value.getUserCharacter(),
      model.value.getMessages(),
    ]);

    if (requestId !== loadRequestId) {
      return;
    }

    world.value = loadedWorld;
    userCharacter.value =
        loadedCharacter;
    messages.value =
        sortMessages(loadedMessages);

    await updateLastLocation(
        messages.value.at(-1),
    );
  } catch (error) {
    if (requestId !== loadRequestId) {
      return;
    }

    console.error(
        "Could not load chat",
        error,
    );

    loadError.value =
        error instanceof Error
            ? error.message
            : "The chat could not be loaded.";
  } finally {
    if (requestId === loadRequestId) {
      loading.value = false;

      await scrollMessagesToBottom();
    }
  }
}

async function onNewUserMessage(
    content: string,
): Promise<boolean> {
  if (isBusy.value) {
    return false;
  }

  const normalizedContent =
      content.trim();

  if (!normalizedContent) {
    return false;
  }

  const previousMessage =
      messages.value.at(-1);

  if (!previousMessage) {
    operationError.value =
        "A new message cannot be created because this session has no previous message.";

    return false;
  }

  operation.value = "sending";
  operationError.value = null;

  try {
    const newUserMessage =
        await model.value.newUserMessage(
            previousMessage,
            normalizedContent,
        );

    messages.value = [
      ...messages.value,
      newUserMessage,
    ];

    await updateLastLocation(
        newUserMessage,
    );

    await scrollMessagesToBottom(true);

    return true;
  } catch (error) {
    console.error(
        "Could not send user message",
        error,
    );

    operationError.value =
        "The message could not be sent.";

    return false;
  } finally {
    operation.value = null;
  }
}

async function onGenerate(
    request: ChatCompletionRequest,
): Promise<boolean> {
  if (isBusy.value) {
    return false;
  }

  operation.value = "generating";
  operationError.value = null;

  try {
    const newMessage =
        await model.value.generateNewMessage(
            request,
        );

    messages.value = [
      ...messages.value,
      newMessage,
    ];

    await updateLastLocation(
        newMessage,
    );

    await scrollMessagesToBottom(true);

    return true;
  } catch (error) {
    console.error(
        "Could not generate response",
        error,
    );

    operationError.value =
        "The response could not be generated.";

    return false;
  } finally {
    operation.value = null;
  }
}

async function onRegenerate(
    message: Message,
): Promise<boolean> {
  if (
      isBusy.value ||
      !isLastMessage(message)
  ) {
    return false;
  }

  operation.value = "regenerating";
  operationError.value = null;

  try {
    await message.regenerate();

    await updateLastLocation(message);
    await scrollMessagesToBottom(true);

    return true;
  } catch (error) {
    console.error(
        "Could not regenerate response",
        error,
    );

    operationError.value =
        "The response could not be regenerated.";

    return false;
  } finally {
    operation.value = null;
  }
}

async function onDeleteMessage(
    message: Message,
): Promise<void> {
  if (!isLastMessage(message)) {
    operationError.value =
        "Only the latest message can be deleted.";

    return;
  }

  if (isBusy.value) {
    return;
  }

  const confirmed = window.confirm(
      "Delete the latest message?",
  );

  if (!confirmed) {
    return;
  }

  operation.value = "deleting";
  operationError.value = null;

  try {
    const deleted =
        await model.value.deleteMessage(
            message,
        );

    if (!deleted) {
      operationError.value =
          "The message could not be deleted.";

      return;
    }

    messages.value =
        messages.value.filter(
            (candidate) =>
                !candidate.equals(message),
        );

    await updateLastLocation(
        messages.value.at(-1),
    );

    await scrollMessagesToBottom();
  } catch (error) {
    console.error(
        "Could not delete message",
        error,
    );

    operationError.value =
        "The message could not be deleted.";
  } finally {
    operation.value = null;
  }
}

function closeChat(): void {
  if (isBusy.value) {
    return;
  }

  emit("close");
}

onMounted(() => {
  void loadChat();
});

watch(
    () => model.value.get("id"),
    () => {
      operation.value = null;
      void loadChat();
    },
);

watch(
    () => messages.value.length,
    () => {
      if (nearBottom.value) {
        void scrollMessagesToBottom();
      }
    },
    {
      flush: "post",
    },
);
</script>

<template>
  <section
      class="chat-view"
      :aria-busy="loading || isBusy"
  >
    <header
        class="
        chat-header
        edit-box
        edit-box--accent
        edit-box--compact
      "
    >
      <div class="edit-box__header">
        <div
            class="edit-box__header-icon"
            aria-hidden="true"
        >
          <svg viewBox="0 0 24 24">
            <path
                d="M4 5h16v11H8l-4 4V5Z"
            />

            <path d="M8 9h8" />
            <path d="M8 12h5" />
          </svg>
        </div>

        <div class="edit-box__header-main">
          <span class="edit-box__eyebrow">
            Active session
          </span>

          <div class="edit-box__title-row">
            <h1 class="edit-box__title">
              {{ sessionName }}
            </h1>

            <span
                class="
                edit-box__badge
                edit-box__badge--neutral
              "
            >
              ID {{ sessionId }}
            </span>

            <span class="edit-box__count">
              Tick {{ currentTick }}
            </span>
          </div>

          <div class="chat-header__context">
            <span
                class="chat-header__context-item"
                :title="worldName"
            >
              <svg
                  viewBox="0 0 24 24"
                  aria-hidden="true"
              >
                <circle
                    cx="12"
                    cy="12"
                    r="9"
                />

                <path d="M3 12h18" />

                <path
                    d="M12 3a15 15 0 0 1 0 18"
                />
              </svg>

              {{ worldName }}
            </span>

            <span
                class="chat-header__context-item"
                :title="characterName"
            >
              <svg
                  viewBox="0 0 24 24"
                  aria-hidden="true"
              >
                <circle
                    cx="12"
                    cy="8"
                    r="4"
                />

                <path
                    d="M4 21a8 8 0 0 1 16 0"
                />
              </svg>

              {{ characterName }}
            </span>

            <span
                class="chat-header__context-item"
                :title="locationName"
            >
              <svg
                  viewBox="0 0 24 24"
                  aria-hidden="true"
              >
                <path
                    d="M20 10c0 5-8 11-8 11S4 15 4 10a8 8 0 1 1 16 0Z"
                />

                <circle
                    cx="12"
                    cy="10"
                    r="2.5"
                />
              </svg>

              {{ locationName }}
            </span>
          </div>
        </div>

        <div class="edit-box__actions">
          <span
              v-if="isBusy"
              class="
              edit-box__badge
              chat-header__operation
            "
              role="status"
              aria-live="polite"
          >
            <span
                class="edit-box__spinner"
                aria-hidden="true"
            />

            {{ operationLabel }}
          </span>

          <button
              type="button"
              class="edit-box__action"
              :disabled="isBusy"
              aria-label="Close chat"
              title="Return to sessions"
              @click="closeChat"
          >
            <svg
                viewBox="0 0 24 24"
                aria-hidden="true"
            >
              <path d="m15 18-6-6 6-6" />
            </svg>

            Sessions
          </button>
        </div>
      </div>
    </header>

    <div
        v-if="operationError"
        class="
        edit-box__state
        edit-box__state--error
        chat-view__operation-error
      "
        role="alert"
    >
      <div class="edit-box__state-content">
        <strong class="edit-box__state-title">
          Chat operation failed
        </strong>

        <p class="edit-box__state-description">
          {{ operationError }}
        </p>
      </div>

      <button
          type="button"
          class="edit-box__action"
          @click="operationError = null"
      >
        Dismiss
      </button>
    </div>

    <main class="chat-body">
      <div
          v-if="loading"
          class="
          edit-box__state
          chat-view__state
        "
          role="status"
          aria-live="polite"
      >
        <span
            class="edit-box__spinner"
            aria-hidden="true"
        />

        <div class="edit-box__state-content">
          <strong class="edit-box__state-title">
            Loading conversation
          </strong>

          <p class="edit-box__state-description">
            Retrieving the world, user character,
            and session messages.
          </p>
        </div>
      </div>

      <div
          v-else-if="loadError"
          class="
          edit-box__state
          edit-box__state--error
          edit-box__state--vertical
          chat-view__state
        "
          role="alert"
      >
        <div class="edit-box__state-icon">
          <svg
              viewBox="0 0 24 24"
              aria-hidden="true"
          >
            <path d="M12 9v4" />
            <path d="M12 17h.01" />

            <path
                d="M10.3 3.8 2.2 18a2 2 0 0 0 1.8 3h16a2 2 0 0 0 1.8-3L13.7 3.8a2 2 0 0 0-3.4 0Z"
            />
          </svg>
        </div>

        <div class="edit-box__state-content">
          <strong class="edit-box__state-title">
            Could not load conversation
          </strong>

          <p class="edit-box__state-description">
            {{ loadError }}
          </p>
        </div>

        <div class="chat-view__state-actions">
          <button
              type="button"
              class="edit-box__action"
              @click="closeChat"
          >
            Return to sessions
          </button>

          <button
              type="button"
              class="
              edit-box__action
              edit-box__action--accent
            "
              @click="loadChat"
          >
            Retry
          </button>
        </div>
      </div>

      <ExpandableSplitPanel
          v-else
          storage-key="main-chat"
          class="chat-layout"
      >
        <template #left>
          <aside
              class="chat-layout__sidebar"
              aria-label="Session configuration"
          >
            <ConfigSidebar
                :model-value="model"
            />
          </aside>
        </template>

        <template #right>
          <section
              class="chat-conversation"
              aria-label="Conversation"
          >
            <div
                ref="messageScrollElement"
                class="chat-conversation__scroll"
                @scroll="updateScrollPosition"
            >
              <div
                  v-if="messages.length"
                  class="
                  chat-conversation__messages
                "
              >
                <ChatMessage
                    v-for="message in messages"
                    :key="message.hashKey()"
                    :message="message"
                    :title="
                    messageTitle(message)
                  "
                    :on-regenerate="
                    onRegenerate
                  "
                    :is-last-message="
                    isLastMessage(message)
                  "
                    @delete="
                    onDeleteMessage
                  "
                />
              </div>

              <div
                  v-else
                  class="
                  edit-box__state
                  edit-box__state--vertical
                  chat-conversation__empty
                "
              >
                <div
                    class="edit-box__state-icon"
                >
                  <svg
                      viewBox="0 0 24 24"
                      aria-hidden="true"
                  >
                    <path
                        d="M4 5h16v11H8l-4 4V5Z"
                    />

                    <path d="M9 10h6" />
                  </svg>
                </div>

                <div
                    class="
                    edit-box__state-content
                  "
                >
                  <strong
                      class="
                      edit-box__state-title
                    "
                  >
                    No messages
                  </strong>

                  <p
                      class="
                      edit-box__state-description
                    "
                  >
                    This session does not contain
                    any messages yet.
                  </p>
                </div>
              </div>
            </div>

            <button
                v-if="showJumpToLatest"
                type="button"
                class="
                edit-box__action
                chat-conversation__jump
              "
                @click="
                scrollMessagesToBottom(true)
              "
            >
              <svg
                  viewBox="0 0 24 24"
                  aria-hidden="true"
              >
                <path d="M12 5v14" />
                <path d="m7 14 5 5 5-5" />
              </svg>

              Latest message
            </button>

            <footer
                class="chat-conversation__composer"
            >
              <div
                  class="
                  chat-conversation__composer-status
                "
              >
                <span>
                  {{ messageCount }}
                  {{
                    messageCount === 1
                        ? "message"
                        : "messages"
                  }}
                </span>

                <span
                    v-if="isBusy"
                    role="status"
                    aria-live="polite"
                >
                  {{ operationLabel }}…
                </span>
              </div>

              <ChatBar
                  v-if="userCharacter"
                  :character-name="
                  characterName
                "
                  :new-user-message="
                  onNewUserMessage
                "
                  :request-prompt="
                  () =>
                    model.getNewPrompt()
                "
                  :generate-new-message="
                  onGenerate
                "
              />

              <div
                  v-else
                  class="
                  edit-box__state
                  chat-conversation__character-error
                "
              >
                The session user character could
                not be loaded.
              </div>
            </footer>
          </section>
        </template>
      </ExpandableSplitPanel>
    </main>
  </section>
</template>

<style scoped>
.chat-view {
  /*
   * App.vue may override --app-header-height globally.
   * The fallback matches the compact persistent header.
   */
  height:
      calc(
          100dvh -
          var(--app-header-height, 4.75rem)
      );

  min-width: 0;
  min-height: 32rem;
  box-sizing: border-box;

  display: flex;
  flex-direction: column;
  gap: var(--space-2);

  padding: var(--space-2);

  overflow: hidden;

  color: rgb(var(--c-fg));
  font-family: var(--font-primary);
}

/* -------------------------------------------------------------------------- */
/* Header                                                                     */
/* -------------------------------------------------------------------------- */

.chat-header {
  flex: 0 0 auto;

  min-width: 0;
}

.chat-header
.edit-box__header {
  align-items: flex-start;
}

.chat-header
.edit-box__action
svg {
  width: 1rem;
  height: 1rem;

  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.chat-header__context {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-2);

  min-width: 0;

  margin-top: var(--space-2);
}

.chat-header__context-item {
  min-width: 0;
  max-width: 15rem;

  display: inline-flex;
  align-items: center;
  gap: var(--space-1);

  padding:
      0.22rem
      0.48rem;

  overflow: hidden;

  color: rgb(var(--c-muted));

  background:
      rgb(var(--c-surface-raised) / 0.44);

  border:
      1px solid
      rgb(var(--c-border) / 0.2);
  border-radius: var(--radius-round);

  font-size: 0.7rem;
  font-weight: 700;
  line-height: 1.3;

  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-header__context-item svg {
  width: 0.85rem;
  height: 0.85rem;
  flex: 0 0 auto;

  color:
      rgb(var(--c-primary-strong));

  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.chat-header__operation {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
}

/* -------------------------------------------------------------------------- */
/* Global error                                                               */
/* -------------------------------------------------------------------------- */

.chat-view__operation-error {
  min-height: auto;
  flex: 0 0 auto;

  justify-content: flex-start;

  padding: var(--space-3);

  text-align: left;
}

/* -------------------------------------------------------------------------- */
/* Chat body                                                                  */
/* -------------------------------------------------------------------------- */

.chat-body {
  flex: 1 1 auto;

  display: flex;
  flex-direction: column;

  min-width: 0;
  min-height: 0;

  overflow: hidden;
}

.chat-view__state {
  flex: 1 1 auto;

  min-height: 18rem;
}

.chat-view__state-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: var(--space-2);
}

/* -------------------------------------------------------------------------- */
/* Split layout                                                               */
/* -------------------------------------------------------------------------- */

.chat-layout {
  flex: 1 1 auto;

  width: 100%;
  min-width: 0;
  min-height: 0;
  height: 100%;

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-surface-raised) / 0.34),
          rgb(var(--c-surface-2) / 0.2)
      );

  border:
      1px solid
      rgb(var(--c-border) / 0.25);
  border-radius: var(--radius-md);

  box-shadow:
      inset 0 1px 0
      rgb(255 255 255 / 0.26),
      0 5px 18px
      rgb(var(--c-shadow) / 0.055);

  overflow: hidden;
}

.chat-layout__sidebar {
  width: 100%;
  min-width: 0;
  height: 100%;
  box-sizing: border-box;

  overflow: hidden;
}

/* -------------------------------------------------------------------------- */
/* Conversation                                                               */
/* -------------------------------------------------------------------------- */

.chat-conversation {
  position: relative;

  width: 100%;
  min-width: 0;
  height: 100%;
  min-height: 0;
  box-sizing: border-box;

  display: flex;
  flex-direction: column;

  overflow: hidden;

  background:
      linear-gradient(
          155deg,
          rgb(var(--c-page) / 0.22),
          rgb(var(--c-surface-2) / 0.18)
      );
}

.chat-conversation__scroll {
  flex: 1 1 auto;

  min-width: 0;
  min-height: 0;

  overflow-x: hidden;
  overflow-y: auto;
  overscroll-behavior: contain;

  padding: var(--space-4);

  scroll-padding:
      var(--space-4)
      var(--space-4)
      var(--space-6);

  scrollbar-width: thin;
  scrollbar-color:
      rgb(var(--c-primary) / 0.42)
      transparent;
}

.chat-conversation__scroll::-webkit-scrollbar {
  width: 0.7rem;
}

.chat-conversation__scroll::-webkit-scrollbar-track {
  background: transparent;
}

.chat-conversation__scroll::-webkit-scrollbar-thumb {
  background:
      rgb(var(--c-primary) / 0.36);

  border: 2px solid transparent;
  border-radius: var(--radius-round);
  background-clip: padding-box;
}

.chat-conversation__scroll::-webkit-scrollbar-thumb:hover {
  background:
      rgb(var(--c-primary) / 0.56);

  border: 2px solid transparent;
  background-clip: padding-box;
}

.chat-conversation__messages {
  min-height: 100%;

  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  gap: var(--space-4);
}

.chat-conversation__empty {
  min-height: 100%;
}

.chat-conversation__jump {
  position: absolute;
  right: var(--space-4);
  bottom: 8.25rem;
  z-index: 4;

  box-shadow:
      0 5px 16px
      rgb(var(--c-shadow) / 0.14);

  backdrop-filter: blur(8px);
}

.chat-conversation__jump svg {
  width: 0.95rem;
  height: 0.95rem;

  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

/* -------------------------------------------------------------------------- */
/* Composer                                                                   */
/* -------------------------------------------------------------------------- */

.chat-conversation__composer {
  position: relative;
  z-index: 3;

  flex: 0 0 auto;

  min-width: 0;

  padding:
      var(--space-2)
      var(--space-3)
      var(--space-3);

  background:
      linear-gradient(
          180deg,
          rgb(var(--c-surface) / 0.72),
          rgb(var(--c-surface-raised) / 0.92)
      );

  border-top:
      1px solid
      rgb(var(--c-border) / 0.28);

  box-shadow:
      0 -7px 20px
      rgb(var(--c-shadow) / 0.055);

  backdrop-filter: blur(12px);
}

.chat-conversation__composer-status {
  min-height: 1.2rem;

  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);

  margin-bottom: var(--space-1);

  color: rgb(var(--c-muted));

  font-size: 0.66rem;
  font-weight: 650;
  line-height: 1.3;
}

.chat-conversation__character-error {
  min-height: 4rem;

  padding: var(--space-3);
}

/* -------------------------------------------------------------------------- */
/* Responsive                                                                 */
/* -------------------------------------------------------------------------- */

@media (max-width: 760px) {
  .chat-view {
    padding: var(--space-1);
  }

  .chat-header__context-item {
    max-width: 10rem;
  }

  .chat-conversation__scroll {
    padding: var(--space-3);
  }

  .chat-conversation__messages {
    gap: var(--space-3);
  }

  .chat-conversation__jump {
    right: var(--space-3);
  }
}

@media (max-width: 520px) {
  .chat-view {
    min-height: 28rem;
  }

  .chat-header
  .edit-box__header {
    align-items: stretch;
    flex-direction: column;
  }

  .chat-header
  .edit-box__actions {
    width: 100%;
    justify-content: space-between;
  }

  .chat-header__context-item {
    max-width: 100%;
  }

  .chat-conversation__scroll {
    padding: var(--space-2);
  }

  .chat-conversation__composer {
    padding: var(--space-2);
  }

  .chat-conversation__jump {
    right: var(--space-2);
    bottom: 8rem;
  }
}

@media (prefers-reduced-motion: reduce) {
  .chat-conversation__scroll {
    scroll-behavior: auto;
  }
}
</style>