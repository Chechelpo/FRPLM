<script setup lang="ts">
import {computedAsync} from "@vueuse/core";
import {LLMBackends, LLMConnection, LLMConnectionData, LLMConnectionKeys} from "@/domain/entities/Connection";
import {createEntity, deleteEntity, fetch_all} from "@/domain/entities/EntityFetch";
import {EntityTypes} from "@/frameworks/entities/EntityTypes";
import SplitPanel from "@/components/utils/panels/SplitPanel.vue";
import List from "@/components/utils/list/List.vue";
import {onMounted, ref} from "vue";
import LLMEditor from "@/components/connections/LLMEditor.vue";

const allConnections = ref<LLMConnection[]>()
const editingConnection = ref<LLMConnection | null>(null);

function edit(value:LLMConnection){
  editingConnection.value = value;
}
async function onCreate(){
  const name = window.prompt("Enter name")
  if (!name) return;
  const newEnt = await createEntity<LLMConnectionKeys,LLMConnectionData,LLMConnection>(
      null,
      {
        name:name,
        type:LLMBackends.NANOGPT.id // Default type
      },
      EntityTypes.LLM,
      LLMConnection)
  await reload()
}

async function deleteConnection(connection:LLMConnection){
  const confirmation = window.confirm(`Are you sure you want to delete ${connection.get('name')}`);
  if (!confirmation) return;
  await deleteEntity<LLMConnectionKeys>({id:connection.get('id')}, EntityTypes.LLM)
  await reload()
}
onMounted(() => reload())
async function reload(){
  allConnections.value = await fetch_all<LLMConnectionKeys,LLMConnectionData,LLMConnection>(
      EntityTypes.LLM,
      LLMConnection
  )
}
</script>

<template>
  <SplitPanel storage-key="LLMConnectionsOuter">
    <template #left >
      <List
        v-if="allConnections"
        :elements="allConnections"
        @edit="i => edit(i!)"
        @create ="onCreate"
        @remove="deleteConnection"
      />
    </template>
    <template #right>
      <LLMEditor
        v-if="editingConnection"
        v-model="editingConnection!"
      />
    </template>
  </SplitPanel>
</template>

<style scoped>

</style>