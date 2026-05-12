import {createRouter, createWebHistory} from "vue-router";
import CharacterLanding from "@/components/char/CharacterLanding.vue";
import EmptyView from "@/components/layout/EmptyView.vue";
import LlmEditor from "@/components/connections/LLMEditor.vue";
import WorldLanding from "@/components/space/WorldLanding.vue";
import {EntityTypes} from "@/domain/EntityTypes";
import LorebookLanding from "@/components/lorebooks/LorebookLanding.vue";
import ConnectionLanding from "@/components/connections/ConnectionLanding.vue";

export function route_to(type:EntityTypes): string {
    return `/${type}`;
}

export const router = createRouter({
    history: createWebHistory(),
    routes: [
        { path: "/", component: EmptyView },
        { path: "/lorebook", component: LorebookLanding, name: "Lorebook Landing" },
        { path: route_to(EntityTypes.CHARACTERS), component: CharacterLanding, name: "Character" },
        { path: route_to(EntityTypes.WORLDS), component: WorldLanding, name: "worldList"},
        { path: route_to(EntityTypes.LLM), component: ConnectionLanding, name:"connection list"}
    ],
});
