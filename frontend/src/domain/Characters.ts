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
import {Lorebook, LorebookData, LorebookKey} from "@/domain/Lorebook";
import {Location, LocationData, LocationKey} from "@/domain/World";
import {parseNumberKey} from "@/utils/ReferenceCodec";

export type CharacterKey = {
    id: number,
    world_id: number
};
export type CharacterData = {
    readonly lorebook_id: number,

    name: string,
    description: string,
    can_be_user: boolean,
    welcome_message: string | null,

    starting_location_id: number | null,
    is_static: boolean,
    reason_why: string,
    ttl: number,
};

export class Character extends ABSEntity<CharacterKey, CharacterData> {
    private lorebook: Lorebook | null = null;
    private static readonly REFERENCE_KEY_ORDER = ["world_id", "id"] as const;

    getEntityType(): EntityTypes {
        return EntityTypes.CHARACTERS;
    }

    protected getReferenceKeyOrder(): readonly ["world_id", "id"] {
        return Character.REFERENCE_KEY_ORDER;
    }

    public static async getAll(): Promise<Character[]> {
        return await fetch_all<CharacterKey, CharacterData, Character>(
            EntityTypes.CHARACTERS, Character
        );
    }

    public static async getWithID(id: number, world_id: number): Promise<Character> {
        return await fetchOne<CharacterKey, CharacterData, Character>({
            id: id,
            world_id: world_id
        }, EntityTypes.CHARACTERS, this)
    }

    public static async fetchFromReference(reference: string): Promise<Character> {
        return await fetchFromReference<CharacterKey, CharacterData, Character>(
            reference,
            EntityTypes.CHARACTERS,
            Character.REFERENCE_KEY_ORDER,
            {
                id: parseNumberKey,
                world_id: parseNumberKey
            },
            Character
        )
    }

    public static async delete(character: Character): Promise<boolean> {
        return await deleteEntity<CharacterKey>(character.key, EntityTypes.CHARACTERS);
    }

    public asReference(): string {
        return `connection: ${this.get('id')}`
    }

    public async getLorebook(): Promise<Lorebook> {
        if (this.lorebook == null)
            this.lorebook = await fetchOne<LorebookKey, LorebookData, Lorebook>(
                {
                    id: this.get("lorebook_id")!
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
    public async saveAvatar(avatar: Blob, replace: boolean = true): Promise<StoredAssetDTO> {
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
    public async deleteAvatar(): Promise<void> {
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
    public async getStartingLocation(): Promise<Location | null> {
        if (this.get('starting_location_id') === null) return null;
        return await fetchOne<LocationKey, LocationData, Location>(
            { worldID: this.get('world_id'), id: this.get('starting_location_id')!},
            EntityTypes.LOCATIONS,
            Location
        )
    }
}

type CharacterTagsKey = { char_id: number, tag_id: number };
type CharacterTagsData = {}

class CharacterTags extends ABSEntity<CharacterTagsKey, CharacterTagsData> {
    getEntityType(): EntityTypes {
        return EntityTypes.CHARACTER_TAGS;
    }

    protected getReferenceKeyOrder(): readonly (keyof CharacterTagsKey & string)[] {
        return ['char_id', 'tag_id'];
    }

    public static async registerTag(key: CharacterTagsKey): Promise<boolean> {
        await createEntity<CharacterTagsKey, CharacterTagsData, CharacterTags>(
            key,
            null,
            EntityTypes.CHARACTER_TAGS,
            this
        )
        return true;
    }

    public static async unregisterTag(key: CharacterTagsKey): Promise<boolean> {
        return await deleteEntity<CharacterTagsKey>(key, EntityTypes.CHARACTER_TAGS);
    }
}

export type SessionCharacterKey = {
    session_id: number,
    id: number
}
export type SessionCharacterData = {
    permanent_character_id: number | null,
    keep_updated : boolean,

    name: string,
    description : string

    session_lorebook_id : number,
    world_id : number,
    current_location_id : number
}
export class SessionCharacter extends ABSEntity<SessionCharacterKey, SessionCharacterData> {
    getEntityType(): EntityTypes {
        return EntityTypes.SESSION_CHARACTERS;
    }

    protected getReferenceKeyOrder(): (keyof SessionCharacterKey & string)[] {
        return ['session_id', 'id'];
    }

    public async getPermanentCharacter() : Promise<Character | null> {
        if (this.dataMap.permanent_character_id == null) return null;
        return await fetchOne<CharacterKey, CharacterData, Character>(
            {
                id: this.get("permanent_character_id")!,
                world_id: this.get('world_id')
            },
            EntityTypes.CHARACTERS,
            Character
        )
    }

    public async getCurrentLocation() : Promise<Location> {
        return await fetchOne<LocationKey, LocationData, Location>(
            {
                worldID: this.get('world_id'),
                id: this.get('current_location_id')
            },
            EntityTypes.SESSION_CHARACTERS,
            Location
        )
    }
}