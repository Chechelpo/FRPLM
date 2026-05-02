<script setup lang="ts">
import { Character, CharacterData, CharacterKey } from "@/domain/entities/Characters";
import ShortTextBox from "@/components/utils/field-editors/ShortTextBox.vue";
import { CommonFields } from "@/utils/CommonFields";
import {computed, onMounted, ref, watch} from "vue";
import { Lorebook } from "@/domain/entities/Lorebook";
import { fetchOne } from "@/domain/entities/EntityFetch";
import { EntityTypes } from "@/frameworks/entities/EntityTypes";
import LorebookEditor from "@/components/lorebooks/LorebookEditor.vue";
import TagAutocomplete from "@/components/tags/TagAutocomplete.vue";
import { Tag } from "@/domain/entities/tags/Tag";

const model = defineModel<Character>({
  required: true
});

const embed_lorebook = ref<Lorebook>();
const characterTags = ref<Tag[]>([]);

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`
// TAGS:
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`
async function loadCharacter(character: Character) {
  console.info(`Editing character ${character}`);

  characterTags.value = await character.getTags();
  embed_lorebook.value = await character.getLorebook();

  console.log(
      `Editing character ${character} with:\n tags: ${characterTags.value}\n ${embed_lorebook.value}`
  );
}
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`
// EVENTS:
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`
onMounted(async () => {
  await loadCharacter(model.value);
});

watch(model, async newValue => {
  await loadCharacter(newValue);
});

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`
// ATTRIBUTES:
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`
const name = computed<string>({
  get(){
    return model.value.get('name');
  },
  set(value:string){
    model.value.update('name', value)
  }
})

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`
// TAGS:
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`
async function handleNewTag(tag: Tag) {
  console.debug(`Adding tag ${tag} to character ${model.value}`);

  await model.value.addTag(tag);

  const exists = characterTags.value.some(t => t.equals(tag));

  if (!exists) {
    characterTags.value = await model.value.getTags()
  }
}

async function handleRemoveTag(tag: Tag) {
  const character = model.value;
  if (!character) return;

  console.debug(`Removing tag ${tag} for character ${character}`);

  await character.removeTag(tag);

  characterTags.value = await character.getTags()
}
</script>

<template>
  <div class="all_fields_rows">
    <!-- name -->
    <ShortTextBox
        v-if="model"
        v-model="name"
        @edit="txt => name = txt"
    ></ShortTextBox>

    <!-- Tag editor -->
    <div v-if="model">
      Tags
      <TagAutocomplete
          v-if="characterTags"
          v-model="characterTags as Tag[]"
          @new-tag="handleNewTag"
          @remove-tag="handleRemoveTag"
      />
    </div>

    <!-- Embed lorebook entry editor -->
    <LorebookEditor
        v-if="embed_lorebook"
        v-model="embed_lorebook"
    />
  </div>
</template>

<style scoped>
.all_fields_rows {
  display: flex;
  flex-direction: column;
}

.top_fields_flex {
  display: flex;
  flex-direction: column;
}
</style>