package io.github.chechelpo.frplm.domain.world.edge;

import io.github.chechelpo.frplm.core.entities.mappers.ABSWireMapper;
import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.core.dispatch.readers.EntityReaders;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationEdgesRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import io.github.chechelpo.frplm.utils.IO.ZipBuilder;
import io.github.chechelpo.frplm.utils.IO.ZipReader;
import io.github.chechelpo.frplm.utils.orders.NewEdgeOrder;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import static io.github.chechelpo.frplm.jooq.generated.Tables.*;

@Component
final class EdgeMapper extends ABSWireMapper<LocationEdgesRecord, EdgeJSON, NewEdgeOrder> {

    private final EntityReaders readers;

    public EdgeMapper(
            ObjectMapper objectMapper,
            EntityReaders readers
    ) {
        super(objectMapper, EdgeJSON.class, null);
        this.readers = readers;
    }

    @Override
    protected String getZipPath(EdgeJSON json) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected @NonNull EdgeJSON internalRecordFrom(@NonNull LocationEdgesRecord record, ZipBuilder builder) {
        String fromName = getName(record.getWorldId(), record.getFromLocationId());
        String toName = getName(record.getWorldId(), record.getToLocationId());

        return new EdgeJSON(
                getRegionName(record.getWorldId(), record.getFromLocationId()),
                fromName,
                getRegionName(record.getWorldId(), record.getToLocationId()),
                toName,
                record.getEdgedescription(),
                record.getTraversable(),
                record.getShowDestinationName(),
                record.getShowDestinationDescription()
        );
    }

    @Contract("_ -> new")
    @Override
    protected @NonNull NewEdgeOrder internalOrderFrom(@NonNull EdgeJSON json) {
        return new NewEdgeOrder(
                json.fromLocationRegion(),
                json.fromName(),
                json.toLocationRegion(),
                json.toName(),
                EntityDataPayload.<LocationEdgesRecord>builder()
                        .set(LOCATION_EDGES.EDGEDESCRIPTION, json.edge_description())
                        .set(LOCATION_EDGES.TRAVERSABLE, json.is_traversable())
                        .set(LOCATION_EDGES.SHOW_DESTINATION_NAME, json.show_destination_name())
                        .set(LOCATION_EDGES.SHOW_DESTINATION_DESCRIPTION, json.show_destination_description())
                        .build()
        );
    }


    private String getRegionName(int worldId, int locationId) {
        LocationsRecord record = readers.locations().find(EntityKey.<LocationsRecord>builder()
                .set(LOCATIONS.WORLD_ID, worldId)
                .set(LOCATIONS.ID, locationId)
                .build()
        ).orElseThrow("Couldn't find base edge when exporting edge ", Severity.SYSTEM);

        return readers.regions().find(EntityKey.<RegionRecord>builder()
                        .set(REGION.WORLD_ID, worldId)
                        .set(REGION.ID, record.getRegionId())
                        .build()
                ).orElseThrow("Couldn't find parent region when exporting edge ", Severity.SYSTEM)
                .getName();
    }

    private String getName(int worldId, int locationId) {
        return readers.readerFor(LOCATIONS).find(EntityKey.<LocationsRecord>builder()
                        .set(LOCATIONS.WORLD_ID, worldId)
                        .set(LOCATIONS.ID, locationId)
                        .build()
                ).orElseThrow(Severity.SYSTEM)
                .getName();
    }
}
