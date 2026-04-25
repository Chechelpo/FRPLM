import {EntityABS, EntityField} from "@/entities/EntityABS";
import {create_Entity, fetch_all, fetchOne} from "@/utils/EntityFetch";
import {ControllerType} from "@/config/ControllerType";
import {CommonFields} from "@/entities/CommonFields";

export type CharacterKey = { id: number };
export type CharacterData = {
    name?: string,
    lorebook_id: number
};

export class Character extends EntityABS<CharacterKey, CharacterData> {
    private static readonly type:ControllerType = ControllerType.CHARACTERS;

    getEntityType(): ControllerType {
        return Character.type;
    }

    public static async getWithID(id: CharacterKey): Promise<Character> {
        return await fetchOne<CharacterKey, CharacterData, Character>(id, this.type, this)
    }

    public static async createNew(initialData:CharacterData): Promise<Character> {
        if (initialData.name === undefined || initialData.name === null)
            throw new Error("Name is required for character instantiation");
        return await create_Entity(null, initialData, this.type, this)
    }

    getIterationArr(): EntityField<CharacterKey,CharacterData>[] {
        return [CommonFields.NAME];
    }
}