<script setup lang="ts">
import { computed } from "vue";

const props = withDefaults(
    defineProps<{
      search?: string;
      placeholder?: string;
      disabled?: boolean;
      ariaLabel?: string;
    }>(),
    {
      search: "",
      placeholder: "Search...",
      disabled: false,
      ariaLabel: "Search",
    },
);

const emit = defineEmits<{
  (event: "update:search", value: string): void;
}>();

const searchModel = computed({
  get: () => props.search,
  set: (value: string) => emit("update:search", value),
});

function clearSearch(): void {
  if (!props.disabled) {
    searchModel.value = "";
  }
}
</script>

<template>
  <div
      class="search-bar"
      :class="{ 'search-bar--disabled': disabled }"
      role="search"
  >
    <svg
        class="search-bar__icon"
        viewBox="0 0 24 24"
        aria-hidden="true"
    >
      <path
          d="m21 21-4.35-4.35m2.35-5.65a8 8 0 1 1-16 0 8 8 0 0 1 16 0Z"
      />
    </svg>

    <input
        v-model="searchModel"
        class="search-bar__input"
        type="search"
        :placeholder="placeholder"
        :aria-label="ariaLabel"
        :disabled="disabled"
        autocomplete="off"
        spellcheck="false"
        @keydown.esc="clearSearch"
    />

    <button
        v-if="searchModel.length > 0"
        class="search-bar__clear"
        type="button"
        aria-label="Clear search"
        :disabled="disabled"
        @mousedown.prevent
        @click="clearSearch"
    >
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M7 7l10 10M17 7 7 17" />
      </svg>
    </button>
  </div>
</template>

<style scoped>
.search-bar {
  width: 100%;
  min-height: 46px;
  box-sizing: border-box;

  display: flex;
  align-items: center;
  gap: 10px;

  padding: 0 12px;

  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(175, 130, 24, 0.45);
  border-radius: 14px;

  box-shadow:
      0 4px 12px rgba(81, 54, 23, 0.08),
      inset 0 1px 0 rgba(255, 255, 255, 0.65);

  backdrop-filter: blur(10px);
  transition:
      border-color 160ms ease,
      box-shadow 160ms ease,
      background-color 160ms ease,
      transform 160ms ease;
}

.search-bar:hover:not(.search-bar--disabled) {
  background: rgba(255, 255, 255, 0.82);
  border-color: var(--primary);
}

.search-bar:focus-within {
  background: rgba(255, 255, 255, 0.92);
  border-color: var(--primary-accent);

  box-shadow:
      0 0 0 4px rgba(255, 198, 0, 0.2),
      0 8px 24px rgba(81, 54, 23, 0.12);

  transform: translateY(-1px);
}

.search-bar__icon {
  width: 20px;
  height: 20px;
  flex: 0 0 auto;

  fill: none;
  stroke: var(--muted-text);
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;

  transition: stroke 160ms ease;
}

.search-bar:focus-within .search-bar__icon {
  stroke: var(--primary);
}

.search-bar__input {
  width: 100%;
  min-width: 0;
  height: 44px;

  padding: 0;

  font-family: var(--primary-text);
  font-size: 0.95rem;
  font-weight: 500;
  color: #392b1e;

  background: transparent;
  border: 0;
  outline: 0;
}

.search-bar__input::placeholder {
  color: var(--muted-text);
  opacity: 0.75;
}

.search-bar__input::-webkit-search-cancel-button {
  display: none;
}

.search-bar__clear {
  width: 30px;
  height: 30px;
  flex: 0 0 auto;

  display: grid;
  place-items: center;

  padding: 0;

  color: var(--muted-text);
  background: transparent;
  border: 0;
  border-radius: 50%;
  cursor: pointer;

  transition:
      color 140ms ease,
      background-color 140ms ease,
      transform 140ms ease;
}

.search-bar__clear:hover {
  color: var(--primary);
  background: rgba(175, 130, 24, 0.12);
}

.search-bar__clear:active {
  transform: scale(0.9);
}

.search-bar__clear:focus-visible {
  outline: 2px solid var(--primary-accent);
  outline-offset: 2px;
}

.search-bar__clear svg {
  width: 17px;
  height: 17px;

  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
}

.search-bar--disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.search-bar--disabled .search-bar__input,
.search-bar--disabled .search-bar__clear {
  cursor: not-allowed;
}

@media (prefers-reduced-motion: reduce) {
  .search-bar,
  .search-bar__icon,
  .search-bar__clear {
    transition: none;
  }
}
</style>