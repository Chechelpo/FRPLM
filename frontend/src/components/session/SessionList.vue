<script setup lang="ts">
import NewSessionPopUp from "@/components/session/NewSessionPopUp.vue";
import {computed, ref} from "vue";
import {Character} from "@/domain/Characters";
import {World} from "@/domain/World";
import {Session, SessionData, SessionKey} from "@/domain/Session";
import {computedAsync} from "@vueuse/core";
import {fetch_all} from "@/frameworks/ABSEntity";
import {EntityTypes} from "@/domain/EntityTypes";

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

function goToSession(session:Session):void{}
</script>

<template>
  <button
    class = max-h-10
    type="button"
    @click="createNewSession=true"> New
  </button>

  <NewSessionPopUp
    :model-value="createNewSession"
    @create-new-session="payload => newSession(payload.name, payload.world, payload.character)"
  />
</template>

<style scoped>

</style>