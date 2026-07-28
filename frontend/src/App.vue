<script setup lang="ts">
import {
  RouterLink,
  RouterView,
  type RouteLocationRaw,
} from "vue-router";

import { route_to } from "@/app/router";
import { EntityTypes } from "@/domain/EntityTypes";

import GlobalError from "@/components/errors/GlobalError.vue";
import Support from "@/components/utils/Support.vue";

interface NavigationItem {
  label: string;
  to: RouteLocationRaw;
  icon?: string;
  exact?: boolean;
}

const navigationItems: NavigationItem[] = [
  {
    label: "Sessions",
    to: "/",
    icon: "/header/session.png",
    exact: true,
  },
  {
    label: "Characters",
    to: route_to(EntityTypes.CHARACTERS),
    icon: "/header/Characters.png",
  },
  {
    label: "Worlds",
    to: route_to(EntityTypes.WORLDS),
    icon: "/header/globe.png",
  },
  {
    label: "Connections",
    to: route_to(EntityTypes.LLM),
    icon: "/header/Connection.png",
  },
  {
    label: "Settings",
    to: "/config",
  },
];
</script>

<template>
  <div class="app-shell">
    <div
        class="app-background"
        aria-hidden="true"
    />

    <header class="app-header">
      <div class="app-header__inner">
        <RouterLink
            to="/"
            class="app-brand"
            aria-label="Open sessions"
        >
          <span
              class="app-brand__mark"
              aria-hidden="true"
          >
            <svg viewBox="0 0 24 24">
              <path d="M12 3 4 7v10l8 4 8-4V7Z" />
              <path d="m4 7 8 4 8-4" />
              <path d="M12 11v10" />
            </svg>
          </span>

          <span class="app-brand__text">
            <span class="app-brand__name">
              Sessions
            </span>

            <span class="app-brand__description">
              Narrative workspace
            </span>
          </span>
        </RouterLink>

        <nav
            class="app-nav"
            aria-label="Main navigation"
        >
          <RouterLink
              v-for="item in navigationItems"
              :key="item.label"
              :to="item.to"
              custom
              v-slot="{
              href,
              navigate,
              isActive,
              isExactActive,
            }"
          >
            <a
                :href="href"
                class="app-nav__link"
                :class="{
                'app-nav__link--active':
                  item.exact
                    ? isExactActive
                    : isActive,
              }"
                :aria-label="item.label"
                :aria-current="
                (
                  item.exact
                    ? isExactActive
                    : isActive
                )
                  ? 'page'
                  : undefined
              "
                :title="item.label"
                @click="navigate"
            >
              <span class="app-nav__icon">
                <img
                    v-if="item.icon"
                    :src="item.icon"
                    alt=""
                />

                <svg
                    v-else-if="item.label === 'Settings'"
                    viewBox="0 0 24 24"
                    aria-hidden="true"
                >
                  <path
                      d="M12 15.5a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7Z"
                  />

                  <path
                      d="M19.4 15a1.7 1.7 0 0 0 .34 1.88l.06.06-2.12 2.12-.06-.06a1.7 1.7 0 0 0-1.88-.34 1.7 1.7 0 0 0-1 1.55V20H9.74v-.09a1.7 1.7 0 0 0-1-1.55 1.7 1.7 0 0 0-1.88.34l-.06.06-2.12-2.12.06-.06A1.7 1.7 0 0 0 5.08 15a1.7 1.7 0 0 0-1.55-1H3.4v-3h.13a1.7 1.7 0 0 0 1.55-1 1.7 1.7 0 0 0-.34-1.88l-.06-.06 2.12-2.12.06.06a1.7 1.7 0 0 0 1.88.34 1.7 1.7 0 0 0 1-1.55V4.7h3v.09a1.7 1.7 0 0 0 1 1.55 1.7 1.7 0 0 0 1.88-.34l.06-.06 2.12 2.12-.06.06A1.7 1.7 0 0 0 19.4 10a1.7 1.7 0 0 0 1.55 1h.13v3h-.13a1.7 1.7 0 0 0-1.55 1Z"
                  />
                </svg>
              </span>

              <span class="app-nav__label">
                {{ item.label }}
              </span>
            </a>
          </RouterLink>

          <Support />
        </nav>
      </div>
    </header>

    <GlobalError />

    <main class="app-content">
      <RouterView v-slot="{ Component }">
        <component
            :is="Component"
            class="app-router-view"
        />
      </RouterView>
    </main>
  </div>
</template>

<style scoped>
.app-shell {
  position: relative;
  isolation: isolate;

  width: 100%;
  min-width: 20rem;
  min-height: 100dvh;

  display: flex;
  flex-direction: column;

  color: rgb(var(--c-fg));
  font-family: var(--font-primary);
}

/* -------------------------------------------------------------------------- */
/* Background                                                                 */
/* -------------------------------------------------------------------------- */

.app-background {
  position: fixed;
  inset: 0;
  z-index: -1;

  pointer-events: none;

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-page) / 0.78),
          rgb(var(--c-page-secondary) / 0.72)
      ),
      url("/background-2.png")
      center /
      cover
      no-repeat;

  background-blend-mode: soft-light;
}

/* -------------------------------------------------------------------------- */
/* Header                                                                     */
/* -------------------------------------------------------------------------- */

.app-header {
  position: sticky;
  top: 0;
  z-index: var(--z-popover);

  flex: 0 0 auto;

  width: 100%;
  box-sizing: border-box;

  padding:
      var(--space-2)
      var(--space-3);

  background:
      linear-gradient(
          180deg,
          rgb(var(--c-page) / 0.92),
          rgb(var(--c-page-secondary) / 0.78)
      );

  border-bottom:
      1px solid
      rgb(var(--c-border) / 0.32);

  box-shadow:
      0 7px 24px
      rgb(var(--c-shadow) / 0.1),
      inset 0 -1px 0
      rgb(255 255 255 / 0.18);

  backdrop-filter: blur(14px);
}

.app-header__inner {
  width: min(100%, 96rem);
  min-width: 0;

  display: flex;
  align-items: center;
  gap: var(--space-4);

  margin: 0 auto;
}

/* -------------------------------------------------------------------------- */
/* Brand                                                                      */
/* -------------------------------------------------------------------------- */

.app-brand {
  flex: 0 0 auto;

  display: inline-flex;
  align-items: center;
  gap: var(--space-2);

  min-width: 0;

  color: rgb(var(--c-fg));
  text-decoration: none;

  border-radius: var(--radius-md);
  outline: 0;
}

.app-brand__mark {
  width: 2.5rem;
  height: 2.5rem;
  flex: 0 0 auto;

  display: grid;
  place-items: center;

  color: rgb(var(--c-primary-strong));

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-accent) / 0.28),
          rgb(var(--c-primary) / 0.14)
      );

  border:
      1px solid
      rgb(var(--c-accent) / 0.42);
  border-radius: var(--radius-md);

  box-shadow:
      inset 0 1px 0
      rgb(255 255 255 / 0.38),
      0 3px 9px
      rgb(var(--c-shadow) / 0.06);
}

.app-brand__mark svg {
  width: 1.35rem;
  height: 1.35rem;

  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.app-brand__text {
  display: flex;
  flex-direction: column;

  min-width: 0;
}

.app-brand__name {
  color: rgb(var(--c-fg-strong));

  font-size: 0.9rem;
  font-weight: 850;
  line-height: 1.2;
}

.app-brand__description {
  color: rgb(var(--c-muted));

  font-size: 0.65rem;
  font-weight: 550;
  line-height: 1.3;
}

.app-brand:focus-visible {
  outline:
      var(--focus-ring-width)
      solid
      rgb(var(--focus-ring-color) / 0.3);

  outline-offset: 3px;
}

/* -------------------------------------------------------------------------- */
/* Navigation                                                                 */
/* -------------------------------------------------------------------------- */

.app-nav {
  flex: 1 1 auto;

  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-1);

  min-width: 0;

  padding: var(--space-1);

  overflow-x: auto;
  overscroll-behavior-inline: contain;

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-surface-raised) / 0.58),
          rgb(var(--c-surface-2) / 0.38)
      );

  border:
      1px solid
      rgb(var(--c-border) / 0.28);
  border-radius: var(--radius-lg);

  box-shadow:
      inset 0 1px 0
      rgb(255 255 255 / 0.3),
      0 4px 14px
      rgb(var(--c-shadow) / 0.05);

  scrollbar-width: none;
}

.app-nav::-webkit-scrollbar {
  display: none;
}

.app-nav__link {
  position: relative;

  min-width: 3rem;
  min-height: 2.75rem;
  flex: 0 0 auto;

  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);

  padding:
      var(--space-1)
      var(--space-2);

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
      box-shadow
      var(--duration-fast)
      var(--ease-standard),
      transform
      var(--duration-fast)
      var(--ease-standard);
}

.app-nav__link:hover {
  color: rgb(var(--c-fg-strong));

  background:
      rgb(var(--c-surface-hover) / 0.7);

  border-color:
      rgb(var(--c-primary) / 0.28);

  transform: translateY(-1px);
}

.app-nav__link--active {
  color: rgb(var(--c-primary-strong));

  background:
      linear-gradient(
          145deg,
          rgb(var(--c-accent) / 0.22),
          rgb(var(--c-primary) / 0.11)
      );

  border-color:
      rgb(var(--c-accent) / 0.46);

  box-shadow:
      inset 0 1px 0
      rgb(255 255 255 / 0.34),
      0 3px 9px
      rgb(var(--c-shadow) / 0.055);
}

.app-nav__link--active::after {
  content: "";

  position: absolute;
  right: 0.75rem;
  bottom: 0.16rem;
  left: 0.75rem;

  height: 2px;

  background:
      rgb(var(--c-accent));

  border-radius: var(--radius-round);
}

.app-nav__link:focus-visible {
  border-color:
      rgb(var(--c-accent) / 0.68);

  box-shadow:
      0 0 0
      var(--focus-ring-width)
      rgb(var(--focus-ring-color) / 0.2);
}

.app-nav__link:active {
  transform: scale(0.97);
}

/* -------------------------------------------------------------------------- */
/* Navigation icons                                                           */
/* -------------------------------------------------------------------------- */

.app-nav__icon {
  width: 2rem;
  height: 2rem;
  flex: 0 0 auto;

  display: grid;
  place-items: center;

  padding: 0.22rem;
  box-sizing: border-box;

  background:
      rgb(var(--c-surface-raised) / 0.52);

  border:
      1px solid
      rgb(var(--c-border) / 0.18);
  border-radius: var(--radius-sm);
}

.app-nav__link--active
.app-nav__icon {
  background:
      rgb(var(--c-accent) / 0.14);

  border-color:
      rgb(var(--c-accent) / 0.28);
}

.app-nav__icon img {
  width: 100%;
  height: 100%;

  object-fit: contain;
  pointer-events: none;
}

.app-nav__icon svg {
  width: 1.15rem;
  height: 1.15rem;

  fill: none;
  stroke: currentColor;
  stroke-width: 1.7;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.app-nav__label {
  display: none;

  font-size: 0.76rem;
  font-weight: 750;
  line-height: 1;

  white-space: nowrap;
}

/* -------------------------------------------------------------------------- */
/* Content                                                                    */
/* -------------------------------------------------------------------------- */

.app-content {
  position: relative;
  z-index: 1;

  flex: 1 1 auto;

  width: 100%;
  min-width: 0;
  min-height: 0;
  box-sizing: border-box;
}

.app-router-view {
  width: 100%;
  min-width: 0;
  min-height: 100%;
  box-sizing: border-box;
}

/* -------------------------------------------------------------------------- */
/* Responsive                                                                 */
/* -------------------------------------------------------------------------- */

@media (min-width: 760px) {
  .app-nav__label {
    display: inline;
  }

  .app-nav__link {
    min-width: auto;
    padding-right: var(--space-3);
  }
}

@media (max-width: 620px) {
  .app-header {
    padding: var(--space-2);
  }

  .app-header__inner {
    gap: var(--space-2);
  }

  .app-brand__text {
    display: none;
  }

  .app-nav {
    justify-content: flex-start;
  }

  .app-nav__link {
    min-width: 2.75rem;
    min-height: 2.6rem;

    padding: var(--space-1);
  }

  .app-nav__icon {
    width: 1.9rem;
    height: 1.9rem;
  }
}

@media (max-width: 390px) {
  .app-brand {
    display: none;
  }

  .app-nav {
    width: 100%;
  }
}

@media (prefers-reduced-motion: reduce) {
  .app-nav__link {
    transition: none;
  }

  .app-nav__link:hover {
    transform: none;
  }
}
</style>