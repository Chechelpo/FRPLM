import {EntityABS} from "@/entities/EntityABS";
import {ControllerType} from "@/config/ControllerType";
import {WorldKey} from "@/entities/space/World";
import {QueryEntities} from "@/utils/EntityFetch";
import {QueryActionType} from "@/types/queries";
import {EntityField} from "@/types/DTOs/DTOs";
import {CommonFields} from "@/entities/CommonFields";

export type LocationKey = {worldID?: number, id?: number}
export type LocationData = {
    name: string,
    description: string,
    lorebook_id: number,
}
export class Location extends EntityABS<LocationKey,LocationData> {
    getEntityType(): ControllerType {
        return ControllerType.LOCATIONS;
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
            ControllerType.LOCATIONS,
            this
        )
    }
}