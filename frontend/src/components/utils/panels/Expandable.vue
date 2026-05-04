<script setup lang="ts">
import {ref} from "vue";

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
      class=""
  >
    <!-- Header -->
    <div
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
          transition-colors
        "
                :class="open ? 'text-amber-400' : ''"
            >
        {{ open ? "▼" : "▶" }}
      </span>

      <!-- Title -->
      <span class="font-medium">
        {{ title }}
      </span>
    </div>

    <!-- Content -->
    <div
        v-show="open"
    >
      <slot/>
    </div>
  </div>
</template>
