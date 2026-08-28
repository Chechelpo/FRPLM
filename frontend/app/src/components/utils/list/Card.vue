<script
    setup
    lang="ts"
    generic="
    Data extends DataRecord,
    EntityType extends ABSEntity<any, Data>
  "
>
import { computed } from "vue";

import {
  ABSEntity,
  CommonFields,
  type DataRecord,
} from "@frplm/host-sdk";

const props = withDefaults(
    defineProps<{
      /**
       * Retained as `character` for compatibility with existing
       * consumers. This component supports any ABSEntity.
       */
      character: EntityType;
      hasDescription?: boolean;
    }>(),
    {
      hasDescription: false,
    },
);

const emit = defineEmits<{
  (event: "open", element: EntityType): void;
  (event: "edit", element: EntityType): void;
  (event: "remove", element: EntityType): void;
}>();

const entityName = computed<string>(() => {
  if (
      !props.character.hasAttribute(
          CommonFields.NAME,
      )
  ) {
    return "Unnamed item";
  }

  const value = props.character.getCommon(
      CommonFields.NAME,
  );

  const normalized = String(value ?? "").trim();

  return normalized || "Unnamed item";
});

const entityDescription = computed<
    string | null
>(() => {
  if (
      !props.hasDescription ||
      !props.character.hasAttribute(
          CommonFields.DESCRIPTION,
      )
  ) {
    return null;
  }

  const value = props.character.getCommon(
      CommonFields.DESCRIPTION,
  );

  const normalized = String(value ?? "").trim();

  return normalized || null;
});

function onOpen(): void {
  emit("open", props.character);
}

function onEdit(): void {
  emit("edit", props.character);
}

function onRemove(): void {
  emit("remove", props.character);
}
</script>

<template>
  <article
      class="entity-card"
      :class="{
      'entity-card--with-description':
        entityDescription,
    }"
  >
    <button
        type="button"
        class="entity-card__main"
        :title="entityName"
        @click="onOpen"
    >
      <span class="entity-card__content">
        <span class="entity-card__name">
          {{ entityName }}
        </span>

        <span
            v-if="entityDescription"
            class="entity-card__description"
        >
          {{ entityDescription }}
        </span>
      </span>
    </button>

    <div class="entity-card__actions">
      <button
          type="button"
          class="
          entity-card__action
          entity-card__action--edit
        "
          :title="`Edit ${entityName}`"
          :aria-label="`Edit ${entityName}`"
          @click.stop="onEdit"
      >
        <svg
            viewBox="0 0 24 24"
            aria-hidden="true"
        >
          <path
              d="M12 20h9"
          />

          <path
              d="M16.5 3.5a2.12 2.12 0 0 1 3 3L8 18l-4 1 1-4Z"
          />
        </svg>
      </button>

      <button
          type="button"
          class="
          entity-card__action
          entity-card__action--remove
        "
          :title="`Remove ${entityName}`"
          :aria-label="`Remove ${entityName}`"
          @click.stop="onRemove"
      >
        <svg
            viewBox="0 0 24 24"
            aria-hidden="true"
        >
          <path d="M3 6h18" />
          <path d="M8 6V4h8v2" />
          <path d="m19 6-1 14H6L5 6" />
          <path d="M10 11v5" />
          <path d="M14 11v5" />
        </svg>
      </button>
    </div>
  </article>
</template>

<style scoped>
.entity-card {
  width: 100%;
  min-width: 0;
  min-height: 3rem;
  box-sizing: border-box;

  display: flex;
  align-items: stretch;

  color: rgb(var(--c-fg));

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-surface-raised) / 0.58),
          rgb(var(--c-surface-2) / 0.36)
      );

  border:
      1px solid
      rgb(var(--c-border) / 0.28);
  border-radius: var(--radius-sm);

  box-shadow:
      inset 0 1px 0
      rgb(255 255 255 / 0.28),
      0 2px 7px
      rgb(var(--c-shadow) / 0.035);

  overflow: hidden;

  transition:
      background-color
      var(--duration-fast)
      var(--ease-standard),
      border-color
      var(--duration-fast)
      var(--ease-standard),
      box-shadow
      var(--duration-fast)
      var(--ease-standard),
      transform
      var(--duration-fast)
      var(--ease-standard);
}

.entity-card:hover {
  background:
      linear-gradient(
          145deg,
          rgb(var(--c-surface-hover) / 0.78),
          rgb(var(--c-surface-2) / 0.48)
      );

  border-color:
      rgb(var(--c-primary) / 0.48);

  box-shadow:
      inset 0 1px 0
      rgb(255 255 255 / 0.34),
      0 4px 12px
      rgb(var(--c-shadow) / 0.065);

  transform: translateY(-1px);
}

.entity-card:focus-within {
  border-color:
      rgb(var(--c-accent) / 0.66);

  box-shadow:
      0 0 0
      var(--focus-ring-width)
      rgb(var(--focus-ring-color) / 0.13),
      inset 0 1px 0
      rgb(255 255 255 / 0.32),
      0 4px 12px
      rgb(var(--c-shadow) / 0.06);
}

/* -------------------------------------------------------------------------- */
/* Main content                                                               */
/* -------------------------------------------------------------------------- */

.entity-card__main {
  flex: 1 1 auto;

  display: flex;
  align-items: center;

  min-width: 0;

  padding:
      var(--space-2)
      var(--space-3);

  color: inherit;

  background: transparent;
  border: 0;
  outline: 0;

  font: inherit;
  text-align: left;

  cursor: pointer;
}

.entity-card__content {
  display: flex;
  flex-direction: column;
  gap: 0.18rem;

  min-width: 0;
}

.entity-card__name {
  overflow: hidden;

  color: rgb(var(--c-fg-strong));

  font-size: 0.88rem;
  font-weight: 750;
  line-height: 1.3;

  text-overflow: ellipsis;
  white-space: nowrap;
}

.entity-card__description {
  display: -webkit-box;

  overflow: hidden;

  color: rgb(var(--c-muted));

  font-size: 0.72rem;
  font-weight: 450;
  line-height: 1.4;

  overflow-wrap: anywhere;

  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.entity-card--with-description {
  min-height: 3.75rem;
}

/* -------------------------------------------------------------------------- */
/* Actions                                                                    */
/* -------------------------------------------------------------------------- */

.entity-card__actions {
  flex: 0 0 auto;

  display: flex;
  align-items: center;
  gap: var(--space-1);

  padding:
      var(--space-1)
      var(--space-1)
      var(--space-1)
      0;
}

.entity-card__action {
  width: 2rem;
  height: 2rem;
  min-height: 0;
  flex: 0 0 auto;

  display: grid;
  place-items: center;

  padding: 0;

  color: rgb(var(--c-muted));

  background: transparent;
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  outline: 0;

  cursor: pointer;

  transition:
      color
      var(--duration-fast)
      var(--ease-standard),
      background-color
      var(--duration-fast)
      var(--ease-standard),
      border-color
      var(--duration-fast)
      var(--ease-standard),
      transform
      var(--duration-fast)
      var(--ease-standard);
}

.entity-card__action svg {
  width: 1rem;
  height: 1rem;

  fill: none;
  stroke: currentColor;
  stroke-width: 1.9;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.entity-card__action--edit:hover {
  color: rgb(var(--c-primary-strong));

  background:
      rgb(var(--c-accent) / 0.15);
  border-color:
      rgb(var(--c-accent) / 0.35);
}

.entity-card__action--remove:hover {
  color: rgb(var(--c-on-danger));

  background:
      rgb(var(--c-danger) / 0.86);
  border-color:
      rgb(var(--c-danger));
}

.entity-card__action:active {
  transform: scale(0.93);
}

.entity-card__action:focus-visible {
  border-color:
      rgb(var(--c-accent) / 0.65);

  box-shadow:
      0 0 0
      2px
      rgb(var(--focus-ring-color) / 0.22);
}

@media (max-width: 420px) {
  .entity-card__main {
    padding:
        var(--space-2)
        var(--space-2);
  }

  .entity-card__actions {
    gap: 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .entity-card,
  .entity-card__action {
    transition: none;
  }

  .entity-card:hover {
    transform: none;
  }
}
</style>