package io.github.chechelpo.frplm.utils.json_mappers;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookService;
import io.github.chechelpo.frplm.domain.world.edge.EdgeService;
import io.github.chechelpo.frplm.domain.world.location.LocationsService;
import io.github.chechelpo.frplm.domain.world.region.RegionService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import io.github.chechelpo.frplm.test_utils.Asserts;
import io.github.chechelpo.frplm.utils.json_mappers.orders.NewWorldOrder;
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
import static io.github.chechelpo.frplm.jooq.generated.Tables.WORLDS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorldMapperTest {
    private static final DSLContext dsl = DSL.using(SQLDialect.H2);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private LocationsService locationsService;
    private LocationMapper locationMapper;
    private LorebookService lorebookService;
    private LorebookMapper lorebookMapper;
    private EdgeService edgeService;
    private RegionService regionService;
    private RegionMapper regionMapper;
    private EdgeMapper edgeMapper;
    private WorldMapper mapper;

    @BeforeEach
    void setUp(){
        locationsService = mock(LocationsService.class);
        locationMapper = mock(LocationMapper.class);
        lorebookService = mock(LorebookService.class);
        lorebookMapper = mock(LorebookMapper.class);
        edgeService = mock(EdgeService.class);
        regionService = mock(RegionService.class);
        regionMapper = mock(RegionMapper.class);
        edgeMapper = mock(EdgeMapper.class);
        mapper = new WorldMapper(
                locationsService, locationMapper,
                lorebookService, lorebookMapper,
                edgeService, regionService, regionMapper, edgeMapper
        );
    }

    @Test
    void testRoundTrip(){
        int worldId = 1;
        WorldsRecord world = new WorldsRecord();
        world.set(WORLDS.ID, worldId);
        world.set(WORLDS.NAME, "world");
        world.set(WORLDS.DESCRIPTION, "description");
        world.set(WORLDS.LOREBOOK_ID, 5);

        when(locationsService.getMatching(any(EntityKey.class)))
                .thenReturn(dsl.newResult(LOCATIONS));
        when(regionService.getMatching(any(EntityKey.class)))
                .thenReturn(dsl.newResult(REGION));
        when(edgeService.getMatching(any(EntityKey.class)))
                .thenReturn(dsl.newResult(LOCATION_EDGES));

        LorebooksRecord lorebooksRecord = new LorebooksRecord();
        when(lorebookService.getLorebookOf(world)).thenReturn(lorebooksRecord);
        when(lorebookMapper.jsonFrom(lorebooksRecord)).thenReturn(OBJECT_MAPPER.nullNode());

        NewWorldOrder worldOrder = mapper.orderFrom(mapper.jsonFrom(world));

        assertTrue(worldOrder.locations().isEmpty(), "Locations should be empty");
        assertTrue(worldOrder.regions().isEmpty(), "Regions should be empty");
        assertTrue(worldOrder.locationEdges().isEmpty(), "Edges should be empty");
        Asserts.assertRecordEqualsPayloadMinusFields(
                world, worldOrder.dataPayload(),
                Set.of(
                        WORLDS.ID,
                        WORLDS.LOREBOOK_ID,
                        WORLDS.NEXT_LOCATION_ID,
                        WORLDS.NEXT_REGION_ID
                )
        );
    }
}