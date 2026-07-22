package io.github.chechelpo.frplm.domain.sessions.movement;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.domain.character.core.CharacterCoreTestContext;
import io.github.chechelpo.frplm.domain.character.starting_locations.StartingLocationTestContext;
import io.github.chechelpo.frplm.domain.sessions.core.SessionTestContext;
import io.github.chechelpo.frplm.domain.sessions.messages.MessageTestContext;
import io.github.chechelpo.frplm.domain.world.edge.EdgeTestContext;
import io.github.chechelpo.frplm.domain.world.location.LocationTestContext;
import io.github.chechelpo.frplm.jooq.generated.tables.records.*;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionRole;
import org.assertj.core.util.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import java.util.*;

import static io.github.chechelpo.frplm.jooq.generated.Tables.*;
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
class CurrentLocationServiceTest {
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

    @BeforeEach
    void setUp() {
        messages.reload();
        locationTestContext.reload();
    }


    @Test
    void rewindLocationsToRestoresPreviousCurrentLocations() {
        int locationAmount = 6;

        SessionTestContext.SessionContext sessionContext =
                sessionTestContext.createSession(locationAmount, 2);

        SessionsRecord session = sessionContext.session();
        CharactersRecord userCharacter = sessionContext.userCharacter();
        List<LocationsRecord> locations = sessionContext.sessionLocations();

        edgeTestContext.linkLinear(locations);

        LocationsRecord location0 =
                currentLocationTestContext.service.getLocationOf(userCharacter, session);

        int location0Index = -1;
        for (int i = 0; i < locations.size(); i++) {
            if (locations.get(i).getId().equals(location0.getId())) {
                location0Index = i;
                break;
            }
        }

        assertTrue(location0Index >= 0);
        assertTrue(location0Index + 3 < locations.size());

        LocationsRecord location1 = locations.get(location0Index + 1);
        LocationsRecord location2 = locations.get(location0Index + 2);
        LocationsRecord location3 = locations.get(location0Index + 3);

        MessagesRecord tick1Message = messages.service.createAndGet(
                EntityDataPayload.<MessagesRecord>builder()
                        .set(MESSAGES.SESSION_ID, session.getId())
                        .set(MESSAGES.WORLD_ID, session.getWorldId())
                        .set(MESSAGES.LOCATION_ID, location0.getId())
                        .set(MESSAGES.ROLE, ChatCompletionRole.ASSISTANT.wireValue())
                        .set(MESSAGES.CONTENT, "tick 1")
                        .build()
        );

        assertTrue(currentLocationTestContext.move(
                userCharacter.getId(),
                location1.getWorldId(),
                tick1Message.getTickNum(),
                location1.getId(),
                session.getId()
        ));

        MessagesRecord tick2Message = messages.service.createAndGet(
                EntityDataPayload.<MessagesRecord>builder()
                        .set(MESSAGES.SESSION_ID, session.getId())
                        .set(MESSAGES.WORLD_ID, session.getWorldId())
                        .set(MESSAGES.LOCATION_ID, location1.getId())
                        .set(MESSAGES.ROLE, ChatCompletionRole.ASSISTANT.wireValue())
                        .set(MESSAGES.CONTENT, "tick 2")
                        .build()
        );

        assertTrue(currentLocationTestContext.move(
                userCharacter.getId(),
                location2.getWorldId(),
                tick2Message.getTickNum(),
                location2.getId(),
                session.getId()
        ));

        MessagesRecord tick3Message = messages.service.createAndGet(
                EntityDataPayload.<MessagesRecord>builder()
                        .set(MESSAGES.SESSION_ID, session.getId())
                        .set(MESSAGES.WORLD_ID, session.getWorldId())
                        .set(MESSAGES.LOCATION_ID, location2.getId())
                        .set(MESSAGES.ROLE, ChatCompletionRole.ASSISTANT.wireValue())
                        .set(MESSAGES.CONTENT, "tick 3")
                        .build()
        );

        assertTrue(currentLocationTestContext.move(
                userCharacter.getId(),
                location3.getWorldId(),
                tick3Message.getTickNum(),
                location3.getId(),
                session.getId()
        ));

        assertEquals(
                location3.getId(),
                currentLocationTestContext.service.getLocationOf(userCharacter, session).getId()
        );

        currentLocationTestContext.service.rollbackLocationsTo(
                session.getId(),
                tick1Message.getTickNum()
        );

        assertEquals(
                location1.getId(),
                currentLocationTestContext.service.getLocationOf(userCharacter, session).getId(),
                "Rewinding to tick1 should restore the location after tick1"
        );
    }

    @Test
    void normalMovementLifecycle() {
        int locationAmount = 10;
        int charactersPerLocation = 3;
        SessionTestContext.SessionContext sessionContext = sessionTestContext.createSession(locationAmount, charactersPerLocation);
        SessionsRecord session = sessionContext.session();
        CharactersRecord userCharacter = sessionContext.userCharacter();
        List<LocationsRecord> locationsOfSession = sessionContext.sessionLocations();
        edgeTestContext.linkLinear(locationsOfSession);

        LocationsRecord currentLocation = currentLocationTestContext.service.getLocationOf(userCharacter, session);
        for (int i = 0; i < locationAmount; i++) {
            LocationsRecord nextLocation = locationsOfSession.get(i);
            if (nextLocation.getId().equals(currentLocation.getId())) continue;
            MessagesRecord newMessage = messages.service.createAndGet(
                    EntityDataPayload.<MessagesRecord>builder()
                            .set(MESSAGES.SESSION_ID, session.getId())
                            .set(MESSAGES.WORLD_ID, session.getWorldId())
                            .set(MESSAGES.LOCATION_ID, currentLocation.getId())
                            .set(MESSAGES.ROLE, ChatCompletionRole.ASSISTANT.wireValue())
                            .set(MESSAGES.CONTENT, "content" + i)
                            .build()
            );
            assertTrue(currentLocationTestContext.move(
                            userCharacter.getId(),
                            nextLocation.getWorldId(),
                            newMessage.getTickNum(),
                            nextLocation.getId(),
                            session.getId()
                    ),
                    "Error moving user character"
            );
            assertEquals(currentLocationTestContext.service.getLocationOf(userCharacter, session).getId(), nextLocation.getId(),
                    "Character at wrong location"
            );
            CharactersRecord[] here = currentLocationTestContext.service.getAtLocation(session.getId(), nextLocation.getId());

            assertEquals(1 + charactersPerLocation, here.length);
            Set<Integer> hereSet = Sets.newHashSet(Arrays.stream(here).map(CharactersRecord::getId).toList());
            assertTrue(hereSet.contains(userCharacter.getId()));

            currentLocation = nextLocation;
        }
    }

    @Test
    void registeringSameMovementDoesNothing() {
        int messageAmount = 10;
        MessageTestContext.Context context = messages.createSessionWithMessages(100, messageAmount);
        SessionsRecord sessionsRecord = context.sessionContext().session();

        CharactersRecord character = context.sessionContext().userCharacter();
        LocationsRecord currentLocationOfUserCharacter = currentLocationTestContext.service.getLocationOf(character, sessionsRecord);
        MessagesRecord lastMessage = messages.service.getLastMessageOf(sessionsRecord);

        EntityKey<CurrentLocationsRecord> currentLocationsKey = EntityKey.<CurrentLocationsRecord>builder()
                .set(CURRENT_LOCATIONS.SESSION_ID, sessionsRecord.getId())
                .set(CURRENT_LOCATIONS.CHARACTER_ID, character.getId())
                .build();

        CurrentLocationsRecord previousRecord = currentLocationTestContext.service.find(currentLocationsKey)
                .orElseThrow();
        int previousTickNum = previousRecord.getTickNum();

        assertDoesNotThrow(() -> currentLocationTestContext.service.update(
                        currentLocationsKey,
                        EntityDataPayload.<CurrentLocationsRecord>builder()
                                .set(CURRENT_LOCATIONS.TICK_NUM, lastMessage.getTickNum())
                                .set(CURRENT_LOCATIONS.WORLD_ID, sessionsRecord.getWorldId())
                                .set(CURRENT_LOCATIONS.LOCATION_ID, currentLocationOfUserCharacter.getId())
                                .build()),
                "Error when moving user character"
        );

        CurrentLocationsRecord nextCurrentLocationsRecord = currentLocationTestContext.service.find(currentLocationsKey)
                .orElseThrow(notFound -> new IllegalStateException("No current location found for user character"));

        assertEquals(previousTickNum, nextCurrentLocationsRecord.getTickNum(),
                "Moving to same location should return the same tick number as its not a movement");
    }

    @Test
    void movingUserMovesMessageLocation() {
        int messageAmount = 10;
        MessageTestContext.Context context = messages.createSessionWithMessages(100, messageAmount);
        SessionsRecord sessionsRecord = context.sessionContext().session();

        CharactersRecord character = context.sessionContext().userCharacter();
        LocationsRecord currentLocationOfUserCharacter = currentLocationTestContext.service.getLocationOf(character, sessionsRecord);
        LocationsRecord nextLocation = context.sessionContext().sessionLocations().stream()
                .filter(location -> !location.getId().equals(currentLocationOfUserCharacter.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No location found to test a movement"));
        edgeTestContext.link(sessionsRecord.getWorldId(), currentLocationOfUserCharacter.getId(), nextLocation.getId());

        MessagesRecord lastMessage = messages.service.getLastMessageOf(sessionsRecord);
        MessagesRecord finalLastMessage = lastMessage;
        assertDoesNotThrow(() -> currentLocationTestContext.service.update(
                        EntityKey.<CurrentLocationsRecord>builder()
                                .set(CURRENT_LOCATIONS.SESSION_ID, sessionsRecord.getId())
                                .set(CURRENT_LOCATIONS.CHARACTER_ID, character.getId())
                                .build(),
                        EntityDataPayload.<CurrentLocationsRecord>builder()
                                .set(CURRENT_LOCATIONS.TICK_NUM, finalLastMessage.getTickNum())
                                .set(CURRENT_LOCATIONS.WORLD_ID, sessionsRecord.getWorldId())
                                .set(CURRENT_LOCATIONS.LOCATION_ID, nextLocation.getId())
                                .build()),
                "Error when moving user character"
        );
        lastMessage = messages.service.getLastMessageOf(sessionsRecord);

        assertEquals(nextLocation.getId(), lastMessage.getLocationId(), "Last message didn't change location");
    }

    @Test
    void startingLocationsRegisteredOnNewSession() {
        int locationsAmount = 10;
        List<LocationsRecord> locations = locationTestContext.createAndGetTestLocationsOfSameWorld(locationsAmount);
        //Create other locations of other worlds
        for (int i = 0; i < 20; i++) {
            locationTestContext.createAndGetTestLocationsOfSameWorld(10);
        }
        int charactersPerLocation = 3;
        List<CharactersRecord> charactersRecords = characterCoreTestContext.createAndGetRecords(locationsAmount * charactersPerLocation);

        CharactersRecord userCharacter = charactersRecords.getLast();
        characterCoreTestContext.service.update(
                characterCoreTestContext.service.keyOf(userCharacter),
                EntityDataPayload.of(CHARACTERS.CAN_BE_USER, true)
        );

        HashMap<Integer, List<CharactersRecord>> locationIDToCharactersStarting = new HashMap<>(locationsAmount);
        int characterIndex = 0;
        for (LocationsRecord locationsRecord : locations) {
            List<CharactersRecord> charactersStartingHere = new ArrayList<>(charactersPerLocation);
            for (int i = characterIndex; i < characterIndex + charactersPerLocation; i++) {
                startingLocationTestContext.service.createAndGet(EntityDataPayload.<StartingLocationsRecord>builder()
                        .set(STARTING_LOCATIONS.WORLD_ID, locationsRecord.getWorldId())
                        .set(STARTING_LOCATIONS.LOCATION_ID, locationsRecord.getId())
                        .set(STARTING_LOCATIONS.CHARACTER_ID, charactersRecords.get(i).getId())
                        .build()
                );
                charactersStartingHere.add(charactersRecords.get(i));
            }
            characterIndex += charactersPerLocation;
            locationIDToCharactersStarting.put(locationsRecord.getId(), charactersStartingHere);
        }

        SessionsRecord newSession = sessionTestContext.service.createAndGet(EntityDataPayload.<SessionsRecord>builder()
                .set(SESSIONS.NAME, "SessionTest")
                .set(SESSIONS.USER_PERSONA_ID, userCharacter.getId())
                .set(SESSIONS.WORLD_ID, locations.getFirst().getWorldId())
                .build()
        );

        for (var loc : locationIDToCharactersStarting.entrySet()) {
            List<CharactersRecord> expectedCharactersThere = loc.getValue();
            LocationsRecord expectedLocation = locationTestContext.service.find(EntityKey.<LocationsRecord>builder()
                    .set(LOCATIONS.WORLD_ID, locations.getFirst().getWorldId())
                    .set(LOCATIONS.ID, loc.getKey())
                    .build()
            ).orElseThrow();

            for (CharactersRecord charactersRecord : expectedCharactersThere) {
                LocationsRecord actualLocation = currentLocationTestContext.service.getLocationOf(charactersRecord, newSession);
                assertEquals(actualLocation, expectedLocation);
            }

            CharactersRecord[] actualHere = currentLocationTestContext.service.getAtLocation(newSession.getId(), expectedLocation.getId());

            int[] actualHereIds = Arrays.stream(actualHere).mapToInt(CharactersRecord::getId).toArray();
            int[] expectedHereIds = expectedCharactersThere.stream().mapToInt(CharactersRecord::getId).toArray();
            Arrays.sort(actualHereIds);
            Arrays.sort(expectedHereIds);
            assertArrayEquals(expectedHereIds, actualHereIds,
                    "Expected: \n%s\nActual:\n%s".formatted(Arrays.toString(expectedHereIds), Arrays.toString(actualHereIds))
            );
        }
    }
}