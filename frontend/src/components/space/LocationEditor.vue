<script setup lang="ts">
import {Location} from "@/domain/entities/World";
import {computed} from "vue";
import {computedAsync} from "@vueuse/core";
import {Lorebook} from "@/domain/entities/Lorebook";
import ShortTextBox from "@/components/utils/field-editors/ShortTextBox.vue";
import LorebookEditor from "@/components/lorebooks/LorebookEditor.vue";
import LocationEdgesEditor from "@/components/space/LocationEdgesEditor.vue";
import Expandable from "@/components/utils/panels/Expandable.vue";
import LongTextBox from "@/components/utils/field-editors/LongTextBox.vue";

const model = defineModel<{
  location: Location;
  all_locations: Location[];
}>({required: true})

const lorebook = computedAsync<Lorebook>(async () => model.value.location.getLorebook())

const name = computed<string>({
  get() {
    return model.value.location.get("name");
  },
  set(value: string) {
    model.value.location.update("name", value);
  }
})
const description = computed<string>({
  get() {
    return model.value.location.get("description");
  },
  set(value: string) {
    model.value.location.update("description", value);
  }
})
</script>

<template>
  <div class="flex flex-col">
    <ShortTextBox
        :model-value="name"
        @edit="payload => name = payload"
    />
    <LongTextBox
        :model-value="description"
        @edit="payload => description = payload"
    />
    <Expandable
        title="Location lorebook"
        :initially-open="false"
    >
      <LorebookEditor
          v-if="lorebook"
          :model-value="lorebook"
      />
    </Expandable>
    <LocationEdgesEditor
        :model-value="{ parentLocation:model.location, all_locations:model.all_locations }"
    />
  </div>
</template>

<style scoped>

</style>