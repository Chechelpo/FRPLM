//src/extensions/extensions.ts!
import {API_BASE} from "@/config";
import {fetchApi} from "@/services/apiClient";

export type ExtensionDTO = {
    id: string;
    displayName: string;
    description: string;
    configMap: null
}

export async function fetchExtensions() : Promise<ExtensionDTO[]> {
    return await fetchApi(
        `api/extensions`,
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


