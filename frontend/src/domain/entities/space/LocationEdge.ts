import {EntityABS} from "@/frameworks/entities/EntityABS";
import {ControllerType} from "@/config/ControllerType";
import {LocationKey} from "@/domain/entities/space/Location";
import {QueryEntities} from "@/utils/EntityFetch";
import {QueryActionType} from "@/frameworks/entities/queries";
import {CustomSet} from "@/utils/CustomSet";
import {set} from "@vueuse/core";
import {CommonFields} from "@/utils/CommonFields";
import {EntityField} from "@/types/DTOs";

export type EdgeKey = {world_id:number, location1_id:number, location2_id:number}
export type EdgeData = {
    description: string | null,
    travel_cost: number
}

export class LocationEdge extends EntityABS<EdgeKey, EdgeData>{
    getEntityType(): ControllerType {
        return ControllerType.EDGES;
    }

    getIterationArr(): EntityField<EdgeKey, EdgeData>[] {
        return [CommonFields.DESCRIPTION];
    }
}