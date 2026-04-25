<script setup lang="ts">
import {onMounted, ref} from "vue";
import {Character_DTO, World_DTO} from "@/types/DTOs";
import {getWorlds} from "@/services/world";
import SimpleList from "@/components/utils/list/SimpleList.vue";

const props = defineProps<{}>();

const worlds = ref<World_DTO[]>([]);
const selectedWorld = ref<World_DTO | null>(null);
const characters = ref<Character_DTO[]>([]);
const selectedCharacter = ref<Character_DTO | null>(null);

async function onSelectWorld(world: World_DTO){
  selectedWorld.value = world;

}
onMounted(async() => {
  worlds.value = await getWorlds();
});
</script>

<template>
  <!-- Backdrop -->
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/70">

    <!-- Modal container -->
    <div
        class="flex flex-col w-[420px] rounded-lg border-2
             bg-stone-900/90 border-stone-800
             shadow-lg shadow-stone-950/60"
    >

      <!-- Header -->
      <div
          class="px-4 py-3 border-b border-stone-800
               text-xl italic font-sans text-stone-200"
      >
        Start New Session
      </div>

      <!-- Body -->
      <div class="flex flex-col gap-4 px-4 py-4">

        <!-- World selector -->
        <SimpleList
            :elements = "worlds"
            @select = "onSelectWorld"
        ></SimpleList>

        <!-- Character selector -->
        <div class="flex flex-col gap-1">
          <label class="text-sm text-stone-400">Character</label>
          <select
              v-model="selectedCharacterId"
              :disabled="!selectedWorldId"
              class="px-2 py-1 rounded
                   bg-stone-800/80 border border-stone-700
                   text-stone-200
                   disabled:opacity-40
                   focus:outline-none focus:border-amber-700/60"
          >
            <option :value="null">Select a character</option>
            <option
                v-for="c in characters"
                :key="c.id"
                :value="c.id"
            >
              {{ c.name }}
            </option>
          </select>
        </div>

      </div>

      <!-- Footer actions -->
      <div
          class="flex justify-end gap-2 px-4 py-3
               border-t border-stone-800"
      >
        <!-- Cancel -->
        <button
            @click="emit('close')"
            class="px-3 py-1 rounded
                 bg-stone-800/60 border border-stone-700/70
                 text-stone-300
                 transition-all duration-200
                 hover:bg-stone-700/60"
        >
          Cancel
        </button>

        <!-- Start -->
        <button
            @click="confirm"
            :disabled="!selectedWorldId || !selectedCharacterId"
            class="px-3 py-1 rounded
                 bg-amber-800/30 border border-amber-700/60
                 text-amber-200
                 transition-all duration-200
                 disabled:opacity-40
                 hover:bg-amber-700/40 hover:shadow-lg hover:shadow-amber-900/40"
        >
          Start
        </button>
      </div>

    </div>
  </div>
</template>
<style scoped>

</style>