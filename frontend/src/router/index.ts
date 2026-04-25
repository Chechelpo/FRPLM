import {createRouter, createWebHistory} from "vue-router";
import CharacterLanding from "@/components/char/CharacterLanding.vue";
import EmptyView from "@/components/layout/EmptyView.vue";
import ConnectionView from "@/components/connections/ConnectionView.vue";
import WorldLanding from "@/components/world/WorldLanding.vue";
import {ControllerType} from "@/config/ControllerType";
import {EntityABS} from "@/entities/EntityABS";
import WorldEdit from "@/components/world/WorldEdit.vue";

export function route_to(type:ControllerType): string {
    return `/${type}`;
}

export const router = createRouter({
    history: createWebHistory(),
    routes: [
        { path: "/", component: EmptyView },
        { path: "/llm", component: ConnectionView, name: "LLM_connections" },
        { path: route_to(ControllerType.CHARACTERS), component: CharacterLanding, name: "Character" },
        { path: route_to(ControllerType.WORLDS), component: WorldLanding, name: "worldList"},
        { path: `${route_to(ControllerType.WORLDS)}/:id`, component: WorldEdit, name: "WorldEdit", props: true },
    ],
});


export function goToEdit(objectType: ControllerType, object: EntityABS<any, any>): void {
    // hashKey returns a string like "id=123" or "worldId=1&locationId=2"
    const idString = object.hashKey();

    router.push({
        path: `${route_to(objectType)}`, // e.g., "WorldEdit"
        params: { id: idString }
    });
}