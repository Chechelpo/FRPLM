<script setup lang="ts">
import { Message } from "@/domain/Session";
import { onMounted, ref, shallowRef, watch } from "vue";
import { Location } from "@/domain/World";
import { Character } from "@/domain/Characters";
import WindowPrompt from "@/components/utils/prompts/WindowPrompt.vue";
import LocationEditor from "@/components/space/LocationEditor.vue";
import CharacterEditor from "@/components/char/CharacterEditor.vue";
import FieldEditorWrapper from "@/components/utils/FieldEditorWrapper.vue";

const props = defineProps<{
  message: Message;
  title: string;
}>();

const emits = defineEmits<{
  (e: 'delete' , value:Message ) : void;
}>();

const location = shallowRef<Location>();
const presentCharacters = shallowRef<Character[]>([]);
const presentCharactersNames = shallowRef<string[]>([]);

const editingLocation = ref<boolean>(false);

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// Message edit
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
const editingMessage = ref<boolean>(false);
const savingMessage = ref<boolean>(false);

const displayedContent = ref<string>("");
const messageDraft = ref<string>("");

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
            @click="emits('delete', props.message)"
          >
           D
          </button>
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
        class="chat-message-content chat-message-content-readonly"
    >
      {{ displayedContent }}
    </div>

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
    <CharacterEditor :model-value="selectedCharacter" />
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
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  user-select: text;
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
</style>