<script setup lang="ts">
import {ref, watch} from "vue";

const search = ref("");
const props = defineProps<
    {
      placeholder?: string;
    }
>();

const placeholder = ref<string>(props.placeholder ? props.placeholder : "");

const emit = defineEmits<{
  (e: "update:search", value: string): void;
}>();

watch(search, (value) => {
  emit("update:search", value);
});

function clearSearch() {
  search.value = "";
}
</script>

<template>
  <div>
    <div class="searchBar-Container">
      <div class="card_icon">
        <img src="/icons/search.png" alt="search"/>
      </div>

      <input
          v-model="search"
          type="text"
          style="flex-grow:3"
          :placeholder="placeholder"
      />

      <button
          type="button"
          @click="clearSearch"
          :disabled="search.length === 0"
          class=" disabled:opacity-40 disabled:hover:bg-stone-800/60 disabled:hover:border-stone-700/70"
          aria-label="Clear search"
      >
        ×
      </button>
    </div>
  </div>
</template>

<style scoped>
.searchBar-Container {
  width: 100%;

  display: flex;
  flex-direction: row;
  justify-content: space-evenly;
}

.card_icon img {
  width: 20px;
  height: 20px;
  object-fit: contain;
}
</style>
