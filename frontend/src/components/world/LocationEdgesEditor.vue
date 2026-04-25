<script setup lang="ts">

import {onMounted, ref, watch} from "vue";
import SimpleList from "@/components/utils/list/SimpleList.vue";
import Editor from "@/components/utils/entity_editors/self-assembling/Editor.vue";
import {LocationEdge} from "@/entities/space/LocationEdge";
import {Location} from "@/entities/space/Location";

const props = defineModel<{
  parentLocation : Location,
  locations : Location[]
}>({required:true});

const editingEdge = ref<LocationEdge | null>(null);
const selectedLocation = ref<Location | null>(null);
const createNew = ref<boolean>(false);

async function selectLocation(selLocation : Location){
  if (selLocation.equals(editingEdge.value)) return;
  console.info(props.value.locations);
}

async function createEdge() {
  if (!selectedLocation.value) return;

  createNew.value = false;
}

onMounted(() => {
})
</script>

<template>
  <div class="flex flex-col flex-1 min-h-0">
    <SimpleList
        :elements="props.locations"
        @select="selectLocation"
    />

    <div class="flex-1 min-h-0">
      <Editor
          v-if="editingEdge"
          :owner="editingEdge"
          :no-edit="[]"
      />

      <div v-else-if="createNew" class="create-edge p-4">
        <p>This edge does not exist.</p>
        <button @click="createEdge">Create edge</button>
      </div>
    </div>
  </div>
</template>