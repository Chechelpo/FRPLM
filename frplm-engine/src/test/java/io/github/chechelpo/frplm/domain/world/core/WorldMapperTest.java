package io.github.chechelpo.frplm.domain.world.core;

import io.github.chechelpo.frplm.core.dispatch.readers.EntityReaders;
import io.github.chechelpo.frplm.core.entities.assets.EntityAssetStore;
import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.fields.FieldActionResult;
import io.github.chechelpo.frplm.core.entities.fields.FieldValidator;
import io.github.chechelpo.frplm.core.entities.mappers.ABSWireMapper;
import io.github.chechelpo.frplm.core.entities.mappers.EntityWireMapper;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityReader;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookJSON;
import io.github.chechelpo.frplm.domain.world.edge.EdgeJSON;
import io.github.chechelpo.frplm.domain.world.location.LocationJSON;
import io.github.chechelpo.frplm.domain.world.region.RegionJSON;
import io.github.chechelpo.frplm.extensions.api.standalone.WorldSnapshot;
import io.github.chechelpo.frplm.jooq.generated.tables.records.*;
import io.github.chechelpo.frplm.test_utils.Asserts;
import io.github.chechelpo.frplm.utils.IO.ZipBuilder;
import io.github.chechelpo.frplm.utils.orders.*;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Set;

import static io.github.chechelpo.frplm.jooq.generated.Tables.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorldMapperTest {

    private static final DSLContext dsl = DSL.using(SQLDialect.H2);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final FieldValidator<WorldsRecord> validator =
            new WorldFieldsHelper();

    @Mock
    private ABSWireMapper<LocationsRecord, LocationJSON, NewLocationOrder> locationMapper;

    @Mock
    private ABSWireMapper<LorebooksRecord, LorebookJSON, NewLorebookOrder> lorebookMapper;

    @Mock
    private ABSWireMapper<RegionRecord, RegionJSON, NewRegionOrder> regionMapper;

    @Mock
    private ABSWireMapper<LocationEdgesRecord, EdgeJSON, NewEdgeOrder> edgeMapper;

    @Mock
    private EntityAssetStore<WorldsRecord, WorldSnapshot.Reference> assetStore;

    @Mock
    private EntityReaders readers;

    @Mock
    private EntityReader<LorebooksRecord> lorebookReader;

    @Mock
    private EntityReader<LocationsRecord> locationReader;

    @Mock
    private EntityReader<RegionRecord> regionReader;

    @Mock
    private EntityReader<LocationEdgesRecord> edgeReader;

    private WorldMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new WorldMapper(
                OBJECT_MAPPER,
                readers,
                lorebookMapper,
                locationMapper,
                regionMapper,
                edgeMapper,
                assetStore
        );
    }

    @Test
    void testRoundTrip() throws Exception {
        int worldId = 1;
        int lorebookId = 5;

        WorldsRecord world = new WorldsRecord();

        world.set(WORLDS.ID, worldId);
        world.set(WORLDS.NAME, "world");
        world.set(WORLDS.DESCRIPTION, "description");
        world.set(WORLDS.LOREBOOK_ID, lorebookId);

        // Background placement
        world.set(WORLDS.BACKGROUND_X, 10.0);
        world.set(WORLDS.BACKGROUND_Y, 20.0);
        world.set(WORLDS.BACKGROUND_WIDTH, 500.0);
        world.set(WORLDS.BACKGROUND_HEIGHT, 300.0);

        // Background behavior
        world.set(WORLDS.BACKGROUND_OPACITY, 0.8);
        world.set(WORLDS.BACKGROUND_VISIBLE, true);
        world.set(WORLDS.BACKGROUND_TRANSFORM_LOCKED, true);
        world.set(WORLDS.BACKGROUND_ASPECT_LOCKED, true);
        world.set(WORLDS.BACKGROUND_FIT, "CONTAIN");

        ZipBuilder zipBuilder = mock(ZipBuilder.class);

        // Lorebook
        LorebooksRecord lorebookRecord = new LorebooksRecord();
        lorebookRecord.setId(lorebookId);

        LorebookJSON lorebookJson = mock(LorebookJSON.class);
        NewLorebookOrder lorebookOrder = mock(NewLorebookOrder.class);

        when(readers.lorebooks())
                .thenReturn(lorebookReader);

        when(lorebookReader.require(any()))
                .thenReturn(lorebookRecord);

        when(lorebookMapper.jsonRecordFrom(lorebookRecord, zipBuilder))
                .thenReturn(lorebookJson);

        when(lorebookMapper.orderFrom(lorebookJson))
                .thenReturn(lorebookOrder);

        // Locations
        when(readers.locations())
                .thenReturn(locationReader);

        when(locationReader.getMatching(any(EntityDataPayload.class)))
                .thenReturn(dsl.newResult(LOCATIONS));

        when(locationMapper.jsonRecordsFrom(any(), any()))
                .thenReturn(List.of());

        when(locationMapper.ordersFrom(List.of()))
                .thenReturn(List.of());

        // Regions
        when(readers.regions())
                .thenReturn(regionReader);

        when(regionReader.getMatching(any(EntityDataPayload.class)))
                .thenReturn(dsl.newResult(REGION));

        when(regionMapper.jsonRecordsFrom(any(), any()))
                .thenReturn(List.of());

        when(regionMapper.ordersFrom(List.of()))
                .thenReturn(List.of());

        // Edges
        when(readers.edges())
                .thenReturn(edgeReader);

        when(edgeReader.getMatching(any(EntityDataPayload.class)))
                .thenReturn(dsl.newResult(LOCATION_EDGES));

        when(edgeMapper.jsonRecordsFrom(any(), any()))
                .thenReturn(List.of());

        when(edgeMapper.ordersFrom(List.of()))
                .thenReturn(List.of());

        NewWorldOrder worldOrder =
                mapper.orderFrom(
                        mapper.jsonRecordFrom(world, zipBuilder)
                );

        assertTrue(
                worldOrder.locations().isEmpty(),
                "Locations should be empty"
        );

        assertTrue(
                worldOrder.regions().isEmpty(),
                "Regions should be empty"
        );

        assertTrue(
                worldOrder.locationEdges().isEmpty(),
                "Edges should be empty"
        );

        Asserts.assertRecordEqualsPayloadMinusFields(
                world,
                worldOrder.payload(),
                Set.of(
                        WORLDS.ID,
                        WORLDS.LOREBOOK_ID,
                        WORLDS.NEXT_LOCATION_ID,
                        WORLDS.NEXT_CHARACTER_ID,
                        WORLDS.NEXT_REGION_ID
                )
        );

        var result =
                validator.validateDataPayload(
                        worldOrder.payload()
                );

        assertTrue(result.isSuccess(), result.toString());
    }
}