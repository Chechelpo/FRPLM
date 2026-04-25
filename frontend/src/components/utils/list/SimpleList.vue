<script setup lang="ts" generic = "Key extends KeyRecord, Data extends DataRecord, T extends EntityABS<Key,Data>">
import {computed, onMounted, onUnmounted, ref, watch} from "vue";
import {DataRecord, EntityABS, KeyRecord} from "@/frameworks/entities/EntityABS";
import {CommonFields} from "@/utils/CommonFields";

/* ----------------------------- Props & Emits ----------------------------- */
const props = defineProps<{
  elements: T[];
  placeholder?: string;
  preSelected? : T | null;
}>();

const emit = defineEmits<{
  (e: "select", element: T): void;
}>();

/* ----------------------------- State ------------------------------------- */
const open = ref(false);
const searchTerm = ref("");
const selected = ref<T | null>(props.preSelected ?? null);
const root = ref<HTMLElement | null>(null);

watch(
    () => props.preSelected,
    (value) => {
      if (value) {
        selected.value = value;
      }
    },
    { immediate: true }
);

/* ----------------------------- Utilities --------------------------------- */
function normalise(s: string): string {
  return s
      .trim()
      .toLowerCase()
      .normalize("NFKD")
      .replace(/\p{Diacritic}/gu, "");
}

/* ----------------------------- Computed ---------------------------------- */
const filtered = computed(() => {
  const q = normalise(searchTerm.value);
  if (q.length === 0) return props.elements;
  return props.elements.filter(e =>
      normalise(e.getCommon(CommonFields.NAME) as string).includes(q)
  );
});

/* ----------------------------- Behaviour --------------------------------- */
function toggle(): void {
  open.value = !open.value;
  if (!open.value) searchTerm.value = "";
}

function select(element: T): void {
  selected.value = element;
  emit("select", element);
  open.value = false;
  searchTerm.value = "";
}

/* ----------------------------- Outside Click ----------------------------- */
function onClickOutside(e: MouseEvent): void {
  if (!root.value) return;
  if (!root.value.contains(e.target as Node)) {
    open.value = false;
    searchTerm.value = "";
  }
}

onMounted(() => {
  document.addEventListener("click", onClickOutside);
});

onUnmounted(() => {
  document.removeEventListener("click", onClickOutside);
});
</script>

<template>
  <div ref="root" class="relative w-full">

    <!-- Collapsed Select -->
    <button
        type="button"
        class="w-full h-11 px-3 rounded-lg border border-stone-700
             bg-stone-900 text-left text-sm text-stone-200
             flex items-center justify-between
             hover:border-amber-600/50 transition"
        @click="toggle"
    >
      <span class="truncate">
        {{ selected?.name ?? (placeholder ?? "Select…") }}
      </span>
      <span class="text-xs opacity-60">▾</span>
    </button>

    <!-- Dropdown -->
    <div
        v-if="open"
        class="absolute z-50 mt-2 w-full rounded-lg
             border border-stone-800 bg-stone-900 shadow-xl"
    >
      <!-- Search -->
      <div class="p-2 border-b border-stone-800">
        <input
            v-model="searchTerm"
            type="text"
            class="w-full h-9 px-2 rounded-md
                 bg-stone-800 text-sm text-stone-200
                 border border-stone-700
                 focus:outline-none focus:border-amber-600/60"
            placeholder="Search…"
        />
      </div>

      <!-- List -->
      <ul class="max-h-56 overflow-y-auto">
        <li
            v-for="el in filtered"
            :key="el.getCommon(CommonFields.NAME) as string"
            class="px-3 py-2 text-sm text-stone-200 cursor-pointer
                 hover:bg-amber-700/20 hover:text-amber-200
                 transition"
            @click="select(el)"
        >
          {{ el.getCommon(CommonFields.NAME) }}
        </li>

        <li
            v-if="filtered.length === 0"
            class="px-3 py-2 text-sm text-stone-500 italic"
        >
          No results
        </li>
      </ul>
    </div>

  </div>
</template>
