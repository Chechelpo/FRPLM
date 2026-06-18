<script setup lang="ts">
import { onMounted, ref } from "vue";
import {ExtensionDTO, fetchConfigPanel, fetchExtensions} from "@/extensions/extensions";
import Expandable from "@/components/utils/panels/Expandable.vue";

const extensions = ref<ExtensionDTO[]>([]);
const configPanels = ref<Record<string, string>>({});


onMounted(async () => {
  extensions.value = await fetchExtensions();

  for (const extension of extensions.value) {
    const html = await fetchConfigPanel(extension.id);

    if (html !== null) {
      configPanels.value[extension.id] = html;
    }
  }
});
</script>

<template>
  <Expandable
      v-for="extension in extensions"
      :key="extension.id"
      :title="extension.displayName"
  >
    <iframe
        v-if="configPanels[extension.id]"
        class="extension-config-frame"
        :src="configPanels[extension.id]"
    />

    <p v-else>
      No configuration panel available.
    </p>
  </Expandable>
</template>

<style scoped>
</style>