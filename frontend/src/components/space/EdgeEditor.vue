<script setup lang="ts">

import {Location, LocationEdge} from "@/domain/World";

const props = defineProps<{
  fromLocation : Location;
  toLocation : Location;
  edgeInfo : LocationEdge;
}>();

const emits = defineEmits<{
  (e:'delete', value: {from:Location, to:Location} ): void
}>();

</script>

<template>

</template>

<style scoped>

</style>