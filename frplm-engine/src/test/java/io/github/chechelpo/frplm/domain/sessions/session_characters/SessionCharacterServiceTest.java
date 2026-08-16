package io.github.chechelpo.frplm.domain.sessions.session_characters;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.SessionCharactersRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import io.github.chechelpo.frplm.test_annotations.SimulithIntegrationTest;
import io.github.chechelpo.frplm.test_utils.comparators.RecordComparator;
import io.github.chechelpo.frplm.test_utils.fixtures.CharacterFixtures;
import io.github.chechelpo.frplm.test_utils.fixtures.EntityFixtureFactory;
import io.github.chechelpo.frplm.test_utils.fixtures.SessionCharacterFixture;
import io.github.chechelpo.frplm.test_utils.fixtures.SessionFixtures;
import io.github.chechelpo.frplm.utils.stable_records.StableRecordCreator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(EntityFixtureFactory.class)
class SessionCharacterServiceTest {

    @Autowired
    private StableRecordCreator stableRecordCreator;

    @Nested
    class PermanentCharacters {
        @Autowired
        private EntityFixtureFactory fixtureFactory;

        @BeforeEach
        void setUp() {
            stableRecordCreator.run();
        }

        @Test
        @SimulithIntegrationTest
        void chckCreatedWithData() {
            String seed = "chck-created-with-data";
            CharacterFixtures characterFixtures = fixtureFactory.characters(seed);
            SessionCharacterFixture sesCharacterFixtures = fixtureFactory.sesCharacters(seed);

            List<CharactersRecord> records = new ArrayList<>(characterFixtures.addAndCreateList(
                    100,
                    i -> EntityDataPayload.<CharactersRecord>builder()
                            .set(CHARACTERS.NAME, "character " + i)
            ));
            CharactersRecord userCharacter = records.getFirst();
            characterFixtures.service().update(
                    EntityDataPayload.<CharactersRecord>builder()
                            .set(CHARACTERS.NAME, "user character")
                            .set(CHARACTERS.CAN_BE_USER, true)
                            .build()
                    ,
                    userCharacter
            );
            userCharacter = characterFixtures.getUpdatedRecord(userCharacter);
            records.set(0, userCharacter);

            SessionsRecord sessionsRecord = fixtureFactory.sessions(seed).createOne(
                    EntityDataPayload.<SessionsRecord>builder()
                            .set(SESSIONS.WORLD_ID, userCharacter.getWorldId())
                            .set(SESSIONS.USER_PERSONA_ID, userCharacter.getId())
                            .build()
            );

            records.forEach(
                    created -> {
                        SessionCharactersRecord found = sesCharacterFixtures
                                .service().getCharacterOf(sessionsRecord.getId(), created);

                        RecordComparator.compare(created, found)
                                .equals(CHARACTERS.NAME, SESSION_CHARACTERS.NAME)
                                .equals(CHARACTERS.DESCRIPTION, SESSION_CHARACTERS.DESCRIPTION)
                                .equals(CHARACTERS.STARTING_LOCATION_ID, SESSION_CHARACTERS.CURRENT_LOCATION_ID)
                                .execute();
                    }
            );
        }

        @Test
        @SimulithIntegrationTest
        void testKeepUpdated(){
            String seed = "test-keep-updated";
            CharacterFixtures characterFixtures = fixtureFactory.characters(seed);
            SessionFixtures sessionFixtures = fixtureFactory.sessions(seed);
            SessionCharacterFixture sesCharacterFixture = fixtureFactory.sesCharacters(seed);

            // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            // Instantiated with permanent character
            // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            CharactersRecord permCharacter = characterFixtures.createOne();
            SessionsRecord session = sessionFixtures.createOne(SESSIONS.WORLD_ID, permCharacter.getWorldId());
            SessionCharactersRecord sesCharacter = sesCharacterFixture.service()
                    .getCharacterOf(session.getId(), permCharacter);

            RecordComparator.compare(permCharacter, sesCharacter)
                    .equals(CHARACTERS.NAME, SESSION_CHARACTERS.NAME)
                    .equals(CHARACTERS.DESCRIPTION, SESSION_CHARACTERS.DESCRIPTION)
                    .execute();

            // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            // Keep updated = false -> don't propagate updates on permanent character
            // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            assertFalse(sesCharacter.getKeepUpdated());
            characterFixtures.service()
                    .update(permCharacter, EntityDataPayload.<CharactersRecord>builder()
                            .set(CHARACTERS.NAME, "newName")
                            .set(CHARACTERS.DESCRIPTION, "newDescription")
                            .build()
                    ).orElseThrow();

            CharactersRecord updatedCharacter = characterFixtures.getUpdatedRecord(permCharacter);
            sesCharacter = sesCharacterFixture.getUpdatedRecord(sesCharacter);

            RecordComparator.compare(updatedCharacter, sesCharacter)
                    .notEquals(CHARACTERS.NAME, SESSION_CHARACTERS.NAME)
                    .notEquals(CHARACTERS.DESCRIPTION, SESSION_CHARACTERS.DESCRIPTION)
                    .execute();

            // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            // Keep updated = true -> immediate propagation of perm character's data
            // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            sesCharacterFixture.service()
                    .update(
                            SESSION_CHARACTERS.KEEP_UPDATED, true,
                            sesCharacter
                    ).orElseThrow();
            sesCharacter = sesCharacterFixture.getUpdatedRecord(sesCharacter);

            RecordComparator.compare(updatedCharacter, sesCharacter)
                    .equals(CHARACTERS.NAME, SESSION_CHARACTERS.NAME)
                    .equals(CHARACTERS.DESCRIPTION, SESSION_CHARACTERS.DESCRIPTION)
                    .execute();

            // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            // Keep updated = true -> immediate propagation of updates on perm character
            // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            String sndNewDescription = "aa";
            String sndNewName        = "s";
            characterFixtures.service()
                    .update(
                            CHARACTERS.NAME, sndNewName,
                            permCharacter
                    ).orElseThrow();
            permCharacter = characterFixtures.getUpdatedRecord(permCharacter);
            sesCharacter = sesCharacterFixture.getUpdatedRecord(sesCharacter);
            assertEquals(sndNewName, sesCharacter.getName());
            characterFixtures.service()
                    .update(
                            CHARACTERS.DESCRIPTION, sndNewDescription,
                            permCharacter
                    ).orElseThrow();
            sesCharacter = sesCharacterFixture.getUpdatedRecord(sesCharacter);
            assertEquals(sndNewDescription, sesCharacter.getDescription());
        }
    }

    @Test
    void rejectsUntraversableMovements(){
        
    }

}