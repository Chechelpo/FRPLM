import {EntityABS} from "@/frameworks/entities/EntityABS";
import {EntityTypes} from "@/frameworks/entities/EntityTypes";
import {DTO, EntityField} from "@/types/DTOs";
import {CharacterKey} from "@/domain/entities/chars/Characters";
import {fetchApi} from "@/domain/entities/EntityFetch";
import {API_BASE} from "@/config";

export type TagKey = {id:number}
export type TagData = {name:string, color?:string}

export class Tag extends EntityABS<TagKey,TagData>{
    getEntityType(): EntityTypes {
        return EntityTypes.TAGS;
    }

    getIterationArr(): EntityField<TagKey, TagData>[] {
        return [];
    }

    public static async ofCharacter(key: CharacterKey): Promise<Tag[]> {
        const response = await fetchApi(
            `${API_BASE}/${EntityTypes.TAGS}/${EntityTypes.CHARACTER_TAGS}/${key.id}`,
            {
                method: "GET",
            })
        const dtos = await response.json() as DTO[];
        return dtos.map(dto => new Tag(dto, EntityTypes.TAGS));
    }
}