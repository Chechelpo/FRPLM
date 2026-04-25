import {EntityABS} from "@/entities/EntityABS";
import {FieldInfo} from "@/types/FieldMetadata";
import {Character} from "@/entities/chars/Characters";
import {filterWithAttribute} from "@/utils/filters";
import {fetch_all} from "@/utils/EntityFetch";
import {DataRecord, EntityField, KeyRecord} from "@/types/DTOs/DTOs";
import {ControllerType} from "@/config/ControllerType";
import {CommonFields} from "@/entities/CommonFields";

interface StartingLocationKeys extends KeyRecord{
    worldID: number;
    characterID: number;
}
interface StartingLocationData extends DataRecord {
    locationID: number;
    description: string;
}

export class StartingLocation extends EntityABS<StartingLocationKeys, StartingLocationData> {
    getEntityType(): ControllerType {
        return ControllerType.STARTING_LOCATIONS;
    }

    getIterationArr(): EntityField<StartingLocationKeys, StartingLocationData>[] {
        return [CommonFields.DESCRIPTION];
    }


    public static async getOfCharacter(character: Character): Promise<StartingLocation[]> {
        return filterWithAttribute(
            "characterID",
            character.get("id"),
            await fetch_all<StartingLocationKeys,StartingLocationData, StartingLocation>(ControllerType.STARTING_LOCATIONS, this),
        )
    }


    public getOfField<F extends keyof StartingLocationData>(field: F): Promise<FieldInfo | null> {
        return Promise.resolve(null);
    }

}