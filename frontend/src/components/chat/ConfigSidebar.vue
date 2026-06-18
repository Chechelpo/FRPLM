<script setup lang="ts">
import {Session} from "@/domain/Session";
import {onMounted, ref, shallowRef} from "vue";
import {PromptTemplate} from "@/domain/Prompts";
import Expandable from "@/components/utils/panels/Expandable.vue";
import PromptTemplateEditor from "@/components/prompts/PromptTemplateEditor.vue";
import SingleEnumInput from "@/components/utils/primitiveEditors/SingleEnumInput.vue";
import WorldEdit from "@/components/space/WorldEdit.vue";
import {World} from "@/domain/World";
import ExtensionConfigs from "@/components/extension/ExtensionConfigs.vue";

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
  <div class="config-item-container">
    <Expandable title="prompt" class = expandable>
        <SingleEnumInput
            :value="template? template.get('name') : '' "
            :possible_values="allTemplates.map(temp => temp.get('name'))"
            @edit = "value => selectNewTemplate(allTemplates.find(temp => temp.get('name') == value )! as PromptTemplate) "
        />
      <PromptTemplateEditor v-if="template" :key="template.get('id')" :model-value="template as PromptTemplate"/>
      <div v-if="!template"> No template configured for this session</div>
    </Expandable>
    <Expandable title="world" class = expandable>
      <WorldEdit v-if="world" :model-value="world"/>
    </Expandable>
    <!--
    <div class = expandable>
      <ExtensionConfigs/>
    </div>
    -->
  </div>
</template>

<style scoped>
.expandable {
  background-color: var(--primary-background);
}
.config-item-container {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
</style>