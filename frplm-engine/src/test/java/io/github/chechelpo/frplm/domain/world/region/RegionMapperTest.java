package io.github.chechelpo.frplm.domain.world.region;

import io.github.chechelpo.frplm.core.dispatch.readers.EntityReaders;
import io.github.chechelpo.frplm.core.entities.assets.EntityAssetStore;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.core.entities.mappers.ABSWireMapper;
import io.github.chechelpo.frplm.core.entities.mappers.EntityWireMapper;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityReader;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookJSON;
import io.github.chechelpo.frplm.extensions.api.standalone.RegionSnapshot;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import io.github.chechelpo.frplm.test_utils.Asserts;
import io.github.chechelpo.frplm.utils.IO.ZipBuilder;
import io.github.chechelpo.frplm.utils.orders.NewLorebookOrder;
import io.github.chechelpo.frplm.utils.orders.NewRegionOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;

import static io.github.chechelpo.frplm.jooq.generated.Tables.REGION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegionMapperTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private EntityReaders readers;

    @Mock
    private EntityReader<RegionRecord> regionReader;

    @Mock
    private EntityReader<LorebooksRecord> lorebookReader;

    @Mock
    private EntityAssetStore<RegionRecord, RegionSnapshot.Reference> regionAssetStore;

    @Mock
    private ABSWireMapper<LorebooksRecord, LorebookJSON, NewLorebookOrder> lorebookMapper;

    private RegionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new RegionMapper(
                OBJECT_MAPPER,
                readers,
                lorebookMapper,
                regionAssetStore
        );
    }

    @Test
    void testRoundTrip() {
        int worldId = 2;
        int parentId = 4;
        int lorebookId = 3;

        RegionRecord region = createRegion();
        region.set(REGION.WORLD_ID, worldId);
        region.set(REGION.PARENT_REGION_ID, parentId);
        region.set(REGION.LOREBOOK_ID, lorebookId);

        String parentName = "parent";

        RegionRecord parent = new RegionRecord();
        parent.set(REGION.WORLD_ID, worldId);
        parent.set(REGION.ID, parentId);
        parent.set(REGION.NAME, parentName);

        EntityKey<RegionRecord> parentKey =
                EntityKey.<RegionRecord>builder()
                        .set(REGION.WORLD_ID, worldId)
                        .set(REGION.ID, parentId)
                        .build();

        LorebooksRecord lorebookRecord = new LorebooksRecord();
        lorebookRecord.setId(lorebookId);

        LorebookJSON lorebookJson = mock(LorebookJSON.class);
        NewLorebookOrder lorebookOrder = mock(NewLorebookOrder.class);

        ZipBuilder zipBuilder = mock(ZipBuilder.class);

        when(readers.regions())
                .thenReturn(regionReader);

        when(regionReader.find(any(EntityKey.class)))
                .thenReturn(
                        EntityReader.RecordFindResult.found(
                                parentKey,
                                parent
                        )
                );

        when(readers.lorebooks())
                .thenReturn(lorebookReader);

        when(lorebookReader.require(any(EntityKey.class)))
                .thenReturn(lorebookRecord);

        when(lorebookMapper.jsonRecordFrom(lorebookRecord, zipBuilder))
                .thenReturn(lorebookJson);

        when(lorebookMapper.orderFrom(lorebookJson))
                .thenReturn(lorebookOrder);

        NewRegionOrder regionOrder =
                mapper.orderFrom(
                        mapper.jsonRecordFrom(region, zipBuilder)
                );

        assertEquals(
                parentName,
                regionOrder.parentRegionName(),
                "Mismatch in parent region"
        );

        Asserts.assertRecordEqualsPayloadMinusFields(
                region,
                regionOrder.payload(),
                Set.of(
                        REGION.ID,
                        REGION.WORLD_ID,
                        REGION.LOREBOOK_ID,
                        REGION.PARENT_REGION_ID
                )
        );

        verify(lorebookMapper).orderFrom(lorebookJson);
    }

    @Test
    void testRoundTripWithoutParentRegion() {
        int lorebookId = 3;

        RegionRecord region = createRegion();
        region.set(REGION.PARENT_REGION_ID, null);
        region.set(REGION.LOREBOOK_ID, lorebookId);

        LorebooksRecord lorebookRecord = new LorebooksRecord();
        lorebookRecord.setId(lorebookId);

        LorebookJSON lorebookJson = mock(LorebookJSON.class);
        NewLorebookOrder lorebookOrder = mock(NewLorebookOrder.class);

        ZipBuilder zipBuilder = mock(ZipBuilder.class);

        when(readers.lorebooks())
                .thenReturn(lorebookReader);

        when(lorebookReader.require(any(EntityKey.class)))
                .thenReturn(lorebookRecord);

        when(lorebookMapper.jsonRecordFrom(lorebookRecord, zipBuilder))
                .thenReturn(lorebookJson);

        when(lorebookMapper.orderFrom(lorebookJson))
                .thenReturn(lorebookOrder);

        NewRegionOrder regionOrder =
                mapper.orderFrom(
                        mapper.jsonRecordFrom(region, zipBuilder)
                );

        assertNull(regionOrder.parentRegionName());

        Asserts.assertRecordEqualsPayloadMinusFields(
                region,
                regionOrder.payload(),
                Set.of(
                        REGION.ID,
                        REGION.WORLD_ID,
                        REGION.LOREBOOK_ID,
                        REGION.PARENT_REGION_ID
                )
        );

        verify(readers, never()).regions();
    }

    private RegionRecord createRegion() {
        RegionRecord region = new RegionRecord();

        region.set(REGION.ID, 1);
        region.set(REGION.WORLD_ID, 2);

        region.set(REGION.NAME, "region");
        region.set(REGION.DESCRIPTION, "description");
        region.set(REGION.LOREBOOK_ID, 3);

        region.set(REGION.LOCKED, true);

        region.set(REGION.X, 10.0);
        region.set(REGION.Y, 20.0);
        region.set(REGION.WIDTH, 300.0);
        region.set(REGION.HEIGHT, 200.0);

        region.set(REGION.BACKGROUND_OPACITY, 0.75);
        region.set(REGION.BACKGROUND_VISIBLE, true);
        region.set(REGION.BACKGROUND_ASPECT_LOCKED, false);
        region.set(REGION.BACKGROUND_FIT, "CONTAIN");

        region.set(REGION.COLLAPSED, false);

        return region;
    }
}