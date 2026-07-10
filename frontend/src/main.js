// src/main.js
import './assets/main.css'
import './assets/styles/EntityEditors.css'

import { createApp } from "vue";
import App from "./App.vue";
import { router } from "./app/router.ts";
import { ABSEntity } from "@/core/ABSEntity.ts";
import { initializeTheme } from "@/core/theme.ts";

// Import the host bindings so window.FrplmHost gets populated
import "@/extensions/HostBindings";

await ABSEntity.initialise();
initializeTheme();

createApp(App)
    .use(router)
    .mount("#app");

window.FRPLMHostSDK = HostSDK;