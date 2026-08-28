// vite.config.js
import { fileURLToPath, URL } from "node:url";
import { resolve } from "node:path";
import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

import { hostSdkPath, uiPath } from "./workspace-aliases.mjs";

export default defineConfig({
    plugins: [
        vue({
            template: {
                compilerOptions: {
                    isCustomElement: (tag) => tag.startsWith("frplm-"),
                },
            },
        }),
    ],

    resolve: {
        alias: [
            {
                find: "@",
                replacement: fileURLToPath(
                    new URL("./src", import.meta.url),
                ),
            },
            {
                find: /^@app\/(.*)$/,
                replacement: fileURLToPath(
                    new URL("./src/app/$1", import.meta.url),
                ),
            },
            {
                find: /^@components\/(.*)$/,
                replacement: fileURLToPath(
                    new URL("./src/components/$1", import.meta.url),
                ),
            },
            {
                find: /^@extensions\/(.*)$/,
                replacement: fileURLToPath(
                    new URL("./src/extensions/$1", import.meta.url),
                ),
            },
            {
                find: /^@core\/(.*)$/,
                replacement: fileURLToPath(
                    new URL("./src/core/$1", import.meta.url),
                ),
            },
            {
                find: /^@services\/(.*)$/,
                replacement: fileURLToPath(
                    new URL("./src/services/$1", import.meta.url),
                ),
            },
            {
                find: /^@assets\/(.*)$/,
                replacement: fileURLToPath(
                    new URL("./src/assets/$1", import.meta.url),
                ),
            },
            {
                find: /^@frplm\/host-sdk$/,
                replacement: hostSdkPath,
            },
            {
                find: /^@frplm\/ui$/,
                replacement: uiPath,
            },
            {
                find: /^@frplm\/ui\/(.*)$/,
                replacement: resolve(uiPath, "..", "$1"),
            },
        ],
    },

    server: {
        host: "127.0.0.1",
        port: 5173,
        strictPort: true,
        proxy: {
            "/api": {
                target: "http://127.0.0.1:8080",
                changeOrigin: true,
            },
        },
    },
});
