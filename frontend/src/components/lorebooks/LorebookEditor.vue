<script setup lang="ts">
import {ref, onMounted, computed, watch} from 'vue'
import {Entry, Lorebook, Outlet} from '@/domain/Lorebook'
import EntryEditor from "@/components/lorebooks/EntryEditor.vue";
import SearchBar from "@/components/utils/SearchBar.vue";
import {API_BASE} from "@/config";
import {EntityTypes} from "@/domain/EntityTypes";

const model = defineModel<Lorebook>({required: true, type: Lorebook})

// ---- State --------------------------------------------------------
const entries = ref<Entry[]>([]);
const keywords = ref<string[]>([]);
const outlets = ref<string[]>([]);

const searchQuery = ref('');

const filteredEntries = computed(() => {
  const query = searchQuery.value.trim().toLowerCase();

  if (!query) {
    return entries.value;
  }

  return entries.value.filter(entry => {
    const entryName = entry.get('name');

    return typeof entryName === 'string'
        && entryName.toLowerCase().includes(query);
  });
});

const error = ref<string | null>(null)
const loading = ref<boolean>(false)

// Import : ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
const importInput = ref<HTMLInputElement | null>(null);
const importing = ref<boolean>(false);

function openImportPicker(): void {
  importInput.value?.click();
}

async function onImportFileSelected(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];

  if (!file) {
    return;
  }

  try {
    importing.value = true;
    error.value = null;

    const formData = new FormData();
    formData.append('file', file);


    const text = await file.text();

    const response = await fetch(
        `${API_BASE}/${EntityTypes.ENTRY}/${model.value.get('id')}/import`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: text,
        },
    );

    if (!response.ok) {
      console.error(`Import failed with status ${response.status}`);
      return;
    }

    await load();
  } catch (e) {
    console.error(e);
    error.value = 'Could not import entries.';
  } finally {
    importing.value = false;

    // Allows selecting the same file again.
    input.value = '';
  }
}

// ---- Data fetching -------------------------------------------------
async function load() {
  loading.value = true

  console.info(`Editing ${model.value}`)
  entries.value = await model.value.getEntries()
  keywords.value = await model.value.keywords();
  outlets.value = await Outlet.outlets();

  loading.value = false
}

onMounted(load)
watch(model, load)

// ---- Create / delete -------------------------------------------------------
async function addEntry() {
  try {
    const newEntry = await model.value.newEntry();
    entries.value.push(newEntry);
  } catch (e) {
    console.error(e);
    error.value = 'Could not create entry.';
  }
}

async function deleteEntry(entry: Entry): Promise<void> {
  try {
    await model.value.deleteEntry(entry);

    entries.value = entries.value.filter(
        current => current.hashKey() !== entry.hashKey(),
    );
  } catch (e) {
    console.error(e);
    error.value = "Could not delete entry.";
  }
}

function onOutletCreate(name: string): void {
  outlets.value.push(name);
}

function onKeywordCreate(name: string): void {
  keywords.value.push(name);
}
</script>

<template>
  <div class="entry-list-editor">
    <div class="toolbar">
      <span
          v-if="loading"
          class="status"
      >
        Loading…
      </span>

      <span
          v-if="error"
          class="status error"
      >
        {{ error }}
      </span>
    </div>

    <!-- Actual useful things -->
    <searchBar
        placeholder="Search entries by name"
        @update:search="value => searchQuery = value"
    />
    <div class = action-row>
      <button
          type="button"
          class="btn add-btn"
          @click="addEntry"
      >
        + New Entry
      </button>

      <input
          ref="importInput"
          type="file"
          class="hidden-file-input"
          accept=".json,.yaml,.yml,.txt"
          @change="onImportFileSelected"
      />
      <button
          type="button"
          class="btn import-btn"
          :disabled="importing || loading"
          @click="openImportPicker"
      >
        {{ importing ? 'Importing…' : 'Import' }}
      </button>
    </div>
    <ul
        v-if="entries.length"
        class="entry-list"
    >
      <li
          v-for="entry in filteredEntries"
          :key="entry.hashKey()"
          class="entry-card"
      >
        <EntryEditor
            :entry="entry as Entry"
            :keywords="keywords"
            :outlets="outlets"
            @new-keyword="onKeywordCreate"
            @new-outlet="onOutletCreate"
            @delete="deleteEntry"
        />
      </li>
    </ul>

    <p
        v-else-if="!loading"
        class="empty-msg"
    >
      No entries yet. Click “New Entry” to create one.
    </p>
  </div>
</template>

<style scoped>z
.action-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.import-btn {
  background: #1976d2;
  color: blue;
  border-color: #1976d2;
}

.import-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.hidden-file-input {
  display: none;
}

.entry-list-editor {
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
  border: 1px solid var(--primary-accent);
  border-radius: 6px;
  padding: 8px 12px;

  background: var(--secondary-background);
  opacity: 0.7;

  transition: box-shadow 0.2s;
}

.empty-msg {
  color: #888;
  font-style: italic;
}
</style>