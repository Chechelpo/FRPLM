package io.github.chechelpo.frplm.utils.json_mappers;

import io.github.chechelpo.frplm.domain.lorebook.core.LorebookService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import io.github.chechelpo.frplm.test_utils.Asserts;
import io.github.chechelpo.frplm.utils.json_mappers.orders.NewCharacterOrder;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;

import static io.github.chechelpo.frplm.jooq.generated.Tables.CHARACTERS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CharacterMapperTest {
    private static final DSLContext dsl = DSL.using(SQLDialect.H2);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private LorebookService lorebookService;
    private LorebookMapper lorebookMapper;
    private CharacterMapper mapper;

    @BeforeEach
    void setUp(){
        lorebookService = mock(LorebookService.class);
        lorebookMapper = mock(LorebookMapper.class);
        mapper = new CharacterMapper(lorebookMapper, lorebookService);
    }

    @Test
    void testRoundTrip(){
        CharactersRecord character = new CharactersRecord();
        character.set(CHARACTERS.ID, 1);
        character.set(CHARACTERS.NAME, "character");
        character.set(CHARACTERS.DESCRIPTION, "description");
        character.set(CHARACTERS.IS_ARCHETYPE, false);
        character.set(CHARACTERS.CAN_BE_USER, true);
        character.set(CHARACTERS.WELCOME_MESSAGE, "welcome");
        character.set(CHARACTERS.LOREBOOK_ID, 3);

        LorebooksRecord lorebooksRecord = new LorebooksRecord();
        when(lorebookService.getLorebookOf(character)).thenReturn(lorebooksRecord);
        when(lorebookMapper.jsonFrom(lorebooksRecord)).thenReturn(OBJECT_MAPPER.nullNode());

        NewCharacterOrder characterOrder = mapper.fromJson(mapper.jsonFrom(character));

        Asserts.assertRecordEqualsPayloadMinusFields(
                character, characterOrder.info(),
                Set.of(
                        CHARACTERS.ID,
                        CHARACTERS.CREATED,
                        CHARACTERS.LOREBOOK_ID
                )
        );
    }
}