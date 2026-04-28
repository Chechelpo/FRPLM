import {EntityABS, EntityField} from "@/frameworks/entities/EntityABS";
import {EntityTypes} from "@/domain/entities/EntityTypes";
import {CommonFields} from "@/utils/CommonFields";

export type LorebookKey = {id:number}
export type LorebookData = {name:string}

export class Lorebook extends EntityABS<LorebookKey, LorebookData>{
    getEntityType(): EntityTypes {
        return EntityTypes.LOREBOOKS;
    }

    getIterationArr(): EntityField<LorebookKey, LorebookData>[] {
        return [CommonFields.NAME];
    }

}