<script setup lang="ts">
import {
  computed,
  onMounted,
  ref,
} from "vue";

import {
  fetchOne,
} from "@/core/ABSEntity";
import { EntityTypes } from "@/domain/EntityTypes";
import {
  Location,
  Region,
  World,
  WorldData,
  WorldKey,
} from "@/domain/World";

import WindowPrompt from "@/components/utils/prompts/WindowPrompt.vue";
import SingleEnumInput from "@/components/utils/primitiveEditors/SingleEnumInput.vue";
import FieldEditorWrapper from "@/components/utils/FieldEditorWrapper.vue";

export type ConnectionDirection =
    | "outgoing"
    | "incoming"
    | "both";

const props = defineProps<{
  parentLocation: Location;
  connectableLocations: Location[];
}>();

const emit = defineEmits<{
  select: [
    location: Location,
    direction: ConnectionDirection,
  ];
  close: [];
}>();

const regions = ref<Region[]>([]);
const loading = ref(false);

const regionNames = computed<string[]>(() =>
    regions.value
        .map((reg) =>
            String(
                reg.get("name") ??
                "Unnamed region",
            ),
        )
        .sort((a, b) =>
            a.localeCompare(b),
        ),
);

const regionFilter = ref<Region | null>(
    null,
);

const selectedLocation =
    ref<Location | null>(null);

const selectedDirection =
    ref<ConnectionDirection>("outgoing");

const directionNames = [
  "Outgoing",
  "Incoming",
  "Bidirectional",
];

const selectedDirectionName = computed(
    () => {
      switch (selectedDirection.value) {
        case "incoming":
          return "Incoming";

        case "both":
          return "Bidirectional";

        case "outgoing":
        default:
          return "Outgoing";
      }
    },
);

const filteredLocations =
    computed<Location[]>(() => {
      if (regionFilter.value == null) {
        return props.connectableLocations;
      }

      const regionId =
          regionFilter.value.get("id");

      return props.connectableLocations.filter(
          (loc) =>
              loc.get("region_id") ===
              regionId,
      );
    });

const filteredLocationNames =
    computed<string[]>(() =>
        filteredLocations.value
            .map((loc) =>
                String(
                    loc.get("name") ??
                    "Unnamed location",
                ),
            )
            .sort((a, b) =>
                a.localeCompare(b),
            ),
    );

function setRegionFilter(
    name: string,
): void {
  regionFilter.value =
      regions.value.find(
          (other) =>
              other.get("name") === name,
      ) ?? null;

  if (
      selectedLocation.value &&
      !filteredLocations.value.includes(
          selectedLocation.value,
      )
  ) {
    selectedLocation.value = null;
  }
}

function setSelectedLocation(
    name: string,
): void {
  selectedLocation.value =
      filteredLocations.value.find(
          (other) =>
              other.get("name") === name,
      ) ?? null;
}

function setSelectedDirection(
    name: string,
): void {
  switch (name) {
    case "Incoming":
      selectedDirection.value =
          "incoming";
      break;

    case "Bidirectional":
      selectedDirection.value =
          "both";
      break;

    case "Outgoing":
    default:
      selectedDirection.value =
          "outgoing";
      break;
  }
}

function confirm(): void {
  if (!selectedLocation.value) {
    emit("close");
    return;
  }

  emit(
      "select",
      selectedLocation.value,
      selectedDirection.value,
  );
}

function close(): void {
  emit("close");
}

onMounted(async () => {
  loading.value = true;

  try {
    const world =
        await fetchOne<
            WorldKey,
            WorldData,
            World
        >(
            {
              id:
                  props.parentLocation.get(
                      "worldID",
                  ),
            },
            EntityTypes.WORLDS,
            World,
        );

    regions.value =
        await world.getAllRegions();
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <WindowPrompt
      title="Connect a location"
      info="Filter candidate locations by region, select a location, and choose the connection direction."
      :min-width="440"
      :min-height="430"
      @close="close"
  >
    <div class="connect-location-prompt">
      <FieldEditorWrapper
          field-name="Region"
          info="Restrict the candidate list to a specific region."
          :vertical="true"
      >
        <SingleEnumInput
            :value="
            regionFilter
              ? regionFilter.get('name')
              : ''
          "
            :possible_values="regionNames"
            placeholder="All regions"
            @edit="setRegionFilter"
        />
      </FieldEditorWrapper>

      <FieldEditorWrapper
          field-name="Location"
          info="The other location participating in the connection."
          :vertical="true"
      >
        <SingleEnumInput
            :value="
            selectedLocation
              ? selectedLocation.get('name')
              : ''
          "
            :possible_values="
            filteredLocationNames
          "
            :placeholder="
            loading
              ? 'Loading...'
              : 'Select a location'
          "
            @edit="setSelectedLocation"
        />
      </FieldEditorWrapper>

      <FieldEditorWrapper
          field-name="Direction"
          info="Outgoing connects the current location to the selected location. Incoming reverses that direction. Bidirectional creates both edges."
          :vertical="true"
      >
        <SingleEnumInput
            :value="selectedDirectionName"
            :possible_values="directionNames"
            @edit="setSelectedDirection"
        />
      </FieldEditorWrapper>

      <div
          v-if="
          !loading &&
          filteredLocations.length === 0
        "
          class="
          edit-box__state
          edit-box__state--vertical
          connect-location-prompt__empty
        "
      >
        <div class="edit-box__state-icon">
          <svg
              viewBox="0 0 24 24"
              aria-hidden="true"
          >
            <circle
                cx="6"
                cy="12"
                r="3"
            />

            <circle
                cx="18"
                cy="12"
                r="3"
            />

            <path d="M9 12h6" />
          </svg>
        </div>

        <div class="edit-box__state-content">
          <strong class="edit-box__state-title">
            No candidates
          </strong>

          <p class="edit-box__state-description">
            No connectable locations match the
            selected region.
          </p>
        </div>
      </div>

      <div
          class="
          connect-location-prompt__count
        "
          aria-live="polite"
      >
        <span class="edit-box__badge">
          {{ filteredLocations.length }}

          {{
            filteredLocations.length === 1
                ? "candidate"
                : "candidates"
          }}
        </span>

        <span
            v-if="loading"
            class="
            edit-box__badge
            edit-box__badge--neutral
          "
        >
          Loading regions
        </span>
      </div>
    </div>

    <template #footer>
      <button
          type="button"
          class="edit-box__action"
          @click="close"
      >
        Cancel
      </button>

      <button
          type="button"
          class="
          edit-box__action
          edit-box__action--accent
        "
          :disabled="
          !selectedLocation || loading
        "
          @click="confirm"
      >
        Connect
      </button>
    </template>
  </WindowPrompt>
</template>

<style scoped>
.connect-location-prompt {
  width: 100%;
  min-width: 0;

  display: flex;
  flex-direction: column;
  gap: var(--space-3);

  padding: var(--space-2);

  color: rgb(var(--c-fg));
  font-family: var(--font-primary);
}

.connect-location-prompt__empty {
  min-height: 9rem;
}

.connect-location-prompt__count {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-2);

  min-width: 0;

  padding-top: var(--space-1);
}

@media (max-width: 480px) {
  .connect-location-prompt {
    padding: var(--space-1);
  }
}
</style>