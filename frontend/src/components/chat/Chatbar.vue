<script setup lang="ts">

</script>

<template>
  <!-- Chatbox utility: message composer -->
  <div class="w-full h-20 rounded-xl bg-stone-900 border border-stone-800 flex items-center gap-3 px-3">

    <!-- LEFT: placeholder image (≈ 1/10) -->
    <div class="basis-1/10 shrink-0 flex items-center justify-center">
      <div class="w-12 h-12 rounded-lg overflow-hidden bg-amber-700/80 ring-1 ring-amber-500/30">
        <img
            src="https://picsum.photos/96"
            alt="placeholder"
            class="w-full h-full object-cover"
        />
      </div>
    </div>

    <!-- MIDDLE: message input area (≈ 9/10) -->
    <div class="basis-9/10 flex-1 h-full flex items-center">
      <div class="w-full h-12 rounded-lg bg-stone-800/80 border border-stone-700/70 px-3 flex items-center">
        <!-- Placeholder text only; wire to textarea/input later -->
        <div class="w-full text-stone-300/60 text-sm select-none">
          Write a message…
        </div>
      </div>
    </div>

    <!-- RIGHT: 3 buttons (≈ 1/10) -->
    <div class="basis-1/10 shrink-0 flex items-center justify-end gap-2">
      <button
          type="button"
          class="h-10 w-10 rounded-lg bg-stone-800 border border-stone-700 text-amber-300 hover:bg-stone-700/80"
          aria-label="Attach"
      >
        +
      </button>

      <button
          type="button"
          class="h-10 w-10 rounded-lg bg-amber-700/90 border border-amber-600 text-stone-950 hover:bg-amber-600"
          aria-label="Send"
      >
        →
      </button>
    </div>
  </div>
</template>

<style scoped>

</style>