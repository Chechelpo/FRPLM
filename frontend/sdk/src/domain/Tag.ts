import {ABSEntity} from "../core/ABSEntity.js";
import {EntityTypes} from "../domain/EntityTypes.js";
import {DTO, EntityField} from "../types/DTOs.js";
import {CharacterKey} from "../domain/Characters.js";
import {API_BASE} from "../config.js";
import {fetchApi} from "../services/apiClient.js";

export type TagKey = {id:number}
export type TagData = {name:string, color?:string}

export class Tag extends ABSEntity<TagKey,TagData>{
    getEntityType(): EntityTypes {
        return EntityTypes.TAGS;
    }

    protected getReferenceKeyOrder(): readonly (keyof TagKey & string)[] {
        return ['id'] as const;
    }


    getIterationArr(): EntityField<TagKey, TagData>[] {
        return [];
    }

    public static async ofCharacter(key: CharacterKey): Promise<Tag[]> {
        const response = await fetchApi(
            `api/${EntityTypes.TAGS}/${EntityTypes.CHARACTER_TAGS}/${key.id}`,
            {
                method: "GET",
            })
        const dtos = await response.json() as DTO[];
        return dtos.map(dto => new Tag(dto, EntityTypes.TAGS));
    }
}