<script setup lang="ts">
import {computed, onMounted, ref, watch} from "vue";
import {Character, StartingLocation, StartingLocationData, StartingLocationKeys} from "@/domain/Characters";
import { Location, World, WorldData, WorldKey } from "@/domain/World";
import { EntityTypes } from "@/domain/EntityTypes";
import SingleEnumInput from "@/components/utils/primitiveEditors/SingleEnumInput.vue";
import List from "@/components/utils/list/List.vue";
import SplitPanel from "@/components/utils/panels/SplitPanel.vue";
import FieldEditorWrapper from "@/components/utils/FieldEditorWrapper.vue";
import EnumPrompt from "@/components/utils/prompts/EnumPrompt.vue";
import LongTextBox from "@/components/utils/primitiveEditors/LongTextBox.vue";
import NumberInput from "@/components/utils/primitiveEditors/NumberInput.vue";
import {deleteEntity, fetch_all} from "@/core/ABSEntity";

const model = defineModel<Character>({ required: true });

const startingLocations = ref<Location[]>([]);

const worlds = ref<World[]>();
const worldSelected = ref<World | null>(null);
const worldsByName = ref<Map<string, World>>(new Map());
const worldsNames = ref<string[]>([]);

const filteredLocations = ref<Location[]>([]);
const selectedLocation = ref<Location | null>(null);

const locationOptionsOpen = ref(false);

const locationsOfWorld = ref<Location[] | null>(null);
const allLocationsNames = ref<string[]>([]);
async function onSelectWorld(name: string) {
  selectedLocation.value = null;
  locationOptionsOpen.value = false;

  const world = worldsByName.value.get(name);

  if (!world) {
    filteredLocations.value = [];
    worldSelected.value = null;
    return;
  }

  worldSelected.value = world;
  locationsOfWorld.value = await world.getLocations();
  allLocationsNames.value = locationsOfWorld.value.map(location => location.get("name"));

  const worldID = world.get("id");

  filteredLocations.value = startingLocations.value.filter(
      location => location.get("worldID") === worldID
  );
}


watch(
    model.value,
    () => load()
)
onMounted(() => {
  load();
});

async function load() {
  const fetchedWorlds = await fetch_all<WorldKey, WorldData, World>(
      EntityTypes.WORLDS,
      World
  );

  worlds.value = fetchedWorlds;

  const index = new Map<string, World>();
  fetchedWorlds.forEach(world => index.set(world.get("name"), world));

  worldsByName.value = index;
  worldsNames.value = fetchedWorlds.map(world => world.get("name"));

  startingLocations.value = await model.value.getStartingLocations();
}

function onCreateLink() {
  locationOptionsOpen.value = true;
}

function onCloseLocationPrompt() {
  locationOptionsOpen.value = false;
}

async function onPickLocationByName(name: string) {
  const location = locationsOfWorld.value!.find(
      location => location.get("name") == name
  ) as Location;
  console.log(`We're adding ${name}`)
  if (!location) {
    console.error("Could not find location");
    return;
  }

  await model.value.markStartingAt(location);
  filteredLocations.value.push(location);
  selectedLocation.value = location;
  await onSelectStartingLocation(location);
  locationOptionsOpen.value = false;
}

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`
// Starting location attributes
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`
const selectedStartingLocation = ref<StartingLocation | null>(null);
const startingLocationName = ref<string>("");
async function onSelectStartingLocation(loc: Location) {
  selectedLocation.value = loc;
  selectedStartingLocation.value = await model.value.getStartingLocation(loc);
}
async function onDeleteStartingLocation(loc: Location) {
  await deleteEntity<StartingLocationKeys>(
      {
        worldID: loc.get("worldID"),
        locationID: loc.get('id')!,
        characterID: model.value.get('id')
      },
      EntityTypes.STARTING_LOCATIONS
  );
  selectedLocation.value = null;
  selectedStartingLocation.value = null;
  filteredLocations.value = filteredLocations.value.filter(other => !other.equals(loc));
  startingLocationName.value = "";
}

</script>

<template>
  <SingleEnumInput
      v-if="worlds"
      :value="null"
      :possible_values="worldsNames"
      @edit="onSelectWorld"
  />

  <SplitPanel storage-key="StartingLocations:outer">
    <template #left>
      <List
          v-if="worldSelected"
          :key="filteredLocations"
          :elements="filteredLocations"
          @create="onCreateLink"
          @edit="element => onSelectStartingLocation(element)"
          @remove="element => onDeleteStartingLocation(element)"
      />
    </template>

    <template #right>
      <FieldEditorWrapper
          v-if="selectedLocation"
          field-name="Name"
      >
        {{ selectedLocation.get("name") }}
      </FieldEditorWrapper>
      <div v-if = "selectedStartingLocation != null" :key="selectedStartingLocation">
        <FieldEditorWrapper
            :vertical="true"
            field-name="Reason_why"
            info="Will be injected at the very first turns as the reason why the character is there. After TTL turns, it'll go away permanently"
        >
          <LongTextBox
              :model-value="selectedStartingLocation.get('reason_why')"
              @edit = "payload => selectedStartingLocation!.update('reason_why', payload)"
          />
        </FieldEditorWrapper>
        <FieldEditorWrapper
            :vertical="true"
            field-name="TTL"
            info="After TTL turns, reason_why will no longer be injected into the prompt"
        >
          <NumberInput
              :model-value="selectedStartingLocation.get('ttl')"
              @edit = "payload => selectedStartingLocation!.update('ttl', payload)"
          />
        </FieldEditorWrapper>
      </div>
    </template>
  </SplitPanel>

  <EnumPrompt
      v-if="locationOptionsOpen"
      message="Select starting location"
      :options="allLocationsNames"
      @select="onPickLocationByName"
      @close="onCloseLocationPrompt"
  />
</template>