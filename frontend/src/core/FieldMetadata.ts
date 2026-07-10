/* Enums here must match the backend's ones. Search the same class(matching name) somewhere under utils.format.fields */

export enum FieldTypes {
    STRING = "STRING",
    NUMBER = "NUMBER",
    FLOAT = "FLOAT",
    BOOLEAN = "BOOLEAN",
    ENUM = "ENUM",
}
export enum FormatTypes {
    SHORT_TEXT = "SHORT_TEXT",
    PLAIN = "PLAIN",
    LONG_TEXT = "LONG_TEXT",
    MARKDOWN = "MARKDOWN"
}
export enum PlaceHolder {
    USER = "{{user}}",
    CHARACTER = "{{char}}",
}

export interface Constraints {
    min: number;
    max: number;
    read_only: boolean;
    allowed_placeholders: PlaceHolder[];
}

export interface FieldPresentation {
    type: FieldTypes;
    format: FormatTypes;
    placeholder: string; //This just plain text for when the field is empty and shiet.
}

export interface DB_Metadata {
    is_key: boolean;
    needed_for_instance: boolean;
}

export interface FieldInfo{
    presentation: FieldPresentation | null;
    constraints : Constraints | null;
    db_metadata : DB_Metadata | null;
}