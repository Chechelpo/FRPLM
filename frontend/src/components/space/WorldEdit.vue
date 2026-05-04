<script setup lang="ts">
import {computed, ref, shallowRef} from "vue";
import List from "@/components/utils/list/List.vue";
import SplitPanel from "@/components/utils/panels/SplitPanel.vue";
import {World, Location} from "@/domain/entities/World";
import {computedAsync, get} from "@vueuse/core";
import {Lorebook} from "@/domain/entities/Lorebook";
import ShortTextBox from "@/components/utils/field-editors/ShortTextBox.vue";
import LorebookEditor from "@/components/lorebooks/LorebookEditor.vue";
import LocationEditor from "@/components/space/LocationEditor.vue";
import Expandable from "@/components/utils/panels/Expandable.vue";

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
const locations = computedAsync<Location[]>(
    async () => {
      return await model.value.getLocations()
    }
);

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`
// Editing
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`
const locationToEdit = shallowRef<Location | null>(null);

async function onCreate() { //Create location of a world
  const name = window.prompt("Enter new location name:");
  if (!name) return;
  await model.value.addLocation(name)
}

async function onEdit(location: Location) {
  if (locationToEdit.value && locationToEdit.value.equals(location)) locationToEdit.value = null;
  else locationToEdit.value = location as Location;
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

    <!-- World editor (natural height, scrolls away) -->
    <div class="worldBox">
      World info edit

      <ShortTextBox
          class="opacity-100"
          :model-value="worldName"
          @edit="payload => worldName = payload"
      />
      <Expandable
          title="Lorebook editor"
          :initially-open="false"
      >
        <LorebookEditor
            class="opacity-100"
            v-if="lorebook"
            :model-value="lorebook"
        />
      </Expandable>
    </div>
    <!-- Locations editor (full-screen section) -->
    <div class="flex-1 min-h-0 flex">
      <SplitPanel storage-key="WorldEdit" class="flex-1 min-h-0">
        <template #left>
          <List
              v-if="locations"
              :elements="locations!"
              @create="onCreate"
              @edit="(element) => onEdit(element as Location)"
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
.worldBox {
  background-color: color-mix(
      in srgb,
      var(--secondary-background) 40%,
      transparent
  );
  border: 1px solid var(--primary-accent) rounded;
  display: flex;
  flex-direction: column;
}
</style>