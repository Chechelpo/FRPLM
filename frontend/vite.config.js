// src/vite.config.js
import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import { fileURLToPath, URL } from "node:url";

export default defineConfig({
  plugins: [
    vue({
      template: {
        compilerOptions: {
          isCustomElement: (tag) => tag.startsWith("frplm-")
        }
      }
    })
  ],

  resolve: {
    alias: {
      "@": fileURLToPath(
          new URL("./src", import.meta.url)
      )
    }
  },

  server: {
    host: "127.0.0.1",
    port: 5173,
    strictPort: true,
    proxy: {
      "/api": {
        target: "http://127.0.0.1:8080",
        changeOrigin: true
      }
    }
  }
});