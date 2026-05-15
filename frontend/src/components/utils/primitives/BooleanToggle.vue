<script setup lang="ts">
const model = defineModel<boolean>({
  required: true,
})

const props = withDefaults(
    defineProps<{
      read_only?: boolean
    }>(),
    {
      read_only: false,
    }
)

const emit = defineEmits<{
  edit: [payload: boolean]
}>()

function onToggle(event: Event): void {
  if (props.read_only) return

  const checked = (event.currentTarget as HTMLInputElement).checked

  model.value = checked
  emit("edit", checked)
}
</script>

<template>
  <label
      class="toggle"
      :class="{ 'toggle--disabled': read_only }"
  >
    <input
        class="toggle__input"
        type="checkbox"
        :checked="model"
        :disabled="read_only"
        @change="onToggle"
    />

    <span class="toggle__track">
      <span class="toggle__thumb" />
    </span>
  </label>
</template>

<style scoped>
.toggle {
  display: inline-flex;
  align-items: center;
  cursor: pointer;
  user-select: none;
}

.toggle--disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.toggle__input {
  position: absolute;
  opacity: 0;
  pointer-events: none;
}

.toggle__track {
  position: relative;
  width: 2.5rem;
  height: 1.4rem;
  border-radius: 999px;
  background: #9ca3af;
  transition: background 120ms ease;
}

.toggle__thumb {
  position: absolute;
  top: 0.2rem;
  left: 0.2rem;
  width: 1rem;
  height: 1rem;
  border-radius: 50%;
  background: white;
  transition: transform 120ms ease;
}

.toggle__input:checked + .toggle__track {
  background: #111827;
}

.toggle__input:checked + .toggle__track .toggle__thumb {
  transform: translateX(1.1rem);
}

.toggle__input:focus-visible + .toggle__track {
  outline: 2px solid #2563eb;
  outline-offset: 2px;
}
</style>