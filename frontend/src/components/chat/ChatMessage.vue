<script setup lang="ts">
import {Message} from "@/domain/Session";
import {computed, onMounted, ref, shallowRef, watch} from "vue";
import {Location} from "@/domain/World";
import {Character} from "@/domain/Characters";
import WindowPrompt from "@/components/utils/prompts/WindowPrompt.vue";
import LocationEditor from "@/components/space/LocationEditor.vue";
import CharacterEditor from "@/components/char/CharacterEditor.vue";
import FieldEditorWrapper from "@/components/utils/FieldEditorWrapper.vue";
import MarkdownIt from "markdown-it";
import DOMPurify from "dompurify";
import {ChatCompletionRole} from "@/types/ChatCompletions";

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

const emits = defineEmits<{
  (e: "delete", value: Message): void;
  (e: "regenerate", value:Message): void;
}>();

const location = shallowRef<Location>();
const presentCharacters = shallowRef<Character[]>([]);
const presentCharactersNames = shallowRef<string[]>([]);

const editingLocation = ref<boolean>(false);
const isAssistantMessage = computed<boolean>(() => props.message.get('role') == ChatCompletionRole.ASSISTANT);
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// Message edit
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
const editingMessage = ref<boolean>(false);
const savingMessage = ref<boolean>(false);

const displayedContent = ref<string>("");
const messageDraft = ref<string>("");

const renderedContent = computed<string>(() => {
  props.message.get("active_response");
  const html = markdown.render(displayedContent.value);
  return DOMPurify.sanitize(html);
});

function getMessageContent(): string {
  const content = props.message.get("content");
  return content == null ? "" : String(content);
}

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
    await props.message.update("content", messageDraft.value);
    displayedContent.value = messageDraft.value;
    editingMessage.value = false;
  } finally {
    savingMessage.value = false;
  }
}
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// Response
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
async function changeActiveResponse(offset: -1 | 1): Promise<void> {
  const nextResponse = props.message.get('active_response') + offset;

  if (nextResponse < 0 || nextResponse > props.message.get('response_num')) {
    await onRegenerate()
    return;
  }

  await props.message.update('active_response', nextResponse);

  displayedContent.value = props.message.get('content');
  console.log(`New content: ${displayedContent.value}`);
  editingMessage.value = false;
  await loadMessageContext();
}
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// Character
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
const editingCharacter = ref<boolean>(false);
const selectedCharacter = shallowRef<Character | null>(null);

function onCharacterClick(name: string): void {
  const character = presentCharacters.value.find(
      character => character.get("name") === name
  );

  if (!character) return;

  selectedCharacter.value = character;
  editingCharacter.value = true;
}

async function loadMessageContext(): Promise<void> {
  location.value = await props.message.getLocation();

  presentCharacters.value = await props.message.getCharacters();
  presentCharactersNames.value = presentCharacters.value.map(character =>
      String(character.get("name") ?? "")
  );

  displayedContent.value = getMessageContent();
  messageDraft.value = displayedContent.value;

  console.debug(
      `${props.message.get("tick_num")} : ${location.value.get("name")} with present characters ${presentCharactersNames.value}`
  );
}

async function onRegenerate(){
  displayedContent.value = "...";
  const success = await props.onRegenerate(props.message);
  if (success) await loadMessageContext()
}

onMounted(loadMessageContext);

watch(
    () => props.message,
    () => {
      editingMessage.value = false;
      void loadMessageContext();
    }
);
</script>

<template>
  <article class="chat-message-box">
    <header class="chat-message-header">
      <div class="chat-message-header-main">
        <span class="chat-message-title">
          {{ props.title }}
        </span>

        <div class="chat-message-actions">
          <button
              type="button"
              class="chat-message-action-button"
              :disabled = "!props.isLastMessage"
              @click="emits('delete', props.message)"
          >
            D
          </button>

          <div
              class="chat-message-response-selector"
              v-if="isAssistantMessage"
              aria-label="Alternative response selector"
          >
            <button
                type="button"
                class="chat-message-response-button"
                aria-label="Previous response"
                :disabled="!props.isLastMessage || props.message.get('active_response') == 1"
                @click="changeActiveResponse(-1)"
            >
              &lt;
            </button>

            <output
                class="chat-message-response-counter"
                aria-live="polite"
            >
              {{ props.message.get('active_response') }}/{{ props.message.get('response_num') }}
            </output>

            <button
                type="button"
                class="chat-message-response-button"
                aria-label="Next response"
                :disabled="!props.isLastMessage"
                @click="changeActiveResponse(1)"
            >
              &gt;
            </button>
          </div>

          <button
              v-if="!editingMessage"
              type="button"
              class="chat-message-action-button"
              @click="beginMessageEdit"
          >
            Edit
          </button>

          <template v-else>
            <button
                type="button"
                class="chat-message-action-button"
                :disabled="savingMessage"
                @click="saveMessageEdit"
            >
              Save
            </button>

            <button
                type="button"
                class="chat-message-action-button"
                :disabled="savingMessage"
                @click="cancelMessageEdit"
            >
              Cancel
            </button>
          </template>
        </div>
      </div>

      <div class="chat-message-context">
        <FieldEditorWrapper field-name="Location">
          <button
              v-if="location"
              class="chat-message-context-button"
              type="button"
              @click="editingLocation = true"
          >
            {{ location.get("name") }}
          </button>
        </FieldEditorWrapper>

        <span class="chat-message-context-separator">•</span>

        <div class="chat-message-context-item">
          <FieldEditorWrapper field-name="Characters">
            <button
                v-for="characterName in presentCharactersNames"
                :key="characterName"
                type="button"
                class="chat-message-context-button"
                @click="onCharacterClick(characterName)"
            >
              {{ characterName }}
            </button>
          </FieldEditorWrapper>
        </div>
      </div>
    </header>

    <div
        v-if="!editingMessage"
        :key="message.get('active_response')"
        class="chat-message-content chat-message-content-readonly"
        v-html="renderedContent"
    />

    <textarea
        v-else
        v-model="messageDraft"
        class="chat-message-content chat-message-content-editor"
        spellcheck="true"
        aria-label="Message content"
    />
  </article>

  <WindowPrompt
      v-if="location && editingLocation"
      :title="String(location.get('name') ?? 'Location')"
      @close="editingLocation = false"
  >
    <LocationEditor
        :model-value="{ location: location, all_locations: [] }"
    />
  </WindowPrompt>

  <WindowPrompt
      v-if="editingCharacter && selectedCharacter"
      :title="String(selectedCharacter.get('name') ?? 'Character')"
      @close="editingCharacter = false"
  >
    <CharacterEditor :model-value="selectedCharacter" :edit-starting-locations="false" />
  </WindowPrompt>
</template>

<style scoped>
.chat-message-box {
  width: 100%;
  min-height: 200px;

  display: flex;
  flex-direction: column;

  overflow: hidden;

  border: 1px solid var(--primary-accent, #f59e0b);
  border-radius: 0.75rem;

  background: var(--primary-background, #1c1917);
  color: var(--primary-text, #e2e8f0);

  box-shadow:
      0 10px 15px rgb(0 0 0 / 0.18),
      inset 0 1px 0 rgb(255 255 255 / 0.06);
}

.chat-message-header {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;

  padding: 0.75rem 0.9rem;

  background: var(--secondary-background, #44403c);
  border-bottom: 1px solid var(--primary-accent, #f59e0b);
}

.chat-message-header-main {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.chat-message-title {
  flex: 1;
  min-width: 0;

  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;

  font-size: 1rem;
  font-weight: 700;
  line-height: 1.3;

  color: var(--primary-text, #e2e8f0);
}

.chat-message-actions {
  display: flex;
  align-items: center;
  gap: 0.35rem;

  margin-left: auto;
}

.chat-message-action-button {
  padding: 0.15rem 0.4rem;

  border: 1px solid color-mix(
      in srgb,
      var(--primary-accent, #f59e0b) 55%,
      transparent
  );
  border-radius: 0.35rem;

  background: transparent;
  color: inherit;

  font: inherit;
  font-size: 0.75rem;
  line-height: 1.2;

  cursor: pointer;
}

.chat-message-action-button:disabled {
  opacity: 0.5;
  cursor: default;
}

.chat-message-context {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.35rem;

  font-size: 0.8rem;
  line-height: 1.35;

  color: var(--muted-text, #94a3b8);
}

.chat-message-context-item {
  min-width: 0;
}

.chat-message-context-separator {
  color: var(--primary-accent, #f59e0b);
  opacity: 0.75;
}

.chat-message-context-button {
  padding: 0;

  border: none;
  background: transparent;

  color: inherit;
  font: inherit;
  text-decoration: underline;

  cursor: pointer;
}

.chat-message-content {
  width: 100%;
  min-height: 9rem;

  padding: 0.9rem;

  background: color-mix(
      in srgb,
      var(--primary-background, #1c1917) 88%,
      black 12%
  );

  color: var(--primary-text, #e2e8f0);

  font: inherit;
  font-size: 0.95rem;
  line-height: 1.55;
}

.chat-message-content-readonly {
  cursor: default;
  overflow-wrap: anywhere;
  user-select: text;
}

.chat-message-content-readonly :deep(p) {
  margin: 0 0 0.75rem;
}

.chat-message-content-readonly :deep(p:last-child) {
  margin-bottom: 0;
}

.chat-message-content-readonly :deep(a) {
  color: var(--primary-accent, #f59e0b);
  text-decoration: underline;
}

.chat-message-content-readonly :deep(pre) {
  overflow-x: auto;
  padding: 0.75rem;
  border-radius: 0.5rem;
  background: rgb(0 0 0 / 0.25);
}

.chat-message-content-readonly :deep(code) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace;
}

.chat-message-content-readonly :deep(blockquote) {
  margin: 0.75rem 0;
  padding-left: 0.75rem;
  border-left: 3px solid var(--primary-accent, #f59e0b);
  color: var(--muted-text, #94a3b8);
}

.chat-message-content-editor {
  resize: vertical;

  border: none;
  outline: none;

  cursor: text;
}

.chat-message-content-editor:focus {
  box-shadow: inset 0 0 0 1px var(--primary-accent, #f59e0b);
}

.chat-message-response-selector {
  display: inline-flex;
  align-items: center;

  overflow: hidden;

  border: 1px solid color-mix(
      in srgb,
      var(--primary-accent, #f59e0b) 55%,
      transparent
  );
  border-radius: 0.35rem;
}

.chat-message-response-button {
  width: 1.6rem;
  min-height: 1.4rem;
  padding: 0;

  border: none;
  background: transparent;
  color: inherit;

  font: inherit;
  font-size: 0.8rem;
  font-weight: 700;
  line-height: 1;

  cursor: pointer;
}

.chat-message-response-button:hover:not(:disabled) {
  background: color-mix(
      in srgb,
      var(--primary-accent, #f59e0b) 18%,
      transparent
  );
}

.chat-message-response-button:disabled {
  opacity: 0.35;
  cursor: default;
}

.chat-message-response-counter {
  min-width: 3rem;
  padding: 0 0.3rem;

  border-right: 1px solid color-mix(
      in srgb,
      var(--primary-accent, #f59e0b) 35%,
      transparent
  );
  border-left: 1px solid color-mix(
      in srgb,
      var(--primary-accent, #f59e0b) 35%,
      transparent
  );

  font-size: 0.75rem;
  line-height: 1.4;
  text-align: center;
  font-variant-numeric: tabular-nums;
}
</style>