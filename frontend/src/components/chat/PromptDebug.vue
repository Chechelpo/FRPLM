<script setup lang="ts">
import {computed, ref, watch} from "vue";
import {
  ChatCompletionRole,
} from "@/types/ChatCompletions";
import {PromptDTO} from "@/types/DTOs";
import {Entry, Lorebook} from "@/domain/Lorebook";
import {DomainPromptDTO, toDomainPrompt} from "@/utils/PromptThings";

const props = defineProps<{
  modelValue: PromptDTO;
}>();

const domainPrompt = computed<DomainPromptDTO>(() => toDomainPrompt(props.modelValue));

type ViewValue = "all" | "raw" | string;

const selectedView = ref<ViewValue>("all");

function lorebookViewValue(lorebook: Lorebook): string {
  return `lorebook-${lorebook.get("id")}`;
}

function parseSelectedLorebookId(): number | null {
  if (typeof selectedView.value !== "string") return null;
  if (!selectedView.value.startsWith("lorebook-")) return null;
  const id = Number(selectedView.value.replace("lorebook-", ""));
  return Number.isNaN(id) ? null : id;
}

function normalizeSelection(): void {
  const lorebooks = domainPrompt.value.lorebooks;
  if (lorebooks.length === 0) {
    selectedView.value = "raw";
    return;
  }
  if (selectedView.value === "all" || selectedView.value === "raw") return;

  const id = parseSelectedLorebookId();
  const stillValid = id !== null && lorebooks.some((lb) => lb.get("id") === id);
  if (!stillValid) selectedView.value = "all";
}

watch(() => domainPrompt.value.lorebooks, normalizeSelection, {immediate: true});

const displayedLorebooks = computed<Lorebook[]>(() => {
  if (selectedView.value === "all") return domainPrompt.value.lorebooks;

  const id = parseSelectedLorebookId();
  if (id === null) return [];
  return domainPrompt.value.lorebooks.filter((lb) => lb.get("id") === id);
});

function entriesForLorebook(lorebook: Lorebook): Entry[] {
  return domainPrompt.value.entriesByLorebookId.get(lorebook.get("id")) ?? [];
}

function totalActivatedCount(): number {
  return Array.from(domainPrompt.value.entriesByLorebookId.values()).reduce(
      (sum, entries) => sum + entries.length,
      0
  );
}

function entryKey(entry: Entry): string {
  return `${entry.get("lorebook_id")}-${entry.get("entry_id")}`;
}

const openEntries = ref<Record<string, boolean>>({});

function toggleEntry(entry: Entry): void {
  const key = entryKey(entry);
  openEntries.value[key] = !openEntries.value[key];
}

function isEntryOpen(entry: Entry): boolean {
  return !!openEntries.value[entryKey(entry)];
}

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
      <div class="chat-request-title">Chat Completion Request</div>
      <div class="chat-request-model">
        Model: {{ domainPrompt.rawRequest.model }}
      </div>
    </header>

    <div class="debug-control">
      <label class="debug-control-label" for="prompt-debug-view">View</label>
      <select
          id="prompt-debug-view"
          v-model="selectedView"
          class="debug-view-selector"
      >
        <option value="all">
          All Lorebooks ({{ totalActivatedCount() }})
        </option>
        <option
            v-for="lorebook in domainPrompt.lorebooks"
            :key="lorebook.get('id')"
            :value="lorebookViewValue(lorebook)"
        >
          {{ lorebook.get('name') }} ({{ entriesForLorebook(lorebook).length }})
        </option>
        <option value="raw">Raw Request</option>
      </select>
    </div>

    <div v-if="selectedView === 'raw'" class="chat-request-messages">
      <article
          v-for="(message, index) in domainPrompt.rawRequest.messages"
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
          <span class="chat-request-message-index">#{{ index + 1 }}</span>
        </header>

        <pre class="chat-request-message-content">{{ message.content }}</pre>
      </article>
    </div>

    <div v-else class="lorebooks-panel">
      <section
          v-for="lorebook in displayedLorebooks"
          :key="lorebook.get('id')"
          class="lorebook-group"
      >
        <h4 class="lorebook-group-title">
          {{ lorebook.get('name') }}
          <span class="lorebook-group-count">
            {{ entriesForLorebook(lorebook).length }}
          </span>
        </h4>

        <ul
            v-if="entriesForLorebook(lorebook).length > 0"
            class="lorebook-entries"
        >
          <li
              v-for="entry in entriesForLorebook(lorebook)"
              :key="entryKey(entry)"
              class="lorebook-entry"
          >
            <button
                type="button"
                class="entry-toggle"
                @click="toggleEntry(entry)"
            >
              <span class="entry-id">#{{ entry.get('entry_id') }}</span>
              <span class="entry-name">
                {{ entry.get('name') ?? '(unnamed)' }}
              </span>
              <span class="entry-chevron" :class="{ 'is-open': isEntryOpen(entry) }">
                ▼
              </span>
            </button>

            <div v-if="isEntryOpen(entry)" class="entry-content-wrapper">
              <pre class="entry-content">{{
                  entry.get('content') ?? '(no content)'
                }}</pre>
            </div>
          </li>
        </ul>

        <p v-else class="lorebook-empty">No activated entries</p>
      </section>

      <p
          v-if="displayedLorebooks.length === 0"
          class="lorebook-empty"
      >
        No lorebooks selected
      </p>
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

.debug-control {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.debug-control-label {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--muted-text, #94a3b8);
}

.debug-view-selector {
  flex: 1;
  padding: 0.4rem 0.55rem;
  border: 1px solid color-mix(
      in srgb,
      var(--primary-accent, #f59e0b) 40%,
      transparent
  );
  border-radius: 0.5rem;
  background: var(--secondary-background, #44403c);
  color: var(--primary-text, #e2e8f0);
  font: inherit;
  font-size: 0.85rem;
  cursor: pointer;
}

.lorebooks-panel {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}

.lorebook-group {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.lorebook-group-title {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.85rem;
  font-weight: 700;
}

.lorebook-group-count {
  margin-left: auto;
  padding: 0.15rem 0.45rem;
  border-radius: 999px;
  background: color-mix(
      in srgb,
      var(--primary-accent, #f59e0b) 20%,
      transparent
  );
  font-size: 0.75rem;
  font-weight: 600;
}

.lorebook-entries {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.lorebook-empty {
  margin: 0;
  font-size: 0.8rem;
  color: var(--muted-text, #94a3b8);
}

.lorebook-entry {
  overflow: hidden;
  border: 1px solid color-mix(
      in srgb,
      var(--primary-accent, #f59e0b) 30%,
      transparent
  );
  border-radius: 0.5rem;
  background: color-mix(
      in srgb,
      var(--primary-background, #1c1917) 90%,
      black 10%
  );
}

.entry-toggle {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.65rem;
  background: transparent;
  border: none;
  color: inherit;
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.entry-toggle:hover {
  background: color-mix(
      in srgb,
      var(--primary-accent, #f59e0b) 10%,
      transparent
  );
}

.entry-id {
  font-size: 0.75rem;
  color: var(--muted-text, #94a3b8);
}

.entry-name {
  flex: 1;
  font-size: 0.85rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.entry-chevron {
  font-size: 0.7rem;
  color: var(--muted-text, #94a3b8);
  transition: transform 0.15s ease;
}

.entry-chevron.is-open {
  transform: rotate(180deg);
}

.entry-content-wrapper {
  padding: 0 0.65rem 0.55rem;
}

.entry-content {
  margin: 0;
  padding: 0.5rem;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  font: inherit;
  font-size: 0.8rem;
  line-height: 1.45;
  background: var(--secondary-background, #44403c);
  border-radius: 0.4rem;
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