package io.github.chechelpo.frplm.domain.world.edge;

import io.github.chechelpo.frplm.core.dispatch.readers.EntityReaders;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.core.entities.fields.FieldValidator;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityReader;
import io.github.chechelpo.frplm.domain.world.region.RegionFields;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationEdgesRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import io.github.chechelpo.frplm.test_utils.Asserts;
import io.github.chechelpo.frplm.utils.IO.ZipBuilder;
import io.github.chechelpo.frplm.utils.orders.NewEdgeOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;

import static io.github.chechelpo.frplm.jooq.generated.Tables.LOCATION_EDGES;
import static io.github.chechelpo.frplm.jooq.generated.Tables.LOCATIONS;
import static io.github.chechelpo.frplm.jooq.generated.Tables.REGION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EdgeMapperTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private EntityReaders readers;

    @Mock
    private EntityReader<LocationsRecord> locationReader;

    @Mock
    private EntityReader<LocationsRecord> genericLocationReader;

    @Mock
    private EntityReader<RegionRecord> regionReader;

    private FieldValidator<LocationEdgesRecord> validator = new EdgeFieldsHelper();

    private EdgeMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new EdgeMapper(
                OBJECT_MAPPER,
                readers
        );
    }

    @Test
    void testRoundTrip() {
        int worldId = 2;

        int fromLocationId = 3;
        int toLocationId = 5;

        int fromRegionId = 7;
        int toRegionId = 11;

        LocationEdgesRecord edge = new LocationEdgesRecord();

        edge.set(LOCATION_EDGES.WORLD_ID, worldId);
        edge.set(LOCATION_EDGES.FROM_LOCATION_ID, fromLocationId);
        edge.set(LOCATION_EDGES.TO_LOCATION_ID, toLocationId);

        edge.set(LOCATION_EDGES.EDGEDESCRIPTION, "edge description");
        edge.set(LOCATION_EDGES.TRAVERSABLE, true);
        edge.set(LOCATION_EDGES.SHOW_DESTINATION_NAME, true);
        edge.set(LOCATION_EDGES.SHOW_DESTINATION_DESCRIPTION, false);

        LocationsRecord fromLocation = new LocationsRecord();
        fromLocation.set(LOCATIONS.WORLD_ID, worldId);
        fromLocation.set(LOCATIONS.ID, fromLocationId);
        fromLocation.set(LOCATIONS.REGION_ID, fromRegionId);
        fromLocation.set(LOCATIONS.NAME, "from");

        LocationsRecord toLocation = new LocationsRecord();
        toLocation.set(LOCATIONS.WORLD_ID, worldId);
        toLocation.set(LOCATIONS.ID, toLocationId);
        toLocation.set(LOCATIONS.REGION_ID, toRegionId);
        toLocation.set(LOCATIONS.NAME, "to");

        RegionRecord fromRegion = new RegionRecord();
        fromRegion.set(REGION.WORLD_ID, worldId);
        fromRegion.set(REGION.ID, fromRegionId);
        fromRegion.set(REGION.NAME, "fromRegion");

        RegionRecord toRegion = new RegionRecord();
        toRegion.set(REGION.WORLD_ID, worldId);
        toRegion.set(REGION.ID, toRegionId);
        toRegion.set(REGION.NAME, "toRegion");

        EntityKey<LocationsRecord> fromLocationKey =
                EntityKey.<LocationsRecord>builder()
                        .set(LOCATIONS.WORLD_ID, worldId)
                        .set(LOCATIONS.ID, fromLocationId)
                        .build();

        EntityKey<LocationsRecord> toLocationKey =
                EntityKey.<LocationsRecord>builder()
                        .set(LOCATIONS.WORLD_ID, worldId)
                        .set(LOCATIONS.ID, toLocationId)
                        .build();

        EntityKey<RegionRecord> fromRegionKey =
                EntityKey.<RegionRecord>builder()
                        .set(REGION.WORLD_ID, worldId)
                        .set(REGION.ID, fromRegionId)
                        .build();

        EntityKey<RegionRecord> toRegionKey =
                EntityKey.<RegionRecord>builder()
                        .set(REGION.WORLD_ID, worldId)
                        .set(REGION.ID, toRegionId)
                        .build();

        /*
         * getName() uses:
         *
         * readers.readerFor(LOCATIONS)
         */
        when(readers.readerFor(LOCATIONS))
                .thenReturn(genericLocationReader);

        when(genericLocationReader.find(any(EntityKey.class)))
                .thenReturn(
                        EntityReader.RecordFindResult.found(
                                fromLocationKey,
                                fromLocation
                        ),
                        EntityReader.RecordFindResult.found(
                                toLocationKey,
                                toLocation
                        )
                );

        /*
         * getRegionName() uses:
         *
         * readers.locations()
         */
        when(readers.locations())
                .thenReturn(locationReader);

        when(locationReader.find(any(EntityKey.class)))
                .thenReturn(
                        EntityReader.RecordFindResult.found(
                                fromLocationKey,
                                fromLocation
                        ),
                        EntityReader.RecordFindResult.found(
                                toLocationKey,
                                toLocation
                        )
                );

        /*
         * Then getRegionName() resolves the corresponding regions.
         */
        when(readers.regions())
                .thenReturn(regionReader);

        when(regionReader.find(any(EntityKey.class)))
                .thenReturn(
                        EntityReader.RecordFindResult.found(
                                fromRegionKey,
                                fromRegion
                        ),
                        EntityReader.RecordFindResult.found(
                                toRegionKey,
                                toRegion
                        )
                );

        ZipBuilder zipBuilder = mock(ZipBuilder.class);

        NewEdgeOrder edgeOrder =
                mapper.orderFrom(
                        mapper.jsonRecordFrom(edge, zipBuilder)
                );

        assertEquals(
                "fromRegion",
                edgeOrder.fromRegion(),
                "Mismatch in from region"
        );

        assertEquals(
                "from",
                edgeOrder.fromName(),
                "Mismatch in from location"
        );

        assertEquals(
                "toRegion",
                edgeOrder.toRegion(),
                "Mismatch in to region"
        );

        assertEquals(
                "to",
                edgeOrder.toName(),
                "Mismatch in to location"
        );

        Asserts.assertRecordEqualsPayloadMinusFields(
                edge,
                edgeOrder.payload(),
                Set.of(
                        LOCATION_EDGES.WORLD_ID,
                        LOCATION_EDGES.FROM_LOCATION_ID,
                        LOCATION_EDGES.TO_LOCATION_ID
                )
        );
        validator.validateDataPayload(edgeOrder.payload());
    }
}