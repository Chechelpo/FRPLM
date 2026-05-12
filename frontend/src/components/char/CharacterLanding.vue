<script setup lang="ts">
import List from "@/components/utils/list/List.vue";

import {onMounted, ref} from "vue";
import SplitPanel from "@/components/utils/panels/SplitPanel.vue";
import {Character, CharacterData, CharacterKey} from "@/domain/Characters";
import {EntityTypes} from "@/domain/EntityTypes";
import CharacterEditor from "@/components/char/CharacterEditor.vue";
import {deleteEntity, fetch_all} from "@/frameworks/ABSEntity";

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
  if (editCharacter.value != null && editCharacter.value.equals(character)) editCharacter.value = null;
  else editCharacter.value = character;
}
async function onDelete(character: Character) {
  const confirmation = window.confirm("Are you sure you want to delete this character?")
  if (!confirmation) return;

  await deleteEntity<CharacterKey>(character.key, EntityTypes.CHARACTERS);
  characters.value = characters.value.filter(t => !t.equals(character));
}
onMounted(async () => {
  characters.value = await fetch_all<CharacterKey, CharacterData, Character>(EntityTypes.CHARACTERS, Character);
  editCharacter.value = null;
  console.log(characters);
});
</script>

<template>
  <SplitPanel storage-key="Character_edit:outer">
    <template #left>
      <List
          v-model:elements="characters!"
          @create="onCreate"
          @edit="(element) => onEdit(element as Character)"
          @remove = "(element) => onDelete(element as Character)"
      ></List>
    </template>

    <template v-if="editCharacter" #right>
      <CharacterEditor
          v-if='editCharacter'
          v-model='editCharacter as Character'
      ></CharacterEditor>


    </template>
  </SplitPanel>
</template>

<style scoped>


</style>