import {
    createRouter,
    createWebHistory,
    type RouteRecordRaw,
} from "vue-router";

import CharacterLanding from "@/components/char/CharacterLanding.vue";
import WorldLanding from "@/components/space/WorldLanding.vue";
import LorebookLanding from "@/components/lorebooks/LorebookLanding.vue";
import ConnectionLanding from "@/components/connections/ConnectionLanding.vue";
import PromptTemplatePicker from "@/components/prompts/PromptTemplatePicker.vue";
import SessionList from "@/components/session/SessionList.vue";

import { EntityTypes } from "@/domain/EntityTypes";

export function route_to(type: EntityTypes): string {
    return `/${type}`;
}

const routes: RouteRecordRaw[] = [
    {
        path: "/",
        component: SessionList,
        name: "sessions",
    },
    {
        path: "/lorebook",
        component: LorebookLanding,
        name: "lorebooks",
    },
    {
        path: route_to(EntityTypes.CHARACTERS),
        component: CharacterLanding,
        name: "characters",
    },
    {
        path: route_to(EntityTypes.WORLDS),
        component: WorldLanding,
        name: "worlds",
    },
    {
        path: route_to(EntityTypes.LLM),
        component: ConnectionLanding,
        name: "connections",
    },
    {
        path: route_to(EntityTypes.TEMPLATES),
        component: PromptTemplatePicker,
        name: "prompts",
    },
];

export const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes,
});