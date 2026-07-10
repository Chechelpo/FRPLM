package io.github.chechelpo.frplm.domain.lorebook.core;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.domain.character.core.CharacterCoreTestContext;
import chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Set;

import static chechelpo.frplm.jooq.generated.Tables.LOREBOOKS;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Sql(
        scripts = "classpath:db/schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Import({LorebookTestContext.class, CharacterCoreTestContext.class})
class LorebookServiceTest {
    @Autowired
    LorebookTestContext testContext;
    @Autowired
    CharacterCoreTestContext characters;

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
}