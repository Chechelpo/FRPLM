<script setup lang="ts">
import { ref, onMounted, onUnmounted } from "vue";

/**
 * Pure UI split panel with persistent width.
 */

const props = defineProps<{
  initialLeftWidth?: number;
  minLeftWidth?: number;
  storageKey: string; // REQUIRED for persistence
}>();
defineOptions({ inheritAttrs: false })

const leftWidth = ref<number>(
    Number(localStorage.getItem(props.storageKey)) ||
    props.initialLeftWidth ||
    320
);

const minWidth = props.minLeftWidth ?? 200;
const container = ref<HTMLElement | null>(null);

let resizing = false;

function startResize(e: MouseEvent): void {
  e.preventDefault();
  resizing = true;
  window.addEventListener("mousemove", onResize);
  window.addEventListener("mouseup", stopResize);
}

function onResize(e: MouseEvent): void {
  if (!resizing || !container.value) return;

  const rect = container.value.getBoundingClientRect();
  const relativeX = e.clientX - rect.left;
  const width = Math.max(minWidth, relativeX);

  leftWidth.value = width;
  localStorage.setItem(props.storageKey, String(width));
}

function stopResize(): void {
  resizing = false;
  window.removeEventListener("mousemove", onResize);
  window.removeEventListener("mouseup", stopResize);
}

onMounted(() => {
  if (!container.value) return;

  // Clamp width to container size on mount
  const maxWidth = container.value.clientWidth - 100;
  leftWidth.value = Math.min(leftWidth.value, maxWidth);
});

onUnmounted(() => {
  window.removeEventListener("mousemove", onResize);
  window.removeEventListener("mouseup", stopResize);
});
</script>

<template>
  <div
      ref="container"
      class="split-container"
      :class="$attrs.class"
  >
    <!-- LEFT -->
    <div
        class="left-pane"
        :style="{ width: leftWidth + 'px' }"
    >
      <slot name="left" />
    </div>

    <!-- DIVIDER -->
    <div
        class="divider"
        @mousedown="startResize"
    />

    <!-- RIGHT -->
    <div class="right-pane">
      <slot name="right" />
    </div>
  </div>
</template>

<style scoped>
.split-container {
  display: flex;
  width: 100%;
  height: 100%;   /* critical: take the parent's height */
  min-height: 0;  /* critical: allow nested shrink/scroll */
}

.left-pane {
  min-width: 200px;
  min-height: 0;
  overflow: auto;
}

.right-pane {
  flex: 1;
  min-width: 0;   /* critical: prevents right pane collapse in flex rows */
  min-height: 0;
  overflow: auto;
}

.divider {
  width: 6px;
  cursor: col-resize;
  background-color: #ddd;
  user-select: none;
}

</style>
