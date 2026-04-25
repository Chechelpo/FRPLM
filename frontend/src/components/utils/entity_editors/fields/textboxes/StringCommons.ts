import {DataRecord, KeyRecord} from "@/frameworks/entities/EntityABS";
import {KeysOfType} from "@/components/utils/entity_editors/field";

export type StringField<Keys extends KeyRecord, Data extends DataRecord> = KeysOfType<Keys, string> | KeysOfType<Data, string>
