import './assets/main.css'
import './assets/styles/EntityEditors.css'

import { createApp } from "vue";
import App from "./App.vue";
import { router } from "./app/router.ts";
import "./assets/main.css";
import {ABSEntity} from "@/frameworks/ABSEntity.ts";

await ABSEntity.initialise()
createApp(App)
    .use(router)
    .mount("#app");