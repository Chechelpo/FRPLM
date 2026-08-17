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

        @Test
        @SimulithIntegrationTest
        void test_deleteMessageRollsBackMovements() {
            String seed = "test-rollback-movements";

            SessionCharacterFixture characterFixtures =
                    entityFixtureFactory.sesCharacters(seed);

            WorldsRecord world = entityFixtureFactory.worlds(seed).createOne();

            List<LocationsRecord> locations = entityFixtureFactory.locations(seed)
                    .addAndCreateList(
                            3,
                            i -> EntityDataPayload.<LocationsRecord>builder()
                                    .set(LOCATIONS.WORLD_ID, world.getId())
                                    .set(LOCATIONS.NAME, "Location " + i)
                    );

            SessionCharactersRecord character = characterFixtures.createOne(
                    EntityDataPayload.<SessionCharactersRecord>builder()
                            .set(SESSION_CHARACTERS.WORLD_ID, world.getId())
                            .set(
                                    SESSION_CHARACTERS.CURRENT_LOCATION_ID,
                                    locations.getFirst().getId()
                            )
                            .build()
            );

            entityFixtureFactory.edges(seed).linkLinear(locations);

            MessagesRecord message = entityFixtureFactory.messages(seed)
                    .service()
                    .getLastMessageOf(character.getSessionId());

            int startingLocationId = locations.getFirst().getId();

            // A -> B
            characterFixtures.service()
                    .update(
                            SESSION_CHARACTERS.CURRENT_LOCATION_ID,
                            locations.get(1).getId(),
                            character
                    )
                    .orElseThrow();

            // B -> C
            characterFixtures.service()
                    .update(
                            SESSION_CHARACTERS.CURRENT_LOCATION_ID,
                            locations.get(2).getId(),
                            character
                    )
                    .orElseThrow();

            assertEquals(
                    locations.get(2).getId(),
                    characterFixtures.service()
                            .require(EntityKey.<SessionCharactersRecord>builder()
                                    .set(SESSION_CHARACTERS.ID, character.getId())
                                    .set(SESSION_CHARACTERS.SESSION_ID, character.getSessionId())
                                    .build()
                            )
                            .getCurrentLocationId()
            );

            // Deleting the message should trigger:
            // MovementEventReactor.onMessageDeletedRewindLocations(...)
            entityFixtureFactory.messages(seed)
                    .service()
                    .delete(message);

            SessionCharactersRecord rolledBackCharacter =
                    characterFixtures.service()
                            .require(
                                    EntityKey.<SessionCharactersRecord>builder()
                                            .set(SESSION_CHARACTERS.ID, character.getId())
                                            .set(SESSION_CHARACTERS.SESSION_ID, character.getSessionId())
                                            .build()
                            );

            assertEquals(
                    startingLocationId,
                    rolledBackCharacter.getCurrentLocationId()
            );
        }

        @Test
        @SimulithIntegrationTest
        void test_changeResponseAppliesLocationChanges() {
            String seed = "test-change-response-movements";

            SessionCharacterFixture characterFixtures =
                    entityFixtureFactory.sesCharacters(seed);
            MessageFixtures messageFixtures =
                    entityFixtureFactory.messages(seed);

            WorldsRecord world = entityFixtureFactory.worlds(seed).createOne();

            List<LocationsRecord> locations = entityFixtureFactory.locations(seed)
                    .addAndCreateList(
                            3,
                            i -> EntityDataPayload.<LocationsRecord>builder()
                                    .set(LOCATIONS.WORLD_ID, world.getId())
                                    .set(LOCATIONS.NAME, "Location " + i)
                    );

            LocationsRecord a = locations.get(0);
            LocationsRecord b = locations.get(1);
            LocationsRecord c = locations.get(2);

            SessionCharactersRecord character = characterFixtures.createOne(
                    EntityDataPayload.<SessionCharactersRecord>builder()
                            .set(SESSION_CHARACTERS.WORLD_ID, world.getId())
                            .set(SESSION_CHARACTERS.CURRENT_LOCATION_ID, a.getId())
                            .build()
            );

            entityFixtureFactory.edges(seed).linkLinear(locations);

            var messageService = messageFixtures.service();

            MessagesRecord message =
                    messageService.getLastMessageOf(character.getSessionId());

            EntityKey<MessagesRecord> messageKey =
                    EntityKey.<MessagesRecord>builder()
                            .set(MESSAGES.SESSION_ID, message.getSessionId())
                            .set(MESSAGES.TICK_NUM, message.getTickNum())
                            .build();

            EntityKey<SessionCharactersRecord> characterKey =
                    EntityKey.<SessionCharactersRecord>builder()
                            .set(SESSION_CHARACTERS.SESSION_ID, character.getSessionId())
                            .set(SESSION_CHARACTERS.ID, character.getId())
                            .build();

            short firstResponse = message.getActiveResponse();

            /*
             * Response 1:
             *
             * A -> B
             */
            characterFixtures.service()
                    .update(
                            SESSION_CHARACTERS.CURRENT_LOCATION_ID,
                            b.getId(),
                            character
                    )
                    .orElseThrow();

            assertEquals(
                    b.getId(),
                    characterFixtures.service()
                            .require(characterKey)
                            .getCurrentLocationId()
            );

            /*
             * Registering response 2 makes it active.
             *
             * Since response 2 has no movement changes yet,
             * switching to it should rollback response 1:
             *
             * B -> A
             */
            messageService.registerNewResponse(
                    message.getSessionId(),
                    message.getTickNum(),
                    "Alternative response",
                    null
            );

            message = messageService.require(messageKey);
            short secondResponse = message.getActiveResponse();

            assertEquals(firstResponse + 1, secondResponse);

            assertEquals(
                    a.getId(),
                    characterFixtures.service()
                            .require(characterKey)
                            .getCurrentLocationId()
            );

            /*
             * Response 2:
             *
             * A -> B -> C
             */
            character = characterFixtures.service().require(characterKey);

            characterFixtures.service()
                    .update(
                            SESSION_CHARACTERS.CURRENT_LOCATION_ID,
                            b.getId(),
                            character
                    )
                    .orElseThrow();

            character = characterFixtures.service().require(characterKey);

            characterFixtures.service()
                    .update(
                            SESSION_CHARACTERS.CURRENT_LOCATION_ID,
                            c.getId(),
                            character
                    )
                    .orElseThrow();

            assertEquals(
                    c.getId(),
                    characterFixtures.service()
                            .require(characterKey)
                            .getCurrentLocationId()
            );

            /*
             * Select response 1.
             *
             * Response 2 movement is rolled back:
             * C -> A
             *
             * Response 1 movement is then applied:
             * A -> B
             */
            messageService.update(
                    messageKey,
                    EntityDataPayload.of(
                            MESSAGES.ACTIVE_RESPONSE,
                            firstResponse
                    )
            ).orElseThrow();

            assertEquals(
                    b.getId(),
                    characterFixtures.service()
                            .require(characterKey)
                            .getCurrentLocationId()
            );

            /*
             * Select response 2 again.
             *
             * B -> A -> C
             */
            messageService.update(
                    messageKey,
                    EntityDataPayload.of(
                            MESSAGES.ACTIVE_RESPONSE,
                            secondResponse
                    )
            ).orElseThrow();

            assertEquals(
                    c.getId(),
                    characterFixtures.service()
                            .require(characterKey)
                            .getCurrentLocationId()
            );
        }
    }

}