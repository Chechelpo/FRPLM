package io.github.chechelpo.frplm.domain.world.location;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookTestContext;
import io.github.chechelpo.frplm.domain.world.region.RegionTestContext;
import io.github.chechelpo.frplm.exceptions.runtime.UnsupportedAction;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import io.github.chechelpo.frplm.test_utils.TestText;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import java.util.ArrayList;
import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.LOCATIONS;
import static io.github.chechelpo.frplm.jooq.generated.Tables.REGION;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Sql(
        scripts = "classpath:db/schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Import({LorebookTestContext.class, LocationTestContext.class, RegionTestContext.class})
class LocationsServiceTest {
    @Autowired
    LorebookTestContext lorebooks;
    @Autowired
    LocationTestContext locations;
    @Autowired
    RegionTestContext regions;

    @BeforeEach
    void setUp() {
        lorebooks.reload();
        locations.reload();
    }

    @Test
    void testLocationLorebookLifeCycle() {
        /*
        int locationAmount = 100;
        List<WorldsRecord> worldsRecords = locations.worldTestContext.createWorlds(locationAmount).createdRecords();
        List<EntityDataPayload<LocationsRecord>> locationsData = new ArrayList<>(locationAmount);
        RegionRecord regionRecord = locations.regionTestContext.createRegions(1).getFirst();
        long seed = 10;

        for (int i = 0; i < locationAmount; i++)
            locationsData.add(EntityDataPayload.<LocationsRecord>builder()
                    .set(LOCATIONS.WORLD_ID, worldsRecords.get(i).getId())
                    .set(LOCATIONS.REGION_ID, regionRecord.getId())
                    .set(LOCATIONS.NAME, TestText.randomText(seed + i, 0, 255))
                    .build()
            );

        List<LocationsRecord> records = locationsData.stream().map(
                data -> assertDoesNotThrow(() -> locations.service.createAndGet(data))
        ).toList();

        for (int i = 0; i < locationAmount; i++)
            assertEquals(lorebooks.service.getLorebookOf(records.get(i)).getName(), records.get(i).getName());

        for (int i = 0; i < locationAmount; i++) {
            LocationsRecord record = records.get(i);
            LorebooksRecord lorebook = lorebooks.service.getLorebookOf(record);

            assertTrue(this.locations.service.delete(locations.service.keyOf(record)), "Error deleting location");
            assertTrue(lorebooks.service.find(
                    lorebooks.service.keyOf(lorebook)
            ).isEmpty(), "Stale lorebook referencing location");
        }*/
    }

    @Test
    void cannotDeleteRegionIfParentOfLocations() {
        int locationAmount = 3;
        List<LocationsRecord> locationsRecords = locations.createAndGetTestLocationsOfSameWorld(locationAmount);
        int worldId = locationsRecords.getFirst().getWorldId();

        RegionRecord region = regions.service.createAndGet(EntityDataPayload.<RegionRecord>builder()
                .set(REGION.NAME, "region")
                .set(REGION.WORLD_ID, worldId)
                .build()
        );

        for (LocationsRecord loc : locationsRecords)
            assertTrue(
                    locations.service.update(
                            locations.service.keyOf(loc),
                            EntityDataPayload.of(LOCATIONS.REGION_ID, region.getId())
                    ).success(),
                    "Could not link location"
            );


        for (LocationsRecord loc : locationsRecords){
            assertThrows(
                    UnsupportedAction.class,
                    () -> regions.service.delete(regions.service.keyOf(region))
            );
            locations.service.delete(locations.service.keyOf(loc));
        }

        assertDoesNotThrow(
                () -> regions.service.delete(regions.service.keyOf(region))
        );
    }
}