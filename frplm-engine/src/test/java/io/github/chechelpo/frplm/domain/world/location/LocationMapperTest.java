package io.github.chechelpo.frplm.domain.world.location;

import io.github.chechelpo.frplm.core.dispatch.readers.EntityReaders;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.core.entities.mappers.EntityWireMapper;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityReader;
import io.github.chechelpo.frplm.domain.character.core.CharacterJSON;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookJSON;
import io.github.chechelpo.frplm.domain.character.core.CharacterService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import io.github.chechelpo.frplm.test_utils.Asserts;
import io.github.chechelpo.frplm.utils.IO.ZipBuilder;
import io.github.chechelpo.frplm.utils.orders.NewCharacterOrder;
import io.github.chechelpo.frplm.utils.orders.NewLocationOrder;
import io.github.chechelpo.frplm.utils.orders.NewLorebookOrder;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Set;

import static io.github.chechelpo.frplm.jooq.generated.Tables.LOCATIONS;
import static io.github.chechelpo.frplm.jooq.generated.Tables.REGION;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class LocationMapperTest {
    private static final DSLContext dsl = DSL.using(SQLDialect.H2);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private EntityWireMapper<CharactersRecord, CharacterJSON, NewCharacterOrder> characterMapper;
    @Mock
    private EntityWireMapper<LorebooksRecord, LorebookJSON, NewLorebookOrder> lorebookMapper;

    @Mock
    private EntityReaders readers;
    @Mock
    private CharacterService characterService;

    private LocationMapper mapper;

    @BeforeEach
    void setUp(){
        mapper = new LocationMapper(
                OBJECT_MAPPER,
                characterMapper,
                readers,
                characterService,
                lorebookMapper
        );
    }

    @Test
    void testRoundtrip(){
        int worldId = 2;
        int parentId = 4;
        LocationsRecord location = new LocationsRecord();
        location.set(LOCATIONS.ID, 1);
        location.set(LOCATIONS.WORLD_ID, worldId);
        location.set(LOCATIONS.REGION_ID, parentId);
        location.set(LOCATIONS.NAME, "location");
        location.set(LOCATIONS.DESCRIPTION, "description");
        location.set(LOCATIONS.LOREBOOK_ID, 3);

        RegionRecord parent = new RegionRecord();
        String parentName = "parent";
        parent.set(REGION.NAME, parentName);
        when(readers.regions().find(EntityKey.<RegionRecord>builder()
                        .set(REGION.ID, parentId)
                        .set(REGION.WORLD_ID, worldId)
                        .build()
                )
        ).thenReturn(EntityReader.RecordFindResult.found(null, parent));

        LorebooksRecord lorebooksRecord = new LorebooksRecord();
        when(readers.lorebooks().find(any(EntityKey.class))).thenReturn(EntityReader.RecordFindResult.found(null, lorebooksRecord));
        when(lorebookMapper.jsonRecordFrom(lorebooksRecord, any(ZipBuilder.class)))
                .thenReturn(new LorebookJSON(null,null,null));

        CharactersRecord character = new CharactersRecord();
        when(characterService.getStartingAt(worldId, 1)).thenReturn(List.of(character));
        when(characterMapper.jsonRecordsFrom(any(List.class), any(ZipBuilder.class))).thenReturn(List.of());

        NewLocationOrder locationOrder = mapper.orderFrom(mapper.jsonRecordFrom(location, null));

        assertEquals(parentName, locationOrder.parentRegionName(), "Mismatch in parent region");
        Asserts.assertRecordEqualsPayloadMinusFields(
                location, locationOrder.payload(),
                Set.of(
                        LOCATIONS.ID,
                        LOCATIONS.WORLD_ID,
                        LOCATIONS.REGION_ID,
                        LOCATIONS.LOREBOOK_ID
                )
        );
    }
}