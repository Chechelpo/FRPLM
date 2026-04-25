import {DataRecord, EntityField, EntityFieldValue, KeyRecord} from "@/types/DTOs/DTOs";

export interface QueryAction<Keys extends KeyRecord, Data extends DataRecord>{
    fieldName: EntityField<Keys,Data>,
    action: QueryActionType,
    value: EntityFieldValue<Keys,Data, any>,
};

export enum QueryActionType {
    EQUALS = "EQUALS",
    LIKE = "LIKE",
    GREATER_THAN = "GREATER_THAN",
    LESS_THAN = "LESS_THAN",
}