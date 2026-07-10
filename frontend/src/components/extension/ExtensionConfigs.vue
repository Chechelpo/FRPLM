<script setup lang="ts">
import { onMounted, ref } from "vue";
import { ExtensionDTO, fetchExtensions } from "@/extensions/extensions";
import Expandable from "@/components/utils/panels/Expandable.vue";
import {fetchApi} from "@/core/ABSEntity";
import {API_BASE} from "@/config";

const extensions = ref<ExtensionDTO[]>([]);
const loadedScripts = ref<Set<string>>(new Set());
const failedPanels = ref<Set<string>>(new Set());

async function loadPanelScript(ext: ExtensionDTO) {
  if (loadedScripts.value.has(ext.id) || failedPanels.value.has(ext.id)) {
    return;
  }

  const url = `http://localhost:8080/api/extensions/${encodeURIComponent(ext.id)}/panel.js`;

  try {
    const response = await fetch(url, {
      method: "GET",
      headers: {
        Accept: "application/javascript, text/javascript"
      }
    });

    const contentType = response.headers.get("content-type") ?? "";
    const source = await response.text();

    console.log("Panel response URL:", response.url);
    console.log("Panel response status:", response.status);
    console.log("Panel response content-type:", contentType);
    console.log("Panel response preview:", source.slice(0, 300));

    if (!response.ok || !contentType.includes("javascript")) {
      failedPanels.value.add(ext.id);
      return;
    }

    const blobUrl = URL.createObjectURL(
        new Blob([source], { type: "application/javascript" })
    );

    try {
      await import(/* @vite-ignore */ blobUrl);
      loadedScripts.value.add(ext.id);
    } finally {
      URL.revokeObjectURL(blobUrl);
    }
  } catch (error) {
    console.warn(`Extension ${ext.id} config panel failed to load.`, error);
    failedPanels.value.add(ext.id);
  }
}
onMounted(async () => {
  extensions.value = await fetchExtensions();
  for (const ext of extensions.value) {
    await loadPanelScript(ext);
  }
});
</script>

<template>
  <Expandable
      v-for="extension in extensions"
      :key="extension.id"
      :title="extension.displayName"
  >
    <!-- Render the Web Component if it loaded successfully -->
    <component
        v-if="loadedScripts.has(extension.id)"
        :is="`frplm-${extension.id}-panel`"
        :extension-id="extension.id"
    />

    <!-- Fallback UI if the panel failed to load -->
    <p v-else-if="failedPanels.has(extension.id)" style="color: #666; padding: 10px;">
      No configuration panel available.
    </p>

    <!-- Loading state -->
    <p v-else style="color: #666; padding: 10px;">
      Loading panel...
    </p>
  </Expandable>
</template>