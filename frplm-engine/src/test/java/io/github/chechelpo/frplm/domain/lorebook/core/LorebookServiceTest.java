package io.github.chechelpo.frplm.domain.lorebook.core;

import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.domain.character.core.CharacterCoreTestContext;
import chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import io.github.chechelpo.frplm.domain.world.location.LocationTestContext;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Set;

import static chechelpo.frplm.jooq.generated.Tables.LOCATIONS;
import static chechelpo.frplm.jooq.generated.Tables.LOREBOOKS;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Sql(
        scripts = "classpath:db/schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Import({LorebookTestContext.class, CharacterCoreTestContext.class, LocationTestContext.class})
class LorebookServiceTest {
    @Autowired
    LorebookTestContext testContext;
    @Autowired
    CharacterCoreTestContext characters;
    @Autowired
    private LocationTestContext locationTestContext;

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

        System.out.println(lorebook1);
        System.out.println(lorebook2);

        assertNotEquals(lorebook1.getId(), lorebook2.getId());
    }
}