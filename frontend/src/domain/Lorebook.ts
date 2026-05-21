import {
    ABSEntity,
    createEntity,
    deleteEntity,
    EntityField,
    fetch_all,
    fetchApi,
    fetchOne
} from "@/frameworks/ABSEntity";
import {EntityTypes} from "@/domain/EntityTypes";
import {CommonFields} from "@/utils/CommonFields";
import {API_BASE} from "@/config";
import {DTO, Primitives} from "@/types/DTOs";

export type LorebookKey = { id: number }
export type LorebookData = { name: string }

export class Lorebook extends ABSEntity<LorebookKey, LorebookData> {
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

type EntryKey = { lorebook_id: number, entry_id: number }
export type EntryData = {
    name: string | null;
    enabled: boolean;
    content: string | null;

    // Injection requirements
    probability: number;
    outlet_id: number | null;
    delay: number;
    cooldown: number;
    stick_through: number;

    // Injection options
    injection_order: number;

    // Activation strategy
    strategy: ActivationStrategy | number;
    embed_text: string | null;
    prevent_further_recursion: boolean;
    non_recursable: boolean;
    delay_until_recursion: boolean;
    scan_depth: number | null;
};

export class Entry extends ABSEntity<EntryKey, EntryData> {
    private keywords: KeyWord[] | null = null;
    private outlet:string | null = null;

    getEntityType(): EntityTypes {
        return EntityTypes.ENTRY;
    }

    getIterationArr(): EntityField<EntryKey, EntryData>[] { //This isn't as important, the entry editor must be special
        return [CommonFields.NAME];
    }

    async update<F extends keyof EntryData>(field: F, value: EntryData[F]): Promise<boolean> {
        if (field == "outlet_id")
            throw new Error("Trying to update outlet via normal update API")
        return super.update(field, value);
    }

    public override get<K extends keyof EntryData>(key: K): EntryData[K];
    public override get<K extends keyof EntryKey>(key: K): EntryKey[K];
    public override get(key: keyof EntryData | keyof EntryKey): Primitives {
        if (key === "outlet_id") {
            throw new Error("Use getOutletName() instead of get('outlet_id')");
        }

        return super.get(key as keyof EntryData);
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
        return dtos.map(dto => new Entry(dto, EntityTypes.ENTRY));
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
                lorebook_id: this.get('lorebook_id')!,
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
                entry_id: this.get('entry_id'),
                keyword_id: keyword.get('id')
            },
            EntityTypes.ENTRY_KEYWORD
        )
    }

    public async getOutletName() : Promise<string | null>{
        if (this.outlet != null) return this.outlet;
        if (super.get('outlet_id') == null) {
            this.outlet = null;
            console.debug(`This has no outlet`)
            return this.outlet;
        }
        if (this.outlet == null)
            await fetchOne<OutletKey, OutletData, Outlet>(
                {
                    id:super.get('outlet_id')!
                },
                EntityTypes.OUTLETS,
                Outlet
            ).then( x=> {
                console.debug(`${x}`)
                this.outlet = x.get('name')
            })
        return this.outlet;
    }

    public async updateOutlet(outlet:string) : Promise<void>{
        console.debug(`Updating outlet for ${this.get('name')} with new value ${outlet}`)
        const result = await fetchApi(
            `${API_BASE}/${EntityTypes.ENTRY}/entity/${this.get('lorebook_id')}/${super.get('entry_id')}`,
            {
                method: "PATCH",
                body: outlet,
            })

        if (await result.json() as boolean) this.outlet = outlet;
    }
    public async clearOutlet(): Promise<void> {
        await super.update('outlet_id', null)
    }
}

type KeywordKey = { id: number };
export type KeywordData = { name: string };

export class KeyWord extends ABSEntity<KeywordKey, KeywordData> {
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

type EntryKeywordsKey = {lorebook_id:number, entry_id: number, keyword_id: number };
class EntryKeywords extends ABSEntity<EntryKeywordsKey, any>{
    getEntityType(): EntityTypes {
        return EntityTypes.ENTRY_KEYWORD;
    }

    getIterationArr(): EntityField<EntryKeywordsKey, any>[] {
        return [];
    }
}

export type OutletKey = {id:number};
export type OutletData = {name: string};
export class Outlet extends ABSEntity<OutletKey, OutletData> {
    getEntityType(): EntityTypes {
        return EntityTypes.OUTLETS
    }

    public static async outlets(): Promise<string[]>{
        return await fetch_all<OutletKey,OutletData,Outlet>(EntityTypes.OUTLETS, Outlet)
            .then(result => result.map(outlet => outlet.get('name')))
    }
}