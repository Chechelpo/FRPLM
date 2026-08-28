// tests/components.test.ts
import { test } from "node:test";
import assert from "node:assert/strict";

import {
    FRPLM_COMPONENT_TAGS,
} from "../dist/components.js";
import type {
    FrplmComponentName,
    FrplmComponentTag,
} from "../dist/components.js";

test("FRPLM_COMPONENT_TAGS exposes all expected primitive editors", () => {
    const expected = [
        "AutoCompleteBox",
        "BooleanTickBox",
        "BooleanToggle",
        "LongTextBox",
        "NumberInput",
        "NumberSlider",
        "ShortTextBox",
        "SingleAutoComplete",
        "SingleEnumInput",
    ];

    for (const name of expected) {
        assert.ok(name in FRPLM_COMPONENT_TAGS, `missing ${name}`);
    }
});

test("every registered tag is a custom-element name with a dash", () => {
    for (const [name, tag] of Object.entries(FRPLM_COMPONENT_TAGS)) {
        assert.ok(
            tag.includes("-"),
            `tag for ${name} should contain a dash: ${tag}`,
        );
        assert.ok(
            tag.toLowerCase().startsWith("frplm"),
            `tag for ${name} should start with frplm-: ${tag}`,
        );
    }
});

test("the component name union is the keys of the registry", () => {
    const keys = Object.keys(FRPLM_COMPONENT_TAGS) as FrplmComponentName[];
    assert.deepEqual(
        new Set(keys),
        new Set([
            "AutoCompleteBox",
            "BooleanTickBox",
            "BooleanToggle",
            "LongTextBox",
            "NumberInput",
            "NumberSlider",
            "ShortTextBox",
            "SingleAutoComplete",
            "SingleEnumInput",
        ]),
    );
});

test("FrplmComponentTag is a literal string union", () => {
    const tag: FrplmComponentTag = FRPLM_COMPONENT_TAGS.AutoCompleteBox;
    assert.equal(tag, "frplm-auto-complete-box");
});
