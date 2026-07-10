import {ABSEntity, appendIDParams, fetch_all, fetchApi, fetchFromReference, fetchOne} from "@/core/ABSEntity";
import {EntityTypes} from "@/domain/EntityTypes";
import {DTO} from "@/types/DTOs";
import {API_BASE} from "@/config";
import {parseNumberKey} from "@/utils/ReferenceCodec";

export type LLMConnectionKeys = {id:number}
export type LLMConnectionData = {
    host_id: number;
    name:string,
    modelID:string | null,
    max_tokens: number
}

export class LLMConnection extends ABSEntity<LLMConnectionKeys, LLMConnectionData> {
    private static readonly REFERENCE_KEY_ORDER = ['id'] as const;

    getEntityType(): EntityTypes {
        return EntityTypes.LLM;
    }

    protected getReferenceKeyOrder(): readonly (keyof LLMConnectionKeys & string)[] {
        return LLMConnection.REFERENCE_KEY_ORDER;
    }

    public static async getFromReference(reference:string){
        return await fetchFromReference<LLMConnectionKeys, LLMConnectionData, LLMConnection>(
            reference, EntityTypes.LLM, this.REFERENCE_KEY_ORDER, {id:parseNumberKey}, LLMConnection
        )
    }

    public static async getAll() : Promise<LLMConnection[]>{
        return await fetch_all<LLMConnectionKeys, LLMConnectionData, LLMConnection>(
            EntityTypes.LLM, LLMConnection
        );
    }
    public static async getWithId(id:number) : Promise<LLMConnection> {
        return await fetchOne<LLMConnectionKeys, LLMConnectionData, LLMConnection>(
            {id:id},
            EntityTypes.LLM,
            LLMConnection
        );
    }

    public async assignNewKey(key:string) : Promise<void> {
        const newItem:ApiKey = await ApiKey.create(key, this.get('host_id'))
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

    protected getReferenceKeyOrder(): readonly (keyof APIKeys & string)[] {
        throw new Error("Tried to parse key reference");
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
    readonly id: number | null;
    readonly name: string;
    readonly host: string | null;
};
export const LLMBackends = {
    NANOGPT: {
        id: 0,
        name: "NanoGPT",
        host: "https://nano-gpt.com",
    },
    OPENROUTER: {
        id: 1 ,
        name: "OpenRouter",
        host: "https://openrouter.ai"
    },
    CUSTOM_OPENAI:{
        id: null,
        name: "OpenAI - Compatible",
        host: null
    }
} as const satisfies Record<string, LLMBackend>;
export const LLMBackendList: readonly LLMBackend[] =
    Object.values(LLMBackends);

export function getBackendFromID(id: number): LLMBackend {
    let backend = LLMBackendList.find(back => back.id === id);
    if (!backend) backend = LLMBackends.CUSTOM_OPENAI;
    return backend;
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

