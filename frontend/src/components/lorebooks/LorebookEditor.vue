<script setup lang="ts">
import {ref, onMounted, computed, watch} from 'vue'
import {Entry, Lorebook} from '@/domain/entities/Lorebook'
import EntryEditor from "@/components/lorebooks/EntryEditor.vue";
import SearchBar from "@/components/utils/SearchBar.vue";

const model = defineModel<Lorebook>({required: true, type: Lorebook})

// ---- State --------------------------------------------------------
const entries = ref<Entry[]>([]);
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

// ---- Data fetching -------------------------------------------------
async function loadEntries() {
  loading.value = true
  console.info(`Editing ${model.value}`)
  entries.value = await model.value.getEntries()

  loading.value = false
}

onMounted(loadEntries)
watch(model, loadEntries)

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
    ></searchBar>
    <button
        type="button"
        class="btn add-btn"
        @click="addEntry"
    >
      + New Entry
    </button>
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

<style scoped>
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