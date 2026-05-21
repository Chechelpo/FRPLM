<script setup lang="ts">
import {Session} from "@/domain/Session";

const props = defineProps<{
  session:Session
}>();
const emit = defineEmits<{
  (e:'open' , value:Session): void,
  (e:'delete', value:Session): void
}>();
interface SessionStats{
  messageNumber: number;
}

</script>

<template>

</template>

<style scoped>

</style>