// src/extensions/HostBindings.ts
import { Character } from "@/domain/Characters";
import {
    Location,
    Region,
    World
} from "@/domain/World";
import {
    Entry,
    Lorebook
} from "@/domain/Lorebook";
import { Tag } from "@/domain/Tag";
import { LLMConnection } from "@/domain/Connection";
import {
    PromptSection,
    PromptTemplate
} from "@/domain/Prompts";
import {
    createEntity,
    deleteEntity,
    fetch_all,
    fetchOne
} from "@/core/ABSEntity";
import { EntityTypes } from "@/domain/EntityTypes";
import {
    registerPrimitiveEditors
} from "@/extensions/registerPrimitiveEditors";
import type {
    ExtensionConfig
} from "@/extensions/extensions";
import { fetchApi } from "@/services/apiClient";


import type {
    FrplmExtensionBindings,
    FrplmHostBindings
} from "../../sdk/src/HostSdkBindings";
import {FRPLM_COMPONENT_TAGS} from "../../sdk/src/components";

registerPrimitiveEditors();

const entities = {
    Character,
    World,
    Region,
    Location,
    PromptTemplate,
    PromptSection,
    LLMConnection,
    Lorebook,
    Entry,
    Tag
} as const;

const api = {
    fetchOne,
    fetch_all,
    createEntity,
    deleteEntity
} as const;

window.FrplmHost = {
    entities,
    components: FRPLM_COMPONENT_TAGS,
    api,
    EntityTypes
} satisfies FrplmHostBindings;

window.FrplmExtension = {
    /**
     * Fetches the current configuration JSON for an extension.
     *
     * Returns an empty object when the extension has no stored
     * configuration yet (e.g. first time the panel is opened).
     * This avoids a JSON parse error on the empty 200 body the
     * backend returns for unconfigured extensions.
     */
    async getConfig(
        extensionId: string
    ): Promise<ExtensionConfig> {
        const response = await fetchApi(
            `api/extensions/${extensionId}/config`
        );

        if (!response.ok) {
            throw new Error(
                `Failed to fetch config for ${extensionId}`
            );
        }

        const text = await response.text();

        if (!text.trim()) {
            return {} as ExtensionConfig;
        }

        try {
            return JSON.parse(text) as ExtensionConfig;
        } catch (cause) {
            throw new Error(
                `Stored config for ${extensionId} is not valid JSON: ${text}`,
                { cause }
            );
        }
    },
    /**
     * Saves an extension configuration.
     *
     * The backend is responsible for validating the submitted
     * configuration.
     */
    async saveConfig(
        extensionId: string,
        config: ExtensionConfig
    ): Promise<boolean> {
        const response = await fetchApi(
            `api/extensions/${extensionId}/config`,
            {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(config)
            }
        );

        if (!response.ok) {
            const message = await response.text();

            throw new Error(
                message || "Failed to save config"
            );
        }

        return true;
    },
    logDebug(id: string, message: string): void {
        console.debug(`[${id}] : ${message}`)
    },
    logError(id: string, message: string): void {
        console.error(`[${id}] : ${message}`)
    },
    logInfo(id: string, message: string): void {
        console.info(`[${id}] : ${message}`)
    },

    /**
     * Shows a toast notification in the host application.
     */
    notify(
        message: string,
        type: "success" | "error" | "info" = "info"
    ): void {
        window.dispatchEvent(
            new CustomEvent("frplm-toast", {
                detail: {
                    message,
                    type
                }
            })
        );
    }
} satisfies FrplmExtensionBindings;

/**
 * Legacy compatibility API.
 *
 * New extensions should use:
 *
 * - window.FrplmHost
 * - window.FrplmExtension
 */
window.FRPLMHostSDK = {
    ...entities,
    ...api,

    components: FRPLM_COMPONENT_TAGS,

    EntityTypes,

    getConfig:
    window.FrplmExtension.getConfig,

    saveConfig:
    window.FrplmExtension.saveConfig,

    notify:
    window.FrplmExtension.notify,

    // Temporary direct aliases for older extension code.
    AutoCompleteBox:
    FRPLM_COMPONENT_TAGS.AutoCompleteBox,

    BooleanTickBox:
    FRPLM_COMPONENT_TAGS.BooleanTickBox,

    BooleanToggle:
    FRPLM_COMPONENT_TAGS.BooleanToggle,

    LongTextBox:
    FRPLM_COMPONENT_TAGS.LongTextBox,

    NumberInput:
    FRPLM_COMPONENT_TAGS.NumberInput,

    NumberSlider:
    FRPLM_COMPONENT_TAGS.NumberSlider,

    ShortTextBox:
    FRPLM_COMPONENT_TAGS.ShortTextBox,

    SingleAutoComplete:
    FRPLM_COMPONENT_TAGS.SingleAutoComplete,

    SingleEnumInput:
    FRPLM_COMPONENT_TAGS.SingleEnumInput
};