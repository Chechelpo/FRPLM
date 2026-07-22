//src/extensions/extensions.ts!
import {fetchApi} from "@/services/apiClient";

export type ExtensionDTO = {
    id: string;
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


