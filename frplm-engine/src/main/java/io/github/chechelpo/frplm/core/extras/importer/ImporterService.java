package io.github.chechelpo.frplm.core.extras.importer;

import io.github.chechelpo.frplm.core.entities.assets.EntityAssetStore;
import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.domain.world.core.WorldJSON;
import io.github.chechelpo.frplm.domain.world.core.WorldMapper;
import io.github.chechelpo.frplm.domain.character.core.CharacterService;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookService;
import io.github.chechelpo.frplm.domain.lorebook.entry.core.EntryService;
import io.github.chechelpo.frplm.domain.lorebook.entry.keywords.EntryKeywordService;
import io.github.chechelpo.frplm.domain.world.core.WorldService;
import io.github.chechelpo.frplm.domain.world.edge.EdgeService;
import io.github.chechelpo.frplm.domain.world.location.LocationsService;
import io.github.chechelpo.frplm.domain.world.region.RegionService;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.extensions.api.standalone.RegionSnapshot;
import io.github.chechelpo.frplm.extensions.api.standalone.WorldSnapshot;
import io.github.chechelpo.frplm.jooq.generated.tables.records.*;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookMapper;
import io.github.chechelpo.frplm.utils.IO.ZipReader;
import io.github.chechelpo.frplm.utils.orders.*;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
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
    private final RegionService regionService;
    private final ObjectMapper objectMapper;
    private final EntityAssetStore<WorldsRecord, WorldSnapshot.Reference> worldAssetStore;
    private final EntityAssetStore<RegionRecord, RegionSnapshot.Reference> regionAssetStore;

    ImporterService(
            LorebookMapper lorebookMapper,
            LorebookService lorebookService,
            EntryService entryService,
            EntryKeywordService entryKeywordService,
            WorldMapper worldMapper,
            WorldService worldService,
            LocationsService locationsService,
            EdgeService edgeService,
            CharacterService characterService,
            RegionService regionService,
            ObjectMapper objectMapper, @Qualifier("worldAssetStore") EntityAssetStore<WorldsRecord, WorldSnapshot.Reference> worldAssetStore,
            @Qualifier("regionAssetStore") EntityAssetStore<RegionRecord, RegionSnapshot.Reference> entityAssetStore
    ) {
        this.lorebookMapper = lorebookMapper;
        this.lorebookService = lorebookService;
        this.entryService = entryService;
        this.entryKeywordService = entryKeywordService;
        this.worldMapper = worldMapper;
        this.worldService = worldService;
        this.locationsService = locationsService;
        this.edgeService = edgeService;
        this.characterService = characterService;
        this.regionService = regionService;
        this.objectMapper = objectMapper;
        this.worldAssetStore = worldAssetStore;
        this.regionAssetStore = entityAssetStore;
    }

    public boolean importLorebook(JsonNode node) {
        throw new UnsupportedOperationException();
        /*NewLorebookOrder order = lorebookMapper.internalOrderFrom(node);
        executeLorebook(order);
        return true;*/
    }

    public WorldsRecord importWorld(InputStream inputStream) throws IOException {
        try (ZipReader zipReader = ZipReader.open(inputStream, objectMapper)){
            NewWorldOrder order = worldMapper.orderFrom(zipReader.readJson("data.json", WorldJSON.class));
            return importWorld(order, zipReader);
        }
    }

    public WorldsRecord importWorld(NewWorldOrder worldOrder, ZipReader reader) {
        LorebooksRecord worldLorebook = executeLorebook(worldOrder.lorebook());
        worldOrder.payload().set(WORLDS.LOREBOOK_ID, worldLorebook.getId());

        WorldsRecord world = worldService.consume(worldOrder);
        worldAssetStore.storeAssetFromCreationOrder(worldOrder, world, reader);
        int worldId = world.getId();

        executeRegions(worldOrder.regions(), worldId, reader);
        executeLocationsAndCharacters(worldOrder.locations(), worldId);
        executeEdges(worldOrder.locationEdges(), worldId);

        return world;
    }

    @NonNull LorebooksRecord executeLorebook(@NonNull NewLorebookOrder order) {
        LorebooksRecord record = lorebookService.consume(order);
        int lorebookId = record.getId();

        for (NewEntryOrder entryOrder : order.entries()) {
            executeEntry(entryOrder, lorebookId);
        }

        return record;
    }

    void executeEntry(NewEntryOrder entryOrder, int lorebookId) {
        entryOrder.payload().set(ENTRY.LOREBOOK_ID, lorebookId);
        EntryRecord entryRecord = entryService.createAndGet(entryOrder.payload());
        for (String keyword : entryOrder.keywords())
            entryKeywordService.associate(lorebookId, entryRecord.getEntryId(), keyword);
    }

    @Contract(mutates = "param1")
    void executeRegions(@NonNull List<NewRegionOrder> regionOrders, int worldId, ZipReader reader) {
        Map<String, RegionRecord> regionsByName = new HashMap<>(regionOrders.size());

        List<RegionRecord> regionsWithParents = new ArrayList<>(regionOrders.size());
        for (NewRegionOrder regionOrder : regionOrders) {
            LorebooksRecord regionLorebook = executeLorebook(regionOrder.lorebookOrder());

            regionOrder.payload()
                    .set(REGION.WORLD_ID, worldId)
                    .set(REGION.LOREBOOK_ID, regionLorebook.getId());
            RegionRecord record = regionService.consume(regionOrder);

            regionAssetStore.storeAssetFromCreationOrder(regionOrder, record, reader);
            regionsByName.put(record.getName(), record);
            if (regionOrder.parentRegionName() != null) regionsWithParents.add(record);
        }

        int i = 0;
        int j = 0;
        for (NewRegionOrder order : regionOrders) {
            if (order.parentRegionName() == null) {
                i++;
                continue;
            }

            RegionRecord parentRegion = regionsByName.get(order.parentRegionName());
            if (parentRegion == null) throw new IllegalArgumentException("Null parent region");
            RegionRecord child = regionsWithParents.get(j);

            if (!Objects.equals(child.getName(), order.payload().require(REGION.NAME)))
                throw new IllegalStateException("The names don't match, the assertion failed");

            regionService.update(
                    regionService.keyOf(child),
                    EntityDataPayload.of(REGION.PARENT_REGION_ID, parentRegion.getId())
            ).orElseThrow();

            i++;
            j++;
        }
    }

    void executeLocationsAndCharacters(
            @NonNull List<NewLocationOrder> locationOrders,
            int worldId
    ) {
        for (NewLocationOrder locationOrder : locationOrders) {
            LorebooksRecord locationLorebook = executeLorebook(locationOrder.lorebookOrder());

            locationOrder.payload()
                    .set(LOCATIONS.WORLD_ID, worldId)
                    .set(LOCATIONS.LOREBOOK_ID, locationLorebook.getId());

            if (locationOrder.parentRegionName() != null) {
                int regionId = regionService.getOneMatching(
                        EntityDataPayload.<RegionRecord>builder()
                                .set(REGION.NAME, locationOrder.parentRegionName())
                                .set(REGION.WORLD_ID, worldId)
                                .build()
                ).resolve().getId();
                locationOrder.payload().set(LOCATIONS.REGION_ID, regionId);
            }

            LocationsRecord newLocation = locationsService.consume(locationOrder);

            if (locationOrder.charactersStartingHere() == null) continue;
            for (NewCharacterOrder newCharacterOrder : locationOrder.charactersStartingHere()) {
                executeCharacter(newCharacterOrder, worldId, newLocation.getId());
            }
        }

    }

    @NonNull CharactersRecord executeCharacter(NewCharacterOrder newCharacterOrder, int worldId, int locationId) {
        LorebooksRecord characterLorebook = executeLorebook(newCharacterOrder.lorebook());
        newCharacterOrder.payload()
                .set(CHARACTERS.WORLD_ID, worldId)
                .set(CHARACTERS.STARTING_LOCATION_ID, locationId)
                .set(CHARACTERS.LOREBOOK_ID, characterLorebook.getId());

        return characterService.consume(newCharacterOrder);
    }

    void executeEdges(@NonNull List<NewEdgeOrder> locationEdges, int worldId) {
        for (NewEdgeOrder edgeOrder : locationEdges) {
            LocationsRecord fromLocation = getOneLocationByName(worldId, edgeOrder.fromRegion(), edgeOrder.fromName());
            LocationsRecord toLocation = getOneLocationByName(worldId, edgeOrder.toRegion(), edgeOrder.toName());

            edgeOrder.payload().set(LOCATION_EDGES.WORLD_ID, worldId);
            edgeOrder.payload().set(LOCATION_EDGES.FROM_LOCATION_ID, fromLocation.getId());
            edgeOrder.payload().set(LOCATION_EDGES.TO_LOCATION_ID, toLocation.getId());

            edgeService.createAndGet(edgeOrder.payload());
        }
    }

    private @NonNull LocationsRecord getOneLocationByName(int worldId, String regionName, String locationName) {
        RegionRecord regionRecord = regionService.getOneMatching(EntityDataPayload.<RegionRecord>builder()
                        .set(REGION.WORLD_ID, worldId)
                        .set(REGION.NAME, regionName)
                        .build()
                )
                .ifEmptyThrow(noMatch -> new EntityNotFound(
                        "Could not find region with name " + regionName + " when importing " + locationName,
                        Severity.SYSTEM)
                )
                .resolve();

        return locationsService.getOneMatching(EntityDataPayload.<LocationsRecord>builder()
                        .set(LOCATIONS.WORLD_ID, worldId)
                        .set(LOCATIONS.REGION_ID, regionRecord.getId())
                        .set(LOCATIONS.NAME, locationName)
                        .build()
                ).ifEmptyThrow(noMatch -> new EntityNotFound(
                        "Could not find location with data \n%s\n with region \n%s\n when importing".formatted(noMatch, regionRecord),
                        Severity.SYSTEM
                ))
                .resolve();
    }


    public JsonNode exportWorld(int worldId) {
        return null;
    }
}
