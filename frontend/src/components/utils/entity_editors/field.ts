import {Component} from "vue";
import NumberInput from "@/components/utils/entity_editors/fields/numeric/NumberInput.vue";
import BooleanToggle from "@/components/utils/entity_editors/fields/bool/BooleanToggle.vue";
import {EntityABS} from "@/entities/EntityABS";
import {DataRecord, EntityField, EntityFieldValue, KeyRecord, Primitives} from "@/types/DTOs/DTOs";
import {FieldInfo, FieldTypes, FormatTypes} from "@/types/FieldMetadata";
import SearchBar from "@/components/utils/SearchBar.vue";
import LongTextBox from "@/components/utils/entity_editors/fields/textboxes/LongTextBox.vue";
import ShortTextBox from "@/components/utils/entity_editors/fields/textboxes/ShortTextBox.vue";

export function getEditorForField<
    Data extends DataRecord,
    Field extends keyof DataRecord,
    Ent extends EntityABS<any, Data>
>(entity:Ent, field:Field): Component | null {
    const fieldInfo:FieldInfo = entity.getFieldInfoOf(field);
    if (fieldInfo.presentation == null) return null; //Means this field shouldn't be shown to user

    switch(fieldInfo.presentation.type){
        case FieldTypes.STRING:
            return handleStringField(fieldInfo.presentation.format)
        case FieldTypes.NUMBER:
            return NumberInput
        case FieldTypes.FLOAT:
            throw new Error("Not yet implemented")
        case FieldTypes.ENUM:
            return SearchBar;
        case FieldTypes.BOOLEAN:
            return BooleanToggle;
    }
}


function handleStringField(format:FormatTypes): Component {
    if (format == null) throw new Error("UNKNOWN STRING FIELD");
    switch (format) {
        case FormatTypes.LONG_TEXT: return LongTextBox;
        case FormatTypes.SHORT_TEXT: return ShortTextBox;
        default: throw new Error(`INVALID FORMAT FOR STRING FIELD ${format}`);
    }
}

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// Interfaces
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
export interface FieldEditor <
    Keys extends KeyRecord,
    Data extends DataRecord,
    Field extends EntityField<Keys,Data>,
    Entity extends EntityABS<Keys,Data>
> {
    fieldName:Field;
    entity: Entity;
}
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// Types
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
export type KeysOfType<T, V> = {
    [K in keyof T]-?: T[K] extends V ? K : never
}[keyof T];

