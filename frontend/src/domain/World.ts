import {
    ABSEntity,
    createEntity,
    deleteEntity,
    EntityField,
    fetchApi,
    fetchMatching,
    fetchOne,
    getEntityController,
    UpdateEntityField
} from "@/frameworks/ABSEntity";
import {EntityTypes} from "@/domain/EntityTypes";
import {CommonFields} from "@/utils/CommonFields";
import {API_BASE} from "@/config";
import {DTO} from "@/types/DTOs";
import {Lorebook, LorebookData, LorebookKey} from "@/domain/Lorebook";

export type WorldKey = { id: number }
export type WorldData = {
    name: string,
    lorebook_id: number,
}

export class World extends ABSEntity<WorldKey, WorldData> {
    private locations: Location[] | null = null;
    private lorebook: Lorebook | null = null;

    getEntityType(): EntityTypes {
        return EntityTypes.WORLDS;
    }

    getIterationArr(): EntityField<WorldKey, WorldData>[] {
        return [CommonFields.NAME];
    }
    public async getLorebook(): Promise<Lorebook> {
        if (this.lorebook == null) {
            this.lorebook = await fetchOne<LorebookKey,LorebookData,Lorebook>(
                {
                    id: this.get('lorebook_id')!
                },
                EntityTypes.LOREBOOKS,
                Lorebook
            )
        }

        return this.lorebook;
    }

    public async getLocations(): Promise<Location[]> {
        return fetchMatching<LocationKey, LocationData, Location>(
            {
                worldID: this.get('id')
            },
            EntityTypes.LOCATIONS,
            Location
        )
    }

    public async addLocation(name:string): Promise<Location> {
        if (this.locations == null)
            this.locations = await this.getLocations();

        const location: Location = await createEntity<LocationKey, LocationData, Location>(
            {worldID: this.get('id')},
            {name: name},
            EntityTypes.LOCATIONS,
            Location
        );
        this.locations.push(location);

        return location;
    }

    public async deleteLocation(location_id: number): Promise<boolean> {
        if (this.locations == null)
            this.locations = await this.getLocations();

        const success:boolean = await deleteEntity<LocationKey>(
            {
                worldID: this.get('id'),
                id:location_id
            },
            EntityTypes.LOCATIONS
        )
        if (success)
            this.locations.filter(loc => loc.get('id') != location_id)

        return success;
    }
}

export type LocationKey = { worldID: number, id?: number }
export type LocationData = {
    name: string,
    lorebook_id: number,
}

export class Location extends ABSEntity<LocationKey, LocationData> {
    private lorebook: Lorebook | null = null;
    private neighbors: Location[] | null = null;

    getEntityType(): EntityTypes {
        return EntityTypes.LOCATIONS;
    }

    getIterationArr(): EntityField<LocationKey, LocationData>[] {
        return [CommonFields.NAME];
    }

    public async getLorebook(): Promise<Lorebook> {
        if (this.lorebook == null)
            this.lorebook = await fetchOne<LorebookKey,LorebookData, Lorebook>(
                {
                    id: this.get('lorebook_id')!,
                },
                EntityTypes.LOREBOOKS,
                Lorebook
            )

        return this.lorebook;
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // LOCATION EDGES
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    public async getNeighbours(): Promise<Location[]>{
        if (this.neighbors == null)
            this.neighbors = await getNeighbours(this.key)
        console.log(`Neighbours: \n ${this.neighbors}`)
        return this.neighbors;
    }
    public async isNeighbour(other:Location): Promise<boolean> {
        await this.getNeighbours()
        return this.neighbors!.some(loc => loc.key == other.key)
    }

    public async updateEdgeInfo<T extends keyof EdgeData>(other:Location, field:T, value:EdgeData[T]): Promise<boolean> {
        if (await this.isNeighbour(other)){
            const success = await UpdateEntityField<EdgeData, EdgeKey, T>(
                {
                    world_id: this.get('worldID'),
                    location1_id: this.get('id')!,
                    location2_id: other.get('id')!,
                },
                field, value,
                EntityTypes.EDGES
            )

            if (success) console.debug(`Updated edge info of ${this} and ${other} \n update: ${field} = ${value}`)
            else console.error(`Error updating edge info of ${this} and ${other} \n update: ${field} = ${value}`)

            return success
        } else {
            console.error(`${other} is not a neighbour of ${this} `)
            return false
        }
    }

    public async connect(other: Location): Promise<boolean>{
        if (await this.isNeighbour(other)) //Pre-existing edge
            return true;

        const newEdge = await createEntity<EdgeKey, EdgeData, LocationEdge>(
            {
                world_id: this.get('worldID'),
                location1_id: this.get('id')!,
                location2_id: other.get('id')!,
            },
            null,
            EntityTypes.EDGES,
            LocationEdge
        )
        this.neighbors!.push(other);
        return true;
    }

    public async disconnect(other: Location): Promise<boolean> {
        if (!await this.isNeighbour(other))
            return false;
        const success = await deleteEntity<EdgeKey>(
            {
                world_id: this.get('worldID')!,
                location1_id: this.get('id')!,
                location2_id: other.get('id')!,
            },
            EntityTypes.EDGES,
        )

        if (success) {
            this.neighbors = this.neighbors!.filter(loc => loc.key != other.key)
            console.log(`Disconnected location ${this} \n from ${other}`)

        } else console.error(`Error occurred while disconnecting ${this} \n from ${other}`)

        return success;
    }

    public async getEdgeInfo(other:Location):Promise<LocationEdge>{
        if (!await this.isNeighbour(other))
            throw new Error("Tried to get edge info for a non-connected location")
        return await fetchOne<EdgeKey,EdgeData,LocationEdge>({
            world_id: this.get('worldID'),
            location1_id: this.get('id')!,
            location2_id: other.get('id')!,
        }, EntityTypes.EDGES, LocationEdge)
    }
}

async function getNeighbours(key:LocationKey): Promise<Location[]> {
    const response = await fetchApi(
        `${API_BASE}/${EntityTypes.LOCATIONS}/entity/ofLocation/${key.worldID}/${key.id!}`,
        {
            method:'GET',
            headers: new Headers({accept:'application/json'})
        }
    )
    const dtos = await response.json() as DTO[];
    console.log(dtos);
    return dtos.map(dto => new Location(dto, EntityTypes.LOCATIONS));
}

export type RegionKey = {world_id: number, id:number}
export type RegionData = {parent_region_id:number, lorebook_id:number, name:string}

export class Region extends ABSEntity<RegionKey, RegionData>{
    getEntityType(): EntityTypes {
        return EntityTypes.REGIONS;
    }

    public async getLocations() : Promise<Location[]> {
        return await fetchApi(
            `${getEntityController(EntityTypes.REGIONS)}/ofRegion?worldId=${this.get('world_id')}&regionId=${this.get('id')}`,
            {
                method:'GET'
            }
        ).then(
            async response => (await response.json() as DTO[]).map(dto => new Location(dto, EntityTypes.LOCATIONS))
        );
    }

    public async getFirstChildren() : Promise<Region[]> {
        return await fetchApi(
            `${getEntityController(EntityTypes.REGIONS)}/ofRegion?worldId=${this.get('world_id')}&regionId=${this.get('id')}`,
            {
                method:'GET'
            }
        ).then(
            async response => (await response.json() as DTO[]).map(dto => new Region(dto, EntityTypes.REGIONS))
        )
    }
}

export type EdgeKey = { world_id: number, location1_id: number, location2_id: number }
export type EdgeData = {
    description: string | null,
    travel_cost: number
}

export class LocationEdge extends ABSEntity<EdgeKey, EdgeData> {
    getEntityType(): EntityTypes {
        return EntityTypes.EDGES;
    }

    getIterationArr(): EntityField<EdgeKey, EdgeData>[] {
        return [CommonFields.DESCRIPTION];
    }
}