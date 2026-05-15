<script setup lang="ts">
import {computed, onMounted, ref, watch} from "vue";
import {Location, LocationEdge} from "@/domain/World";
import SplitPanel from "@/components/utils/panels/SplitPanel.vue";
import List from "@/components/utils/list/List.vue";
import EnumPrompt from "@/components/utils/prompts/EnumPrompt.vue";
import {computedAsync} from "@vueuse/core";
import FieldEditorWrapper from "@/components/utils/FieldEditorWrapper.vue";
import LongTextBox from "@/components/utils/primitives/LongTextBox.vue";

const model = defineModel<{
  parentLocation: Location;
  all_locations: Location[];
}>({required: true});

const all_locations_names = computed<string[]>(() => model.value.all_locations
    .filter(loc => !loc.equals(model.value.parentLocation))
    .map(loc => loc.get('name'))
)
const all_locations_by_name = computed<Map<string, Location>>(() => {
  const map = new Map<string, Location>()
  model.value.all_locations
      .filter(loc => !loc.equals((model.value.parentLocation)))
      .forEach(loc => map.set(loc.get('name'), loc))
  return map;
})

const neighbours = computedAsync<Location[]>(async () => await model.value.parentLocation.getNeighbours());
const creatingNewEdge = ref<boolean>(false);

function startCreating() {
  creatingNewEdge.value = true;
}

async function createNewEdge(name: string) {
  creatingNewEdge.value = false;
  const location = all_locations_by_name.value.get(name);
  if (!location || location.equals(model.value.parentLocation)) return;

  const success = await model.value.parentLocation.connect(location);
}

async function disconnectEdge(location: Location) {
  await model.value.parentLocation.disconnect(location);
}

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// Edge editing
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

async function onSelectEdge(location: Location) {
  if (selectedEdge.value?.equals(location)) {
    selectedEdge.value = null
    editingEdgeName.value = ""
    return;
  }

  editingEdgeName.value = location.get('name');
  selectedEdge.value = await model.value.parentLocation.getEdgeInfo(location);
}

const editingEdgeName = ref<string>("");
const selectedEdge = ref<LocationEdge | null>(null);
const edgeDescription = computed<string | null>({
  get() {
    if (selectedEdge.value == null) return null;
    return selectedEdge.value.get('description');
  },
  set(value: string) {
    if (selectedEdge.value == null) return;
    selectedEdge.value.update('description', value);
  }
});

watch(
    () => model.value.parentLocation,
    () => {
  selectedEdge.value = null;
  editingEdgeName.value = "";
})
</script>

<template>
  <SplitPanel storage-key="LocationEdgesEditor">
    <template #left>
      <List
          v-if="neighbours"
          :type="Location"
          :elements="neighbours"
          @create="startCreating"
          @edit="loc => onSelectEdge(loc)"
          @remove="disconnectEdge"
      />
    </template>
    <template #right>
      <div v-if = selectedEdge>
        <FieldEditorWrapper
            field-name="Edge to"
        >
          <div> {{ editingEdgeName }}</div>
        </FieldEditorWrapper>
        <FieldEditorWrapper
            field-name="Edge description"
            info="Will be injected into both locations"
        >
          <LongTextBox
              :model-value="edgeDescription"
              @edit="payload => edgeDescription = payload"
          />
        </FieldEditorWrapper>
      </div>
    </template>
  </SplitPanel>
  <EnumPrompt
      v-if="creatingNewEdge"
      message="Select new location to connect"
      :options="all_locations_names"
      @select="option => createNewEdge(option)"
      @close="creatingNewEdge = false"
  />
</template>