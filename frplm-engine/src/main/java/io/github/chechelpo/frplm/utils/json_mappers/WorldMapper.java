package io.github.chechelpo.frplm.utils.json_mappers;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookService;
import io.github.chechelpo.frplm.domain.world.edge.EdgeService;
import io.github.chechelpo.frplm.domain.world.location.LocationsService;
import io.github.chechelpo.frplm.domain.world.region.RegionService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import io.github.chechelpo.frplm.utils.json_mappers.orders.NewWorldOrder;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.*;

@Component
public class WorldMapper {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final LocationsService locationsService;
    private final LocationMapper locationMapper;
    private final LorebookService lorebookService;
    private final LorebookMapper lorebookExporter;
    private final EdgeService edgeService;
    private final RegionService regionService;
    private final RegionMapper regionMapper;
    private final EdgeMapper edgeMapper;

    public WorldMapper(LocationsService locationsService, LocationMapper locationExporter, LorebookService lorebookService, LorebookMapper lorebookExporter, EdgeService edgeService, RegionService regionService, RegionMapper regionMapper, EdgeMapper edgeMapper) {
        this.locationsService = locationsService;
        this.locationMapper = locationExporter;
        this.lorebookService = lorebookService;
        this.lorebookExporter = lorebookExporter;
        this.edgeService = edgeService;
        this.regionService = regionService;
        this.regionMapper = regionMapper;
        this.edgeMapper = edgeMapper;
    }

    private record WorldJson(
            String name,
            JsonNode lorebook,
            List<JsonNode> regions,
            List<JsonNode> locations,
            List<JsonNode> edges
    ) {}

    public JsonNode jsonFrom(@NonNull WorldsRecord record) {
        List<LocationsRecord> locationsOfWorld = locationsService.getMatching(
                EntityKey.of(LOCATIONS.WORLD_ID, record.getId())
        );
        return MAPPER.valueToTree(new WorldJson(
                record.getName(),
                lorebookFrom(record),
                regionService.getMatching(EntityKey.of(REGION.WORLD_ID, record.getId())).stream()
                        .map(regionMapper::toJson)
                        .toList(),
                locationsOfWorld.stream()
                        .map(locationMapper::jsonFrom)
                        .toList(),
                edgeService.getMatching(EntityKey.of(LOCATION_EDGES.WORLD_ID, record.getId())).stream()
                        .map(edgeMapper::fromRecord)
                        .toList()
        ));
    }

    JsonNode lorebookFrom(WorldsRecord record) {
        return lorebookExporter.jsonFrom(lorebookService.getLorebookOf(record));
    }

    public NewWorldOrder orderFrom(JsonNode node) {
        WorldJson json = MAPPER.treeToValue(node, WorldJson.class);

        return new NewWorldOrder(
                EntityDataPayload.<WorldsRecord>builder()
                        .set(WORLDS.NAME, json.name)
                        .build(),
                lorebookExporter.orderFrom(json.lorebook),
                json.locations.stream()
                        .map(locationMapper::orderFrom)
                        .toList(),
                json.regions.stream()
                        .map(regionMapper::fromJson)
                        .toList(),
                json.edges.stream()
                        .map(edgeMapper::fromJSON)
                        .toList()
        );
    }
}
