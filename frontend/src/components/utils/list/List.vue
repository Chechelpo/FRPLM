<script setup lang="ts" generic="
Key extends KeyRecord,
Data extends DataRecord,
Ent extends EntityABS<Key,Data>
">
import {computed, ref} from "vue";

import SearchBar from "@/components/utils/SearchBar.vue";
import Card from "@/components/utils/list/Card.vue";
import {EntityABS} from "@/frameworks/entities/EntityABS";
import {CommonFields} from "@/utils/CommonFields";
import {DataRecord, KeyRecord} from "@/types/DTOs";

const elements = defineModel<Ent[]>("elements", {required: true});
const hasDescription: boolean = elements.value.length > 0 ? elements.value[0].hasAttribute(CommonFields.DESCRIPTION) : false;

/**
 * Search term from SearchBar
 */
const searchTerm = ref("");

function normalise(s: string): string {
  return s
      .trim()
      .toLowerCase()
      .normalize("NFKD")
      .replace(/\p{Diacritic}/gu, "");
}

const filtered = computed<Ent[]>(() => {
  const q: string = normalise(searchTerm.value);
  if (q.length === 0) return elements.value;
  return elements.value.filter((c) =>
      normalise(c.getCommon(CommonFields.NAME) as string).includes(q)
  );

});

const emit = defineEmits({
  create: () => true,
  open: (element: Ent) => true,
  edit: (element: Ent) => true,
  remove: (element: Ent) => true,
});

function onEdit(element: Ent): Ent {
  emit("edit", element);
  return element;
}

function onCreate() {
  emit("create");
}

function onRemove(element: Ent) {
  emit("remove", element);
}
</script>

<template>
  <div>
    <div class="main-container">
      <!-- 1) Create (single-use here) -->
      <button
          type="button"
          class = "wide-button"
          @click="onCreate"
      >
        <span class="text-lg leading-none">+</span>
        <span>New</span>
      </button>

      <!-- 2) Search  -->
      <SearchBar @update:search="searchTerm = $event"/>

      <!-- 3) List -->
      <div class="flex-container">
        <Card
            class=""
            v-for="element in filtered"
            :has-description="hasDescription"
            :character="element"
            @open="emit('open', $event)"
            @edit="onEdit"
            @remove="onRemove"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>

.main-container {
  height: 100%;
  display: flex;
  gap: 10px;
  flex-direction: column;
}

.flex-container {
  height: 100%;

  display: flex;
  flex-direction: column;

  gap: 6px;
}
</style>
