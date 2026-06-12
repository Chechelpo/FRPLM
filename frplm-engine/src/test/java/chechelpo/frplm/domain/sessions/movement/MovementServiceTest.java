package chechelpo.frplm.domain.sessions.movement;

import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.domain.character.core.CharacterCoreTestContext;
import chechelpo.frplm.domain.character.starting_locations.StartingLocationTestContext;
import chechelpo.frplm.domain.sessions.core.SessionTestContext;
import chechelpo.frplm.domain.sessions.messages.core.MessageTestContext;
import chechelpo.frplm.domain.world.edge.EdgeTestContext;
import chechelpo.frplm.domain.world.location.LocationTestContext;
import chechelpo.frplm.jooq.generated.tables.records.*;
import chechelpo.frplm.openai_compatible.ChatCompletionRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Optional;

import static chechelpo.frplm.jooq.generated.Tables.*;
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
            Optional<MovementsRecord> movement = movementService.find(EntityKey.<MovementsRecord>builder()
                    .set(MOVEMENTS.SESSION_ID, thisSession.getId())
                    .set(MOVEMENTS.CHARACTER_ID, userCharacter.getId())
                    .set(MOVEMENTS.AT_TICK, newMessage.getTickNum())
                    .build()
            );

            assertTrue(movement.isPresent(), "No movement registered");
            assertEquals(previousLocation.getId(), movement.get().getLocationId(), "Movement location id mismatch");
            assertEquals(userCharacter.getId(), movement.get().getCharacterId(), "Movement character id mismatch");
            assertEquals(newMessage.getTickNum(), movement.get().getAtTick(), "Movement at tick mismatch");

            previousLocation = nextLocation;
        }
    }
}