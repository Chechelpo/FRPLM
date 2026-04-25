<script setup lang="ts">
import { ref } from "vue";

const props = defineProps<{
  title: string;
  initiallyOpen?: boolean;
  disabled?: boolean;
}>();

const open = ref(props.initiallyOpen ?? false);

function toggle() {
  if (!props.disabled) {
    open.value = !open.value;
  }
}
</script>

<template>
  <div
      class="
      rounded-md
      border border-slate-700
      bg-stone-500
    "
  >
    <!-- Header -->
    <div
        class="
        flex items-center gap-2
        px-3 py-2
        cursor-pointer select-none
        text-slate-300
        transition-colors
        hover:bg-amber-500/10
        hover:text-amber-300
      "
        :class="{
        'opacity-50 cursor-not-allowed': disabled,
        'bg-stone-700 text-slate-200': open
      }"
        @click="toggle"
    >
      <!-- Chevron -->
      <span
          class="
          w-4
          text-slate-500
          transition-transform
        "
          :class="open ? 'rotate-90 text-amber-400' : ''"
      >
        ▶
      </span>

      <!-- Title -->
      <span class="font-medium">
        {{ title }}
      </span>
    </div>

    <!-- Content -->
    <div
        v-show="open"
        class="
        border-t border-slate-700
        bg-stone-400
        px-4 py-3
        text-slate-300
      "
    >
      <slot />
    </div>
  </div>
</template>
