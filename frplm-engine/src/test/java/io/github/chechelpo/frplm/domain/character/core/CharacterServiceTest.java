package io.github.chechelpo.frplm.domain.character.core;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.jooq.generated.tables.records.*;
import io.github.chechelpo.frplm.test_annotations.SimulithIntegrationTest;
import io.github.chechelpo.frplm.test_utils.fixtures.*;
import io.github.chechelpo.frplm.utils.stable_records.StableRecordCreator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static io.github.chechelpo.frplm.jooq.generated.Tables.*;

@SpringBootTest
@Import(EntityFixtureFactory.class)
class CharacterServiceTest {
    @Autowired
    EntityFixtureFactory factory;
    @Autowired
    private StableRecordCreator stableRecordCreator;

    private LorebookFixtures lorebookFixtures;
    private CharacterFixtures characterFixtures;

    @BeforeEach
    void setUp() {
        String seed = "test-character";

        lorebookFixtures = factory.lorebook(seed);
        characterFixtures = factory.characters(seed);

        stableRecordCreator.run();
    }

    @Test
    @SimulithIntegrationTest
    void testCharacterLorebook_creates_updates_deletes() {
        characterFixtures.addAndCreateList(
                10,
                i -> EntityDataPayload.<CharactersRecord>builder()
                        .set(CHARACTERS.NAME, "Character " + i)
        ).forEach(
                created -> {
                    EntityKey<LorebooksRecord> lorebookKey = EntityKey.of(LOREBOOKS.ID, created.getLorebookId());
                    lorebookFixtures.assertFieldEquals(created.getName(), LOREBOOKS.NAME, lorebookKey);

                    String newName = "newName";
                    characterFixtures.service().update(CHARACTERS.NAME, newName, created);
                    lorebookFixtures.assertFieldEquals(newName, LOREBOOKS.NAME, lorebookKey);

                    characterFixtures.service().delete(created);
                    lorebookFixtures.assertDoesNotExist(lorebookKey);
                }
        );
    }
}