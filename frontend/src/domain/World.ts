// src/domain/World.ts
import {
    ABSEntity,
    createEntity,
    deleteEntity,
    EntityAssetType,
    fetchMatching,
    fetchOne,
    getEntityController,
    StoredAssetDTO
} from "@/core/ABSEntity";
import {EntityTypes} from "@/domain/EntityTypes";
import {DTO} from "@/types/DTOs";
import {Lorebook, LorebookData, LorebookKey} from "@/domain/Lorebook";
import {fetchApi} from "@/services/apiClient";
import {Character, CharacterData, CharacterKey} from "@/domain/Characters";

export type BackgroundFit =
    | "contain"
    | "cover";

export type Position = {
    x: number,
    y: number,
}

export type RegionGeometry = Position & {
    width: number,
    height: number,
}
export type BackgroundGeometry = Position & {
    width: number;
    height: number;
};

export type ImageDimensions = {
    pixelWidth: number;
    pixelHeight: number;
};

export type WorldKey = { id: number }
export type WorldData = {
    name: string;
    description: string;
    lorebook_id: number;

    background_x: number | null;
    background_y: number | null;
    background_width: number | null;
    background_height: number | null;

    background_opacity: number;
    background_visible: boolean;
    background_transform_locked: boolean;
    background_aspect_locked: boolean;
    background_fit: BackgroundFit;
};

export class World extends ABSEntity<WorldKey, WorldData> {
    private static readonly REFERENCE_KEY_ORDER: readonly (keyof WorldKey & string)[] = ['id'] as const;

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

    public async getCharacters() : Promise<Character[]> {
        return fetchMatching<
            CharacterKey,
            CharacterData,
            Character
        > (
            {world_id:this.get('id')},
            EntityTypes.WORLDS,
            Character
        )
    }

    public async getLocations(): Promise<Location[]> {
        return fetchMatching<
            LocationKey,
            LocationData,
            Location
        >(
            {
                worldID: this.get("id"),
            },
            EntityTypes.LOCATIONS,
            Location,
        );
    }

    public async addLocation(
        name: string,
    ): Promise<Location> {
        return createEntity<
            LocationKey,
            LocationData,
            Location
        >(
            {
                worldID: this.get("id"),
            },
            {
                name,
            },
            EntityTypes.LOCATIONS,
            Location,
        );
    }
    public async deleteLocation(
        location: Location,
    ): Promise<boolean> {
        if (
            location.get("worldID") !==
            this.get("id")
        ) {
            throw new Error(
                "Cannot delete a location belonging to another world",
            );
        }

        return deleteEntity<LocationKey>(
            location.key,
            EntityTypes.LOCATIONS,
        );
    }

    public async deleteRegion(
        region: Region,
    ): Promise<boolean> {
        if (
            region.get("world_id") !==
            this.get("id")
        ) {
            throw new Error(
                "Cannot delete a region belonging to another world",
            );
        }

        return deleteEntity<RegionKey>(
            region.key,
            EntityTypes.REGIONS,
        );
    }

    public async createRootRegion(
        name: string,
        geometry: RegionGeometry,
    ): Promise<Region> {
        return createEntity<
            RegionKey,
            RegionData,
            Region
        >(
            {
                world_id: this.get("id"),
            },
            {
                parent_region_id: null,
                name,
                x: geometry.x,
                y: geometry.y,
                width: geometry.width,
                height: geometry.height,
            },
            EntityTypes.REGIONS,
            Region,
        );
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

    public async getAllEdges(): Promise<LocationEdge[]> {
        return fetchMatching<EdgeKey, EdgeData, LocationEdge>(
            {
                world_id: this.get("id"),
            },
            EntityTypes.EDGES,
            LocationEdge,
        );
    }

}

export type RegionKey = {world_id: number, id:number}
export type RegionData = {
    parent_region_id: number | null;
    lorebook_id: number;
    name: string;
    description: string;

    locked:boolean;
    x: number;
    y: number;
    width: number;
    height: number;

    background_opacity: number;
    background_visible: boolean;
    background_aspect_locked: boolean;
    background_fit: BackgroundFit;

    collapsed: boolean;
};
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

    public async createLocation(
        name: string,
        position: Position,
    ): Promise<Location> {
        return createEntity<
            LocationKey,
            LocationData,
            Location
        >(
            {
                worldID: this.get("world_id"),
            },
            {
                name,
                region_id: this.get("id"),
                x: position.x,
                y: position.y,
            },
            EntityTypes.LOCATIONS,
            Location,
        );
    }
    public async createSubRegion(
        name: string,
        initialGeometry: RegionGeometry,
    ): Promise<Region> {
        return createEntity<RegionKey, RegionData, Region>(
            {
                world_id: this.get("world_id"),
            },
            {
                parent_region_id: this.get("id"),
                name:name,
                x: initialGeometry.x,
                y: initialGeometry.y,
                width: initialGeometry.width,
                height: initialGeometry.height,
            },
            EntityTypes.REGIONS,
            Region,
        );
    }
    public async updatePosition(
        position: Position,
    ): Promise<boolean> {
        return this.updateMany({
            x: position.x,
            y: position.y,
        });
    }
    public async updateGeometry(
        geometry: RegionGeometry,
    ): Promise<boolean> {
        return this.updateMany({
            x: geometry.x,
            y: geometry.y,
            width: geometry.width,
            height: geometry.height,
        });
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // BACKGROUND
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * Fetches this region's background image.
     *
     * @return the background Blob, or null when no background is stored
     */
    public async fetchBackground(): Promise<Blob | null> {
        return this.getAsset(EntityAssetType.BACKGROUND);
    }

    /**
     * Uploads or replaces this region's background image.
     *
     * @param background image data to upload
     * @param replace when false, saving fails if a background already exists
     * @return metadata for the stored background
     */
    public async saveBackground(
        background: File | Blob,
        replace = true,
    ): Promise<StoredAssetDTO> {
        return this.postAsset(
            EntityAssetType.BACKGROUND,
            background,
            replace,
        );
    }

    /**
     * Deletes this region's background image.
     *
     * The underlying operation is idempotent.
     */
    public async deleteBackground(): Promise<void> {
        await this.deleteAsset(EntityAssetType.BACKGROUND);
    }
}

export type LocationKey = { worldID: number, id: number }
export type LocationData = {
    name: string,
    lorebook_id: number,
    description: string,
    region_id: number | null,

    locked:boolean,
    x:number,
    y:number,
    radius:number
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

    public async updatePosition(
        position: Position,
    ): Promise<boolean> {
        return this.updateMany({
            x: position.x,
            y: position.y,
        });
    }
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // Characters here
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    public async getStartingHere(): Promise<Character[]>{
        return await fetchApi(
            `api/${EntityTypes.CHARACTERS}/startingAt` +
            `?worldId=${this.get("worldID")}` +
            `&locationId=${this.get("id")}`,
            {
                method: "GET",
                headers: {
                    accept: "content-type/application-json"
                }
            },
        ).then(async response => (await response.json() as DTO[]).map(dto => new Character(dto)))
    }

    public async createCharacterStartingHere(name:string): Promise<Character>{
        return await createEntity<CharacterKey,CharacterData,Character>(
            {world_id:this.get('worldID')},
            {name:name},
            EntityTypes.CHARACTERS,
            Character
        ).then(async response => {
            await response.markStartingAt(this);
            return response;
        })
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // LOCATION ASSETS
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    /**
     * Fetches this location's background image.
     *
     * @return the background Blob, or null when no background is stored
     */
    public async fetchBackground(): Promise<Blob | null> {
        return this.getAsset(EntityAssetType.BACKGROUND);
    }

    /**
     * Uploads or replaces this location's background image.
     *
     * @param background image data to upload
     * @param replace when false, saving fails if a background already exists
     * @return metadata for the stored background
     */
    public async saveBackground(
        background: File | Blob,
        replace = true,
    ): Promise<StoredAssetDTO> {
        return this.postAsset(
            EntityAssetType.BACKGROUND,
            background,
            replace,
        );
    }

    /**
     * Deletes this location's background image.
     *
     * The underlying operation is idempotent.
     */
    public async deleteBackground(): Promise<void> {
        await this.deleteAsset(EntityAssetType.BACKGROUND);
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // LOCATION EDGES
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    /** @return neighbors ( from this location to other or the inverse) */
    public async getNeighbours(): Promise<Location[]>{
        return await fetchApi(
            `api/${EntityTypes.EDGES}/${this.get('worldID')}/${this.get('id')}/neighbours`,
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

    private assertValidEdgeEndpoint(
        other: Location,
    ): void {
        if (
            this.get("worldID") !==
            other.get("worldID")
        ) {
            throw new Error(
                "Locations must belong to the same world",
            );
        }

        if (this.equals(other)) {
            throw new Error(
                "A location cannot connect to itself",
            );
        }
    }

    public async connect(other: Location, initial_data:Partial<EdgeData>): Promise<LocationEdge>{
        this.assertValidEdgeEndpoint(other);
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
        this.assertValidEdgeEndpoint(other);
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
        this.assertValidEdgeEndpoint(other);
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

export class LocationEdge extends ABSEntity<
    EdgeKey,
    EdgeData
> {
    private static readonly REFERENCE_KEY_ORDER: readonly (
        keyof EdgeKey & string
        )[] = [
        "world_id",
        "from_id",
        "to_id",
    ] as const;

    getEntityType(): EntityTypes {
        return EntityTypes.EDGES;
    }

    protected getReferenceKeyOrder(): readonly (
        keyof EdgeKey & string
        )[] {
        return LocationEdge.REFERENCE_KEY_ORDER;
    }
}