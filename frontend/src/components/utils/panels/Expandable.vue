<script setup lang="ts">
import {
  getCurrentInstance,
  ref,
  watch,
} from "vue";

import BooleanToggle from "@/components/primitive-editors/BooleanToggle.vue";
import FieldEditorWrapper from "@/components/utils/FieldEditorWrapper.vue";

export type ExpandableVariant =
    | "default"
    | "compact"
    | "card";

const props = withDefaults(
    defineProps<{
      title: string;
      info?: string;
      description?: string;

      initiallyOpen?: boolean;
      disabled?: boolean;

      variant?: ExpandableVariant;

      showEnabledToggle?: boolean;
      enabled?: boolean;
      enabledLabel?: string;
    }>(),
    {
      info: "",
      description: "",

      initiallyOpen: false,
      disabled: false,

      variant: "default",

      showEnabledToggle: false,
      enabled: true,
      enabledLabel: "Enabled",
    },
);

const emit = defineEmits<{
  (
      event: "statusChange",
      isOpen: boolean,
  ): void;

  (
      event: "enabledChange",
      enabled: boolean,
  ): void;

  (
      event: "update:enabled",
      enabled: boolean,
  ): void;
}>();

const open = ref(props.initiallyOpen);
const enabledState = ref(props.enabled);

const instance = getCurrentInstance();

const componentId =
    `expandable-${instance?.uid ?? Math.random()
        .toString(36)
        .slice(2)}`;

const titleId = `${componentId}-title`;
const contentId = `${componentId}-content`;

watch(
    () => props.enabled,
    (enabled) => {
      enabledState.value = enabled;
    },
);

function toggle(): void {
  if (props.disabled) {
    return;
  }

  open.value = !open.value;

  emit(
      "statusChange",
      open.value,
  );
}

function setOpen(value: boolean): void {
  if (
      props.disabled ||
      open.value === value
  ) {
    return;
  }

  open.value = value;

  emit(
      "statusChange",
      value,
  );
}

function onEnabledChange(
    enabled: boolean,
): void {
  if (props.disabled) {
    return;
  }

  enabledState.value = enabled;

  emit(
      "enabledChange",
      enabled,
  );

  emit(
      "update:enabled",
      enabled,
  );
}

defineExpose({
  open: () => setOpen(true),
  close: () => setOpen(false),
  toggle,
});
</script>

<template>
  <section
      class="expandable"
      :class="[
      `expandable--${props.variant}`,
      {
        'expandable--open': open,
        'expandable--disabled':
          props.disabled,
        'expandable--inactive':
          props.showEnabledToggle &&
          !enabledState,
      },
    ]"
  >
    <header
        class="expandable__header"
        @click="toggle"
    >
      <button
          type="button"
          class="expandable__disclosure"
          :class="{
    'expandable__disclosure--open': open,
  }"
          :disabled="props.disabled"
          :aria-expanded="open"
          :aria-controls="contentId"
          :aria-label="
    open
      ? `Collapse ${props.title}`
      : `Expand ${props.title}`
  "
          @click.stop="toggle"
      >
        <svg
            viewBox="0 0 20 20"
            aria-hidden="true"
        >
          <path d="m6.5 8 3.5 3.5L13.5 8"/>
        </svg>
      </button>

      <div class="expandable__heading">
        <div class="expandable__title-row">
          <div
              :id="titleId"
              class="expandable__title"
          >
            <FieldEditorWrapper
                class="expandable__field"
                :field-name="props.title"
                :info="props.info"
            />
          </div>
        </div>

        <p
            v-if="props.description"
            class="expandable__description"
        >
          {{ props.description }}
        </p>
      </div>

      <div
          v-if="
          $slots.actions ||
          props.showEnabledToggle
        "
          class="expandable__actions"
          @click.stop
          @keydown.stop
      >
        <slot
            name="actions"
            :open="open"
            :enabled="enabledState"
            :toggle="toggle"
        />

        <div
            v-if="props.showEnabledToggle"
            class="expandable__enable-control"
        >
          <span class="expandable__enable-label">
            {{ props.enabledLabel }}
          </span>

          <BooleanToggle
              :model-value="enabledState"
              :read_only="props.disabled"
              :label="
              `${props.enabledLabel}: ${
                enabledState
                  ? 'enabled'
                  : 'disabled'
              }`
            "
              @edit="onEnabledChange"
          />
        </div>
      </div>
    </header>

    <Transition name="expandable-content">
      <div
          v-if="open"
          :id="contentId"
          class="expandable__content"
          role="region"
          :aria-labelledby="titleId"
          :aria-disabled="
          props.showEnabledToggle &&
          !enabledState
        "
      >
        <div class="expandable__content-inner">
          <slot
              :open="open"
              :enabled="enabledState"
          />
        </div>
      </div>
    </Transition>
  </section>
</template>

<style scoped>
.expandable {
  width: 100%;
  min-width: 0;
  box-sizing: border-box;

  color: rgb(var(--c-fg));
  font-family: var(--font-primary);

  background: linear-gradient(
      145deg,
      rgb(var(--c-surface-raised) / 0.5),
      rgb(var(--c-surface-2) / 0.3)
  );

  border: 1px solid rgb(var(--c-border) / 0.3);
  border-radius: var(--radius-md);

  box-shadow: inset 0 1px 0 rgb(255 255 255 / 0.22);

  overflow: hidden;

  transition: background-color var(--duration-normal) var(--ease-standard),
  border-color var(--duration-normal) var(--ease-standard),
  box-shadow var(--duration-normal) var(--ease-standard),
  opacity var(--duration-normal) var(--ease-standard);
}

/* -------------------------------------------------------------------------- */
/* Header                                                                     */
/* -------------------------------------------------------------------------- */

.expandable__header {
  min-width: 0;
  min-height: 3.75rem;
  box-sizing: border-box;

  display: grid;
  grid-template-columns:
    auto
    minmax(0, 1fr)
    auto;
  align-items: center;
  gap: var(--space-3);

  padding: var(--space-3) var(--space-4);

  cursor: pointer;
  user-select: none;

  transition: background-color var(--duration-fast) var(--ease-standard),
  color var(--duration-fast) var(--ease-standard);
}

.expandable__header:hover {
  background: rgb(var(--c-surface-hover) / 0.58);
}

.expandable--open
.expandable__header {
  background: linear-gradient(
      145deg,
      rgb(var(--c-accent) / 0.1),
      rgb(var(--c-primary) / 0.045)
  );

  border-bottom: 1px solid rgb(var(--c-border) / 0.24);
}

/* -------------------------------------------------------------------------- */
/* Disclosure button                                                          */
/* -------------------------------------------------------------------------- */

.expandable__disclosure {
  width: 2.15rem;
  height: 2.15rem;
  flex: 0 0 auto;

  display: grid;
  place-items: center;

  padding: 0;

  color: rgb(var(--c-muted));

  background: rgb(var(--c-surface-raised) / 0.52);

  border: 1px solid rgb(var(--c-border) / 0.3);
  border-radius: var(--radius-sm);
  outline: 0;

  cursor: pointer;

  transition: color var(--duration-fast) var(--ease-standard),
  background-color var(--duration-fast) var(--ease-standard),
  border-color var(--duration-fast) var(--ease-standard),
  transform var(--duration-normal) var(--ease-standard);
}

.expandable__disclosure:hover:not(:disabled) {
  color: rgb(var(--c-primary-strong));

  background: rgb(var(--c-accent) / 0.13);

  border-color: rgb(var(--c-accent) / 0.45);
}

.expandable__disclosure:focus-visible {
  outline: var(--focus-ring-width) solid rgb(var(--focus-ring-color) / 0.3);

  outline-offset: 2px;
}

.expandable__disclosure svg {
  width: 1.15rem;
  height: 1.15rem;

  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;

  transform: rotate(-90deg);

  transition: transform var(--duration-normal) var(--ease-standard);
}

.expandable__disclosure--open {
  color: rgb(var(--c-on-accent));

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-primary)),
          rgb(var(--c-primary-strong))
      );

  border-color:
      rgb(var(--c-accent) / 0.66);
}

.expandable__disclosure--open svg {
  transform: rotate(0deg);
}

/* -------------------------------------------------------------------------- */
/* Heading                                                                    */
/* -------------------------------------------------------------------------- */

.expandable__heading {
  min-width: 0;
}

.expandable__title-row {
  min-width: 0;

  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.expandable__title {
  min-width: 0;
}

.expandable__field {
  width: auto;
}

.expandable__field :deep(.field-wrapper) {
  width: auto;
}

.expandable__field
:deep(.field-wrapper__label),
.expandable__field
:deep(.field-label) {
  color: rgb(var(--c-fg-strong));

  font-size: 0.92rem;
  font-weight: 800;
  line-height: 1.3;
}

.expandable__field
:deep(.field-wrapper__separator) {
  display: none;
}

.expandable__description {
  max-width: 48rem;

  margin: var(--space-1) 0 0;

  color: rgb(var(--c-muted));

  font-size: 0.72rem;
  line-height: 1.45;
}

.expandable__status {
  display: inline-flex;
  align-items: center;

  padding: 0.16rem 0.42rem;

  color: rgb(var(--c-muted));

  background: rgb(var(--c-surface-3) / 0.48);

  border: 1px solid rgb(var(--c-border) / 0.25);
  border-radius: var(--radius-round);

  font-size: 0.59rem;
  font-weight: 850;
  line-height: 1.2;

  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.expandable__status--enabled {
  color: rgb(var(--c-success-strong));

  background: rgb(var(--c-success) / 0.09);

  border-color: rgb(var(--c-success) / 0.25);
}

/* -------------------------------------------------------------------------- */
/* Header actions                                                             */
/* -------------------------------------------------------------------------- */

.expandable__actions {
  min-width: 0;

  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-3);
}

.expandable__enable-control {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);

  padding-left: var(--space-3);

  border-left: 1px solid rgb(var(--c-border) / 0.25);
}

.expandable__enable-label {
  color: rgb(var(--c-muted));

  font-size: 0.67rem;
  font-weight: 750;
  line-height: 1.2;
}

/* -------------------------------------------------------------------------- */
/* Content                                                                    */
/* -------------------------------------------------------------------------- */

.expandable__content {
  min-width: 0;
}

.expandable__content-inner {
  min-width: 0;

  padding: var(--space-4);

  background: rgb(var(--c-surface) / 0.18);
}

/* -------------------------------------------------------------------------- */
/* Default variant                                                            */
/* -------------------------------------------------------------------------- */

.expandable--default {
  border-radius: var(--radius-md);
}

/* -------------------------------------------------------------------------- */
/* Compact variant                                                            */
/* -------------------------------------------------------------------------- */

.expandable--compact {
  border-radius: var(--radius-sm);

  box-shadow: none;
}

.expandable--compact
.expandable__header {
  min-height: 2.75rem;

  gap: var(--space-2);

  padding: var(--space-2) var(--space-3);
}

.expandable--compact
.expandable__disclosure {
  width: 1.65rem;
  height: 1.65rem;

  border-radius: var(--radius-xs);
}

.expandable--compact
.expandable__disclosure
svg {
  width: 0.9rem;
  height: 0.9rem;
}

.expandable--compact
.expandable__field
:deep(.field-wrapper__label),
.expandable--compact
.expandable__field
:deep(.field-label) {
  font-size: 0.76rem;
}

.expandable--compact
.expandable__description {
  font-size: 0.64rem;
}

.expandable--compact
.expandable__content-inner {
  padding: var(--space-3);
}

.expandable--compact
.expandable__enable-control {
  padding-left: var(--space-2);
}

/* -------------------------------------------------------------------------- */
/* Card variant                                                               */
/* -------------------------------------------------------------------------- */

.expandable--card {
  background: linear-gradient(
      145deg,
      rgb(var(--c-surface-raised) / 0.88),
      rgb(var(--c-surface-2) / 0.52)
  );

  border: 1px solid rgb(var(--c-border-strong) / 0.44);
  border-radius: var(--radius-lg);

  box-shadow: 0 8px 24px rgb(var(--c-shadow) / 0.11),
  inset 0 1px 0 rgb(255 255 255 / 0.3);
}

.expandable--card
.expandable__header {
  min-height: 4.5rem;

  padding: var(--space-4) var(--space-5);
}

.expandable--card
.expandable__disclosure {
  width: 2.5rem;
  height: 2.5rem;

  border-radius: var(--radius-md);
}

.expandable--card
.expandable__disclosure
svg {
  width: 1.25rem;
  height: 1.25rem;
}

.expandable--card
.expandable__field
:deep(.field-wrapper__label),
.expandable--card
.expandable__field
:deep(.field-label) {
  font-size: 1rem;
}

.expandable--card
.expandable__description {
  font-size: 0.76rem;
}

.expandable--card
.expandable__content-inner {
  padding: var(--space-5);

  background: linear-gradient(
      145deg,
      rgb(var(--c-surface) / 0.24),
      rgb(var(--c-surface-2) / 0.18)
  );
}

/* -------------------------------------------------------------------------- */
/* Inactive and disabled                                                      */
/* -------------------------------------------------------------------------- */

.expandable--inactive
.expandable__heading {
  opacity: 0.72;
}

.expandable--inactive
.expandable__content-inner {
  opacity: 0.72;
}

.expandable--disabled {
  opacity: 0.5;
}

.expandable--disabled
.expandable__header {
  cursor: not-allowed;
}

.expandable--disabled
.expandable__header:hover {
  background: transparent;
}

.expandable__disclosure:disabled {
  cursor: not-allowed;
}

/* -------------------------------------------------------------------------- */
/* Transition                                                                 */
/* -------------------------------------------------------------------------- */

.expandable-content-enter-active,
.expandable-content-leave-active {
  transform-origin: top;

  transition: opacity var(--duration-normal) var(--ease-standard),
  transform var(--duration-normal) var(--ease-standard);
}

.expandable-content-enter-from,
.expandable-content-leave-to {
  opacity: 0;
  transform: translateY(-0.3rem) scaleY(0.98);
}

/* -------------------------------------------------------------------------- */
/* Responsive                                                                 */
/* -------------------------------------------------------------------------- */

@media (max-width: 620px) {
  .expandable__header {
    grid-template-columns:
      auto
      minmax(0, 1fr);
  }

  .expandable__actions {
    grid-column: 1 / -1;

    width: 100%;
    justify-content: flex-end;

    padding-top: var(--space-2);

    border-top: 1px solid rgb(var(--c-border) / 0.18);
  }

  .expandable__enable-control {
    padding-left: 0;
    border-left: 0;
  }

  .expandable--card
  .expandable__header {
    padding: var(--space-4);
  }
}

@media (max-width: 420px) {
  .expandable__enable-label {
    display: none;
  }

  .expandable__content-inner,
  .expandable--card
  .expandable__content-inner {
    padding: var(--space-3);
  }
}

@media (prefers-reduced-motion: reduce) {
  .expandable,
  .expandable__header,
  .expandable__disclosure,
  .expandable__disclosure svg,
  .expandable-content-enter-active,
  .expandable-content-leave-active {
    transition: none;
  }
}
</style>