// tests/config.test.ts
import { test } from "node:test";
import assert from "node:assert/strict";

import {
    API_BASE,
    configureApiBase,
    getApiBase,
} from "../dist/config.js";

test("getApiBase returns the configured override when present", () => {
    configureApiBase(new URL("https://api.example.test/"));
    try {
        assert.equal(
            getApiBase().toString(),
            "https://api.example.test/",
        );
    } finally {
        configureApiBase(API_BASE);
    }
});

test("configureApiBase accepts a string and normalises it", () => {
    configureApiBase("https://api.example.test/");
    try {
        assert.equal(
            getApiBase().toString(),
            "https://api.example.test/",
        );
    } finally {
        configureApiBase(API_BASE);
    }
});

test("API_BASE defaults to /api/ on the current origin in browser-like envs", () => {
    configureApiBase(API_BASE);
    const base = getApiBase();
    assert.equal(base.pathname, "/api/");
});
