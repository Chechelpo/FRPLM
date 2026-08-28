// ui/src/index.ts
/**
 * Public entry point of the FRPLM UI package.
 *
 * The package bundles the primitive editor components used by the
 * host application and by extension panels. The matching
 * `style.css` stylesheet gives the components the same look and
 * feel as the rest of the app, so extensions that consume the UI
 * package inherit the host's design system.
 *
 * The host application and any extension should:
 *
 *   1. Import the components they need.
 *   2. Load `@frplm/ui/style.css` once at startup so the component
 *      styles are available.
 *
 * All components are plain Vue 3 single-file components and depend
 * only on `vue` and the CSS variables exposed by the host theme.
 * They do not import any host or SDK code.
 */

export { default as AutoCompleteBox } from "./components/AutoCompleteBox.vue";
export { default as BooleanTickBox } from "./components/BooleanTickBox.vue";
export { default as BooleanToggle } from "./components/BooleanToggle.vue";
export { default as LongTextBox } from "./components/LongTextBox.vue";
export { default as NumberInput } from "./components/NumberInput.vue";
export { default as NumberSlider } from "./components/NumberSlider.vue";
export { default as ShortTextBox } from "./components/ShortTextBox.vue";
export { default as SingleAutoComplete } from "./components/SingleAutoComplete.vue";
export { default as SingleEnumInput } from "./components/SingleEnumInput.vue";

/**
 * A function that returns the token count for a piece of text.
 *
 * The host application supplies a token counter (typically backed by
 * a call to the backend) so {@link LongTextBox} can display token
 * counts without the UI package taking a hard dependency on any
 * specific tokenizer.
 */
export type TokenCounter = (text: string) => Promise<number>;
