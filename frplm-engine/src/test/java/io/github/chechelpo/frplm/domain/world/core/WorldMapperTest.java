package io.github.chechelpo.frplm.domain.world.core;

import io.github.chechelpo.frplm.core.dispatch.readers.EntityReaders;
import io.github.chechelpo.frplm.core.entities.assets.EntityAssetStore;
import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.fields.FieldActionResult;
import io.github.chechelpo.frplm.core.entities.fields.FieldValidator;
import io.github.chechelpo.frplm.core.entities.mappers.EntityWireMapper;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookJSON;
import io.github.chechelpo.frplm.domain.world.edge.EdgeJSON;
import io.github.chechelpo.frplm.domain.world.location.LocationJSON;
import io.github.chechelpo.frplm.domain.world.region.RegionJSON;
import io.github.chechelpo.frplm.extensions.api.standalone.WorldSnapshot;
import io.github.chechelpo.frplm.jooq.generated.tables.records.*;
import io.github.chechelpo.frplm.test_utils.Asserts;
import io.github.chechelpo.frplm.utils.orders.*;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;

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

    FieldValidator<WorldsRecord> validator = new WorldFieldsHelper();

    @Mock
    private EntityWireMapper<LocationsRecord, LocationJSON, NewLocationOrder> locationMapper;
    @Mock
    private EntityWireMapper<LorebooksRecord, LorebookJSON, NewLorebookOrder> lorebookMapper;
    @Mock
    private EntityWireMapper<RegionRecord, RegionJSON, NewRegionOrder> regionMapper;
    @Mock
    private EntityWireMapper<LocationEdgesRecord, EdgeJSON, NewEdgeOrder> edgeMapper;
    @Mock
    private EntityAssetStore<WorldsRecord, WorldSnapshot.Reference> assetStore;

    @Mock
    private EntityReaders readers;

    private WorldMapper mapper;

    @BeforeEach
    void setUp(){
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
    void testRoundTrip(){
        int worldId = 1;
        WorldsRecord world = new WorldsRecord();
        world.set(WORLDS.ID, worldId);
        world.set(WORLDS.NAME, "world");
        world.set(WORLDS.DESCRIPTION, "description");
        world.set(WORLDS.LOREBOOK_ID, 5);

        when(readers.lorebooks().getMatching(any(EntityDataPayload.class)))
                .thenReturn(dsl.newResult(LOCATIONS));
        when(readers.regions().getMatching(any(EntityDataPayload.class)))
                .thenReturn(dsl.newResult(REGION));

        LorebooksRecord lorebooksRecord = new LorebooksRecord();


        NewWorldOrder worldOrder = mapper.orderFrom(mapper.jsonRecordFrom(world, null));

        assertTrue(worldOrder.locations().isEmpty(), "Locations should be empty");
        assertTrue(worldOrder.regions().isEmpty(), "Regions should be empty");
        assertTrue(worldOrder.locationEdges().isEmpty(), "Edges should be empty");
        Asserts.assertRecordEqualsPayloadMinusFields(
                world, worldOrder.payload(),
                Set.of(
                        WORLDS.ID,
                        WORLDS.LOREBOOK_ID,
                        WORLDS.NEXT_LOCATION_ID,
                        WORLDS.NEXT_REGION_ID
                )
        );

        FieldActionResult<WorldsRecord, EntityDataPayload<WorldsRecord>> field =
                validator.validateDataCreationPayload(worldOrder.payload());

        assertTrue(field.isSuccess());
    }
}