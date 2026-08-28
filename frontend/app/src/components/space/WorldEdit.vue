<script setup lang="ts">
import {     LongTextBox,
    ShortTextBox } from "@frplm/ui";

import {     Lorebook,
    fetchApi } from "@frplm/host-sdk";

import {
  computed,
  onBeforeUnmount,
  onMounted,
  ref,
  shallowRef,
  watch,
} from "vue";

import {
  Location,
  Region,
  World,
} from "@frplm/host-sdk";

import LorebookEditor from "@components/lorebooks/LorebookEditor.vue";
import LocationEditor from "@components/space/LocationEditor.vue";
import LocationEdgesEditor from "@components/space/LocationEdgesEditor.vue";
import RegionEditor from "@components/space/RegionEditor.vue";
import {
  WorldEditGraph,
  type GraphEntityEditIntent,
} from "@components/space/world_graph";
import IconButton from "@components/utils/buttons/IconButton.vue";
import FieldEditorWrapper from "@components/utils/FieldEditorWrapper.vue";
import Expandable from "@components/utils/panels/Expandable.vue";
import WindowPrompt from "@components/utils/prompts/WindowPrompt.vue";

const model = defineModel<World>({
  required: true,
  type: World,
});
const emit = defineEmits<{
  (e:"back"): void
}>()

type EditorTarget =
    | {
  kind: "location";
  location: Location;
}
    | {
  kind: "region";
  region: Region;
}
    | {
  kind: "connection";
  first: Location;
  second: Location;
};

const DEFAULT_INSPECTOR_WIDTH = 420;
const MIN_INSPECTOR_WIDTH = 320;
const MAX_INSPECTOR_WIDTH = 1000;
const MIN_GRAPH_WIDTH = 500;
const MOBILE_BREAKPOINT = 840;

const rootElement =
    ref<HTMLElement | null>(null);

const editorTarget =
    shallowRef<EditorTarget | null>(null);

const lorebook =
    shallowRef<Lorebook | null>(null);

const connectionRefreshToken = ref(0);

const loadingLorebook = ref(false);
const lorebookError =
    ref<string | null>(null);

const showWorldInformation = ref(false);

const exportingWorld = ref(false);
const exportError =
    ref<string | null>(null);

const inspectorWidth =
    ref(DEFAULT_INSPECTOR_WIDTH);

const resizingInspector = ref(false);

let lorebookRequestId = 0;

let stopInspectorResize:
    (() => void) | null = null;

const worldName = computed(() => {
  const name = String(
      model.value.get("name") ?? "",
  ).trim();

  return name || "Unnamed world";
});

const inspectorTitle = computed(() => {
  const target = editorTarget.value;

  if (target === null) {
    return "";
  }

  if (target.kind === "location") {
    return String(
        target.location.get("name") ??
        "Unnamed location",
    );
  }

  if (target.kind === "region") {
    return String(
        target.region.get("name") ??
        "Unnamed region",
    );
  }

  const firstName = String(
      target.first.get("name") ??
      "Unnamed location",
  );

  const secondName = String(
      target.second.get("name") ??
      "Unnamed location",
  );

  return `${firstName} ↔ ${secondName}`;
});

const inspectorKindLabel = computed(() => {
  const target = editorTarget.value;

  if (target?.kind === "location") {
    return "Location";
  }

  if (target?.kind === "region") {
    return "Region";
  }

  if (target?.kind === "connection") {
    return "Connection";
  }

  return "";
});
const workspaceClass = computed(() => ({
  "world-edit__workspace--inspector-open":
      editorTarget.value !== null,

  "world-edit__workspace--resizing":
  resizingInspector.value,
}));

const rootStyle = computed(() => ({
  "--inspector-width":
      `${inspectorWidth.value}px`,
}));

function sameLocation(
    first: Location,
    second: Location,
): boolean {
  return first.equals(second);
}

function sameRegion(
    first: Region,
    second: Region,
): boolean {
  return first.equals(second);
}

function sameUnorderedPair(
    first: Location,
    second: Location,
    otherFirst: Location,
    otherSecond: Location,
): boolean {
  return (
      (
          sameLocation(first, otherFirst) &&
          sameLocation(second, otherSecond)
      ) ||
      (
          sameLocation(first, otherSecond) &&
          sameLocation(second, otherFirst)
      )
  );
}

function openLocationEditor(
    location: Location,
    intent: GraphEntityEditIntent = "open",
): void {
  const current = editorTarget.value;
  if (
      intent === "toggle-if-current" &&
      current?.kind === "location" &&
      sameLocation(current.location, location)
  ) {
    closeInspector();
    return;
  }

  editorTarget.value = {
    kind: "location",
    location,
  };
}

function openRegionEditor(
    region: Region,
    intent: GraphEntityEditIntent = "open",
): void {
  const current = editorTarget.value;
  if (
      intent === "toggle-if-current" &&
      current?.kind === "region" &&
      sameRegion(current.region, region)
  ) {
    closeInspector();
    return;
  }

  editorTarget.value = {
    kind: "region",
    region,
  };
}

function openConnectionEditor(
    first: Location,
    second: Location,
): void {
  editorTarget.value = {
    kind: "connection",
    first,
    second,
  };

  connectionRefreshToken.value += 1;
}

function closeInspector(): void {
  editorTarget.value = null;
}

function openWorldInformation(): void {
  showWorldInformation.value = true;
}

function closeWorldInformation(): void {
  showWorldInformation.value = false;
}

function onLocationDeleted(
    location: Location,
): void {
  const target = editorTarget.value;

  if (
      target?.kind === "location" &&
      sameLocation(
          target.location,
          location,
      )
  ) {
    closeInspector();
    return;
  }

  if (
      target?.kind === "connection" &&
      (
          sameLocation(
              target.first,
              location,
          ) ||
          sameLocation(
              target.second,
              location,
          )
      )
  ) {
    closeInspector();
  }
}

function onRegionDeleted(
    region: Region,
): void {
  const target = editorTarget.value;

  if (target === null) {
    return;
  }

  if (
      target.kind === "region" &&
      sameRegion(
          target.region,
          region,
      )
  ) {
    closeInspector();
    return;
  }

  /*
   * Region deletion may cascade through nested
   * regions and locations. Any existing target may
   * therefore have become stale.
   */
  closeInspector();
}

function onEdgeChanged(
    source: Location,
    destination: Location,
): void {
  const target = editorTarget.value;

  if (
      target?.kind === "connection" &&
      sameUnorderedPair(
          target.first,
          target.second,
          source,
          destination,
      )
  ) {
    connectionRefreshToken.value += 1;
  }
}

async function loadLorebook(): Promise<void> {
  const requestId = ++lorebookRequestId;

  lorebook.value = null;
  loadingLorebook.value = true;
  lorebookError.value = null;

  try {
    const loaded =
        await model.value.getLorebook();

    if (requestId === lorebookRequestId) {
      lorebook.value = loaded;
    }
  } catch (error) {
    if (requestId !== lorebookRequestId) {
      return;
    }

    console.error(
        "Could not load world lorebook",
        error,
    );

    lorebookError.value =
        "The world lorebook could not be loaded.";
  } finally {
    if (requestId === lorebookRequestId) {
      loadingLorebook.value = false;
    }
  }
}

async function exportWorld(): Promise<void> {
  exportingWorld.value = true;
  exportError.value = null;

  try {
    const response = await fetchApi(
        `api/export/${model.value.get('id')}/world`,
        {
          method: "GET",
          headers: {
            accept: "application/zip",
          },
        },
    );

    if (!response.ok) {
      throw new Error(
          `Export failed with status ${response.status}`,
      );
    }

    const blob = await response.blob();

    const url = URL.createObjectURL(blob);

    const link = document.createElement("a");

    const safeName = worldName.value
        .replace(
            /[<>:"/\\|?*\u0000-\u001F]/g,
            "_",
        )
        .trim();

    link.href = url;
    link.download = `${safeName || "world"}.zip`;

    document.body.appendChild(link);
    link.click();
    link.remove();

    window.setTimeout(
        () => URL.revokeObjectURL(url),
        0,
    );
  } catch (error) {
    console.error(
        "Failed to export world",
        error,
    );

    exportError.value =
        "The world could not be exported.";
  } finally {
    exportingWorld.value = false;
  }
}

function availableEditorWidth(): number {
  return (
      rootElement.value?.clientWidth ??
      window.innerWidth
  );
}

function clampInspectorWidth(
    width: number,
): number {
  const availableMaximum =
      availableEditorWidth() -
      MIN_GRAPH_WIDTH;

  const viewportMaximum = Math.max(
      MIN_INSPECTOR_WIDTH,
      Math.min(
          MAX_INSPECTOR_WIDTH,
          availableMaximum,
      ),
  );

  return Math.min(
      Math.max(
          width,
          MIN_INSPECTOR_WIDTH,
      ),
      viewportMaximum,
  );
}

function normalizeInspectorWidth(): void {
  if (
      availableEditorWidth() <=
      MOBILE_BREAKPOINT
  ) {
    return;
  }

  inspectorWidth.value =
      clampInspectorWidth(
          inspectorWidth.value,
      );
}

function beginInspectorResize(
    event: PointerEvent,
): void {
  if (
      editorTarget.value === null ||
      availableEditorWidth() <=
      MOBILE_BREAKPOINT
  ) {
    return;
  }

  stopInspectorResize?.();

  const startX = event.clientX;
  const startWidth =
      inspectorWidth.value;

  resizingInspector.value = true;

  const onPointerMove = (
      moveEvent: PointerEvent,
  ): void => {
    inspectorWidth.value =
        clampInspectorWidth(
            startWidth +
            startX -
            moveEvent.clientX,
        );
  };

  const stop = (): void => {
    window.removeEventListener(
        "pointermove",
        onPointerMove,
    );

    window.removeEventListener(
        "pointerup",
        stop,
    );

    window.removeEventListener(
        "pointercancel",
        stop,
    );

    resizingInspector.value = false;
    stopInspectorResize = null;
  };

  stopInspectorResize = stop;

  window.addEventListener(
      "pointermove",
      onPointerMove,
  );

  window.addEventListener(
      "pointerup",
      stop,
      {
        once: true,
      },
  );

  window.addEventListener(
      "pointercancel",
      stop,
      {
        once: true,
      },
  );

  event.preventDefault();
}

onMounted(() => {
  normalizeInspectorWidth();

  window.addEventListener(
      "resize",
      normalizeInspectorWidth,
  );

  void loadLorebook();
});

watch(
    () => model.value.get("id"),
    () => {
      closeInspector();
      closeWorldInformation();

      connectionRefreshToken.value = 0;
      exportError.value = null;

      void loadLorebook();
    },
);

onBeforeUnmount(() => {
  stopInspectorResize?.();

  window.removeEventListener(
      "resize",
      normalizeInspectorWidth,
  );
});
</script>

<template>
  <main
      ref="rootElement"
      class="world-edit"
      :style="rootStyle"
  >
    <section
        class="world-edit__workspace"
        :class="workspaceClass"
    >
      <div class="world-edit__graph-shell">
        <WorldEditGraph
            class="world-edit__graph"
            :world="model"
            :exporting-world="exportingWorld"
            :export-error="exportError"
            @back="emit('back')"
            @edit-world="openWorldInformation"
            @export-world="exportWorld"
            @close-editing="closeInspector"
            @dismiss-export-error="exportError = null"
            @edit-location="openLocationEditor"
            @edit-region="openRegionEditor"
            @edit-connection="openConnectionEditor"
            @location-deleted="onLocationDeleted"
            @region-deleted="onRegionDeleted"
            @edge-created="onEdgeChanged"
            @edge-deleted="onEdgeChanged"
        />
      </div>

      <template v-if="editorTarget">
        <div
            class="world-edit__separator"
            role="separator"
            aria-orientation="vertical"
            aria-label="Resize inspector"
            @pointerdown="beginInspectorResize"
        />

        <aside class="world-edit__inspector">
          <header class="world-edit__inspector-header">
            <div class="world-edit__inspector-heading">
              <span class="world-edit__inspector-kind">
                {{ inspectorKindLabel }}
              </span>

              <h2>{{ inspectorTitle }}</h2>
            </div>

            <IconButton
                title="Close inspector"
                @click="closeInspector"
            >
              <svg
                  viewBox="0 0 24 24"
                  aria-hidden="true"
              >
                <path d="M6 6l12 12" />
                <path d="M18 6 6 18" />
              </svg>
            </IconButton>
          </header>

          <div class="world-edit__inspector-body">
            <LocationEditor
                v-if="
                editorTarget.kind ===
                'location'
              "
                :key="
                editorTarget.location.hashKey()
              "
                :location="
                editorTarget.location
              "
            />

            <RegionEditor
                v-else-if="
                editorTarget.kind ===
                'region'
              "
                :key="
                editorTarget.region.hashKey()
              "
                :region="
                editorTarget.region
              "
            />

            <LocationEdgesEditor
                v-else
                :key="
                `${editorTarget.first.hashKey()}:${editorTarget.second.hashKey()}`
              "
                :first="editorTarget.first"
                :second="editorTarget.second"
                :refresh-token="
                connectionRefreshToken
              "
            />
          </div>
        </aside>
      </template>

    </section>

    <WindowPrompt
        v-if="showWorldInformation"
        title="World information"
        @close="closeWorldInformation"
    >
      <section class="world-edit__world-fields">
        <FieldEditorWrapper
            field-name="Name"
            info="The world's display name."
            :vertical="true"
        >
          <ShortTextBox
              :model-value="
              model.get('name')
            "
              aria-label="World name"
              @edit="
              (value: string) =>
                model.update(
                  'name',
                  value,
                )
            "
          />
        </FieldEditorWrapper>

        <FieldEditorWrapper
            field-name="Description"
            info="
            Global narrative and semantic
            context.
          "
            :vertical="true"
        >
          <LongTextBox
              :model-value="
              model.get('description')
            "
              aria-label="World description"
              tokenize
              :tokenization-started="true"
              @edit="
              (value: string) =>
                model.update(
                  'description',
                  value,
                )
            "
          />
        </FieldEditorWrapper>

        <Expandable
            title="World lorebook"
            info="
            Lore available throughout this
            world.
          "
            :initially-open="false"
        >
          <LorebookEditor
              v-if="lorebook"
              :model-value="lorebook"
          />

          <div
              v-else-if="loadingLorebook"
              class="world-edit__state"
              role="status"
          >
            Loading world lorebook…
          </div>

          <div
              v-else-if="lorebookError"
              class="
              world-edit__state
              world-edit__state--error
            "
              role="alert"
          >
            <span>{{ lorebookError }}</span>

            <IconButton
                title="Retry loading lorebook"
                variant="accent"
                @click="loadLorebook"
            >
              <svg
                  viewBox="0 0 24 24"
                  aria-hidden="true"
              >
                <path
                    d="
                    M20 11
                    a8 8 0 1 0-2.34 5.66
                  "
                />

                <path d="M20 4v7h-7" />
              </svg>
            </IconButton>
          </div>

          <div
              v-else
              class="world-edit__state"
          >
            This world does not currently
            expose a lorebook.
          </div>
        </Expandable>
      </section>
    </WindowPrompt>
  </main>
</template>

<style scoped>
.world-edit {
  position: absolute;
  inset: 0;
  z-index: 1;

  display: block;

  width: auto;
  height: auto;
  min-width: 0;
  min-height: 0;

  overflow: hidden;
  isolation: isolate;

  color: rgb(var(--c-fg));
  background: rgb(var(--c-page));
  font-family: var(--font-primary);
}

.world-edit__workspace {
  position: absolute;
  inset: 0;

  min-width: 0;
  min-height: 0;

  overflow: hidden;
}

.world-edit__graph-shell {
  position: absolute;
  inset: 0;

  min-width: 0;
  min-height: 0;

  overflow: hidden;

  transition:
      right
      var(--duration-normal)
      var(--ease-standard);
}

.world-edit__workspace--inspector-open
.world-edit__graph-shell {
  right:
      calc(
          var(--inspector-width) +
          9px
      );
}

.world-edit__workspace--resizing
.world-edit__graph-shell {
  transition: none;
}

.world-edit__graph {
  display: block;

  width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
}

/* -------------------------------------------------------------------------- */
/* Inspector                                                                  */
/* -------------------------------------------------------------------------- */

.world-edit__separator {
  position: absolute;
  z-index: 31;
  top: 0;
  right: var(--inspector-width);
  bottom: 0;

  width: 9px;

  cursor: col-resize;

  background:
      rgb(var(--c-border) / 0.58);

  touch-action: none;

  transition:
      background
      var(--duration-fast)
      var(--ease-standard);
}

.world-edit__separator::after {
  content: "";

  position: absolute;
  inset: 0 3px;

  background:
      rgb(var(--c-accent-2) / 0.7);
}

.world-edit__separator:hover,
.world-edit__workspace--resizing
.world-edit__separator {
  background:
      rgb(var(--c-accent-soft));
}

.world-edit__inspector {
  position: absolute;
  z-index: 30;
  top: 0;
  right: 0;
  bottom: 0;

  display: grid;
  grid-template-rows:
    auto
    minmax(0, 1fr);

  width: var(--inspector-width);
  min-width: 0;
  min-height: 0;

  overflow: hidden;

  background: rgb(var(--c-surface));

  border-left:
      1px solid
      rgb(var(--c-border));

  box-shadow:
      -14px 0 32px
      rgb(var(--c-shadow) / 0.18);
}

.world-edit__inspector-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-3);

  min-width: 0;

  padding: var(--space-4);

  background:
      rgb(var(--c-surface-raised));

  border-bottom:
      1px solid
      rgb(var(--c-border));
}

.world-edit__inspector-heading {
  min-width: 0;
}

.world-edit__inspector-kind {
  color: rgb(var(--c-muted));

  font-size: 0.66rem;
  font-weight: 800;
  line-height: 1.1;

  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.world-edit__inspector-header h2 {
  margin: var(--space-1) 0 0;

  overflow-wrap: anywhere;

  color: rgb(var(--c-fg-strong));

  font-size: 1rem;
}

.world-edit__inspector-body {
  min-width: 0;
  min-height: 0;

  overflow: auto;
  overscroll-behavior: contain;

  padding: var(--space-4);
}

/* -------------------------------------------------------------------------- */
/* World information                                                         */
/* -------------------------------------------------------------------------- */

.world-edit__world-fields {
  display: grid;
  gap: var(--space-4);

  width: min(48rem, 100%);
  min-width: 0;
}

/* -------------------------------------------------------------------------- */
/* Errors and states                                                          */
/* -------------------------------------------------------------------------- */

.world-edit__state {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);

  padding: var(--space-3);

  color: rgb(var(--c-fg));

  background:
      rgb(var(--c-surface));

  border:
      1px solid
      rgb(var(--c-border));

  border-radius: var(--radius-md);
}

.world-edit__state--error {
  color: rgb(var(--c-danger-strong));

  background:
      rgb(var(--c-danger-soft));

  border-color:
      rgb(var(--c-danger) / 0.5);
}

/* -------------------------------------------------------------------------- */
/* Responsive                                                                 */
/* -------------------------------------------------------------------------- */

@media (max-width: 840px) {
  .world-edit__workspace--inspector-open
  .world-edit__graph-shell {
    right: 0;
  }

  .world-edit__separator {
    display: none;
  }

  .world-edit__inspector {
    width: 100%;

    border-left: 0;
  }

}

@media (max-width: 520px) {
  .world-edit__inspector-header,
  .world-edit__inspector-body {
    padding: var(--space-3);
  }
}

@media (prefers-reduced-motion: reduce) {
  .world-edit__graph-shell,
  .world-edit__separator {
    transition: none;
  }
}
</style>
