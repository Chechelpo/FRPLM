<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from "vue";

const props = withDefaults(
    defineProps<{
      storageKey?: string;

      expanded?: boolean;
      defaultExpanded?: boolean;

      initialLeftWidth?: number;
      minLeftWidth?: number;
      maxLeftWidth?: number;

      collapsedLeftWidth?: number;
      collapseToZero?: boolean;
    }>(),
    {
      defaultExpanded: true,
      initialLeftWidth: 320,
      minLeftWidth: 200,
      maxLeftWidth: 700,
      collapsedLeftWidth: 48,
      collapseToZero: false,
    }
);

const emit = defineEmits<{
  "update:expanded": [value: boolean];
  "resize": [width: number];
  "toggle": [expanded: boolean];
}>();

const container = ref<HTMLElement | null>(null);

const internalExpanded = ref(props.defaultExpanded);
const isControlled = computed(() => props.expanded !== undefined);

const isExpanded = computed<boolean>({
  get() {
    return isControlled.value ? props.expanded! : internalExpanded.value;
  },
  set(value: boolean) {
    if (!isControlled.value) {
      internalExpanded.value = value;
    }

    emit("update:expanded", value);
    emit("toggle", value);
  },
});

function loadInitialWidth(): number {
  if (!props.storageKey) {
    return props.initialLeftWidth;
  }

  const stored = Number(localStorage.getItem(props.storageKey));

  if (!Number.isFinite(stored) || stored <= 0) {
    return props.initialLeftWidth;
  }

  return stored;
}

const leftWidth = ref(loadInitialWidth());

const renderedLeftWidth = computed<number>(() => {
  if (isExpanded.value) {
    return leftWidth.value;
  }

  return props.collapseToZero ? 0 : props.collapsedLeftWidth;
});

let resizing = false;

function clampWidth(width: number): number {
  return Math.min(
      props.maxLeftWidth,
      Math.max(props.minLeftWidth, width)
  );
}

function persistWidth(width: number): void {
  if (props.storageKey) {
    localStorage.setItem(props.storageKey, String(width));
  }
}

function startResize(event: MouseEvent): void {
  if (!isExpanded.value) return;

  event.preventDefault();

  resizing = true;
  window.addEventListener("mousemove", onResize);
  window.addEventListener("mouseup", stopResize);
}

function onResize(event: MouseEvent): void {
  if (!resizing || !container.value) return;

  const rect = container.value.getBoundingClientRect();
  const rawWidth = event.clientX - rect.left;
  const nextWidth = clampWidth(rawWidth);

  leftWidth.value = nextWidth;
  persistWidth(nextWidth);
  emit("resize", nextWidth);
}

function stopResize(): void {
  resizing = false;

  window.removeEventListener("mousemove", onResize);
  window.removeEventListener("mouseup", stopResize);
}

function toggleLeft(): void {
  isExpanded.value = !isExpanded.value;
}

function expandLeft(): void {
  isExpanded.value = true;
}

function collapseLeft(): void {
  isExpanded.value = false;
}

watch(
    () => props.expanded,
    value => {
      if (value !== undefined) {
        internalExpanded.value = value;
      }
    }
);

onMounted(() => {
  leftWidth.value = clampWidth(leftWidth.value);
});

onUnmounted(() => {
  stopResize();
});

defineExpose({
  toggleLeft,
  expandLeft,
  collapseLeft,
});
</script>

<template>
  <section ref="container" class="expandable-split-panel">
    <aside
        class="expandable-split-panel__left"
        :class="{ 'is-collapsed': !isExpanded }"
        :style="{ width: `${renderedLeftWidth}px` }"
    >
      <button
          type="button"
          class="expandable-split-panel__toggle"
          :aria-expanded="isExpanded"
          @click="toggleLeft"
      >
        {{ isExpanded ? "◀" : "▶" }}
      </button>

      <div
          v-show="isExpanded || !collapseToZero"
          class="expandable-split-panel__left-content"
      >
        <slot
            name="left"
            :expanded="isExpanded"
            :toggle="toggleLeft"
            :expand="expandLeft"
            :collapse="collapseLeft"
        />
      </div>
    </aside>

    <div
        class="expandable-split-panel__divider"
        :class="{ 'is-disabled': !isExpanded }"
        @mousedown="startResize"
    />

    <main class="expandable-split-panel__right">
      <slot
          name="right"
          :expanded="isExpanded"
          :toggle="toggleLeft"
          :expand="expandLeft"
          :collapse="collapseLeft"
      />
    </main>
  </section>
</template>

<style scoped>
.expandable-split-panel {
  display: flex;
  width: 100%;
  height: 100%;
  min-height: 0;
  overflow: hidden;

  color: var(--primary-text, #e2e8f0);
  background: var(--primary-background, transparent);
}

.expandable-split-panel__left {
  position: relative;

  height: 100%;
  min-width: 0;
  min-height: 0;

  display: flex;
  flex-direction: column;

  overflow: hidden;

  border-right: 1px solid var(--primary-accent, #f59e0b);
  background: var(--primary, transparent);

  transition: width 160ms ease;
}

.expandable-split-panel__left.is-collapsed {
  background: var(--secondary-background, #44403c);
}

.expandable-split-panel__left-content {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.expandable-split-panel__toggle {
  height: 32px;
  width: 100%;

  border: none;
  border-bottom: 1px solid var(--primary-accent, #f59e0b);

  background: var(--secondary-background, transparent);
  color: var(--primary-text, #e2e8f0);

  cursor: pointer;
  user-select: none;

  transition:
      background-color 120ms ease,
      color 120ms ease,
      opacity 120ms ease;
}

.expandable-split-panel__toggle:hover {
  background: color-mix(
      in srgb,
      var(--primary-accent, #f59e0b) 18%,
      var(--secondary-background, transparent)
  );
}

.expandable-split-panel__divider {
  width: 6px;
  flex: 0 0 6px;

  cursor: col-resize;
  user-select: none;

  background: var(--primary-accent, #f59e0b);
  opacity: 0.55;

  transition:
      opacity 120ms ease,
      background-color 120ms ease;
}

.expandable-split-panel__divider:hover {
  opacity: 1;
}

.expandable-split-panel__divider.is-disabled {
  cursor: default;
  opacity: 0.25;
}

.expandable-split-panel__right {
  flex: 1;
  min-width: 0;
  min-height: 0;
  height: 100%;

  overflow: auto;

  background: var(--primary-background, transparent);
  color: var(--primary-text, #e2e8f0);
}
</style>