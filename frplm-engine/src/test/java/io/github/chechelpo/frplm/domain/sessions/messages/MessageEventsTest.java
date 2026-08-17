package io.github.chechelpo.frplm.domain.sessions.messages;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.jooq.generated.tables.records.*;
import io.github.chechelpo.frplm.test_annotations.SimulithIntegrationTest;
import io.github.chechelpo.frplm.test_utils.comparators.RecordComparator;
import io.github.chechelpo.frplm.test_utils.fixtures.*;
import io.github.chechelpo.frplm.utils.stable_records.StableRecordCreator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
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
    @Autowired
    private MessageSource messageSource;
    @Autowired
    private MessageService messageService;

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

    @Test
    @SimulithIntegrationTest
    void onUserCharacterMovement_UpdateMessageLocation(){
        String seed = "user-character-movement";
        SessionFixtures sessionFixtures = fixtureFactory.sessions(seed);
        SessionCharacterFixture sessionCharacterFixture = fixtureFactory.sesCharacters(seed);
        LocationFixtures locationFixtures = fixtureFactory.locations(seed);
        EdgesFixtures edgesFixtures = fixtureFactory.edges(seed);

        SessionsRecord session = sessionFixtures.createOne();
        SessionCharactersRecord character = sessionCharacterFixture.service().getUserCharacterOf(session);

        List<LocationsRecord> locations = new ArrayList<>(locationFixtures.addAndCreateList(
                10,
                i -> EntityDataPayload.<LocationsRecord>builder()
                        .set(LOCATIONS.WORLD_ID, session.getWorldId())
                        .set(LOCATIONS.NAME, "Location " + i)
                )
        );
        locations.addFirst(locationFixtures.service().require(
                EntityKey.<LocationsRecord>builder()
                        .set(LOCATIONS.WORLD_ID, session.getWorldId())
                        .set(LOCATIONS.ID, character.getCurrentLocationId())
                        .build()
        ));
        edgesFixtures.linkLinear(locations);

        locations.forEach(
                newLocation -> {
                    sessionCharacterFixture.move(character, newLocation);
                    MessagesRecord lastMessage = messageService.getLastMessageOf(session);
                    SessionCharactersRecord updatedChar = sessionCharacterFixture.getUpdatedRecord(character);

                    RecordComparator.compare(updatedChar, lastMessage)
                            .equals(SESSION_CHARACTERS.SESSION_ID, MESSAGES.SESSION_ID)
                            .equals(SESSION_CHARACTERS.WORLD_ID, MESSAGES.WORLD_ID)
                            .equals(SESSION_CHARACTERS.CURRENT_LOCATION_ID, MESSAGES.LOCATION_ID)
                            .execute();
                }
        );
    }
}