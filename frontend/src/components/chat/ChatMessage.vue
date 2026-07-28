<script setup lang="ts">
import {
  computed,
  onMounted,
  ref,
  shallowRef,
  watch,
} from "vue";

import MarkdownIt from "markdown-it";
import DOMPurify from "dompurify";

import {Message} from "@/domain/Session";
import {Location} from "@/domain/World";
import {Character} from "@/domain/Characters";
import {ChatCompletionRole} from "@/types/ChatCompletions";

import WindowPrompt from "@/components/utils/prompts/WindowPrompt.vue";
import LocationEditor from "@/components/space/LocationEditor.vue";
import CharacterEditor from "@/components/char/CharacterEditor.vue";
import FieldEditorWrapper from "@/components/utils/FieldEditorWrapper.vue";

const markdown = new MarkdownIt({
  html: true,
  linkify: true,
  breaks: true,
});

const props = defineProps<{
  message: Message;
  isLastMessage: boolean;
  title: string;
  onRegenerate: (message: Message) => Promise<boolean>;
}>();

const emit = defineEmits<{
  (event: "delete", value: Message): void;
}>();

/* -------------------------------------------------------------------------- */
/* Message metadata                                                           */
/* -------------------------------------------------------------------------- */

const location = shallowRef<Location>();
const presentCharacters = shallowRef<Character[]>([]);
const presentCharacterNames = computed<string[]>(() =>
    presentCharacters.value
        .map((character) => String(character.get("name") ?? ""))
        .filter(Boolean),
);

const editingLocation = ref(false);
const editingCharacter = ref(false);
const selectedCharacter = shallowRef<Character | null>(null);

const isAssistantMessage = computed(
    () =>
        props.message.get("role") ===
        ChatCompletionRole.ASSISTANT,
);

const activeResponse = computed<number>(() =>
    Number(props.message.get("active_response") ?? 1),
);

const responseCount = computed<number>(() =>
    Number(props.message.get("response_num") ?? 1),
);

/* -------------------------------------------------------------------------- */
/* Message content                                                            */
/* -------------------------------------------------------------------------- */

const editingMessage = ref(false);
const savingMessage = ref(false);
const regeneratingMessage = ref(false);

const displayedContent = ref("");
const messageDraft = ref("");

const renderedContent = computed<string>(() => {
  const renderedHtml = markdown.render(displayedContent.value);

  return DOMPurify.sanitize(renderedHtml);
});

function beginMessageEdit(): void {
  messageDraft.value = displayedContent.value;
  editingMessage.value = true;
}

function cancelMessageEdit(): void {
  messageDraft.value = displayedContent.value;
  editingMessage.value = false;
}

async function saveMessageEdit(): Promise<void> {
  savingMessage.value = true;

  try {
    await props.message.update(
        "content",
        messageDraft.value,
    );

    displayedContent.value = messageDraft.value;
    editingMessage.value = false;
  } finally {
    savingMessage.value = false;
  }
}
/* -------------------------------------------------------------------------- */
/* Reasoning                                                                  */
/* -------------------------------------------------------------------------- */
const reasoningExpanded = ref(false);

const reasoningContent = computed<string>(() => {
  const reasoning = props.message.get("reasoning");

  return reasoning == null ? "" : String(reasoning);
});

const hasReasoning = computed<boolean>(
    () => reasoningContent.value.trim().length > 0,
);

const renderedReasoning = computed<string>(() => {
  const renderedHtml = markdown.render(reasoningContent.value);

  return DOMPurify.sanitize(renderedHtml);
});

const reasoningPanelId = computed<string>(() => {
  const messageKey = String(
      props.message.get("tick_num") ?? "current",
  ).replace(/[^a-zA-Z0-9_-]/g, "-");

  return `chat-message-reasoning-${messageKey}-${activeResponse.value}`;
});

/* -------------------------------------------------------------------------- */
/* Alternative responses                                                      */
/* -------------------------------------------------------------------------- */

async function changeActiveResponse(
    offset: -1 | 1,
): Promise<void> {
  if (regeneratingMessage.value) {
    return;
  }

  const nextResponse = activeResponse.value + offset;

  if (nextResponse < 1) {
    return;
  }

  if (nextResponse > responseCount.value) {
    await regenerateMessage();
    return;
  }

  await props.message.update(
      "active_response",
      nextResponse,
  );

  displayedContent.value = props.message.get("content");
  messageDraft.value = displayedContent.value;
  editingMessage.value = false;
  reasoningExpanded.value = false;

  await loadMessageContext();
}

async function regenerateMessage(): Promise<void> {
  if (regeneratingMessage.value) {
    return;
  }

  regeneratingMessage.value = true;
  editingMessage.value = false;
  reasoningExpanded.value = false;
  displayedContent.value = "";

  try {
    const success = await props.onRegenerate(
        props.message,
    );

    if (success) {
      await loadMessageContext();
    } else {
      displayedContent.value = props.message.get("content");
    }
  } finally {
    regeneratingMessage.value = false;
  }
}

/* -------------------------------------------------------------------------- */
/* Context editors                                                            */
/* -------------------------------------------------------------------------- */

function openCharacterEditor(name: string): void {
  const character = presentCharacters.value.find(
      (candidate) =>
          candidate.get("name") === name,
  );

  if (!character) {
    return;
  }

  selectedCharacter.value = character;
  editingCharacter.value = true;
}

function closeCharacterEditor(): void {
  editingCharacter.value = false;
  selectedCharacter.value = null;
}

let contextLoadSequence = 0;

async function loadMessageContext(): Promise<void> {
  const loadSequence = ++contextLoadSequence;
  console.log(`[ChatMessage] ${props.message}`);
  const [
    nextLocation,
    nextCharacters,
  ] = await Promise.all([
    props.message.getLocation(),
    props.message.getCharacters(),
  ]);

  if (loadSequence !== contextLoadSequence) {
    return;
  }

  location.value = nextLocation;
  presentCharacters.value = nextCharacters;

  displayedContent.value = props.message.get("content");
  messageDraft.value = displayedContent.value;

  console.debug(
      `[ChatMessage] tick ${props.message.get('tick_num')}
        Reasoning:
          ${props.message.get('reasoning')}
        Content:
          ${props.message.get('content')}
      `
  );
}

onMounted(() => {
  void loadMessageContext();
});

watch(
    () => props.message,
    () => {
      editingMessage.value = false;
      reasoningExpanded.value = false;
      selectedCharacter.value = null;
      editingCharacter.value = false;

      void loadMessageContext();
    },
);
</script>

<template>
  <article
      :class="['edit-box','chat-message',
      isAssistantMessage
        ? 'edit-box--accent'
        : 'edit-box--primary',
    ]"
      :aria-busy="
      savingMessage || regeneratingMessage
    "
  >
    <header
        class="
        edit-box__header
        chat-message__header
      "
    >
      <div class="chat-message__header-row">
        <div class="edit-box__header-main">
          <div class="edit-box__title-row">
            <h2 class="edit-box__title">
              {{ props.title }}
            </h2>
          </div>
        </div>

        <div
            class="
            edit-box__actions
            chat-message__actions
          "
        >
          <div
              v-if="isAssistantMessage"
              class="chat-message__response-selector"
              aria-label="Alternative response selector"
          >
            <button
                type="button"
                class="chat-message__response-button"
                aria-label="Previous response"
                title="Previous response"
                :disabled="
                !props.isLastMessage ||
                activeResponse <= 1 ||
                regeneratingMessage
              "
                @click="changeActiveResponse(-1)"
            >
              <svg
                  viewBox="0 0 24 24"
                  aria-hidden="true"
              >
                <path d="m15 18-6-6 6-6"/>
              </svg>
            </button>

            <output
                class="chat-message__response-counter"
                aria-live="polite"
            >
              {{ activeResponse }}/{{ responseCount }}
            </output>

            <button
                type="button"
                class="chat-message__response-button"
                aria-label="Next response"
                title="Next response"
                :disabled="
                !props.isLastMessage ||
                regeneratingMessage
              "
                @click="changeActiveResponse(1)"
            >
              <svg
                  viewBox="0 0 24 24"
                  aria-hidden="true"
              >
                <path d="m9 18 6-6-6-6"/>
              </svg>
            </button>
          </div>

          <button
              v-if="!editingMessage"
              type="button"
              class="
              edit-box__action
              chat-message__action
            "
              :disabled="regeneratingMessage"
              @click="beginMessageEdit"
          >
            <svg
                viewBox="0 0 24 24"
                aria-hidden="true"
            >
              <path d="M12 20h9"/>
              <path
                  d="
                  M16.5 3.5
                  a2.1 2.1 0 0 1 3 3
                  L8 18l-4 1 1-4Z
                "
              />
            </svg>

            Edit
          </button>

          <template v-else>
            <button
                type="button"
                class="
                edit-box__action
                edit-box__action--accent
                chat-message__action
              "
                :disabled="savingMessage"
                @click="saveMessageEdit"
            >
              <span
                  v-if="savingMessage"
                  class="edit-box__spinner"
                  aria-hidden="true"
              />

              <svg
                  v-else
                  viewBox="0 0 24 24"
                  aria-hidden="true"
              >
                <path d="M20 6 9 17l-5-5"/>
              </svg>

              {{ savingMessage ? "Saving..." : "Save" }}
            </button>

            <button
                type="button"
                class="
                edit-box__action
                chat-message__action
              "
                :disabled="savingMessage"
                @click="cancelMessageEdit"
            >
              Cancel
            </button>
          </template>

          <button
              type="button"
              class="
              edit-box__action
              edit-box__action--danger
              chat-message__icon-action
            "
              title="Delete message"
              aria-label="Delete message"
              :disabled="
              !props.isLastMessage ||
              savingMessage ||
              regeneratingMessage
            "
              @click="emit('delete', props.message)"
          >
            <svg
                viewBox="0 0 24 24"
                aria-hidden="true"
            >
              <path d="M3 6h18"/>
              <path d="M8 6V4h8v2"/>
              <path d="m19 6-1 14H6L5 6"/>
              <path d="M10 11v5"/>
              <path d="M14 11v5"/>
            </svg>
          </button>
        </div>
      </div>

      <div class="chat-message__context">
        <div
            v-if="location"
            class="chat-message__context-group"
        >
          <span class="chat-message__context-label">
            Location
          </span>

          <button
              type="button"
              class="chat-message__context-button"
              @click="editingLocation = true"
          >
            <svg
                viewBox="0 0 24 24"
                aria-hidden="true"
            >
              <path
                  d="
                  M20 10
                  c0 5-8 11-8 11
                  S4 15 4 10
                  a8 8 0 1 1 16 0Z
                "
              />
              <circle cx="12" cy="10" r="2.5"/>
            </svg>

            {{ location.get("name") }}
          </button>
        </div>

        <div
            v-if="presentCharacterNames.length"
            class="chat-message__context-group"
        >
          <span class="chat-message__context-label">
            Characters
          </span>

          <div class="chat-message__character-list">
            <button
                v-for="characterName in presentCharacterNames"
                :key="characterName"
                type="button"
                class="chat-message__context-button"
                @click="openCharacterEditor(characterName)"
            >
              {{ characterName }}
            </button>
          </div>
        </div>
      </div>


    </header>

    <section
        v-if="isAssistantMessage && hasReasoning && !regeneratingMessage"
        class="chat-message__reasoning"
        aria-label="Assistant reasoning"
    >
      <button
          type="button"
          class="chat-message__reasoning-toggle"
          :aria-expanded="reasoningExpanded"
          :aria-controls="reasoningPanelId"
          @click="reasoningExpanded = !reasoningExpanded"
      >
        <span class="chat-message__reasoning-title">
          <svg
              viewBox="0 0 24 24"
              aria-hidden="true"
          >
            <path d="M9.5 4.5a3.5 3.5 0 0 0-3.22 4.87A3.5 3.5 0 0 0 7.5 16h1"/>
            <path d="M14.5 4.5a3.5 3.5 0 0 1 3.22 4.87A3.5 3.5 0 0 1 16.5 16h-1"/>
            <path d="M9.5 4.5V19"/>
            <path d="M14.5 4.5V19"/>
            <path d="M9.5 8h2"/>
            <path d="M12.5 12h2"/>
            <path d="M9.5 16h2"/>
          </svg>

          Reasoning
        </span>

        <span class="chat-message__reasoning-action">
          {{ reasoningExpanded ? "Hide" : "Show" }}

          <svg
              :class="[
              'chat-message__reasoning-chevron',
              {
                'chat-message__reasoning-chevron--expanded':
                  reasoningExpanded,
              },
            ]"
              viewBox="0 0 24 24"
              aria-hidden="true"
          >
            <path d="m6 9 6 6 6-6"/>
          </svg>
        </span>
      </button>

      <div
          v-show="reasoningExpanded"
          :id="reasoningPanelId"
          class="
          chat-message__reasoning-content
          chat-message__content--readonly
        "
          v-html="renderedReasoning"
      />
    </section>

    <div
        class="
        edit-box__body
        chat-message__body
      "
    >
      <div
          v-if="regeneratingMessage"
          class="
          edit-box__state
          edit-box__state--vertical
          chat-message__loading-state
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
            Generating response
          </strong>

          <p class="edit-box__state-description">
            The assistant is producing another response.
          </p>
        </div>
      </div>

      <div
          v-else-if="!editingMessage"
          :key="activeResponse"
          class="
          chat-message__content
          chat-message__content--readonly
        "
          v-html="renderedContent"
      />

      <textarea
          v-else
          v-model="messageDraft"
          class="
          chat-message__content
          chat-message__content--editor
        "
          spellcheck="true"
          aria-label="Message content"
      />
    </div>
  </article>

  <WindowPrompt
      v-if="location && editingLocation"
      :title="
      String(
        location.get('name') ??
        'Location',
      )
    "
      @close="editingLocation = false"
  >
    <LocationEditor
        :model-value="{
        location,
        all_locations: [],
      }"
    />
  </WindowPrompt>

  <WindowPrompt
      v-if="
      editingCharacter &&
      selectedCharacter
    "
      :title="
      String(
        selectedCharacter.get('name') ??
        'Character',
      )
    "
      @close="closeCharacterEditor"
  >
    <CharacterEditor
        :model-value="selectedCharacter"
        :edit-starting-locations="false"
    />
  </WindowPrompt>
</template>

<style scoped>
.chat-message {
  width: 100%;
  min-width: 0;
  min-height: 12.5rem;
}

/* -------------------------------------------------------------------------- */
/* Header                                                                     */
/* -------------------------------------------------------------------------- */

.chat-message__header {
  display: flex;
  align-items: stretch;
  flex-direction: column;
  gap: var(--space-3);
}

.chat-message__header-row {
  width: 100%;
  min-width: 0;

  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
}

.chat-message__actions {
  max-width: 100%;
}

/* -------------------------------------------------------------------------- */
/* Actions                                                                    */
/* -------------------------------------------------------------------------- */

.chat-message__action {
  min-height: 2rem;

  padding: 0.4rem 0.65rem;

  font-size: 0.74rem;
}

.chat-message__action svg,
.chat-message__icon-action svg {
  width: 1rem;
  height: 1rem;

  fill: none;
  stroke: currentColor;
  stroke-width: 1.9;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.chat-message__icon-action {
  width: 2rem;
  height: 2rem;
  min-height: 0;

  padding: 0;
}

/* -------------------------------------------------------------------------- */
/* Response selector                                                          */
/* -------------------------------------------------------------------------- */

.chat-message__response-selector {
  min-height: 2rem;

  display: inline-flex;
  align-items: stretch;

  overflow: hidden;

  color: rgb(var(--c-fg));

  background: rgb(var(--c-surface-raised) / 0.55);
  border: 1px solid rgb(var(--c-border) / 0.3);
  border-radius: var(--radius-sm);

  box-shadow: inset 0 1px 0 rgb(255 255 255 / 0.28);
}

.chat-message__response-button {
  width: 2rem;
  min-height: 2rem;
  flex: 0 0 auto;

  display: grid;
  place-items: center;

  padding: 0;

  color: inherit;

  background: transparent;
  border: 0;

  cursor: pointer;

  transition: color var(--duration-fast) var(--ease-standard),
  background-color var(--duration-fast) var(--ease-standard);
}

.chat-message__response-button:hover:not(:disabled) {
  color: rgb(var(--c-on-accent));
  background: rgb(var(--c-accent) / 0.24);
}

.chat-message__response-button:focus-visible {
  position: relative;
  z-index: 1;

  outline: var(--focus-ring-width) solid rgb(var(--focus-ring-color) / 0.28);

  outline-offset: -3px;
}

.chat-message__response-button:disabled {
  opacity: 0.38;
  cursor: not-allowed;
}

.chat-message__response-button svg {
  width: 1rem;
  height: 1rem;

  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.chat-message__response-counter {
  min-width: 3.25rem;

  display: inline-flex;
  align-items: center;
  justify-content: center;

  padding: 0 var(--space-2);

  color: rgb(var(--c-muted));

  border-right: 1px solid rgb(var(--c-border) / 0.24);
  border-left: 1px solid rgb(var(--c-border) / 0.24);

  font-size: 0.72rem;
  font-weight: 750;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

/* -------------------------------------------------------------------------- */
/* Message context                                                            */
/* -------------------------------------------------------------------------- */

.chat-message__context {
  width: 100%;
  min-width: 0;

  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-2) var(--space-4);

  padding: var(--space-2) var(--space-3);

  background: rgb(var(--c-surface-raised) / 0.34);
  border: 1px solid rgb(var(--c-border) / 0.2);
  border-radius: var(--radius-md);

  box-shadow: inset 0 1px 0 rgb(255 255 255 / 0.24);
}

.chat-message__context-group {
  min-width: 0;

  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.chat-message__context-label {
  color: rgb(var(--c-muted));

  font-size: 0.68rem;
  font-weight: 800;
  line-height: 1;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.chat-message__character-list {
  min-width: 0;

  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-1);
}

.chat-message__context-button {
  min-height: 1.75rem;

  display: inline-flex;
  align-items: center;
  gap: 0.35rem;

  padding: 0.25rem 0.55rem;

  color: rgb(var(--edit-box-accent-strong));

  background: rgb(var(--edit-box-accent) / 0.1);
  border: 1px solid rgb(var(--edit-box-accent) / 0.22);
  border-radius: var(--radius-round);

  font-family: var(--font-primary);
  font-size: 0.74rem;
  font-weight: 700;
  line-height: 1;

  cursor: pointer;

  transition: color var(--duration-fast) var(--ease-standard),
  background-color var(--duration-fast) var(--ease-standard),
  border-color var(--duration-fast) var(--ease-standard);
}

.chat-message__context-button:hover {
  color: rgb(var(--c-on-accent));

  background: rgb(var(--edit-box-accent) / 0.26);
  border-color: rgb(var(--edit-box-accent) / 0.46);
}

.chat-message__context-button:focus-visible {
  outline: var(--focus-ring-width) solid rgb(var(--focus-ring-color) / 0.3);

  outline-offset: 2px;
}

.chat-message__context-button svg {
  width: 0.9rem;
  height: 0.9rem;

  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

/* -------------------------------------------------------------------------- */
/* Reasoning                                                                  */
/* -------------------------------------------------------------------------- */

.chat-message__reasoning {
  width: 100%;
  min-width: 0;

  background: rgb(var(--c-surface-raised) / 0.24);
  border-top: 1px solid rgb(var(--c-border) / 0.18);
  border-bottom: 1px solid rgb(var(--c-border) / 0.18);
}

.chat-message__reasoning-toggle {
  width: 100%;
  min-height: 2.75rem;

  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);

  padding: var(--space-2) var(--space-4);

  color: rgb(var(--c-muted));

  background: transparent;
  border: 0;

  font-family: var(--font-primary);

  cursor: pointer;

  transition: color var(--duration-fast) var(--ease-standard),
  background-color var(--duration-fast) var(--ease-standard);
}

.chat-message__reasoning-toggle:hover {
  color: rgb(var(--c-fg-strong));

  background: rgb(var(--edit-box-accent) / 0.07);
}

.chat-message__reasoning-toggle:focus-visible {
  outline: var(--focus-ring-width) solid rgb(var(--focus-ring-color) / 0.3);

  outline-offset: calc(-1 * var(--focus-ring-width));
}

.chat-message__reasoning-title,
.chat-message__reasoning-action {
  display: inline-flex;
  align-items: center;
}

.chat-message__reasoning-title {
  gap: var(--space-2);

  color: rgb(var(--c-fg-strong));

  font-size: 0.76rem;
  font-weight: 800;
  letter-spacing: 0.045em;
  text-transform: uppercase;
}

.chat-message__reasoning-title svg {
  width: 1rem;
  height: 1rem;

  fill: none;
  stroke: currentColor;
  stroke-width: 1.75;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.chat-message__reasoning-action {
  gap: var(--space-1);

  font-size: 0.72rem;
  font-weight: 700;
}

.chat-message__reasoning-chevron {
  width: 0.9rem;
  height: 0.9rem;

  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;

  transition: transform var(--duration-fast) var(--ease-standard);
}

.chat-message__reasoning-chevron--expanded {
  transform: rotate(180deg);
}

.chat-message__reasoning-content {
  max-height: 28rem;
  overflow: auto;

  padding: var(--space-3) var(--space-4) var(--space-4);

  color: rgb(var(--c-muted));

  border-top: 1px solid rgb(var(--c-border) / 0.14);

  font-size: 0.86rem;
  line-height: 1.6;

  scrollbar-width: thin;
  scrollbar-color: rgb(var(--edit-box-accent) / 0.45) transparent;
}

/* -------------------------------------------------------------------------- */
/* Content                                                                    */
/* -------------------------------------------------------------------------- */

.chat-message__body {
  min-height: 9rem;

  display: flex;

  padding: 0;

  background: linear-gradient(
      145deg,
      rgb(var(--c-surface) / 0.74),
      rgb(var(--c-surface-2) / 0.48)
  );
}

.chat-message__content {
  width: 100%;
  min-width: 0;
  min-height: 11rem;
  box-sizing: border-box;

  padding: var(--space-4);

  color: rgb(var(--c-fg));

  background: transparent;
  border: 0;
  outline: 0;

  font-family: var(--font-primary);
  font-size: 0.95rem;
  font-weight: 400;
  line-height: 1.65;
}

.chat-message__content--readonly {
  overflow-wrap: anywhere;
  user-select: text;
}

.chat-message__content--editor {
  display: block;

  caret-color: rgb(var(--c-accent-2));

  resize: vertical;

  transition: background-color var(--duration-normal) var(--ease-standard),
  box-shadow var(--duration-normal) var(--ease-standard);
}

.chat-message__content--editor:focus {
  background: rgb(var(--c-surface-raised) / 0.24);

  box-shadow: inset 0 0 0 var(--focus-ring-width) rgb(var(--focus-ring-color) / 0.18);
}

.chat-message__content--editor::selection {
  color: rgb(var(--c-on-accent));
  background: rgb(var(--c-accent) / 0.46);
}

.chat-message__loading-state {
  width: 100%;
  min-height: 11rem;

  margin: var(--space-4);
}

/* -------------------------------------------------------------------------- */
/* Rendered markdown                                                          */
/* -------------------------------------------------------------------------- */

.chat-message__content--readonly :deep(p) {
  margin: 0 0 var(--space-3);
}

.chat-message__content--readonly :deep(p:last-child) {
  margin-bottom: 0;
}

.chat-message__content--readonly :deep(h1),
.chat-message__content--readonly :deep(h2),
.chat-message__content--readonly :deep(h3),
.chat-message__content--readonly :deep(h4),
.chat-message__content--readonly :deep(h5),
.chat-message__content--readonly :deep(h6) {
  margin: var(--space-5) 0 var(--space-2);

  color: rgb(var(--c-fg-strong));

  font-weight: 750;
  line-height: 1.3;
}

.chat-message__content--readonly :deep(h1:first-child),
.chat-message__content--readonly :deep(h2:first-child),
.chat-message__content--readonly :deep(h3:first-child) {
  margin-top: 0;
}

.chat-message__content--readonly :deep(a) {
  color: rgb(var(--c-primary-strong));

  text-decoration-color: rgb(var(--c-primary) / 0.55);
  text-underline-offset: 0.16em;
}

.chat-message__content--readonly :deep(a:hover) {
  color: rgb(var(--c-accent-2));

  text-decoration-color: rgb(var(--c-accent) / 0.8);
}

.chat-message__content--readonly :deep(ul),
.chat-message__content--readonly :deep(ol) {
  margin: var(--space-3) 0;

  padding-left: var(--space-6);
}

.chat-message__content--readonly :deep(li + li) {
  margin-top: var(--space-1);
}

.chat-message__content--readonly :deep(blockquote) {
  margin: var(--space-4) 0;

  padding: var(--space-2) var(--space-3);

  color: rgb(var(--c-muted));

  background: rgb(var(--edit-box-accent) / 0.055);
  border-left: 3px solid rgb(var(--edit-box-accent) / 0.68);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
}

.chat-message__content--readonly :deep(pre) {
  max-width: 100%;
  overflow-x: auto;

  margin: var(--space-4) 0;

  padding: var(--space-3);

  color: rgb(var(--c-fg));

  background: rgb(var(--c-surface-3) / 0.48);
  border: 1px solid rgb(var(--c-border) / 0.25);
  border-radius: var(--radius-md);

  box-shadow: inset 0 1px 3px rgb(var(--c-shadow) / 0.07);

  scrollbar-width: thin;
  scrollbar-color: rgb(var(--edit-box-accent) / 0.45) transparent;
}

.chat-message__content--readonly :deep(code) {
  padding: 0.1rem 0.3rem;

  color: rgb(var(--c-fg-strong));

  background: rgb(var(--c-surface-3) / 0.5);
  border-radius: var(--radius-xs);

  font-family: var(--font-monospace);
  font-size: 0.88em;
}

.chat-message__content--readonly :deep(pre code) {
  padding: 0;

  color: inherit;

  background: transparent;
  border-radius: 0;
}

.chat-message__content--readonly :deep(hr) {
  height: 1px;

  margin: var(--space-5) 0;

  background: linear-gradient(
      90deg,
      transparent,
      rgb(var(--c-border) / 0.48),
      transparent
  );

  border: 0;
}

.chat-message__content--readonly :deep(table) {
  width: 100%;
  max-width: 100%;

  margin: var(--space-4) 0;

  border-collapse: collapse;

  font-size: 0.9rem;
}

.chat-message__content--readonly :deep(th),
.chat-message__content--readonly :deep(td) {
  padding: var(--space-2) var(--space-3);

  border: 1px solid rgb(var(--c-border) / 0.3);

  text-align: left;
  vertical-align: top;
}

.chat-message__content--readonly :deep(th) {
  color: rgb(var(--c-fg-strong));

  background: rgb(var(--c-surface-2) / 0.55);

  font-weight: 750;
}

.chat-message__content--readonly :deep(img) {
  height: auto;
  max-width: 100%;

  margin: var(--space-4) auto;

  border-radius: var(--radius-md);

  box-shadow: 0 5px 18px rgb(var(--c-shadow) / 0.1);
}

/* -------------------------------------------------------------------------- */
/* Responsive                                                                 */
/* -------------------------------------------------------------------------- */

@media (max-width: 700px) {
  .chat-message__header-row {
    flex-direction: column;
  }

  .chat-message__actions {
    width: 100%;
    justify-content: flex-end;
  }

  .chat-message__context {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (max-width: 480px) {
  .chat-message__actions {
    justify-content: flex-start;
  }

  .chat-message__response-selector {
    margin-right: auto;
  }

  .chat-message__content,
  .chat-message__reasoning-toggle,
  .chat-message__reasoning-content {
    padding-right: var(--space-3);
    padding-left: var(--space-3);
  }

  .chat-message__context-group {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (prefers-reduced-motion: reduce) {
  .chat-message__response-button,
  .chat-message__context-button,
  .chat-message__reasoning-toggle,
  .chat-message__reasoning-chevron,
  .chat-message__content--editor {
    transition: none;
  }
}
</style>