import { API_BASE } from "@/config";
import { FieldInfo } from "@/frameworks/entities/FieldMetadata";
import {fetchApi, getEntityController, UpdateEntityField} from "@/domain/entities/EntityFetch";
import {DTO} from "@/types/DTOs";
import {CommonFields} from "@/utils/CommonFields";
import {EntityTypes} from "@/frameworks/entities/EntityTypes";
import {Equatable, ValueComparable} from "@/types/Equatable";

/** JSON-safe primitives typically received from backend SQL row payloads */
export type Primitives = string | number | boolean | null;

/** See backend's fields for declaring correct ones */
export type KeyRecord = Record<string, Primitives>;
/** See backend's fields for declaring correct ones */
export type DataRecord = Record<string, Primitives>;
/** Combination Key-Data fields of an entity type */
export type EntityField<keys extends KeyRecord, data extends DataRecord> =  keyof keys | keyof data;
export type EntityFieldsMap<keys extends KeyRecord, data extends DataRecord> = Map<EntityField<keys,data>, FieldInfo>

export abstract class EntityABS<Key extends KeyRecord, Data extends DataRecord> implements ValueComparable {
    private static registered:Set<EntityTypes> = new Set;
    protected static fields:Map<EntityTypes, EntityFieldsMap<any,any>> = new Map;

    public static registerType(type:EntityTypes): void {
        if (this.registered.has(type)) throw new Error(`Duplicate entity handler ${type}`)
        this.registered.add(type)
    }
    public static getFields(type:EntityTypes) : Map<string, FieldInfo> {
        const fields = this.fields.get(type);

        if (!fields) {
            throw new Error(`No fields for type ${type}`);
        }

        return fields as Map<string, FieldInfo>;
    }

    static async initialise(): Promise<void> {
        if (this.fields.size != 0) return;
        for (const type of Object.values(EntityTypes)) {

            const response = await fetchApi(
                `${getEntityController(type)}/fields`,
                {
                    method: "GET",
                    headers: new Headers({
                        "Content-Type": "application/json"
                    })
                }
            );

            const json = await response.json() as Record<string, FieldInfo>;

            const map = new Map<string, FieldInfo>(Object.entries(json));

            EntityABS.fields.set(type, map);
        }
    }

    /** Key attributes of entity (composite identity) */
    public readonly key: Readonly<Key>;
    /** Other attributes. Must not collide with KeyMap */
    public dataMap: Data;

    public constructor(dto:DTO, expected_type?: EntityTypes) {
        console.log(`Received ${expected_type} with dto: \n ${JSON.stringify(dto)}`)
        if (dto.type !== expected_type)
            throw new Error(`Mismatch in entity type: Response:${dto.type} vs expected:${expected_type}`);

        if (Object.entries(dto.key).length == 0) {
            console.error("Key with no entries")
            throw new Error(`Entity with no keys: ${dto.key}`);
        }
        this.key = dto.key as Key;
        this.dataMap = dto.payload as Data;
    }

    /** Method used for avoiding duplication of entity type string */
    abstract getEntityType(): EntityTypes;
    abstract getIterationArr():EntityField<Key, Data>[];
    public getFields(): EntityFieldsMap<Key, Data> {
        return EntityABS.getFields(this.getEntityType())
    }
    public getFieldInfoOf<Field extends EntityField<Key, Data>>(field:Field):FieldInfo{
        return EntityABS.getFields(this.getEntityType()).get(field as string)!;
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
        if (!(other instanceof EntityABS)) {
            return false;
        }

        if (other.constructor !== this.constructor) {
            return false;
        }

        const casted = other as EntityABS<Key, Data>;
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
