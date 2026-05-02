import {EntityABS, EntityField} from "@/frameworks/entities/EntityABS";
import {EntityTypes} from "@/frameworks/entities/EntityTypes";
import {CommonFields} from "@/utils/CommonFields";
import {createEntity, deleteEntity, fetch_all, fetchApi} from "@/domain/entities/EntityFetch";
import {API_BASE} from "@/config";
import {DTO} from "@/types/DTOs";

export type LorebookKey = { id: number }
export type LorebookData = { name: string }

export class Lorebook extends EntityABS<LorebookKey, LorebookData> {
    private entries: Entry[] | null = null;

    getEntityType(): EntityTypes {
        return EntityTypes.LOREBOOKS;
    }

    getIterationArr(): EntityField<LorebookKey, LorebookData>[] {
        return [CommonFields.NAME];
    }

    public async getEntries(): Promise<Entry[]> {
        if (this.entries == null)
            console.debug(`Fetching entries for lorebook ${this}`)
        this.entries = await Entry.ofLorebook(this);

        return this.entries;
    }

    public async newEntry(): Promise<Entry> {
        if (this.entries != null)
            await this.getEntries();

        const newEntry = await createEntity<EntryKey, EntryData, Entry>(
            {
                lorebook_id: this.get('id')
            },
            null,
            EntityTypes.ENTRY,
            Entry
        )
        console.info(`Created new entry ${newEntry} \n for lorebook ${this}`)
        this.entries!.push(newEntry);

        return newEntry;
    }

    public async deleteEntry(entry: Entry): Promise<boolean> {
        const success: boolean = await deleteEntity<EntryKey>(entry.key, EntityTypes.ENTRY)
        if (this.entries == null)
            await this.getEntries();
        this.entries!.filter(e => entry.key != e.key)

        return success;
    }
}

export enum ActivationStrategy {
    /** Always active per message (this doesn't mean it will appear, It's still bound by probabilities) */
    CONSTANT = 0,
    /** Keyword/Regex activation */
    COMMON = 1,
    /** Common keyword activation and embedding vector matching */
    EMBEDDING = 2,
}

type EntryKey = {
    lorebook_id: number,
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
    strategy: ActivationStrategy | number;
    embed_text: string | null;
    prevent_further_recursion: boolean;
    non_recursable: boolean;
    delay_until_recursion: boolean;
    scan_depth: number | null;
};

export class Entry extends EntityABS<EntryKey, EntryData> {
    private keywords: KeyWord[] | null = null;

    getEntityType(): EntityTypes {
        return EntityTypes.ENTRY;
    }


    getIterationArr(): EntityField<EntryKey, EntryData>[] { //This isn't as important, the entry editor must be special
        return [CommonFields.NAME];
    }

    //Workaround until a query works
    static async ofLorebook(lorebook: Lorebook | number): Promise<Entry[]> {
        let lorebookID: number;
        if (typeof lorebook === "number") {
            lorebookID = lorebook;
        } else
            lorebookID = lorebook.get('id')

        console.info(`Fetching entries of lorebook with id ${lorebookID}`);
        const response = await fetchApi(
            `${API_BASE}/${EntityTypes.ENTRY}/entity/${lorebookID}`,
            {
                method: "GET",
            }
        )
        const dtos = await response.json() as DTO[];
        const entries: Entry[] = dtos.map(dto => new Entry(dto, EntityTypes.ENTRY));
        return entries;
    }

    public async getKeywords(): Promise<KeyWord[]> {
        if (this.keywords == null)
            this.keywords = await KeyWord.getOfEntry(this);
        return this.keywords;
    }

    async addKeyword(keyword: KeyWord): Promise<void> {
        if (this.keywords == null)
            await this.getKeywords()
        if (this.keywords!.some(key => key.key == keyword.key))
            return
        const newEntry = await createEntity<EntryKeywordsKey, any, EntryKeywords>(
            {
                lorebook_id: this.get('lorebook_id'),
                entry_id: this.get('entry_id')!, // Its initialized, so it's guaranteed to be defined
                keyword_id: keyword.get('id')
            },
            null,
            EntityTypes.ENTRY_KEYWORD,
            EntryKeywords
        )

        this.keywords!.push(keyword);
    }

    async removeKeyword(keyword: KeyWord): Promise<void> {
        if (this.keywords == null)
            await this.getKeywords()
        if (!this.keywords!.some(key => key.key == keyword.key))
            return;

        const response = await deleteEntity<EntryKeywordsKey>(
            {
                lorebook_id: this.get('lorebook_id'),
                entry_id: this.get('entry_id')!,
                keyword_id: keyword.get('id')
            },
            EntityTypes.ENTRY_KEYWORD
        )
    }
}

type KeywordKey = { id: number };
export type KeywordData = { name: string };

export class KeyWord extends EntityABS<KeywordKey, KeywordData> {
    public static async getAll(): Promise<KeyWord[]> {
        return fetch_all<KeywordKey, KeywordData, KeyWord>(EntityTypes.KEYWORD, this)
    }

    getEntityType(): EntityTypes {
        return EntityTypes.KEYWORD;
    }

    getIterationArr(): EntityField<KeywordKey, KeywordData>[] {
        return [];
    }

    static async getOfEntry(entry: Entry): Promise<KeyWord[]> {
        const response = await fetchApi(
            `${API_BASE}/${EntityTypes.KEYWORD}/entry/${entry.get('lorebook_id')}/${entry.get('entry_id')}`,
            {
                method: 'GET'
            }
        )

        const dtos = await response.json() as DTO[];
        return dtos.map(dto => new KeyWord(dto, EntityTypes.KEYWORD));
    }
}
type EntryKeywordsKey = { lorebook_id:number, entry_id: number, keyword_id: number };
class EntryKeywords extends EntityABS<EntryKeywordsKey, any>{
    getEntityType(): EntityTypes {
        return EntityTypes.ENTRY_KEYWORD;
    }

    getIterationArr(): EntityField<EntryKeywordsKey, any>[] {
        return [];
    }
}