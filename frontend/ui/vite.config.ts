// vite.config.ts
import { resolve } from "node:path";
import vue from "@vitejs/plugin-vue";
import { defineConfig } from "vite";

const uiRoot = import.meta.dirname;

export default defineConfig({
    plugins: [
        vue(),
    ],

    resolve: {
        dedupe: [
            "vue",
        ],
    },

    build: {
        outDir: resolve(
            uiRoot,
            "dist",
        ),

        emptyOutDir: true,

        lib: {
            entry: resolve(
                uiRoot,
                "src/index.ts",
            ),

            formats: [
                "es",
            ],

            fileName: "index",

            cssFileName: "style",
        },

        rollupOptions: {
            external: [
                "vue",
            ],
        },
    },
});
