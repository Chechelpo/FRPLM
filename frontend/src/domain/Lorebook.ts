import {
    ABSEntity,
    createEntity,
    deleteEntity,
    EntityField,
    fetch_all,
    fetchFromReference,
    fetchMatching,
    fetchOne,
    getEntityController
} from "@/core/ABSEntity";
import {EntityTypes} from "@/domain/EntityTypes";
import {API_BASE} from "@/config";
import {DTO, Primitives} from "@/types/DTOs";
import {parseNumberKey} from "@/utils/ReferenceCodec";
import {fetchApi} from "@/services/apiClient";

export type LorebookKey = { id: number }
export type LorebookData = { name: string }

export class Lorebook extends ABSEntity<LorebookKey, LorebookData> {
    private entries: Entry[] | null = null;
    private static readonly REFERENCE_KEY_ORDER: readonly (keyof LorebookKey & string)[] = ['id'] as const

    getEntityType(): EntityTypes {
        return EntityTypes.LOREBOOKS;
    }

    protected getReferenceKeyOrder(): readonly (keyof LorebookKey & string)[] {
        return Lorebook.REFERENCE_KEY_ORDER;
    }

    public static async getFromReference(reference:string) : Promise<Lorebook> {
        return await fetchFromReference<LorebookKey, LorebookData, Lorebook>(
            reference, EntityTypes.LOREBOOKS, this.REFERENCE_KEY_ORDER, {id:parseNumberKey}, Lorebook
        )
    }

    public static async getAll() : Promise<Lorebook[]> {
        return await fetch_all<LorebookKey, LorebookData, Lorebook>(EntityTypes.LOREBOOKS, Lorebook);
    }

    public async getEntries(): Promise<Entry[]> {
        if (this.entries == null)
            console.debug(`Fetching entries for lorebook ${this}`)
        this.entries = await Entry.ofLorebook(this);

        return this.entries;
    }

    public async newEntry(name:string): Promise<Entry> {
        if (this.entries != null)
            await this.getEntries();

        const newEntry = await createEntity<EntryKey, EntryData, Entry>(
            {
                lorebook_id: this.get('id')
            },
            {name:name},
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

    public async keywords(): Promise<string[]> {
        return fetchApi(
            `${getEntityController(EntityTypes.ENTRY_KEYWORD)}/${this.get('id')}`,
            {
                method:"GET"
            }
        ).then(async response => await response.json() as string[])
    }
}

export enum ActivationStrategy {
    /** Always active per message (this doesn't mean it will appear, It's still bound by probabilities) */
    CONSTANT = 0,
    /** Keyword/Regex activation */
    COMMON = 1,
    /** Common keyword activation and embedding vector matching */
    //EMBEDDING = 2,
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
    private static readonly REFERENCE_KEY_ORDER: readonly (keyof EntryKey & string)[] = ['lorebook_id', 'entry_id'] as const;
    getEntityType(): EntityTypes {
        return EntityTypes.ENTRY;
    }

    protected getReferenceKeyOrder(): readonly (keyof EntryKey & string)[] {
        return Entry.REFERENCE_KEY_ORDER;
    }

    public static async fromReference(reference:string) : Promise<Entry>{
        return await fetchFromReference<EntryKey, EntryData, Entry>(
            reference, EntityTypes.ENTRY, Entry.REFERENCE_KEY_ORDER, {lorebook_id:parseNumberKey, entry_id:parseNumberKey},
            Entry
        )
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

    static async ofLorebook(lorebook: Lorebook | number): Promise<Entry[]> {
        let lorebookID: number;
        if (typeof lorebook === "number") {
            lorebookID = lorebook;
        } else lorebookID = lorebook.get('id')

        console.info(`Fetching entries of lorebook with id ${lorebookID}`);
        return  await fetchMatching<EntryKey, EntryData, Entry>(
            {
                lorebook_id: lorebookID,
            },
            EntityTypes.ENTRY,
            Entry
        );
    }

    private keywordPath(keyword : string): string {
        return `${getEntityController(EntityTypes.ENTRY_KEYWORD)}/${this.get('lorebook_id')}/${this.get('entry_id')}?name=${keyword}`
    }
    public async keywords(): Promise<string[]> {
        return await fetchApi(
            `${getEntityController(EntityTypes.ENTRY_KEYWORD)}/${this.get('lorebook_id')}/${this.get('entry_id')}`,
            {
                method: "GET",
            }
        ).then(async response => await response.json() as string[])
    }
    async addKeyword(name: string): Promise<boolean> {
        const response = await fetchApi(
            this.keywordPath(name),
            {
                method: "PUT",
            }
        )
        return response.status == 200;
    }
    async removeKeyword(keyword: string): Promise<boolean> {
        const response = await fetchApi(
            this.keywordPath(keyword),
            {
                method: "DELETE",
            })
        return response.status == 200;
    }

    public async getOutletName(): Promise<string | null> {
        if (this.dataMap['outlet_id'] == null) return null;
        return await fetchOne<OutletKey, OutletData, Outlet>(
                {
                    id: super.get('outlet_id')!
                },
                EntityTypes.OUTLETS,
                Outlet
            ).then(x => {
                console.debug(`${x}`)
                return x.get('name')
            })
    }
    public async updateOutlet(outlet: string): Promise<void> {
        console.debug(`Updating outlet for ${this.get('name')} with new value ${outlet}`)
        const result = await fetchApi(
            `${getEntityController(EntityTypes.ENTRY)}/entity/${this.get('lorebook_id')}/${super.get('entry_id')}?outlet=${outlet}`,
            {
                method: "PATCH"
            })
    }
    public async clearOutlet(): Promise<void> {
        await super.update('outlet_id', null)
    }
    /**
     * Moves this entry to another lorebook.
     * Callers must be aware that <b> this essentially destroys the current reference </b> as entity keys are read-only.
     * As such, it must be discarded after calling this function.
     */
    public async moveToLorebook(id:number) : Promise<boolean> {
        const response = await fetchApi(
            `${getEntityController(EntityTypes.ENTRY)}/exchange?fromLorebookId=${this.get('lorebook_id')}&toLorebookId=${id}&entryId=${this.get('entry_id')}`,
            {
                method:'POST'
            }
        ).then(async response => new Entry((await response.json() as DTO), EntityTypes.ENTRY))
        this.dataMap = response.dataMap;

        return true;
    }
}

export async function getAllKeywords() : Promise<string[]>{
    return await fetchApi(
        `api/${EntityTypes.KEYWORD}`,
        {
            method:'GET'
        }
    ).then(async response => await response.json() as string[])
}

export type OutletKey = { id: number };
export type OutletData = { name: string };
export class Outlet extends ABSEntity<OutletKey, OutletData> {
    protected getReferenceKeyOrder(): (keyof OutletKey & string)[] {
        throw new Error("Tried to get reference key order for outlet");
    }

    getEntityType(): EntityTypes {
        return EntityTypes.OUTLETS
    }

    public static async outlets(): Promise<string[]> {
        return await fetch_all<OutletKey, OutletData, Outlet>(EntityTypes.OUTLETS, Outlet)
            .then(result => result.map(outlet => outlet.get('name')))
    }
}