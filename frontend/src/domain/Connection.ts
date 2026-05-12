import {ABSEntity, appendIDParams, fetchApi} from "@/frameworks/ABSEntity";
import {EntityTypes} from "@/domain/EntityTypes";
import {DTO} from "@/types/DTOs";
import {API_BASE} from "@/config";

export type LLMConnectionKeys = {id:number}
export type LLMConnectionData = {
    host_id: number,
    type: number;
    api_key?: number | null,
    name?:string,
    modelID?:string | null,
}

export class LLMConnection extends ABSEntity<LLMConnectionKeys, LLMConnectionData>{
    getEntityType(): EntityTypes {
        return EntityTypes.LLM;
    }
    public async assignNewKey(key:string){
        const newItem:ApiKey = await ApiKey.create(key, LLMBackends.NANOGPT.id)
        await this.update('api_key', newItem.get('id'))
    }

    public async testConnection(): Promise<boolean> {
        const url = new URL(`${API_BASE}/${EntityTypes.LLM}/test`);
        appendIDParams(url, this.key);
        const response = await fetchApi(
            url.toString(),
            {
                method: "GET"
            }
        )
        return await response.json() as boolean;
    }
}

export type APIKeys = {id:number}
export class ApiKey extends ABSEntity<any, any>{
    getEntityType(): EntityTypes {
        return EntityTypes.API_KEY;
    }

    static async create(key:string, host_id: number) : Promise<ApiKey> {
        const response = await fetchApi(
            `${API_BASE}/${EntityTypes.API_KEY}/new/${host_id}`,
            {
                method: "POST",
                headers: new Headers({
                    "Content-Type": "application/json",
                    "Accept": "application/json",
                }),
                body: JSON.stringify({ key })
            }
        )

        return new ApiKey(await response.json() as DTO, EntityTypes.API_KEY);
    }

    async update<F extends keyof any>(field: F, value: any): Promise<boolean> {
        console.error("Api keys can't be updated normally. Use their specific methods.");
        return false;
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

export function getBackendFromID(id: number): LLMBackend {
    return LLMBackends.NANOGPT
}

export type BackendLLMModel = {
    readonly id: string;
    readonly label?: string;
    readonly context_length?: number | null;
    readonly max_output_tokens?: number | null;
}

export async function getLLMModels(backend:LLMBackend): Promise<BackendLLMModel[]> {
    switch (backend) {
        case LLMBackends.NANOGPT: return getNanoModels();
        default: throw new Error("No backend to fetch models from");
    }
}

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// NANOGPT
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
type LLMBackendKey = keyof typeof LLMBackends;
type LLMBackendValue = typeof LLMBackends[LLMBackendKey];

type NanoGPTModel = {
    id: string;
    object: "model" | string;
    created?: number;
    owned_by?: string;
    name?: string;
    description?: string;
    context_length?: number | null;
    max_output_tokens?: number | null;
};
type NanoGPTModelsResponse = {
    object: "list" | string;
    data: NanoGPTModel[];
};
async function getNanoModels(): Promise<BackendLLMModel[]> {
    const response = await fetchApi(
        `${LLMBackends.NANOGPT.host}/api/v1/models`,
        {
            method: "GET",
            headers: new Headers({
                "Accept": "application/json",
            }),
        }
    );

    if (!response.ok) {
        throw new Error(
            `Failed to fetch LLM models from ${LLMBackends.NANOGPT.name}: HTTP ${response.status}`
        );
    }

    const body: unknown = await response.json();

    if (!isNanoGPTModelsResponse(body)) {
        throw new Error(`Invalid models response from ${LLMBackends.NANOGPT.name}`);
    }

    return body.data.map((model) => (
        {
            id: model.id,
            label: model.name,
            context_length: model.context_length,
            max_output_tokens: model.max_output_tokens
        }
    ));
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