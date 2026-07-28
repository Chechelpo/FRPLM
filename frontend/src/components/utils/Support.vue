<script setup lang="ts">
import { ref } from "vue";

import WindowPrompt from "@/components/utils/prompts/WindowPrompt.vue";

interface SupportLink {
  label: string;
  url: string;
  description?: string;
}

const isSupportOpen = ref(false);

const supportLinks: SupportLink[] = [
  {
    label: "GitHub",
    url: "https://github.com/chechelpo",
    description: "Source code & issues. Please star the main project",
  },
  {
    label: "Patreon",
    url : "https://patreon.com/Simulith?utm_medium=unknown&utm_source=join_link&utm_campaign=creatorshare_creator&utm_content=copyLink",
    description: "The best way to support my work and keep up to date with new features"
  },
  {
    label: "Kofi",
    url : "https://ko-fi.com/chechelpo",
    description: "An alternative to patreon"
  },
  {
    label: "Email",
    url: "mailto:felipeyelpo26@gmail.com",
    description: "Direct contact",
  },
];

function openSupport(): void {
  isSupportOpen.value = true;
}

function closeSupport(): void {
  isSupportOpen.value = false;
}
</script>

<template>
  <button
      type="button"
      class="support-nav-link"
      aria-label="Support"
      title="Support"
      @click="openSupport"
  >
    <span class="support-nav-icon">
      <svg
          viewBox="0 0 24 24"
          aria-hidden="true"
      >
        <path
            d="M12 14a4 4 0 0 0-4 4v2h8v-2a4 4 0 0 0-4-4Z"
        />
        <path
            d="M12 2a5 5 0 0 0-5 5 5 5 0 0 0 10 0 5 5 0 0 0-5-5Z"
        />
        <path
            d="M4 22h16"
        />
      </svg>
    </span>

    <span class="support-nav-label">
      Support
    </span>
  </button>

  <WindowPrompt
      v-if="isSupportOpen"
      title="Support & Links"
      info="Connect with us on socials and platforms."
      @close="closeSupport"
  >
    <div class="support-content">
      <p class="support-intro">
        Thanks for using Frplm! Here are a couple ways you can support the project:
      </p>

      <ul class="support-links">
        <li
            v-for="link in supportLinks"
            :key="link.label"
            class="support-link-item"
        >
          <a
              :href="link.url"
              class="support-link"
              target="_blank"
              rel="noopener noreferrer"
          >
            <span class="support-link__label">
              {{ link.label }}
            </span>

            <span
                v-if="link.description"
                class="support-link__description"
            >
              {{ link.description }}
            </span>
          </a>
        </li>
      </ul>
    </div>
  </WindowPrompt>
</template>

<style scoped>
.support-nav-link {
  position: relative;

  min-width: 3rem;
  min-height: 2.75rem;
  flex: 0 0 auto;

  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);

  padding: var(--space-1) var(--space-2);
  box-sizing: border-box;

  color: rgb(var(--c-muted));

  background: transparent;
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  outline: 0;

  font: inherit;
  text-decoration: none;

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

.support-nav-link:hover {
  color: rgb(var(--c-fg-strong));

  background: rgb(var(--c-surface-hover) / 0.7);

  border-color: rgb(var(--c-primary) / 0.28);

  transform: translateY(-1px);
}

.support-nav-link:active {
  transform: scale(0.97);
}

.support-nav-link:focus-visible {
  border-color: rgb(var(--c-accent) / 0.68);

  box-shadow:
      0 0 0
      var(--focus-ring-width)
      rgb(var(--focus-ring-color) / 0.2);
}

/* -------------------------------------------------------------------------- */
/* Nav icon                                                                   */
/* -------------------------------------------------------------------------- */

.support-nav-icon {
  width: 2rem;
  height: 2rem;
  flex: 0 0 auto;

  display: grid;
  place-items: center;

  padding: 0.22rem;
  box-sizing: border-box;

  background: rgb(var(--c-surface-raised) / 0.52);

  border: 1px solid rgb(var(--c-border) / 0.18);
  border-radius: var(--radius-sm);
}

.support-nav-icon svg {
  width: 1.15rem;
  height: 1.15rem;

  fill: none;
  stroke: currentColor;
  stroke-width: 1.7;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.support-nav-label {
  display: none;

  font-size: 0.76rem;
  font-weight: 750;
  line-height: 1;

  white-space: nowrap;
}

@media (min-width: 760px) {
  .support-nav-label {
    display: inline;
  }

  .support-nav-link {
    min-width: auto;
    padding-right: var(--space-3);
  }
}

/* -------------------------------------------------------------------------- */
/* Prompt content                                                             */
/* -------------------------------------------------------------------------- */

.support-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);

  padding: var(--space-3);
}

.support-intro {
  margin: 0;

  color: rgb(var(--c-muted));

  font-size: 0.85rem;
  line-height: 1.5;
}

.support-links {
  list-style: none;
  margin: 0;
  padding: 0;

  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.support-link-item {
  border: 1px solid rgb(var(--c-border) / 0.28);
  border-radius: var(--radius-md);

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-surface-raised) / 0.58),
          rgb(var(--c-surface-2) / 0.38)
      );

  transition:
      border-color
      var(--duration-fast)
      var(--ease-standard),
      transform
      var(--duration-fast)
      var(--ease-standard);
}

.support-link-item:hover {
  border-color: rgb(var(--c-primary) / 0.28);

  transform: translateY(-1px);
}

.support-link {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;

  padding: var(--space-2) var(--space-3);

  color: rgb(var(--c-fg));
  text-decoration: none;

  outline: 0;
}

.support-link:focus-visible {
  outline:
      var(--focus-ring-width)
      solid
      rgb(var(--focus-ring-color) / 0.3);

  outline-offset: 2px;
}

.support-link__label {
  color: rgb(var(--c-fg-strong));

  font-size: 0.9rem;
  font-weight: 750;
}

.support-link__description {
  color: rgb(var(--c-muted));

  font-size: 0.72rem;
  font-weight: 500;
  line-height: 1.3;
}
</style>