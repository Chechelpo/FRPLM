<script setup lang="ts">
import WindowPrompt from "@/components/utils/prompts/WindowPrompt.vue";
import {World, WorldData, WorldKey} from "@/domain/World";
import {Character} from "@/domain/Characters";
import {computed, ref, shallowRef} from "vue";
import {computedAsync} from "@vueuse/core";
import {fetch_all} from "@/frameworks/ABSEntity";
import {EntityTypes} from "@/domain/EntityTypes";
import SingleEnumInput from "@/components/utils/primitiveEditors/SingleEnumInput.vue";
import FieldEditorWrapper from "@/components/utils/FieldEditorWrapper.vue";
import ShortTextBox from "@/components/utils/primitiveEditors/ShortTextBox.vue";

const model = defineModel<boolean>({required:true, type: Boolean})
const emit = defineEmits<{
  (e: 'createNewSession', payload:{name:string, world:World, character:Character}) : void,
  (e: 'close'): void;
}>()

const newSessionName = ref<string>('');

// Worlds ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
const allWorlds = computedAsync<World[]>(async () => await fetch_all<WorldKey,WorldData,World>(
    EntityTypes.WORLDS,
    World), [])
const allWorldsNames = computed<string[]>(() => {
  if (!allWorlds.value) return [];
  return allWorlds.value.map(world => world.get('name'))
})
const selectedWorld = shallowRef<World | null>(null);
const selectedWorldName = computed(() => {
  if (selectedWorld.value != null) {
    return selectedWorld.value.get('name');
  }
  return '';
})

async function selectWorld(name: string){
  selectedWorld.value = allWorlds.value.find(world => world.get('name') == name)!;
  allCharacters.value = (await Character.getStartingAt(selectedWorld.value))
      .filter(character => character.get('can_be_user'));
}

// Characters ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
const allCharacters = ref<Character[]>([]);
const allCharactersNames = computed<string[]>(() => allCharacters.value.map(character => character.get('name')))
const characterName = computed<string>(() => {
  if (!selectedCharacter.value) return "";
  return selectedCharacter.value.get('name');
})
const selectedCharacter = shallowRef<Character | null>(null);

function createNewSession() : void {
  if (selectedCharacter.value == null || selectedWorld.value == null) {
    emit('close')
    return
  }
  emit('createNewSession',
      {
        name : newSessionName.value,
        world : selectedWorld.value,
        character : selectedCharacter.value
      }
  )
  model.value = false;
}
</script>

<template>
  <WindowPrompt v-if="model" title="New Session" @close="model = false">
    <template #default>
      <FieldEditorWrapper field-name="Name">
        <ShortTextBox
            :model-value="newSessionName"
            @edit="payload => newSessionName=payload"
        />
      </FieldEditorWrapper>
      <FieldEditorWrapper field-name="World">
        <SingleEnumInput
            :value="selectedWorldName"
            :possible_values="allWorldsNames"
            @edit="txt => selectWorld(txt)"
        />
      </FieldEditorWrapper>
      <FieldEditorWrapper field-name="As character">
        <SingleEnumInput
            :value="characterName"
            :possible_values="allCharactersNames"
            @edit="txt => selectedCharacter = allCharacters.find(char => char.get('name') == txt)!"
        />
      </FieldEditorWrapper>
    </template>
    <template #footer>
      <button
          type="button"
          @click="createNewSession"
      >
        Submit
      </button>
    </template>
  </WindowPrompt>
</template>

<style scoped>

</style>