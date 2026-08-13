package io.github.chechelpo.frplm.domain.world.core;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import io.github.chechelpo.frplm.test_annotations.SimulithIntegrationTest;
import io.github.chechelpo.frplm.utils.stable_records.StableRecordCreator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static io.github.chechelpo.frplm.jooq.generated.Tables.LOREBOOKS;
import static io.github.chechelpo.frplm.jooq.generated.Tables.WORLDS;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WorldServiceTest {
    @Autowired
    WorldService worldService;
    @Autowired
    LorebookService lorebookService;
    @Autowired
    private StableRecordCreator stableRecordCreator;

    @BeforeEach
    void setUp() {
        stableRecordCreator.run();
    }

    @Test
    @SimulithIntegrationTest
    public void testWorldLorebook_born_Updated_Killed_WithParent(){
        String worldName = "name";
        WorldsRecord world = worldService.createAndGet(
                EntityDataPayload.of(WORLDS.NAME, worldName)
        );
        assertEquals(worldName, world.getName());
        // Create
        var result = lorebookService.getOneMatching(
                EntityDataPayload.of(LOREBOOKS.NAME, worldName)
        );
        assertTrue(result.isPresent());
        assertEquals(worldName, result.resolve().getName());

        // Update
        String newName = "newName";
        worldService.update(world, EntityDataPayload.of(WORLDS.NAME, newName));
        world = worldService.getUpToDate(world).orElseThrow();
        assertEquals(newName, world.getName());

        var updatedLorebook = lorebookService.find(EntityKey.of(LOREBOOKS.ID, world.getLorebookId()));
        assertTrue(updatedLorebook.isFound());
        assertEquals(newName, updatedLorebook.get().getName());

        // Delete
        assertTrue(worldService.delete(world));

        var lorebook = lorebookService.find(
                EntityKey.of(LOREBOOKS.ID, world.getLorebookId())
        );

        assertFalse(lorebook.isFound());
    }
}