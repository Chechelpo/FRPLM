<script setup lang="ts">
import {
  computed,
  onMounted,
  ref,
  shallowRef,
} from "vue";

import { Session } from "@/domain/Session";
import {
  PromptTemplate,
  type PromptTemplateData,
  type PromptTemplateKey,
} from "@/domain/Prompts";
import { World } from "@/domain/World";
import { EntityTypes } from "@/domain/EntityTypes";
import { createEntity } from "@/core/ABSEntity";

import Expandable from "@/components/utils/panels/Expandable.vue";
import PromptTemplateEditor from "@/components/prompts/PromptTemplateEditor.vue";
import SingleEnumInput from "@/components/utils/primitiveEditors/SingleEnumInput.vue";
import WorldEdit from "@/components/space/WorldEdit.vue";
import IconButton from "@/components/utils/buttons/IconButton.vue";
import PromptTemplatePicker from "@/components/prompts/PromptTemplatePicker.vue";
import ExtensionConfigs from "@/components/extension/ExtensionConfigs.vue";

const model = defineModel<Session>({
  required: true,
  type: Session,
});

const world = shallowRef<World>();
const template = shallowRef<PromptTemplate | null>(null);

const loading = ref(true);


onMounted(async () => {
  loading.value = true;

  try {
    const [
      sessionTemplate,
      sessionWorld,
    ] = await Promise.all([
      model.value.getTemplate(),
      model.value.getWorld(),
    ]);

    template.value = sessionTemplate;
    world.value = sessionWorld;
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <aside
      class="session-config-sidebar"
      aria-label="Session configuration"
      :aria-busy="loading"
  >
    <header class="session-config-sidebar__header">
      <span class="edit-box__eyebrow">
        Session
      </span>

      <h2 class="session-config-sidebar__title">
        Configuration
      </h2>

      <p class="session-config-sidebar__description">
        Configure the prompt template and world used by this
        conversation.
      </p>
    </header>

    <div
        v-if="loading"
        class="
        edit-box__state
        edit-box__state--vertical
        session-config-sidebar__state
      "
        role="status"
    >
      <span
          class="edit-box__spinner"
          aria-hidden="true"
      />

      <span>Loading configuration...</span>
    </div>

    <div
        v-else
        class="session-config-sidebar__sections"
    >
      <Expandable
          title="Prompt"
          class="session-config-sidebar__panel"
      >
        <PromptTemplatePicker :session="model" />
      </Expandable>

      <Expandable
          title="World"
          class="session-config-sidebar__panel"
      >
        <div class="session-config-sidebar__panel-content">
          <div class="session-config-sidebar__section-heading">
            <span class="session-config-sidebar__section-title">
              World configuration
            </span>

            <span class="session-config-sidebar__section-description">
              Edit locations, characters, and other world state.
            </span>
          </div>

          <div
              v-if="world"
              class="session-config-sidebar__editor"
          >
            <WorldEdit :model-value="world" />
          </div>

          <div
              v-else
              class="
              edit-box__state
              edit-box__state--vertical
              session-config-sidebar__state
            "
          >
            <div class="edit-box__state-content">
              <strong class="edit-box__state-title">
                No world configured
              </strong>

              <p class="edit-box__state-description">
                This session does not currently have an associated
                world.
              </p>
            </div>
          </div>
        </div>
      </Expandable>
      <ExtensionConfigs/>
    </div>
  </aside>
</template>

<style scoped>
.session-config-sidebar {
  width: 100%;
  min-width: 0;
  height: 100%;
  max-height: 100dvh;
  box-sizing: border-box;

  display: flex;
  flex-direction: column;
  gap: var(--space-3);

  padding: var(--space-3);

  overflow-y: auto;
  overscroll-behavior: contain;

  color: rgb(var(--c-fg));

  background:
      linear-gradient(
          160deg,
          rgb(var(--c-surface-raised) / 0.64),
          rgb(var(--c-surface) / 0.48) 48%,
          rgb(var(--c-surface-2) / 0.42)
      );

  border-right:
      1px solid
      rgb(var(--c-border) / 0.3);

  box-shadow:
      5px 0 18px
      rgb(var(--c-shadow) / 0.045);

  scrollbar-width: thin;
  scrollbar-color:
      rgb(var(--c-primary) / 0.42)
      transparent;
}

.session-config-sidebar::-webkit-scrollbar {
  width: 0.65rem;
}

.session-config-sidebar::-webkit-scrollbar-track {
  background: transparent;
}

.session-config-sidebar::-webkit-scrollbar-thumb {
  background: rgb(var(--c-primary) / 0.36);
  border: 2px solid transparent;
  border-radius: var(--radius-round);
  background-clip: padding-box;
}

.session-config-sidebar::-webkit-scrollbar-thumb:hover {
  background: rgb(var(--c-primary) / 0.56);
  border: 2px solid transparent;
  background-clip: padding-box;
}

/* -------------------------------------------------------------------------- */
/* Sidebar header                                                             */
/* -------------------------------------------------------------------------- */

.session-config-sidebar__header {
  flex: 0 0 auto;

  padding:
      var(--space-2)
      var(--space-2)
      var(--space-3);

  border-bottom:
      1px solid
      rgb(var(--c-border) / 0.24);
}

.session-config-sidebar__title {
  margin: 0;

  color: rgb(var(--c-fg-strong));

  font-size: 1.05rem;
  font-weight: 800;
  line-height: 1.25;
}

.session-config-sidebar__description {
  margin:
      var(--space-2)
      0
      0;

  color: rgb(var(--c-muted));

  font-size: 0.76rem;
  line-height: 1.5;
}

/* -------------------------------------------------------------------------- */
/* Panels                                                                     */
/* -------------------------------------------------------------------------- */

.session-config-sidebar__sections {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);

  min-width: 0;
}

.session-config-sidebar__panel {
  min-width: 0;

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-surface-raised) / 0.62),
          rgb(var(--c-surface-2) / 0.34)
      );

  border:
      1px solid
      rgb(var(--c-border) / 0.28);
  border-radius: var(--radius-md);

  box-shadow:
      inset 0 1px 0
      rgb(255 255 255 / 0.3),
      0 4px 14px
      rgb(var(--c-shadow) / 0.045);

  overflow: hidden;
}

.session-config-sidebar__panel-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);

  min-width: 0;

  padding: var(--space-3);
}

.session-config-sidebar__toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-3);

  min-width: 0;
}

.session-config-sidebar__toolbar-text,
.session-config-sidebar__section-heading {
  min-width: 0;
}

.session-config-sidebar__section-title {
  display: block;

  color: rgb(var(--c-fg-strong));

  font-size: 0.82rem;
  font-weight: 800;
  line-height: 1.3;
}

.session-config-sidebar__section-description {
  display: block;

  margin-top: var(--space-1);

  color: rgb(var(--c-muted));

  font-size: 0.72rem;
  line-height: 1.45;
}

.session-config-sidebar__new-button {
  min-height: 2rem;
  flex: 0 0 auto;

  padding:
      0.35rem
      0.65rem;
}

.session-config-sidebar__editor {
  min-width: 0;
}

.session-config-sidebar__state {
  min-height: 7rem;

  padding: var(--space-4);
}

/* -------------------------------------------------------------------------- */
/* Responsive                                                                 */
/* -------------------------------------------------------------------------- */

@media (max-width: 700px) {
  .session-config-sidebar {
    max-height: none;

    border-right: 0;
    border-bottom:
        1px solid
        rgb(var(--c-border) / 0.3);

    box-shadow:
        0 5px 18px
        rgb(var(--c-shadow) / 0.045);
  }
}

@media (max-width: 420px) {
  .session-config-sidebar__toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .session-config-sidebar__new-button {
    width: 100%;
  }
}

@media (prefers-reduced-motion: reduce) {
  .session-config-sidebar {
    scroll-behavior: auto;
  }
}
</style>