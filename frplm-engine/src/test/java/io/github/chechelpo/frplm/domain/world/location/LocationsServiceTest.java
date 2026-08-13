package io.github.chechelpo.frplm.domain.world.location;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookService;
import io.github.chechelpo.frplm.domain.world.region.RegionService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import io.github.chechelpo.frplm.test_utils.fixtures.*;
import io.github.chechelpo.frplm.domain.world.core.WorldService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import io.github.chechelpo.frplm.test_annotations.SimulithIntegrationTest;
import io.github.chechelpo.frplm.utils.stable_records.StableRecordCreator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static io.github.chechelpo.frplm.jooq.generated.Tables.*;

@SpringBootTest
class LocationsServiceTest {
    @Autowired
    private LocationsService locationsService;
    @Autowired
    private StableRecordCreator stableRecordCreator;

    @Autowired
    EntityFixtureFactory fixtureFactory;

    @BeforeEach
    void setUp() {
        stableRecordCreator.run();
    }

    @Test
    @SimulithIntegrationTest
    void testLocationLorebook_born_Updated_Killed_WithParent() {
        String seed = "location-lorebook-test";
        LorebookFixtures lorebookFixtures = fixtureFactory.lorebook(seed);
        fixtureFactory.locations(seed).addAndCreateList(
                100,
                i ->
                        EntityDataPayload.<LocationsRecord>builder()
                                .set(LOCATIONS.NAME, "location " + i)
        ).forEach(
                created -> {
                    // Create
                    EntityKey<LorebooksRecord> lorebookKey = EntityKey.of(LOREBOOKS.ID, created.getLorebookId());
                    lorebookFixtures.assertFieldEquals(created.getName(), LOREBOOKS.NAME, lorebookKey);

                    // Update
                    String newName = "newName of " + created.getName();
                    locationsService.update(created, EntityDataPayload.of(LOCATIONS.NAME, newName));
                    lorebookFixtures.assertFieldEquals(newName, LOREBOOKS.NAME, lorebookKey);

                    // Delete
                    locationsService.delete(created);
                    lorebookFixtures.assertDoesNotExist(lorebookKey);
                }
        );
    }
}