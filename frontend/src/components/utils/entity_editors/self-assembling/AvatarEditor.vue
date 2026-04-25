<script setup lang="ts"
        generic="
    Ent extends EntityABS<any, any>
  ">
import {ref, onMounted, computed, toRef} from "vue";
import Editor from "@/components/utils/entity_editors/self-assembling/Editor.vue";
import {EntityABS} from "@/entities/EntityABS";

const entity = defineModel<Ent>(
    "entity",
    {
      required: true
    });
/**
 * FieldInfo for explicitly handled fields (name)
 */

async function loadNameFieldInfo() {
  /*
  try {
    fieldInfo.value[NAME_FIELD] = await fetchFieldInfo(
        element.value,
        NAME_FIELD
    );
  } catch {
    // name not editable → ignore
  }*/
}

async function saveName(value: unknown): Promise<void> {
  if (typeof value !== "string")
    throw new Error("Invalid value for name");


  /*
  element.value.name = value;

  const response = await fetch(
      `${element.value.url}/${element.value.id}`,
      {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name: value }),
      }
  );

  if (!response.ok) {
    throw new Error(`Failed to patch name`);
  }*/
}

onMounted(loadNameFieldInfo);
</script>



<template>
  <div class="bg-stone-950 p-6">
    <div
        class="mx-auto h-full w-full rounded-xl
             border border-stone-800 bg-stone-900/85
             shadow-lg shadow-stone-950/40 overflow-hidden"
    >
      <!-- Header: Avatar + Name -->
      <div class="flex items-center gap-4 p-4 border-b border-stone-800">
        <!-- Avatar -->
        <div class="shrink-0 w-20 h-20">
          <!--
          <AvatarDisplay
              class="w-full h-full"
              :owner="element"
              :editable="true"
          />
          -->
        </div>

        <!-- Name field (explicit, not generic) -->
        <div class="flex-grow">
          <!--
          <component
              v-if="nameFieldInfo"
              :is="resolveFieldComponent(nameFieldInfo!)"
              :modelValue="element.name"
              :fieldName="NAME_FIELD"
              :fieldInfo="nameFieldInfo!"
              @edit="saveName($event.value)"
          />
          -->
        </div>
      </div>

      <!-- Generic Object Editor -->
      <Editor
          v-model:entity="entity"
      />
    </div>
  </div>
</template>

