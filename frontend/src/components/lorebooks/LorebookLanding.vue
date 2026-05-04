<script setup lang="ts">
import {computedAsync} from "@vueuse/core";
import {Lorebook, LorebookData, LorebookKey} from "@/domain/entities/Lorebook";
import {createEntity, fetch_all} from "@/domain/entities/EntityFetch";
import {EntityTypes} from "@/frameworks/entities/EntityTypes";
import SplitPanel from "@/components/utils/panels/SplitPanel.vue";
import List from "@/components/utils/list/List.vue";
import {computed, shallowRef} from "vue";
import FieldEditorWrapper from "@/components/utils/FieldEditorWrapper.vue";
import ShortTextBox from "@/components/utils/field-editors/ShortTextBox.vue";
import Expandable from "@/components/utils/panels/Expandable.vue";
import LorebookEditor from "@/components/lorebooks/LorebookEditor.vue";

const lorebooks = computedAsync<Lorebook[]>(
    async () => fetch_all<LorebookKey, LorebookData, Lorebook>(EntityTypes.LOREBOOKS, Lorebook)
)

const editingLorebook = shallowRef<Lorebook | null>(null);
const lorebookName = computed<string>({
  get() {
    return editingLorebook.value!.get('name')
  },
  set(newValue: string) {
    editingLorebook.value?.update('name', newValue)
  }
})

async function onCreate() {
  const name = window.prompt("Input new lorebook name")
  if (!name) return;

  const newLorebook = await createEntity<LorebookKey, LorebookData, Lorebook>(
      null,
      {
        name: name,
      },
      EntityTypes.LOREBOOKS,
      Lorebook
  )
  lorebooks.value!.push(newLorebook)
}

function onSelect(lorebook: Lorebook) {
  if (editingLorebook.value && editingLorebook.value.equals(lorebook)) {
    editingLorebook.value = null;
  }
  editingLorebook.value = lorebook;
}
</script>

<template>
  <SplitPanel
      storage-key="LorebooksLanding">
    <template #left>
      <List
          v-if="lorebooks"
          v-model:elements="lorebooks"
          @create="onCreate"
          @edit="loc => onSelect(loc)"
      />
    </template>
    <template #right v-if="editingLorebook">
      <div class = background-edit-box>
        <FieldEditorWrapper
            field-name="name"
            info="Lorebook's name, purely metadata"
        >
          <ShortTextBox
              :model-value="lorebookName"
              @edit="payload => lorebookName = payload"
          />
        </FieldEditorWrapper>
        <Expandable
            title="Entry editor">
          <LorebookEditor
            :model-value="editingLorebook"
          />
        </Expandable>
      </div>
    </template>
  </SplitPanel>
</template>

<style scoped>

</style>