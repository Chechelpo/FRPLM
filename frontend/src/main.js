import './assets/main.css'

import { createApp } from "vue";
import App from "./App.vue";
import { router } from "./router";
import "./assets/main.css";
import {EntityABS} from "@/entities/EntityABS.js";

await EntityABS.initialise()
createApp(App)
    .use(router)
    .mount("#app");