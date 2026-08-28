import {     fetchApi } from "@frplm/host-sdk";
//src/extensions/extensions.ts!

export type ExtensionDTO = {
    id: string;
    enabled: boolean;
    displayName: string;
    description: string;
}

export async function fetchExtensions() : Promise<ExtensionDTO[]> {
    return await fetchApi(
        `api/extensions`,
        {
            method:'GET'
        }
    ).then(async response => await response.json() as ExtensionDTO[])
}


