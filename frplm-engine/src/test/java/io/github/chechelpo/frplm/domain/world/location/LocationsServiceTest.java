package io.github.chechelpo.frplm.domain.world.location;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookService;
import io.github.chechelpo.frplm.domain.world.region.RegionService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import io.github.chechelpo.frplm.test_utils.fixtures.LocationFixtures;
import io.github.chechelpo.frplm.test_utils.fixtures.LorebookFixtures;
import io.github.chechelpo.frplm.test_utils.fixtures.RegionFixtures;
import io.github.chechelpo.frplm.test_utils.fixtures.WorldFixtures;
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
    private WorldService worldService;
    @Autowired
    private LocationsService locationsService;
    @Autowired
    private LorebookService lorebookService;
    @Autowired
    private RegionService regionService;

    @Autowired
    private StableRecordCreator stableRecordCreator;

    private LocationFixtures locationFixtures;
    private WorldFixtures worldFixtures;
    private RegionFixtures regionFixtures;
    private LorebookFixtures lorebookFixtures;

    @BeforeEach
    void setUp() {
        worldFixtures = new WorldFixtures(worldService, "location-test");
        locationFixtures = new LocationFixtures(
                locationsService,
                "location-test"
        );
        regionFixtures = new RegionFixtures(
                regionService,
                "location-test"
        );
        lorebookFixtures = new LorebookFixtures(
                lorebookService,
                "location-test"
        );

        stableRecordCreator.run();
    }

    @Test
    @SimulithIntegrationTest
    void testLocationLorebook_born_Updated_Killed_WithParent() {
        WorldsRecord world = worldFixtures.addAndCreateTo(EntityDataPayload.empty());
        RegionRecord region = regionFixtures.addAndCreateTo(EntityDataPayload.of(REGION.WORLD_ID, world.getId()));

        locationFixtures.addAndCreateList(
                100,
                i ->
                        EntityDataPayload.<LocationsRecord>builder()
                                .set(LOCATIONS.WORLD_ID, world.getId())
                                .set(LOCATIONS.REGION_ID, region.getId())
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