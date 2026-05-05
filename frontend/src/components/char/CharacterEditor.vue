<script setup lang="ts">
import {Character} from "@/domain/entities/Characters";
import ShortTextBox from "@/components/utils/field-editors/ShortTextBox.vue";
import {computed, onMounted, ref, shallowRef, watch} from "vue";
import {Lorebook} from "@/domain/entities/Lorebook";
import {fetch_all} from "@/domain/entities/EntityFetch";
import {EntityTypes} from "@/frameworks/entities/EntityTypes";
import LorebookEditor from "@/components/lorebooks/LorebookEditor.vue";
import TagAutocomplete from "@/components/tags/TagAutocomplete.vue";
import {Tag} from "@/domain/entities/Tag";
import FieldEditorWrapper from "@/components/utils/FieldEditorWrapper.vue";
import Expandable from "@/components/utils/panels/Expandable.vue";
import {computedAsync} from "@vueuse/core";
import {Location, World, WorldData, WorldKey} from "@/domain/entities/World";
import List from "@/components/utils/list/List.vue";
import SplitPanel from "@/components/utils/panels/SplitPanel.vue";
import StartingLocation from "@/components/char/StartingLocation.vue";

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
  get() {
    return model.value.get('name');
  },
  set(value: string) {
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
  <div class="all_fields_rows background-edit-box">
    <div>
      <!-- name -->
      <div class="flex flex-row">
        <FieldEditorWrapper
            field-name="Name"
            info="Character's name, will be included in the prompt"
        >
          <ShortTextBox
              v-if="model"
              v-model="name"
              @edit="txt => name = txt"
          ></ShortTextBox>
        </FieldEditorWrapper>
      </div>
      <!-- Tag editor -->
      <div v-if="model">
        <FieldEditorWrapper
            field-name="Tags"
            info="Character tags, write new ones to automatically create and link them"
            :vertical="true"
        >
          <TagAutocomplete
              v-if="characterTags"
              v-model="characterTags as Tag[]"
              @new-tag="handleNewTag"
              @remove-tag="handleRemoveTag"
          />
        </FieldEditorWrapper>
      </div>
    </div>
    <!-- Embed lorebook entry editor -->
    <Expandable
        title="Lorebook"
        info="A lorebook that will be activated if the character is present in the current location"
        :initially-open="false"
    >
      <LorebookEditor
          v-if="embed_lorebook"
          v-model="embed_lorebook"
      />
    </Expandable>
    <Expandable
        title="Starting locations"
        info="If a session is opened in this world, where this character may spawn"
    >
      <StartingLocation
        v-model:model-value="model"
      />
    </Expandable>
  </div>
</template>

<style scoped>
.all_fields_rows {
  display: flex;
  flex-direction: column;
  overflow: scroll;
}

.top_fields_flex {
  display: flex;
  flex-direction: column;
}
</style>