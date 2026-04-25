<script setup lang="ts">
import List from "@/components/utils/list/List.vue";
import AvatarEditor from "@/components/utils/entity_editors/self-assembling/AvatarEditor.vue";

import {onMounted, ref} from "vue";
import SplitPanel from "@/components/utils/panels/SplitPanel.vue";
import StartingLocation from "@/components/char/StartingLocation.vue";
import Expandable from "@/components/utils/panels/Expandable.vue";
import {fetch_all} from "@/utils/EntityFetch";
import {Character, CharacterData, CharacterKey} from "@/domain/entities/chars/Characters";
import {ControllerType} from "@/config/ControllerType";
import {EntityABS} from "@/frameworks/entities/EntityABS";

/* data */
const characters = ref<Character[]>([]);
const editCharacter = ref<Character | null>(null);

/* actions */
async function onCreate() {
  const input_name = window.prompt("Enter new character name:");
  if (!input_name) return;

  const created = Character.createNew({name: input_name})

  editCharacter.value = await created;
  characters.value.push(editCharacter.value);
}

async function onEdit(character: Character) {
  if (editCharacter.value!= null && editCharacter.value.equals(character)) editCharacter.value = null;
  else editCharacter.value = character;
}

onMounted(async () => {
  characters.value = await fetch_all<CharacterKey, CharacterData, Character>(ControllerType.CHARACTERS, Character);
  editCharacter.value = null;
  console.log(characters);
});
</script>

<template>
  <SplitPanel storage-key="Character_edit:outer">
    <template #left>
      <List
          v-model:elements="characters as EntityABS<CharacterKey,CharacterData>[]"
          @create="onCreate"
          @edit="(element) => onEdit(element as Character)"
      ></List>
    </template>

    <template v-if="editCharacter" #right>
      <SplitPanel
          v-if = "editCharacter"
          storage-key="Character_edit:inner"
      >
        <template #left>
          <div class = "text-2xl w-fit h-fit"> General information editor </div>
          <AvatarEditor
              v-if="editCharacter"
              v-model:entity="editCharacter"
          >
          </AvatarEditor>
        </template>
        <template #right>
          <Expandable title="StartingLocations">
          <StartingLocation
              v-if = "editCharacter"
              :key = "editCharacter.get('id')"
              model-value:character = "editCharacter"
          ></StartingLocation>
          </Expandable>
        </template>
      </SplitPanel>

    </template>
  </SplitPanel>
</template>

<style scoped>


</style>