<script setup lang="ts">
import {computed, onMounted, ref, shallowRef} from "vue";
import List from "@/components/utils/list/List.vue";
import SplitPanel from "@/components/utils/panels/SplitPanel.vue";
import {World, Location} from "@/domain/entities/World";
import {computedAsync, get} from "@vueuse/core";
import {Lorebook} from "@/domain/entities/Lorebook";
import ShortTextBox from "@/components/utils/field-editors/ShortTextBox.vue";
import LorebookEditor from "@/components/lorebooks/LorebookEditor.vue";
import LocationEditor from "@/components/space/LocationEditor.vue";
import Expandable from "@/components/utils/panels/Expandable.vue";
import FieldEditorWrapper from "@/components/utils/FieldEditorWrapper.vue";

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`
// MODEL & EMITS
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`
const model = defineModel<World>({required: true, type: World});
const emit = defineEmits<{ (e: "stopEditing"): void; }>();

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`
// WORLD INFO
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`
const worldName = computed<string>({
  get() {
    return model.value.get('name');
  },
  set(value: string) {
    model.value.update('name', value);
  }
})
const lorebook = computedAsync<Lorebook>(async () => {
  return await model.value.getLorebook()
})
const locations = ref<Location[]>([]);

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`
// Editing
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`
const locationToEdit = shallowRef<Location | null>(null);

async function onCreate() { //Create location of a world
  const name = window.prompt("Enter new location name:");
  if (!name) return;
  const location = await model.value.addLocation(name)
  if (!location) return;
  console.debug(`Created location ${location}`)
  locations.value = await model.value.getLocations();
}

async function onEdit(location: Location) {
  if (locationToEdit.value && locationToEdit.value.equals(location)) locationToEdit.value = null;
  else locationToEdit.value = location as Location;
}

onMounted(() => {
  load()
})
async function load(){
  locations.value = await model.value.getLocations();
}
</script>

<template>
  <div class="flex flex-col flex-1 min-h-0 gap-4">
    <!-- Go back to world list -->
    <button
        type="button"
        class="px-3 py-1 rounded border"
        @click="emit('stopEditing')"
    >
      Back
    </button>
    <Expandable
      info="Logical groupings of locations"
      title="World Editor"
    >
      <!-- World editor (natural height, scrolls away) -->
      <div class="background-edit-box">
        World info edit
        <FieldEditorWrapper
            field-name="World's Name "
            info="Purely metadata, won't be injected at runtime unless you use the outlet {{worldname}}"
        >
          <ShortTextBox
              class="opacity-100"
              :model-value="worldName"
              @edit="payload => worldName = payload"
          />
        </FieldEditorWrapper>
        <Expandable
            title="World Info"
            info="Lorebook active throughout an entire session, regardless of location"
            :initially-open="false"
        >
          <LorebookEditor
              class="opacity-100"
              v-if="lorebook"
              :model-value="lorebook"
          />
        </Expandable>
      </div>
    </Expandable>

    <!-- Locations editor (full-screen section) -->
    <div class="flex-1 min-h-0 flex">
      <SplitPanel storage-key="WorldEdit" class="flex-1 min-h-0">
        <template #left>
          <List
              v-if="locations"
              :elements="locations!"
              @create="onCreate"
              @edit="(element) => onEdit(element as Location)"
              @remove = "element => model.deleteLocation(element.get('id')!)"
          />
        </template>

        <template #right>
          <LocationEditor
              v-if="locationToEdit"
              :model-value="{location: locationToEdit, all_locations: locations!}"
          />
        </template>
      </SplitPanel>
    </div>
  </div>
</template>


<style scoped>
</style>