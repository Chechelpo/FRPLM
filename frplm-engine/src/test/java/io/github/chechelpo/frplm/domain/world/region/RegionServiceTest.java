package io.github.chechelpo.frplm.domain.world.region;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookService;
import io.github.chechelpo.frplm.domain.world.core.WorldService;
import io.github.chechelpo.frplm.exceptions.runtime.InvalidValue;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import io.github.chechelpo.frplm.test_annotations.SimulithIntegrationTest;
import io.github.chechelpo.frplm.test_utils.fixtures.LorebookFixtures;
import io.github.chechelpo.frplm.test_utils.fixtures.RegionFixtures;
import io.github.chechelpo.frplm.test_utils.fixtures.WorldFixtures;
import io.github.chechelpo.frplm.utils.stable_records.StableRecordCreator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class RegionServiceTest {
    @Autowired
    private WorldService worldService;
    @Autowired
    private RegionService regionService;
    @Autowired
    private StableRecordCreator stableRecordCreator;
    @Autowired
    private LorebookService lorebookService;

    private WorldFixtures worldFixture;
    private RegionFixtures regionFixtures;
    private LorebookFixtures lorebookFixtures;

    @BeforeEach
    void setUp() {
        worldFixture = new WorldFixtures(worldService, "region-test");
        regionFixtures = new RegionFixtures(regionService, "region-test");
        lorebookFixtures = new LorebookFixtures(lorebookService, "region-test");
        stableRecordCreator.run();
    }

    @Test
    @SimulithIntegrationTest
    void regionLorebook_creates_updates_deletes_with_parent(){
        WorldsRecord world = worldFixture.addAndCreateTo(EntityDataPayload.empty());

        regionFixtures.addAndCreateList(
                100,
                i -> EntityDataPayload.<RegionRecord>builder()
                        .set(REGION.WORLD_ID, world.getId())
                        .set(REGION.NAME, "Region " + i)
        ).forEach(
                createdRegion -> {
                    EntityKey<LorebooksRecord> lorebookKey = EntityKey.of(LOREBOOKS.ID, createdRegion.getLorebookId());
                    lorebookFixtures.assertFieldEquals(createdRegion.getName(), LOREBOOKS.NAME, lorebookKey);

                    String newName = "newName";
                    regionService.update(
                            REGION.NAME, newName,
                            createdRegion
                    );
                    RegionRecord updatedRecord = regionService.requireUpToDate(createdRegion);;
                    assertEquals(newName, updatedRecord.getName());

                    lorebookFixtures.assertFieldEquals(newName, LOREBOOKS.NAME, lorebookKey);
                    regionService.delete(createdRegion);

                    lorebookFixtures.assertDoesNotExist(lorebookKey);
                }
        );
    }

    @Test
    void update_cannotCreateCycles() {
        WorldsRecord world = worldFixture.addAndCreateTo(EntityDataPayload.of(WORLDS.NAME, "world"));

        List<RegionRecord> regions =  regionFixtures.addAndCreateList(
                3,
                i -> EntityDataPayload.<RegionRecord>builder()
                        .set(REGION.NAME, "Region " + i)
                        .set(REGION.WORLD_ID, world.getId())
        );
        RegionRecord parent = regions.getFirst();
        RegionRecord child = regions.get(1);
        RegionRecord asshole = regions.get(2);

        regionFixtures.makeParent(parent, child);
        regionFixtures.makeParent(child, asshole);

        assertThrows(
                InvalidValue.class,
                () -> regionFixtures.makeParent(asshole, parent)
        );
    }
}