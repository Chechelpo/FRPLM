import {EntityABS, EntityField} from "@/frameworks/entities/EntityABS";
import {EntityTypes} from "@/frameworks/entities/EntityTypes";
import {CommonFields} from "@/utils/CommonFields";
import {CharacterKey} from "@/domain/entities/chars/Characters";

export type LorebookKey = {id:number}
export type LorebookData = {name:string}

export class Lorebook extends EntityABS<LorebookKey, LorebookData>{
    getEntityType(): EntityTypes {
        return EntityTypes.LOREBOOKS;
    }

    getIterationArr(): EntityField<LorebookKey, LorebookData>[] {
        return [CommonFields.NAME];
    }
    /*
    public static async ofCharacter(key:CharacterKey):Promise<Lorebook{
        return null;
    }
    */
}