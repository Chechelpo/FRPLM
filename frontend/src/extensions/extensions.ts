import {fetchApi} from "@/frameworks/ABSEntity";
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
export async function fetchConfigPanel(extensionId: string): Promise<string | null> {
    const response = await fetch(`/api/extensions/${extensionId}/config-panel`, {
        method: "GET",
        headers: {
            Accept: "text/html",
        },
    });

    if (response.status === 404) {
        return null;
    }

    if (!response.ok) {
        throw new Error(`Failed to fetch config panel for ${extensionId}`);
    }

    return await response.text();
}
