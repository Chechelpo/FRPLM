package io.github.chechelpo.frplm.domain.sessions.messages;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.MessagesRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import io.github.chechelpo.frplm.test_annotations.SimulithIntegrationTest;
import io.github.chechelpo.frplm.test_utils.comparators.RecordComparator;
import io.github.chechelpo.frplm.test_utils.fixtures.CharacterFixtures;
import io.github.chechelpo.frplm.test_utils.fixtures.EntityFixtureFactory;
import io.github.chechelpo.frplm.test_utils.fixtures.MessageFixtures;
import io.github.chechelpo.frplm.test_utils.fixtures.SessionFixtures;
import io.github.chechelpo.frplm.utils.stable_records.StableRecordCreator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(EntityFixtureFactory.class)
class MessageEventsTest {

    @Autowired
    private StableRecordCreator stableRecordCreator;
    @Autowired
    private EntityFixtureFactory fixtureFactory;

    @BeforeEach
    void setUp() {
        stableRecordCreator.run();
    }

    @Test
    @SimulithIntegrationTest
    void testWelcome_MessageCreation(){
        String seed = "test-message-creation";

        CharacterFixtures characterFixtures = fixtureFactory.characters(seed);
        CharactersRecord userCharacter = characterFixtures.createOne(
                EntityDataPayload.of(CHARACTERS.CAN_BE_USER, true)
        );

        SessionFixtures sessionFixtures = fixtureFactory.sessions(seed);
        SessionsRecord session = sessionFixtures.createOne(
                EntityDataPayload.<SessionsRecord>builder()
                        .set(SESSIONS.USER_PERSONA_ID, userCharacter.getId())
                        .set(SESSIONS.WORLD_ID, userCharacter.getWorldId())
                        .build()
        );

        MessageFixtures messageFixtures = fixtureFactory.messages(seed);
        List<MessagesRecord> messages = messageFixtures.service().getMatching(
                EntityKey.<MessagesRecord>builder()
                        .set(MESSAGES.SESSION_ID, session.getId())
                .build()
        );

        assertEquals(1, messages.size());
        MessagesRecord firstMessage = messages.getFirst();

        RecordComparator.compare(userCharacter, firstMessage)
                .equals(CHARACTERS.WELCOME_MESSAGE, MESSAGES.CONTENT)
                .equals(CHARACTERS.STARTING_LOCATION_ID, MESSAGES.LOCATION_ID)
                .equals(CHARACTERS.WORLD_ID, MESSAGES.WORLD_ID)
                .execute();
    }
}