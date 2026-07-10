package chechelpo.frplm.utils.json_mappers;

import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.domain.world.location.LocationsService;
import chechelpo.frplm.exceptions.Severity;
import chechelpo.frplm.exceptions.runtime.EntityNotFound;
import chechelpo.frplm.jooq.generated.tables.records.LocationEdgesRecord;
import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import chechelpo.frplm.utils.json_mappers.orders.NewEdgeOrder;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Objects;

import static chechelpo.frplm.jooq.generated.Tables.LOCATIONS;
import static chechelpo.frplm.jooq.generated.Tables.LOCATION_EDGES;

@Component
public class EdgeMapper {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final LocationsService locationsService;

    public EdgeMapper(LocationsService locationsService) {
        this.locationsService = locationsService;
    }

    private record EdgeJSON(
            String fromName,
            String toName,
            String edge_description,
            boolean is_traversable,
            boolean show_destination_name,
            boolean show_destination_description
    ){}

    public JsonNode fromRecord(LocationEdgesRecord record){
        Objects.requireNonNull(record, "Edge is null");
        String fromName = getName(record.getWorldId(), record.getFromLocationId());
        String toName = getName(record.getWorldId(), record.getToLocationId());

        return OBJECT_MAPPER.valueToTree(new EdgeJSON(
                fromName,
                toName,
                record.getEdgedescription(),
                record.getTraversable(),
                record.getShowDestinationName(),
                record.getShowDestinationDescription()
        ));
    }
    private String getName(int worldId, int locationId) {
        return locationsService.find(EntityKey.<LocationsRecord>builder()
                        .set(LOCATIONS.WORLD_ID, worldId)
                        .set(LOCATIONS.ID, locationId)
                        .build()
                ).orElseThrow(() -> new EntityNotFound("Location with id " + locationId, Severity.SYSTEM))
                .getName();
    }

    public NewEdgeOrder fromJSON(JsonNode file){
        Objects.requireNonNull(file, "Json is null");
        EdgeJSON json = OBJECT_MAPPER.treeToValue(file, EdgeJSON.class);

        return new NewEdgeOrder(
                json.fromName,
                json.toName,
                EntityDataPayload.<LocationEdgesRecord>builder()
                        .set(LOCATION_EDGES.EDGEDESCRIPTION, json.edge_description)
                        .set(LOCATION_EDGES.TRAVERSABLE, json.is_traversable)
                        .set(LOCATION_EDGES.SHOW_DESTINATION_NAME, json.show_destination_name)
                        .set(LOCATION_EDGES.SHOW_DESTINATION_DESCRIPTION, json.show_destination_description)
                        .build()
        );
    }
}
