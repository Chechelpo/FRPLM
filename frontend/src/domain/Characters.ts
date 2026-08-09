import {
    ABSEntity,
    createEntity,
    deleteEntity,
    EntityAssetType,
    fetch_all,
    fetchFromReference,
    fetchOne,
    StoredAssetDTO
} from "@/core/ABSEntity";
import {EntityTypes} from "@/domain/EntityTypes";
import {Tag} from "@/domain/Tag";
import {Lorebook, LorebookData, LorebookKey} from "@/domain/Lorebook";
import {Location} from "@/domain/World";
import {DTO} from "@/types/DTOs";
import {parseNumberKey} from "@/utils/ReferenceCodec";
import {fetchApi} from "@/services/apiClient";

export type CharacterKey = {
    id: number,
    world_id: number
};
export type CharacterData = {
    name: string,
    can_be_user: boolean,
    firstMessage: string,
    lorebook_id: number
};

export class Character extends ABSEntity<CharacterKey, CharacterData> {
    private tags: Tag[] | null = null;
    private lorebook: Lorebook | null = null;
    private starting_locations: Location[] | null = null
    private static readonly REFERENCE_KEY_ORDER = ["world_id","id"] as const;

    getEntityType(): EntityTypes {
        return EntityTypes.CHARACTERS;
    }
    protected getReferenceKeyOrder(): readonly ["world_id", "id"] {
        return Character.REFERENCE_KEY_ORDER;
    }

    public static async getAll() : Promise<Character[]> {
        return await fetch_all<CharacterKey, CharacterData, Character>(
            EntityTypes.CHARACTERS, Character
        );
    }

    public static async getWithID(id: number, world_id:number): Promise<Character> {
        return await fetchOne<CharacterKey, CharacterData, Character>({id:id, world_id:world_id}, EntityTypes.CHARACTERS, this)
    }

    public static async fetchFromReference(reference:string): Promise<Character> {
        return await fetchFromReference<CharacterKey, CharacterData, Character>(
            reference,
            EntityTypes.CHARACTERS,
            Character.REFERENCE_KEY_ORDER,
            {
                id:parseNumberKey,
                world_id:parseNumberKey
            },
            Character
        )
    }
    public static async delete(character:Character) : Promise<boolean> {
        return await deleteEntity<CharacterKey>(character.key, EntityTypes.CHARACTERS);
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
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Assets:
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    /**
     * Fetches this character's avatar.
     *
     * @return the avatar Blob, or null when no avatar is stored
     */
    public async fetchAvatar(): Promise<Blob | null> {
        return this.getAsset(EntityAssetType.AVATAR);
    }

    /**
     * Uploads or replaces this character's avatar.
     *
     * @param avatar image data to upload
     * @param replace when false, saving fails if a background already exists
     * @return metadata for the stored avatar
     */
    public async saveAvatar(avatar: Blob, replace:boolean = true): Promise<StoredAssetDTO> {
        return this.postAsset(
            EntityAssetType.AVATAR,
            avatar,
            replace,
        );
    }

    /**
     * Deletes this character's avatar.
     *
     * The underlying operation is idempotent.
     */
    public async deleteAvatar() : Promise<void> {
        await this.deleteAsset(EntityAssetType.AVATAR);
    }

/*
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // TAGS
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    Tags are ignored for now, as they serve no direct purpose for the engine.

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
*/
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

    public async unmarkStartingAt(loc: Location): Promise<void> {
        const startingLocations = this.starting_locations ?? await getStartingLocations(this.key);
        this.starting_locations = startingLocations;

        const deleted = await deleteEntity<StartingLocationKeys>(
            {
                worldID: loc.get('worldID'),
                locationID: loc.get('id'),
                characterID: this.get('id'),
            },
            EntityTypes.STARTING_LOCATIONS,
        );

        if (!deleted)
            throw new Error(`Could not remove starting location ${loc} from character ${this}`);

        this.starting_locations = startingLocations.filter(
            startingLocation => !startingLocation.equals(loc),
        );
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
        `api/${EntityTypes.LOCATIONS}/entity/ofCharacter/${key.id}`,
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