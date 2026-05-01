## Component: `AutoCompleteBox.vue`

A reusable Vue 3 component that provides a multi‑tag autocomplete input. It is designed to be completely independent – it receives a list of suggestion strings and manages the selected tags internally, keeping the parent in sync via `v-model`.

### Props

| Prop          | Type       | Default                           | Description                                                                 |
|---------------|------------|-----------------------------------|-----------------------------------------------------------------------------|
| `suggestions`  | `string[]` | **required**                      | All possible suggestion strings shown during autocomplete.                  |
| `modelValue`   | `string[]` | `[]`                              | Array of currently selected tags; used for two‑way binding with `v-model`.  |
| `placeholder`  | `string`   | `'Type and press Tab to add…'`   | Placeholder text for the input field.                                       |

### Emits

| Event              | Payload    | Description                                                                                     |
|--------------------|------------|-------------------------------------------------------------------------------------------------|
| `update:modelValue` | `string[]` | Emitted whenever the selected‑tags array changes (enables `v-model`).                           |
| `add`              | `string`   | Emitted when a new tag is added (via tab/enter/autocomplete). The payload is the tag text.      |
| `remove`           | `string`   | Emitted when a tag is deleted (via the × button or backspace on empty input).                   |

### Behaviour

- The user types into the visible input.
- The **first suggestion** (case‑insensitive prefix match) that is not already selected appears in a tooltip **above** the input box.
- Pressing **Tab** or **Enter** inserts that suggestion as a new tag, clears the input, and keeps the focus for further typing.
- **Backspace** on an empty input removes the last tag.
- Each tag is shown as a rounded chip with a small **×** button to remove it.
- All changes are reflected in the `v-model` value and emitted individually as `add`/`remove` events.

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'

interface Props {
  suggestions: string[]
  modelValue?: string[]
  placeholder?: string
  allowCustom?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: () => [],
  placeholder: 'Type and press Tab to add…',
  allowCustom: false,
})

const emit = defineEmits<{
  (e: 'update:modelValue', tags: string[]): void
  (e: 'add', newValue: string): void
  (e: 'remove', stringToRemove: string): void
}>()

const internalTags = ref<string[]>([...props.modelValue])
const inputText = ref<string>('')
const isFocused = ref<boolean>(false)
const inputRef = ref<HTMLInputElement | null>(null)

watch(
    () => props.modelValue,
    (newVal) => {
      internalTags.value = [...(newVal ?? [])]
    }
)

const showSuggestion = computed<string | null>(() => {
  const query = inputText.value.trim().toLowerCase()
  if (!query) return null

  const match = props.suggestions.find((s) =>
      s.toLowerCase().startsWith(query)
  )

  if (!match || internalTags.value.includes(match)) return null

  return match
})

function setTags(tags: string[]) {
  internalTags.value = tags
  emit('update:modelValue', tags)
}

function addTag(tag: string) {
  if (!tag || internalTags.value.includes(tag)) return

  const updated = [...internalTags.value, tag]

  setTags(updated)
  emit('add', tag)
}

function removeTag(index: number) {
  const removed = internalTags.value[index]
  if (removed === undefined) return

  const updated = [...internalTags.value]
  updated.splice(index, 1)

  setTags(updated)
  emit('remove', removed)
}

function applySuggestion() {
  const text = inputText.value.trim()
  if (!text) return

  if (showSuggestion.value) {
    addTag(showSuggestion.value)
  } else if (props.allowCustom) {
    addTag(text)
  }

  inputText.value = ''
  nextTick(() => inputRef.value?.focus())
}

function handleBackspace(e: KeyboardEvent) {
  if (inputText.value === '' && internalTags.value.length > 0) {
    e.preventDefault()
    removeTag(internalTags.value.length - 1)
  }
}
</script>

<template>
  <div class="tag-autocomplete" :class="{ 'is-focused': isFocused }">
    <!-- Suggestion popup (shows above the input) -->
    <transition name="suggest-fade">
      <div v-if="showSuggestion" class="suggestion-popup">
        {{ showSuggestion }}
      </div>
    </transition>

    <div class="input-wrapper">
      <!-- Existing tags as chips -->
      <span
          v-for="(tag, index) in internalTags"
          :key="tag"
          class="tag-chip"
      >
        {{ tag }}
        <button
            type="button"
            class="tag-remove"
            @click="removeTag(index)"
            aria-label="Remove tag"
        >
          &times;
        </button>
      </span>

      <!-- Actual text input -->
      <input
          ref="inputRef"
          v-model="inputText"
          class="tag-input"
          :placeholder="placeholder"
          @keydown.tab.prevent="applySuggestion"
          @keydown.enter.prevent="applySuggestion"
          @keydown.backspace="handleBackspace"
          @focus="isFocused = true"
          @blur="isFocused = false"
      />
    </div>
  </div>
</template>

<style scoped>
/* ---------- container ---------- */
.tag-autocomplete {
  position: relative;
  font-family: system-ui, sans-serif;
}

/* ---------- suggestion popup ---------- */
.suggestion-popup {
  position: absolute;
  bottom: 100%;
  left: 0;
  margin-bottom: 4px;
  padding: 2px 8px;
  background: #f0f0f0;
  border-radius: 4px;
  font-size: 0.85em;
  color: #444;
  white-space: nowrap;
  box-shadow: 0 1px 4px rgba(0,0,0,0.1);
  transition: opacity 0.15s ease;
}
.suggest-fade-enter-active, .suggest-fade-leave-active {
  transition: opacity 0.15s ease;
}
.suggest-fade-enter-from, .suggest-fade-leave-to {
  opacity: 0;
}

/* ---------- input area (flex wrap) ---------- */
.input-wrapper {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border: 1px solid #ccc;
  border-radius: 6px;
  background: #fff;
  transition: border-color 0.2s;
}
.is-focused .input-wrapper {
  border-color: #4a90d9;
}

/* ---------- tag chips ---------- */
.tag-chip {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 2px 6px;
  background: #e6edf6;
  border-radius: 12px;
  font-size: 0.9em;
  line-height: 1.4;
}
.tag-remove {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 1.1em;
  color: #666;
  padding: 0 2px;
  line-height: 1;
}
.tag-remove:hover {
  color: #c00;
}

/* ---------- the actual <input> ---------- */
.tag-input {
  flex: 1 1 120px;
  min-width: 80px;
  border: none;
  outline: none;
  font-size: 0.95em;
  padding: 2px 0;
  background: transparent;
}
.tag-input::placeholder {
  color: #aaa;
}
</style>