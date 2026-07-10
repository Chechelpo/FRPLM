<script setup lang="ts">
import {
  computed,
  ref,
  shallowRef,
  watch,
} from "vue";

import {
  Message,
  Session,
} from "@/domain/Session";
import { World } from "@/domain/World";
import { Character } from "@/domain/Characters";
import { ChatCompletionRole } from "@/types/ChatCompletions";

const props = withDefaults(
    defineProps<{
      session: Session;
      deleting?: boolean;
    }>(),
    {
      deleting: false,
    },
);

const emit = defineEmits<{
  (event: "open", value: Session): void;
  (event: "delete", value: Session): void;
}>();

const loading = ref(false);
const loadError = ref<string | null>(null);
const partialLoadError = ref(false);

const lastMessage =
    shallowRef<Message | null>(null);
const world =
    shallowRef<World | null>(null);
const userCharacter =
    shallowRef<Character | null>(null);

let loadRequestId = 0;

const sessionName = computed(() => {
  const name = String(
      props.session.get("name") ?? "",
  ).trim();

  return name || "Unnamed session";
});

const sessionId = computed(() =>
    String(props.session.get("id") ?? "—"),
);

const currentTick = computed(() =>
    Number(
        props.session.get("current_tick") ??
        0,
    ),
);

const tickLabel = computed(() =>
    currentTick.value > 0
        ? `Tick ${currentTick.value}`
        : "No ticks",
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
        : "Unknown character";
  }

  return String(
      userCharacter.value.get("name") ??
      "Unnamed character",
  );
});

const roleLabel = computed(() => {
  const role =
      lastMessage.value?.get("role");

  switch (role) {
    case ChatCompletionRole.USER:
      return "User";

    case ChatCompletionRole.ASSISTANT:
      return "Assistant";

    case ChatCompletionRole.SYSTEM:
      return "System";

    default:
      return "No messages";
  }
});

const roleBadgeClass = computed(() => {
  const role =
      lastMessage.value?.get("role");

  switch (role) {
    case ChatCompletionRole.ASSISTANT:
      return "edit-box__badge--success";

    case ChatCompletionRole.SYSTEM:
      return "edit-box__badge--warning";

    case ChatCompletionRole.USER:
      return "";

    default:
      return "edit-box__badge--neutral";
  }
});

const messagePreview = computed(() => {
  if (loading.value) {
    return "Loading session summary...";
  }

  if (
      loadError.value &&
      !lastMessage.value
  ) {
    return loadError.value;
  }

  if (!lastMessage.value) {
    return "No messages have been created in this session.";
  }

  const content = String(
      lastMessage.value.get("content") ??
      "",
  )
      .replace(/\s+/g, " ")
      .trim();

  if (!content) {
    return "The latest message is empty.";
  }

  const maximumLength = 180;

  return content.length > maximumLength
      ? `${content
          .slice(0, maximumLength)
          .trim()}…`
      : content;
});

async function loadSessionSummary(): Promise<void> {
  const requestId = ++loadRequestId;

  loading.value = true;
  loadError.value = null;
  partialLoadError.value = false;

  lastMessage.value = null;
  world.value = null;
  userCharacter.value = null;

  const [
    messagesResult,
    worldResult,
    characterResult,
  ] = await Promise.allSettled([
    props.session.getMessages(),
    props.session.getWorld(),
    props.session.getUserCharacter(),
  ]);

  if (requestId !== loadRequestId) {
    return;
  }

  if (
      messagesResult.status === "fulfilled"
  ) {
    const orderedMessages = [
      ...messagesResult.value,
    ].sort(
        (first, second) =>
            Number(
                first.get("tick_num") ?? 0,
            ) -
            Number(
                second.get("tick_num") ?? 0,
            ),
    );

    lastMessage.value =
        orderedMessages.at(-1) ?? null;
  } else {
    console.error(
        "Could not load session messages",
        messagesResult.reason,
    );

    partialLoadError.value = true;
  }

  if (
      worldResult.status === "fulfilled"
  ) {
    world.value = worldResult.value;
  } else {
    console.error(
        "Could not load session world",
        worldResult.reason,
    );

    partialLoadError.value = true;
  }

  if (
      characterResult.status ===
      "fulfilled"
  ) {
    userCharacter.value =
        characterResult.value;
  } else {
    console.error(
        "Could not load session character",
        characterResult.reason,
    );

    partialLoadError.value = true;
  }

  if (
      messagesResult.status ===
      "rejected" &&
      worldResult.status === "rejected" &&
      characterResult.status ===
      "rejected"
  ) {
    loadError.value =
        "The session summary could not be loaded.";
  }

  loading.value = false;
}

function onOpen(): void {
  if (
      props.deleting ||
      loading.value
  ) {
    return;
  }

  emit("open", props.session);
}

function onDelete(): void {
  emit("delete", props.session);
}

watch(
    () => [
      props.session,
      props.session.get("current_tick"),
    ],
    () => {
      void loadSessionSummary();
    },
    {
      immediate: true,
    },
);
</script>

<template>
  <article
      class="
      session-card
      edit-box
      edit-box--primary
      edit-box--compact
    "
      :aria-busy="loading || deleting"
  >
    <button
        type="button"
        class="session-card__main"
        :disabled="deleting"
        @click="onOpen"
    >
      <span
          class="session-card__icon"
          aria-hidden="true"
      >
        <svg viewBox="0 0 24 24">
          <path
              d="M4 5h16v11H8l-4 4V5Z"
          />

          <path d="M8 9h8" />
          <path d="M8 12h5" />
        </svg>
      </span>

      <span class="session-card__identity">
        <span class="session-card__name-row">
          <span class="session-card__name">
            {{ sessionName }}
          </span>

          <span
              class="
              edit-box__badge
              edit-box__badge--neutral
            "
          >
            {{ tickLabel }}
          </span>
        </span>

        <span
            class="session-card__identifier"
        >
          Session ID {{ sessionId }}
        </span>
      </span>

      <svg
          class="session-card__open-icon"
          viewBox="0 0 24 24"
          aria-hidden="true"
      >
        <path d="m9 18 6-6-6-6" />
      </svg>
    </button>

    <div class="session-card__context">
      <div class="session-card__context-item">
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

        <span>{{ worldName }}</span>
      </div>

      <div class="session-card__context-item">
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

        <span>{{ characterName }}</span>
      </div>
    </div>

    <section class="session-card__preview">
      <header class="session-card__preview-header">
        <span class="session-card__preview-label">
          Latest message
        </span>

        <span
            class="edit-box__badge"
            :class="roleBadgeClass"
        >
          {{ roleLabel }}
        </span>
      </header>

      <p class="session-card__preview-content">
        {{ messagePreview }}
      </p>
    </section>

    <footer class="session-card__footer">
      <span
          v-if="partialLoadError"
          class="
          edit-box__badge
          edit-box__badge--warning
        "
          title="Some session metadata could not be loaded."
      >
        Partial data
      </span>

      <span
          v-else-if="loading"
          class="
          edit-box__badge
          edit-box__badge--neutral
        "
      >
        Loading
      </span>

      <span v-else />

      <div class="session-card__actions">
        <button
            type="button"
            class="edit-box__action"
            :disabled="deleting || loading"
            @click="onOpen"
        >
          Open
        </button>

        <button
            type="button"
            class="
            edit-box__action
            edit-box__action--danger
            session-card__delete
          "
            :disabled="deleting"
            :aria-label="
            `Delete ${sessionName}`
          "
            :title="
            `Delete ${sessionName}`
          "
            @click="onDelete"
        >
          <span
              v-if="deleting"
              class="edit-box__spinner"
              aria-hidden="true"
          />

          <svg
              v-else
              viewBox="0 0 24 24"
              aria-hidden="true"
          >
            <path d="M3 6h18" />
            <path d="M8 6V4h8v2" />

            <path
                d="m19 6-1 14H6L5 6"
            />

            <path d="M10 11v5" />
            <path d="M14 11v5" />
          </svg>
        </button>
      </div>
    </footer>
  </article>
</template>

<style scoped>
.session-card {
  min-width: 0;
  height: 100%;

  display: flex;
  flex-direction: column;

  overflow: hidden;
}

/* -------------------------------------------------------------------------- */
/* Main identity                                                              */
/* -------------------------------------------------------------------------- */

.session-card__main {
  width: 100%;
  min-width: 0;

  display: flex;
  align-items: center;
  gap: var(--space-3);

  padding: var(--space-4);

  color: inherit;

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-surface-raised) / 0.58),
          rgb(var(--c-surface-2) / 0.3)
      );

  border: 0;
  border-bottom:
      1px solid
      rgb(var(--c-border) / 0.22);
  outline: 0;

  font: inherit;
  text-align: left;

  cursor: pointer;

  transition:
      background-color
      var(--duration-fast)
      var(--ease-standard);
}

.session-card__main:hover:not(:disabled) {
  background:
      linear-gradient(
          145deg,
          rgb(var(--c-surface-hover) / 0.82),
          rgb(var(--c-accent) / 0.08)
      );
}

.session-card__main:focus-visible {
  box-shadow:
      inset 0 0 0
      var(--focus-ring-width)
      rgb(var(--focus-ring-color) / 0.28);
}

.session-card__main:disabled {
  opacity: 0.62;
  cursor: not-allowed;
}

.session-card__icon {
  width: 2.65rem;
  height: 2.65rem;
  flex: 0 0 auto;

  display: grid;
  place-items: center;

  color: rgb(var(--c-primary-strong));

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-accent) / 0.2),
          rgb(var(--c-primary) / 0.09)
      );

  border:
      1px solid
      rgb(var(--c-accent) / 0.3);
  border-radius: var(--radius-md);
}

.session-card__icon svg {
  width: 1.3rem;
  height: 1.3rem;

  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.session-card__identity {
  flex: 1 1 auto;
  min-width: 0;

  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.session-card__name-row {
  display: flex;
  align-items: center;
  gap: var(--space-2);

  min-width: 0;
}

.session-card__name {
  flex: 1 1 auto;
  min-width: 0;

  overflow: hidden;

  color: rgb(var(--c-fg-strong));

  font-size: 0.95rem;
  font-weight: 825;
  line-height: 1.3;

  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-card__identifier {
  color: rgb(var(--c-muted));

  font-family: var(--font-monospace);
  font-size: 0.66rem;
  font-weight: 600;
  line-height: 1.3;
}

.session-card__open-icon {
  width: 1rem;
  height: 1rem;
  flex: 0 0 auto;

  color: rgb(var(--c-muted));

  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

/* -------------------------------------------------------------------------- */
/* Session context                                                            */
/* -------------------------------------------------------------------------- */

.session-card__context {
  display: grid;
  grid-template-columns:
    repeat(2, minmax(0, 1fr));

  gap: var(--space-2);

  padding: var(--space-3);

  background:
      rgb(var(--c-surface) / 0.2);

  border-bottom:
      1px solid
      rgb(var(--c-border) / 0.2);
}

.session-card__context-item {
  min-width: 0;

  display: flex;
  align-items: center;
  gap: var(--space-2);

  padding:
      var(--space-2)
      var(--space-3);

  color: rgb(var(--c-muted));

  background:
      rgb(var(--c-surface-raised) / 0.46);

  border:
      1px solid
      rgb(var(--c-border) / 0.18);
  border-radius: var(--radius-sm);

  font-size: 0.72rem;
  font-weight: 700;
  line-height: 1.3;
}

.session-card__context-item span {
  min-width: 0;

  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-card__context-item svg {
  width: 0.95rem;
  height: 0.95rem;
  flex: 0 0 auto;

  color: rgb(var(--c-primary-strong));

  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

/* -------------------------------------------------------------------------- */
/* Message preview                                                            */
/* -------------------------------------------------------------------------- */

.session-card__preview {
  flex: 1 1 auto;

  min-height: 7.25rem;

  padding: var(--space-3);

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-surface-raised) / 0.34),
          rgb(var(--c-surface-2) / 0.18)
      );
}

.session-card__preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);

  margin-bottom: var(--space-2);
}

.session-card__preview-label {
  color: rgb(var(--c-muted));

  font-size: 0.66rem;
  font-weight: 800;
  line-height: 1.2;

  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.session-card__preview-content {
  display: -webkit-box;

  margin: 0;
  overflow: hidden;

  color: rgb(var(--c-fg));

  font-size: 0.8rem;
  line-height: 1.55;

  overflow-wrap: anywhere;

  -webkit-box-orient: vertical;
  -webkit-line-clamp: 4;
}

/* -------------------------------------------------------------------------- */
/* Footer                                                                     */
/* -------------------------------------------------------------------------- */

.session-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);

  min-height: 3rem;
  box-sizing: border-box;

  padding: var(--space-2);

  background:
      rgb(var(--c-surface-2) / 0.28);

  border-top:
      1px solid
      rgb(var(--c-border) / 0.2);
}

.session-card__actions {
  flex: 0 0 auto;

  display: flex;
  align-items: center;
  gap: var(--space-1);
}

.session-card__actions
.edit-box__action {
  min-height: 2rem;

  padding:
      0.35rem
      0.65rem;
}

.session-card__delete {
  width: 2rem;
  padding: 0;
}

.session-card__delete svg {
  width: 0.95rem;
  height: 0.95rem;

  fill: none;
  stroke: currentColor;
  stroke-width: 1.9;
  stroke-linecap: round;
  stroke-linejoin: round;
}

@media (max-width: 400px) {
  .session-card__context {
    grid-template-columns: 1fr;
  }
}

@media (prefers-reduced-motion: reduce) {
  .session-card__main {
    transition: none;
  }
}
</style>