<script setup lang="ts">
import {Character, CharacterKey, CharacterData} from "@/domain/entities/chars/Characters";
import ShortTextBox from "@/components/utils/entity_editors/fields/textboxes/ShortTextBox.vue";
import {CommonFields} from "@/utils/CommonFields";
import {onMounted, ref} from "vue";
import {Lorebook, LorebookData, LorebookKey} from "@/domain/entities/lorebook/Lorebook";
import {fetchOne} from "@/utils/EntityFetch";
import {EntityTypes} from "@/domain/entities/EntityTypes";
import EntryList from "@/components/lorebooks/Entry/EntryList.vue";

const props = defineProps<{
  editCharacter: Character | number
}>()
const embed_lorebook = ref<Lorebook>();
const characterRef = ref<Character>();

onMounted(async () => {
  let character:Character;
  if (typeof props.editCharacter === 'number'){
    character = await fetchOne<CharacterKey,CharacterData,Character>(
        {
          id:props.editCharacter
        },
        EntityTypes.CHARACTERS,
        Character
    )
  } else character = props.editCharacter;
  console.info(`Editing character ${character}`)
  characterRef.value = character;

  embed_lorebook.value = await fetchOne<LorebookKey, LorebookData, Lorebook>(
      {
        id:characterRef.value.get("lorebook_id")!
      },
      EntityTypes.LOREBOOKS,
      Lorebook
  )
})

</script>

<template>
  <div class = "all_fields_rows">
    <!-- name -->
    <ShortTextBox
        :read_only="false",
        :initial_value="characterRef?.getCommon(CommonFields.NAME) as string">
    </ShortTextBox>

    <!-- Embed lorebook entry editor -->
    <EntryList
        v-if="characterRef"
        :lorebook="characterRef?.get('lorebook_id')!"
    ></EntryList>
  </div>
</template>

<style scoped>
.all_fields_rows{
  display: flex;
  flex-direction: row;
}
.top_fields_flex {
  display: flex;
  flex-direction: column;
}
</style>