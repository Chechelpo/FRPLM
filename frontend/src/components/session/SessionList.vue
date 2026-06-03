<script setup lang="ts">
import NewSessionPopUp from "@/components/session/NewSessionPopUp.vue";
import {computed, ref, shallowRef} from "vue";
import {Character} from "@/domain/Characters";
import {World} from "@/domain/World";
import {Session, SessionData, SessionKey} from "@/domain/Session";
import {computedAsync} from "@vueuse/core";
import {deleteEntity, fetch_all} from "@/frameworks/ABSEntity";
import {EntityTypes} from "@/domain/EntityTypes";
import Chat from "@/components/chat/Chat.vue";
import SessionTease from "@/components/session/SessionTease.vue";

// Sessions : ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
const allSessions = computedAsync<Session[]>(async () => await fetch_all<SessionKey,SessionData,Session>(
    EntityTypes.SESSIONS,
    Session
), [])
const filteringTerm = ref<string>("");
const filteredSessions = computed<Session[]>(() => allSessions.value.filter(ses => ses.get('name').includes(filteringTerm)))

//New session: ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

const createNewSession =  ref<boolean>(false);
async function newSession(name:string, world:World, character:Character): Promise<void> {
  const newSession = await Session.newSession(name,world,character);
  goToSession(newSession);
}
//Open session: ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
const sessionSelected = shallowRef<Session | null>(null);
function goToSession(session:Session):void{
  sessionSelected.value = session;
}

async function deleteSession(session:Session) : Promise<void> {
  let deleted = await deleteEntity<SessionKey>(session.key, EntityTypes.SESSIONS);
  if (!deleted) {
    console.error("Error deleting session")
    return
  }
  allSessions.value = allSessions.value.filter(ses => !ses.equals(session))
}
</script>

<template>
  <button
    v-if="!sessionSelected"
    class = max-h-10
    type="button"
    @click="createNewSession=true"
  > New </button>
  <SessionTease
      v-if="allSessions && !sessionSelected"
      v-for="session in allSessions"
      :session="session"
      @open="goToSession"
      @delete="deleteSession"
  />
  <NewSessionPopUp
    :model-value="createNewSession"
    @create-new-session="payload => {
      newSession(payload.name, payload.world, payload.character);
      createNewSession = false
    }"
    @close="createNewSession = false"
  />

  <Chat
      v-if="sessionSelected"
      :key="sessionSelected.get('id')"
      :model-value="sessionSelected"
      @close="sessionSelected=null"
  />
</template>

<style scoped>

</style>