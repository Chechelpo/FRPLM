<script setup lang="ts">
import {     Character,
    World,
    EntityTypes } from "@frplm/host-sdk";

import {
  computed,
  onMounted,
  ref,
  shallowRef,
} from "vue";

import {
  Session,
  type SessionData,
  type SessionKey,
} from "@frplm/host-sdk";

import {
  deleteEntity,
  fetch_all,
} from "@frplm/host-sdk";

import Chat from "@components/chat/Chat.vue";
import SearchBar from "@components/utils/SearchBar.vue";
import NewSessionPopUp from "@components/session/NewSessionPopUp.vue";
import SessionTease from "@components/session/SessionTease.vue";

interface NewSessionPayload {
  name: string;
  world: World;
  character: Character;
}

const sessions = ref<Session[]>([]);
const loadingSessions = ref(false);
const sessionLoadError = ref<string | null>(null);
const sessionOperationError = ref<string | null>(null);

const filteringTerm = ref("");
const creatingSession = ref(false);
const createNewSession = ref(false);

const deletingSessionIds = ref<Set<number>>(
    new Set(),
);

const sessionSelected =
    shallowRef<Session | null>(null);

/**
 * Forces session previews to reload after closing Chat, because Session mutates
 * its internal current_tick without replacing the Session instance.
 */
const sessionListRevision = ref(0);

function normalize(value: unknown): string {
  return String(value ?? "")
      .trim()
      .toLocaleLowerCase()
      .normalize("NFKD")
      .replace(/\p{Diacritic}/gu, "");
}

function getSessionId(
    session: Session,
): number {
  return Number(session.get("id"));
}

function getSessionName(
    session: Session,
): string {
  const name = String(
      session.get("name") ?? "",
  ).trim();

  return name || "Unnamed session";
}

const filteredSessions = computed<Session[]>(
    () => {
      // Explicit dependency for updates after returning from Chat.
      sessionListRevision.value;

      const query = normalize(
          filteringTerm.value,
      );

      return [...sessions.value]
          .filter((session) => {
            if (!query) {
              return true;
            }

            const name = normalize(
                getSessionName(session),
            );

            const id = normalize(
                session.get("id"),
            );

            return (
                name.includes(query) ||
                id.includes(query)
            );
          })
          .sort((first, second) => {
            const tickDifference =
                Number(
                    second.get("current_tick") ?? 0,
                ) -
                Number(
                    first.get("current_tick") ?? 0,
                );

            if (tickDifference !== 0) {
              return tickDifference;
            }

            return getSessionName(
                first,
            ).localeCompare(
                getSessionName(second),
            );
          });
    },
);

const sessionCountLabel = computed(() => {
  if (!filteringTerm.value.trim()) {
    return String(sessions.value.length);
  }

  return (
      `${filteredSessions.value.length}` +
      ` of ${sessions.value.length}`
  );
});

async function loadSessions(): Promise<void> {
  loadingSessions.value = true;
  sessionLoadError.value = null;

  try {
    sessions.value = await fetch_all<
        SessionKey,
        SessionData,
        Session
    >(
        EntityTypes.SESSIONS,
        Session,
    );

    sessionListRevision.value += 1;
  } catch (error) {
    console.error(
        "Could not load sessions",
        error,
    );

    sessionLoadError.value =
        error instanceof Error
            ? error.message
            : "The sessions could not be loaded.";
  } finally {
    loadingSessions.value = false;
  }
}

async function handleCreateSession(
    payload: NewSessionPayload,
): Promise<void> {
  creatingSession.value = true;
  sessionOperationError.value = null;

  try {
    const newSession =
        await Session.newSession(
            payload.name.trim(),
            payload.world,
            payload.character,
        );

    sessions.value = [
      newSession,
      ...sessions.value,
    ];

    createNewSession.value = false;
    sessionSelected.value = newSession;
  } catch (error) {
    console.error(
        "Could not create session",
        error,
    );

    sessionOperationError.value =
        "The new session could not be created.";
  } finally {
    creatingSession.value = false;
  }
}

function openSession(
    session: Session,
): void {
  sessionOperationError.value = null;
  sessionSelected.value = session;
}

function closeSession(): void {
  sessionSelected.value = null;
  sessionListRevision.value += 1;
}

function isDeletingSession(
    session: Session,
): boolean {
  return deletingSessionIds.value.has(
      getSessionId(session),
  );
}

function setDeletingSession(
    session: Session,
    deleting: boolean,
): void {
  const nextIds = new Set(
      deletingSessionIds.value,
  );

  const id = getSessionId(session);

  if (deleting) {
    nextIds.add(id);
  } else {
    nextIds.delete(id);
  }

  deletingSessionIds.value = nextIds;
}

async function deleteSession(
    session: Session,
): Promise<void> {
  const name = getSessionName(session);

  const confirmed = window.confirm(
      `Delete session "${name}"? This action cannot be undone.`,
  );

  if (!confirmed) {
    return;
  }

  setDeletingSession(session, true);
  sessionOperationError.value = null;

  try {
    const deleted =
        await deleteEntity<SessionKey>(
            session.key,
            EntityTypes.SESSIONS,
        );

    if (!deleted) {
      sessionOperationError.value =
          `${name} could not be deleted.`;

      return;
    }

    sessions.value =
        sessions.value.filter(
            (candidate) =>
                !candidate.equals(session),
        );

    sessionListRevision.value += 1;
  } catch (error) {
    console.error(
        "Could not delete session",
        error,
    );

    sessionOperationError.value =
        `${name} could not be deleted.`;
  } finally {
    setDeletingSession(session, false);
  }
}

function clearFilter(): void {
  filteringTerm.value = "";
}

onMounted(() => {
  void loadSessions();
});
</script>

<template>
  <Chat
      v-if="sessionSelected"
      :key="sessionSelected.get('id')"
      :model-value="sessionSelected"
      @close="closeSession"
  />

  <main
      v-else
      class="session-library"
      :aria-busy="
      loadingSessions ||
      creatingSession
    "
  >
    <section
        class="
        edit-box
        edit-box--accent
        session-library__header
      "
    >
      <header class="edit-box__header">
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
            Conversation library
          </span>

          <div class="edit-box__title-row">
            <h1 class="edit-box__title">
              Sessions
            </h1>

            <span class="edit-box__count">
              {{ sessionCountLabel }}
            </span>
          </div>

          <p class="edit-box__description">
            Continue an existing narrative
            session or create a new conversation.
          </p>
        </div>

        <div class="edit-box__actions">
          <button
              type="button"
              class="edit-box__action"
              :disabled="loadingSessions"
              @click="loadSessions"
          >
            <svg
                viewBox="0 0 24 24"
                aria-hidden="true"
            >
              <path
                  d="M20 11a8.1 8.1 0 0 0-15.5-2"
              />

              <path d="M4 4v5h5" />

              <path
                  d="M4 13a8.1 8.1 0 0 0 15.5 2"
              />

              <path d="M20 20v-5h-5" />
            </svg>

            Refresh
          </button>

          <button
              type="button"
              class="
              edit-box__action
              edit-box__action--accent
            "
              :disabled="creatingSession"
              @click="createNewSession = true"
          >
            <span
                v-if="creatingSession"
                class="edit-box__spinner"
                aria-hidden="true"
            />

            <svg
                v-else
                viewBox="0 0 24 24"
                aria-hidden="true"
            >
              <path d="M12 5v14" />
              <path d="M5 12h14" />
            </svg>

            {{
              creatingSession
                  ? "Creating..."
                  : "New session"
            }}
          </button>
        </div>
      </header>

      <div
          class="
          edit-box__body
          session-library__toolbar
        "
      >
        <div class="session-library__search">
          <SearchBar
              v-model:search="filteringTerm"
              placeholder="Search sessions by name or ID"
              aria-label="Search sessions by name or ID"
          />
        </div>

        <button
            v-if="filteringTerm.trim()"
            type="button"
            class="edit-box__action"
            @click="clearFilter"
        >
          Clear filter
        </button>
      </div>
    </section>

    <div
        v-if="sessionOperationError"
        class="
        edit-box__state
        edit-box__state--error
        session-library__error
      "
        role="alert"
    >
      <div class="edit-box__state-content">
        <strong class="edit-box__state-title">
          Session operation failed
        </strong>

        <p class="edit-box__state-description">
          {{ sessionOperationError }}
        </p>
      </div>

      <button
          type="button"
          class="edit-box__action"
          @click="
          sessionOperationError = null
        "
      >
        Dismiss
      </button>
    </div>

    <div
        v-if="loadingSessions"
        class="
        edit-box__state
        session-library__state
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
          Loading sessions
        </strong>

        <p class="edit-box__state-description">
          Retrieving conversations and their
          latest messages.
        </p>
      </div>
    </div>

    <div
        v-else-if="sessionLoadError"
        class="
        edit-box__state
        edit-box__state--error
        edit-box__state--vertical
        session-library__state
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
          Could not load sessions
        </strong>

        <p class="edit-box__state-description">
          {{ sessionLoadError }}
        </p>
      </div>

      <button
          type="button"
          class="
          edit-box__action
          edit-box__action--accent
        "
          @click="loadSessions"
      >
        Retry
      </button>
    </div>

    <section
        v-else-if="filteredSessions.length"
        class="session-library__grid"
        aria-label="Available sessions"
    >
      <SessionTease
          v-for="session in filteredSessions"
          :key="
          `${session.hashKey()}:${sessionListRevision}`
        "
          :session="session"
          :deleting="
          isDeletingSession(session)
        "
          @open="openSession"
          @delete="deleteSession"
      />
    </section>

    <div
        v-else-if="filteringTerm.trim()"
        class="
        edit-box__state
        edit-box__state--vertical
        session-library__state
      "
    >
      <div class="edit-box__state-icon">
        <svg
            viewBox="0 0 24 24"
            aria-hidden="true"
        >
          <circle
              cx="11"
              cy="11"
              r="7"
          />

          <path d="m20 20-4-4" />
        </svg>
      </div>

      <div class="edit-box__state-content">
        <strong class="edit-box__state-title">
          No matching sessions
        </strong>

        <p class="edit-box__state-description">
          No session matches
          “{{ filteringTerm.trim() }}”.
        </p>
      </div>

      <button
          type="button"
          class="edit-box__action"
          @click="clearFilter"
      >
        Clear filter
      </button>
    </div>

    <div
        v-else
        class="
        edit-box__state
        edit-box__state--vertical
        session-library__state
      "
    >
      <div class="edit-box__state-icon">
        <svg
            viewBox="0 0 24 24"
            aria-hidden="true"
        >
          <path
              d="M4 5h16v11H8l-4 4V5Z"
          />

          <path d="M12 8v5" />
          <path d="M9.5 10.5h5" />
        </svg>
      </div>

      <div class="edit-box__state-content">
        <strong class="edit-box__state-title">
          No sessions
        </strong>

        <p class="edit-box__state-description">
          Create a session to begin a new
          conversation.
        </p>
      </div>

      <button
          type="button"
          class="
          edit-box__action
          edit-box__action--accent
        "
          @click="createNewSession = true"
      >
        Create first session
      </button>
    </div>

    <NewSessionPopUp
        :model-value="createNewSession"
        @create-new-session="
        handleCreateSession
      "
        @close="
        createNewSession = false
      "
    />
  </main>
</template>

<style scoped>
.session-library {
  width: min(100%, 96rem);
  min-width: 0;
  min-height: 100%;
  box-sizing: border-box;

  display: flex;
  flex-direction: column;
  gap: var(--space-4);

  margin: 0 auto;
  padding: var(--space-4);

  color: rgb(var(--c-fg));
  font-family: var(--font-primary);
}

.session-library
.edit-box__action
svg {
  width: 1rem;
  height: 1rem;

  fill: none;
  stroke: currentColor;
  stroke-width: 1.9;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.session-library__header {
  flex: 0 0 auto;
}

.session-library__toolbar {
  display: flex;
  align-items: center;
  gap: var(--space-3);

  padding-top: var(--space-3);
}

.session-library__search {
  flex: 1 1 24rem;
  min-width: 12rem;
}

.session-library__error {
  min-height: auto;
  justify-content: flex-start;

  padding: var(--space-3);

  text-align: left;
}

.session-library__state {
  min-height: 18rem;
}

.session-library__grid {
  display: grid;
  grid-template-columns:
    repeat(
      auto-fill,
      minmax(min(100%, 22rem), 1fr)
    );

  align-items: stretch;
  gap: var(--space-4);

  min-width: 0;
}

.session-library__settings {
  margin-top: auto;

  overflow: hidden;
}

.session-library__settings-body {
  min-width: 0;
}

@media (max-width: 720px) {
  .session-library {
    padding: var(--space-3);
  }

  .session-library__toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .session-library__search {
    flex-basis: auto;
    width: 100%;
  }
}

@media (max-width: 480px) {
  .session-library {
    padding: var(--space-2);
  }
}
</style>