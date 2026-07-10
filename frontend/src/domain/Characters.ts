import {
    ABSEntity,
    createEntity,
    deleteEntity,
    EntityField,
    fetch_all,
    fetchApi,
    fetchFromReference,
    fetchOne
} from "@/core/ABSEntity";
import {EntityTypes} from "@/domain/EntityTypes";
import {CommonFields} from "@/utils/CommonFields";
import {Tag} from "@/domain/Tag";
import {Lorebook, LorebookData, LorebookKey} from "@/domain/Lorebook";
import {Location, World} from "@/domain/World";
import {DTO} from "@/types/DTOs";
import {API_BASE} from "@/config";
import {parseNumberKey} from "@/utils/ReferenceCodec";

export type CharacterKey = { id: number };
export type CharacterData = {
    name: string,
    is_archetype: boolean,
    can_be_user: boolean,
    firstMessage: string,
    lorebook_id?: number
};
export type CharacterCreationData = Pick<CharacterData, 'name'>

export class Character extends ABSEntity<CharacterKey, CharacterData> {
    private tags: Tag[] | null = null;
    private lorebook: Lorebook | null = null;
    private starting_locations: Location[] | null = null
    private static readonly REFERENCE_KEY_ORDER = ["id"] as const;

    getEntityType(): EntityTypes {
        return EntityTypes.CHARACTERS;
    }
    protected getReferenceKeyOrder(): readonly "id"[] {
        return Character.REFERENCE_KEY_ORDER;
    }

    public static async getAll() : Promise<Character[]> {
        return await fetch_all<CharacterKey, CharacterData, Character>(
            EntityTypes.CHARACTERS, Character
        );
    }
    public static async getWithID(id: number): Promise<Character> {
        return await fetchOne<CharacterKey, CharacterData, Character>({id:id}, EntityTypes.CHARACTERS, this)
    }
    public static async createNew(initialData:CharacterCreationData): Promise<Character> {
        if (initialData.name === undefined || initialData.name === null)
            throw new Error("Name is required for character instantiation");
        return await createEntity(null, initialData, EntityTypes.CHARACTERS, this)
    }
    public static async getStartingAt(world:World):Promise<Character[]>{
        const response = await fetchApi(
            `${API_BASE}/${EntityTypes.CHARACTERS}/${world.get('id')}`,
            {
                method:'GET'
            }
        )
        return (await response.json() as DTO[]).map(dto => new Character(dto, EntityTypes.CHARACTERS))
    }
    public static async fromReference(reference:string): Promise<Character> {
        return await fetchFromReference<CharacterKey, CharacterData, Character>(
            reference,
            EntityTypes.CHARACTERS,
            Character.REFERENCE_KEY_ORDER,
            {
                id:parseNumberKey
            },
            Character
        )
    }

    public asReference() : string {
        return `connection: ${this.get('id')}`
    }

    public async getLorebook(): Promise<Lorebook> {
        if (this.lorebook == null)
            this.lorebook = await fetchOne<LorebookKey, LorebookData, Lorebook>(
                {
                    id:this.get("lorebook_id")!
                },
                EntityTypes.LOREBOOKS,
                Lorebook
            )
        return this.lorebook;
    }

    public async getTags(): Promise<Tag[]> {
        if (this.tags == null)
            this.tags = await Tag.ofCharacter(this.key);
        return this.tags;
    }

    public async removeTag(tag:Tag): Promise<void> {
        if (
            !await CharacterTags.unregisterTag({
            char_id: this.get('id'),
            tag_id: tag.get('id')
            })
        ) {
            console.error(`Error removing tag ${tag} from character ${this}`)
            throw new Error(`Error removing tag ${tag} from character ${this}`)
        }
        console.info(`Removed tag ${tag} of character ${this}`)

        if (this.tags != null) this.tags = this.tags.filter(t => !t.equals(tag))
    }

    public async addTag(tag: Tag): Promise<void> {
        if (
            !await CharacterTags.registerTag(
                {
                    char_id: this.get('id'),
                    tag_id: tag.get('id')
                })
        ) {
            console.error("Error adding new tag", tag);
            throw new Error(`Error adding new tag ${tag} to character ${this}`);
        }
        console.info(`Added new tag ${tag} to character ${this}`)
        if (this.tags != null)
            this.tags.push(tag)
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // STARTING LOCATIONS:
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    public async getStartingLocations(): Promise<Location[]>{
        if (this.starting_locations == null) {
            console.debug(`Fetching starting locations of character ${this}`)
            this.starting_locations = await getStartingLocations(this.key);
        }
        return this.starting_locations;
    }

    public async markStartingAt(loc:Location):Promise<void>{
        if (this.starting_locations == null)
            this.starting_locations = await getStartingLocations(this.key);

        await createEntity<StartingLocationKeys,StartingLocationData,StartingLocation>(
            {
                worldID: loc.get('worldID'),
                locationID: loc.get('id'),
                characterID: this.get('id'),
            },
            null,
            EntityTypes.STARTING_LOCATIONS,
            StartingLocation
        )

        //If it's unsuccessful, it should throw before this
        this.starting_locations.push(loc)
    }

    public async getStartingLocation(loc:Location):Promise<StartingLocation>{
        return await fetchOne<StartingLocationKeys,StartingLocationData,StartingLocation>(
            {
                worldID: loc.get('worldID')!,
                locationID: loc.get('id')!,
                characterID: this.get('id'),
            },
            EntityTypes.STARTING_LOCATIONS,
            StartingLocation
        )
    }
}

type CharacterTagsKey = {char_id: number, tag_id: number};
type CharacterTagsData = {}

class CharacterTags extends ABSEntity<CharacterTagsKey, CharacterTagsData>{
    getEntityType(): EntityTypes {
        return EntityTypes.CHARACTER_TAGS;
    }

    protected getReferenceKeyOrder(): readonly (keyof CharacterTagsKey & string)[] {
        return ['char_id', 'tag_id'];
    }

    public static async registerTag(key:CharacterTagsKey): Promise<boolean> {
        await createEntity<CharacterTagsKey,CharacterTagsData,CharacterTags>(
            key,
            null,
            EntityTypes.CHARACTER_TAGS,
            this
        )
        return true;
    }
    public static async unregisterTag(key:CharacterTagsKey): Promise<boolean> {
        return await deleteEntity<CharacterTagsKey>(key, EntityTypes.CHARACTER_TAGS);
    }
}

async function getStartingLocations(key:CharacterKey): Promise<Location[]> {
    const response = await fetchApi(
        `${API_BASE}/${EntityTypes.LOCATIONS}/entity/ofCharacter/${key.id}`,
        {
            method: 'GET'
        }
    )
    console.debug(response)
    const dtos = await response.json() as DTO[];
    return dtos.map(dto => new Location(dto, EntityTypes.LOCATIONS));
}

export type StartingLocationKeys = {
    worldID: number,
    locationID: number,
    characterID: number
}
export type StartingLocationData = {
    reason_why: string,
    is_static: boolean,
    ttl: number
}

export class StartingLocation extends ABSEntity<StartingLocationKeys, StartingLocationData> {
    getEntityType(): EntityTypes {
        return EntityTypes.STARTING_LOCATIONS;
    }

    protected getReferenceKeyOrder(): (keyof StartingLocationKeys & string)[] {
        return ["worldID", "locationID", "characterID"];
    }

}