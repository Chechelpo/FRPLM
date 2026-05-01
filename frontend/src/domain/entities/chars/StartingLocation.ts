import {EntityABS} from "@/frameworks/entities/EntityABS";
import {FieldInfo} from "@/frameworks/entities/FieldMetadata";
import {Character} from "@/domain/entities/chars/Characters";
import {filterWithAttribute} from "@/utils/filters";
import {fetch_all} from "@/domain/entities/EntityFetch";
import {DataRecord, EntityField, KeyRecord} from "@/types/DTOs";
import {EntityTypes} from "@/frameworks/entities/EntityTypes";
import {CommonFields} from "@/utils/CommonFields";

interface StartingLocationKeys extends KeyRecord{
    worldID: number;
    characterID: number;
}
interface StartingLocationData extends DataRecord {
    locationID: number;
    description: string;
}

export class StartingLocation extends EntityABS<StartingLocationKeys, StartingLocationData> {
    getEntityType(): EntityTypes {
        return EntityTypes.STARTING_LOCATIONS;
    }

    getIterationArr(): EntityField<StartingLocationKeys, StartingLocationData>[] {
        return [CommonFields.DESCRIPTION];
    }


    public static async getOfCharacter(character: Character): Promise<StartingLocation[]> {
        return filterWithAttribute(
            "characterID",
            character.get("id"),
            await fetch_all<StartingLocationKeys,StartingLocationData, StartingLocation>(EntityTypes.STARTING_LOCATIONS, this),
        )
    }


    public getOfField<F extends keyof StartingLocationData>(field: F): Promise<FieldInfo | null> {
        return Promise.resolve(null);
    }

}