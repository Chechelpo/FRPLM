<script setup lang="ts">
import {Location} from "@/domain/World";
import {onMounted, ref, watch} from "vue";
import {computedAsync} from "@vueuse/core";
import {Lorebook} from "@/domain/Lorebook";
import ShortTextBox from "@/components/utils/primitiveEditors/ShortTextBox.vue";
import LorebookEditor from "@/components/lorebooks/LorebookEditor.vue";
import LocationEdgesEditor from "@/components/space/LocationEdgesEditor.vue";
import Expandable from "@/components/utils/panels/Expandable.vue";
import FieldEditorWrapper from "@/components/utils/FieldEditorWrapper.vue";
import {Character, CharacterData, CharacterKey} from "@/domain/Characters";
import {createEntity, deleteEntity, fetchApi} from "@/frameworks/ABSEntity";
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

async function createCharacterInLocation(){
  const name = window.prompt("Enter new character name");
  if (!name) return;

  const newCharacter = await createEntity<CharacterKey,CharacterData,Character>(
      null,
      {
        name:name
      },
      EntityTypes.CHARACTERS,
      Character
  )
  await newCharacter.markStartingAt(model.value.location);
  charactersHere.value.push(newCharacter);
}
async function onDeleteCharacter(character: Character) {
  const confirm = window.confirm(`Delete ${character.get('name')}`)
  if (!confirm) return;

  const success = await deleteEntity<CharacterKey>(character.key, EntityTypes.CHARACTERS);
  if (!success) {
    console.error("Could not delete character");
    return;
  }

  charactersHere.value = charactersHere.value.filter(other => other.get('id') != character.get('id'));
}

watch(
    model.value.location,
    () => {
      fetchCharacters()
      editingCharacter.value = null;
    }
)
async function fetchCharacters() {
  charactersHere.value = await fetchApi(
      `${API_BASE}/${EntityTypes.CHARACTERS}/startingAt?worldId=${model.value.location.get('worldID')}&locationId=${model.value.location.get('id')}`
      ,
      {
        method: 'GET',
      }
  ).then(async response => (await response.json() as DTO[]).map(dto => new Character(dto, EntityTypes.CHARACTERS)))
}
onMounted(async () => {
  await fetchCharacters();
})
</script>

<template>
  <div class="flex flex-col" style = "height:100dvh" :key="model.location.get('id')">
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
        title="Connected locations"
      >
        <LocationEdgesEditor
            :model-value="{ parentLocation:model.location, all_locations:model.all_locations }"
        />
      </Expandable>
      <Expandable title="Characters starting here" info="Static starting here, not the session locations">
        <split-panel storage-key="location-characters-editor" style="height:100dvh">
          <template #left>
            <List
              :elements="charactersHere as Character[]"
              @edit = "value => editingCharacter = value"
              @create ="createCharacterInLocation()"
              @remove = "value => onDeleteCharacter(value)"
            />
          </template>
          <template #right>
            <CharacterEditor
                style = "height:100dvh"
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