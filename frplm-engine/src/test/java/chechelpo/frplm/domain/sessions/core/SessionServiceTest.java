package chechelpo.frplm.domain.sessions.core;

import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.domain.character.core.CharacterCoreTestContext;
import chechelpo.frplm.domain.character.starting_locations.StartingLocationTestContext;
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

import static chechelpo.frplm.jooq.generated.Tables.SESSIONS;
import static chechelpo.frplm.jooq.generated.Tables.STARTING_LOCATIONS;
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

    @BeforeEach
    void setUp() {
        locations.reload();
        characters.reload();
    }

    @Test
    void throwsOnNonUserCharacter() {
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
}