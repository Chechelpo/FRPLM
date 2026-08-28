<script setup lang="ts">
import {     BooleanToggle } from "@frplm/ui";

import {
  computed,
  ref,
  watch,
} from "vue";

import {
  creativeCssActive,
  customTheme,
  exportCustomThemeJson,
  importCustomThemeJson,
  resolvedTheme,
  resetCustomTheme,
  setCustomCssEnabled,
  setCustomThemeBase,
  setCustomThemeCss,
  setCustomThemeName,
  setCustomThemeTokens,
  setThemePreference,
  themePreference,
  type BaseTheme,
  type ThemePreference,
} from "@core/theme";

import Expandable from "@components/utils/panels/Expandable.vue";

interface ThemeOption {
  value: ThemePreference;
  label: string;
  description: string;
}

const themes: ThemeOption[] = [
  {
    value: "system",
    label: "System",
    description:
        "Follow the operating system appearance.",
  },
  {
    value: "warm",
    label: "Warm",
    description:
        "Orange and white.",
  },
  {
    value: "dark",
    label: "Dark",
    description:
        "Dark wood, charred paper, and candlelight.",
  },
  {
    value: "custom",
    label: "Custom",
    description:
        "Apply your own CSS color variables.",
  },
];

const paletteInput =
    ref<HTMLInputElement | null>(null);

const cssInput =
    ref<HTMLInputElement | null>(null);

const paletteDraft = ref("");
const cssDraft = ref("");

const paletteError =
    ref<string | null>(null);

const paletteStatus =
    ref<string | null>(null);

const cssError =
    ref<string | null>(null);

const cssStatus =
    ref<string | null>(null);

const customTokenCount = computed(
    () =>
        Object.keys(
            customTheme.value.tokens,
        ).length,
);

const customThemeIsActive = computed(
    () =>
        themePreference.value ===
        "custom",
);
const colorSetExample = `{
  "--c-page": "72 42 28",
  "--c-surface": "194 146 96",
  "--c-surface-raised": "218 178 126",
  "--c-fg": "49 30 20",
  "--c-accent": "221 112 38",
  "--radius-md": "0.65rem"
}`;

const completeThemeExample = `{
  "version": 1,
  "name": "Burnt ledger",
  "base": "dark",
  "tokens": {
    "--c-page": "25 15 10",
    "--c-surface": "73 43 27",
    "--c-fg": "232 202 162",
    "--c-accent": "226 112 37"
  },
  "cssEnabled": true,
  "css": ".edit-box { transform: rotate(-0.1deg); }"
}`;

const creativeCssExample = `:root {
  --radius-md: 0.35rem;
  --font-primary: Georgia, serif;
}

.edit-box {
  border-style: dashed;
  box-shadow:
    4px 5px 0 rgb(var(--c-shadow) / 0.18);
}

.app-background {
  filter: sepia(0.35) contrast(1.08);
}

.chat-message {
  transform: rotate(-0.12deg);
}`;

const helpCopyStatus = ref<string | null>(null);

async function copyCustomizationExample(
    source: string,
    label: string,
): Promise<void> {
  try {
    await navigator.clipboard.writeText(source);

    helpCopyStatus.value =
        `${label} copied to the clipboard.`;
  } catch (error) {
    console.error(
        "Could not copy customization example",
        error,
    );

    helpCopyStatus.value =
        "The example could not be copied.";
  }
}

watch(
    customTheme,
    (theme) => {
      paletteDraft.value =
          JSON.stringify(
              theme.tokens,
              null,
              2,
          );

      cssDraft.value = theme.css;
    },
    {
      immediate: true,
      deep: true,
    },
);

function isRecord(
    value: unknown,
): value is Record<string, unknown> {
  return (
      typeof value === "object" &&
      value !== null &&
      !Array.isArray(value)
  );
}

function changeTheme(
    theme: ThemePreference,
): void {
  setThemePreference(theme);
}

function changeCustomBase(
    base: BaseTheme,
): void {
  setCustomThemeBase(base);
}

function updateThemeName(
    event: Event,
): void {
  const input =
      event.currentTarget as HTMLInputElement;

  setCustomThemeName(input.value);
}

function openPalettePicker(): void {
  paletteError.value = null;
  paletteInput.value?.click();
}

function openCssPicker(): void {
  cssError.value = null;
  cssInput.value?.click();
}

async function importPalette(
    event: Event,
): Promise<void> {
  const input =
      event.currentTarget as HTMLInputElement;

  const file = input.files?.[0];

  if (!file) {
    return;
  }

  paletteError.value = null;
  paletteStatus.value = null;

  try {
    const source = await file.text();

    importCustomThemeJson(source);
    setThemePreference("custom");

    paletteStatus.value =
        `Imported ${file.name}.`;
  } catch (error) {
    console.error(
        "Could not import theme",
        error,
    );

    paletteError.value =
        error instanceof Error
            ? error.message
            : "The theme file is invalid.";
  } finally {
    input.value = "";
  }
}

function applyPaletteDraft(): void {
  paletteError.value = null;
  paletteStatus.value = null;

  try {
    const parsed =
        JSON.parse(
            paletteDraft.value,
        ) as unknown;

    const tokens =
        isRecord(parsed) &&
        isRecord(parsed.tokens)
            ? parsed.tokens
            : parsed;

    if (!isRecord(tokens)) {
      throw new Error(
          "The color set must be a JSON object.",
      );
    }

    setCustomThemeTokens(tokens);
    setThemePreference("custom");

    paletteStatus.value =
        "Color variables applied.";
  } catch (error) {
    paletteError.value =
        error instanceof Error
            ? error.message
            : "The color set is invalid.";
  }
}

function exportCustomTheme(): void {
  const source =
      exportCustomThemeJson();

  const blob = new Blob(
      [source],
      {
        type: "application/json",
      },
  );

  const url =
      URL.createObjectURL(blob);

  const link =
      document.createElement("a");

  const safeName =
      customTheme.value.name
          .replace(
              /[<>:"/\\|?*\u0000-\u001F]/g,
              "_",
          )
          .trim() || "custom-theme";

  link.href = url;
  link.download = `${safeName}.json`;

  document.body.appendChild(link);
  link.click();
  link.remove();

  URL.revokeObjectURL(url);
}

function resetTheme(): void {
  resetCustomTheme();

  paletteError.value = null;
  cssError.value = null;

  paletteStatus.value =
      "Custom theme reset.";

  cssStatus.value = null;
}

function toggleCreativeCss(
    enabled: boolean,
): void {
  setCustomCssEnabled(enabled);

  cssStatus.value = enabled
      ? "Creative CSS enabled."
      : "Creative CSS disabled.";
}

function applyCssDraft(): void {
  cssError.value = null;

  setCustomThemeCss(
      cssDraft.value,
  );

  cssStatus.value =
      "Custom CSS applied.";
}

async function importCss(
    event: Event,
): Promise<void> {
  const input =
      event.currentTarget as HTMLInputElement;

  const file = input.files?.[0];

  if (!file) {
    return;
  }

  cssError.value = null;
  cssStatus.value = null;

  try {
    const source = await file.text();

    cssDraft.value = source;
    setCustomThemeCss(source);

    cssStatus.value =
        `Imported ${file.name}.`;
  } catch (error) {
    console.error(
        "Could not import CSS",
        error,
    );

    cssError.value =
        "The CSS file could not be read.";
  } finally {
    input.value = "";
  }
}

function clearCss(): void {
  cssDraft.value = "";
  setCustomThemeCss("");

  cssError.value = null;
  cssStatus.value =
      "Custom CSS cleared.";
}

function onCssKeydown(
    event: KeyboardEvent,
): void {
  if (
      (event.ctrlKey || event.metaKey) &&
      event.key === "Enter"
  ) {
    event.preventDefault();
    applyCssDraft();
  }
}
</script>

<template>
  <section
      class="
      settings
      edit-box
      edit-box--accent
    "
  >
    <input
        ref="paletteInput"
        type="file"
        class="settings__file-input"
        accept=".json,application/json"
        @change="importPalette"
    />

    <input
        ref="cssInput"
        type="file"
        class="settings__file-input"
        accept=".css,text/css"
        @change="importCss"
    />

    <header class="edit-box__header">
      <div
          class="edit-box__header-icon"
          aria-hidden="true"
      >
        <svg viewBox="0 0 24 24">
          <circle
              cx="13.5"
              cy="6.5"
              r="2.5"
          />

          <circle
              cx="6.5"
              cy="10.5"
              r="2.5"
          />

          <circle
              cx="9.5"
              cy="18"
              r="2.5"
          />

          <path
              d="M13 3a9 9 0 1 0 8 13c.6-1.3-.5-2.5-1.9-2.3l-1.5.2a2 2 0 0 1-2.2-2.7l.8-1.9A4.6 4.6 0 0 0 13 3Z"
          />
        </svg>
      </div>

      <div class="edit-box__header-main">
        <span class="edit-box__eyebrow">
          Preferences
        </span>

        <div class="edit-box__title-row">
          <h1 class="edit-box__title">
            Appearance
          </h1>

          <span
              class="
              edit-box__badge
              edit-box__badge--neutral
            "
          >
            {{ resolvedTheme }}
          </span>
        </div>

        <p class="edit-box__description">
          Select a built-in theme, import a color
          set, or replace the interface styling
          with your own CSS.
        </p>
      </div>
    </header>

    <div
        class="
        edit-box__body
        edit-box__stack
      "
    >
      <!-- Theme selection -->
      <section class="edit-box__section">
        <header class="edit-box__section-header">
          <div class="edit-box__section-heading">
            <h2 class="edit-box__section-title">
              Theme
            </h2>

            <p class="edit-box__section-description">
              Choose the base appearance used
              throughout the application.
            </p>
          </div>
        </header>

        <div
            class="theme-options"
            role="radiogroup"
            aria-label="Application theme"
        >
          <button
              v-for="theme in themes"
              :key="theme.value"
              type="button"
              role="radio"
              class="theme-option"
              :class="{
              'theme-option--selected':
                themePreference === theme.value,
            }"
              :aria-checked="
              themePreference === theme.value
            "
              @click="changeTheme(theme.value)"
          >
            <span
                class="theme-option__preview"
                :class="[
                `theme-option__preview--${
                  theme.value === 'custom'
                    ? customTheme.base
                    : theme.value
                }`,
                {
                  'theme-option__preview--system':
                    theme.value === 'system',
                },
              ]"
                aria-hidden="true"
            >
              <span/>
              <span/>
              <span/>
            </span>

            <span class="theme-option__content">
              <strong class="theme-option__label">
                {{ theme.label }}
              </strong>

              <span class="theme-option__description">
                {{ theme.description }}
              </span>
            </span>

            <span class="theme-option__indicator"/>
          </button>
        </div>
      </section>

      <!-- Usage guide -->
      <Expandable title="Guide" variant="compact">
        <section
            class="
          edit-box__section
          customization-guide
        "
        >
          <header class="edit-box__section-header">
            <div class="edit-box__section-heading">
            <span class="edit-box__eyebrow">
              Reference
            </span>

              <h2 class="edit-box__section-title">
                Colors and creative mayhem
              </h2>

              <p class="edit-box__section-description">
                Color sets override the design
                variables. Raw CSS is injected after
                the application styles and can change
                anything rendered by the application.
              </p>
            </div>
          </header>

          <div class="customization-guide__modes">
            <article class="customization-mode">
              <div class="customization-mode__icon">
                <svg
                    viewBox="0 0 24 24"
                    aria-hidden="true"
                >
                  <path
                      d="M12 3a9 9 0 1 0 9 9"
                  />

                  <path
                      d="M12 3v9h9"
                  />
                </svg>
              </div>

              <div>
                <strong>System</strong>

                <p>
                  Follows the operating system.
                  Imported tokens and raw CSS are not
                  applied.
                </p>
              </div>
            </article>

            <article class="customization-mode">
              <div class="customization-mode__icon">
                <svg
                    viewBox="0 0 24 24"
                    aria-hidden="true"
                >
                  <circle
                      cx="12"
                      cy="12"
                      r="4"
                  />

                  <path d="M12 2v2"/>
                  <path d="M12 20v2"/>
                  <path d="m4.93 4.93 1.42 1.42"/>
                  <path d="m17.65 17.65 1.42 1.42"/>
                  <path d="M2 12h2"/>
                  <path d="M20 12h2"/>
                  <path d="m4.93 19.07 1.42-1.42"/>
                  <path d="m17.65 6.35 1.42-1.42"/>
                </svg>
              </div>

              <div>
                <strong>Warm or Dark</strong>

                <p>
                  Uses the built-in palette. Raw CSS
                  can still be enabled for either
                  theme.
                </p>
              </div>
            </article>

            <article class="customization-mode">
              <div class="customization-mode__icon">
                <svg
                    viewBox="0 0 24 24"
                    aria-hidden="true"
                >
                  <path
                      d="M12 3a9 9 0 1 0 8 13c.6-1.3-.5-2.5-1.9-2.3l-1.5.2a2 2 0 0 1-2.2-2.7l.8-1.9A4.6 4.6 0 0 0 12 3Z"
                  />

                  <circle
                      cx="8"
                      cy="9"
                      r="1"
                  />

                  <circle
                      cx="8"
                      cy="15"
                      r="1"
                  />

                  <circle
                      cx="13"
                      cy="7"
                      r="1"
                  />
                </svg>
              </div>

              <div>
                <strong>Custom</strong>

                <p>
                  Starts with the selected Warm or
                  Dark base and then applies the
                  imported token overrides.
                </p>
              </div>
            </article>
          </div>
          <div class="customization-guide__content">
            <article class="customization-guide__block">
              <header class="customization-guide__block-header">
                <div>
                  <h3>Color set input</h3>

                  <p>
                    Paste a JSON object containing CSS
                    custom properties. Only property
                    names beginning with
                    <code>--</code> are used.
                  </p>
                </div>

                <span
                    class="
                  edit-box__badge
                  edit-box__badge--neutral
                "
                >
                JSON
              </span>
              </header>

              <div class="customization-guide__notice">
                <svg
                    viewBox="0 0 24 24"
                    aria-hidden="true"
                >
                  <circle
                      cx="12"
                      cy="12"
                      r="9"
                  />

                  <path d="M12 11v5"/>
                  <path d="M12 8h.01"/>
                </svg>

                <p>
                  Theme colors are stored as three
                  space-separated RGB channels.
                  Do not include commas or wrap the
                  value in <code>rgb()</code>.
                </p>
              </div>

              <div class="customization-guide__comparison">
                <div
                    class="
                  customization-guide__format
                  customization-guide__format--valid
                "
                >
                  <span>Expected</span>

                  <code>
                    "--c-accent": "224 126 45"
                  </code>
                </div>

                <div
                    class="
                  customization-guide__format
                  customization-guide__format--invalid
                "
                >
                  <span>Not compatible</span>

                  <code>
                    "--c-accent": "rgb(224, 126, 45)"
                  </code>
                </div>
              </div>

              <pre
                  v-pre
                  class="customization-guide__code"
              ><code>{
  "--c-page": "72 42 28",
  "--c-page-secondary": "52 31 22",

  "--c-surface": "194 146 96",
  "--c-surface-2": "171 118 73",
  "--c-surface-raised": "218 178 126",

  "--c-fg": "49 30 20",
  "--c-fg-strong": "34 20 14",
  "--c-muted": "103 69 47",

  "--c-primary": "181 82 27",
  "--c-primary-strong": "128 53 18",
  "--c-accent": "224 126 45",

  "--radius-md": "0.65rem"
}</code></pre>

              <div class="customization-guide__details">
                <div>
                  <strong>Partial sets are valid</strong>

                  <p>
                    You only need to provide the
                    variables you want to change.
                    Everything else comes from the
                    selected base theme.
                  </p>
                </div>

                <div>
                  <strong>Other CSS values are valid</strong>

                  <p>
                    Geometry, spacing, fonts, and
                    motion values can use normal CSS,
                    such as <code>0.5rem</code> or
                    <code>Georgia, serif</code>.
                  </p>
                </div>
              </div>
            </article>

            <article class="customization-guide__block">
              <header class="customization-guide__block-header">
                <div>
                  <h3>Complete theme file</h3>

                  <p>
                    An imported JSON file can also
                    contain theme metadata, the base
                    theme, tokens, and raw CSS.
                  </p>
                </div>

                <span
                    class="
                  edit-box__badge
                  edit-box__badge--neutral
                "
                >
                .json
              </span>
              </header>

              <pre
                  v-pre
                  class="customization-guide__code"
              ><code>{
  "version": 1,
  "name": "Burnt ledger",
  "base": "dark",
  "tokens": {
    "--c-page": "25 15 10",
    "--c-surface": "73 43 27",
    "--c-fg": "232 202 162",
    "--c-accent": "226 112 37"
  },
  "cssEnabled": true,
  "css": ".edit-box { transform: rotate(-0.1deg); }"
}</code></pre>

              <dl class="customization-guide__definition-list">
                <div>
                  <dt>version</dt>

                  <dd>
                    Theme file format. Currently
                    <code>1</code>.
                  </dd>
                </div>

                <div>
                  <dt>name</dt>

                  <dd>
                    Display name used when exporting
                    the theme.
                  </dd>
                </div>

                <div>
                  <dt>base</dt>

                  <dd>
                    Either <code>warm</code> or
                    <code>dark</code>.
                  </dd>
                </div>

                <div>
                  <dt>tokens</dt>

                  <dd>
                    A map of CSS custom properties
                    and their values.
                  </dd>
                </div>

                <div>
                  <dt>cssEnabled</dt>

                  <dd>
                    Whether the raw CSS layer should
                    be active.
                  </dd>
                </div>

                <div>
                  <dt>css</dt>

                  <dd>
                    Arbitrary CSS inserted after the
                    application stylesheet.
                  </dd>
                </div>
              </dl>
            </article>


            <article
                class="
              customization-guide__block
              customization-guide__block--wide
            "
            >
              <header class="customization-guide__block-header">
                <div>
                  <h3>Raw CSS input</h3>

                  <p>
                    Paste ordinary CSS directly into
                    the Creative mayhem editor. No
                    JSON wrapper is required.
                  </p>
                </div>

                <span
                    class="
                  edit-box__badge
                  edit-box__badge--warning
                "
                >
                Unrestricted
              </span>
              </header>

              <pre
                  v-pre
                  class="customization-guide__code"
              ><code>:root {
  --radius-md: 0.35rem;
  --font-primary: Georgia, serif;
}

.edit-box {
  border-style: dashed;
  box-shadow:
    4px 5px 0 rgb(var(--c-shadow) / 0.18);
}

.app-background {
  filter:
    sepia(0.35)
    contrast(1.08);
}

.chat-message {
  transform: rotate(-0.12deg);
}</code></pre>

              <div
                  class="
                customization-guide__notice
                customization-guide__notice--warning
              "
              >
                <svg
                    viewBox="0 0 24 24"
                    aria-hidden="true"
                >
                  <path
                      d="M10.3 3.8 2.2 18a2 2 0 0 0 1.8 3h16a2 2 0 0 0 1.8-3L13.7 3.8a2 2 0 0 0-3.4 0Z"
                  />

                  <path d="M12 9v4"/>
                  <path d="M12 17h.01"/>
                </svg>

                <p>
                  Raw CSS can alter or hide any part
                  of the interface. Clear the editor
                  or disable Creative CSS to restore
                  the normal application styles.
                </p>
              </div>
            </article>
          </div>

          <div class="customization-guide__workflow">
            <div class="customization-guide__step">
              <span>1</span>

              <div>
                <strong>Choose a base</strong>

                <p>
                  Select Warm, Dark, or Custom.
                </p>
              </div>
            </div>

            <div class="customization-guide__step">
              <span>2</span>

              <div>
                <strong>Add overrides</strong>

                <p>
                  Paste color tokens or import a JSON
                  file.
                </p>
              </div>
            </div>

            <div class="customization-guide__step">
              <span>3</span>

              <div>
                <strong>Apply the color set</strong>

                <p>
                  Select Custom to see token
                  overrides.
                </p>
              </div>
            </div>

            <div class="customization-guide__step">
              <span>4</span>

              <div>
                <strong>Enable mayhem</strong>

                <p>
                  Apply the CSS and enable the
                  Creative CSS toggle.
                </p>
              </div>
            </div>
          </div>
        </section>
      </Expandable>

      <!-- Custom color set -->
      <section
          class="
          edit-box__section
          edit-box__section--accent
          custom-theme
        "
      >
        <header class="edit-box__section-header">
          <div class="edit-box__section-heading">
            <span class="edit-box__eyebrow">
              Custom theme
            </span>

            <h2 class="edit-box__section-title">
              Color set
            </h2>

            <p class="edit-box__section-description">
              Override any global CSS custom
              property. The values are applied when
              Custom is selected.
            </p>
          </div>

          <div class="custom-theme__badges">
            <span class="edit-box__badge">
              {{ customTokenCount }}
              variables
            </span>

            <span
                class="
                edit-box__badge
                edit-box__badge--neutral
              "
            >
              Base {{ customTheme.base }}
            </span>

            <span
                v-if="customThemeIsActive"
                class="
                edit-box__badge
                edit-box__badge--success
              "
            >
              Active
            </span>
          </div>
        </header>

        <div class="custom-theme__identity">
          <label class="custom-theme__field">
            <span>Theme name</span>

            <input
                type="text"
                :value="customTheme.name"
                @change="updateThemeName"
            />
          </label>

          <div class="custom-theme__field">
            <span>Base theme</span>

            <div class="custom-theme__base-options">
              <button
                  type="button"
                  :class="{
                  'custom-theme__base--active':
                    customTheme.base === 'warm',
                }"
                  @click="changeCustomBase('warm')"
              >
                Warm
              </button>

              <button
                  type="button"
                  :class="{
                  'custom-theme__base--active':
                    customTheme.base === 'dark',
                }"
                  @click="changeCustomBase('dark')"
              >
                Dark
              </button>
            </div>
          </div>
        </div>

        <textarea
            v-model="paletteDraft"
            class="
            settings__code-editor
            settings__code-editor--palette
          "
            rows="12"
            spellcheck="false"
            aria-label="Custom theme variables"
        />

        <div class="settings__action-row">
          <button
              type="button"
              class="edit-box__action"
              @click="openPalettePicker"
          >
            Import JSON
          </button>

          <button
              type="button"
              class="edit-box__action"
              @click="exportCustomTheme"
          >
            Export JSON
          </button>

          <button
              type="button"
              class="edit-box__action"
              @click="resetTheme"
          >
            Reset
          </button>

          <button
              type="button"
              class="
              edit-box__action
              edit-box__action--accent
            "
              @click="applyPaletteDraft"
          >
            Apply color set
          </button>
        </div>

        <p
            v-if="paletteError"
            class="
            settings__message
            settings__message--error
          "
            role="alert"
        >
          {{ paletteError }}
        </p>

        <p
            v-else-if="paletteStatus"
            class="
            settings__message
            settings__message--success
          "
            role="status"
        >
          {{ paletteStatus }}
        </p>
      </section>

      <!-- Arbitrary CSS -->
      <section
          class="
          edit-box__section
          creative-css
        "
      >
        <header class="edit-box__section-header">
          <div class="edit-box__section-heading">
            <span class="edit-box__eyebrow">
              Creative mayhem
            </span>

            <h2 class="edit-box__section-title">
              Raw CSS
            </h2>

            <p class="edit-box__section-description">
              Inject arbitrary CSS after the
              application stylesheet. It is active
              with Warm, Dark, or Custom and
              inactive under System.
            </p>
          </div>

          <div class="creative-css__toggle">
            <span
                class="edit-box__badge"
                :class="{
                'edit-box__badge--success':
                  creativeCssActive,
                'edit-box__badge--neutral':
                  !creativeCssActive,
              }"
            >
              {{
                creativeCssActive
                    ? "Injected"
                    : "Inactive"
              }}
            </span>

            <BooleanToggle
                :model-value="customTheme.cssEnabled"
                label="Enable arbitrary CSS"
                @edit="toggleCreativeCss"
            />
          </div>
        </header>

        <textarea
            v-model="cssDraft"
            class="settings__code-editor"
            rows="16"
            spellcheck="false"
            placeholder=":root {
  --radius-md: 0;
}

.edit-box {
  transform: rotate(-0.1deg);
}"
            aria-label="Custom CSS"
            @keydown="onCssKeydown"
        />

        <div class="settings__action-row">
          <button
              type="button"
              class="edit-box__action"
              @click="openCssPicker"
          >
            Import CSS
          </button>

          <button
              type="button"
              class="edit-box__action"
              @click="clearCss"
          >
            Clear
          </button>

          <button
              type="button"
              class="
              edit-box__action
              edit-box__action--accent
            "
              @click="applyCssDraft"
          >
            Apply CSS
          </button>
        </div>

        <p
            v-if="cssError"
            class="
            settings__message
            settings__message--error
          "
            role="alert"
        >
          {{ cssError }}
        </p>

        <p
            v-else-if="cssStatus"
            class="
            settings__message
            settings__message--success
          "
            role="status"
        >
          {{ cssStatus }}
        </p>
      </section>
    </div>
  </section>
</template>

<style scoped>
.settings {
  width: min(100%, 58rem);
  margin-inline: auto;

  color: rgb(var(--c-fg));
  font-family: var(--font-primary);
}

.settings__file-input {
  display: none;
}

/* -------------------------------------------------------------------------- */
/* Theme cards                                                                */
/* -------------------------------------------------------------------------- */

.theme-options {
  display: grid;
  grid-template-columns:
    repeat(2, minmax(0, 1fr));

  gap: var(--space-2);
}

.theme-option {
  position: relative;

  min-width: 0;
  min-height: 5.5rem;

  display: grid;
  grid-template-columns:
    3.2rem
    minmax(0, 1fr)
    auto;
  align-items: center;
  gap: var(--space-3);

  padding: var(--space-3);

  color: rgb(var(--c-fg));

  background: linear-gradient(
      145deg,
      rgb(var(--c-surface-raised) / 0.55),
      rgb(var(--c-surface-2) / 0.32)
  );

  border: 1px solid rgb(var(--c-border) / 0.3);
  border-radius: var(--radius-md);
  outline: 0;

  font: inherit;
  text-align: left;

  cursor: pointer;

  transition: background-color var(--duration-fast) var(--ease-standard),
  border-color var(--duration-fast) var(--ease-standard),
  box-shadow var(--duration-fast) var(--ease-standard),
  transform var(--duration-fast) var(--ease-standard);
}

.theme-option:hover {
  background: rgb(var(--c-surface-hover) / 0.8);

  border-color: rgb(var(--c-accent) / 0.5);

  transform: translateY(-1px);
}

.theme-option:focus-visible {
  outline: var(--focus-ring-width) solid rgb(var(--focus-ring-color) / 0.3);

  outline-offset: 2px;
}

.theme-option--selected {
  background: linear-gradient(
      145deg,
      rgb(var(--c-accent) / 0.18),
      rgb(var(--c-primary) / 0.09)
  );

  border-color: rgb(var(--c-accent) / 0.7);

  box-shadow: inset 0 1px 0 rgb(255 255 255 / 0.3),
  0 4px 12px rgb(var(--c-shadow) / 0.07);
}

.theme-option__preview {
  width: 3.2rem;
  height: 3.2rem;

  display: grid;
  grid-template-columns:
    repeat(3, 1fr);

  overflow: hidden;

  border: 1px solid rgb(var(--c-border) / 0.4);
  border-radius: var(--radius-sm);

  box-shadow: inset 0 1px 0 rgb(255 255 255 / 0.2);
}

.theme-option__preview span {
  display: block;
}

.theme-option__preview--warm
span:nth-child(1) {
  background: rgb(99 61 40);
}

.theme-option__preview--warm
span:nth-child(2) {
  background: rgb(207 168 116);
}

.theme-option__preview--warm
span:nth-child(3) {
  background: rgb(224 126 45);
}

.theme-option__preview--dark
span:nth-child(1) {
  background: rgb(27 18 14);
}

.theme-option__preview--dark
span:nth-child(2) {
  background: rgb(65 44 33);
}

.theme-option__preview--dark
span:nth-child(3) {
  background: rgb(235 143 63);
}

.theme-option__preview--system
span:nth-child(1) {
  background: rgb(207 168 116);
}

.theme-option__preview--system
span:nth-child(2) {
  background: linear-gradient(
      to bottom,
      rgb(207 168 116) 50%,
      rgb(65 44 33) 50%
  );
}

.theme-option__preview--system
span:nth-child(3) {
  background: rgb(235 143 63);
}

.theme-option__content {
  min-width: 0;

  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.theme-option__label {
  color: rgb(var(--c-fg-strong));

  font-size: 0.82rem;
  font-weight: 800;
}

.theme-option__description {
  color: rgb(var(--c-muted));

  font-size: 0.7rem;
  line-height: 1.4;
}

.theme-option__indicator {
  width: 0.75rem;
  height: 0.75rem;
  box-sizing: border-box;

  background: transparent;

  border: 2px solid rgb(var(--c-muted));
  border-radius: 50%;
}

.theme-option--selected
.theme-option__indicator {
  background: rgb(var(--c-accent));

  border-color: rgb(var(--c-primary-strong));

  box-shadow: 0 0 0 3px rgb(var(--c-accent) / 0.15);
}

/* -------------------------------------------------------------------------- */
/* Customization guide                                                        */
/* -------------------------------------------------------------------------- */

.customization-guide {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.customization-guide__modes {
  display: grid;
  grid-template-columns:
    repeat(3, minmax(0, 1fr));

  gap: var(--space-2);
}

.customization-mode {
  min-width: 0;

  display: grid;
  grid-template-columns:
    auto minmax(0, 1fr);
  align-items: flex-start;
  gap: var(--space-2);

  padding: var(--space-3);

  background: linear-gradient(
      145deg,
      rgb(var(--c-surface-raised) / 0.52),
      rgb(var(--c-surface-2) / 0.28)
  );

  border: 1px solid rgb(var(--c-border) / 0.26);
  border-radius: var(--radius-sm);

  box-shadow: inset 0 1px 0 rgb(255 255 255 / 0.2);
}

.customization-mode__icon {
  width: 2rem;
  height: 2rem;
  flex: 0 0 auto;

  display: grid;
  place-items: center;

  color: rgb(var(--c-primary-strong));

  background: rgb(var(--c-accent) / 0.11);

  border: 1px solid rgb(var(--c-accent) / 0.22);
  border-radius: var(--radius-sm);
}

.customization-mode__icon svg {
  width: 1rem;
  height: 1rem;

  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.customization-mode strong {
  display: block;

  margin-bottom: var(--space-1);

  color: rgb(var(--c-fg-strong));

  font-size: 0.75rem;
  font-weight: 800;
}

.customization-mode p {
  margin: 0;

  color: rgb(var(--c-muted));

  font-size: 0.67rem;
  line-height: 1.45;
}

.customization-guide__content {
  display: grid;
  grid-template-columns:
    repeat(2, minmax(0, 1fr));

  gap: var(--space-3);
}

.customization-guide__block {
  min-width: 0;

  display: flex;
  flex-direction: column;

  overflow: hidden;

  background: rgb(var(--c-surface-2) / 0.2);

  border: 1px solid rgb(var(--c-border) / 0.25);
  border-radius: var(--radius-md);
}

.customization-guide__block--wide {
  grid-column: 1 / -1;
}

.customization-guide__block-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-3);

  padding: var(--space-3);

  background: linear-gradient(
      145deg,
      rgb(var(--c-surface-raised) / 0.55),
      rgb(var(--c-surface-2) / 0.3)
  );

  border-bottom: 1px solid rgb(var(--c-border) / 0.22);
}

.customization-guide__block-header > div {
  min-width: 0;
}

.customization-guide__block-header h3 {
  margin: 0 0 var(--space-1);

  color: rgb(var(--c-fg-strong));

  font-size: 0.79rem;
  font-weight: 800;
  line-height: 1.3;
}

.customization-guide__block-header p {
  margin: 0;

  color: rgb(var(--c-muted));

  font-size: 0.67rem;
  line-height: 1.45;
}

.customization-guide code {
  color: rgb(var(--c-primary-strong));

  font-family: var(--font-monospace);
  font-size: 0.92em;
}

.customization-guide__notice {
  display: grid;
  grid-template-columns:
    auto minmax(0, 1fr);
  align-items: flex-start;
  gap: var(--space-2);

  margin: var(--space-3) var(--space-3) 0;
  padding: var(--space-2);

  color: rgb(var(--c-info-strong));

  background: rgb(var(--c-info) / 0.08);

  border: 1px solid rgb(var(--c-info) / 0.22);
  border-radius: var(--radius-sm);
}

.customization-guide__notice--warning {
  color: rgb(var(--c-warning-strong));

  background: rgb(var(--c-warning) / 0.09);

  border-color: rgb(var(--c-warning) / 0.25);

  margin-bottom: var(--space-3);
}

.customization-guide__notice svg {
  width: 1rem;
  height: 1rem;

  margin-top: 0.05rem;

  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.customization-guide__notice p {
  margin: 0;

  color: inherit;

  font-size: 0.67rem;
  line-height: 1.45;
}

.customization-guide__comparison {
  display: grid;
  grid-template-columns:
    repeat(2, minmax(0, 1fr));

  gap: var(--space-2);

  padding: var(--space-3);
}

.customization-guide__format {
  min-width: 0;

  display: flex;
  flex-direction: column;
  gap: var(--space-1);

  padding: var(--space-2);

  background: rgb(var(--c-surface-raised) / 0.46);

  border: 1px solid rgb(var(--c-border) / 0.2);
  border-radius: var(--radius-xs);

  overflow: hidden;
}

.customization-guide__format--valid {
  border-color: rgb(var(--c-success) / 0.28);
}

.customization-guide__format--invalid {
  border-color: rgb(var(--c-danger) / 0.24);
}

.customization-guide__format span {
  color: rgb(var(--c-muted));

  font-size: 0.58rem;
  font-weight: 800;
  line-height: 1.2;

  text-transform: uppercase;
  letter-spacing: 0.07em;
}

.customization-guide__format--valid span {
  color: rgb(var(--c-success-strong));
}

.customization-guide__format--invalid span {
  color: rgb(var(--c-danger-strong));
}

.customization-guide__format code {
  display: block;

  overflow-x: auto;

  color: rgb(var(--c-fg));

  white-space: nowrap;
}

.customization-guide__code {
  min-width: 0;
  min-height: 10rem;
  max-height: 24rem;

  margin: 0;
  padding: var(--space-3);

  overflow: auto;

  color: rgb(var(--c-fg));

  background: linear-gradient(
      145deg,
      rgb(var(--c-page-secondary) / 0.78),
      rgb(var(--c-surface-3) / 0.62)
  );

  border-top: 1px solid rgb(var(--c-border) / 0.2);
  border-bottom: 1px solid rgb(var(--c-border) / 0.2);

  font-family: var(--font-monospace);
  font-size: 0.67rem;
  line-height: 1.6;

  tab-size: 2;
  white-space: pre;
}

.customization-guide__code code {
  color: inherit;
  font: inherit;
}

.customization-guide__details {
  display: grid;
  grid-template-columns:
    repeat(2, minmax(0, 1fr));

  gap: var(--space-2);

  padding: var(--space-3);
}

.customization-guide__details > div {
  min-width: 0;

  padding: var(--space-2);

  background: rgb(var(--c-surface-raised) / 0.35);

  border: 1px solid rgb(var(--c-border) / 0.18);
  border-radius: var(--radius-xs);
}

.customization-guide__details strong {
  display: block;

  margin-bottom: var(--space-1);

  color: rgb(var(--c-fg-strong));

  font-size: 0.68rem;
  font-weight: 800;
}

.customization-guide__details p {
  margin: 0;

  color: rgb(var(--c-muted));

  font-size: 0.64rem;
  line-height: 1.45;
}

.customization-guide__definition-list {
  display: grid;
  grid-template-columns:
    repeat(2, minmax(0, 1fr));

  gap: var(--space-2);

  margin: 0;
  padding: var(--space-3);
}

.customization-guide__definition-list > div {
  min-width: 0;

  padding: var(--space-2);

  background: rgb(var(--c-surface-raised) / 0.32);

  border: 1px solid rgb(var(--c-border) / 0.18);
  border-radius: var(--radius-xs);
}

.customization-guide__definition-list dt {
  margin-bottom: var(--space-1);

  color: rgb(var(--c-fg-strong));

  font-family: var(--font-monospace);
  font-size: 0.67rem;
  font-weight: 800;
}

.customization-guide__definition-list dd {
  margin: 0;

  color: rgb(var(--c-muted));

  font-size: 0.63rem;
  line-height: 1.45;
}

.customization-guide__workflow {
  display: grid;
  grid-template-columns:
    repeat(4, minmax(0, 1fr));

  gap: var(--space-2);
}

.customization-guide__step {
  min-width: 0;

  display: grid;
  grid-template-columns:
    auto minmax(0, 1fr);
  align-items: flex-start;
  gap: var(--space-2);

  padding: var(--space-2);

  background: rgb(var(--c-surface-raised) / 0.36);

  border: 1px solid rgb(var(--c-border) / 0.2);
  border-radius: var(--radius-sm);
}

.customization-guide__step > span {
  width: 1.5rem;
  height: 1.5rem;
  flex: 0 0 auto;

  display: grid;
  place-items: center;

  color: rgb(var(--c-on-accent));

  background: rgb(var(--c-primary));

  border: 1px solid rgb(var(--c-accent) / 0.65);
  border-radius: 50%;

  font-size: 0.62rem;
  font-weight: 850;
}

.customization-guide__step strong {
  display: block;

  margin-bottom: 0.15rem;

  color: rgb(var(--c-fg-strong));

  font-size: 0.67rem;
  font-weight: 800;
}

.customization-guide__step p {
  margin: 0;

  color: rgb(var(--c-muted));

  font-size: 0.61rem;
  line-height: 1.4;
}

/* -------------------------------------------------------------------------- */
/* Custom theme                                                               */
/* -------------------------------------------------------------------------- */

.custom-theme__badges {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: var(--space-1);
}

.custom-theme__identity {
  display: grid;
  grid-template-columns:
    minmax(0, 1fr)
    minmax(12rem, 0.7fr);

  gap: var(--space-3);

  margin-bottom: var(--space-3);
}

.custom-theme__field {
  min-width: 0;

  display: flex;
  flex-direction: column;
  gap: var(--space-1);

  color: rgb(var(--c-muted));

  font-size: 0.68rem;
  font-weight: 750;
}

.custom-theme__field input {
  min-height: 2.45rem;
  box-sizing: border-box;

  padding: var(--space-2) var(--space-3);

  color: rgb(var(--c-fg-strong));

  background: rgb(var(--c-surface-raised) / 0.65);

  border: 1px solid rgb(var(--c-border) / 0.35);
  border-radius: var(--radius-sm);
  outline: 0;

  font: inherit;
  font-size: 0.78rem;
}

.custom-theme__field input:focus {
  border-color: rgb(var(--c-accent) / 0.72);

  box-shadow: 0 0 0 var(--focus-ring-width) rgb(var(--focus-ring-color) / 0.14);
}

.custom-theme__base-options {
  display: grid;
  grid-template-columns:
    repeat(2, 1fr);
}

.custom-theme__base-options button {
  min-height: 2.45rem;

  color: rgb(var(--c-muted));

  background: rgb(var(--c-surface-raised) / 0.5);

  border: 1px solid rgb(var(--c-border) / 0.3);

  font: inherit;
  font-size: 0.74rem;
  font-weight: 750;

  cursor: pointer;
}

.custom-theme__base-options
button:first-child {
  border-radius: var(--radius-sm) 0 0 var(--radius-sm);
}

.custom-theme__base-options
button:last-child {
  margin-left: -1px;

  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
}

.custom-theme__base-options
.custom-theme__base--active {
  position: relative;
  z-index: 1;

  color: rgb(var(--c-on-accent));

  background: rgb(var(--c-primary));

  border-color: rgb(var(--c-accent));
}

/* -------------------------------------------------------------------------- */
/* Editors                                                                    */
/* -------------------------------------------------------------------------- */

.settings__code-editor {
  width: 100%;
  min-width: 0;
  box-sizing: border-box;

  padding: var(--space-3);

  color: rgb(var(--c-fg));

  background: linear-gradient(
      145deg,
      rgb(var(--c-page-secondary) / 0.72),
      rgb(var(--c-surface-3) / 0.58)
  );

  border: 1px solid rgb(var(--c-border) / 0.42);
  border-radius: var(--radius-sm);
  outline: 0;

  box-shadow: inset 0 2px 7px rgb(var(--c-shadow) / 0.12);

  font-family: var(--font-monospace);
  font-size: 0.72rem;
  line-height: 1.55;

  tab-size: 2;
  resize: vertical;
}

.settings__code-editor:focus {
  border-color: rgb(var(--c-accent) / 0.76);

  box-shadow: 0 0 0 var(--focus-ring-width) rgb(var(--focus-ring-color) / 0.14),
  inset 0 2px 7px rgb(var(--c-shadow) / 0.12);
}

.settings__code-editor--palette {
  min-height: 12rem;
}

/* -------------------------------------------------------------------------- */
/* Creative CSS                                                               */
/* -------------------------------------------------------------------------- */

.creative-css__toggle {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

/* -------------------------------------------------------------------------- */
/* Actions and messages                                                       */
/* -------------------------------------------------------------------------- */

.settings__action-row {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: var(--space-2);

  margin-top: var(--space-3);
}

.settings__message {
  margin: var(--space-2) 0 0;

  padding: var(--space-2) var(--space-3);

  border-radius: var(--radius-sm);

  font-size: 0.72rem;
  font-weight: 650;
  line-height: 1.4;
}

.settings__message--error {
  color: rgb(var(--c-danger-strong));

  background: rgb(var(--c-danger) / 0.08);

  border: 1px solid rgb(var(--c-danger) / 0.24);
}

.settings__message--success {
  color: rgb(var(--c-success-strong));

  background: rgb(var(--c-success) / 0.08);

  border: 1px solid rgb(var(--c-success) / 0.24);
}

/* -------------------------------------------------------------------------- */
/* Responsive                                                                 */
/* -------------------------------------------------------------------------- */

@media (max-width: 820px) {
  .customization-guide__modes {
    grid-template-columns: 1fr;
  }

  .customization-guide__workflow {
    grid-template-columns:
      repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .theme-options,
  .custom-theme__identity,
  .customization-guide__content {
    grid-template-columns: 1fr;
  }

  .customization-guide__block--wide {
    grid-column: auto;
  }
}

@media (max-width: 520px) {
  .customization-guide__comparison,
  .customization-guide__details,
  .customization-guide__definition-list,
  .customization-guide__workflow {
    grid-template-columns: 1fr;
  }

  .customization-guide__block-header {
    flex-direction: column;
  }
}

@media (max-width: 480px) {
  .theme-option {
    grid-template-columns:
      2.7rem
      minmax(0, 1fr)
      auto;
  }

  .theme-option__preview {
    width: 2.7rem;
    height: 2.7rem;
  }

  .settings__action-row {
    align-items: stretch;
    flex-direction: column;
  }

  .settings__action-row
  .edit-box__action {
    width: 100%;
  }

  .custom-theme__badges,
  .creative-css__toggle {
    justify-content: flex-start;
  }
}

@media (prefers-reduced-motion: reduce) {
  .theme-option {
    transition: none;
  }

  .theme-option:hover {
    transform: none;
  }
}
</style>