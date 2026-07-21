<script setup lang="ts">
import {computedAsync} from "@vueuse/core";
import {Lorebook, LorebookData, LorebookKey} from "@/domain/Lorebook";
import {EntityTypes} from "@/domain/EntityTypes";
import SplitPanel from "@/components/utils/panels/SplitPanel.vue";
import List from "@/components/utils/list/List.vue";
import {computed, onMounted, ref, shallowRef} from "vue";
import FieldEditorWrapper from "@/components/utils/FieldEditorWrapper.vue";
import ShortTextBox from "@/components/primitive-editors/ShortTextBox.vue";
import Expandable from "@/components/utils/panels/Expandable.vue";
import LorebookEditor from "@/components/lorebooks/LorebookEditor.vue";
import {createEntity, deleteEntity, fetch_all} from "@/core/ABSEntity";

const lorebooks = ref<Lorebook[]>([])

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
async function onDelete(lorebook: Lorebook) {
  const confirm = window.confirm("Are you sure you want to delete this lorebook?");
  if (!confirm) return;
  if (await deleteEntity<LorebookKey>({id:lorebook.get('id')}, EntityTypes.LOREBOOKS))
    lorebooks.value = lorebooks.value!.filter(other => !other.equals(lorebook));
}
onMounted(async () => {
  lorebooks.value = await fetch_all<LorebookKey, LorebookData, Lorebook>(EntityTypes.LOREBOOKS, Lorebook)
})
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
          @remove="loc => onDelete(loc)"
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