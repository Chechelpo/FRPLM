<script setup lang="ts">
import { ref } from "vue";
import FieldEditorWrapper from "@/components/utils/FieldEditorWrapper.vue";

const props = defineProps<{
  title: string;
  info?: string;
  initiallyOpen?: boolean;
  disabled?: boolean;
}>();

const open = ref(props.initiallyOpen ?? false);

function toggle() {
  if (!props.disabled) {
    open.value = !open.value;
  }
}
</script>

<template>
  <div class="expandable">
    <div
        class="expandable-header"
        :class="{
        'expandable-header-disabled': disabled,
        'expandable-header-open': open
      }"
        @click="toggle"
    >
      <span
          class="expandable-chevron"
          :class="{ 'expandable-chevron-open': open }"
      >
        {{ open ? "▼" : "▶" }}
      </span>

      <FieldEditorWrapper
          :field-name="title"
          :info="info"
      />
    </div>

    <div
        v-show="open"
        class="expandable-content"
    >
      <slot />
    </div>
  </div>
</template>

<style scoped>
.expandable {
  display: flex;
  flex-direction: column;
}

.expandable-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;

  padding: 0.5rem 0.75rem;

  cursor: pointer;
  user-select: none;

  color: var(--primary-text, #e2e8f0);

  transition:
      background-color 120ms ease,
      color 120ms ease;
}

.expandable-header:hover {
  background-color: color-mix(
      in srgb,
      var(--primary-accent, #f59e0b) 10%,
      transparent
  );
}

.expandable-header-open {
  background-color: var(--secondary-background, #44403c);
}

.expandable-header-disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.expandable-chevron {
  width: 1rem;
  color: var(--muted-text, #64748b);
}

.expandable-chevron-open {
  color: var(--primary-accent, #f59e0b);
}

.expandable-content {
  padding: 0.75rem 1rem;
}
</style>