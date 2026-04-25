//import { setGlobalError } from "@/services/GlobalError";
import {EntityABS} from "@/frameworks/entities/EntityABS";
import {DataRecord, DTO, KeyRecord} from "@/types/DTOs";
import {log_error} from "@/services/ErrorHandler";
import {ControllerType} from "@/config/ControllerType";
import {QueryAction} from "@/frameworks/entities/queries";

const BASE:string ="http://localhost:8080/api";
const API_BASE: string = "api";

const ENTITY_SUFFIX:string = "entity";
const QUERY_SUFFIX:string = "query";

export function getEntityController(object_type:ControllerType): URL{
    return new URL(`${API_BASE}/${object_type}`, BASE);
}
function getQueryPath(object_type:ControllerType): URL {
    return new URL(`${API_BASE}/${object_type}/${QUERY_SUFFIX}`, BASE)
}

function getPathWithIDParams<Key extends KeyRecord>(object_type:ControllerType, key:Key | null): URL {
    const url = new URL(`${API_BASE}/${object_type}/${ENTITY_SUFFIX}`, BASE);
    if (key == null) return url;

    // identityParams come from query string
    for (const [k, v] of Object.entries(key)) {
        console.log('{k}, {v}')
        if (v === undefined) continue;
        // Spring @RequestParam Map<String, Object> will parse strings; send canonical string form
        url.searchParams.set(k, String(v));
    }

    return url;
}

export async function fetch_all<
    Key extends KeyRecord,
    Data extends DataRecord,
    T extends EntityABS<Key,Data>
>(
    object_type: ControllerType,
    ctor: new (dto: DTO, object_type:ControllerType) => T
    ): Promise<T[]>{
    console.log(getEntityController(object_type));
    console.log(getQueryPath(object_type));
    const response = await fetchApi(
        getQueryPath(object_type).toString()
        , {
            method:"POST",
            headers: new Headers({accept:"application/json"})
        });
    const result:DTO[] = await response.json() as DTO[];

    return result.map(dto => new ctor(dto,object_type));
}

export async function fetchOne<
    Key extends KeyRecord,
    Data extends DataRecord,
    T extends EntityABS<Key, Data>
>(
    key:Key,
    object_type:ControllerType,
    ctor: new (dto: DTO, object_type:ControllerType) => T
): Promise<T>{
    const response = await fetchApi(
        getPathWithIDParams<Key>(object_type,key).toString(),
        {
            method:"GET"
        }
        );

    return new ctor(await response.json() as DTO, object_type) ;
}

export async function create_Entity<
    Key extends KeyRecord,
    Data extends DataRecord,
    T extends EntityABS<Key,Data>,
>(
    initial_key: Key | null,
    initial_data: Data | null,
    object_type: ControllerType,
    ctor: new (dto: DTO, object_type:ControllerType) => T
    ): Promise<T>{
    console.log(initial_data);
    const response = await fetchApi(
        getPathWithIDParams(object_type, initial_key).toString(),
        {
            method: "POST",
            body: JSON.stringify(initial_data),
            headers: new Headers({"Content-Type": "application/json"})
        }
    )

    return new ctor(await response.json() as DTO, object_type);
}

export async function UpdateEntityField<
    Data extends DataRecord,
    Key extends KeyRecord,
    Field extends keyof Data,
>(
    key: Key,
    field:Field,
    value: Data[Field],
    object_type:ControllerType,
): Promise<boolean>
{
    console.log(`Updating ${String(field)} of [${object_type}], key:`, Object.entries(key), `new value:`, value);
    const response = await fetchApi(
        getPathWithIDParams(object_type, key).toString(),
        {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ [field]: value }),
    });

    return response.status === 200;
}

export async function QueryEntities<
    Key extends KeyRecord,
    Data extends DataRecord,
    T extends EntityABS<Key, Data>
>(
    queries:QueryAction<Key,Data>[],
    object_type:ControllerType,
    ctor: new (dto: DTO, object_type:ControllerType) => T
): Promise<T[]>{
    const response = await fetchApi(
        getEntityController(object_type).toString(),
        {
            method:"POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(queries),
        }
    )
    const result:DTO[] = await response.json() as DTO[];

    return result.map(dto => new ctor(dto, object_type));
}

export async function fetchApi(
    input: RequestInfo,
    init?: RequestInit
): Promise<Response> {

    const res = await fetch(input, init);

    if (!res.ok) {
        // Attempt to parse backend ErrorResponse
        try {
            const err = await res.json();
            //setGlobalError(err);
            log_error("Error when fetching api", err);
        } catch {
            /*
            setGlobalError({
                status: res.status,
                error: "Unknown error",
                message: res.statusText,
                path: String(input)
            });*/
        }

        throw res; // still reject for local handling if needed
    }

    return res;
}
