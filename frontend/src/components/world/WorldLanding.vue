<script setup lang="ts">

import {onMounted, ref} from "vue";
import List from "@/components/utils/list/List.vue";
import {goToEdit} from "@/router";
import {World, WorldData, WorldKey} from "@/entities/space/World";
import {create_Entity, fetch_all} from "@/utils/EntityFetch";
import {ControllerType} from "@/config/ControllerType";

const worlds = ref<World[]>();
async function onCreate(){
  const input_name = window.prompt('Enter new world name:');
  if (!input_name) return;

  // Create
  const new_world = await create_Entity<WorldKey, WorldData, World>(
      null,
      {name:input_name},
      ControllerType.WORLDS,
      World
  );

  goToEdit(ControllerType.WORLDS, new_world)
}
async function onEdit(new_world : World) {
  goToEdit(ControllerType.WORLDS, new_world)
}

onMounted(async () => {
  worlds.value = await fetch_all<WorldKey, WorldData, World>(ControllerType.WORLDS, World);
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