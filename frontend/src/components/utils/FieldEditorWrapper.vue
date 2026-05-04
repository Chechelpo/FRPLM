<script setup lang="ts">
import { computed, nextTick, ref } from "vue";

const props = withDefaults(
    defineProps<{
      fieldName: string | null;
      info?: string;
      vertical?: boolean;
    }>(),
    {
      vertical: false,
    }
);

const iconEl = ref<HTMLElement | null>(null);
const tooltipVisible = ref(false);

const tooltipPosition = ref({
  top: 0,
  left: 0,
});

const hasInfo = computed(() => Boolean(props.info));

async function updateTooltipPosition() {
  if (!iconEl.value) return;

  await nextTick();

  const rect = iconEl.value.getBoundingClientRect();

  tooltipPosition.value = {
    top: rect.bottom + window.scrollY + 8,
    left: rect.left + window.scrollX + rect.width / 2,
  };
}

async function showTooltip() {
  if (!hasInfo.value) return;

  await updateTooltipPosition();

  tooltipVisible.value = true;
}

function hideTooltip() {
  tooltipVisible.value = false;
}
</script>

<template>
  <div
      class="field-wrapper"
      :class="{ 'field-wrapper-vertical': vertical }"
  >
    <div
        v-if="fieldName"
        class="field-label-row"
    >
      <span class="field-label">
        {{ fieldName }}

        <sup
            v-if="info"
            ref="iconEl"
            class="field-info-marker"
            tabindex="0"
            aria-label="Field information"
            @mouseenter.stop="showTooltip"
            @mouseleave.stop="hideTooltip"
            @focus.stop="showTooltip"
            @blur.stop="hideTooltip"
            @click.stop
        >
          ⓘ
        </sup>:
      </span>
    </div>

    <div
        v-if="$slots.default"
        class="field-editor"
    >
      <slot />
    </div>

    <Teleport to="body">
      <div
          v-if="tooltipVisible && info"
          class="field-tooltip-teleported"
          :style="{
          top: `${tooltipPosition.top}px`,
          left: `${tooltipPosition.left}px`,
        }"
      >
        {{ info }}
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.field-wrapper {
  display: grid;
  grid-template-columns: max-content minmax(0, 1fr);
  align-items: center;
  column-gap: 0.5rem;
  row-gap: 0.35rem;
}

.field-wrapper-vertical {
  grid-template-columns: 1fr;
  align-items: stretch;
}

.field-label-row {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  white-space: nowrap;
}

.field-label {
  font-weight: 600;
  color: var(--primary-text, #e2e8f0);
}

.field-editor {
  min-width: 0;
}

.field-info-marker {
  position: relative;
  top: -0.35em;

  display: inline-flex;
  align-items: center;
  justify-content: center;

  margin-left: 0.12rem;

  min-width: 0.85em;
  height: 0.85em;

  color: var(--primary-accent, #f59e0b);

  font-size: 0.65em;
  font-weight: 700;
  line-height: 1;

  cursor: help;
  user-select: none;
}

.field-tooltip-teleported {
  position: absolute;
  transform: translateX(-50%);

  width: max-content;
  max-width: 16rem;

  padding: 0.45rem 0.6rem;
  border-radius: 0.375rem;
  border: 1px solid var(--primary-accent, #f59e0b);

  background: var(--secondary-background, #292524);
  color: var(--primary-text, #e2e8f0);

  font-size: 0.8rem;
  font-weight: 400;
  line-height: 1.3;
  white-space: normal;

  z-index: 999999;
  pointer-events: none;
}
</style>