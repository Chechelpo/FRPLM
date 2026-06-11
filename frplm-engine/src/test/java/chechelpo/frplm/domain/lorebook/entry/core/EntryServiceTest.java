package chechelpo.frplm.domain.lorebook.entry.core;

import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.domain.lorebook.core.LorebookTestContext;
import chechelpo.frplm.domain.lorebook.entry.ActivationStrategy;
import chechelpo.frplm.domain.lorebook.entry.keywords.EntryKeywordService;
import chechelpo.frplm.domain.lorebook.keywords.KeywordService;
import chechelpo.frplm.domain.lorebook.keywords.KeywordTestContext;
import chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import chechelpo.frplm.test_utils.TestText;
import chechelpo.frplm.utils.collections.IntSetFactory;
import chechelpo.frplm.utils.importers.lorebooks.NewEntryOrder;
import chechelpo.frplm.utils.importers.lorebooks.STLorebookImporter;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static chechelpo.frplm.jooq.generated.Tables.ENTRY;
import static chechelpo.frplm.jooq.generated.Tables.LOREBOOKS;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import({LorebookTestContext.class, EntryTestContext.class})
class EntryServiceTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    LorebookTestContext lorebookTestContext;
    @Autowired
    EntryTestContext entryTestContext;
    @Autowired
    EntryKeywordService keywords;
    @Autowired
    EntryService entryService;
    @Autowired
    EntryFieldsHelper fields;
    @Autowired
    KeywordTestContext keywordTestContext;

    @BeforeEach
    void setUp() {
        lorebookTestContext.reload();
        entryTestContext.reload();
    }

    @Test
    void updateOutlet() {
        long seed = 2000;
        int lorebooks = 3;
        int entriesPerLorebook = 10;
        int totalEntries = lorebooks * entriesPerLorebook;
        List<EntryRecord> entries = new ArrayList<>(totalEntries);
        entryTestContext.createEntries(seed, lorebooks, entriesPerLorebook).values()
                .forEach(entries::addAll);

        List<String> outlets = new ArrayList<>(totalEntries);
        for (int i = 0; i < totalEntries; i++)
            outlets.add(TestText.randomText(seed + i, 2, 30));

        int i = 0;
        for (EntryRecord entry : entries) {
            EntityKey<EntryRecord> entryKey = EntityKey.<EntryRecord>builder()
                    .set(ENTRY.ENTRY_ID, entry.getEntryId())
                    .set(ENTRY.LOREBOOK_ID, entry.getLorebookId())
                    .build();
            String newOutlet = outlets.get(i);
            assertTrue(
                    entryService.updateOutlet(entryKey, newOutlet),
                    "Couldn't update outlet"
            );
            Optional<Integer> newOutletID = lorebookTestContext.outlets.outletService.getOutletID(newOutlet);
            assertTrue(newOutletID.isPresent(), "Didn't register new outlet " + newOutlet);

            Optional<Integer> actualOutletID = entryService.getValueOf(ENTRY.OUTLET, entryKey);
            assertTrue(actualOutletID.isPresent());
            assertEquals(actualOutletID.get(), newOutletID.get(), "Mismatch outletID");

            i++;
        }
    }

    @Test
    void getWithKeywordsAndOutlets() {
        long seed = 3000;
        int lorebookAmount = 3;
        int entriesPerLorebook = 10;
        int keywordAmount = 100;

        Map<LorebooksRecord, List<EntryRecord>> lorebooksEntries =
                entryTestContext.createEntries(seed, lorebookAmount, entriesPerLorebook);

        String[] keywordsNames = new String[keywordAmount];
        for (int i = 0; i < keywordAmount; i++) {
            keywordsNames[i] = "keyword_" + i;
        }

        LorebooksRecord lorebook = lorebooksEntries.keySet().iterator().next();
        List<EntryRecord> entries = lorebooksEntries.get(lorebook);

        EntryRecord matchingEntry = entries.get(0);
        EntryRecord nonMatchingEntry = entries.get(1);
        EntryRecord disabledEntry = entries.get(2);
        EntryRecord noKeywordEntry = entries.get(3);

        disabledEntry.setEnabled(false);
        disabledEntry.update();

        String keywordA = keywordsNames[1];
        String keywordB = keywordsNames[2];
        String keywordC = keywordsNames[3];
        String keywordOutside = keywordsNames[99];

        keywords.associate(
                matchingEntry.getLorebookId(),
                matchingEntry.getEntryId(),
                keywordA
        );
        keywords.associate(
                matchingEntry.getLorebookId(),
                matchingEntry.getEntryId(),
                keywordB
        );

        keywords.associate(
                nonMatchingEntry.getLorebookId(),
                nonMatchingEntry.getEntryId(),
                keywordA
        );
        keywords.associate(
                nonMatchingEntry.getLorebookId(),
                nonMatchingEntry.getEntryId(),
                keywordOutside
        );

        keywords.associate(
                disabledEntry.getLorebookId(),
                disabledEntry.getEntryId(),
                keywordA
        );

        IntSet lorebookIDs = new IntOpenHashSet();
        lorebookIDs.add(lorebook.getId());

        Set<Integer> detectedKeywordIDs = Set.of(
                keywordTestContext.service.getOrGenerate(keywordA),
                keywordTestContext.service.getOrGenerate(keywordB),
                keywordTestContext.service.getOrGenerate(keywordC)
        );

        List<EntryRecord> actual =
                entryService.getEntriesWith(lorebookIDs, detectedKeywordIDs);

        assertEquals(2, actual.size());
        List<EntryRecord> expected = List.of(matchingEntry, nonMatchingEntry);
        assertEquals(expected, actual);
    }

    @Test
    void getWithKeywordsAndOutlets_AlwaysReturnsConstantEntries(){
        long seed = 3000;
        int lorebookAmount = 3;
        int entriesPerLorebook = 10;
        int keywordAmount = 100;

        Map<LorebooksRecord, List<EntryRecord>> lorebooksEntries =
                entryTestContext.createEntries(seed, lorebookAmount, entriesPerLorebook);

        Set<EntryRecord> expectedConstantEntries = new HashSet<>(keywordAmount/3);
        for (var lorebookAndEntries : lorebooksEntries.entrySet()){
            List<EntryRecord> entriesOfLorebook = lorebookAndEntries.getValue();
            for (int i = 0; i < entriesOfLorebook.size() ; i+=3){
                EntryRecord updatedRecord = entriesOfLorebook.get(i);
                entryService.update(
                        entryService.keyOf(updatedRecord),
                        EntityDataPayload.of(ENTRY.STRATEGY, ActivationStrategy.CONSTANT.stable_id)
                );
                updatedRecord.setStrategy(ActivationStrategy.CONSTANT.stable_id);
                expectedConstantEntries.add(updatedRecord);
            }
        }

        IntSet lorebookIDs = IntSetFactory.ofValues(lorebooksEntries.keySet().stream()
                .flatMapToInt(record -> IntStream.of(record.getId()))
                .toArray()
        );
        IntSet outlet = IntSet.of(lorebooksEntries.keySet().stream().findFirst().get().getDefaultOutletId());

        Set<EntryRecord> actualConstantEntries = Set.of(entryService.getEntriesWith(lorebookIDs, outlet).toArray(new EntryRecord[0]));
        assertEquals(expectedConstantEntries, actualConstantEntries);
    }

    @Test
    void importEntriesFromJSON() {
        InputStream in = EntryServiceTest.class.getResourceAsStream("/imports/eldoria.json");
        assert in != null : "Resource not found: /imports/eldoria.json";
        JsonNode testLorebook = MAPPER.readTree(in);

        List<NewEntryOrder> newEntriesOrders = STLorebookImporter.getEntries(testLorebook);
        LorebooksRecord lorebook = lorebookTestContext.service.createAndGet(
                EntityDataPayload.of(LOREBOOKS.NAME, "Eldoria")
        );

        List<EntryRecord> createdRecords = entryService.importEntriesFromJSON(lorebook.getId(), testLorebook);

        for (NewEntryOrder order : newEntriesOrders) {
            Optional<EntryRecord> thisOrderRecord = createdRecords.stream()
                    .filter(record -> record.getName().equals(order.entryInfo().requireValue(ENTRY.NAME)))
                    .findFirst();
            assertTrue(thisOrderRecord.isPresent(), "Couldn't find imported entry with name " + order.entryInfo().requireValue(ENTRY.NAME));

            Set<String> keywordsOfThisEntry = Set.of(order.keywords().toArray(String[]::new));
            Set<String> actualKeywordsOfThisEntry = keywords.keywordsOfEntry(thisOrderRecord.get().getLorebookId(), thisOrderRecord.get().getEntryId());

            assertEquals(keywordsOfThisEntry, actualKeywordsOfThisEntry);
        }
    }


}