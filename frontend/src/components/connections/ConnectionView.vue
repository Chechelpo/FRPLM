<script setup lang="ts">
import { ref, onMounted } from "vue";
import type { LLM_connection } from "@/types/LLM";
import {
  fetchAllConnections,
  fetchActiveConnection,
  fetchConnectionTypes,
  setActiveConnection,
  setConnectionType,
  setConnectionHost,
  createConnection,
} from "@/services/Connection";

const connections = ref<LLM_connection[]>([]);
const active = ref<LLM_connection | null>(null);
const types = ref<string[]>([]);

const hostInput = ref("");

async function reload() {
  connections.value = await fetchAllConnections();
  active.value = await fetchActiveConnection();
  console.log(active.value);
  hostInput.value = active.value?.url ?? "";
}

async function onSelect(id: number) {
  await setActiveConnection(id);
  await reload();
}

async function onTypeChange(type: string) {
  await setConnectionType(type);
  await reload();
}

async function onHostSave() {
  await setConnectionHost(hostInput.value);
  await reload();
}

async function onCreate() {
  const name = prompt("New connection name:");
  if (!name) return;
  await createConnection(name);
  await reload();
}

onMounted(async () => {
  types.value = await fetchConnectionTypes();
  connections.value = await fetchAllConnections();
  if (connections.value.length > 0) console.log("Connection values " + connections.value)
  await reload();
});
</script>


<template>
  <div class="h-full w-full grid grid-cols-[1fr_8fr] gap-4 p-4">

    <!-- LEFT: Profile create + profile list -->
    <div class="border border-stone-800 rounded p-2 space-y-2">
      <button
          class="w-full bg-stone-800 p-2 rounded"
          @click="onCreate"
      >
        + New connection
      </button>

      <div
          v-for="c in connections.values()"
          :key="c.id"
          class="p-2 w-full h-full rounded cursor-pointer"
          :class="c.id === active?.id ? 'bg-amber-700/30' : 'hover:bg-stone-800'"
          @click="onSelect(c.id)"
      >
        {{ c.name }}
      </div>
    </div>
    <!-- RIGHT: AvatarEditor -->
    <div
        v-if="active"
        class="border border-stone-800 rounded p-4 space-y-4">
      <h2 class="text-xl font-semibold">{{ active.name }}</h2>

      <!-- Type -->
      <div>
        <label class="block text-sm text-stone-400">Type</label>
        <select
            class="bg-stone-800 p-2 rounded w-full"
            :value="active.type"
            @change="onTypeChange(($event.target as HTMLSelectElement).value)"
        >
          <option v-for="t in types" :key="t" :value="t">
            {{ t }}
          </option>
        </select>
      </div>

      <!-- Host -->
      <div>
        <label class="block text-sm text-stone-400">Host</label>
        <input
            class="bg-stone-800 p-2 rounded w-full"
            v-model="hostInput"
        />
        <button
            class="mt-2 px-4 py-2 bg-amber-700 rounded"
            @click="onHostSave"
        >
          Save
        </button>
      </div>
    </div>

  </div>
</template>


<style scoped>

</style>