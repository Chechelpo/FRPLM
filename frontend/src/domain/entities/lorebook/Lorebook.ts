import {EntityABS, EntityField} from "@/frameworks/entities/EntityABS";
import {ControllerType} from "@/config/ControllerType";
import {CommonFields} from "@/utils/CommonFields";

export type LorebookKey = {id:number}
export type LorebookData = {name:string}

export class Lorebook extends EntityABS<LorebookKey, LorebookData>{
    getEntityType(): ControllerType {
        return ControllerType.LOREBOOKS;
    }

    getIterationArr(): EntityField<LorebookKey, LorebookData>[] {
        return [CommonFields.NAME];
    }

}