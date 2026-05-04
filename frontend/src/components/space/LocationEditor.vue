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
import FieldEditorWrapper from "@/components/utils/FieldEditorWrapper.vue";

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
    <div class = "background-edit-box">
      <FieldEditorWrapper
        field-name="Name"
        info="Location's name. Metadata, won't be injected by default unless the outlet is used"
      >
        <ShortTextBox
            :model-value="name"
            @edit="payload => name = payload"
        />
      </FieldEditorWrapper>
      <FieldEditorWrapper
          field-name="Description"
          info="Location description, will be injected in prompt constantly if its the current location"
          :vertical="true"
      >
        <LongTextBox
            :model-value="description"
            @edit="payload => description = payload"
        />
      </FieldEditorWrapper>

      <Expandable
          title="Location info"
          :initially-open="false"
          info="Lorebook that will be activated if the characters are at this particular location"
      >
        <LorebookEditor
            v-if="lorebook"
            :model-value="lorebook"
        />
      </Expandable>
      <Expandable
        title="Edges editor"
        info="Controls which locations"
      >
        <LocationEdgesEditor
            :model-value="{ parentLocation:model.location, all_locations:model.all_locations }"
        />
      </Expandable>
    </div>
  </div>
</template>

<style scoped>

</style>