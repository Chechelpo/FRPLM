<script setup lang="ts">
import {onMounted, ref} from "vue";
import AvatarEditor from "@/components/utils/entity_editors/self-assembling/AvatarEditor.vue";
import List from "@/components/utils/list/List.vue";
import SplitPanel from "@/components/utils/panels/SplitPanel.vue";
import LocationEdgesEditor from "@/components/world/LocationEdgesEditor.vue";
import {World, WorldData, WorldKey} from "@/domain/entities/space/World";
import {Location, LocationData, LocationKey} from "@/domain/entities/space/Location";
import {create_Entity, fetchOne} from "@/utils/EntityFetch";
import {EntityTypes} from "@/domain/entities/EntityTypes";

const props = defineProps<{
  id:number
}>();

const world = ref<World>();
const locations = ref<Location[]>([]);
const locationToEdit = ref<Location | null>(null);

async function onCreate() { //Create location of a world
  if (world.value == null) return;
  const name = window.prompt("Enter new character name:");
  if (!name) return;

  locationToEdit.value = await create_Entity<LocationKey,LocationData,Location>(
      {worldID:props.value.world.get('id')},
      {name: name},
      EntityTypes.LOCATIONS,
      Location
  );
  locations.value.push(locationToEdit.value);
}

async function onEdit(location: Location) {
  if (locationToEdit.value && locationToEdit.value.equals(location)) locationToEdit.value = null;
  else locationToEdit.value = location as Location;
}

onMounted(async () => {
  if (!props.id) console.error("No ID found for editor")
  world.value = await fetchOne<WorldKey, WorldData, World>({id:props.id}, EntityTypes.WORLDS, World)
  if (!world.value) console.error("No world")
  locations.value = await Location.getLocationsOfWorld(world.value.key)
})
</script>

<template>
  <div class="flex flex-col flex-1 min-h-0">

    <!-- World editor (natural height, scrolls away) -->
    <AvatarEditor
        v-model:entity="world!"
    />

    <!-- Locations editor (full-screen section) -->
    <div class="flex-1 min-h-0 flex">
      <SplitPanel storage-key="WorldEdit" class = "flex-1 min-h-0">
        <template #left>
          <List
              :elements="locations!"
              @create="onCreate"
              @edit="(element) => onEdit(element as Location)"
          />
        </template>

        <template #right>
          <SplitPanel storage-key="WorldEdit:Inner">
            <template #left>
              <AvatarEditor
                  v-if="locationToEdit"
                  :key = "locationToEdit.get('id')"
                  :owner="locationToEdit"
                  :no-edit="[]"
              />
            </template>
            <template #right>
              <LocationEdgesEditor
                  v-if = "locationToEdit"
                  :key = "locationToEdit.get('id')"
                  :parent-location="locationToEdit"
                  :locations="locations">
              </LocationEdgesEditor>
            </template>
          </SplitPanel>
        </template>
      </SplitPanel>
    </div>
  </div>
</template>



<style scoped>

</style>