import './assets/main.css'

import { createApp } from "vue";
import App from "./App.vue";
import { router } from "./app/router.ts";
import "./assets/main.css";
import {EntityABS} from "@/frameworks/entities/EntityABS.ts";

await EntityABS.initialise()
createApp(App)
    .use(router)
    .mount("#app");