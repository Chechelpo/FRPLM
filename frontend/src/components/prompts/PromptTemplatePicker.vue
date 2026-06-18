!! Intermediate selector for template editing
<script setup lang="ts">

import {computedAsync} from "@vueuse/core";
import {PromptTemplate, PromptTemplateKey, PromptTemplateData} from "@/domain/Prompts";
import {computed, ref} from "vue";
import {createEntity, fetch_all} from "@/frameworks/ABSEntity";
import {EntityTypes} from "@/domain/EntityTypes";
import SingleEnumInput from "@/components/utils/primitiveEditors/SingleEnumInput.vue";
import PromptTemplateEditor from "@/components/prompts/PromptTemplateEditor.vue";

const allPrompts = computedAsync<PromptTemplate[]>(async () => await fetch_all<PromptTemplateKey,PromptTemplateData,PromptTemplate>(
        EntityTypes.TEMPLATES,
        PromptTemplate
    ));
const allPromptsNames = computed<string[] | undefined>(() => allPrompts.value?.map(prompt => prompt.get('name')))

const editPrompt = ref<PromptTemplate | null>(null);

async function createTemplate() {
  const name = window.prompt('new template name');
  if (!name) return;
  await createEntity<PromptTemplateKey,PromptTemplateData, PromptTemplate>(null,
      {
        name:name
      },
      EntityTypes.TEMPLATES,
      PromptTemplate
  ).then(newPrompt => {
    allPrompts.value!.push(newPrompt)
    editPrompt.value = newPrompt
  });
}
</script>

<template>
  <div class = "flex flex-col">
    <div class = "max-h-5">
      <SingleEnumInput
          v-if="allPrompts"
          :value="editPrompt ? editPrompt.get('name') : '' "
          :possible_values="allPromptsNames!"
          @edit = "value => editPrompt=allPrompts!.find(prompt => prompt.get('name') == value)!"
      />
      <button
        type="button"
        @click="createTemplate"
      >
        new
      </button>
    </div>
    <PromptTemplateEditor
        v-if="editPrompt"
        :model-value="editPrompt!"
    />
  </div>
</template>

<style scoped>

</style>