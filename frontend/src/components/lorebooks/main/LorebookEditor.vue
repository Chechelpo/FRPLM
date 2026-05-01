<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { Entry, EntryKey, EntryData } from '@/domain/entities/lorebook/Entry'
import { Lorebook } from '@/domain/entities/lorebook/Lorebook'
import { EntityTypes } from '@/frameworks/entities/EntityTypes'
import { createEntity } from '@/domain/entities/EntityFetch'

const props = defineProps<{
  lorebook: Lorebook | number
}>()

// ---- State --------------------------------------------------------
const entries = ref<Entry[]>([])
const loading = ref(false)
const error = ref<string | null>(null)

// ID extracted from prop (works with entity or raw number)
const lorebookId = computed<number>(() =>
    typeof props.lorebook === 'number' ? props.lorebook : props.lorebook.get('id') as number
)

// Inline editing state
const editingHash = ref<string | null>(null)    // hashKey of the entry being edited
const editName = ref('')
const editContent = ref('')

// ---- Data fetching -------------------------------------------------
async function loadEntries() {
  loading.value = true
  error.value = null
  try {
    entries.value = await Entry.ofLorebook(lorebookId.value);
  } catch (e) {
    error.value = 'Failed to load entries.'
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(loadEntries)
watch(lorebookId, loadEntries)

// ---- Create -------------------------------------------------------
async function addEntry() {
  try {
    const newEntry = await createEntity<EntryKey, EntryData, Entry>(
        {
          lorebook_id: lorebookId.value
        },
        null,
        EntityTypes.ENTRY,
        Entry
    )
    entries.value.push(newEntry)
  } catch (e) {
    console.error(e)
    error.value = 'Could not create entry.'
  }
}

// ---- Delete (placeholder hook) ------------------------------------
// TODO: implement once backend DELETE endpoint is ready
// async function deleteEntry(entry: Entry) {
//   // Example call:
//   // await DeleteEntity(entry.key, EntityTypes.ENTRY)
//   // entries.value = entries.value.filter(e => e.hashKey() !== entry.hashKey())
// }

// ---- Inline editing ------------------------------------------------
function startEdit(entry: Entry) {
  editingHash.value = entry.hashKey()
  editName.value    = (entry.get('name') as string) ?? ''
  editContent.value = (entry.get('content') as string) ?? ''
}

function cancelEdit() {
  editingHash.value = null
}

async function saveEdit(entry: Entry) {
  if (!editingHash.value) return
  try {
    await entry.update('name', editName.value)
    await entry.update('content', editContent.value)
    editingHash.value = null
  } catch (e) {
    console.error(e)
    alert('Failed to save changes')
  }
}

// ---- Helpers -------------------------------------------------------
function previewText(content: unknown) {
  return (content as string)?.slice(0, 80) ?? ''
}
</script>

<template>
  <div class="entry-list-editor">
    <div class="toolbar">
      <button @click="addEntry" class="btn add-btn">+ New Entry</button>
      <span v-if="loading" class="status">Loading…</span>
      <span v-if="error" class="status error">{{ error }}</span>
    </div>

    <ul v-if="entries.length" class="entry-list">
      <li
          v-for="entry in entries"
          :key='entry.hashKey()'
          class="entry-card"
          :class="{ editing: editingHash === entry.hashKey() }"
      >
        <!-- Read‑only preview -->
        <div v-if="editingHash !== entry.hashKey()" class="preview">
          <span class="entry-name">{{
              entry.get('name') ?? 'Untitled'
            }}</span>
          <span class="entry-content">{{
              previewText(entry.get('content'))
            }}</span>
          <div class="entry-actions">
            <button @click="startEdit(entry)" class="btn edit-btn">Edit</button>
            <!--
            <button @click="deleteEntry(entry)" class="btn del-btn">Delete</button>
            -->
          </div>
        </div>

        <!-- Inline edit -->
        <div v-else class="edit-form">
          <input
              v-model="editName"
              placeholder="Entry name"
              class="edit-input"
          />
          <textarea
              v-model="editContent"
              rows="3"
              placeholder="Content"
              class="edit-textarea"
          ></textarea>
          <div class="edit-actions">
            <button @click="saveEdit(entry)" class="btn save-btn">Save</button>
            <button @click="cancelEdit" class="btn cancel-btn">Cancel</button>
          </div>
        </div>
      </li>
    </ul>

    <p v-else class="empty-msg">No entries yet. Click “New Entry” to create one.</p>
  </div>
</template>

<style scoped>
.entry-list-editor {
  max-width: 640px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 8px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn {
  padding: 4px 10px;
  border: 1px solid #999;
  background: #f5f5f5;
  cursor: pointer;
  font-size: 0.9rem;
  border-radius: 4px;
}

.add-btn {
  background: #4caf50;
  color: white;
  border-color: #4caf50;
}

.status {
  font-size: 0.85rem;
  color: #666;
}
.error {
  color: #c0392b;
  font-weight: 500;
}

.entry-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.entry-card {
  border: 1px solid #ddd;
  border-radius: 6px;
  padding: 8px 12px;
  transition: box-shadow 0.2s;
}

.entry-card.editing {
  border-color: #2196f3;
  box-shadow: 0 0 0 1px #2196f3;
}

.preview {
  display: flex;
  align-items: center;
  gap: 12px;
}

.entry-name {
  font-weight: 600;
  min-width: 120px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.entry-content {
  flex: 1;
  color: #555;
  font-size: 0.9em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.entry-actions,
.edit-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.edit-btn {
  background: #2196f3;
  color: white;
  border-color: #2196f3;
}

.del-btn {
  background: #e74c3c;
  color: white;
  border-color: #e74c3c;
}

.save-btn {
  background: #4caf50;
  color: white;
  border-color: #4caf50;
}

.cancel-btn {
  background: #aaa;
  color: white;
  border-color: #aaa;
}

.edit-form {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 4px;
}

.edit-input,
.edit-textarea {
  width: 100%;
  padding: 4px 6px;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 0.9rem;
}

.edit-textarea {
  resize: vertical;
}

.empty-msg {
  color: #888;
  font-style: italic;
}
</style>