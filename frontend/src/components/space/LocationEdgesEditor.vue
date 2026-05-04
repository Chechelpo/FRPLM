<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { Location } from "@/domain/entities/World";

const model = defineModel<{
  parentLocation: Location;
  all_locations: Location[];
}>({ required: true });

const selectedLocation = ref<Location | null>(null);
const neighbours = ref<Location[]>([]);
const edgeDescription = ref<string>("");

const possibleLocations = computed(() => {
  return model.value.all_locations.filter(location => {
    return !location.equals(model.value.parentLocation);
  });
});

const selectedIsNeighbour = computed(() => {
  if (!selectedLocation.value) return false;

  return neighbours.value.some(location =>
      location.equals(selectedLocation.value!)
  );
});

async function refreshNeighbours() {
  neighbours.value = await model.value.parentLocation.getNeighbours();
}

async function selectLocation(locationId: number | string) {
  const numericId = Number(locationId);

  const location = model.value.all_locations.find(location =>
      location.get("id") === numericId
  );

  if (!location) return;

  selectedLocation.value = location;
  edgeDescription.value = "";

  const alreadyNeighbour = await model.value.parentLocation.isNeighbour(location);

  if (!alreadyNeighbour) {
    const success = await model.value.parentLocation.connect(location);

    if (success) {
      await refreshNeighbours();
    }
  }
}

async function updateDescription() {
  if (!selectedLocation.value) return;

  await model.value.parentLocation.updateEdgeInfo(
      selectedLocation.value,
      "description",
      edgeDescription.value
  );
}

onMounted(async () => {
  await refreshNeighbours();
});
</script>

<template>
  <div class="flex flex-col flex-1 min-h-0 gap-4">
    <label class="flex flex-col gap-1">
      <span>Connected location</span>

      <select
          class="border rounded p-2"
          :value="selectedLocation?.get('id') ?? ''"
          @change="selectLocation(($event.target as HTMLSelectElement).value)"
      >
        <option disabled value="">
          Select location
        </option>

        <option
            v-for="location in possibleLocations"
            :key="location.get('id')"
            :value="location.get('id')"
        >
          {{ location.get("name") }}
        </option>
      </select>
    </label>

    <div
        v-if="selectedLocation"
        class="flex flex-col gap-2"
    >
      <p v-if="selectedIsNeighbour">
        Edge exists.
      </p>

      <p v-else>
        Creating edge...
      </p>

      <label class="flex flex-col gap-1">
        <span>Edge description</span>

        <textarea
            v-model="edgeDescription"
            class="border rounded p-2 min-h-24"
        />
      </label>

      <button
          class="border rounded p-2"
          @click="updateDescription"
      >
        Update description
      </button>
    </div>
  </div>
</template>