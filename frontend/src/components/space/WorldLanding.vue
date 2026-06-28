<script setup lang="ts">

import {World, WorldData, WorldKey} from "@/domain/World";
import {EntityTypes} from "@/domain/EntityTypes";
import {computed, onMounted, ref, shallowRef} from "vue";
import WorldEdit from "@/components/space/WorldEdit.vue";
import {createEntity, deleteEntity, fetch_all, fetchApi} from "@/frameworks/ABSEntity";
import SingleEnumInput from "@/components/utils/primitiveEditors/SingleEnumInput.vue";
import {API_BASE} from "@/config";

const worlds = ref<World[]>([]);
const worldNames = computed<string[]>(() => worlds.value.map(world => world.get('name')))

const isEditing = ref<boolean>(false);
const editingWorld = shallowRef<World | null>(null);

const importInput = ref<HTMLInputElement | null>(null);
const importing = ref<boolean>(false);

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

function openImportPicker(): void {
  importInput.value?.click();
}

async function onImportFileSelected(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];

  if (!file) {
    return;
  }

  try {
    importing.value = true;

    const text = await file.text();

    const response = await fetchApi(`${API_BASE}/import/world`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: text,
    });

    const dto = await response.json();
    const importedWorld = new World(dto, EntityTypes.WORLDS);

    worlds.value.push(importedWorld);
    onEdit(importedWorld);
  } catch (e) {
    console.error(e);
  } finally {
    importing.value = false;

    // Allows selecting the same file again.
    input.value = '';
  }
}

async function onDelete(){
  if (!isEditing.value || !editingWorld) return;
  const confirm = window.confirm("Are you sure you want to delete this world?");
  if (!confirm) return;

  const success = await deleteEntity<WorldKey>(editingWorld.value?.key!, EntityTypes.WORLDS);

  if (success) {
    isEditing.value = false;
    worlds.value = worlds.value.filter(other => !other.equals(editingWorld.value))
    editingWorld.value = null;
  }
}

onMounted(async () => {
  worlds.value = await fetch_all<WorldKey, WorldData, World>(EntityTypes.WORLDS, World);
})
</script>

<template>
  <div style="height: 200dvh; max-height: 200dvh">
    <button
        v-if="!isEditing"
        type="button"
        @click="onCreate"
    >
      New
    </button>
    <input
        ref="importInput"
        type="file"
        class="hidden-file-input"
        accept=".json"
        @change="onImportFileSelected"
    />
    <button
        v-if="!isEditing"
        type="button"
        :disabled="importing"
        @click="openImportPicker"
    >
      {{ importing ? 'Importing…' : 'Import' }}
    </button>
    <SingleEnumInput
        v-if="!isEditing"
        :value="null"
        :possible_values="worldNames"
        @edit="value => onEdit(worlds.find(i => i.get('name') == value)! as World)"
    />
    <WorldEdit
        style="height:100dvh; width: 100dvw; overflow:hidden"
        v-if="isEditing && editingWorld != null"
        v-model="editingWorld!"
        @delete="onDelete"
        @stop-editing="isEditing = false; editingWorld = null"
    />
  </div>
</template>

<style scoped>
.full_visor{
  height:100dvh;
}

.hidden-file-input {
  display: none;
}
</style>