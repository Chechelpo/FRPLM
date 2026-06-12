<script setup lang="ts">
import {Session} from "@/domain/Session";
import {onMounted, ref, shallowRef} from "vue";
import {PromptTemplate} from "@/domain/Prompts";
import Expandable from "@/components/utils/panels/Expandable.vue";
import PromptTemplateEditor from "@/components/prompts/PromptTemplateEditor.vue";
import SingleEnumInput from "@/components/utils/primitives/SingleEnumInput.vue";
import WorldEdit from "@/components/space/WorldEdit.vue";
import {World} from "@/domain/World";
import Configs from "@/components/extension/Configs.vue";

const model = defineModel<Session>({required:true, type:Session});

const world = ref<World>();
const template = ref<PromptTemplate | null>(null);
const allTemplates = ref<PromptTemplate[]>([]);

async function selectNewTemplate(selectedTemplate: PromptTemplate) : Promise<void> {
  await model.value.update('template_id', selectedTemplate.get('id'));
  template.value = selectedTemplate;
}

onMounted(async () => {
  template.value = await model.value.getTemplate();
  allTemplates.value = await PromptTemplate.getAll();
  world.value = await model.value.getWorld();
})
</script>

<template>
  <div>
    <Expandable title="prompt">
        <SingleEnumInput
            :value="template? template.get('name') : '' "
            :possible_values="allTemplates.map(temp => temp.get('name'))"
            @edit = "value => selectNewTemplate(allTemplates.find(temp => temp.get('name') == value )! as PromptTemplate) "
        />
      <PromptTemplateEditor v-if="template" :key="template.get('id')" :model-value="template as PromptTemplate"/>
      <div v-if="!template"> No template configured for this session</div>
    </Expandable>
    <Expandable title="world">
      <WorldEdit v-if="world" :model-value="world"/>
    </Expandable>
    <Configs/>
  </div>
</template>

<style scoped>

</style>