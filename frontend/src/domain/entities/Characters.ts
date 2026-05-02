import {EntityABS, EntityField} from "@/frameworks/entities/EntityABS";
import {createEntity, deleteEntity, fetch_all, fetchOne} from "@/domain/entities/EntityFetch";
import {EntityTypes} from "@/frameworks/entities/EntityTypes";
import {CommonFields} from "@/utils/CommonFields";
import {Tag} from "@/domain/entities/tags/Tag";
import {Lorebook, LorebookData, LorebookKey} from "@/domain/entities/Lorebook";
import {filterWithAttribute} from "@/utils/filters";

export type CharacterKey = { id: number };
export type CharacterData = {
    name: string,
    lorebook_id?: number
};

export class Character extends EntityABS<CharacterKey, CharacterData> {
    private static readonly type:EntityTypes = EntityTypes.CHARACTERS;
    private tags: Tag[] | null = null;
    private lorebook: Lorebook | null = null;

    getEntityType(): EntityTypes {
        return Character.type;
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

        if (this.tags != null)
            this.tags = this.tags.filter(t => !t.equals(tag))
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

    public static async getWithID(id: CharacterKey): Promise<Character> {
        return await fetchOne<CharacterKey, CharacterData, Character>(id, this.type, this)
    }

    public static async createNew(initialData:CharacterData): Promise<Character> {
        if (initialData.name === undefined || initialData.name === null)
            throw new Error("Name is required for character instantiation");
        return await createEntity(null, initialData, this.type, this)
    }

    getIterationArr(): EntityField<CharacterKey,CharacterData>[] {
        return [CommonFields.NAME];
    }
}

type CharacterTagsKey = {char_id: number, tag_id: number};
type CharacterTagsData = {}

class CharacterTags extends EntityABS<CharacterTagsKey, CharacterTagsData>{
    getEntityType(): EntityTypes {
        return EntityTypes.CHARACTER_TAGS;
    }

    getIterationArr(): EntityField<CharacterTagsKey, CharacterTagsData>[] {
        return [];
    }

    static async registerTag(key:CharacterTagsKey): Promise<boolean> {
        await createEntity<CharacterTagsKey,CharacterTagsData,CharacterTags>(
            key,
            null,
            EntityTypes.CHARACTER_TAGS,
            this
        )
        return true;
    }
    static async unregisterTag(key:CharacterTagsKey): Promise<boolean> {
        return await deleteEntity<CharacterTagsKey>(key, EntityTypes.CHARACTER_TAGS);
    }
}


type StartingLocationKeys = {
    worldID: number,
    characterID: number
}
type StartingLocationData = {
    locationID: number,
    description: string
}

class StartingLocation extends EntityABS<StartingLocationKeys, StartingLocationData> {
    getEntityType(): EntityTypes {
        return EntityTypes.STARTING_LOCATIONS;
    }

    getIterationArr(): EntityField<StartingLocationKeys, StartingLocationData>[] {
        return [CommonFields.DESCRIPTION];
    }


    static async getOfCharacter(character: Character): Promise<StartingLocation[]> {
        return filterWithAttribute(
            "characterID",
            character.get("id"),
            await fetch_all<StartingLocationKeys,StartingLocationData, StartingLocation>(EntityTypes.STARTING_LOCATIONS, this),
        )
    }

}