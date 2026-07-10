import type { Character } from "../../src/domain/Characters";
import type { World, Location, Region } from "../../src/domain/World";
import type { Entry, Lorebook } from "../../src/domain/Lorebook";
import type { Tag } from "../../src/domain/Tag";
import type { LLMConnection } from "../../src/domain/Connection";
import type { PromptSection, PromptTemplate } from "../../src/domain/Prompts";
import type { fetchOne, fetch_all, createEntity, deleteEntity } from "../../src/core/ABSEntity";
import type { EntityTypes } from "../../src/domain/EntityTypes";

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
    components: {
        SingleEnumInput: "frplm-enum-input";
    };
    api: {
        fetchOne: typeof fetchOne;
        fetch_all: typeof fetch_all;
        createEntity: typeof createEntity;
        deleteEntity: typeof deleteEntity;
    };
    EntityTypes: typeof EntityTypes;
}

export interface FrplmExtensionBindings {
    getConfig: (id: string) => Promise<any>;
    saveConfig: (id: string, config: any) => Promise<boolean>;
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