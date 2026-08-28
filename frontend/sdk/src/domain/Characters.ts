import {
    ABSEntity,
    createEntity,
    deleteEntity,
    EntityAssetType,
    fetch_all,
    fetchFromReference,
    fetchOne,
    StoredAssetDTO
} from "../core/ABSEntity.js";
import {EntityTypes} from "./EntityTypes.js";
import {Lorebook, LorebookData, LorebookKey} from "./Lorebook.js";
import {Location, LocationData, LocationKey} from "./World.js";
import {parseNumberKey} from "../utils/ReferenceCodec.js";

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

/**
 * Join entity that pins a {@link Character} to a {@link Location} as
 * a "starting location" - the place the character is positioned at
 * when a session begins.
 */
export type StartingLocationKeys = {
    worldID: number;
    locationID: number;
    characterID: number;
};
export type StartingLocationData = {
    reason_why: string;
    is_static: boolean;
    ttl: number;
};

export class StartingLocation extends ABSEntity<StartingLocationKeys, StartingLocationData> {
    getEntityType(): EntityTypes {
        return EntityTypes.STARTING_LOCATIONS;
    }

    protected getReferenceKeyOrder(): (keyof StartingLocationKeys & string)[] {
        return ['worldID', 'locationID', 'characterID'];
    }
}

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

    public override asReference(): string {
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

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // STARTING LOCATIONS:
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * Returns every {@link StartingLocation} row that pins this
     * character to a starting location.
     */
    public async getStartingLocations(): Promise<StartingLocation[]> {
        return await fetch_all<StartingLocationKeys, StartingLocationData, StartingLocation>(
            EntityTypes.STARTING_LOCATIONS,
            StartingLocation
        ).then(rows => rows.filter(
            row => row.key.characterID === this.get('id'),
        ));
    }

    /**
     * Returns the {@link StartingLocation} row that pins this
     * character to the supplied location, or null when there is no
     * such row.
     */
    public async getStartingLocation(loc: Location): Promise<StartingLocation | null> {
        const rows = await fetch_all<StartingLocationKeys, StartingLocationData, StartingLocation>(
            EntityTypes.STARTING_LOCATIONS,
            StartingLocation
        );

        return rows.find(
            row => row.key.characterID === this.get('id')
                && row.key.locationID === loc.get('id'),
        ) ?? null;
    }

    /**
     * Pins this character to a starting location.
     *
     * Calling this method for a pair that is already linked updates
     * the existing row's metadata rather than creating a duplicate.
     */
    public async markStartingAt(loc: Location): Promise<void> {
        const existing = await this.getStartingLocation(loc);
        if (existing != null) {
            return;
        }

        await createEntity<StartingLocationKeys, StartingLocationData, StartingLocation>(
            {
                worldID: loc.get('worldID'),
                locationID: loc.get('id'),
                characterID: this.get('id'),
            },
            {
                reason_why: '',
                is_static: false,
                ttl: 0,
            },
            EntityTypes.STARTING_LOCATIONS,
            StartingLocation
        );
    }

    /**
     * Removes the link between this character and a starting
     * location. No-op when the link does not exist.
     */
    public async unmarkStartingAt(loc: Location): Promise<void> {
        const existing = await this.getStartingLocation(loc);
        if (existing == null) {
            return;
        }
        await deleteEntity<StartingLocationKeys>(existing.key, EntityTypes.STARTING_LOCATIONS);
    }

    /**
     * Convenience accessor kept for callers that need a single
     * "primary" starting location.
     */
    public async getPrimaryStartingLocation(): Promise<Location | null> {
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
