import {EntityABS, EntityField} from "@/frameworks/entities/EntityABS";
import {EntityTypes} from "@/domain/entities/EntityTypes";
import {fetch_all} from "@/utils/EntityFetch";
import {CommonFields} from "@/utils/CommonFields";

export type WorldKey = {id:number}
export type WorldData = {
    name:string,
    lorebook_id:number,
}
export class World extends EntityABS<WorldKey, WorldData> {
    getEntityType(): EntityTypes {
        return EntityTypes.WORLDS;
    }

    getIterationArr(): EntityField<WorldKey, WorldData>[] {
        return [CommonFields.NAME];
    }

}