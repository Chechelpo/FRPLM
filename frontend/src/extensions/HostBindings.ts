// src/extensions/HostBindings.ts

import { Character } from "@/domain/Characters";
import { World, Location, Region } from "@/domain/World";
import { Entry, Lorebook } from "@/domain/Lorebook";
import { Tag } from "@/domain/Tag";
import { LLMConnection } from "@/domain/Connection";
import { PromptSection, PromptTemplate } from "@/domain/Prompts";
import {fetchOne, fetch_all, createEntity, deleteEntity} from "@/core/ABSEntity";
import { EntityTypes } from "@/domain/EntityTypes";
import { defineCustomElement } from "vue";
import SingleEnumInput from "@/components/utils/primitiveEditors/SingleEnumInput.vue";

// Import the types so we can cast the window object
import type { FrplmHostBindings, FrplmExtensionBindings } from "../../sdk/src/HostSdkBindings";
import {ExtensionConfig} from "@/extensions/extensions";
import {API_BASE} from "@/config";
import {fetchApi} from "@/services/apiClient";

const FrplmEnumInput = defineCustomElement(SingleEnumInput);
customElements.define('frplm-enum-input', FrplmEnumInput);


window.FrplmHost = {
    entities: { Character, World, Region, Location, PromptTemplate, PromptSection, LLMConnection, Lorebook, Entry, Tag },
    components: { SingleEnumInput: 'frplm-enum-input' },
    api: { fetchOne, fetch_all, createEntity, deleteEntity },
    EntityTypes
} satisfies FrplmHostBindings;

window.FrplmExtension = {
    /**
     * Fetches the current configuration JSON for this extension.
     */
    getConfig: async (extensionId: string): Promise<ExtensionConfig> => {
        const res = await fetchApi(`api/extensions/${extensionId}/config`);
        if (!res.ok) throw new Error(`Failed to fetch config for ${extensionId}`);
        return res.json();
    },

    /**
     * Saves the new configuration. The backend will validate it.
     * Throws an error if validation fails.
     */
    saveConfig: async (extensionId: string, config: ExtensionConfig): Promise<boolean> => {
        const res = await fetchApi(`api/extensions/${extensionId}/config`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(config),
        });
        if (!res.ok) {
            const err = await res.text();
            throw new Error(err || 'Failed to save config');
        }
        return true;
    },

    /**
     * Shows a toast notification in the host Vue app.
     */
    notify: (message: string, type: 'success' | 'error' | 'info' = 'info') => {
        window.dispatchEvent(new CustomEvent('frplm-toast', { detail: { message, type } }));
    }
};

window.FRPLMHostSDK = {
    Character,
    World,
    Region,
    Location,
    PromptTemplate,
    PromptSection,
    LLMConnection,
    Lorebook,
    Entry,
    Tag,

    SingleEnumInput: "frplm-enum-input",

    fetchOne,
    fetch_all,
    createEntity,
    deleteEntity,

    EntityTypes,

    getConfig: window.FrplmExtension.getConfig,
    saveConfig: window.FrplmExtension.saveConfig,
    notify: window.FrplmExtension.notify
};