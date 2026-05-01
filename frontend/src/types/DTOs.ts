//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// Character DTOs
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
import type {BackendObject, NamedObject} from "@/frameworks/interfaces";
import {EntityTypes} from "@/frameworks/entities/EntityTypes";
import {CommonFields} from "@/utils/CommonFields";

export type Primitives = string | number | boolean | null;
export type KeyRecord = Record<string, Primitives>;
export type DataRecord = Record<string, Primitives>;

// Union of entity records
export type EntityField<keys extends KeyRecord, data extends DataRecord> =  keyof keys | keyof data;
export type EntityFieldValue<keys extends KeyRecord, data extends DataRecord, K extends EntityField<keys,data>> =
        K extends keyof keys ? keys[K] :
        K extends keyof data ? data[K] :
        never;
export type EntityPackage<key extends KeyRecord, data extends DataRecord> = {
    [K in EntityField<key, data>]?: EntityFieldValue<key, data, EntityField<key, data>>
}
/** MUST MATCH BACKEND'S ENTITY_DTO */
export interface DTO {
    type: EntityTypes;
    key: Record<string, Primitives>;
    payload: Record<string, Primitives>;
}

/**
 * @abstract
 * Base shape for all characters.
 */
export interface Character_DTO extends NamedObject {
    physDescription: string;
    miscDescription: string;

    agility: Number;
    sight: Number;
    hearing: Number;
}

//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// Connection DTOs
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
export interface LLM_connection{
    id: number;
    name: string;
    url: string;
    type: string;
}