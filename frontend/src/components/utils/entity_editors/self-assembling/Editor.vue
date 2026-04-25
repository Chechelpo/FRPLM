<script setup lang="ts"
        generic="
    Keys extends KeyRecord,
    Data extends DataRecord,
    Ent extends EntityABS<Keys, Data>
  ">
import {KeyRecord, DataRecord, EntityABS} from "@/entities/EntityABS";
import FieldWrapper from "@/components/utils/entity_editors/fields/FieldWrapper.vue";
import {onMounted} from "vue";

const entity = defineModel<Ent>(
    "entity",
    {required: true}
);
onMounted(async () => {
  console.log(entity.value.getIterationArr())
  for (const name of entity.value.getIterationArr()){
    console.log(entity.value.getFieldInfoOf(name))
  }
})
</script>

<template>
  <div class="flex flex-col flex-1 min-h-0 divide-y divide-stone-800 overflow-y-auto">
    <div
        v-for="fieldName of entity.getIterationArr()"
        :key="entity.getFieldInfoOf(fieldName).presentation?.type"
        class="p-4"
    >
      <!-- Field editor -->
      <FieldWrapper
          :field-name="fieldName as string"
          :entity = "entity"
      />
    </div>
  </div>
</template>


<style scoped>
</style>