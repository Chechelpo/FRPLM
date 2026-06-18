<script setup lang="ts">
import {Location} from "@/domain/World";
import {computed, onMounted, ref} from "vue";
import {computedAsync} from "@vueuse/core";
import {Lorebook} from "@/domain/Lorebook";
import ShortTextBox from "@/components/utils/primitiveEditors/ShortTextBox.vue";
import LorebookEditor from "@/components/lorebooks/LorebookEditor.vue";
import LocationEdgesEditor from "@/components/space/LocationEdgesEditor.vue";
import Expandable from "@/components/utils/panels/Expandable.vue";
import LongTextBox from "@/components/utils/primitiveEditors/LongTextBox.vue";
import FieldEditorWrapper from "@/components/utils/FieldEditorWrapper.vue";
import {Character} from "@/domain/Characters";
import {fetchApi} from "@/frameworks/ABSEntity";
import {API_BASE} from "@/config";
import {EntityTypes} from "@/domain/EntityTypes";
import {DTO} from "@/types/DTOs";
import List from "@/components/utils/list/List.vue";
import SplitPanel from "@/components/utils/panels/SplitPanel.vue";
import CharacterEditor from "@/components/char/CharacterEditor.vue";

const model = defineModel<{
  location: Location;
  all_locations: Location[];
}>({required: true})

const lorebook = computedAsync<Lorebook>(async () => model.value.location.getLorebook())
const charactersHere = ref<Character[]>([])
const editingCharacter = ref<Character | null>(null);

onMounted(async () => {
  charactersHere.value = await fetchApi(
      `${API_BASE}/${EntityTypes.CHARACTERS}/startingAt?worldId=${model.value.location.get('worldID')}&locationId=${model.value.location.get('id')}`
      ,
      {
        method:'GET',
      }
  ).then(async response => (await response.json() as DTO[]).map(dto => new Character(dto, EntityTypes.CHARACTERS)))
})
</script>

<template>
  <div class="flex flex-col">
    <div class = "background-edit-box">
      <FieldEditorWrapper
        field-name="Name"
        info="Location's name."
      >
        <ShortTextBox
            :model-value="model.location.get('name')"
            @edit="payload => model.location.update('name', payload)"
        />
      </FieldEditorWrapper>
      <FieldEditorWrapper
          field-name="Description"
          info="Location description, will be injected in prompt constantly if its the current location"
          :vertical="true"
      >
        <!--
        <LongTextBox
            :model-value="description"
            @edit="payload => description = payload"
        />
        -->
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
      <Expandable title="Characters starting here" info="Static starting here, not the session locations">
        <split-panel storage-key="location-characters-editor">
          <template #left>
            <List
              :elements="charactersHere as Character[]"
              @edit = "value => editingCharacter = value"
            />
          </template>
          <template #right>
            <CharacterEditor
                v-if="editingCharacter"
                :model-value="editingCharacter as Character"
                :edit-starting-locations="false"
            />
          </template>
        </split-panel>
      </Expandable>
    </div>
  </div>
</template>

<style scoped>

</style>