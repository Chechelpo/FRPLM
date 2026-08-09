package io.github.chechelpo.frplm.domain.world.region;

import io.github.chechelpo.frplm.core.dispatch.readers.EntityReaders;
import io.github.chechelpo.frplm.core.entities.assets.EntityAssetStore;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.core.entities.mappers.EntityWireMapper;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityReader;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookJSON;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookMapper;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookService;
import io.github.chechelpo.frplm.extensions.api.standalone.RegionSnapshot;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import io.github.chechelpo.frplm.test_utils.Asserts;
import io.github.chechelpo.frplm.utils.orders.NewLorebookOrder;
import io.github.chechelpo.frplm.utils.orders.NewRegionOrder;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;

import static io.github.chechelpo.frplm.jooq.generated.Tables.REGION;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RegionMapperTest {
    private static final DSLContext dsl = DSL.using(SQLDialect.H2);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private EntityReaders readers;
    @Mock
    private LorebookService lorebookService;

    @Mock
    private EntityAssetStore<RegionRecord, RegionSnapshot.Reference> regionAssetStore;
    @Mock
    private EntityWireMapper<LorebooksRecord, LorebookJSON, NewLorebookOrder> lorebookMapper;

    private RegionService regionService;
    private RegionMapper mapper;

    @BeforeEach
    void setUp(){
        mapper = new RegionMapper(OBJECT_MAPPER, readers, lorebookMapper, regionAssetStore);
    }

    @Test
    void testRoundTrip(){
        int worldId = 2;
        int parentId = 4;
        RegionRecord region = new RegionRecord();
        region.set(REGION.ID, 1);
        region.set(REGION.WORLD_ID, worldId);
        region.set(REGION.NAME, "region");
        region.set(REGION.DESCRIPTION, "description");
        region.set(REGION.LOREBOOK_ID, 3);
        region.set(REGION.PARENT_REGION_ID, parentId);

        RegionRecord parent = new RegionRecord();
        String parentName = "parent";
        parent.set(REGION.NAME, parentName);
        when(regionService.find(EntityKey.<RegionRecord>builder()
                        .set(REGION.ID, parentId)
                        .set(REGION.WORLD_ID, worldId)
                        .build()
                )
        ).thenReturn(EntityReader.RecordFindResult.found(null, parent));

        LorebooksRecord lorebooksRecord = new LorebooksRecord();
        when(lorebookService.getLorebookOf(region)).thenReturn(lorebooksRecord);
        when(lorebookMapper.jsonFrom(lorebooksRecord)).thenReturn(OBJECT_MAPPER.nullNode());

        NewRegionOrder regionOrder = mapper.orderFrom(mapper.jsonRecordFrom(region, null));

        assertEquals(parentName, regionOrder.parentRegionName(), "Mismatch in parent region");
        Asserts.assertRecordEqualsPayloadMinusFields(
                region, regionOrder.payload(),
                Set.of(
                        REGION.ID,
                        REGION.WORLD_ID,
                        REGION.LOREBOOK_ID,
                        REGION.PARENT_REGION_ID
                )
        );
    }
}