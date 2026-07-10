import {
    ABSEntity,
    createEntity,
    deleteEntity,
    fetchApi,
    fetchMatching,
    fetchOne,
    getEntityController
} from "@/core/ABSEntity";
import {EntityTypes} from "@/domain/EntityTypes";
import {API_BASE} from "@/config";
import {DTO} from "@/types/DTOs";
import {Lorebook, LorebookData, LorebookKey} from "@/domain/Lorebook";

export type WorldKey = { id: number }
export type WorldData = {
    name: string,
    lorebook_id: number,
}

export class World extends ABSEntity<WorldKey, WorldData> {
    private static readonly REFERENCE_KEY_ORDER: readonly (keyof WorldKey & string)[] = ['id'] as const;

    private locations: Location[] | null = null;
    private lorebook: Lorebook | null = null;

    getEntityType(): EntityTypes {
        return EntityTypes.WORLDS;
    }

    protected getReferenceKeyOrder(): readonly (keyof WorldKey & string)[] {
        return World.REFERENCE_KEY_ORDER;
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
    /** @return regions with no parents */
    public async getRootRegions() : Promise<Region[]>{
        return await fetchApi(
            `${getEntityController(EntityTypes.REGIONS)}/${this.get('id')}/roots`,
            {
                method:'GET'
            }
        ).then(async response => (await response.json() as DTO[]).map(dto => new Region(dto)))
    }
    public async getAllRegions(): Promise<Region[]> {
        return await fetchMatching<RegionKey, RegionData, Region>(
            {
                world_id: this.get('id')
            },
            EntityTypes.REGIONS,
            Region
        )
    }
    /** @return a list of locations with no parent region */
    public async getFreeLocations():Promise<Location[]>{
        return await fetchApi(
            `${getEntityController(EntityTypes.LOCATIONS)}/ofRegion?worldId=${this.get('id')}`,
            {
                method:'GET'
            }
        ).then(async response => (await response.json() as DTO[]).map(dto => new Location(dto)))
    }
}

export type RegionKey = {world_id: number, id:number}
export type RegionData = {parent_region_id:number, lorebook_id:number, name:string}
export class Region extends ABSEntity<RegionKey, RegionData>{
    private static readonly REFERENCE_KEY_ORDER: readonly (keyof RegionKey & string)[] = ['world_id' , 'id'] as const;

    getEntityType(): EntityTypes {
        return EntityTypes.REGIONS;
    }

    protected getReferenceKeyOrder(): readonly (keyof RegionKey & string)[] {
        return Region.REFERENCE_KEY_ORDER;
    }

    public async getLocations() : Promise<Location[]> {
        return await fetchApi(
            `${getEntityController(EntityTypes.LOCATIONS)}/ofRegion?worldId=${this.get('world_id')}&regionId=${this.get('id')}`,
            {
                method:'GET'
            }
        ).then(
            async response => (await response.json() as DTO[]).map(dto => new Location(dto, EntityTypes.LOCATIONS))
        );
    }

    public async getFirstChildren() : Promise<Region[]> {
        return await fetchApi(
            `${getEntityController(EntityTypes.REGIONS)}/childrenOf?worldId=${this.get('world_id')}&regionId=${this.get('id')}`,
            {
                method:'GET'
            }
        ).then(
            async response => (await response.json() as DTO[]).map(dto => new Region(dto, EntityTypes.REGIONS))
        )
    }

    public async getLorebook():Promise<Lorebook>{
        return await fetchOne<LorebookKey,LorebookData,Lorebook>(
            {
                id:this.get('lorebook_id')
            },
            EntityTypes.LOREBOOKS,
            Lorebook
        )
    }
}

export type LocationKey = { worldID: number, id: number }
export type LocationData = {
    name: string,
    lorebook_id: number,
    region_id: number | null
}

export class Location extends ABSEntity<LocationKey, LocationData> {
    private static readonly REFERENCE_KEY_ORDER = ['worldID', 'id'] as const;
    private lorebook: Lorebook | null = null;

    getEntityType(): EntityTypes {
        return EntityTypes.LOCATIONS;
    }

    protected getReferenceKeyOrder(): readonly (keyof LocationKey & string)[] {
        return Location.REFERENCE_KEY_ORDER;
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
    /** @return neighbors ( from this location to other or the inverse) */
    public async getNeighbours(): Promise<Location[]>{
        return await fetchApi(
            `${API_BASE}/${EntityTypes.EDGES}/${this.get('worldID')}/${this.get('id')}/neighbours`,
            {
                method:'GET',
                headers: new Headers({accept:'application/json'})
            }
        ).then(async response => (await response.json() as DTO[]).map(dto => new Location(dto)))
    }

    public async getOutEdges() : Promise<LocationEdge[]> {
        return await fetchMatching<EdgeKey, EdgeData, LocationEdge>(
            {world_id: this.get('worldID'), from_id: this.get('id')},
            EntityTypes.EDGES,
            LocationEdge
        )
    }

    public async getInEdges() : Promise<LocationEdge[]> {
        return await fetchMatching<EdgeKey, EdgeData, LocationEdge>(
            {world_id: this.get('worldID'), to_id: this.get('id')},
            EntityTypes.EDGES,
            LocationEdge
        )
    }

    public async connect(other: Location, initial_data:Partial<EdgeData>): Promise<LocationEdge>{
        return await createEntity<EdgeKey, EdgeData, LocationEdge>(
            {
                world_id: this.get('worldID'),
                from_id: this.get('id')!,
                to_id: other.get('id')!,
            },
            initial_data,
            EntityTypes.EDGES,
            LocationEdge
        )
    }

    public async disconnect(other: Location): Promise<boolean> {
        return await deleteEntity<EdgeKey>(
            {
                world_id: this.get('worldID')!,
                from_id: this.get('id')!,
                to_id: other.get('id')!,
            },
            EntityTypes.EDGES,
        );
    }

    public async getEdgeInfo(other:Location):Promise<LocationEdge>{
        return await fetchOne<EdgeKey,EdgeData,LocationEdge>({
            world_id: this.get('worldID'),
            from_id: this.get('id')!,
            to_id: other.get('id')!,
        }, EntityTypes.EDGES, LocationEdge)
    }
}

export type EdgeKey = { world_id: number, from_id: number, to_id: number }
export type EdgeData = {
    edge_description: string,
    show_destination_name: boolean
    show_destination_description: boolean,
    is_traversable:boolean
}

export class LocationEdge extends ABSEntity<EdgeKey, EdgeData> {
    getEntityType(): EntityTypes {
        return EntityTypes.EDGES;
    }

    protected getReferenceKeyOrder(): (keyof EdgeKey & string)[] {
        throw new Error("Not yet implemented");
    }

}