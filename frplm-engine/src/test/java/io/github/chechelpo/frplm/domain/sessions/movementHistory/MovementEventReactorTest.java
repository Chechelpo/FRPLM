package io.github.chechelpo.frplm.domain.sessions.movementHistory;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.jooq.generated.tables.records.*;
import io.github.chechelpo.frplm.test_annotations.SimulithIntegrationTest;
import io.github.chechelpo.frplm.test_utils.comparators.RecordComparator;
import io.github.chechelpo.frplm.test_utils.fixtures.*;
import io.github.chechelpo.frplm.utils.stable_records.StableRecordCreator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Import(EntityFixtureFactory.class)
class MovementEventReactorTest {

    @Autowired
    private StableRecordCreator stableRecordCreator;
    @Autowired
    private EntityFixtureFactory entityFixtureFactory;
    @Autowired
    private MovementsService movementsService;
    @Autowired
    private ResponseMovementService responseMovementService;

    @BeforeEach
    void setUp(){
        stableRecordCreator.run();
    }

    @Nested
    class MovementHistory {
        @BeforeEach
        void setUp(){
            stableRecordCreator.run();
        }

        @Test
        @SimulithIntegrationTest
        void test_updateMovement(){
            String seed = "test-update-movements";
            MessageFixtures messageFixtures = entityFixtureFactory.messages(seed);
            SessionCharacterFixture sesCharacterFixture = entityFixtureFactory.sesCharacters(seed);

            WorldsRecord world = entityFixtureFactory.worlds(seed).createOne();
            List<LocationsRecord> locations = entityFixtureFactory.locations(seed)
                    .addAndCreateList(
                            20,
                            i -> EntityDataPayload.<LocationsRecord>builder()
                                    .set(LOCATIONS.WORLD_ID, world.getId())
                                    .set(LOCATIONS.NAME, "Location " + i)
                    );
            SessionCharactersRecord sessionCharacter = sesCharacterFixture.createOne(
                    EntityDataPayload.<SessionCharactersRecord>builder()
                            .set(SESSION_CHARACTERS.WORLD_ID, world.getId())
                            .set(SESSION_CHARACTERS.CURRENT_LOCATION_ID, locations.getFirst().getId())
                            .build()
            );
            assertEquals(world.getId(), sessionCharacter.getWorldId());

            MessagesRecord currentMessage = messageFixtures.service().getLastMessageOf(sessionCharacter.getSessionId());
            EdgesFixtures edges = entityFixtureFactory.edges(seed);
            assertEquals(world.getId(), currentMessage.getWorldId());

            EntityKey<MovementsRecord> movementKey = EntityKey.<MovementsRecord>builder()
                    .set(MOVEMENTS.SES_CHARACTER_ID, sessionCharacter.getId())
                    .set(MOVEMENTS.SESSION_ID, currentMessage.getSessionId())
                    .set(MOVEMENTS.AT_TICK, currentMessage.getTickNum())
                    .build();
            System.out.println(sessionCharacter);
            edges.linkLinear(locations);
            int startingLocationId = sessionCharacter.getCurrentLocationId();
            locations.forEach(
                    newLocation -> {
                        sesCharacterFixture.service()
                                .update(SESSION_CHARACTERS.CURRENT_LOCATION_ID, newLocation.getId(), sessionCharacter)
                                .orElseThrow();

                        var movementRecord = movementsService.require(movementKey);

                        // Location must stay the same pre-message (we are moving throughout the same message)
                        assertEquals(startingLocationId, movementRecord.getPreviousLocationId());

                        var responseRecord = responseMovementService.require(
                                EntityKey.<ResponseLocationChangesRecord>builder()
                                        .set(RESPONSE_LOCATION_CHANGES.SESSION_ID, MOVEMENTS.SESSION_ID, movementKey)
                                        .set(RESPONSE_LOCATION_CHANGES.SESSION_CHARACTER_ID, MOVEMENTS.SES_CHARACTER_ID, movementKey)
                                        .set(RESPONSE_LOCATION_CHANGES.RESPONSE_NUM, currentMessage.getActiveResponse())
                                        .set(RESPONSE_LOCATION_CHANGES.TICK_NUM, MOVEMENTS.AT_TICK, movementKey)
                                        .build()
                        );

                        assertEquals(newLocation.getId(), responseRecord.getLocationId());
                    }
            );
        }
    }

}