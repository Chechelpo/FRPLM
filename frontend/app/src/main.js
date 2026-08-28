// src/main.js
import "./assets/main.css";
import "./assets/styles/EntityEditors.css";
import "@frplm/ui/style.css";

import { createApp } from "vue";
import { ABSEntity, onGlobalError } from "@frplm/host-sdk";

import App from "./App.vue";
import { router } from "./app/router.js";
import { initializeTheme } from "./core/theme.js";

import { registerGlobalErrorHandler } from "./core/GlobalError.js";
import { registerPrimitiveEditors } from "./extensions/registerPrimitiveEditors.js";
import "./extensions/HostBindings.js";

registerGlobalErrorHandler(onGlobalError);
registerPrimitiveEditors();

await ABSEntity.initialise();
initializeTheme();

createApp(App)
    .use(router)
    .mount("#app");
