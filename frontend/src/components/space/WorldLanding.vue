<script setup lang="ts">

import List from "@/components/utils/list/List.vue";
import {World, WorldData, WorldKey} from "@/domain/entities/World";
import {createEntity, fetch_all} from "@/domain/entities/EntityFetch";
import {EntityTypes} from "@/frameworks/entities/EntityTypes";
import {computedAsync} from "@vueuse/core";
import {ref, shallowRef} from "vue";
import WorldEdit from "@/components/space/WorldEdit.vue";

const worlds = computedAsync<World[]>(
    async () => await fetch_all<WorldKey, WorldData, World>(EntityTypes.WORLDS, World)
);
const isEditing = ref<boolean>(false);
const editingWorld = shallowRef<World | null>(null);

async function onCreate(){
  const input_name = window.prompt('Enter new world name:');
  if (!input_name) return;

  // Create
  const new_world = await createEntity<WorldKey, WorldData, World>(
      null,
      {
        name:input_name
      },
      EntityTypes.WORLDS,
      World
  );

  onEdit(new_world)
}

function onEdit(new_world : World) {
  isEditing.value = true;
  editingWorld.value = new_world;
}
</script>

<template>
  <div>
    <List
        v-if="worlds && !isEditing"
        v-model:elements = "worlds as World[]"
        @create = onCreate
        @edit = "(element) => onEdit(element as World)"
    ></List>
    <WorldEdit
      v-if = "isEditing && editingWorld != null"
      v-model="editingWorld!"
      @stop-editing="isEditing = false; editingWorld = null"
    />
  </div>
</template>

<style scoped>

</style>