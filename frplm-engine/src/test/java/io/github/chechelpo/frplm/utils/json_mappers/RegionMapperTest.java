package io.github.chechelpo.frplm.utils.json_mappers;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityReader;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookService;
import io.github.chechelpo.frplm.domain.world.region.RegionService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import io.github.chechelpo.frplm.test_utils.Asserts;
import io.github.chechelpo.frplm.utils.json_mappers.orders.NewRegionOrder;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;

import static io.github.chechelpo.frplm.jooq.generated.Tables.REGION;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RegionMapperTest {
    private static final DSLContext dsl = DSL.using(SQLDialect.H2);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private LorebookService lorebookService;
    private LorebookMapper lorebookMapper;
    private RegionService regionService;
    private RegionMapper mapper;

    @BeforeEach
    void setUp(){
        lorebookService = mock(LorebookService.class);
        lorebookMapper = mock(LorebookMapper.class);
        regionService = mock(RegionService.class);
        mapper = new RegionMapper(regionService, lorebookMapper, lorebookService);
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

        NewRegionOrder regionOrder = mapper.fromJson(mapper.toJson(region));

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