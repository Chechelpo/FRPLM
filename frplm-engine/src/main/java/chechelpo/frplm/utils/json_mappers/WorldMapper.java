package chechelpo.frplm.utils.json_mappers;

import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.domain.lorebook.core.LorebookService;
import chechelpo.frplm.domain.world.edge.EdgeService;
import chechelpo.frplm.domain.world.location.LocationsService;
import chechelpo.frplm.exceptions.Severity;
import chechelpo.frplm.exceptions.runtime.UnexpectedException;
import chechelpo.frplm.jooq.generated.tables.records.LocationNeighborsRecord;
import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import chechelpo.frplm.utils.json_mappers.orders.NewEdgeOrder;
import chechelpo.frplm.utils.json_mappers.orders.NewWorldOrder;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

import static chechelpo.frplm.jooq.generated.Tables.*;

@Component
public class WorldMapper {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final LocationsService locationsService;
    private final LocationMapper locationMapper;
    private final LorebookService lorebookService;
    private final LorebookMapper lorebookExporter;
    private final EdgeService edgeService;

    public WorldMapper(LocationsService locationsService, LocationMapper locationExporter, LorebookService lorebookService, LorebookMapper lorebookExporter, EdgeService edgeService) {
        this.locationsService = locationsService;
        this.locationMapper = locationExporter;
        this.lorebookService = lorebookService;
        this.lorebookExporter = lorebookExporter;
        this.edgeService = edgeService;
    }

    private record WorldJson(
            String name,
            JsonNode lorebook,
            List<JsonNode> locations,
            List<NewEdgeOrder> neighbours
    ) {}

    public JsonNode jsonFrom(@NonNull WorldsRecord record) {
        List<LocationsRecord> locationsOfWorld = locationsService.getMatching(
                EntityKey.of(LOCATIONS.WORLD_ID, record.getId())
        );
        return MAPPER.valueToTree(new WorldJson(
                record.getName(),
                lorebookFrom(record),
                locationsOfWorld.stream()
                        .map(locationMapper::jsonFrom)
                        .toList(),
                fetchNeighbours(locationsOfWorld)
        ));
    }

    List<NewEdgeOrder> fetchNeighbours(@NonNull List<LocationsRecord> records) {
        List<NewEdgeOrder> result = new ArrayList<>(records.size());

        for (LocationsRecord record : records){
            List<LocationsRecord> neighbours = edgeService.getNeighboursOf(record);

            for (LocationsRecord neighbour : neighbours){
                LocationNeighborsRecord edgeInfo = edgeService.find(EntityKey.<LocationNeighborsRecord>builder()
                                .set(LOCATION_NEIGHBORS.WORLD_ID, record.getWorldId())
                                .set(LOCATION_NEIGHBORS.LOCATION1_ID, record.getId())
                                .set(LOCATION_NEIGHBORS.LOCATION2_ID, neighbour.getId())
                                .build()
                ).orElseThrow(() -> new UnexpectedException("Couldn't find edge", Severity.SYSTEM));
                result.add(new NewEdgeOrder(
                        record.getName(),
                        neighbour.getName(),
                        edgeInfo.getEdgedescription(),
                        edgeInfo.getTravelcost()
                ));
            }
        }

        return result;
    }

    JsonNode lorebookFrom(WorldsRecord record) {
        return lorebookExporter.jsonFrom(lorebookService.getLorebookOf(record));
    }


    public NewWorldOrder orderFrom(JsonNode node){
        WorldJson json = MAPPER.treeToValue(node, WorldJson.class);

        return new NewWorldOrder(
                EntityDataPayload.<WorldsRecord>builder()
                        .set(WORLDS.NAME, json.name)
                        .build(),
                lorebookExporter.orderFrom(json.lorebook),
                json.locations.stream()
                        .map(locationMapper::orderFrom)
                        .toList(),
                json.neighbours
        );
    }
}
