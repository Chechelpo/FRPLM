<script setup lang="ts">

import Expandable from "@/components/utils/panels/Expandable.vue";
import ThemeSettings from "@/components/settings/ThemeSettings.vue";
import PrologValidator from "@/components/utils/PrologValidator.vue";
</script>

<template>
  <Expandable title="Appearance" variant="card">
    <ThemeSettings/>
  </Expandable>
  <PrologValidator/>
</template>

<style scoped>

</style>