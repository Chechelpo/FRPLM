//src/domain/ABSEntity.ts
import {FieldInfo} from "@/core/FieldMetadata";
import {CommonFields} from "@/utils/CommonFields";
import {ValueComparable} from "@/types/Equatable";
import {EntityTypes} from "@/domain/EntityTypes";
import {DTO} from "@/types/DTOs";
import {API_BASE} from "@/config";
import {ReferenceCodec, ReferenceParsers} from "@/utils/ReferenceCodec";
import {fetchApi} from "@/services/apiClient";
import {reactive} from "vue";

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
        console.debug(`[${expected_type}] with dto: \n ${JSON.stringify(dto)}`)
        expected_type = expected_type ? expected_type : this.getEntityType();
        if (dto.type !== expected_type) {
            console.error(`Mismatch in entity type: Response:${dto.type} vs expected:${expected_type}`)
            throw new Error(`Mismatch in entity type: Response:${dto.type} vs expected:${expected_type}`);
        }
        if (Object.entries(dto.key).length == 0) {
            console.error("Key with no entries")
            throw new Error(`Entity with no keys: ${dto.key}`);
        }
        this.throw_if_collision(dto.key as Key, dto.payload as Data)
        this.key = dto.key as Key;
        this.dataMap = reactive(dto.payload as Data) as unknown as Data;
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
        try{
            const updated: boolean = await UpdateEntityField(
                this.key,
                field,
                value,
                this.getEntityType()
            );
            if (updated) this.dataMap[field] = value;

            return updated;
        } catch (e){
            console.error(`Error when updating field ${String(field)} with new value ${value}`)
            return false;
        }
    }
    public async updateMany(
        values: Partial<Data>,
    ): Promise<boolean> {
        try {
            const updated = await UpdateEntityFields(
                this.key,
                values,
                this.getEntityType(),
            );

            if (updated) {
                Object.assign(
                    this.dataMap,
                    values,
                );
            }

            return updated;
        } catch (error) {
            console.error(
                `Failed to update fields of ${this.getEntityType()}`,
                error,
            );

            return false;
        }
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
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // ASSETS
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * Builds the content endpoint for one asset belonging to this entity.
     */
    public getAssetEndpoint(
        assetType: EntityAssetType,
    ): URL {
        return getEntityAssetPath(
            this.getEntityType(),
            this.key,
            assetType,
        );
    }

    /**
     * Builds the metadata endpoint for one asset belonging to this entity.
     */
    public getAssetMetadataEndpoint(
        assetType: EntityAssetType,
    ): URL {
        return getEntityAssetMetadataPath(
            this.getEntityType(),
            this.key,
            assetType,
        );
    }

    /**
     * Retrieves an asset from the backend.
     *
     * @return the asset Blob, or null when the asset does not exist
     */
    public async getAsset(
        assetType: EntityAssetType,
    ): Promise<Blob | null> {
        return fetchEntityAsset(
            this.key,
            this.getEntityType(),
            assetType,
        );
    }

    /**
     * Retrieves metadata for an asset.
     *
     * @return metadata, or null when the asset does not exist
     */
    public async getAssetMetadata(
        assetType: EntityAssetType,
    ): Promise<StoredAssetDTO | null> {
        return fetchEntityAssetMetadata(
            this.key,
            this.getEntityType(),
            assetType,
        );
    }

    /**
     * Uploads or replaces an asset.
     *
     * @param assetType asset wire value accepted by the backend
     * @param file uploaded file or blob
     * @param replace when false, the backend returns HTTP 409 if the asset
     * already exists
     */
    public async postAsset(
        assetType: EntityAssetType,
        file: File | Blob,
        replace = true,
    ): Promise<StoredAssetDTO> {
        return postEntityAsset(
            this.key,
            this.getEntityType(),
            assetType,
            file,
            replace,
        );
    }

    /**
     * Deletes an asset.
     *
     * The backend operation is idempotent and returns HTTP 204 whether or not
     * the asset previously existed.
     */
    public async deleteAsset(
        assetType: EntityAssetType,
    ): Promise<void> {
        await deleteEntityAsset(
            this.key,
            this.getEntityType(),
            assetType,
        );
    }
}

const ENTITY_SUFFIX = "entity";
const QUERY_SUFFIX = "query";

export function getEntityController(
    objectType: EntityTypes,
): URL {
    return new URL(
        `${objectType}`,
        API_BASE,
    );
}

function getQueryPath(
    objectType: EntityTypes,
): URL {
    return new URL(
        `${objectType}/${QUERY_SUFFIX}`,
        API_BASE,
    );
}

function getPathWithIDParams<Key extends KeyRecord>(
    objectType: EntityTypes,
    key: Partial<Key> | null,
): URL {
    const url = new URL(
        `${objectType}/${ENTITY_SUFFIX}`,
        API_BASE,
    );

    if (key !== null) {
        appendIDParams(url, key);
    }

    return url;
}

/**
 * @param url mutates, will append params
 * @param key to append
 */
export function appendIDParams<Key extends KeyRecord>(url:URL, key: Readonly<Partial<Key>>): void {
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
    return response.ok;
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
export async function UpdateEntityFields<
    Data extends DataRecord,
    Key extends KeyRecord,
>(
    key: Key,
    values: Partial<Data>,
    objectType: EntityTypes,
): Promise<boolean> {
    const response = await fetchApi(
        getPathWithIDParams(
            objectType,
            key,
        ).toString(),
        {
            method: "PATCH",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify(values),
        },
    );

    return response.ok;
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

    return response.ok;
}


// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// ASSETS
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

const ASSET_ROUTE = "asset";
const ASSET_METADATA_ROUTE = "asset-metadata";

export type EntityAssetType =
    | "avatar"
    | "background";

export type StoredAssetDTO = Readonly<{
    type: string;
    size: number;
    lastModified: string;
}>;

export class EntityAssetRequestError extends Error {
    public constructor(
        public readonly status: number,
        message: string,
    ) {
        super(message);
        this.name = "EntityAssetRequestError";
    }
}

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// ASSET ENDPOINTS
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

function getEntityAssetPath<Key extends KeyRecord>(
    objectType: EntityTypes,
    key: Readonly<Partial<Key>>,
    assetType: EntityAssetType,
): URL {
    return getEntityAssetEndpoint(
        objectType,
        key,
        ASSET_ROUTE,
        assetType,
    );
}

function getEntityAssetMetadataPath<Key extends KeyRecord>(
    objectType: EntityTypes,
    key: Readonly<Partial<Key>>,
    assetType: EntityAssetType,
): URL {
    return getEntityAssetEndpoint(
        objectType,
        key,
        ASSET_METADATA_ROUTE,
        assetType,
    );
}

function getEntityAssetEndpoint<Key extends KeyRecord>(
    objectType: EntityTypes,
    key: Readonly<Partial<Key>>,
    route: typeof ASSET_ROUTE | typeof ASSET_METADATA_ROUTE,
    assetType: EntityAssetType,
): URL {
    const url = new URL(
        `${objectType}/${route}/${encodeURIComponent(assetType)}`,
        API_BASE,
    );

    appendIDParams(
        url,
        key,
    );

    return url;
}

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// GET ASSET
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

/**
 * Maps to:
 *
 * GET /{entityType}/asset/{assetType}
 */
export async function fetchEntityAsset<
    Key extends KeyRecord,
>(
    key: Readonly<Key>,
    objectType: EntityTypes,
    assetType: EntityAssetType,
): Promise<Blob | null> {
    const response = await fetchApi(
        getEntityAssetPath(
            objectType,
            key,
            assetType,
        ).toString(),
        {
            method: "GET",
            headers: {
                Accept: "image/webp,image/*;q=0.9,*/*;q=0.1",
            },
        },
    );

    if (response.status === 404) {
        return null;
    }

    requireSuccessfulAssetResponse(
        response,
        `Could not retrieve ${assetType} asset`,
    );

    return response.blob();
}

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// GET METADATA
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

/**
 * Maps to:
 *
 * GET /{entityType}/asset-metadata/{assetType}
 */
export async function fetchEntityAssetMetadata<
    Key extends KeyRecord,
>(
    key: Readonly<Key>,
    objectType: EntityTypes,
    assetType: EntityAssetType,
): Promise<StoredAssetDTO | null> {
    const response = await fetchApi(
        getEntityAssetMetadataPath(
            objectType,
            key,
            assetType,
        ).toString(),
        {
            method: "GET",
            headers: {
                Accept: "application/json",
            },
        },
    );

    if (response.status === 404) {
        return null;
    }

    requireSuccessfulAssetResponse(
        response,
        `Could not retrieve ${assetType} asset metadata`,
    );

    return response.json() as Promise<StoredAssetDTO>;
}

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// POST ASSET
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

/**
 * Maps to:
 *
 * POST /{entityType}/asset/{assetType}?replace={boolean}
 *
 * Content-Type must not be assigned manually because the browser must generate
 * the multipart boundary.
 */
export async function postEntityAsset<
    Key extends KeyRecord,
>(
    key: Readonly<Key>,
    objectType: EntityTypes,
    assetType: EntityAssetType,
    file: File | Blob,
    replace = true,
): Promise<StoredAssetDTO> {
    const url = getEntityAssetPath(
        objectType,
        key,
        assetType,
    );

    url.searchParams.set(
        "replace",
        String(replace),
    );

    const formData = new FormData();

    formData.append(
        "file",
        file,
        getAssetUploadName(
            assetType,
            file,
        ),
    );
    console.log(
        `[AssetUpdate] ${objectType} ${assetType} ${key}`
    )
    const response = await fetchApi(
        url.toString(),
        {
            method: "POST",
            body: formData,
        },
    );

    if (response.status === 409) {
        throw new EntityAssetRequestError(
            response.status,
            `${assetType} asset already exists`,
        );
    }

    requireSuccessfulAssetResponse(
        response,
        `Could not upload ${assetType} asset`,
    );

    return response.json() as Promise<StoredAssetDTO>;
}

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// DELETE ASSET
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

/**
 * Maps to:
 *
 * DELETE /{entityType}/asset/{assetType}
 */
export async function deleteEntityAsset<
    Key extends KeyRecord,
>(
    key: Readonly<Key>,
    objectType: EntityTypes,
    assetType: EntityAssetType,
): Promise<void> {
    const response = await fetchApi(
        getEntityAssetPath(
            objectType,
            key,
            assetType,
        ).toString(),
        {
            method: "DELETE",
        },
    );

    requireSuccessfulAssetResponse(
        response,
        `Could not delete ${assetType} asset`,
    );
}

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// ASSET UTILS
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

function requireSuccessfulAssetResponse(
    response: Response,
    message: string,
): void {
    if (response.ok) {
        return;
    }

    throw new EntityAssetRequestError(
        response.status,
        message,
    );
}

function getAssetUploadName(
    assetType: EntityAssetType,
    file: File | Blob,
): string {
    if (
        file instanceof File
        && file.name.trim().length > 0
    ) {
        return file.name;
    }

    return `${assetType}.webp`;
}