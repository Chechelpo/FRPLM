package chechelpo.frplm.domain.sessions.movement;

import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.domain.character.core.CharacterCoreTestContext;
import chechelpo.frplm.domain.character.starting_locations.StartingLocationTestContext;
import chechelpo.frplm.domain.sessions.core.SessionTestContext;
import chechelpo.frplm.domain.sessions.messages.MessageTestContext;
import chechelpo.frplm.domain.world.edge.EdgeTestContext;
import chechelpo.frplm.domain.world.location.LocationTestContext;
import chechelpo.frplm.exceptions.runtime.InvalidValue;
import chechelpo.frplm.jooq.generated.tables.records.*;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Optional;

import static chechelpo.frplm.jooq.generated.Tables.CURRENT_LOCATIONS;
import static chechelpo.frplm.jooq.generated.Tables.MESSAGES;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Sql(
        scripts = "classpath:db/schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Import({MessageTestContext.class, LocationTestContext.class, SessionTestContext.class,
        StartingLocationTestContext.class, CharacterCoreTestContext.class, EdgeTestContext.class,
        CurrentLocationTestContext.class
})
class LocationsEventReactorTest {
    @Autowired
    MessageTestContext messages;
    @Autowired
    private LocationTestContext locationTestContext;
    @Autowired
    private CharacterCoreTestContext characterCoreTestContext;
    @Autowired
    private StartingLocationTestContext startingLocationTestContext;
    @Autowired
    private SessionTestContext sessionTestContext;
    @Autowired
    private EdgeTestContext edgeTestContext;
    @Autowired
    private CurrentLocationTestContext currentLocationTestContext;
    @Autowired
    private Movements movements;

    @BeforeEach
    void setUp() {
        messages.reload();
        locationTestContext.reload();
        characterCoreTestContext.reload();
        startingLocationTestContext.reload();
        sessionTestContext.reload();
        edgeTestContext.reload();
        currentLocationTestContext.reload();
    }

    @Test
    void rewindLocationsOnMessageDeleted() {
        int messageAmount = 10;
        MessageTestContext.Context context = messages.createSessionWithMessages(100, messageAmount);
        SessionsRecord sessionsRecord = context.sessionContext().session();
        CharactersRecord userCharacter = context.sessionContext().userCharacter();

        LocationsRecord previousLocation = currentLocationTestContext.service.getLocationOf(userCharacter, sessionsRecord);
        LocationsRecord nextLocation = context.sessionContext().sessionLocations().stream()
                .filter(location -> !location.getId().equals(previousLocation.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No location found to test a movement"));
        edgeTestContext.link(sessionsRecord.getWorldId(), previousLocation.getId(), nextLocation.getId());

        EntityKey<CurrentLocationsRecord> currentLocationsKey = EntityKey.<CurrentLocationsRecord>builder()
                .set(CURRENT_LOCATIONS.SESSION_ID, sessionsRecord.getId())
                .set(CURRENT_LOCATIONS.CHARACTER_ID, userCharacter.getId())
                .build();

        CurrentLocationsRecord beforeDeletion = currentLocationTestContext.service.find(currentLocationsKey)
                .orElseThrow(() -> new IllegalStateException("Current locations record not found"));

        MessagesRecord lastMessage = messages.service.getLastOf(sessionsRecord);

        assertTrue(
                currentLocationTestContext.service.update(currentLocationsKey,
                        EntityDataPayload.<CurrentLocationsRecord>builder()
                                .set(CURRENT_LOCATIONS.WORLD_ID, sessionsRecord.getWorldId())
                                .set(CURRENT_LOCATIONS.LOCATION_ID, nextLocation.getId())
                                .set(CURRENT_LOCATIONS.TICK_NUM, lastMessage.getTickNum())
                                .build()
                ),
                "Error moving user character"
        );
        assertTrue(
                messages.service.delete(EntityKey.<MessagesRecord>builder()
                        .set(MESSAGES.SESSION_ID, sessionsRecord.getId())
                        .set(MESSAGES.TICK_NUM, lastMessage.getTickNum())
                        .build()
                ),
                "Error deleting last message"
        );
        Optional<CurrentLocationsRecord> actualLocation = currentLocationTestContext.service.find(currentLocationsKey);
        assertTrue(actualLocation.isPresent(), "Character has no current location");

        assertNotEquals(nextLocation.getId(), actualLocation.get().getLocationId(), "Character is still at next location");
        assertEquals(previousLocation.getId(), actualLocation.get().getLocationId(), "Didn't move back to previous location");
    }

    @Test
    void rewindLocationsOnMessageDeleted_doesNothingOnWrongMessageDeletion() {
        int messageAmount = 10;
        MessageTestContext.Context context = messages.createSessionWithMessages(100, messageAmount);
        SessionsRecord sessionsRecord = context.sessionContext().session();
        CharactersRecord userCharacter = context.sessionContext().userCharacter();

        LocationsRecord previousLocation = currentLocationTestContext.service.getLocationOf(userCharacter, sessionsRecord);
        LocationsRecord nextLocation = context.sessionContext().sessionLocations().stream()
                .filter(location -> !location.getId().equals(previousLocation.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No location found to test a movement"));
        edgeTestContext.link(sessionsRecord.getWorldId(), previousLocation.getId(), nextLocation.getId());

        EntityKey<CurrentLocationsRecord> currentLocationsKey = EntityKey.<CurrentLocationsRecord>builder()
                .set(CURRENT_LOCATIONS.SESSION_ID, sessionsRecord.getId())
                .set(CURRENT_LOCATIONS.CHARACTER_ID, userCharacter.getId())
                .build();

        CurrentLocationsRecord beforeDeletion = currentLocationTestContext.service.find(currentLocationsKey)
                .orElseThrow(() -> new IllegalStateException("Current locations record not found"));

        MessagesRecord lastMessage = messages.service.getLastOf(sessionsRecord);

        assertTrue(
                currentLocationTestContext.service.update(currentLocationsKey,
                        EntityDataPayload.<CurrentLocationsRecord>builder()
                                .set(CURRENT_LOCATIONS.WORLD_ID, sessionsRecord.getWorldId())
                                .set(CURRENT_LOCATIONS.LOCATION_ID, nextLocation.getId())
                                .set(CURRENT_LOCATIONS.TICK_NUM, lastMessage.getTickNum())
                                .build()
                ),
                "Error moving user character"
        );
        assertThrows(
                InvalidValue.class,
                () -> messages.service.delete(EntityKey.<MessagesRecord>builder()
                        .set(MESSAGES.SESSION_ID, sessionsRecord.getId())
                        .set(MESSAGES.TICK_NUM, lastMessage.getTickNum() - 3) //Here we change the tick num
                        .build()
                ),
                "Error deleting last message"
        );
        Optional<CurrentLocationsRecord> actualLocation = currentLocationTestContext.service.find(currentLocationsKey);
        assertTrue(actualLocation.isPresent(), "Character has no current location");

        assertEquals(nextLocation.getId(), actualLocation.get().getLocationId(), "Character is still at next location");
        assertNotEquals(previousLocation.getId(), actualLocation.get().getLocationId(), "Didn't move back to previous location");
    }

    @Test
    void onNewMessageInjectUserLocation() {
        MessageTestContext.Context context = messages.createSessionWithMessages(100, 100);
        CharactersRecord userCharacter = context.sessionContext().userCharacter();
        LocationsRecord currentUserLocation = movements.getLocationOf(userCharacter, context.sessionContext().session());

        MessagesRecord createdMessage = messages.service.createAndGet(
                EntityDataPayload.<MessagesRecord>builder()
                        .set(MESSAGES.SESSION_ID, context.sessionContext().session().getId())
                        .set(MESSAGES.ROLE, ChatCompletionRole.USER.wireValue())
                        .set(MESSAGES.CONTENT, "Test")
                        .build()
        );

        assertEquals(currentUserLocation.getId(), createdMessage.getLocationId(), "Message is not at user location");
    }

    @Test
    void onNewResponseRegisterLocation() {
        MessageTestContext.Context context = messages.createSessionWithMessages(100, 100);
        SessionsRecord sessionsRecord = context.sessionContext().session();
        CharactersRecord userCharacter = context.sessionContext().userCharacter();
        LocationsRecord currentUserLocation = movements.getLocationOf(userCharacter, context.sessionContext().session());

        MessagesRecord lastMessage = messages.service.getLastOf(context.sessionContext().session());
        EntityKey<MessagesRecord> lastMessageKey = messages.service.keyOf(lastMessage);

        //Necessary cause you can't add responses to user messages
        messages.service.update(lastMessageKey, EntityDataPayload.of(MESSAGES.ROLE, ChatCompletionRole.ASSISTANT.wireValue()));
        messages.service.registerNewResponse(sessionsRecord.getId(), lastMessage.getTickNum(), "A");

        ResponsesRecord activeResponse = messages.service.getActiveResponseOf(lastMessage);

        assertEquals(currentUserLocation.getId(), activeResponse.getLocationId(), "Message is not at user location");
    }

    @Test
    void onActiveResponseChange_singleUserTwoResponses() {
        MessageTestContext.Context context = messages.createSessionWithMessages(100, 100);

        SessionsRecord sessionsRecord = context.sessionContext().session();
        CharactersRecord userCharacter = context.sessionContext().userCharacter();
        List<LocationsRecord> locationsRecordList = context.sessionContext().sessionLocations();
        LocationsRecord startingUserLocation = movements.getLocationOf(userCharacter, sessionsRecord);
        MessagesRecord lastMessage = messages.service.getLastOf(sessionsRecord);
        messages.service.update(
                messages.service.keyOf(lastMessage),
                EntityDataPayload.of(MESSAGES.ROLE, ChatCompletionRole.ASSISTANT.wireValue())
        );

        ResponsesRecord firstResponse = messages.service.getActiveResponseOf(lastMessage);
        edgeTestContext.linkLinear(context.sessionContext().sessionLocations());

        int sessionId = sessionsRecord.getId();
        int characterId = userCharacter.getId();
        LocationsRecord location1 = locationsRecordList.get(1);
        LocationsRecord location2 = locationsRecordList.get(2);

        //First response
        movements.move(sessionId, characterId, location1.getId());
        movements.move(sessionId, characterId, location2.getId());
        assertEquals(location2.getId(), movements.getLocationOf(userCharacter, sessionsRecord).getId());

        messages.service.registerNewResponse(sessionsRecord.getId(), lastMessage.getTickNum(), "A");
        LocationsRecord actualRollbackLocation = movements.getLocationOf(userCharacter, sessionsRecord);
        assertEquals(startingUserLocation.getId(), actualRollbackLocation.getId(),
                "Mismatch after rollback. Expected \n%s. \n Got: \n%s".formatted(startingUserLocation, actualRollbackLocation));

        //Second response
        movements.move(sessionId, characterId, location1.getId());
        assertEquals(location1.getId(), movements.getLocationOf(userCharacter, sessionsRecord).getId());

        messages.service.registerNewResponse(sessionsRecord.getId(), lastMessage.getTickNum(), "A");
        LocationsRecord secondRollbackLocation = movements.getLocationOf(userCharacter, sessionsRecord);
        assertEquals(startingUserLocation.getId(), secondRollbackLocation.getId(),
                "Mismatch after rollback. Expected \n%s. \n Got: \n%s".formatted(startingUserLocation, actualRollbackLocation));

        //Change response back to first
        EntityKey<MessagesRecord> messageKey = EntityKey.<MessagesRecord>builder()
                .set(MESSAGES.SESSION_ID, sessionId)
                .set(MESSAGES.TICK_NUM, lastMessage.getTickNum())
                .build();
        messages.service.update(messageKey, EntityDataPayload.of(MESSAGES.ACTIVE_RESPONSE, firstResponse.getResponseNum())
        );

        assertEquals(location2.getId(), movements.getLocationOf(userCharacter, sessionsRecord).getId());

        //Change response to second
        messages.service.update(messageKey,
                EntityDataPayload.of(MESSAGES.ACTIVE_RESPONSE, (short) (firstResponse.getResponseNum() + 1))
        );
        assertEquals(location1.getId(), movements.getLocationOf(userCharacter, sessionsRecord).getId());
    }

}