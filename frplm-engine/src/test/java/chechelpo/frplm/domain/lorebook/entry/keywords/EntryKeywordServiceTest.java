package chechelpo.frplm.domain.lorebook.entry.keywords;

import chechelpo.frplm.domain.lorebook.entry.core.EntryTestContext;
import chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import java.util.*;

import static java.util.Arrays.stream;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Sql(
        scripts = "classpath:db/schema.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Import(EntryTestContext.class)
class EntryKeywordServiceTest {

    @Autowired EntryTestContext entries;
    @Autowired EntryKeywordService entryKeywords;
    @Autowired EntryKeywordsFieldHelper fields;

    @BeforeEach
    void setUp() {
        entries.reload();
    }

    @Test
    void testKeywordLifecycle() {
        long seed = 100;
        Random random = new Random(seed);

        int lorebookAmount = 5;
        int perLorebook = 100;

        Map<LorebooksRecord, List<EntryRecord>> created =
                entries.createEntries(seed, lorebookAmount, perLorebook);

        int numberKeywords = 1000;
        List<String> keywords = new ArrayList<>(numberKeywords);

        for (int i = 0; i < numberKeywords; i++) {
            keywords.add("keyword_" + i);
        }

        Map<Integer, Set<String>> expectedByLorebook = new HashMap<>();
        Map<Integer, Map<Integer, Set<String>>> expectedByEntry = new HashMap<>();

        for (var lorebookEntries : created.entrySet()) {
            LorebooksRecord lorebook = lorebookEntries.getKey();
            List<EntryRecord> entryRecords = lorebookEntries.getValue();

            int lorebookID = lorebook.getId();

            expectedByLorebook.put(lorebookID, new HashSet<>());
            expectedByEntry.put(lorebookID, new HashMap<>());

            for (EntryRecord entry : entryRecords) {
                int entryID = entry.getEntryId();

                int keywordCount = 1 + random.nextInt(5);

                Set<String> selectedKeywords = new HashSet<>();

                while (selectedKeywords.size() < keywordCount) {
                    selectedKeywords.add(keywords.get(random.nextInt(keywords.size())));
                }

                expectedByEntry.get(lorebookID).put(entryID, selectedKeywords);
                expectedByLorebook.get(lorebookID).addAll(selectedKeywords);

                for (String keyword : selectedKeywords) {
                    boolean associated = entryKeywords.associate(lorebookID, entryID, keyword);
                    assertTrue(associated);
                }
            }
        }

        for (var lorebookExpected : expectedByLorebook.entrySet()) {
            int lorebookID = lorebookExpected.getKey();
            Set<String> expectedKeywords = lorebookExpected.getValue();

            Set<String> actualKeywords = entryKeywords.keywordsOfLorebook(lorebookID);

            assertEquals(
                    expectedKeywords,
                    actualKeywords,
                    "Lorebook keywords do not match for lorebookID=" + lorebookID
            );
        }

        for (var lorebookExpected : expectedByEntry.entrySet()) {
            int lorebookID = lorebookExpected.getKey();

            for (var entryExpected : lorebookExpected.getValue().entrySet()) {
                int entryID = entryExpected.getKey();
                Set<String> expectedKeywords = entryExpected.getValue();


                Set<String> actualKeywords = entryKeywords.keywordsOfEntry(lorebookID, entryID);

                assertEquals(
                        expectedKeywords,
                        actualKeywords,
                        "Entry keywords do not match for lorebookID=" + lorebookID
                                + ", entryID=" + entryID
                );

                for (String keyword : actualKeywords) {
                    assertTrue(entryKeywords.dissociate(lorebookID, entryID, keyword),
                            "Could not dissociate keyword for " + lorebookID
                    );
                    assertFalse(
                            entryKeywords.keywordsOfEntry(lorebookID, entryID)
                                    .contains(keyword),
                            "Keyword still associated"
                    );
                }
            }
            assertTrue(
                    entryKeywords.keywordsOfLorebook(lorebookID).isEmpty()
            );
        }
    }
}