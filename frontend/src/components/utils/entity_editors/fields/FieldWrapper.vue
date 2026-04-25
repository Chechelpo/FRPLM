Field Wrapper vue component
  - Decides what input box is apropiate for an entity field
  - Wraps an input box.
  - Emits edit after checking for constraints

<script
    setup lang="ts"
    generic = "Key extends KeyRecord, Data extends DataRecord"
>

import {EntityABS} from "@/entities/EntityABS";
import {DataRecord, EntityField, KeyRecord, Primitives} from "@/types/DTOs/DTOs";
import {computed, ref} from "vue";
import {getEditorForField} from "@/components/utils/entity_editors/field";
import {FieldTypes, PlaceHolder} from "@/types/FieldMetadata";

const props = defineProps<{
  fieldName:EntityField<Key, Data>;
  entity:EntityABS<Key,Data>;
}>();

const fieldInfo = computed(() => {
  let a = props.entity.getFieldInfoOf(props.fieldName)
  if (a.presentation == null)
    throw new Error(`Unpresentable field: ${String(props.fieldName)} of entity ${props.entity}`)
  return a
})

const fieldType = ref(fieldInfo.value.presentation?.type);
const violatedConstraints = ref<boolean>(false);
//Shows only one constraint violated at a time.
const constraintMessage = ref<string>("If you see this, something went wrong with the constraint managing of FieldEditor")

function violates_constraints(value: Primitives): boolean {
  const constraints = fieldInfo.value.constraints;
  if (constraints == null) { //Value is always correct if no constraints
    violatedConstraints.value = false;
    return false;
  }
  if (constraints.read_only) {
    setConstraint(true, "Read only field");
    throw new Error("Somehow the user edited a read only field");
  }

  //Cases in which max and min hold value
  if(fieldType.value == FieldTypes.NUMBER || fieldType.value == FieldTypes.FLOAT ){
    let a = value as number;
    if (a > constraints.max){
      setConstraint(true, `Number value too large, max is ${constraints.max}`)
      return true;
    }
    if (a < constraints.min){
      setConstraint(true, `Number value too small, min is ${constraints.min}`)
      return true;
    }
  }

  if (fieldType.value == FieldTypes.STRING){
    let stringValue = value as string;
    if(stringValue.length > constraints.max){
      setConstraint(true, `String value too long, max is ${constraints.max}`)
      return true;
    }
    if (stringValue.length < constraints.min){
      setConstraint(true, `String value too short, min is ${constraints.min}`)
      return true;
    }
    if(containsForbiddenPlaceholder(stringValue, constraints.allowed_placeholders)){
      setConstraint(true, `String contains forbidden placeholders. Allowed are: ${constraints.allowed_placeholders}`)
      return true;
    }
  }

  setConstraint(false, "No error") //Valid state
  return false;
}

function containsForbiddenPlaceholder(
    content: string,
    allowedPlaceholders: PlaceHolder[]
): boolean {
  const regex = /\{\{(.*?)\}\}/g;

  const allowed = new Set<string>(allowedPlaceholders.map(String));

  for (const match of content.matchAll(regex)) {
    const placeholder = match[1];

    if (!allowed.has(placeholder)) {
      return true;
    }
  }

  return false;
}

function setConstraint(status:boolean, message:string): void {
  violatedConstraints.value = status;
  constraintMessage.value = message;
}

function saveEdit(newValue:Primitives){
  if (!violates_constraints(newValue)){
    props.entity.update(props.fieldName, newValue) //Dont care for TS error
  }
}
</script>

<template>
<div class = "editor-container">
  <div class = "fieldName">
  {{fieldName}}
  </div>
  <!--- TODO: Constraints pop up message
  <div> </div>
  -->
  <component
      :is="getEditorForField(entity, String(fieldName))"
      :entity="entity"
      :fieldName="fieldName"
      :initial_value="entity.get(fieldName)"
      :read-only="entity.getFieldInfoOf(fieldName).constraints?.read_only"
      @edit="saveEdit($event.value)"
  >
  </component>
</div>
</template>

<style scoped>
.editor-container{
  display: flex;
  flex-direction: column;
}
.fieldName{
  width:100%;
  height: 100%;
}
</style>