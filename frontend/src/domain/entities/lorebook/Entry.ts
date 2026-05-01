import {EntityABS, EntityField} from "@/frameworks/entities/EntityABS";
import {EntityTypes} from "@/frameworks/entities/EntityTypes";
import {CommonFields} from "@/utils/CommonFields";
import {fetchApi} from "@/domain/entities/EntityFetch";
import {API_BASE} from "@/config";
import {DTO} from "@/types/DTOs";
import {Lorebook} from "@/domain/entities/lorebook/Lorebook";

export enum ActivationStrategy {
    /** Always active per message (this doesn't mean it will appear, It's still bound by probabilities) */
    CONSTANT = 0,
    /** Keyword activation */
    COMMON = 1,
    /** Common keyword activation and embedding vector matching */
    EMBEDDING = 2,
}

export type EntryKey = {
    lorebook_id:number,
    entry_id?: number // If its initialized its always there
}
export type EntryData = {
    name: string | null;
    content: string | null;

    // Injection requirements
    probability: number | null;
    outlet: string | null;
    delay: number;
    cooldown: number;
    stick_through: number;

    // Injection options
    injection_order: number | null;

    // Activation strategy
    strategy: ActivationStrategy | number | null;
    embed_text: string | null;
    prevent_further_recursion: boolean;
    non_recursable: boolean;
    delay_until_recursion: boolean;
    scan_depth: number | null;
};

export class Entry extends EntityABS<EntryKey, EntryData>{
    getEntityType(): EntityTypes {
        return EntityTypes.ENTRY;
    }


    getIterationArr(): EntityField<EntryKey, EntryData>[] { //This isn't as important, the entry editor must be special
        return [CommonFields.NAME];
    }
    //Workaround until query works
    static async ofLorebook(lorebook:Lorebook | number): Promise<Entry[]> {
        let lorebookID:number;
        if (typeof lorebook === "number") {
            lorebookID = lorebook;
        }else
            lorebookID = lorebook.get('id')

        console.info(`Fetching entries of lorebook with id ${lorebookID}`);
        const response = await fetchApi(
            `${API_BASE}/${EntityTypes.ENTRY}/entity/${lorebookID}`,
            {
                method:"GET",
            }
        )
        const dtos = await response.json() as DTO[];
        const entries: Entry[] = dtos.map(dto => new Entry(dto, EntityTypes.ENTRY));
        return entries;
    }
}