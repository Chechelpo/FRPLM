import {EntityABS, EntityField} from "@/frameworks/entities/EntityABS";
import {ControllerType} from "@/config/ControllerType";
import {CommonFields} from "@/utils/CommonFields";

export enum ActivationStrategy {
    /** Always active per message (this doesn't mean it will appear, It's still bound by probabilities) */
    CONSTANT = 0,
    /** Keyword activation */
    COMMON = 1,
    /** Common keyword activation and embedding vector matching */
    EMBEDDING = 2,
}

export type EntryKey = {lorebook_id:number, entry_id: number}
export type EntryData = {
    name: string,
    content:string,
    probability: number,

    strategy: number,
    embedding_text: string | null,

    outlet_id: number;
    scan_depth: number
}

export class Entry extends EntityABS<EntryKey, EntryData>{
    getEntityType(): ControllerType {
        return ControllerType.ENTRY;
    }


    getIterationArr(): EntityField<EntryKey, EntryData>[] { //This isn't as important, the entry editor must be special
        return [CommonFields.NAME];
    }

}