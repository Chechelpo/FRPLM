package io.github.chechelpo.frplm.domain.lorebook.core;

import io.github.chechelpo.frplm.jooq.generated.tables.records.*;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.domain.character.core.CharacterCoreTestContext;
import io.github.chechelpo.frplm.domain.world.core.WorldTestContext;
import io.github.chechelpo.frplm.domain.world.location.LocationTestContext;
import io.github.chechelpo.frplm.domain.world.region.RegionTestContext;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Set;

import static io.github.chechelpo.frplm.jooq.generated.Tables.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Sql(
        scripts = "classpath:db/schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Import({LorebookTestContext.class, CharacterCoreTestContext.class, LocationTestContext.class, WorldTestContext.class, RegionTestContext.class})
class LorebookServiceTest {
    @Autowired
    LorebookTestContext testContext;
    @Autowired
    CharacterCoreTestContext characters;
    @Autowired
    private LocationTestContext locationTestContext;
    @Autowired
    private RegionTestContext regionTestContext;
    @Autowired
    private WorldTestContext worldTestContext;

    @BeforeEach
    void setUp() {
        testContext.reload();
    }

    @Test
    void getIndependent_doesNotReturnAssociatedLorebooks() {
        int characterAmount = 150;
        List<CharactersRecord> createdCharacters = characters.createAndGetRecords(characterAmount);
        Set<Integer> associatedLorebookIDs = new IntOpenHashSet(characterAmount);
        createdCharacters.forEach(character ->
                associatedLorebookIDs.add(testContext.service.getLorebookOf(character).getId())
        );

        int lorebookAmount = 20;
        String prefix = "lorebook";
        for (int i = 0; i < lorebookAmount; i++)
            //This could theoretically throw if somehow TestText creates a character with name prefix + i in the exact iteration.
            //If that happens, I'll owe you 100,000,000,000,000 (100 gorillion) USD
            testContext.service.createAndGet(EntityDataPayload.of(LOREBOOKS.NAME, prefix + "i"));



        List<LorebooksRecord> lorebooks = testContext.service.getIndependent();
        assertEquals(lorebookAmount, lorebooks.size());
        lorebooks.forEach(lorebook -> assertFalse(associatedLorebookIDs.contains(lorebook.getId()),
                        "Found associated lorebook in getAll call"
                ));
    }

    @Test
    void createDifferentLocations_doNotCollideInLorebook(){
        String locationName = "locationName";
        List<WorldsRecord> worlds = locationTestContext.worldTestContext.createWorlds(2).createdRecords();

        LocationsRecord location1 = locationTestContext.service.createAndGet(EntityDataPayload.<LocationsRecord>builder()
                        .set(LOCATIONS.WORLD_ID, worlds.getFirst().getId())
                        .set(LOCATIONS.NAME, locationName)
                        .build()
        );

        LocationsRecord location2 = locationTestContext.service.createAndGet(EntityDataPayload.<LocationsRecord>builder()
                                .set(LOCATIONS.WORLD_ID, worlds.get(1).getId())
                                .set(LOCATIONS.NAME, locationName)
                                .build()
        );

        LorebooksRecord lorebook1 = testContext.service.getLorebookOf(location1);
        LorebooksRecord lorebook2 = testContext.service.getLorebookOf(location2);

        assertNotEquals(lorebook1.getId(), lorebook2.getId());
    }

    @Test
    void updatingParentNameUpdatesLorebookName_Location(){
        LocationsRecord locationToUpdate = locationTestContext
                .createAndGetTestLocationsOfSameWorld(1).getFirst();
        assertEquals(
                locationToUpdate.getName(),
                testContext.service.getLorebookOf(locationToUpdate).getName(),
                "Starting name doesn't start as equals"
        );

        String newName = "LocationNewName";
        assertTrue(
                locationTestContext.service.update(
                    locationTestContext.service.keyOf(locationToUpdate),
                    EntityDataPayload.of(LOCATIONS.NAME, newName)
            )
        );
        assertEquals(
                newName,
                testContext.service.getLorebookOf(locationToUpdate).getName(),
                "Name not updated"
        );
    }
    @Test
    void updatingParentNameUpdatesLorebookName_World() {
        WorldsRecord worldToUpdate = worldTestContext
                .createWorlds(1)
                .createdRecords().getFirst();

        assertEquals(
                worldToUpdate.getName(),
                testContext.service.getLorebookOf(worldToUpdate).getName(),
                "World and lorebook names are not initially equal"
        );

        String newName = "WorldNewName";

        assertTrue(
                worldTestContext.service.update(
                        worldTestContext.service.keyOf(worldToUpdate),
                        EntityDataPayload.of(WORLDS.NAME, newName)
                ),
                "World update failed"
        );

        assertEquals(
                newName,
                testContext.service.getLorebookOf(worldToUpdate).getName(),
                "Lorebook name was not updated after changing the world name"
        );
    }

    @Test
    void updatingParentNameUpdatesLorebookName_Region() {
        RegionRecord regionToUpdate = regionTestContext
                .createRegions(1, 0)
                .getFirst();

        assertEquals(
                regionToUpdate.getName(),
                testContext.service.getLorebookOf(regionToUpdate).getName(),
                "Region and lorebook names are not initially equal"
        );

        String newName = "RegionNewName";

        assertTrue(
                regionTestContext.service.update(
                        regionTestContext.service.keyOf(regionToUpdate),
                        EntityDataPayload.of(REGION.NAME, newName)
                ),
                "Region update failed"
        );

        assertEquals(
                newName,
                testContext.service.getLorebookOf(regionToUpdate).getName(),
                "Lorebook name was not updated after changing the region name"
        );
    }

    @Test
    void updatingParentNameUpdatesLorebookName_Character() {
        CharactersRecord characterToUpdate = characters
                .createAndGetRecords(1)
                .getFirst();

        assertEquals(
                characterToUpdate.getName(),
                testContext.service.getLorebookOf(characterToUpdate).getName(),
                "Character and lorebook names are not initially equal"
        );

        String newName = "CharacterNewName";

        assertTrue(
                characters.service.update(
                        characters.service.keyOf(characterToUpdate),
                        EntityDataPayload.of(CHARACTERS.NAME, newName)
                ),
                "Character update failed"
        );

        assertEquals(
                newName,
                testContext.service.getLorebookOf(characterToUpdate).getName(),
                "Lorebook name was not updated after changing the character name"
        );
    }
}