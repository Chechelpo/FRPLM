import {
    computed,
    readonly,
    ref,
} from "vue";

export type BaseTheme =
    | "warm"
    | "dark";

export type ThemePreference =
    | "system"
    | "warm"
    | "dark"
    | "custom";

export type ResolvedTheme = BaseTheme;

export interface CustomThemeDefinition {
    version: 1;
    name: string;
    base: BaseTheme;
    tokens: Record<string, string>;
    cssEnabled: boolean;
    css: string;
}

const THEME_STORAGE_KEY = "app-theme";
const CUSTOM_THEME_STORAGE_KEY =
    "app-custom-theme";

const USER_CSS_ELEMENT_ID =
    "app-user-theme-css";

const DEFAULT_CUSTOM_THEME: CustomThemeDefinition = {
    version: 1,
    name: "Custom tavern",
    base: "warm",
    tokens: {},
    cssEnabled: false,
    css: "",
};

const preference =
    ref<ThemePreference>("system");

const systemTheme =
    ref<ResolvedTheme>("warm");

const customThemeState =
    ref<CustomThemeDefinition>(
        createDefaultCustomTheme(),
    );

let initialized = false;

let mediaQuery:
    | MediaQueryList
    | null = null;

const appliedTokenNames =
    new Set<string>();

export const themePreference =
    readonly(preference);

export const customTheme =
    readonly(customThemeState);

export const resolvedTheme =
    computed<ResolvedTheme>(() => {
        if (preference.value === "system") {
            return systemTheme.value;
        }

        if (preference.value === "custom") {
            return customThemeState.value.base;
        }

        return preference.value;
    });

export const creativeCssActive =
    computed(
        () =>
            preference.value !== "system" &&
            customThemeState.value.cssEnabled &&
            customThemeState.value.css.trim()
                .length > 0,
    );

function createDefaultCustomTheme():
    CustomThemeDefinition {
    return {
        ...DEFAULT_CUSTOM_THEME,
        tokens: {
            ...DEFAULT_CUSTOM_THEME.tokens,
        },
    };
}

function isRecord(
    value: unknown,
): value is Record<string, unknown> {
    return (
        typeof value === "object" &&
        value !== null &&
        !Array.isArray(value)
    );
}

function isBaseTheme(
    value: unknown,
): value is BaseTheme {
    return (
        value === "warm" ||
        value === "dark"
    );
}

function isThemePreference(
    value: unknown,
): value is ThemePreference {
    return (
        value === "system" ||
        value === "warm" ||
        value === "dark" ||
        value === "custom"
    );
}

function isValidCustomPropertyName(
    name: string,
): boolean {
    return /^--[a-zA-Z0-9_-]+$/.test(
        name,
    );
}

function normalizeTokens(
    value: unknown,
): Record<string, string> {
    if (!isRecord(value)) {
        throw new Error(
            "Theme tokens must be a JSON object.",
        );
    }

    const result:
        Record<string, string> = {};

    for (
        const [name, rawValue] of
        Object.entries(value)
        ) {
        if (
            !isValidCustomPropertyName(name)
        ) {
            continue;
        }

        if (
            typeof rawValue !== "string" &&
            typeof rawValue !== "number"
        ) {
            continue;
        }

        const normalizedValue =
            String(rawValue).trim();

        if (!normalizedValue) {
            continue;
        }

        result[name] = normalizedValue;
    }

    return result;
}

function parseCustomTheme(
    value: unknown,
    fallback: CustomThemeDefinition =
    createDefaultCustomTheme(),
): CustomThemeDefinition {
    if (!isRecord(value)) {
        throw new Error(
            "The custom theme must be a JSON object.",
        );
    }

    /*
     * Accept either:
     *
     * {
     *   "name": "...",
     *   "base": "warm",
     *   "tokens": { ... }
     * }
     *
     * or a direct token map:
     *
     * {
     *   "--c-page": "90 55 30",
     *   "--c-accent": "220 110 35"
     * }
     */
    const tokenSource =
        isRecord(value.tokens)
            ? value.tokens
            : value;

    return {
        version: 1,

        name:
            typeof value.name === "string" &&
            value.name.trim()
                ? value.name.trim()
                : fallback.name,

        base: isBaseTheme(value.base)
            ? value.base
            : fallback.base,

        tokens: normalizeTokens(
            tokenSource,
        ),

        cssEnabled:
            typeof value.cssEnabled ===
            "boolean"
                ? value.cssEnabled
                : fallback.cssEnabled,

        css:
            typeof value.css === "string"
                ? value.css
                : fallback.css,
    };
}

function detectSystemTheme():
    ResolvedTheme {
    if (
        typeof window !== "undefined" &&
        window.matchMedia(
            "(prefers-color-scheme: dark)",
        ).matches
    ) {
        return "dark";
    }

    return "warm";
}

function persistCustomTheme(): void {
    if (
        typeof localStorage ===
        "undefined"
    ) {
        return;
    }

    localStorage.setItem(
        CUSTOM_THEME_STORAGE_KEY,
        JSON.stringify(
            customThemeState.value,
        ),
    );
}

function clearAppliedTokens(): void {
    if (
        typeof document ===
        "undefined"
    ) {
        return;
    }

    const root =
        document.documentElement;

    for (
        const name of appliedTokenNames
        ) {
        root.style.removeProperty(name);
    }

    appliedTokenNames.clear();
}

function applyCustomTokens(): void {
    if (
        typeof document ===
        "undefined"
    ) {
        return;
    }

    clearAppliedTokens();

    if (
        preference.value !== "custom"
    ) {
        return;
    }

    const root =
        document.documentElement;

    for (
        const [name, value] of
        Object.entries(
            customThemeState.value.tokens,
        )
        ) {
        root.style.setProperty(
            name,
            value,
        );

        appliedTokenNames.add(name);
    }
}

function getUserCssElement():
    HTMLStyleElement {
    const existing =
        document.getElementById(
            USER_CSS_ELEMENT_ID,
        );

    if (
        existing instanceof
        HTMLStyleElement
    ) {
        return existing;
    }

    const style =
        document.createElement("style");

    style.id = USER_CSS_ELEMENT_ID;
    style.dataset.userThemeCss = "true";

    document.head.appendChild(style);

    return style;
}

function applyCreativeCss(): void {
    if (
        typeof document ===
        "undefined"
    ) {
        return;
    }

    const style =
        getUserCssElement();

    if (!creativeCssActive.value) {
        style.textContent = "";
        style.disabled = true;
        return;
    }

    style.disabled = false;
    style.textContent =
        customThemeState.value.css;
}

function applyResolvedTheme(): void {
    if (
        typeof document ===
        "undefined"
    ) {
        return;
    }

    const theme =
        resolvedTheme.value;

    const root =
        document.documentElement;

    /*
     * Custom themes inherit all normal Warm or Dark CSS first.
     * Their custom properties are then applied as overrides.
     */
    root.dataset.theme = theme;

    root.dataset.themePreference =
        preference.value;

    root.style.colorScheme =
        theme === "dark"
            ? "dark"
            : "light";

    applyCustomTokens();
    applyCreativeCss();
}

function handleSystemThemeChange(
    event: MediaQueryListEvent,
): void {
    systemTheme.value =
        event.matches
            ? "dark"
            : "warm";

    if (
        preference.value === "system"
    ) {
        applyResolvedTheme();
    }
}

function loadStoredCustomTheme(): void {
    if (
        typeof localStorage ===
        "undefined"
    ) {
        return;
    }

    const stored =
        localStorage.getItem(
            CUSTOM_THEME_STORAGE_KEY,
        );

    if (!stored) {
        return;
    }

    try {
        customThemeState.value =
            parseCustomTheme(
                JSON.parse(stored),
            );
    } catch (error) {
        console.error(
            "Could not load custom theme",
            error,
        );

        localStorage.removeItem(
            CUSTOM_THEME_STORAGE_KEY,
        );
    }
}

export function initializeTheme(): void {
    if (
        initialized ||
        typeof window === "undefined"
    ) {
        return;
    }

    initialized = true;

    mediaQuery = window.matchMedia(
        "(prefers-color-scheme: dark)",
    );

    systemTheme.value =
        mediaQuery.matches
            ? "dark"
            : "warm";

    loadStoredCustomTheme();

    const storedPreference =
        localStorage.getItem(
            THEME_STORAGE_KEY,
        );

    if (
        isThemePreference(
            storedPreference,
        )
    ) {
        preference.value =
            storedPreference;
    }

    applyResolvedTheme();

    mediaQuery.addEventListener(
        "change",
        handleSystemThemeChange,
    );
}

export function setThemePreference(
    value: ThemePreference,
): void {
    preference.value = value;

    if (
        typeof localStorage !==
        "undefined"
    ) {
        localStorage.setItem(
            THEME_STORAGE_KEY,
            value,
        );
    }

    applyResolvedTheme();
}

export function setCustomThemeName(
    name: string,
): void {
    customThemeState.value = {
        ...customThemeState.value,
        name:
            name.trim() ||
            "Custom theme",
    };

    persistCustomTheme();
}

export function setCustomThemeBase(
    base: BaseTheme,
): void {
    customThemeState.value = {
        ...customThemeState.value,
        base,
    };

    persistCustomTheme();
    applyResolvedTheme();
}

export function setCustomThemeTokens(
    tokens: Record<string, unknown>,
): void {
    customThemeState.value = {
        ...customThemeState.value,
        tokens: normalizeTokens(tokens),
    };

    persistCustomTheme();
    applyResolvedTheme();
}

export function setCustomThemeCss(
    css: string,
): void {
    customThemeState.value = {
        ...customThemeState.value,
        css,
    };

    persistCustomTheme();
    applyCreativeCss();
}

export function setCustomCssEnabled(
    enabled: boolean,
): void {
    customThemeState.value = {
        ...customThemeState.value,
        cssEnabled: enabled,
    };

    persistCustomTheme();
    applyCreativeCss();
}

export function importCustomThemeJson(
    source: string,
): CustomThemeDefinition {
    const parsed =
        JSON.parse(source) as unknown;

    const imported =
        parseCustomTheme(
            parsed,
            customThemeState.value,
        );

    customThemeState.value =
        imported;

    persistCustomTheme();
    applyResolvedTheme();

    return imported;
}

export function exportCustomThemeJson():
    string {
    return JSON.stringify(
        customThemeState.value,
        null,
        2,
    );
}

export function resetCustomTheme(): void {
    customThemeState.value =
        createDefaultCustomTheme();

    if (
        typeof localStorage !==
        "undefined"
    ) {
        localStorage.removeItem(
            CUSTOM_THEME_STORAGE_KEY,
        );
    }

    applyResolvedTheme();
}

export function resetThemePreference(): void {
    preference.value = "system";

    if (
        typeof localStorage !==
        "undefined"
    ) {
        localStorage.removeItem(
            THEME_STORAGE_KEY,
        );
    }

    applyResolvedTheme();
}