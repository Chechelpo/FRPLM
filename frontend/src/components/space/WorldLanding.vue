<script setup lang="ts">

import {World, WorldData, WorldKey} from "@/domain/World";
import {EntityTypes} from "@/domain/EntityTypes";
import {computed, onMounted, ref, shallowRef} from "vue";
import WorldEdit from "@/components/space/WorldEdit.vue";
import {createEntity, fetch_all} from "@/frameworks/ABSEntity";
import SingleEnumInput from "@/components/utils/primitiveEditors/SingleEnumInput.vue";

const worlds = ref<World[]>([]);
const worldNames = computed<string[]>(() => worlds.value.map(world => world.get('name')))

const isEditing = ref<boolean>(false);
const editingWorld = shallowRef<World | null>(null);

async function onCreate() {
  const input_name = window.prompt('Enter new world name:');
  if (!input_name) return;

  // Create
  const new_world = await createEntity<WorldKey, WorldData, World>(
      null,
      {
        name: input_name
      },
      EntityTypes.WORLDS,
      World
  );

  onEdit(new_world)
}

function onEdit(new_world: World) {
  isEditing.value = true;
  editingWorld.value = new_world;
}

onMounted(async () => {
  worlds.value = await fetch_all<WorldKey, WorldData, World>(EntityTypes.WORLDS, World);
})
</script>

<template>
  <div>
    <button
        v-if="!isEditing"
        type="button"
        @click="onCreate"
    >
      New
    </button>
    <SingleEnumInput
        v-if="!isEditing"
        :value="null"
        :possible_values="worldNames"
        @edit="value => onEdit(worlds.find(i => i.get('name') == value)! as World)"
    />
    <WorldEdit
        v-if="isEditing && editingWorld != null"
        v-model="editingWorld!"
        @stop-editing="isEditing = false; editingWorld = null"
    />
  </div>
</template>

<style scoped>

</style>