import {DataRecord, KeyRecord} from "@/entities/EntityABS";
import {KeysOfType} from "@/components/utils/entity_editors/field";

export type StringField<Keys extends KeyRecord, Data extends DataRecord> = KeysOfType<Keys, string> | KeysOfType<Data, string>
