package chechelpo.frplm.domain.sessions.core;

import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.domain.character.core.CharacterCoreTestContext;
import chechelpo.frplm.domain.character.starting_locations.StartingLocationTestContext;
import chechelpo.frplm.domain.character.starting_locations.StartingLocationsService;
import chechelpo.frplm.domain.lorebook.core.LorebookTestContext;
import chechelpo.frplm.domain.world.location.LocationTestContext;
import chechelpo.frplm.exceptions.runtime.InvalidValue;
import chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import chechelpo.frplm.jooq.generated.tables.records.StartingLocationsRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static chechelpo.frplm.jooq.generated.Tables.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Sql(
        scripts = "classpath:db/schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Import({CharacterCoreTestContext.class, StartingLocationTestContext.class, LocationTestContext.class, SessionTestContext.class})
class SessionServiceTest {
    @Autowired
    StartingLocationTestContext startingLocations;
    @Autowired
    CharacterCoreTestContext characters;
    @Autowired
    LocationTestContext locations;
    @Autowired
    SessionTestContext sessions;
    @Autowired
    private StartingLocationsService startingLocationsService;

    @BeforeEach
    void setUp() {
        locations.reload();
        characters.reload();
    }

    @Test
    void create_throwsOnNonUserCharacter() {
        LocationsRecord locationRecord = locations.createAndGetTestLocationsOfSameWorld(1).getFirst();
        CharactersRecord characterRecord = characters.createAndGetRecords(1).getFirst();

        assertFalse(characterRecord.getCanBeUser(), "Test assumption of default value of can be user is false");

        startingLocations.service.createAndGet(EntityDataPayload.<StartingLocationsRecord>builder()
                .set(STARTING_LOCATIONS.WORLD_ID, locationRecord.getWorldId())
                .set(STARTING_LOCATIONS.LOCATION_ID, locationRecord.getId())
                .set(STARTING_LOCATIONS.CHARACTER_ID, characterRecord.getId())
                .build()
        );

        assertThrows(
                InvalidValue.class,
                () -> sessions.service.createAndGet(
                        EntityDataPayload.<SessionsRecord>builder()
                                .set(SESSIONS.WORLD_ID, locationRecord.getWorldId())
                                .set(SESSIONS.USER_PERSONA_ID, characterRecord.getId())
                                .build()
                ),
                "Could create a session with a non user character"
        );
    }
    @Test
    void create_throwsOnNonStartingLocationCharacter(){
        LocationsRecord locationRecord = locations.createAndGetTestLocationsOfSameWorld(1).getFirst();
        CharactersRecord characterRecord = characters.createAndGetRecords(1).getFirst();

        characters.service.update(
                characters.service.keyOf(characterRecord),
                EntityDataPayload.<CharactersRecord>builder()
                        .set(CHARACTERS.CAN_BE_USER, true)
                        .set(CHARACTERS.WELCOME_MESSAGE, "A")
                        .build()
        );

        assertThrows(
                InvalidValue.class,
                () -> sessions.service.createAndGet(
                        EntityDataPayload.<SessionsRecord>builder()
                                .set(SESSIONS.NAME, "test")
                                .set(SESSIONS.WORLD_ID, locationRecord.getWorldId())
                                .set(SESSIONS.USER_PERSONA_ID, characterRecord.getId())
                                .build()
                ),
                "Created a session with a user character with no starting location"
        );
    }

    @Test
    void create_DoesNotThrowOnValidInput(){
        LocationsRecord locationRecord = locations.createAndGetTestLocationsOfSameWorld(1).getFirst();
        CharactersRecord characterRecord = characters.createAndGetRecords(1).getFirst();

        startingLocationsService.createAndGet(
                EntityDataPayload.<StartingLocationsRecord>builder()
                        .set(STARTING_LOCATIONS.CHARACTER_ID, characterRecord.getId())
                        .set(STARTING_LOCATIONS.WORLD_ID, locationRecord.getWorldId())
                        .set(STARTING_LOCATIONS.LOCATION_ID, locationRecord.getId())
                        .build()
        );
        characters.service.update(
                characters.service.keyOf(characterRecord),
                EntityDataPayload.<CharactersRecord>builder()
                        .set(CHARACTERS.CAN_BE_USER, true)
                        .set(CHARACTERS.WELCOME_MESSAGE, "A")
                        .build()
        );

        assertDoesNotThrow(
                () -> sessions.service.createAndGet(
                EntityDataPayload.<SessionsRecord>builder()
                        .set(SESSIONS.NAME, "test")
                        .set(SESSIONS.WORLD_ID, locationRecord.getWorldId())
                        .set(SESSIONS.USER_PERSONA_ID, characterRecord.getId())
                        .build()
            ),
                "Could not create a session"
        );
    }
}