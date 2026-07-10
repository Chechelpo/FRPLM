import { FieldInfo } from "@/core/FieldMetadata";
import {CommonFields} from "@/utils/CommonFields";
import {Equatable, ValueComparable} from "@/types/Equatable";
import {EntityTypes} from "@/domain/EntityTypes";
import {DTO} from "@/types/DTOs";
import {QueryAction} from "@/core/queries";
import {API_BASE} from "@/config";
import {setGlobalError} from "@/core/GlobalError";
import {ApiRequestError} from "@/core/ApiRequestError";
import {ReferenceCodec, ReferenceParsers} from "@/utils/ReferenceCodec";

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

    protected abstract getReferenceKeyOrder(): readonly (keyof Key & string)[];

    public asReference(): string {
        return ReferenceCodec.encodeKey(
            this.getEntityType(),
            this.key,
            this.getReferenceKeyOrder(),
        );
    }

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

const ENTITY_SUFFIX:string = "entity";
const QUERY_SUFFIX:string = "query";

export function getEntityController(object_type:EntityTypes): URL{
    return new URL(`${API_BASE}/${object_type}`, API_BASE);
}
function getQueryPath(object_type:EntityTypes): URL {
    return new URL(`${API_BASE}/${object_type}/${QUERY_SUFFIX}`, API_BASE)
}

function getPathWithIDParams<Key extends KeyRecord>(object_type:EntityTypes, key:Partial<Key> | null): URL {
    const url = new URL(`${API_BASE}/${object_type}/${ENTITY_SUFFIX}`, API_BASE);
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

export type EntityConstructor<
    Key extends KeyRecord,
    Data extends DataRecord,
    T extends ABSEntity<Key, Data>,
> = new (dto: DTO, objectType: EntityTypes) => T;

export async function fetchFromReference<
    Key extends KeyRecord,
    Data extends DataRecord,
    T extends ABSEntity<Key, Data>,
>(
    reference: string,
    objectType: EntityTypes,
    keyOrder: readonly (keyof Key & string)[],
    parsers: ReferenceParsers<Key>,
    ctor: EntityConstructor<Key, Data, T>,
): Promise<T> {
    const key = ReferenceCodec.decodeKey<Key>(
        reference,
        objectType,
        keyOrder,
        parsers,
    );

    return fetchOne<Key, Data, T>(
        key,
        objectType,
        ctor,
    );
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
            method:"GET"
        }
    );
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
    let path = new URL(`${getEntityController(object_type).toString()}/query`);
    appendIDParams<Key>(path, key);
    const response = await fetchApi(
        path.toString(),
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

type ErrorResponse = {
    status:number,
    type:string,
    message:string,
    path:string,
}


export interface FetchApiInit extends RequestInit {
    /**
     * Set to false when the caller handles the failure itself and no global
     * notification should be displayed.
     */
    showGlobalError?: boolean;
}

function getRequestPath(input: RequestInfo | URL): string {
    if (typeof input === "string") {
        return input;
    }

    if (input instanceof URL) {
        return input.toString();
    }

    return input.url;
}

function isRecord(value: unknown): value is Record<string, unknown> {
    return typeof value === "object" && value !== null;
}

function readString(
    source: Record<string, unknown>,
    key: string,
): string | undefined {
    const value = source[key];

    return typeof value === "string"
        ? value
        : undefined;
}

function readNumber(
    source: Record<string, unknown>,
    key: string,
): number | undefined {
    const value = source[key];

    return typeof value === "number"
        ? value
        : undefined;
}

async function parseErrorResponse(
    response: Response,
    fallbackPath: string,
): Promise<ErrorResponse> {
    /*
     * Parse a clone so that the original Response body remains available to
     * local error handlers through ApiRequestError.response.
     */
    const clonedResponse = response.clone();
    const contentType =
        clonedResponse.headers.get("content-type")?.toLowerCase() ?? "";

    let body: unknown;

    try {
        if (contentType.includes("application/json")) {
            body = await clonedResponse.json();
        } else {
            const text = await clonedResponse.text();

            if (text.trim()) {
                body = {
                    message: text,
                };
            }
        }
    } catch (error) {
        console.warn("Failed to parse backend error response:", error);
    }

    const record = isRecord(body)
        ? body
        : {};

    return {
        status:
            readNumber(record, "status") ??
            response.status,

        type:
            readString(record, "type") ??
            readString(record, "error") ??
            response.statusText ??
            "HttpError",

        message:
            readString(record, "message") ??
            response.statusText ??
            `Request failed with HTTP status ${response.status}`,

        path:
            readString(record, "path") ??
            fallbackPath,
    };
}

export async function fetchApi(
    input: RequestInfo | URL,
    init: FetchApiInit = {},
): Promise<Response> {
    const {
        showGlobalError = true,
        ...requestInit
    } = init;

    const path = getRequestPath(input);

    let response: Response;

    try {
        response = await fetch(input, requestInit);
    } catch (cause) {
        /*
         * fetch() only rejects for network-level failures, aborted requests,
         * CORS failures, malformed URLs, and similar transport errors.
         */
        const errorResponse: ErrorResponse = {
            status: 0,
            type:
                cause instanceof DOMException && cause.name === "AbortError"
                    ? "AbortError"
                    : "NetworkError",
            message:
                cause instanceof Error
                    ? cause.message
                    : "The backend could not be reached.",
            path,
        };

        /*
         * Aborted requests are commonly intentional and normally should not
         * produce a global error notification.
         */
        const isAbort =
            cause instanceof DOMException &&
            cause.name === "AbortError";

        if (showGlobalError && !isAbort) {
            setGlobalError(errorResponse);
        }

        throw new ApiRequestError(
            errorResponse,
            undefined,
            { cause },
        );
    }

    if (response.ok) {
        return response;
    }

    const errorResponse = await parseErrorResponse(
        response,
        path,
    );

    console.error(
        "Backend request failed:",
        errorResponse,
    );

    if (showGlobalError) {
        setGlobalError(errorResponse);
    }

    throw new ApiRequestError(
        errorResponse,
        response,
    );
}
