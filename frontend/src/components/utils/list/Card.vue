<script
    setup
    lang="ts"
    generic="
    Data extends DataRecord,
    EntityType extends ABSEntity<any, Data>
  "
>
import { computed } from "vue";
import { ABSEntity } from "@/frameworks/ABSEntity";
import { DataRecord } from "@/types/DTOs";
import { CommonFields } from "@/utils/CommonFields";

const props = defineProps<{
  character: EntityType;
  hasDescription: boolean;
}>();

const emit = defineEmits<{
  edit: [element: EntityType];
  remove: [element: EntityType];
}>();

const characterName = computed<string>(() => {
  if (!props.character.hasAttribute(CommonFields.NAME)) {
    throw new Error(
        `Character of ${props.character.getEntityType} must specify a name to be shown in a card`,
    );
  }

  return String(
      props.character.getCommon(CommonFields.NAME),
  );
});

function onEdit(): void {
  emit("edit", props.character);
}

function onRemove(): void {
  emit("remove", props.character);
}
</script>

<template>
  <div class="card">
    <div
        class="cardName"
        :title="characterName"
    >
      {{ characterName }}
    </div>

    <div class="cardActions">
      <button
          type="button"
          class="cardIcon cardIcon--edit"
          title="Edit character"
          aria-label="Edit character"
          @click.stop="onEdit"
      >
        <img
            src="/icons/edit.png"
            alt=""
        />
      </button>

      <button
          type="button"
          class="cardIcon cardIcon--remove"
          title="Remove character"
          aria-label="Remove character"
          @click.stop="onRemove"
      >
        <img
            src="/characterCard/Trash.png"
            alt=""
        />
      </button>
    </div>
  </div>
</template>

<style scoped>
.card {
  width: 100%;
  height: 50px;
  box-sizing: border-box;

  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;

  padding: 4px 6px 4px 14px;

  color: var(--primary-text-color, #2f2418);

  background-color: rgba(184, 143, 90, 0.55);
  background-color: color-mix(
      in srgb,
      var(--secondary-background) 55%,
      transparent
  );

  border: 1px solid rgba(175, 130, 24, 0.7);
  border-radius: 8px;

  transition:
      background-color 150ms ease,
      border-color 150ms ease,
      box-shadow 150ms ease;
}

.card:hover {
  background-color: rgba(184, 143, 90, 0.72);
  background-color: color-mix(
      in srgb,
      var(--secondary-background) 72%,
      transparent
  );

  border-color: var(--primary-accent);
  box-shadow: 0 3px 10px rgba(47, 36, 24, 0.18);
}

.cardName {
  flex: 1;
  min-width: 0;

  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;

  font-family: var(
      --primary-text,
      "Manrope",
      "Avenir Next",
      Avenir,
      system-ui,
      sans-serif
  );
  font-size: 0.95rem;
  font-weight: 600;
  line-height: 1.2;
}

.cardActions {
  flex: 0 0 auto;

  display: flex;
  align-items: center;
  gap: 3px;
}

.cardIcon {
  flex: 0 0 auto;

  padding: 0;

  display: inline-flex;
  align-items: center;
  justify-content: center;

  background-color: transparent;
  border: 1px solid transparent;
  border-radius: 6px;

  cursor: pointer;

  transition:
      background-color 120ms ease,
      border-color 120ms ease,
      transform 120ms ease,
      box-shadow 120ms ease;
}

.cardIcon--edit {
  width: 40px;
  height: 40px;
}

.cardIcon--edit img {
  width: 28px;
  height: 28px;
}

.cardIcon--remove {
  width: 34px;
  height: 34px;
}

.cardIcon--remove img {
  width: 20px;
  height: 20px;
}

.cardIcon:hover {
  background-color: rgba(255, 198, 0, 0.2);
  border-color: rgba(255, 198, 0, 0.55);
}

.cardIcon--remove:hover {
  background-color: rgba(130, 38, 24, 0.16);
  border-color: rgba(130, 38, 24, 0.4);
}

.cardIcon:active {
  transform: scale(0.94);
}

.cardIcon:focus-visible {
  outline: none;
  border-color: var(--primary-accent);
  box-shadow: 0 0 0 2px rgba(255, 198, 0, 0.25);
}

.cardIcon img {
  display: block;
  object-fit: contain;
  pointer-events: none;
}
</style>