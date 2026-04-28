import {EntityABS} from "@/frameworks/entities/EntityABS";
import {EntityTypes} from "@/domain/entities/EntityTypes";
import {WorldKey} from "@/domain/entities/space/World";
import {QueryEntities} from "@/utils/EntityFetch";
import {QueryActionType} from "@/frameworks/entities/queries";
import {EntityField} from "@/types/DTOs";
import {CommonFields} from "@/utils/CommonFields";

export type LocationKey = {worldID?: number, id?: number}
export type LocationData = {
    name: string,
    description: string,
    lorebook_id: number,
}
export class Location extends EntityABS<LocationKey,LocationData> {
    getEntityType(): EntityTypes {
        return EntityTypes.LOCATIONS;
    }

    getIterationArr(): EntityField<LocationKey, LocationData>[] {
        return [CommonFields.NAME, CommonFields.DESCRIPTION];
    }

    public static async getLocationsOfWorld(key:WorldKey): Promise<Location[]> {
        return QueryEntities([
            {
                fieldName:'worldID',
                action:QueryActionType.EQUALS,
                value:key.id
            }],
            EntityTypes.LOCATIONS,
            this
        )
    }
}