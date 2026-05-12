//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// Character DTOs
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
import {EntityTypes} from "@/domain/EntityTypes";

export type Primitives = string | number | boolean | null;
export type KeyRecord = Record<string, Primitives>;
export type DataRecord = Record<string, Primitives>;

// Union of entity records
export type EntityField<keys extends KeyRecord, data extends DataRecord> =  keyof keys | keyof data;
export type EntityFieldValue<keys extends KeyRecord, data extends DataRecord, K extends EntityField<keys,data>> =
        K extends keyof keys ? keys[K] :
        K extends keyof data ? data[K] :
        never;
/** MUST MATCH BACKEND'S ENTITY_DTO */
export interface DTO {
    type: EntityTypes;
    key: Record<string, Primitives>;
    payload: Record<string, Primitives>;
}