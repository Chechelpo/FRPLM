package chechelpo.frplm.core.extras;

import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.domain.character.core.CharacterService;
import chechelpo.frplm.domain.character.starting_locations.StartingLocationsService;
import chechelpo.frplm.domain.lorebook.core.LorebookService;
import chechelpo.frplm.domain.lorebook.entry.core.EntryService;
import chechelpo.frplm.domain.lorebook.entry.keywords.EntryKeywordService;
import chechelpo.frplm.domain.world.core.WorldService;
import chechelpo.frplm.domain.world.edge.EdgeService;
import chechelpo.frplm.domain.world.location.LocationsService;
import chechelpo.frplm.exceptions.Severity;
import chechelpo.frplm.exceptions.runtime.EntityNotFound;
import chechelpo.frplm.jooq.generated.tables.records.*;
import chechelpo.frplm.utils.json_mappers.LorebookMapper;
import chechelpo.frplm.utils.json_mappers.WorldMapper;
import chechelpo.frplm.utils.json_mappers.orders.*;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Map;

import static chechelpo.frplm.jooq.generated.Tables.*;

@Component
class ImporterService {

    private final LorebookMapper lorebookMapper;
    private final LorebookService lorebookService;
    private final EntryService entryService;
    private final EntryKeywordService entryKeywordService;
    private final WorldMapper worldMapper;
    private final WorldService worldService;
    private final LocationsService locationsService;
    private final EdgeService edgeService;
    private final CharacterService characterService;
    private final StartingLocationsService startingLocationsService;

    ImporterService(LorebookMapper lorebookMapper, LorebookService lorebookService, EntryService entryService, EntryKeywordService entryKeywordService, WorldMapper worldMapper, WorldService worldService, LocationsService locationsService, EdgeService edgeService, CharacterService characterService, StartingLocationsService startingLocationsService) {
        this.lorebookMapper = lorebookMapper;
        this.lorebookService = lorebookService;
        this.entryService = entryService;
        this.entryKeywordService = entryKeywordService;
        this.worldMapper = worldMapper;
        this.worldService = worldService;
        this.locationsService = locationsService;
        this.edgeService = edgeService;
        this.characterService = characterService;
        this.startingLocationsService = startingLocationsService;
    }

    public boolean importLorebook(JsonNode node){
        NewLorebookOrder order = lorebookMapper.orderFrom(node);
        execute(order);
        return true;
    }
    private @NonNull LorebooksRecord execute(@NonNull NewLorebookOrder order){
        LorebooksRecord record = lorebookService.createAndGet(order.entityPayload());
        int lorebookId = record.getId();

        for (NewEntryOrder entryOrder : order.entries()){
            entryOrder.entryInfo().set(ENTRY.LOREBOOK_ID, lorebookId);
            EntryRecord entryRecord = entryService.createAndGet(entryOrder.entryInfo());
            for (String keyword : entryOrder.keywords())
                entryKeywordService.associate(lorebookId, entryRecord.getEntryId(), keyword);
        }

        return record;
    }

    public WorldsRecord importWorld(JsonNode file){
        NewWorldOrder worldOrder = worldMapper.orderFrom(file);
        LorebooksRecord worldLorebook = execute(worldOrder.lorebook());
        worldOrder.dataPayload().set(WORLDS.LOREBOOK_ID, worldLorebook.getId());

        WorldsRecord world = worldService.createAndGet(worldOrder.dataPayload());

        int worldId = world.getId();
        Map<String, Integer> lorebookIdByName = new HashMap<>(worldOrder.locations().size());
        for (NewLocationOrder locationOrder : worldOrder.locations()){
            LorebooksRecord locationLorebook = execute(locationOrder.lorebookOrder());

            locationOrder.payload().set(LOCATIONS.WORLD_ID, worldId);
            locationOrder.payload().set(LOCATIONS.LOREBOOK_ID, locationLorebook.getId());

            LocationsRecord newLocation = locationsService.createAndGet(locationOrder.payload());
            lorebookIdByName.put(newLocation.getName(), newLocation.getId());

            for (NewCharacterOrder newCharacterOrder : locationOrder.charactersStartingHere()){
                LorebooksRecord characterLorebook = execute(newCharacterOrder.lorebook());
                newCharacterOrder.info().set(CHARACTERS.LOREBOOK_ID, characterLorebook.getId());
                CharactersRecord characterRecord = characterService.createAndGet(newCharacterOrder.info());
                startingLocationsService.createAndGet(EntityDataPayload.<StartingLocationsRecord>builder()
                                .set(STARTING_LOCATIONS.WORLD_ID, worldId)
                                .set(STARTING_LOCATIONS.LOCATION_ID, newLocation.getId())
                                .set(STARTING_LOCATIONS.CHARACTER_ID, characterRecord.getId())
                                .build()
                );
            }
        }

        for (NewEdgeOrder edgeOrder : worldOrder.locationEdges()){
            int fromId = lorebookIdByName.get(edgeOrder.fromName());
            int toId = lorebookIdByName.get(edgeOrder.toName());
            edgeService.createAndGet(
                    EntityDataPayload.<LocationNeighborsRecord>builder()
                            .set(LOCATION_NEIGHBORS.WORLD_ID, worldId)
                            .set(LOCATION_NEIGHBORS.LOCATION1_ID, fromId)
                            .set(LOCATION_NEIGHBORS.LOCATION2_ID, toId)
                            .set(LOCATION_NEIGHBORS.EDGEDESCRIPTION, edgeOrder.description())
                            .set(LOCATION_NEIGHBORS.TRAVELCOST, edgeOrder.travel_cost())
                            .build()
            );
        }

        return world;
    }

    public JsonNode exportWorld(int worldId){
        return worldMapper.jsonFrom(
                worldService.find(EntityKey.of(WORLDS.ID, worldId))
                        .orElseThrow(() -> new EntityNotFound("No world with id " + worldId, Severity.USER))
        );
    }
}
