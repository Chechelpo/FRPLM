<script setup lang="ts">
import {
  computed,
  onMounted,
  onUnmounted,
  ref,
  watch,
} from "vue";

type PaneSide = "left" | "right";

type CollapsibleConfiguration =
    | boolean
    | {
  left?: boolean;
  right?: boolean;
};

const props = withDefaults(
    defineProps<{
      initialLeftWidth?: number;
      minLeftWidth?: number;
      minRightWidth?: number;

      /**
       * Base key used for width and collapsed-state persistence.
       */
      storageKey: string;

      /**
       * false:
       *   Neither pane can collapse.
       *
       * true:
       *   Both panes can collapse.
       *
       * { left: true }:
       *   Only the left pane can collapse.
       */
      collapsible?: CollapsibleConfiguration;

      /**
       * Initial collapsed pane, provided that pane is collapsible.
       */
      initialCollapsed?: PaneSide | null;

      /**
       * Persist collapsed state as `${storageKey}:collapsed`.
       */
      persistCollapsed?: boolean;
    }>(),
    {
      initialLeftWidth: 320,
      minLeftWidth: 200,
      minRightWidth: 100,
      collapsible: false,
      initialCollapsed: null,
      persistCollapsed: true,
    },
);

const emit = defineEmits<{
  (event: "resize", width: number): void;
  (
      event: "collapse-change",
      collapsedPane: PaneSide | null,
  ): void;
}>();

defineOptions({
  inheritAttrs: false,
});

const container = ref<HTMLElement | null>(null);
const divider = ref<HTMLElement | null>(null);

const leftWidth = ref(props.initialLeftWidth);
const collapsedPane = ref<PaneSide | null>(null);

let resizing = false;
let resizeObserver: ResizeObserver | null = null;

let previousBodyCursor = "";
let previousBodyUserSelect = "";

const collapseStorageKey = computed(
    () => `${props.storageKey}:collapsed`,
);

const canCollapseLeft = computed(
    () =>
        props.collapsible === true ||
        (
            typeof props.collapsible === "object" &&
            props.collapsible.left === true
        ),
);

const canCollapseRight = computed(
    () =>
        props.collapsible === true ||
        (
            typeof props.collapsible === "object" &&
            props.collapsible.right === true
        ),
);

const leftCollapsed = computed(
    () => collapsedPane.value === "left",
);

const rightCollapsed = computed(
    () => collapsedPane.value === "right",
);

const bothPanesVisible = computed(
    () => collapsedPane.value === null,
);

const leftPaneStyle = computed<Record<string, string>>(() => {
  /*
   * When the right pane is collapsed, the left pane stops using its
   * persisted fixed width and occupies the complete container.
   */
  if (rightCollapsed.value) {
    return {
      width: "100%",
      flex: "1 1 auto",
    };
  }

  return {
    width: `${leftWidth.value}px`,
    flex: `0 0 ${leftWidth.value}px`,
  };
});

function getStoredValue(key: string): string | null {
  if (typeof window === "undefined") {
    return null;
  }

  try {
    return window.localStorage.getItem(key);
  } catch {
    return null;
  }
}

function setStoredValue(
    key: string,
    value: string | null,
): void {
  if (typeof window === "undefined") {
    return;
  }

  try {
    if (value === null) {
      window.localStorage.removeItem(key);
    } else {
      window.localStorage.setItem(key, value);
    }
  } catch {
    // Persistence failure must not break the component.
  }
}

function getMaximumLeftWidth(): number {
  if (!container.value) {
    return Number.POSITIVE_INFINITY;
  }

  const dividerWidth = divider.value?.offsetWidth ?? 6;

  return Math.max(
      0,
      container.value.clientWidth -
      props.minRightWidth -
      dividerWidth,
  );
}

function clampLeftWidth(width: number): number {
  const maximumWidth = getMaximumLeftWidth();

  if (!Number.isFinite(maximumWidth)) {
    return Math.max(props.minLeftWidth, width);
  }

  /*
   * In containers narrower than minLeftWidth + minRightWidth,
   * the effective minimum must become smaller to avoid overflow.
   */
  const effectiveMinimum = Math.min(
      props.minLeftWidth,
      maximumWidth,
  );

  return Math.min(
      Math.max(width, effectiveMinimum),
      maximumWidth,
  );
}

function setLeftWidth(
    width: number,
    persist = false,
): void {
  const nextWidth = clampLeftWidth(width);

  if (nextWidth === leftWidth.value) {
    if (persist) {
      setStoredValue(props.storageKey, String(nextWidth));
    }

    return;
  }

  leftWidth.value = nextWidth;
  emit("resize", nextWidth);

  if (persist) {
    setStoredValue(props.storageKey, String(nextWidth));
  }
}

function startResize(event: PointerEvent): void {
  if (
      event.button !== 0 ||
      !bothPanesVisible.value
  ) {
    return;
  }

  event.preventDefault();

  resizing = true;

  previousBodyCursor = document.body.style.cursor;
  previousBodyUserSelect = document.body.style.userSelect;

  document.body.style.cursor = "col-resize";
  document.body.style.userSelect = "none";

  window.addEventListener("pointermove", onResize);
  window.addEventListener("pointerup", stopResize);
  window.addEventListener("pointercancel", stopResize);
}

function onResize(event: PointerEvent): void {
  if (!resizing || !container.value) {
    return;
  }

  const containerRect =
      container.value.getBoundingClientRect();

  const relativeX =
      event.clientX - containerRect.left;

  setLeftWidth(relativeX);
}

function stopResize(): void {
  if (!resizing) {
    return;
  }

  resizing = false;

  document.body.style.cursor = previousBodyCursor;
  document.body.style.userSelect = previousBodyUserSelect;

  window.removeEventListener("pointermove", onResize);
  window.removeEventListener("pointerup", stopResize);
  window.removeEventListener("pointercancel", stopResize);

  setStoredValue(
      props.storageKey,
      String(leftWidth.value),
  );
}

function isPaneCollapsible(side: PaneSide): boolean {
  return side === "left"
      ? canCollapseLeft.value
      : canCollapseRight.value;
}

function setCollapsedPane(
    side: PaneSide | null,
): void {
  if (side !== null && !isPaneCollapsible(side)) {
    return;
  }

  if (collapsedPane.value === side) {
    return;
  }

  stopResize();

  collapsedPane.value = side;
  emit("collapse-change", side);

  if (props.persistCollapsed) {
    setStoredValue(
        collapseStorageKey.value,
        side,
    );
  }
}

function collapseLeft(): void {
  setCollapsedPane("left");
}

function collapseRight(): void {
  setCollapsedPane("right");
}

function expandLeft(): void {
  if (leftCollapsed.value) {
    setCollapsedPane(null);
  }
}

function expandRight(): void {
  if (rightCollapsed.value) {
    setCollapsedPane(null);
  }
}

function toggleLeft(): void {
  leftCollapsed.value
      ? expandLeft()
      : collapseLeft();
}

function toggleRight(): void {
  rightCollapsed.value
      ? expandRight()
      : collapseRight();
}

function onDividerKeydown(event: KeyboardEvent): void {
  if (!bothPanesVisible.value) {
    return;
  }

  const step = event.shiftKey ? 50 : 10;

  switch (event.key) {
    case "ArrowLeft":
      event.preventDefault();
      setLeftWidth(leftWidth.value - step, true);
      break;

    case "ArrowRight":
      event.preventDefault();
      setLeftWidth(leftWidth.value + step, true);
      break;

    case "Home":
      event.preventDefault();
      setLeftWidth(props.minLeftWidth, true);
      break;

    case "End":
      event.preventDefault();
      setLeftWidth(
          getMaximumLeftWidth(),
          true,
      );
      break;
  }
}

function restorePersistedState(): void {
  const storedWidth = Number(
      getStoredValue(props.storageKey),
  );

  if (
      Number.isFinite(storedWidth) &&
      storedWidth > 0
  ) {
    leftWidth.value = storedWidth;
  }

  let requestedCollapsedPane =
      props.initialCollapsed;

  if (props.persistCollapsed) {
    const storedCollapsedPane = getStoredValue(
        collapseStorageKey.value,
    );

    if (
        storedCollapsedPane === "left" ||
        storedCollapsedPane === "right"
    ) {
      requestedCollapsedPane =
          storedCollapsedPane;
    }
  }

  if (
      requestedCollapsedPane &&
      isPaneCollapsible(requestedCollapsedPane)
  ) {
    collapsedPane.value =
        requestedCollapsedPane;
  } else {
    collapsedPane.value = null;
  }

  setLeftWidth(leftWidth.value, true);
}

watch(
    [canCollapseLeft, canCollapseRight],
    () => {
      const current = collapsedPane.value;

      if (
          current !== null &&
          !isPaneCollapsible(current)
      ) {
        setCollapsedPane(null);
      }
    },
);

onMounted(() => {
  restorePersistedState();

  if (
      container.value &&
      typeof ResizeObserver !== "undefined"
  ) {
    resizeObserver = new ResizeObserver(() => {
      /*
       * Re-clamp the stored width when the parent container changes
       * size, for example after a window resize.
       */
      setLeftWidth(leftWidth.value);
    });

    resizeObserver.observe(container.value);
  }
});

onUnmounted(() => {
  stopResize();

  resizeObserver?.disconnect();
  resizeObserver = null;
});

defineExpose({
  collapseLeft,
  collapseRight,
  expandLeft,
  expandRight,
  toggleLeft,
  toggleRight,
});
</script>

<template>
  <div
      ref="container"
      class="split-container"
      v-bind="$attrs"
  >
    <!-- LEFT PANE -->
    <div
        v-show="!leftCollapsed"
        class="split-pane left-pane"
        :class="{
        'split-pane--expanded': rightCollapsed,
      }"
        :style="leftPaneStyle"
        :aria-hidden="leftCollapsed"
    >
      <slot name="left" />
    </div>

    <!-- DIVIDER -->
    <div
        v-if="bothPanesVisible"
        ref="divider"
        class="divider"
        role="separator"
        aria-orientation="vertical"
        :aria-valuenow="Math.round(leftWidth)"
        :aria-valuemin="minLeftWidth"
        :aria-valuemax="Math.round(getMaximumLeftWidth())"
        tabindex="0"
        @pointerdown="startResize"
        @keydown="onDividerKeydown"
    >
      <div
          v-if="canCollapseLeft || canCollapseRight"
          class="divider-controls"
      >
        <button
            v-if="canCollapseLeft"
            type="button"
            class="pane-toggle"
            aria-label="Collapse left pane"
            title="Collapse left pane"
            @pointerdown.stop
            @click.stop="collapseLeft"
        >
          ‹
        </button>

        <button
            v-if="canCollapseRight"
            type="button"
            class="pane-toggle"
            aria-label="Collapse right pane"
            title="Collapse right pane"
            @pointerdown.stop
            @click.stop="collapseRight"
        >
          ›
        </button>
      </div>
    </div>

    <!-- RIGHT PANE -->
    <div
        v-show="!rightCollapsed"
        class="split-pane right-pane"
        :aria-hidden="rightCollapsed"
    >
      <slot name="right" />
    </div>

    <!-- EXPAND LEFT -->
    <button
        v-if="leftCollapsed"
        type="button"
        class="expand-button expand-button--left"
        aria-label="Expand left pane"
        title="Expand left pane"
        @click="expandLeft"
    >
      ›
    </button>

    <!-- EXPAND RIGHT -->
    <button
        v-if="rightCollapsed"
        type="button"
        class="expand-button expand-button--right"
        aria-label="Expand right pane"
        title="Expand right pane"
        @click="expandRight"
    >
      ‹
    </button>
  </div>
</template>

<style scoped>
.split-container {
  position: relative;
  display: flex;

  width: 100%;
  height: 100%;

  min-width: 0;
  min-height: 0;
}

.split-pane {
  min-width: 0;
  min-height: 0;
  overflow: auto;
}

.left-pane {
  /*
   * Width and flex-basis are assigned dynamically.
   * Do not hard-code min-width here because minLeftWidth is a prop.
   */
  flex-shrink: 0;
}

.right-pane {
  flex: 1 1 auto;
}

.divider {
  position: relative;
  z-index: 2;

  flex: 0 0 6px;
  width: 6px;

  cursor: col-resize;
  background-color: var(
      --split-divider-color,
      #ddd
  );

  touch-action: none;
  user-select: none;
}

.divider:hover,
.divider:focus-visible {
  background-color: var(
      --split-divider-active-color,
      #aaa
  );
}

.divider:focus-visible {
  outline: 2px solid
  var(--split-focus-color, #4b8df8);
  outline-offset: -1px;
}

.divider-controls {
  position: absolute;
  top: 8px;
  left: 50%;

  display: flex;
  flex-direction: column;
  gap: 4px;

  transform: translateX(-50%);
}

.pane-toggle,
.expand-button {
  display: grid;
  place-items: center;

  width: 22px;
  height: 22px;
  padding: 0;

  border: 1px solid
  var(--split-control-border, #bbb);
  border-radius: 4px;

  color: var(--split-control-text, #333);
  background: var(
      --split-control-background,
      #fff
  );

  font: inherit;
  font-size: 18px;
  line-height: 1;

  cursor: pointer;
}

.pane-toggle:hover,
.expand-button:hover {
  background: var(
      --split-control-hover-background,
      #eee
  );
}

.expand-button {
  position: absolute;
  top: 8px;
  z-index: 3;
}

.expand-button--left {
  left: 4px;
}

.expand-button--right {
  right: 4px;
}
</style>