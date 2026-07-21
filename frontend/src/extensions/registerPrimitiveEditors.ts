import {
    defineCustomElement
} from "vue";

import {
    AutoCompleteBox,
    BooleanTickBox,
    BooleanToggle,
    LongTextBox,
    NumberInput,
    NumberSlider,
    ShortTextBox,
    SingleAutoComplete,
    SingleEnumInput
} from "@/components/primitive-editors";

import {
    FRPLM_COMPONENT_TAGS
} from "../../sdk/src/components";

import type {
    FrplmComponentName
} from "../../sdk/src/components";

const constructors = {
    AutoCompleteBox: defineCustomElement(
        AutoCompleteBox,
        {
            shadowRoot: false
        }
    ),

    BooleanTickBox: defineCustomElement(
        BooleanTickBox,
        {
            shadowRoot: false
        }
    ),

    BooleanToggle: defineCustomElement(
        BooleanToggle,
        {
            shadowRoot: false
        }
    ),

    LongTextBox: defineCustomElement(
        LongTextBox,
        {
            shadowRoot: false
        }
    ),

    NumberInput: defineCustomElement(
        NumberInput,
        {
            shadowRoot: false
        }
    ),

    NumberSlider: defineCustomElement(
        NumberSlider,
        {
            shadowRoot: false
        }
    ),

    ShortTextBox: defineCustomElement(
        ShortTextBox,
        {
            shadowRoot: false
        }
    ),

    SingleAutoComplete: defineCustomElement(
        SingleAutoComplete,
        {
            shadowRoot: false
        }
    ),

    SingleEnumInput: defineCustomElement(
        SingleEnumInput,
        {
            shadowRoot: false
        }
    )
} satisfies Record<
    FrplmComponentName,
    CustomElementConstructor
>;

export function registerPrimitiveEditors(): void {
    for (
        const componentName of Object.keys(
        constructors
    ) as FrplmComponentName[]
        ) {
        const tagName =
            FRPLM_COMPONENT_TAGS[
                componentName
                ];

        if (customElements.get(tagName)) {
            continue;
        }

        customElements.define(
            tagName,
            constructors[componentName]
        );
    }
}