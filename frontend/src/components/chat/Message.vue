<script setup lang="ts">
import {onMounted} from "vue";


onMounted(() => {

})
</script>

<template>
  <div class="grid rounded-l-full grid-cols-[1fr_4fr] gap-4 w-full p-3 bg-gray-500">

    <!-- Avatar column -->
    <!-- FIXED AVATAR (never resizes or deforms) -->
    <div class="w-32 h-32 rounded-full overflow-hidden shrink-0 bg-blue-700">
      <img
          src="https://picsum.photos/200"
          class="w-fill h-fill object-cover"
          alt="avatar"
      />
    </div>
    <!-- FLEXIBLE MESSAGE AREA -->
    <div class="flex flex-col flex-grow relative">

      <!-- speaker name -->
      <div class="text-lg font-semibold text-white mb-1">
        Char Name
      </div>

      <!-- message text to FORMAT (*txt* = grey ; "txt" = orange ; **txt** = bold and also add \n formatting) -->
      <div class="text-white leading-relaxed text-sm"
           v-html = ""
      > </div>

      <!-- SMALL ICON + COUNTER IN BOTTOM RIGHT (to later add mesagge regen) -->
      <div class="absolute bottom-0 right-0 flex items-center gap-1 text-xs text-white opacity-80">
        <!-- counter -->
        <span>3 / 12</span>
        <!-- tiny image -->
        <button class = "cursor-pointer">
          <img
              src="/right-arrow.png"
              class="w-4 h-4 object-cover rounded"
              alt="tiny"
          />
        </button>
      </div>

    </div>
  </div>
</template>

<style scoped>

</style>