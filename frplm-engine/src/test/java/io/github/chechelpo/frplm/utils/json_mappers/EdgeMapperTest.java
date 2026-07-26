package io.github.chechelpo.frplm.utils.json_mappers;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityReader;
import io.github.chechelpo.frplm.domain.world.location.LocationsService;
import io.github.chechelpo.frplm.domain.world.region.RegionService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationEdgesRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import io.github.chechelpo.frplm.test_utils.Asserts;
import io.github.chechelpo.frplm.utils.json_mappers.orders.NewEdgeOrder;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;

import static io.github.chechelpo.frplm.jooq.generated.Tables.LOCATION_EDGES;
import static io.github.chechelpo.frplm.jooq.generated.Tables.LOCATIONS;
import static io.github.chechelpo.frplm.jooq.generated.Tables.REGION;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EdgeMapperTest {
    private static final DSLContext dsl = DSL.using(SQLDialect.H2);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private LocationsService locationsService;
    private RegionService regionService;
    private EdgeMapper mapper;

    @BeforeEach
    void setUp(){
        locationsService = mock(LocationsService.class);
        regionService = mock(RegionService.class);
        mapper = new EdgeMapper(locationsService, regionService);
    }

    @Test
    void testRoundTrip(){
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

        when(locationsService.find(EntityKey.<LocationsRecord>builder()
                .set(LOCATIONS.WORLD_ID, worldId)
                .set(LOCATIONS.ID, fromLocationId)
                .build()
        )).thenReturn(EntityReader.RecordFindResult.found(null, fromLocation));

        when(locationsService.find(EntityKey.<LocationsRecord>builder()
                .set(LOCATIONS.WORLD_ID, worldId)
                .set(LOCATIONS.ID, toLocationId)
                .build()
        )).thenReturn(EntityReader.RecordFindResult.found(null, toLocation));

        RegionRecord fromRegion = new RegionRecord();
        fromRegion.set(REGION.NAME, "fromRegion");
        when(regionService.find(EntityKey.<RegionRecord>builder()
                .set(REGION.WORLD_ID, worldId)
                .set(REGION.ID, fromRegionId)
                .build()
        )).thenReturn(EntityReader.RecordFindResult.found(null, fromRegion));

        RegionRecord toRegion = new RegionRecord();
        toRegion.set(REGION.NAME, "toRegion");
        when(regionService.find(EntityKey.<RegionRecord>builder()
                .set(REGION.WORLD_ID, worldId)
                .set(REGION.ID, toRegionId)
                .build()
        )).thenReturn(EntityReader.RecordFindResult.found(null, toRegion));

        NewEdgeOrder edgeOrder = mapper.fromJSON(mapper.fromRecord(edge));

        assertEquals("fromRegion", edgeOrder.fromRegion(), "Mismatch in from region");
        assertEquals("from", edgeOrder.fromName(), "Mismatch in from location");
        assertEquals("toRegion", edgeOrder.toRegion(), "Mismatch in to region");
        assertEquals("to", edgeOrder.toName(), "Mismatch in to location");
        Asserts.assertRecordEqualsPayloadMinusFields(
                edge, edgeOrder.payload(),
                Set.of(
                        LOCATION_EDGES.WORLD_ID,
                        LOCATION_EDGES.FROM_LOCATION_ID,
                        LOCATION_EDGES.TO_LOCATION_ID
                )
        );
    }
}