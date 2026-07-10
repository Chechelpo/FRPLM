package io.github.chechelpo.frplm.domain.character.core;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookTestContext;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import io.github.chechelpo.frplm.test_utils.TestText;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.github.chechelpo.frplm.jooq.generated.Tables.CHARACTERS;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest()
@Sql(
        scripts = "classpath:db/schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Import({
        LorebookTestContext.class,
})
class CharacterServiceTest {
    @Autowired
    LorebookTestContext lorebookTestContext;
    @Autowired
    private CharacterService characterService;
    @Autowired
    private CharacterFieldsHelper characterFieldsHelper;

    @BeforeEach
    void setUp() {
        lorebookTestContext.reload();
    }

    @Test
    void testGetWithName(){
        int characterAmount = 100;

        List<EntityDataPayload<CharactersRecord>> charactersData = new ArrayList<>(characterAmount);
        long seed = 10;

        for (int i = 0; i < characterAmount; i++)
            charactersData.add(EntityDataPayload.<CharactersRecord>builder()
                    .set(CHARACTERS.NAME, TestText.randomText(seed + i, 0, 255))
                    .build()
            );
        charactersData.forEach(data -> {
            CharactersRecord record = characterService.createAndGet(data);
            assertNotNull(record);
            Optional<CharactersRecord> withName = characterService.getCharacterWith(data.requireValue(CHARACTERS.NAME));
            assertTrue(withName.isPresent());

            assertEquals(characterService.keyOf(record),
                    characterService.keyOf(withName.get()),
                    "Mismatch in getWith name"
            );
        });

    }

    @Test
    void testCharacterLorebook() {
        int characterAmount = 100;

        List<EntityDataPayload<CharactersRecord>> charactersData = new ArrayList<>(characterAmount);
        long seed = 10;

        for (int i = 0; i < characterAmount; i++)
            charactersData.add(EntityDataPayload.<CharactersRecord>builder()
                    .set(CHARACTERS.NAME, TestText.randomText(seed + i, 0, 255))
                    .build()
            );

        List<CharactersRecord> records = charactersData.stream().map(
                data -> assertDoesNotThrow(() -> characterService.createAndGet(data))
        ).toList();

        for (int i = 0; i < characterAmount; i++)
            assertEquals(lorebookTestContext.service.getLorebookOf(records.get(i)).getName(), records.get(i).getName());

        for (int i = 0; i < characterAmount; i++){
            CharactersRecord record = records.get(i);
            LorebooksRecord lorebook = lorebookTestContext.service.getLorebookOf(record);

            assertTrue(this.characterService.delete(characterService.keyOf(record)), "Error deleting character");
            assertTrue(lorebookTestContext.service.find(
                    lorebookTestContext.service.keyOf(lorebook)
            ).isEmpty(), "Stale lorebook referencing character");
        }
    }
}