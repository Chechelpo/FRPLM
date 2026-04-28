<template>
  <div class="app-shell">
    <header class="app-header">
      <nav class="nav-links" aria-label="Main navigation">
        <div>
          <RouterLink :to="route_to(EntityTypes.CHARACTERS)" class="nav-icon" aria-label="Characters">
            <img src="/header/Characters.png" alt="Characters" />
          </RouterLink>
        </div>

        <div>
          <RouterLink to="/lorebook" class="nav-icon" aria-label="Lorebook">
            <img src="/header/lorebook.png" alt="Lorebook" />
          </RouterLink>
        </div>

        <div>
          <RouterLink :to="route_to(EntityTypes.WORLDS)" class="nav-icon" aria-label="Worlds">
            <img src="/header/globe.png" alt="Worlds" />
          </RouterLink>
        </div>

        <div>
          <RouterLink to="/llm" class="nav-icon" aria-label="Connections">
            <img src="/header/Connection.png" alt="Connections" />
          </RouterLink>
        </div>
      </nav>
    </header>

    <div class="main-background"></div>

    <main class="content">
      <RouterView class = "router-view" />
    </main>
  </div>
</template>

<script setup lang="ts">
import { RouterLink, RouterView } from "vue-router";
import {route_to} from "@/app/router";
import {EntityTypes} from "@/domain/entities/EntityTypes";
</script>

<style scoped>
.app-shell {
  --header-height: 40px;
  --gap-base: 4rem;

  min-height: 100vh;
  display: flex;
  flex-direction: column;
  gap: var(--header-height);
}

.main-background {
  position: fixed;
  inset: 0;
  background: url("/background-2.png") center / cover no-repeat;
  z-index: 0;
}
.router-view {
  height: 100%;
  width: 100%;
}

.app-header,
.content {
  position: relative;
  z-index: 1;
}

.app-header {
  height: var(--header-height);
  display: flex;
  justify-content: space-around;
}

.content {
  flex: 1;
  min-height: 0;
}

.nav-links {
  height: 100%;
  width: 100%;
  margin: 10px;

  border-radius: 30px;
  border: 1px solid antiquewhite;
  background-color: #dfae7c;

  display: flex;
  align-items: center;
  justify-content: space-around;
  gap: var(--gap-base);
}

.nav-icon {
  width: 40px;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  text-decoration: none;
  transition: color 0.15s ease, transform 0.15s ease;
}

.nav-icon:hover {
  transform: translateY(-1px);
}

.nav-icon img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

@media (min-width: 640px) {
  .app-shell {
    --gap-base: 5rem;
  }
}

@media (min-width: 1024px) {
  .app-shell {
    --gap-base: 6.5rem;
  }
}
</style>