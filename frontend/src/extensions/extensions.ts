//src/extensions/extensions.ts!
import {fetchApi} from "@/core/ABSEntity";
import {API_BASE} from "@/config";

export type ExtensionDTO = {
    id: string;
    displayName: string;
    description: string;
    configMap: null
}

export async function fetchExtensions() : Promise<ExtensionDTO[]> {
    return await fetchApi(
        `${API_BASE}/extensions`,
        {
            method:'GET'
        }
    ).then(async response => await response.json() as ExtensionDTO[])
}

// src/extensions/extensionSdk.ts

export interface ExtensionField {
    name: string;
    description: string | null;
    kind: 'String' | 'Number' | 'Double' | 'Boolean';
    value: unknown;
    possible_values: string[];
}

export interface ExtensionConfig {
    [key: string]: unknown;
}


