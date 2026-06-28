<script setup lang="ts">

import {Region} from "@/domain/World";
import {ref} from "vue";

const props = defineProps<{
  region:Region
}>();

const childrenRegions = ref<Region[]>([]);
const childrenLocation = ref<Location[]>([]);

</script>

<template>

</template>

<style scoped>

</style>