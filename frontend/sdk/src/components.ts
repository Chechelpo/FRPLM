// src/components.ts
/**
 * Component registry consumed by host applications to expose the FRPLM
 * primitive editors to extension panels.
 *
 * The SDK only declares the custom-element tag names here; the actual
 * custom-element definitions and Vue components live in the
 * `@frplm/ui` package, which the host registers at runtime via
 * `registerPrimitiveEditors()`.
 */
export const FRPLM_COMPONENT_TAGS = {
    AutoCompleteBox:
        "frplm-auto-complete-box",

    BooleanTickBox:
        "frplm-boolean-tick-box",

    BooleanToggle:
        "frplm-boolean-toggle",

    LongTextBox:
        "frplm-long-text-box",

    NumberInput:
        "frplm-number-input",

    NumberSlider:
        "frplm-number-slider",

    ShortTextBox:
        "frplm-short-text-box",

    SingleAutoComplete:
        "frplm-single-auto-complete",

    SingleEnumInput:
        "frplm-single-enum-input"
} as const satisfies Record<
    string,
    `${string}-${string}`
>;

export type FrplmComponentRegistry =
    Readonly<typeof FRPLM_COMPONENT_TAGS>;

export type FrplmComponentName =
    keyof FrplmComponentRegistry;

export type FrplmComponentTag =
    FrplmComponentRegistry[
        FrplmComponentName
        ];
