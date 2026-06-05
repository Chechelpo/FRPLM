package chechelpo.frplm.domain.character.starting_locations;

import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.domain.character.core.CharacterCoreTestContext;
import chechelpo.frplm.domain.world.location.LocationTestContext;
import chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import chechelpo.frplm.jooq.generated.tables.records.StartingLocationsRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static chechelpo.frplm.jooq.generated.Tables.STARTING_LOCATIONS;
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

    @Autowired StartingLocationsService service;
    @Autowired StartingLocationFieldsHelper fieldsHelper;

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
}