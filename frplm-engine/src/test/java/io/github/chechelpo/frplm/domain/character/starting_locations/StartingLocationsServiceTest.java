package io.github.chechelpo.frplm.domain.character.starting_locations;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.domain.character.core.CharacterCoreTestContext;
import io.github.chechelpo.frplm.domain.world.location.LocationTestContext;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.StartingLocationsRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.STARTING_LOCATIONS;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Sql(
        scripts = "classpath:db/schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Import({CharacterCoreTestContext.class, LocationTestContext.class})
class StartingLocationsServiceTest {
    @Autowired CharacterCoreTestContext characters;
    @Autowired LocationTestContext locations;

    @Autowired
    StartingLocationsService service;
    @Autowired
    StartingLocationFieldsHelper fieldsHelper;

    @BeforeEach
    void setUp() {
        characters.reload();
        locations.reload();
    }

    @Test
    void getStartingLocationsOf() {
        int characterAmount = 100;

        List<CharactersRecord> charactersRecords = characters.createAndGetRecords(characterAmount);
        List<LocationsRecord> locationsRecords = locations.createAndGetTestLocationsOfSameWorld(characterAmount);

        for (int i = 0; i < characterAmount; i++) {
            LocationsRecord locationRecord = locationsRecords.get(i);
            CharactersRecord charactersRecord = charactersRecords.get(i);

            service.createAndGet(EntityDataPayload.<StartingLocationsRecord>builder()
                            .set(STARTING_LOCATIONS.WORLD_ID, locationRecord.getWorldId())
                            .set(STARTING_LOCATIONS.LOCATION_ID, locationRecord.getId())
                            .set(STARTING_LOCATIONS.CHARACTER_ID, charactersRecord.getId())
                            .build()
            );

            List<LocationsRecord> startingLocations = service.getStartingLocationsOf(charactersRecord);
            assertEquals(1, startingLocations.size());
            LocationsRecord startingLocation = startingLocations.getFirst();
            assertEquals(startingLocation, locationRecord);

            assertEquals(service.startingLocationAt(charactersRecord, locationRecord.getWorldId()).getFirst(),
                    locationRecord,
                    "Expected: " + startingLocation  + " to be " + locationRecord
            );
        }
    }

    @Test
    void testStartingLocationsAtWorld(){
        LocationsRecord location1 = locations.createAndGetTestLocationsOfSameWorld(1).getFirst();
        LocationsRecord location2 = locations.createAndGetTestLocationsOfSameWorld(1).getFirst();

        assertNotEquals(location1.getWorldId(), location2.getWorldId(), "Expected world ids of locations to be different");

        CharactersRecord characterRecord = characters.createAndGetRecords(1).getFirst();

        assertDoesNotThrow(
                () -> service.createAndGet(EntityDataPayload.<StartingLocationsRecord>builder()
                    .set(STARTING_LOCATIONS.WORLD_ID, location1.getWorldId())
                    .set(STARTING_LOCATIONS.LOCATION_ID, location1.getId())
                    .set(STARTING_LOCATIONS.CHARACTER_ID, characterRecord.getId())
                    .build()
            ),
                "Error creating starting location record of location 1"
        );
        assertDoesNotThrow(
                () -> service.createAndGet(EntityDataPayload.<StartingLocationsRecord>builder()
                    .set(STARTING_LOCATIONS.WORLD_ID, location2.getWorldId())
                    .set(STARTING_LOCATIONS.LOCATION_ID, location2.getId())
                    .set(STARTING_LOCATIONS.CHARACTER_ID, characterRecord.getId())
                    .build()
            ),
                "Error creating starting locations record of location 2"
        );

        List<LocationsRecord> actualFromWorld1 = service.startingLocationAt(characterRecord, location1.getWorldId());
        List<LocationsRecord> actualFromWorld2 = service.startingLocationAt(characterRecord, location2.getWorldId());

        assertEquals(1, actualFromWorld1.size(), "Expected one location record");
        assertEquals(1, actualFromWorld2.size(), "Expected one location record");

        assertNotEquals(actualFromWorld1.getFirst(), actualFromWorld2.getFirst(), "Expected two different location records");
    }
}