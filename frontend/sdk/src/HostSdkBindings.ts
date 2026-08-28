// src/HostSdkBindings.ts
import type { Character } from "./domain/Characters.js";
import type { World, Location, Region } from "./domain/World.js";
import type { Entry, Lorebook } from "./domain/Lorebook.js";
import type { Tag } from "./domain/Tag.js";
import type { LLMConnection } from "./domain/Connection.js";
import type { PromptSection, PromptTemplate } from "./domain/Prompts.js";
import type { fetchOne, fetch_all, createEntity, deleteEntity } from "./core/ABSEntity.js";
import type { EntityTypes } from "./domain/EntityTypes.js";
import { FrplmComponentRegistry } from "./components.js";

/**
 * The bindings the host application publishes on `window.FrplmHost`
 * so that extension panels can interact with the application.
 */
export interface FrplmHostBindings {
    entities: {
        Character: typeof Character;
        World: typeof World;
        Region: typeof Region;
        Location: typeof Location;
        PromptTemplate: typeof PromptTemplate;
        PromptSection: typeof PromptSection;
        LLMConnection: typeof LLMConnection;
        Lorebook: typeof Lorebook;
        Entry: typeof Entry;
        Tag: typeof Tag;
    };
    components: FrplmComponentRegistry;
    api: {
        fetchOne: typeof fetchOne;
        fetch_all: typeof fetch_all;
        createEntity: typeof createEntity;
        deleteEntity: typeof deleteEntity;
    };
    EntityTypes: typeof EntityTypes;
}

/**
 * The bindings the host application publishes on
 * `window.FrplmExtension`. Extension panels call these helpers to
 * persist their configuration and surface notifications.
 */
export interface FrplmExtensionBindings {
    getConfig: (id: string) => Promise<any>;
    saveConfig: (id: string, config: any) => Promise<boolean>;
    logError: (id: string, message:string) => void;
    logDebug: (id: string, message:string) => void;
    logInfo: (id:string, message:string) => void;
    notify: (message: string, type?: "success" | "error" | "info") => void;
}

declare global {
    interface Window {
        FrplmHost: FrplmHostBindings;
        FrplmExtension: FrplmExtensionBindings;
        FRPLMHostSDK: any;
    }
}

export {};
