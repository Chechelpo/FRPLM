<script setup lang="ts">
import {onMounted, ref} from "vue";
import SimpleList from "@/components/utils/list/SimpleList.vue";
import Editor from "@/components/utils/entity_editors/self-assembling/Editor.vue";
import {Character} from "@/domain/entities/chars/Characters";
import {World} from "@/domain/entities/space/World";
import {CommonFields} from "@/utils/CommonFields";
import {fetch_all} from "@/utils/EntityFetch";
import {ControllerType} from "@/config/ControllerType";

const props = defineModel<{
  character: Character;
}>();

const worlds = ref<World[]>([])
const selectedWorld = ref<World | null>(null);
const locationsOfWorld = ref<Location[]>([])
const selectedLocation = ref<Location | null>(null)
const startingInfo = ref<World | null>(null);
const creatingNew = ref<boolean>(false);

async function onSelectWorld(world: World) {
  console.trace(`Selecting world: ${world.getCommon(CommonFields.NAME)}`);
  selectedWorld.value = world;
  /*
  locationsOfWorld.value = await getLocations(world.id);
  //Now we need to check if there's a starting info
  const response = await fetch(`/api/starting/${props.character.id}/${world.id}`,
      {
        method: "GET",
      })

  if (response.status === 404) {
    creatingNew.value = true;
    return;
  }
  if (!response.ok) throw new Error(`Error when fetching starting location`);

  //There's a starting info then:
  startingInfo.value = await response.json();

  //fetch matching location:
  if (startingInfo.value === null) throw new Error("Unexpected error while fetching starting location");
  for (let i = 0; i < locationsOfWorld.value.length; i++) {
    const location = locationsOfWorld.value[i];

    if (location.id === startingInfo.value.locationID) {
      selectedLocation.value = location;
      break;
    }
  }*/
}

async function onSelectLocation(location: Location) {
  /*
  console.trace(`Setting location: ${location.name}`);
  if (selectedWorld.value == null) throw new Error("Invalid state")
  selectedLocation.value = location;

  if (creatingNew.value) {
    const response = await fetch(`/api/starting/${props.character.id}/${selectedWorld.value.id}?locationID=${encodeURIComponent(selectedLocation.value.id)}`,
        {method: "POST",}
    );
    if (!response.ok) throw new Error("Error creating starting location");

    startingInfo.value = await response.json();
    creatingNew.value = false;
    return
  } else await saveField("locationID", selectedLocation.value.id);
  */
}


onMounted(async () => {
  worlds.value = await fetch_all(ControllerType.WORLDS, World);
});
</script>

<template>
<div class = "flex flex-col" >
  <div class = "w-fit h-fit text-2xl"> Select world </div>
  <SimpleList
    :elements = "worlds"
    @select = "onSelectWorld"
  >
  </SimpleList>
  <div class = "w-fit h-fit text-2xl"
    v-if = "selectedWorld"
  >  Select starting location </div>
  <SimpleList
      v-if = "selectedWorld"
      :elements = "locationsOfWorld"
      :pre-selected = "selectedLocation"
      @select = "onSelectLocation"
  ></SimpleList>
  <Editor
      v-if = "startingInfo"
      :owner = "startingInfo"
      :no-edit = "['charID', 'locationID']"
  >
  </Editor>
</div>
</template>

<style scoped>

</style>