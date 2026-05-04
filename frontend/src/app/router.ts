import {createRouter, createWebHistory} from "vue-router";
import CharacterLanding from "@/components/char/CharacterLanding.vue";
import EmptyView from "@/components/layout/EmptyView.vue";
import ConnectionView from "@/components/connections/ConnectionView.vue";
import WorldLanding from "@/components/space/WorldLanding.vue";
import {EntityTypes} from "@/frameworks/entities/EntityTypes";
import {EntityABS} from "@/frameworks/entities/EntityABS";
import WorldEdit from "@/components/space/WorldEdit.vue";

export function route_to(type:EntityTypes): string {
    return `/${type}`;
}

export const router = createRouter({
    history: createWebHistory(),
    routes: [
        { path: "/", component: EmptyView },
        { path: "/llm", component: ConnectionView, name: "LLM_connections" },
        { path: route_to(EntityTypes.CHARACTERS), component: CharacterLanding, name: "Character" },
        { path: route_to(EntityTypes.WORLDS), component: WorldLanding, name: "worldList"}
    ],
});
