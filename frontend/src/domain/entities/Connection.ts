import {EntityABS} from "@/frameworks/entities/EntityABS";
import {EntityTypes} from "@/frameworks/entities/EntityTypes";
import {API_BASE, fetchApi} from "@/domain/entities/EntityFetch";

export type LLMConnectionKeys = {id:number}
export type LLMConnectionData = {
    host_id?: string | null,
    type?: number | null;
    api_key?: number | null,
    name?:string,
    model?:string | null,
}

export class LLMConnection extends EntityABS<LLMConnectionKeys, LLMConnectionData>{
    getEntityType(): EntityTypes {
        return EntityTypes.LLM;
    }

    public async assign_key(key:string): Promise<boolean> {
        const response = await fetchApi(
            `${API_BASE}/${EntityTypes.LLM}/entity/${key}`,
            {
                method: "POST",
                headers: new Headers({"Content-Type": "application/json"}),
                body:  JSON.stringify(key)
            }
        );

        return response.ok;
    }
}

export type LLMBackend = {
    readonly id: number;
    readonly name: string;
    readonly host: string;
};

export const LLMBackends = {
    NANOGPT: {
        id: 0,
        name: "NanoGPT",
        host: "https://nano-gpt.com",
    },
} as const satisfies Record<string, LLMBackend>;

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// NANOGPT
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
export type LLMBackendKey = keyof typeof LLMBackends;
export type LLMBackendValue = typeof LLMBackends[LLMBackendKey];

export type NanoGPTModel = {
    id: string;
    object: "model" | string;
    created?: number;
    owned_by?: string;
    name?: string;
    description?: string;
    context_length?: number | null;
    max_output_tokens?: number | null;
};
export type NanoGPTModelsResponse = {
    object: "list" | string;
    data: NanoGPTModel[];
};
export async function getModelNames(backend: LLMBackend): Promise<string[]> {
    const response = await fetchApi(
        `${backend.host}/api/v1/models`,
        {
            method: "GET",
            headers: new Headers({
                "Accept": "application/json",
            }),
        }
    );

    if (!response.ok) {
        throw new Error(
            `Failed to fetch LLM models from ${backend.name}: HTTP ${response.status}`
        );
    }

    const body: unknown = await response.json();

    if (!isNanoGPTModelsResponse(body)) {
        throw new Error(`Invalid models response from ${backend.name}`);
    }

    return body.data.map((model) => model.id);
}

function isNanoGPTModelsResponse(value: unknown): value is NanoGPTModelsResponse {
    if (typeof value !== "object" || value === null) {
        return false;
    }

    const candidate = value as Partial<NanoGPTModelsResponse>;

    if (!Array.isArray(candidate.data)) {
        return false;
    }

    return candidate.data.every((model) => {
        if (typeof model !== "object" || model === null) {
            return false;
        }

        const candidateModel = model as Partial<NanoGPTModel>;

        return typeof candidateModel.id === "string";
    });
}