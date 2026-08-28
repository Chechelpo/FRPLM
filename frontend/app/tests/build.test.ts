// app/tests/build.test.ts
import { test } from "node:test";
import assert from "node:assert/strict";
import { readFile, stat } from "node:fs/promises";
import { resolve, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = dirname(fileURLToPath(import.meta.url));
const distDir = resolve(__dirname, "..", "dist");
const sdkDist = resolve(__dirname, "..", "..", "sdk", "dist", "index.js");
const uiDist = resolve(__dirname, "..", "..", "ui", "dist", "index.js");

test("app build produces an index.html", async () => {
    const info = await stat(resolve(distDir, "index.html"));
    assert.ok(info.size > 0, "index.html should not be empty");
});

test("app build bundles the SDK and the UI", async () => {
    const [html, sdk, ui] = await Promise.all([
        readFile(resolve(distDir, "index.html"), "utf8"),
        readFile(sdkDist, "utf8").catch(() => ""),
        readFile(uiDist, "utf8").catch(() => ""),
    ]);

    assert.ok(html.length > 0);
    assert.ok(sdk.length > 0, "sdk dist must exist before app build");
    assert.ok(ui.length > 0, "ui dist must exist before app build");
});

test("app build embeds the SDK component registry", async () => {
    const bundle = await readFile(
        resolve(distDir, "assets", await pickBundleFile()),
        "utf8",
    );

    // The host exposes the SDK's component tag registry, so the
    // bundled output should mention at least one custom element
    // name from FRPLM_COMPONENT_TAGS.
    assert.ok(
        bundle.includes("frplm-boolean-toggle"),
        "app bundle should reference the frplm-boolean-toggle custom element",
    );
    assert.ok(
        bundle.includes("frplm-long-text-box"),
        "app bundle should reference the frplm-long-text-box custom element",
    );
});

async function pickBundleFile(): Promise<string> {
    const { readdir } = await import("node:fs/promises");
    const files = await readdir(resolve(distDir, "assets"));
    const js = files.find(
        (f: string) => f.startsWith("index-") && f.endsWith(".js"),
    );
    if (!js) {
        throw new Error("no bundled JS file in dist/assets/");
    }
    return js;
}
