package io.github.chechelpo.frplm.domain.character.core;

import io.github.chechelpo.frplm.core.dispatch.readers.EntityReaders;
import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.fields.FieldActionResult;
import io.github.chechelpo.frplm.core.entities.fields.FieldValidator;
import io.github.chechelpo.frplm.core.entities.mappers.ABSWireMapper;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityReader;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookJSON;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import io.github.chechelpo.frplm.test_utils.Asserts;
import io.github.chechelpo.frplm.utils.IO.ZipBuilder;
import io.github.chechelpo.frplm.utils.orders.NewCharacterOrder;
import io.github.chechelpo.frplm.utils.orders.NewLorebookOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;

import static io.github.chechelpo.frplm.jooq.generated.Tables.CHARACTERS;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CharacterMapperTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private ABSWireMapper<LorebooksRecord, LorebookJSON, NewLorebookOrder> lorebookMapper;

    @Mock
    private EntityReaders readers;

    @Mock
    private EntityReader<LorebooksRecord> lorebookReader;

    private FieldValidator<CharactersRecord> fieldValidator;

    private CharacterMapper mapper;

    @BeforeEach
    void setUp() {
        fieldValidator = new CharacterFieldsHelper();
        mapper = new CharacterMapper(
                OBJECT_MAPPER,
                lorebookMapper,
                readers
        );
    }

    @Test
    void testRoundTrip() {
        CharactersRecord character = new CharactersRecord();

        character.set(CHARACTERS.ID, 1);
        character.set(CHARACTERS.NAME, "character");
        character.set(CHARACTERS.DESCRIPTION, "description");
        character.set(CHARACTERS.CAN_BE_USER, true);
        character.set(CHARACTERS.WELCOME_MESSAGE, "welcome");
        character.set(CHARACTERS.LOREBOOK_ID, 3);

        LorebooksRecord lorebooksRecord = new LorebooksRecord();

        LorebookJSON lorebookJson = mock(LorebookJSON.class);
        NewLorebookOrder lorebookOrder = mock(NewLorebookOrder.class);
        ZipBuilder zipBuilder = mock(ZipBuilder.class);

        when(readers.lorebooks())
                .thenReturn(lorebookReader);

        when(lorebookReader.require(any()))
                .thenReturn(lorebooksRecord);

        when(lorebookMapper.jsonRecordFrom(lorebooksRecord, zipBuilder))
                .thenReturn(lorebookJson);

        when(lorebookMapper.orderFrom(lorebookJson))
                .thenReturn(lorebookOrder);

        NewCharacterOrder characterOrder =
                mapper.orderFrom(
                        mapper.jsonRecordFrom(character, zipBuilder)
                );

        Asserts.assertRecordEqualsPayloadMinusFields(
                character,
                characterOrder.payload(),
                Set.of(
                        CHARACTERS.ID,
                        CHARACTERS.WORLD_ID,
                        CHARACTERS.STARTING_LOCATION_ID,
                        CHARACTERS.LOREBOOK_ID
                )
        );
        var validationResult = fieldValidator.validateDataPayload(characterOrder.payload());
        assertTrue(validationResult.isSuccess(), "Validation error:\n" + validationResult.validatorMessage());

        assertSame(lorebookOrder, characterOrder.lorebook());
    }
}