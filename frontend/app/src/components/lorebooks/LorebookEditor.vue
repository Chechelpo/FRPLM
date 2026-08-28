<script setup lang="ts">
import {     Entry,
    Lorebook,
    Outlet,
    EntityTypes,
    getEntityController,
    fetchApi } from "@frplm/host-sdk";

import {ref, onMounted, computed, watch} from 'vue'
import EntryEditor from "@components/lorebooks/EntryEditor.vue";
import SearchBar from "@components/utils/SearchBar.vue";
import IconButton from "@components/utils/buttons/IconButton.vue";

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

    const response = await fetchApi(
        `${getEntityController(EntityTypes.ENTRY)}/${model.value.get('id')}/import`,
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
  try {
    const modelToEdit = model.value

    console.info(`Editing ${modelToEdit}`)

    const [loadedEntries, loadedKeywords, loadedOutlets] = await Promise.all([
      modelToEdit.getEntries(),
      modelToEdit.keywords(),
      Outlet.outlets(),
    ])

    entries.value = loadedEntries
    keywords.value = loadedKeywords
    outlets.value = loadedOutlets
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(model, load)

// ---- Create / delete -------------------------------------------------------
async function addEntry() {
  const newEntryName : string | null = window.prompt("Enter new entry name ");
  if (newEntryName == null) return;
  try {
    const newEntry = await model.value.newEntry(newEntryName);
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
  <section
      class="entry-list-editor edit-box edit-box--accent"
      :aria-busy="loading"
  >
    <header class="edit-box__header entry-list-editor__header">
      <div class="edit-box__header-main">
        <span class="edit-box__eyebrow">
          Lorebook
        </span>

        <div class="edit-box__title-row">
          <h2 class="edit-box__title">
            Entries
          </h2>

          <span class="edit-box__count">
            {{ filteredEntries.length }}
          </span>
        </div>

        <p class="edit-box__description">
          Search, create, import and edit lorebook entries.
        </p>
      </div>

      <div class="edit-box__actions">
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
    </header>

    <div class="entry-list-editor__controls">
      <div class="edit-box__toolbar entry-search-toolbar">
        <div class="edit-box__toolbar-main">
          <SearchBar
              v-model:search="searchQuery"
              class="entry-search-toolbar__search"
              placeholder="Search entries by name"
              aria-label="Search lorebook entries by name"
          />
        </div>

        <div
            v-if="loading || importing"
            class="entry-status"
            role="status"
            aria-live="polite"
        >
          <span class="edit-box__spinner" />

          <span>
            {{ importing ? "Importing entries…" : "Loading entries…" }}
          </span>
        </div>
      </div>

      <div
          v-if="error"
          class="entry-error"
          role="alert"
      >
        <svg
            viewBox="0 0 24 24"
            aria-hidden="true"
        >
          <path d="M12 9v4" />
          <path d="M12 17h.01" />
          <path d="M10.3 3.8 2.2 18a2 2 0 0 0 1.8 3h16a2 2 0 0 0 1.8-3L13.7 3.8a2 2 0 0 0-3.4 0Z" />
        </svg>

        <span>{{ error }}</span>

        <button
            type="button"
            aria-label="Dismiss error"
            @click="error = null"
        >
          <svg
              viewBox="0 0 24 24"
              aria-hidden="true"
          >
            <path d="M6 6l12 12" />
            <path d="M18 6 6 18" />
          </svg>
        </button>
      </div>

      <div
          v-if="filteredEntries.length"
          class="pagination-toolbar"
      >
        <div class="pagination-toolbar__summary">
          Showing
          <strong>
            {{ firstVisibleEntry }}–{{ lastVisibleEntry }}
          </strong>
          of
          <strong>{{ filteredEntries.length }}</strong>
          entries
        </div>

        <div class="pagination-toolbar__controls">
          <label class="page-size-control">
            <span>Per page</span>

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

          <div
              class="page-navigation"
              aria-label="Entry pagination"
          >
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
              <strong>{{ currentPage }}</strong>
              /
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

    <div
        class="entry-list-editor__viewport"
        tabindex="0"
        aria-label="Lorebook entry editors"
    >
      <div
          v-if="loading && entries.length === 0"
          class="edit-box__state entry-list-state"
      >
        <span class="edit-box__spinner" />

        <div class="edit-box__state-content">
          <strong class="edit-box__state-title">
            Loading entries
          </strong>

          <p class="edit-box__state-description">
            Retrieving lorebook entries, keywords and outlets.
          </p>
        </div>
      </div>

      <ul
          v-else-if="paginatedEntries.length"
          class="entry-list"
      >
        <li
            v-for="entry in paginatedEntries"
            :key="entry.hashKey()"
            class="entry-card"
        >
          <EntryEditor
              class="entry-card__editor"
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

      <div
          v-else-if="!loading && entries.length === 0"
          class="
          edit-box__state
          edit-box__state--vertical
          entry-list-state
        "
      >
        <div class="edit-box__state-icon">
          <svg
              viewBox="0 0 24 24"
              aria-hidden="true"
          >
            <path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H11v17H6.5A2.5 2.5 0 0 0 4 22V5.5Z" />
            <path d="M20 5.5A2.5 2.5 0 0 0 17.5 3H13v17h4.5A2.5 2.5 0 0 1 20 22V5.5Z" />
          </svg>
        </div>

        <div class="edit-box__state-content">
          <strong class="edit-box__state-title">
            No entries
          </strong>

          <p class="edit-box__state-description">
            Create a new entry or import entries from a file.
          </p>
        </div>

        <button
            class="edit-box__action edit-box__action--accent"
            type="button"
            @click="addEntry"
        >
          Create first entry
        </button>
      </div>

      <div
          v-else-if="!loading"
          class="
          edit-box__state
          edit-box__state--vertical
          entry-list-state
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
            No matching entries
          </strong>

          <p class="edit-box__state-description">
            No entry contains
            “{{ searchQuery.trim() }}”
            in its name.
          </p>
        </div>

        <button
            class="edit-box__action"
            type="button"
            @click="searchQuery = ''"
        >
          Clear search
        </button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.entry-list-editor {
  /*
   * Override this variable when the component appears below a fixed
   * application header:
   *
   * style="--entry-editor-viewport-offset: 5rem"
   */
  --entry-editor-viewport-offset: 1rem;

  display: grid;
  grid-template-rows:
    auto
    auto
    minmax(0, 1fr);

  width: 100%;
  min-width: 0;

  height: calc(
      100dvh - var(--entry-editor-viewport-offset)
  );

  max-height: calc(
      100dvh - var(--entry-editor-viewport-offset)
  );

  overflow: hidden;
}

/* -------------------------------------------------------------------------- */
/* Header                                                                     */
/* -------------------------------------------------------------------------- */

.entry-list-editor__header {
  z-index: 3;
  align-items: center;
}

.hidden-file-input {
  display: none;
}

/* -------------------------------------------------------------------------- */
/* Fixed controls                                                             */
/* -------------------------------------------------------------------------- */

.entry-list-editor__controls {
  position: relative;
  z-index: 2;

  display: flex;
  flex-direction: column;
  gap: var(--space-2);

  min-width: 0;
  padding:
      var(--space-3)
      var(--space-4);

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-surface-raised) / 0.8),
          rgb(var(--c-surface-2) / 0.54)
      );

  border-bottom: 1px solid rgb(var(--c-border) / 0.25);

  box-shadow:
      0 5px 14px rgb(var(--c-shadow) / 0.07),
      inset 0 1px 0 rgb(255 255 255 / 0.3);
}

.entry-search-toolbar {
  margin: 0;
  padding: 0;

  background: transparent;
  border: 0;
  box-shadow: none;
}

.entry-search-toolbar__search {
  width: 100%;
}

.entry-status {
  display: flex;
  align-items: center;
  gap: var(--space-2);

  flex: 0 0 auto;

  color: rgb(var(--c-muted));

  font-size: 0.78rem;
  font-weight: 650;
  white-space: nowrap;
}

/* -------------------------------------------------------------------------- */
/* Error                                                                      */
/* -------------------------------------------------------------------------- */

.entry-error {
  display: flex;
  align-items: center;
  gap: var(--space-2);

  min-width: 0;
  padding: var(--space-2) var(--space-3);

  color: rgb(var(--c-danger-strong));

  background: rgb(var(--c-danger) / 0.08);
  border: 1px solid rgb(var(--c-danger) / 0.24);
  border-radius: var(--radius-sm);

  font-size: 0.8rem;
  font-weight: 650;
}

.entry-error > svg {
  width: 1rem;
  height: 1rem;
  flex: 0 0 auto;

  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.entry-error > span {
  flex: 1 1 auto;
  min-width: 0;

  overflow-wrap: anywhere;
}

.entry-error > button {
  width: 1.75rem;
  height: 1.75rem;
  flex: 0 0 auto;

  display: grid;
  place-items: center;

  padding: 0;

  color: currentColor;

  background: transparent;
  border: 0;
  border-radius: var(--radius-xs);

  cursor: pointer;
}

.entry-error > button:hover {
  background: rgb(var(--c-danger) / 0.12);
}

.entry-error > button:focus-visible {
  outline: 2px solid rgb(var(--c-danger) / 0.45);
  outline-offset: 1px;
}

.entry-error > button svg {
  width: 0.9rem;
  height: 0.9rem;

  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
}

/* -------------------------------------------------------------------------- */
/* Pagination                                                                 */
/* -------------------------------------------------------------------------- */

.pagination-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);

  min-width: 0;
  padding: var(--space-2) var(--space-3);

  background: rgb(var(--c-surface-2) / 0.42);
  border: 1px solid rgb(var(--c-border) / 0.22);
  border-radius: var(--radius-sm);
}

.pagination-toolbar__summary {
  flex: 1 1 auto;
  min-width: 0;

  color: rgb(var(--c-muted));

  font-size: 0.78rem;
  white-space: nowrap;
}

.pagination-toolbar__summary strong {
  color: rgb(var(--c-fg-strong));
  font-weight: 750;
}

.pagination-toolbar__controls {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-4);

  flex: 0 0 auto;
}

.page-size-control {
  display: flex;
  align-items: center;
  gap: var(--space-2);

  color: rgb(var(--c-muted));

  font-size: 0.78rem;
  white-space: nowrap;
}

.page-size-control__select {
  min-height: 2rem;
  padding: 0.25rem 1.8rem 0.25rem 0.55rem;

  color: rgb(var(--c-fg));

  background: rgb(var(--c-surface-raised) / 0.72);
  border: 1px solid rgb(var(--c-border) / 0.32);
  border-radius: var(--radius-xs);

  font: inherit;
  cursor: pointer;

  transition:
      background-color var(--duration-fast) var(--ease-standard),
      border-color var(--duration-fast) var(--ease-standard);
}

.page-size-control__select:hover {
  background: rgb(var(--c-surface-hover) / 0.8);
  border-color: rgb(var(--c-accent) / 0.5);
}

.page-size-control__select:focus-visible {
  outline: 2px solid rgb(var(--focus-ring-color) / 0.5);
  outline-offset: 2px;
}

.page-navigation {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.page-navigation__number {
  min-width: 4.5rem;

  color: rgb(var(--c-muted));

  font-size: 0.78rem;
  text-align: center;
  white-space: nowrap;
}

.page-navigation__number strong {
  color: rgb(var(--c-fg-strong));
  font-weight: 750;
}

/* -------------------------------------------------------------------------- */
/* Scrollable viewport                                                        */
/* -------------------------------------------------------------------------- */

.entry-list-editor__viewport {
  min-width: 0;
  min-height: 0;

  padding: var(--space-3);

  overflow-x: hidden;
  overflow-y: auto;

  overscroll-behavior: contain;
  scrollbar-gutter: stable;
  scroll-padding-block: var(--space-3);

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-surface) / 0.2),
          rgb(var(--c-surface-2) / 0.12)
      );

  scrollbar-width: thin;
  scrollbar-color:
      rgb(var(--c-accent) / 0.48)
      transparent;
}

.entry-list-editor__viewport:focus-visible {
  outline: 2px solid rgb(var(--focus-ring-color) / 0.4);
  outline-offset: -2px;
}

.entry-list-editor__viewport::-webkit-scrollbar {
  width: 0.7rem;
  height: 0.7rem;
}

.entry-list-editor__viewport::-webkit-scrollbar-track {
  background: transparent;
}

.entry-list-editor__viewport::-webkit-scrollbar-thumb {
  background: rgb(var(--c-accent) / 0.4);
  border: 2px solid transparent;
  border-radius: var(--radius-round);
  background-clip: padding-box;
}

.entry-list-editor__viewport::-webkit-scrollbar-thumb:hover {
  background: rgb(var(--c-accent) / 0.62);
  border: 2px solid transparent;
  background-clip: padding-box;
}

/* -------------------------------------------------------------------------- */
/* Entries                                                                    */
/* -------------------------------------------------------------------------- */

.entry-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);

  width: 100%;
  min-width: 0;

  margin: 0;
  padding: 0;

  list-style: none;
}

.entry-card {
  width: 100%;
  min-width: 0;
  max-width: 100%;
  box-sizing: border-box;

  padding: var(--space-3);

  /*
   * The card must never clip an expanded EntryEditor. The viewport above
   * provides vertical scrolling for editors taller than the screen.
   */
  overflow: visible;

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-surface-raised) / 0.58),
          rgb(var(--c-surface-2) / 0.32)
      );

  border: 1px solid rgb(var(--c-border) / 0.25);
  border-left: 3px solid rgb(var(--c-accent) / 0.62);
  border-radius: var(--radius-md);

  box-shadow:
      0 5px 16px rgb(var(--c-shadow) / 0.055),
      inset 0 1px 0 rgb(255 255 255 / 0.28);

  scroll-margin-block: var(--space-3);

  transition:
      background-color var(--duration-normal) var(--ease-standard),
      border-color var(--duration-normal) var(--ease-standard),
      box-shadow var(--duration-normal) var(--ease-standard);
}

.entry-card:hover {
  border-color: rgb(var(--c-accent) / 0.38);
  border-left-color: rgb(var(--c-accent));

  box-shadow:
      0 8px 22px rgb(var(--c-shadow) / 0.08),
      inset 0 1px 0 rgb(255 255 255 / 0.34);
}

.entry-card:focus-within {
  border-color: rgb(var(--c-accent) / 0.58);
  border-left-color: rgb(var(--c-accent));

  box-shadow:
      0 0 0 var(--focus-ring-width)
      rgb(var(--focus-ring-color) / 0.12),
      0 8px 22px rgb(var(--c-shadow) / 0.08);
}

.entry-card__editor {
  display: block;

  width: 100%;
  min-width: 0;
  max-width: 100%;

  /*
   * No height or overflow restriction is applied here. An expanded editor
   * may grow to its complete natural height.
   */
}

/* Keep EntryEditor containers within the horizontal viewport. */
.entry-card :deep(.entry-header),
.entry-card :deep(.expandedTop),
.entry-card :deep(.entry-editor),
.entry-card :deep(.entry-content),
.entry-card :deep(.editor-content) {
  width: 100%;
  min-width: 0;
  max-width: 100%;
  box-sizing: border-box;

  overflow: visible;
}

/* Prevent form controls from forcing the card beyond the viewport. */
.entry-card :deep(input),
.entry-card :deep(textarea),
.entry-card :deep(select),
.entry-card :deep(button) {
  max-width: 100%;
  box-sizing: border-box;
}

.entry-card :deep(textarea) {
  width: 100%;
  resize: vertical;
}

.entry-card :deep(pre) {
  max-width: 100%;

  overflow-x: auto;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.entry-card :deep(code) {
  overflow-wrap: anywhere;
}

.entry-card :deep(img),
.entry-card :deep(video),
.entry-card :deep(canvas),
.entry-card :deep(svg) {
  max-width: 100%;
}

/*
 * Large tables remain accessible through local horizontal scrolling rather
 * than widening or clipping the entire editor.
 */
.entry-card :deep(table) {
  max-width: 100%;
}

.entry-card :deep(.table-wrapper),
.entry-card :deep(.table-container) {
  max-width: 100%;
  overflow-x: auto;
}

/* -------------------------------------------------------------------------- */
/* Empty and loading states                                                   */
/* -------------------------------------------------------------------------- */

.entry-list-state {
  min-height: min(18rem, 50dvh);
}

/* -------------------------------------------------------------------------- */
/* Responsive                                                                 */
/* -------------------------------------------------------------------------- */

@media (max-width: 760px) {
  .entry-list-editor {
    --entry-editor-viewport-offset: 0.5rem;
  }

  .entry-list-editor__controls {
    padding:
        var(--space-2)
        var(--space-3);
  }

  .entry-list-editor__header {
    align-items: flex-start;
  }

  .entry-search-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .entry-status {
    align-self: flex-start;
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

  .entry-list-editor__viewport {
    padding: var(--space-2);
  }

  .entry-card {
    padding: var(--space-2);
  }
}

@media (max-width: 500px) {
  .entry-list-editor {
    border-right: 0;
    border-left: 0;
    border-radius: 0;
  }

  .entry-list-editor__header {
    flex-direction: row;
  }

  .entry-list-editor__header .edit-box__description {
    display: none;
  }

  .pagination-toolbar__controls {
    align-items: stretch;
    flex-direction: column;
    gap: var(--space-2);
  }

  .page-size-control,
  .page-navigation {
    justify-content: center;
  }

  .entry-list-editor__viewport {
    padding-inline: var(--space-1);
  }

  .entry-card {
    padding: var(--space-2);
    border-radius: var(--radius-sm);
  }
}

@media (prefers-reduced-motion: reduce) {
  .entry-card,
  .page-size-control__select {
    transition: none;
  }
}
</style>