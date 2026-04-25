<script setup lang="ts">
import { ref, onMounted, onUnmounted } from "vue";
/*
const props = defineProps<{
  editable?: boolean;
}>();

const avatarResource = ref<AvatarResource | null>(null);
const avatarUrl = ref<string>("/characterCard/default.png");

const fileInput = ref<HTMLInputElement | null>(null);

function triggerFileInput(): void {
  if (props.editable) {
    fileInput.value?.click();
  }
}

async function onFileSelected(e: Event): Promise<void> {
  const input = e.target as HTMLInputElement;
  if (!input.files || input.files.length === 0) return;

  const file = input.files[0];

  const formData = new FormData();
  formData.append("charID", String(props.owner.id));
  formData.append("avatar", file);

  await setAvatar(props.owner, formData);

  // Refresh avatar after upload
  avatarResource.value?.revoke();
  avatarResource.value = await fetchAvatarResource(props.owner);
  avatarUrl.value =
      avatarResource.value?.url ?? "/icons/default.png";

  input.value = ""; // allow reselecting same file
}

onMounted(async () => {
  avatarResource.value = await fetchAvatarResource(props.owner);
  avatarUrl.value =
      avatarResource.value?.url ?? "/icons/default.png";
});

onUnmounted(() => {
  avatarResource.value?.revoke();
});*/
</script>

<template>
  <!--
  <button
      type="button"
      class="relative w-full h-full aspect-square
         rounded-full overflow-hidden
         bg-amber-700/70 ring-1 ring-amber-500/25
         flex items-center justify-center"
      :class="editable ? 'cursor-pointer' : 'cursor-default'"
      @click="triggerFileInput"
      :disabled="!editable"
      aria-label="Avatar"
  >
    <img
        :src="avatarUrl"
        alt="avatar"
        class="w-full h-full object-cover"
    />

    <input
        v-if="editable"
        ref="fileInput"
        type="file"
        accept="image/png,image/jpeg,image/webp"
        class="hidden"
        @change="onFileSelected"
    />
  </button>
  -->
</template>

<style scoped>

</style>