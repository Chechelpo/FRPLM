import {EntityABS, EntityField} from "@/entities/EntityABS";
import {ControllerType} from "@/config/ControllerType";
import {fetch_all} from "@/utils/EntityFetch";
import {CommonFields} from "@/entities/CommonFields";

export type WorldKey = {id:number}
export type WorldData = {
    name:string,
    lorebook_id:number,
}
export class World extends EntityABS<WorldKey, WorldData> {
    getEntityType(): ControllerType {
        return ControllerType.WORLDS;
    }

    getIterationArr(): EntityField<WorldKey, WorldData>[] {
        return [CommonFields.NAME];
    }

}