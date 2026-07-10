package io.github.chechelpo.frplm.core.extras;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.domain.character.core.CharacterService;
import io.github.chechelpo.frplm.domain.character.starting_locations.StartingLocationsService;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookService;
import io.github.chechelpo.frplm.domain.lorebook.entry.core.EntryService;
import io.github.chechelpo.frplm.domain.lorebook.entry.keywords.EntryKeywordService;
import io.github.chechelpo.frplm.domain.world.core.WorldService;
import io.github.chechelpo.frplm.domain.world.edge.EdgeService;
import io.github.chechelpo.frplm.domain.world.location.LocationsService;
import io.github.chechelpo.frplm.domain.world.region.RegionService;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.jooq.generated.tables.records.*;
import io.github.chechelpo.frplm.utils.json_mappers.LorebookMapper;
import io.github.chechelpo.frplm.utils.json_mappers.WorldMapper;
import io.github.chechelpo.frplm.utils.json_mappers.orders.*;
import io.github.chechelpo.frplm.utils.json_mappers.orders.*;
import io.github.chechelpo.frplm.utils.json_mappers.orders.*;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.*;

import static io.github.chechelpo.frplm.jooq.generated.Tables.*;

@Component
final class ImporterService {

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
    private final RegionService regionService;

    ImporterService(LorebookMapper lorebookMapper, LorebookService lorebookService, EntryService entryService, EntryKeywordService entryKeywordService, WorldMapper worldMapper, WorldService worldService, LocationsService locationsService, EdgeService edgeService, CharacterService characterService, StartingLocationsService startingLocationsService, RegionService regionService) {
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
        this.regionService = regionService;
    }

    public boolean importLorebook(JsonNode node) {
        NewLorebookOrder order = lorebookMapper.orderFrom(node);
        executeLorebook(order);
        return true;
    }

    public WorldsRecord importWorld(JsonNode file) {
        NewWorldOrder worldOrder = worldMapper.orderFrom(file);
        LorebooksRecord worldLorebook = executeLorebook(worldOrder.lorebook());
        worldOrder.dataPayload().set(WORLDS.LOREBOOK_ID, worldLorebook.getId());

        WorldsRecord world = worldService.createAndGet(worldOrder.dataPayload());
        int worldId = world.getId();

        Map<String, RegionRecord> regionsByName = executeRegions(worldOrder.regions(), worldId);
        Map<String, Integer> locationIdByName = executeLocationsAndCharacters(worldOrder.locations(), regionsByName, worldId);
        executeEdges(worldOrder.locationEdges(), locationIdByName, worldId);

        return world;
    }

    @NonNull LorebooksRecord executeLorebook(@NonNull NewLorebookOrder order) {
        LorebooksRecord record = lorebookService.createAndGet(order.entityPayload());
        int lorebookId = record.getId();

        for (NewEntryOrder entryOrder : order.entries()) {
            executeEntry(entryOrder, lorebookId);
        }

        return record;
    }

    void executeEntry(NewEntryOrder entryOrder, int lorebookId) {
        entryOrder.entryInfo().set(ENTRY.LOREBOOK_ID, lorebookId);
        EntryRecord entryRecord = entryService.createAndGet(entryOrder.entryInfo());
        for (String keyword : entryOrder.keywords())
            entryKeywordService.associate(lorebookId, entryRecord.getEntryId(), keyword);
    }

    @Contract(mutates = "param1")
    @NonNull Map<String, RegionRecord> executeRegions(@NonNull List<NewRegionOrder> regionOrders, int worldId) {
        Map<String, RegionRecord> regionsByName = new HashMap<>(regionOrders.size());

        List<RegionRecord> regionsWithParents = new ArrayList<>(regionOrders.size());
        for (NewRegionOrder regionOrder : regionOrders) {
            LorebooksRecord regionLorebook = executeLorebook(regionOrder.lorebookOrder());

            regionOrder.payload().set(REGION.WORLD_ID, worldId);
            regionOrder.payload().set(REGION.LOREBOOK_ID, regionLorebook.getId());
            RegionRecord record = regionService.createAndGet(regionOrder.payload());

            regionsByName.put(record.getName(), record);
            if (regionOrder.parentRegionName() != null) regionsWithParents.add(record);
        }

        int i = 0;
        int j = 0;
        for (NewRegionOrder order : regionOrders){
            if (order.parentRegionName() == null) {
                i++;
                continue;
            }

            RegionRecord parentRegion = regionsByName.get(order.parentRegionName());
            if (parentRegion == null) throw new IllegalArgumentException("Null parent region");
            RegionRecord child = regionsWithParents.get(j);

            if (!Objects.equals(child.getName(), order.payload().requireValue(REGION.NAME)))
                throw new IllegalStateException("The names don't match, the assertion failed");

            regionService.update(
                    regionService.keyOf(child),
                    EntityDataPayload.of(REGION.PARENT_REGION_ID, parentRegion.getId())
            );

            i++;
            j++;
        }

        return regionsByName;
    }

    @NonNull Map<String, Integer> executeLocationsAndCharacters(
            @NonNull List<NewLocationOrder> locationOrders,
            Map<String, RegionRecord> regions,
            int worldId
    ){
        Map<String, Integer> locationIdByName = new HashMap<>(locationOrders.size());

        for (NewLocationOrder locationOrder : locationOrders) {
            LorebooksRecord locationLorebook = executeLorebook(locationOrder.lorebookOrder());

            locationOrder.payload().set(LOCATIONS.WORLD_ID, worldId);
            locationOrder.payload().set(LOCATIONS.LOREBOOK_ID, locationLorebook.getId());
            if (locationOrder.parentRegionName() != null){
                locationOrder.payload().set(LOCATIONS.REGION_ID, regions.get(locationOrder.parentRegionName()).getId());
            }

            LocationsRecord newLocation = locationsService.createAndGet(locationOrder.payload());
            locationIdByName.put(newLocation.getName(), newLocation.getId());

            if (locationOrder.charactersStartingHere() == null) continue;
            for (NewCharacterOrder newCharacterOrder : locationOrder.charactersStartingHere()) {
                CharactersRecord characterRecord = executeCharacter(newCharacterOrder);
                startingLocationsService.createAndGet(EntityDataPayload.<StartingLocationsRecord>builder()
                        .set(STARTING_LOCATIONS.WORLD_ID, worldId)
                        .set(STARTING_LOCATIONS.LOCATION_ID, newLocation.getId())
                        .set(STARTING_LOCATIONS.CHARACTER_ID, characterRecord.getId())
                        .build()
                );
            }
        }

        return locationIdByName;
    }

    @NonNull CharactersRecord executeCharacter(NewCharacterOrder newCharacterOrder) {
        LorebooksRecord characterLorebook = executeLorebook(newCharacterOrder.lorebook());
        newCharacterOrder.info().set(CHARACTERS.LOREBOOK_ID, characterLorebook.getId());
        return characterService.createAndGet(newCharacterOrder.info());
    }

    void executeEdges(@NonNull List<NewEdgeOrder> locationEdges, Map<String, Integer> locationIdByName, int worldId) {
        for (NewEdgeOrder edgeOrder : locationEdges) {
            int fromId = locationIdByName.get(edgeOrder.fromName());
            int toId = locationIdByName.get(edgeOrder.toName());

            edgeOrder.payload().set(LOCATION_EDGES.WORLD_ID, worldId);
            edgeOrder.payload().set(LOCATION_EDGES.FROM_LOCATION_ID, fromId);
            edgeOrder.payload().set(LOCATION_EDGES.TO_LOCATION_ID, toId);

            edgeService.createAndGet(edgeOrder.payload());
        }
    }

    public JsonNode exportWorld(int worldId) {
        return worldMapper.jsonFrom(
                worldService.find(EntityKey.of(WORLDS.ID, worldId))
                        .orElseThrow(() -> new EntityNotFound("No world with id " + worldId, Severity.USER))
        );
    }
}
