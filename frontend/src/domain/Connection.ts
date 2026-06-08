import {ABSEntity, appendIDParams, fetchApi} from "@/frameworks/ABSEntity";
import {EntityTypes} from "@/domain/EntityTypes";
import {DTO} from "@/types/DTOs";
import {API_BASE} from "@/config";

export type LLMConnectionKeys = {id:number}
export type LLMConnectionData = {
    type: number;
    name:string,
    modelID:string | null,
}

export class LLMConnection extends ABSEntity<LLMConnectionKeys, LLMConnectionData>{
    getEntityType(): EntityTypes {
        return EntityTypes.LLM;
    }
    public async assignNewKey(key:string){
        const newItem:ApiKey = await ApiKey.create(key, LLMBackends.NANOGPT.id)
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

        interface TestResponse { status:boolean , message:string }
        return (await response.json() as TestResponse).status;
    }

    public async getModels(): Promise<ModelResponse[]> {
        const url = new URL(`${API_BASE}/${EntityTypes.LLM}/models`);
        appendIDParams(url, this.key);
        const response = await fetchApi(
            url.toString(),
            {
                method: "GET"
            }
        )
        return (await response.json() as ModelResponses).data;
    }
}

export type APIKeys = {id:number}
export class ApiKey extends ABSEntity<APIKeys, any>{
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
export interface ModelResponses {
    object: string;
    data: ModelResponse[];
}
export interface ModelResponse {
    id: string;
    object: string;
    name: string;
    context_length: number | null;
}

