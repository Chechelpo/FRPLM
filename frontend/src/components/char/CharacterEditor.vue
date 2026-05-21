<script setup lang="ts">
import {Character} from "@/domain/Characters";
import ShortTextBox from "@/components/utils/primitives/ShortTextBox.vue";
import {computed, onMounted, ref, shallowRef, watch} from "vue";
import {Lorebook} from "@/domain/Lorebook";
import LorebookEditor from "@/components/lorebooks/LorebookEditor.vue";
import TagAutocomplete from "@/components/tags/TagAutocomplete.vue";
import {Tag} from "@/domain/Tag";
import FieldEditorWrapper from "@/components/utils/FieldEditorWrapper.vue";
import Expandable from "@/components/utils/panels/Expandable.vue";
import StartingLocation from "@/components/char/StartingLocation.vue";
import BooleanToggle from "@/components/utils/primitives/BooleanToggle.vue";
import LongTextBox from "@/components/utils/primitives/LongTextBox.vue";

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
const can_be_user = computed<boolean>({
  get(){
    return model.value.get('can_be_user');
  },
  set(value: boolean) {
    model.value.update('can_be_user', value);
  }
});

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
        <FieldEditorWrapper field-name="CanBeUser" info="Whether this character can be played by user">
          <BooleanToggle
              :model-value="can_be_user"
              @edit="value => can_be_user = value"
          />
        </FieldEditorWrapper>

        <FieldEditorWrapper
            v-if = "can_be_user"
            field-name="FirstMessage"
        >
          <LongTextBox
              :model-value="model.get('firstMessage')"
              @edit="payload => model.update('firstMessage', payload)"
          />
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
        :key="model.get('id')"
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