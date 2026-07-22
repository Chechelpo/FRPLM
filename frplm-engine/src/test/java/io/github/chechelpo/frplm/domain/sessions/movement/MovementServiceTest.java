package io.github.chechelpo.frplm.domain.sessions.movement;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityReader;
import io.github.chechelpo.frplm.domain.character.core.CharacterCoreTestContext;
import io.github.chechelpo.frplm.domain.character.starting_locations.StartingLocationTestContext;
import io.github.chechelpo.frplm.domain.sessions.core.SessionTestContext;
import io.github.chechelpo.frplm.domain.sessions.messages.MessageService;
import io.github.chechelpo.frplm.domain.sessions.messages.MessageTestContext;
import io.github.chechelpo.frplm.domain.world.edge.EdgeTestContext;
import io.github.chechelpo.frplm.domain.world.location.LocationTestContext;
import io.github.chechelpo.frplm.jooq.generated.tables.records.*;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static io.github.chechelpo.frplm.domain.sessions.messages.MessageService.FIRST_MESSAGE_TICK_NUM;
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
class MovementServiceTest {
    @Autowired
    CurrentLocationTestContext currentLocationTestContext;
    @Autowired
    private EdgeTestContext edgeTestContext;
    @Autowired
    private MovementService movementService;
    @Autowired
    private MovementFieldsHelper fields;
    @Autowired
    private MessageService messageService;

    @BeforeEach
    void setUp() {
        currentLocationTestContext.reload();
    }

    @Test
    void movements_areRegistered() {
        int locationAmount = 100;
        MessageTestContext.Context context = currentLocationTestContext.messages.createSessionWithMessages(locationAmount, 2);

        SessionsRecord thisSession = context.sessionContext().session();
        CharactersRecord userCharacter = context.sessionContext().userCharacter();
        List<LocationsRecord> locations = context.sessionContext().sessionLocations();
        edgeTestContext.linkLinear(locations);

        EntityKey<CurrentLocationsRecord> currentLocationKey = EntityKey.<CurrentLocationsRecord>builder()
                .set(CURRENT_LOCATIONS.SESSION_ID, thisSession.getId())
                .set(CURRENT_LOCATIONS.CHARACTER_ID, userCharacter.getId())
                .build();

        LocationsRecord previousLocation = locations.getFirst();
        for (int i = 1; i < locations.size(); i++) {
            LocationsRecord nextLocation = locations.get(i);
            MessagesRecord newMessage = currentLocationTestContext.messages.service.createAndGet(
                    EntityDataPayload.<MessagesRecord>builder()
                            .set(MESSAGES.SESSION_ID, thisSession.getId())
                            .set(MESSAGES.ROLE, ChatCompletionRole.USER.wireValue())
                            .set(MESSAGES.CONTENT, "Message")
                            .build()
            );
            currentLocationTestContext.service.update(currentLocationKey,
                    EntityDataPayload.<CurrentLocationsRecord>builder()
                            .set(CURRENT_LOCATIONS.WORLD_ID, thisSession.getWorldId())
                            .set(CURRENT_LOCATIONS.LOCATION_ID, nextLocation.getId())
                            .build()
            );
            EntityReader.FindResult<MovementsRecord> movement = movementService.find(EntityKey.<MovementsRecord>builder()
                    .set(MOVEMENTS.SESSION_ID, thisSession.getId())
                    .set(MOVEMENTS.CHARACTER_ID, userCharacter.getId())
                    .set(MOVEMENTS.AT_TICK, newMessage.getTickNum())
                    .build()
            );

            assertTrue(movement.isFound(), "No movement registered");
            assertEquals(previousLocation.getId(), movement.get().getPreviousLocationId(), "Movement location id mismatch");
            assertEquals(userCharacter.getId(), movement.get().getCharacterId(), "Movement character id mismatch");
            assertEquals(newMessage.getTickNum(), movement.get().getAtTick(), "Movement at tick mismatch");

            previousLocation = nextLocation;
        }
    }


    @Test
    void movements_inSameTickAreIgnored() {
        int locationAmount = 100;
        MessageTestContext.Context context = currentLocationTestContext.messages.createSessionWithMessages(locationAmount, 2);

        SessionsRecord thisSession = context.sessionContext().session();
        CharactersRecord userCharacter = context.sessionContext().userCharacter();

        List<LocationsRecord> locations = context.sessionContext().sessionLocations();
        edgeTestContext.linkLinear(locations);

        LocationsRecord currentLocation = locations.getFirst();

        CurrentLocationsRecord movementRecord = new CurrentLocationsRecord();

        movementRecord.setSessionId(thisSession.getId());
        movementRecord.setTickNum(FIRST_MESSAGE_TICK_NUM);
        movementRecord.setWorldId(thisSession.getWorldId());
        movementRecord.setLocationId(currentLocation.getId());
        movementRecord.setCharacterId(userCharacter.getId());

        //We first register that this character was moved
        movementService.registerMovementChange(movementRecord, FIRST_MESSAGE_TICK_NUM);

        for (int i = 1; i < locations.size(); i++) {
            LocationsRecord nextLocation = locations.get(i);

            movementRecord.setLocationId(currentLocation.getId());
            // Next calls should preserve the first location (the one before this loop)
            movementService.registerMovementChange(movementRecord, FIRST_MESSAGE_TICK_NUM);

            currentLocation = nextLocation;
        }

        EntityReader.FindResult<MovementsRecord> movementsRecord = movementService.find(
                EntityKey.<MovementsRecord>builder()
                        .set(MOVEMENTS.SESSION_ID, thisSession.getId())
                        .set(MOVEMENTS.CHARACTER_ID, userCharacter.getId())
                        .set(MOVEMENTS.AT_TICK, FIRST_MESSAGE_TICK_NUM)
                        .build()
        );

        assertTrue(movementsRecord.isFound(), "No movement record registered");
        assertEquals(locations.getFirst().getId(), movementsRecord.get().getPreviousLocationId(), "Movement location id mismatch");
    }

    @Test
    void rollbackLatestMovementsOf_singleCharacter() {
        int locationAmount = 100;
        MessageTestContext.Context context = currentLocationTestContext.messages.createSessionWithMessages(locationAmount, 2);

        SessionsRecord thisSession = context.sessionContext().session();
        CharactersRecord userCharacter = context.sessionContext().userCharacter();

        List<LocationsRecord> locations = context.sessionContext().sessionLocations();
        edgeTestContext.linkLinear(locations);

        LocationsRecord currentLocation = locations.getFirst();

        CurrentLocationsRecord movementRecord = new CurrentLocationsRecord();

        movementRecord.setSessionId(thisSession.getId());
        movementRecord.setTickNum(FIRST_MESSAGE_TICK_NUM);
        movementRecord.setWorldId(thisSession.getWorldId());
        movementRecord.setLocationId(currentLocation.getId());
        movementRecord.setCharacterId(userCharacter.getId());

        for (int i = 1; i < locations.size(); i++) {
            LocationsRecord nextLocation = locations.get(i);

            movementRecord.setLocationId(currentLocation.getId());
            movementService.registerMovementChange(movementRecord, 2);

            currentLocation = nextLocation;
        }

        movementService.rollbackLatestMovementsOf(thisSession.getId(), 2);
        assertEquals(
                currentLocationTestContext.service.getLocationOf(userCharacter, thisSession).getId(),
                locations.getFirst().getId(),
                "Movement location id mismatch"
        );
    }

    @Test
    void rollbackLatestMovementsOf_multipleCharacters() {
        int locationAmount = 100;
        int charactersPerLocation = 4;

        MessageTestContext.Context context =
                currentLocationTestContext.messages.createSessionWithMessages(
                        locationAmount,
                        2,
                        charactersPerLocation
                );

        SessionsRecord session = context.sessionContext().session();
        List<LocationsRecord> locations =
                context.sessionContext().sessionLocations();

        edgeTestContext.linkLinear(locations);

        LocationsRecord initialLocation = locations.getFirst();

        CharactersRecord[] characters =
                currentLocationTestContext.service.getAtLocation(
                        session.getId(),
                        initialLocation.getId()
                );

        assertEquals(charactersPerLocation, characters.length);

        for (CharactersRecord character : characters) {
            CurrentLocationsRecord current = new CurrentLocationsRecord();

            current.setSessionId(session.getId());
            current.setTickNum(FIRST_MESSAGE_TICK_NUM);
            current.setWorldId(session.getWorldId());
            current.setCharacterId(character.getId());

            LocationsRecord previousLocation = initialLocation;

            for (int i = 1; i < locations.size(); i++) {
                current.setLocationId(previousLocation.getId());

                movementService.registerMovementChange(current, 2);

                previousLocation = locations.get(i);
            }
        }

        movementService.rollbackLatestMovementsOf(
                session.getId(),
                2
        );

        CharactersRecord[] rolledBackCharacters =
                currentLocationTestContext.service.getAtLocation(
                        session.getId(),
                        initialLocation.getId()
                );

        Set<Integer> expectedCharacterIds = Arrays.stream(characters)
                .map(CharactersRecord::getId)
                .collect(Collectors.toSet());

        Set<Integer> actualCharacterIds = Arrays.stream(rolledBackCharacters)
                .map(CharactersRecord::getId)
                .collect(Collectors.toSet());

        assertEquals(expectedCharacterIds, actualCharacterIds);
    }
}