<script setup lang="ts">
import {computed, onMounted, ref, shallowRef} from "vue";
import List from "@/components/utils/list/List.vue";
import SplitPanel from "@/components/utils/panels/SplitPanel.vue";
import {World, Location} from "@/domain/World";
import {computedAsync, get} from "@vueuse/core";
import {Lorebook} from "@/domain/Lorebook";
import ShortTextBox from "@/components/utils/primitiveEditors/ShortTextBox.vue";
import LorebookEditor from "@/components/lorebooks/LorebookEditor.vue";
import LocationEditor from "@/components/space/LocationEditor.vue";
import Expandable from "@/components/utils/panels/Expandable.vue";
import FieldEditorWrapper from "@/components/utils/FieldEditorWrapper.vue";
import {API_BASE} from "@/config";
import {fetchApi} from "@/frameworks/ABSEntity";
import IconButton from "@/components/utils/buttons/IconButton.vue";

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`
// MODEL & EMITS
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`
const model = defineModel<World>({required: true, type: World});
const emit = defineEmits<{
  (e: "stopEditing"): void;
  (e: "delete"): void;
}>();

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
  locations.value.push(location);
}

async function onEdit(location: Location) {
  if (locationToEdit.value && locationToEdit.value.equals(location)) locationToEdit.value = null;
  else locationToEdit.value = location as Location;
}

async function onExportWorld(): Promise<void> {
  if (!model.value) return;

  try {
    const worldId = model.value.get('id');
    const response = await fetchApi(`${API_BASE}/import/world?worldId=${worldId}`, {
      method: 'GET',
      headers: {
        accept: 'application/json',
      },
    });

    const json = await response.json();
    const blob = new Blob([JSON.stringify(json, null, 2)], {type: 'application/json'});
    const url = URL.createObjectURL(blob);

    const link = document.createElement('a');
    link.href = url;
    link.download = `${model.value.get('name')}.json`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    URL.revokeObjectURL(url);
  } catch (e) {
    console.error(e);
  }
}

onMounted(() => {
  load()
})

async function load() {
  locations.value = await model.value.getLocations();
}
</script>

<template>
  <div>
    <!-- Go back to world list -->
    <div class="world-editor-actions">
      <IconButton
          title="Back to world list"
          @click="emit('stopEditing')"
      >
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M19 12H5"/>
          <path d="m12 19-7-7 7-7"/>
        </svg>
      </IconButton>

      <div class="world-editor-actions__name">
        <ShortTextBox
            :model-value="worldName"
            aria-label="World name"
            @edit="payload => worldName = payload"
        />
      </div>

      <IconButton
          title="Export world as JSON"
          variant="accent"
          @click="onExportWorld"
      >
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M12 3v12"/>
          <path d="m7 10 5 5 5-5"/>
          <path d="M5 21h14"/>
        </svg>
      </IconButton>

      <IconButton
          title="Delete world"
          variant="danger"
          @click="emit('delete')"
      >
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M3 6h18"/>
          <path d="M8 6V4h8v2"/>
          <path d="M19 6 18 21H6L5 6"/>
          <path d="M10 11v6"/>
          <path d="M14 11v6"/>
        </svg>
      </IconButton>
    </div>

    <div class="background-edit-box">
      <Expandable
          style="max-height: 100dvh;overflow:scroll"
          info="Logical groupings of locations"
          title="World Editor"
      >
        <!-- World editor (natural height, scrolls away) -->
        <div class="background-edit-box">
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
    </div>

    <!-- Locations editor (full-screen section) -->
    <div>
      <SplitPanel storage-key="WorldEdit">
        <template #left>
          <List
              v-if="locations"
              :elements="locations!"
              @create="onCreate"
              @edit="(element) => onEdit(element as Location)"
              @remove="element => model.deleteLocation(element.get('id')!)"
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
.world-editor-actions {
  position: sticky;
  top: 0;
  z-index: 10;

  display: flex;
  align-items: center;
  gap: 0.5rem;

  width: 100%;
  min-width: 0;
  margin-bottom: 0.75rem;
  padding: 0.4rem;

  background: color-mix(
      in srgb,
      var(--primary-background, #1c1917) 92%,
      transparent
  );

  border: 1px solid color-mix(
      in srgb,
      currentColor 18%,
      transparent
  );

  border-radius: 0.55rem;
  box-shadow: 0 4px 12px rgb(0 0 0 / 0.14);

  backdrop-filter: blur(8px);
}

.world-editor-actions__name {
  flex: 1 1 auto;
  min-width: 8rem;
  max-width: 30rem;
}

.world-editor-actions__name :deep(input) {
  width: 100%;
  min-width: 0;

  font-size: 1rem;
  font-weight: 600;
}

@media (max-width: 600px) {
  .world-editor-actions__name {
    min-width: 0;
    max-width: none;
  }
}
</style>