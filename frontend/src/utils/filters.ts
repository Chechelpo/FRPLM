import {EntityABS} from "@/frameworks/entities/EntityABS";
import {DataRecord, KeyRecord} from "@/types/DTOs";

export function filterWithAttribute<
    Data extends DataRecord,
    Field extends Extract<keyof Data, string>,
    T extends EntityABS<any, Data> // Silence that motherfucker >:(
>(
        attrName:Field,
        attrValue:Data[Field],
        entities:T[]
    ): T[]
    {
        if (entities.length === 0) return [];
        if (!entities[0].hasAttribute(attrName))
            throw new Error(`Entities of type ${entities[0].getEntityType()} don't have attribute name ${attrName}`);

        return entities.filter(entity =>
            entity.get(attrName) === attrValue
        );
    }