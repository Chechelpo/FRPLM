package io.github.chechelpo.frplm.utils.json_mappers;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityReader;
import io.github.chechelpo.frplm.domain.character.core.CharacterService;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookService;
import io.github.chechelpo.frplm.domain.world.region.RegionService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import io.github.chechelpo.frplm.test_utils.Asserts;
import io.github.chechelpo.frplm.utils.json_mappers.orders.NewLocationOrder;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Set;

import static io.github.chechelpo.frplm.jooq.generated.Tables.LOCATIONS;
import static io.github.chechelpo.frplm.jooq.generated.Tables.REGION;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocationMapperTest {
    private static final DSLContext dsl = DSL.using(SQLDialect.H2);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private LorebookService lorebookService;
    private LorebookMapper lorebookMapper;
    private CharacterService characterService;
    private CharacterMapper characterMapper;
    private RegionService regionService;
    private LocationMapper mapper;

    @BeforeEach
    void setUp(){
        lorebookService = mock(LorebookService.class);
        lorebookMapper = mock(LorebookMapper.class);
        characterService = mock(CharacterService.class);
        characterMapper = mock(CharacterMapper.class);
        regionService = mock(RegionService.class);
        mapper = new LocationMapper(lorebookService, lorebookMapper, characterService, characterMapper, regionService);
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
        when(regionService.find(EntityKey.<RegionRecord>builder()
                        .set(REGION.ID, parentId)
                        .set(REGION.WORLD_ID, worldId)
                        .build()
                )
        ).thenReturn(EntityReader.RecordFindResult.found(null, parent));

        LorebooksRecord lorebooksRecord = new LorebooksRecord();
        when(lorebookService.getLorebookOf(location)).thenReturn(lorebooksRecord);
        when(lorebookMapper.jsonFrom(lorebooksRecord)).thenReturn(OBJECT_MAPPER.nullNode());

        CharactersRecord character = new CharactersRecord();
        when(characterService.getStartingAt(worldId, 1)).thenReturn(List.of(character));
        when(characterMapper.jsonFrom(character)).thenReturn(OBJECT_MAPPER.createObjectNode());

        NewLocationOrder locationOrder = mapper.orderFrom(mapper.jsonFrom(location));

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