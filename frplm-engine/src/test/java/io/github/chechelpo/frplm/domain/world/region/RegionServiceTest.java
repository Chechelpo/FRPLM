package io.github.chechelpo.frplm.domain.world.region;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.exceptions.runtime.InvalidValue;
import io.github.chechelpo.frplm.exceptions.runtime.UnsupportedAction;
import io.github.chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import java.util.ArrayList;
import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.REGION;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Sql(
        scripts = "classpath:db/schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Import(RegionTestContext.class)
class RegionServiceTest {
    @Autowired
    RegionTestContext testContext;

    @BeforeEach
    void setUp() {
        testContext.reload();
    }

    @Test
    void update_cannotCreateCycles() {
        WorldsRecord world = testContext.worlds.createWorlds(1).createdRecords().getFirst();
        int worldId = world.getId();
        int regionNum = 100;

        List<RegionRecord> createdRegions = new ArrayList<>(regionNum);
        for (int i = 0; i < regionNum; i++) {
            createdRegions.add(
                    testContext.service.createAndGet( //This throws cause of smthn on full test runs. not quite sure why.
                    EntityDataPayload.<RegionRecord>builder()    // When you test it in isolation, it's fine
                            .set(REGION.WORLD_ID, worldId)
                            .set(REGION.NAME, "Region number " + i)
                            .build()
            ));
        }

        // Simple test
        RegionRecord first = createdRegions.getFirst();
        RegionRecord secondRegion = createdRegions.get(1);

        assertDoesNotThrow(
                () -> testContext.service.update(testContext.service.keyOf(secondRegion),
                        EntityDataPayload.of(REGION.PARENT_REGION_ID, first.getId())
                ),
                "Could not link second with first"
        );
        assertThrows(
                InvalidValue.class,
                () -> testContext.service.update(testContext.service.keyOf(first),
                        EntityDataPayload.of(REGION.PARENT_REGION_ID, secondRegion.getId())
                ),
                "Could create cycle"
        );
        testContext.service.update(testContext.service.keyOf(secondRegion),
                EntityDataPayload.of(REGION.PARENT_REGION_ID, null)
        );

        for (int i = createdRegions.size() - 1; i > 0; i--) {
            RegionRecord childRegion = createdRegions.get(i);
            RegionRecord parentRegion = createdRegions.get(i - 1);

            assertDoesNotThrow(
                    () -> testContext.service.update(testContext.service.keyOf(childRegion),
                            EntityDataPayload.of(REGION.PARENT_REGION_ID, parentRegion.getId()))
            );
        }

        RegionRecord lastRegion = createdRegions.getLast();
        assertThrows(
                InvalidValue.class,
                () -> testContext.service.update(
                        testContext.service.keyOf(first),
                        EntityDataPayload.of(REGION.PARENT_REGION_ID, lastRegion.getId())
                )
        );
    }

    @Test
    void delete_CannotDeleteRegionIfHasChildrenRegions() {
        WorldsRecord world = testContext.worlds.createWorlds(1).createdRecords().getFirst();
        int worldId = world.getId();

        RegionRecord parent = testContext.service.createAndGet(EntityDataPayload.<RegionRecord>builder()
                .set(REGION.NAME, "parent")
                .set(REGION.WORLD_ID, worldId)
                .build()
        );
        RegionRecord childRegion1 = testContext.service.createAndGet(EntityDataPayload.<RegionRecord>builder()
                .set(REGION.NAME, "child 1")
                .set(REGION.WORLD_ID, worldId)
                .build()
        );
        RegionRecord childRegion2 = testContext.service.createAndGet(EntityDataPayload.<RegionRecord>builder()
                .set(REGION.NAME, "child 2")
                .set(REGION.WORLD_ID, worldId)
                .build()
        );

        assertDoesNotThrow(
                () -> testContext.service.update(
                        testContext.service.keyOf(childRegion1),
                        EntityDataPayload.of(REGION.PARENT_REGION_ID, parent.getId())
                )
        );
        assertDoesNotThrow(
                () -> testContext.service.update(
                        testContext.service.keyOf(childRegion2),
                        EntityDataPayload.of(REGION.PARENT_REGION_ID, parent.getId())
                )
        );

        assertThrows(
                UnsupportedAction.class,
                () -> testContext.service.delete(testContext.service.keyOf(parent))
        );
        testContext.service.update(testContext.service.keyOf(childRegion1), EntityDataPayload.of(REGION.PARENT_REGION_ID, null));
        assertThrows(
                UnsupportedAction.class,
                () -> testContext.service.delete(testContext.service.keyOf(parent))
        );
        testContext.service.update(testContext.service.keyOf(childRegion2), EntityDataPayload.of(REGION.PARENT_REGION_ID, null));
        assertDoesNotThrow(
                () -> testContext.service.delete(testContext.service.keyOf(parent))
        );
    }
}