<script setup lang="ts">

import {onMounted, ref} from "vue";
import List from "@/components/utils/list/List.vue";
import {goToEdit} from "@/app/router";
import {World, WorldData, WorldKey} from "@/domain/entities/space/World";
import {createEntity, fetch_all} from "@/domain/entities/EntityFetch";
import {EntityTypes} from "@/frameworks/entities/EntityTypes";

const worlds = ref<World[]>();
async function onCreate(){
  const input_name = window.prompt('Enter new world name:');
  if (!input_name) return;

  // Create
  const new_world = await createEntity<WorldKey, WorldData, World>(
      null,
      {name:input_name},
      EntityTypes.WORLDS,
      World
  );

  goToEdit(EntityTypes.WORLDS, new_world)
}
async function onEdit(new_world : World) {
  goToEdit(EntityTypes.WORLDS, new_world)
}

onMounted(async () => {
  worlds.value = await fetch_all<WorldKey, WorldData, World>(EntityTypes.WORLDS, World);
})
</script>

<template>
  <List
      v-if="worlds"
      v-model:elements = "worlds as World[]"
      @create = onCreate
      @edit = "(element) => onEdit(element as World)"
  ></List>
</template>

<style scoped>

</style>