<script setup lang="ts">
import {ref, onMounted, computed, watch} from 'vue'
import {Entry, Lorebook, Outlet} from '@/domain/Lorebook'
import EntryEditor from "@/components/lorebooks/EntryEditor.vue";
import SearchBar from "@/components/utils/SearchBar.vue";
import {API_BASE} from "@/config";
import {EntityTypes} from "@/domain/EntityTypes";
import IconButton from "@/components/utils/buttons/IconButton.vue";

const model = defineModel<Lorebook>({required: true, type: Lorebook})
const props = withDefaults(
    defineProps<{
      otherLorebooks?: Lorebook[]
    }>(),
    {
      otherLorebooks: () => []
    }
)
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
const pageSizeOptions = [20, 50, 100] as const;

const pageSize = ref<number>(20);
const currentPage = ref<number>(1);

const pageCount = computed<number>(() => {
  return Math.max(
      1,
      Math.ceil(filteredEntries.value.length / pageSize.value),
  );
});

const paginatedEntries = computed<Entry[]>(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  const end = start + pageSize.value;

  return filteredEntries.value.slice(start, end);
});

const firstVisibleEntry = computed<number>(() => {
  if (filteredEntries.value.length === 0) {
    return 0;
  }

  return (currentPage.value - 1) * pageSize.value + 1;
});

const lastVisibleEntry = computed<number>(() => {
  return Math.min(
      currentPage.value * pageSize.value,
      filteredEntries.value.length,
  );
});

function previousPage(): void {
  currentPage.value = Math.max(1, currentPage.value - 1);
}

function nextPage(): void {
  currentPage.value = Math.min(
      pageCount.value,
      currentPage.value + 1,
  );
}

function setPageSize(event: Event): void {
  const select = event.target as HTMLSelectElement;
  const requestedSize = Number(select.value);

  if (!pageSizeOptions.includes(requestedSize as 20 | 50 | 100)) {
    return;
  }

  pageSize.value = requestedSize;
  currentPage.value = 1;
}

watch(searchQuery, () => {
  currentPage.value = 1;
});

watch(
    () => filteredEntries.value.length,
    () => {
      currentPage.value = Math.min(
          currentPage.value,
          pageCount.value,
      );
    },
);

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
async function moveEntry(entry:Entry, toLorebookId:number) {
  const success = await entry.moveToLorebook(toLorebookId);

  if (!success){
    console.error("Error when moving entry")
    return;
  }

  entries.value = entries.value.filter(other => !other.equals(entry))
}
</script>

<template>
  <div class="entry-list-editor">
    <div class="entry-list-controls">
      <div
          v-if="loading || error"
          class="toolbar"
      >
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

      <div class="entry-list-toolbar">
        <SearchBar
            class="entry-list-toolbar__search"
            placeholder="Search entries by name"
            @update:search="value => searchQuery = value"
        />

        <div class="action-row">
          <IconButton
              title="Create new entry"
              variant="accent"
              :disabled="loading || importing"
              @click="addEntry"
          >
            <svg
                viewBox="0 0 24 24"
                aria-hidden="true"
            >
              <path d="M12 5v14" />
              <path d="M5 12h14" />
            </svg>
          </IconButton>

          <input
              ref="importInput"
              type="file"
              class="hidden-file-input"
              accept=".json,.yaml,.yml,.txt"
              @change="onImportFileSelected"
          />

          <IconButton
              title="Import entries from file"
              :loading="importing"
              :disabled="loading"
              @click="openImportPicker"
          >
            <svg
                viewBox="0 0 24 24"
                aria-hidden="true"
            >
              <path d="M12 21V9" />
              <path d="m7 14 5-5 5 5" />
              <path d="M5 3h14" />
            </svg>
          </IconButton>
        </div>
      </div>

      <div
          v-if="filteredEntries.length"
          class="pagination-toolbar"
      >
        <div class="pagination-toolbar__summary">
          Showing
          <strong>{{ firstVisibleEntry }}–{{ lastVisibleEntry }}</strong>
          of
          <strong>{{ filteredEntries.length }}</strong>
          entries
        </div>

        <div class="pagination-toolbar__controls">
          <label class="page-size-control">
            <span>Entries per page</span>

            <select
                class="page-size-control__select"
                :value="pageSize"
                @change="setPageSize"
            >
              <option
                  v-for="size in pageSizeOptions"
                  :key="size"
                  :value="size"
              >
                {{ size }}
              </option>
            </select>
          </label>

          <div class="page-navigation">
            <IconButton
                title="Previous page"
                :disabled="currentPage <= 1"
                @click="previousPage"
            >
              <svg
                  viewBox="0 0 24 24"
                  aria-hidden="true"
              >
                <path d="m15 18-6-6 6-6" />
              </svg>
            </IconButton>

            <span class="page-navigation__number">
              Page
              <strong>{{ currentPage }}</strong>
              of
              <strong>{{ pageCount }}</strong>
            </span>

            <IconButton
                title="Next page"
                :disabled="currentPage >= pageCount"
                @click="nextPage"
            >
              <svg
                  viewBox="0 0 24 24"
                  aria-hidden="true"
              >
                <path d="m9 18 6-6-6-6" />
              </svg>
            </IconButton>
          </div>
        </div>
      </div>
    </div>

    <div class="entry-list-scroll">
      <ul
          v-if="paginatedEntries.length"
          class="entry-list"
      >
        <li
            v-for="entry in paginatedEntries"
            :key="entry.hashKey()"
            class="entry-card"
        >
          <EntryEditor
              :entry="entry"
              :keywords="keywords"
              :outlets="outlets"
              :move-entry="moveEntry"
              @new-keyword="onKeywordCreate"
              @new-outlet="onOutletCreate"
              @delete="deleteEntry"
          />
        </li>
      </ul>

      <p
          v-else-if="!loading && entries.length === 0"
          class="empty-msg"
      >
        No entries yet. Create a new entry or import entries from a file.
      </p>

      <p
          v-else-if="!loading"
          class="empty-msg"
      >
        No entries match the current search.
      </p>
    </div>
  </div>
</template>

<style scoped>
.entry-list-editor {
  display: flex;
  flex-direction: column;

  width: 100%;
  height: min(70dvh, 900px);
  min-width: 0;
  min-height: 20rem;
  max-height: 70dvh;

  overflow: hidden;
  box-sizing: border-box;
}

.entry-list-scroll {
  flex: 1 1 0;
  width: 100%;
  min-width: 0;
  min-height: 0;

  padding: 0.75rem 0.5rem;

  overflow-x: hidden;
  overflow-y: auto;

  box-sizing: border-box;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
}

.entry-list-controls {
  position: relative;
  z-index: 20;

  display: flex;
  flex: 0 0 auto;
  flex-direction: column;
  gap: 0.65rem;

  width: 100%;
  min-width: 0;
  padding: 0.5rem;

  box-sizing: border-box;

  background: var(--primary-background, #1c1917);

  border-bottom: 1px solid color-mix(
      in srgb,
      currentColor 16%,
      transparent
  );

  box-shadow: 0 4px 10px rgb(0 0 0 / 0.12);
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 0.5rem;

  min-height: 1.5rem;
}

.entry-list-toolbar {
  display: flex;
  align-items: center;
  gap: 0.65rem;

  width: 100%;
  min-width: 0;
}

.entry-list-toolbar__search {
  flex: 1 1 auto;
  min-width: 0;
}

.action-row {
  display: flex;
  align-items: center;
  gap: 0.4rem;

  flex: 0 0 auto;
  padding: 0.3rem;

  box-sizing: border-box;

  background: color-mix(
      in srgb,
      var(--primary-background, #1c1917) 90%,
      transparent
  );

  border: 1px solid color-mix(
      in srgb,
      currentColor 16%,
      transparent
  );

  border-radius: 0.5rem;
  box-shadow: 0 2px 8px rgb(0 0 0 / 0.1);
}

.hidden-file-input {
  display: none;
}

.status {
  font-size: 0.85rem;
  color: var(--muted-text, #94a3b8);
}

.error {
  color: var(--danger-color, #dc2626);
  font-weight: 500;
}

.pagination-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;

  width: 100%;
  min-width: 0;
  padding: 0.45rem 0.55rem;

  box-sizing: border-box;

  background: color-mix(
      in srgb,
      var(--secondary-background, #44403c) 75%,
      transparent
  );

  border: 1px solid color-mix(
      in srgb,
      currentColor 16%,
      transparent
  );

  border-radius: 0.5rem;
}

.pagination-toolbar__summary {
  flex: 1 1 auto;

  color: var(--muted-text, #94a3b8);
  font-size: 0.85rem;
  white-space: nowrap;
}

.pagination-toolbar__summary strong {
  color: var(--primary-text, currentColor);
  font-weight: 600;
}

.pagination-toolbar__controls {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 1rem;

  flex: 0 0 auto;
}

.page-size-control {
  display: flex;
  align-items: center;
  gap: 0.5rem;

  color: var(--muted-text, #94a3b8);
  font-size: 0.85rem;
  white-space: nowrap;
}

.page-size-control__select {
  min-height: 2rem;
  padding: 0.25rem 1.8rem 0.25rem 0.55rem;

  color: var(--primary-text, #e2e8f0);
  background: var(--primary-background, #1c1917);

  border: 1px solid color-mix(
      in srgb,
      currentColor 22%,
      transparent
  );

  border-radius: 0.35rem;

  font: inherit;
  cursor: pointer;
}

.page-size-control__select:hover {
  border-color: var(--primary-accent, #f59e0b);
}

.page-size-control__select:focus-visible {
  outline: 2px solid var(--primary-accent, #f59e0b);
  outline-offset: 2px;
}

.page-navigation {
  display: flex;
  align-items: center;
  gap: 0.45rem;
}

.page-navigation__number {
  min-width: 7.5rem;

  color: var(--muted-text, #94a3b8);
  font-size: 0.85rem;
  text-align: center;
  white-space: nowrap;
}

.page-navigation__number strong {
  color: var(--primary-text, currentColor);
  font-weight: 600;
}

.entry-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;

  width: 100%;
  min-width: 0;

  list-style: none;
  padding: 0;
  margin: 0;

  box-sizing: border-box;
}

.entry-card {
  width: 100%;
  max-width: 100%;
  min-width: 0;
  padding: 0.5rem 0.75rem;

  box-sizing: border-box;
  overflow: hidden;

  background: var(--secondary-background, #44403c);

  border: 1px solid var(--primary-accent, #f59e0b);
  border-radius: 0.4rem;

  opacity: 0.82;

  transition:
      box-shadow 150ms ease,
      opacity 150ms ease;
}

.entry-card:hover,
.entry-card:focus-within {
  opacity: 1;

  box-shadow:
      0 4px 12px rgb(0 0 0 / 0.15),
      0 0 0 1px color-mix(
          in srgb,
          var(--primary-accent, #f59e0b) 30%,
          transparent
      );
}

/*
 * EntryEditor declares width: 100% on these elements.
 * Force border-box sizing so their padding and border stay inside the card.
 */
.entry-card :deep(.entry-header),
.entry-card :deep(.expandedTop) {
  width: 100%;
  max-width: 100%;
  min-width: 0;

  box-sizing: border-box;
}

.empty-msg {
  width: 100%;
  margin: 0;
  padding: 1rem;

  box-sizing: border-box;

  color: var(--muted-text, #94a3b8);
  text-align: center;
  font-style: italic;

  border: 1px dashed color-mix(
      in srgb,
      currentColor 20%,
      transparent
  );

  border-radius: 0.5rem;
}

@media (max-width: 760px) {
  .entry-list-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .entry-list-toolbar__search {
    width: 100%;
  }

  .action-row {
    align-self: flex-end;
  }

  .pagination-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .pagination-toolbar__summary {
    text-align: center;
  }

  .pagination-toolbar__controls {
    justify-content: space-between;
    width: 100%;
  }
}

@media (max-width: 500px) {
  .pagination-toolbar__controls {
    align-items: stretch;
    flex-direction: column;
  }

  .page-size-control,
  .page-navigation {
    justify-content: center;
  }

  .entry-card {
    padding: 0.5rem;
  }
}
</style>