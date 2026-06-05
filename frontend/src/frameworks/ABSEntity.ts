import { FieldInfo } from "@/frameworks/FieldMetadata";
import {CommonFields} from "@/utils/CommonFields";
import {Equatable, ValueComparable} from "@/types/Equatable";
import {EntityTypes} from "@/domain/EntityTypes";
import {DTO} from "@/types/DTOs";
import {QueryAction} from "@/frameworks/queries";

/** JSON-safe primitives typically received from backend SQL row payloads */
export type Primitives = string | number | boolean | null;

/** See backend's fields for declaring correct ones */
export type KeyRecord = Record<string, Primitives>;
/** See backend's fields for declaring correct ones */
export type DataRecord = Record<string, Primitives>;
/** Combination Key-Data fields of an entity type */
export type EntityField<keys extends KeyRecord, data extends DataRecord> =  keyof keys | keyof data;
export type EntityFieldsMap<keys extends KeyRecord, data extends DataRecord> = Map<EntityField<keys,data>, FieldInfo>

export abstract class ABSEntity<Key extends KeyRecord, Data extends DataRecord> implements ValueComparable {
    private static registered:Set<EntityTypes> = new Set;
    
    static async initialise(): Promise<void> {}

    /** Key attributes of entity (composite identity) */
    public readonly key: Readonly<Key>;
    /** Other attributes. Must not collide with KeyMap */
    public dataMap: Data;

    public constructor(dto:DTO, expected_type?: EntityTypes) {
        console.debug(`Received ${expected_type} with dto: \n ${JSON.stringify(dto)}`)
        expected_type = expected_type ? expected_type : this.getEntityType();
        if (dto.type !== expected_type) {
            console.error(`Mismatch in entity type: Response:${dto.type} vs expected:${expected_type}`)
            throw new Error(`Mismatch in entity type: Response:${dto.type} vs expected:${expected_type}`);
        }
        if (Object.entries(dto.key).length == 0) {
            console.error("Key with no entries")
            throw new Error(`Entity with no keys: ${dto.key}`);
        }

        this.key = dto.key as Key;
        this.dataMap = dto.payload as Data;
    }

    /** Method used for avoiding duplication of entity type string */
    abstract getEntityType(): EntityTypes;
    public getIterationArr():EntityField<Key, Data>[]{
        return []
    }
    /*
    public getFields(): EntityFieldsMap<Key, Data> {
        return ABSEntity.getFields(this.getEntityType())
    }
    public getFieldInfoOf<Field extends EntityField<Key, Data>>(field:Field):FieldInfo{
        return ABSEntity.getFields(this.getEntityType()).get(field as string)!;
    }*/


    /**
     * Get a value by field name.
     * - Prefers data fields, then key fields.
     * - Overloads provide type-safety when passing known keys at compile time.
     */
    public get<K extends keyof Data>(key: K): Data[K];
    public get<K extends keyof Key>(key: K): Key[K];
    public get(key: string): Primitives {
        if (key in this.dataMap) return this.dataMap[key as keyof Data];
        if (key in this.key) return this.key[key as keyof Key];
        throw new Error(`No key with name ${key}`);
    }

    public getCommon(field: CommonFields): Primitives | null {
        const k = field as unknown as string; // enum value is a string at runtime
        if (k in this.dataMap) return this.dataMap[k as keyof Data];
        if (k in this.key)     return this.key[k as keyof Key];
        return null;
    }
    public hasAttribute(name: string): boolean {
        return name in this.dataMap || name in this.key;
    }


    /**
     * Updates a field of this entity, instantly triggering a patch to the backend.
     * Field names are restricted to Data keys, and values are restricted to the field's type.
     */
    public async update<F extends keyof Data>(field: F, value: Data[F]): Promise<boolean> {
        console.debug(`Updating ${String(field)} of [${this.getEntityType()}], key:`, JSON.stringify(this.key), `new value:`, value);
        const updated: boolean = await UpdateEntityField(
            this.key,
            field,
            value,
            this.getEntityType()
        );

        if (updated) {
            this.dataMap[field] = value;
        }

        return updated;
    }

    protected throw_if_collision(keys: Key, data: Data): void {
        for (const k of Object.keys(keys)) {
            if (k in data) {
                throw new Error(
                    `Key/data collision detected for field '${k}'. ` +
                    `Key fields must not appear in data payload.`
                );
            }
        }
    }

    /** @return true if class types match and all key values are equal */
    public equals(other: unknown): boolean {
        if (!(other instanceof ABSEntity)) {
            return false;
        }

        if (other.constructor !== this.constructor) {
            return false;
        }

        const casted = other as ABSEntity<Key, Data>;
        const thisKeys = Object.keys(this.key) as Array<keyof Key>;

        if (thisKeys.length !== Object.keys(casted.key).length) {
            return false;
        }

        for (const k of thisKeys) {
            if (this.key[k] !== casted.key[k]) {
                return false;
            }
        }

        return true;
    }

    public toString():string {
        return `${this.getEntityType()} \n 
        Key: ${JSON.stringify(this.key)} \n 
        Data: ${JSON.stringify(this.dataMap)}
        `
    }

    public hashKey(): string {
        const entries = Object.entries(this.key)
            .sort(([a], [b]) => a.localeCompare(b));

        const encoded = entries.map(([k, v]) => `${k}=${this.encodePrimitive(v)}`);

        return `${this.getEntityType()}|${encoded.join("&")}`;
    }
    private encodePrimitive(value: Primitives): string {
        if (value === null) return "null";

        switch (typeof value) {
            case "string":
                return `s:${value}`;
            case "number":
                return `n:${value}`;
            case "boolean":
                return `b:${value}`;
            default:
                throw new Error(`Unsupported primitive type: ${String(value)}`);
        }
    }
}


const BASE:string ="http://localhost:8080/api";
export const API_BASE = "http://localhost:8080/api";

const ENTITY_SUFFIX:string = "entity";
const QUERY_SUFFIX:string = "query";

export function getEntityController(object_type:EntityTypes): URL{
    return new URL(`${API_BASE}/${object_type}`, BASE);
}
function getQueryPath(object_type:EntityTypes): URL {
    return new URL(`${API_BASE}/${object_type}/${QUERY_SUFFIX}`, BASE)
}

function getPathWithIDParams<Key extends KeyRecord>(object_type:EntityTypes, key:Partial<Key> | null): URL {
    const url = new URL(`${API_BASE}/${object_type}/${ENTITY_SUFFIX}`, BASE);
    if (key == null) return url;
    // identityParams come from query string
    appendIDParams(url, key)
    return url;
}

/**
 * @param url mutates, will append params
 * @param key to append
 */
export function appendIDParams<Key extends KeyRecord>(url:URL, key: Partial<Key>): void {
    // identityParams come from query string
    for (const [k, v] of Object.entries(key)) {
        if (v === undefined) continue;
        // Spring @RequestParam Map<String, Object> will parse strings; send canonical string form
        url.searchParams.set(k, String(v));
    }
}

export async function fetch_all<
    Key extends KeyRecord,
    Data extends DataRecord,
    T extends ABSEntity<Key,Data>
>(
    object_type: EntityTypes,
    ctor: new (dto: DTO, object_type:EntityTypes) => T
): Promise<T[]>{
    const response = await fetchApi(
        getQueryPath(object_type).toString()
        , {
            method:"GET",
            headers:{Accept:"application/json"}
        });
    const result:DTO[] = await response.json() as DTO[];

    return result.map(dto => new ctor(dto,object_type));
}
export async function fetchMatching<
    Key extends KeyRecord,
    Data extends DataRecord,
    T extends ABSEntity<Key, Data>
>(
    key:Partial<Key>,
    object_type:EntityTypes,
    ctor: new (dto: DTO, object_type:EntityTypes) => T
): Promise<T[]>{
    const response = await fetchApi(
        getPathWithIDParams<Key>(object_type,key).toString(),
        {
            method:"GET",
            headers: new Headers({accept:"application/json"})
        }
    );

    return (await response.json() as DTO[]).map(dto => new ctor(dto,object_type));
}

export async function fetchOne<
    Key extends KeyRecord,
    Data extends DataRecord,
    T extends ABSEntity<Key, Data>
>(
    key:Key,
    object_type:EntityTypes,
    ctor: new (dto: DTO, object_type:EntityTypes) => T
): Promise<T>{
    const response = await fetchApi(
        getPathWithIDParams<Key>(object_type,key).toString(),
        {
            method:"GET"
        }
    );

    return new ctor(await response.json() as DTO, object_type) ;
}
export async function deleteEntity<
    Key extends KeyRecord
>(
    key:Key,
    object_type:EntityTypes,
): Promise<boolean>
{
    const response = await fetchApi(
        getPathWithIDParams<Key>(object_type,key).toString(),
        {
            method:"DELETE"
        }
    )
    return response.status === 200;
}
export async function createEntity<
    Key extends KeyRecord,
    Data extends DataRecord,
    T extends ABSEntity<Key,Data>,
>(
    initial_key: Partial<Key> | null,
    initial_data: Partial<Data> | null,
    object_type: EntityTypes,
    ctor: new (dto: DTO, object_type:EntityTypes) => T
): Promise<T>{
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
    field: Field,
    value: Data[Field],
    object_type:EntityTypes,
): Promise<boolean>
{
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
    T extends ABSEntity<Key, Data>
>(
    queries:QueryAction<Key,Data>[],
    object_type:EntityTypes,
    ctor: new (dto: DTO, object_type:EntityTypes) => T
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

type ErrorResponse = {
    status:number,
    type:string,
    message:string,
    path:string,
}

export async function fetchApi(
    input: RequestInfo,
    init?: RequestInit
): Promise<Response> {
    const res = await fetch(input, init);

    if (!res.ok) {
        // Attempt to parse backend ErrorResponse
        try {
            const err = await res.json() as ErrorResponse;
            //setGlobalError(err);
            console.error(`Encountered error when fetching backend. \n ${JSON.stringify(err, null, "\n")}`);
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
