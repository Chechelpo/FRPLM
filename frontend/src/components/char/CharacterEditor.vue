<script setup lang="ts">
import { Character, CharacterData, CharacterKey } from "@/domain/entities/chars/Characters";
import ShortTextBox from "@/components/utils/entity_editors/fields/textboxes/ShortTextBox.vue";
import { CommonFields } from "@/utils/CommonFields";
import { onMounted, ref, watch } from "vue";
import { Lorebook } from "@/domain/entities/lorebook/Lorebook";
import { fetchOne } from "@/domain/entities/EntityFetch";
import { EntityTypes } from "@/frameworks/entities/EntityTypes";
import LorebookEditor from "@/components/lorebooks/main/LorebookEditor.vue";
import TagAutocomplete from "@/components/tags/TagAutocomplete.vue";
import { Tag } from "@/domain/entities/tags/Tag";

const model = defineModel<Character | number>({
  required: true
});

const characterRef = ref<Character>();
const embed_lorebook = ref<Lorebook>();
const characterTags = ref<Tag[]>([]);

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`
// TAGS:
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`
async function loadCharacter(value: Character | number) {
  let character: Character;

  if (typeof value === "number") {
    character = await fetchOne<CharacterKey, CharacterData, Character>(
        { id: value },
        EntityTypes.CHARACTERS,
        Character
    );
  } else {
    character = value;
  }

  console.info(`Editing character ${character}`);

  characterRef.value = character;
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
// TAGS:
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`
async function handleNewTag(tag: Tag) {
  const character = characterRef.value;
  if (!character) return;

  console.trace(`Adding tag ${tag} to character ${character}`);

  await character.addTag(tag);

  const exists = characterTags.value.some(t => t.equals(tag));

  if (!exists) {
    characterTags.value = await character.getTags()
  }
}

async function handleRemoveTag(tag: Tag) {
  const character = characterRef.value;
  if (!character) return;

  console.trace(`Removing tag ${tag} for character ${character}`);

  await character.removeTag(tag);

  characterTags.value = await character.getTags()
}
</script>

<template>
  <div class="all_fields_rows">
    <!-- name -->
    <ShortTextBox
        v-if="characterRef"
        :read_only="false"
        :initial_value="characterRef.getCommon(CommonFields.NAME) as string"
    />

    <!-- Tag editor -->
    <div v-if="characterRef">
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
        :lorebook="embed_lorebook"
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