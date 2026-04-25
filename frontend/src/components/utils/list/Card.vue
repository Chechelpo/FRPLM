<script setup lang="ts" generic="
Data extends DataRecord,
EntityType extends EntityABS<any, Data>
">
import {onMounted, ref} from "vue";
import AvatarDisplay from "@/components/utils/img/AvatarDisplay.vue";
import {EntityABS} from "@/frameworks/entities/EntityABS";
import {DataRecord, KeyRecord} from "@/types/DTOs";
import {CommonFields} from "@/utils/CommonFields";

const props = defineProps<{
  character: EntityType
  hasDescription: boolean
}>()
let description: string | null = null;

/**
 * Emits events:
 * <pre>
 * 1. Open (touching the char's name)
 * </pre>
 */
const emit = defineEmits({
  edit: (element: EntityType) => true,
  remove: (element: EntityType) => true
})
const expanded = ref(false); //Triggers visible description

function toggleExpanded() {
  expanded.value = !expanded.value;
}

function onEdit() {
  emit("edit", props.character)
}

function onRemove() {
  const ok = window.confirm(`Are you sure you want to delete "${props.character.getCommon(CommonFields.NAME)}"?`)
  if (ok) emit("remove", props.character)
}

onMounted(() => {
  if (!props.character.hasAttribute(CommonFields.NAME))
    throw new Error(`Character of ${props.character.getEntityType} must specify a name to be shown in a card`);
  if (props.hasDescription)
    description = props.character.getCommon(CommonFields.DESCRIPTION) as string; //Prob safe cast, description must be a string
})
</script>

<template>
  <div class="card">
    <div>
      {{ props.character.getCommon(CommonFields.NAME) }}
    </div>

    <div>
      <button
          type="button"
          class="card_icon"
          @click.stop="onEdit"
      >
        <img src="/icons/edit.png" alt="edit" />
      </button>

      <button
          type="button"
          class="card_icon"
          @click.stop="onRemove"
      >
        <img src="/characterCard/Trash.png" alt="remove" />
      </button>
    </div>
  </div>
</template>

<style scoped>
.card {
  width: 100%;
  height: 50px;
  display: flex;

  flex-direction: row;
  align-items: center;
  justify-content: space-between;

  border-radius: 5px;
  border: 1px solid var(--primary-accent);
  opacity:0.7;
  background-color: var(--primary);
}

.card:hover {
  opacity: 1;
  background-color: var(--primary-accent);
}

.card_icon img {
  width: 40px;
  height: 40px;
  object-fit: contain;
}
</style>
