import type {
    App,
    Component,
    Plugin
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
} from "./components";

const primitiveEditors = Object.freeze({
    AutoCompleteBox,
    BooleanTickBox,
    BooleanToggle,
    LongTextBox,
    NumberInput,
    NumberSlider,
    ShortTextBox,
    SingleAutoComplete,
    SingleEnumInput
}) satisfies Readonly<
    Record<string, Component>
>;

export const PrimitiveEditorsPlugin: Plugin = {
    install(app: App): void {
        for (
            const [name, component]
            of Object.entries(primitiveEditors)
            ) {
            app.component(name, component);
        }
    }
};