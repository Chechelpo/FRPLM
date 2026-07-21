import { resolve } from "node:path";

import vue from "@vitejs/plugin-vue";
import cssInjectedByJsPlugin
    from "vite-plugin-css-injected-by-js";

import { defineConfig } from "vite";

const uiRoot =
    import.meta.dirname;

const frontendRoot =
    resolve(
        uiRoot,
        ".."
    );

const frontendSourceRoot =
    resolve(
        frontendRoot,
        "src"
    );

export default defineConfig({
    plugins: [
        vue(),

        cssInjectedByJsPlugin({
            topExecutionPriority: true,
            styleId: "frplm-ui-styles"
        })
    ],

    resolve: {
        alias: [
            {
                find: "@",
                replacement:
                frontendSourceRoot
            }
        ],

        dedupe: [
            "vue"
        ]
    },

    build: {
        outDir: resolve(
            uiRoot,
            "dist"
        ),

        emptyOutDir: true,

        lib: {
            entry: resolve(
                uiRoot,
                "src/index.ts"
            ),

            formats: [
                "es"
            ],

            fileName: "index"
        },

        rollupOptions: {
            external: [
                "vue"
            ]
        }
    }
});