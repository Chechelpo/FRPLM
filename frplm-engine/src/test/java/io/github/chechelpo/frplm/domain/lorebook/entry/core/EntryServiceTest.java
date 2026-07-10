package io.github.chechelpo.frplm.domain.lorebook.entry.core;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookTestContext;
import io.github.chechelpo.frplm.domain.lorebook.entry.ActivationStrategy;
import io.github.chechelpo.frplm.domain.lorebook.entry.keywords.EntryKeywordService;
import io.github.chechelpo.frplm.domain.lorebook.entry.keywords.EntryKeywordsTestContext;
import io.github.chechelpo.frplm.domain.lorebook.keywords.KeywordTestContext;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.exceptions.runtime.InvalidValue;
import chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import io.github.chechelpo.frplm.test_utils.TestText;
import io.github.chechelpo.frplm.utils.collections.IntSetFactory;
import io.github.chechelpo.frplm.utils.json_mappers.orders.NewEntryOrder;
import io.github.chechelpo.frplm.utils.importers.sillytavern.STLorebookImporter;
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
import java.util.stream.IntStream;

import static chechelpo.frplm.jooq.generated.Tables.ENTRY;
import static chechelpo.frplm.jooq.generated.Tables.LOREBOOKS;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import({LorebookTestContext.class, EntryTestContext.class, EntryKeywordsTestContext.class})
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
    @Autowired
    private EntryKeywordsTestContext entryKeywordsTestContext;

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
            assertDoesNotThrow(
                    () -> entryService.updateOutlet(entryKey, newOutlet),
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
                        EntityDataPayload.<EntryRecord>builder()
                                .set(ENTRY.STRATEGY, ActivationStrategy.CONSTANT.stable_id)
                                .set(ENTRY.ENABLED, true) //Need to enable, constant entries otherwise won't appear anyways
                                .build()
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
        assertEquals(expectedConstantEntries.size(), actualConstantEntries.size());
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

    @Test
    void exchangeEntry(){
        int entriesPerLorebook = 20;
        Map<LorebooksRecord, List<EntryRecord>> entries = entryTestContext.createEntries(10L, 2, entriesPerLorebook);
        LorebooksRecord initialLorebook = entries.keySet().stream().toList().getFirst();
        LorebooksRecord secondLorebook = entries.keySet().stream().toList().get(1);

        List<EntryRecord> toExchange = entries.get(initialLorebook);
        for (EntryRecord entry : toExchange){
            EntityKey<EntryRecord> initialKey = entryService.keyOf(entry);
            Set<String> expectedKeywords = entryKeywordsTestContext.entryKeywordsService.keywordsOfEntry(entry.getLorebookId(), entry.getEntryId());

            EntryRecord exchangedEntry = entryService.exchangeEntry(initialKey, secondLorebook.getId());
            Set<String> actualKeywords = entryKeywordsTestContext.entryKeywordsService
                    .keywordsOfEntry(exchangedEntry.getLorebookId(), exchangedEntry.getEntryId());

            assertNotNull(exchangedEntry, "Exchange entry returned null");
            assertTrue(entryService.find(initialKey).isEmpty(), "Stale reference: Could find entry with previous key after exchange");
            assertEquals(secondLorebook.getId(), exchangedEntry.getLorebookId(), "Mismatch in lorebook id");
            assertEquals(exchangedEntry.getContent(), entry.getContent(), "Different content");
            assertEquals(expectedKeywords, actualKeywords, "Mismatch in keywords after exchange");
        }
        List<EntryRecord> newSecondLorebookEntries = entryService.getMatching(EntityKey.of(ENTRY.LOREBOOK_ID, secondLorebook.getId()));
        assertEquals(entriesPerLorebook * 2, newSecondLorebookEntries.size());
    }

    @Test
    void exchangeEntry_throwsEntityNotFoundOnFalseLorebookOrFalseEntryOrSameLorebook() {
        Map<LorebooksRecord, List<EntryRecord>> entries = entryTestContext.createEntries(10L, 2, 20);
        LorebooksRecord actualLorebook = entries.keySet().stream().toList().getFirst();
        LorebooksRecord secondLorebook = entries.keySet().stream().toList().get(1);

        int falseLorebookId = actualLorebook.getId() + secondLorebook.getId();
        EntryRecord legitimateEntry = entries.get(actualLorebook).getFirst();
        EntityKey<EntryRecord> legitimateEntryKey = entryService.keyOf(legitimateEntry);
        assertThrows(
                EntityNotFound.class,
                () -> entryService.exchangeEntry(legitimateEntryKey, falseLorebookId),
                "Moved to unknown lorebook"
        );
        assertThrows(
                InvalidValue.class,
                () -> entryService.exchangeEntry(legitimateEntryKey, actualLorebook.getId()),
                "Could move to same lorebook"
        );

        int falseEntryId = entries.get(actualLorebook).stream().mapToInt(EntryRecord::getEntryId).sum();

        assertThrows(
                EntityNotFound.class,
                () -> entryService.exchangeEntry(
                        EntityKey.<EntryRecord>builder()
                                .set(ENTRY.LOREBOOK_ID, actualLorebook.getId())
                                .set(ENTRY.ENTRY_ID, falseEntryId)
                                .build(),
                        secondLorebook.getId()
                        ),
                "Could exchange false entry"
        );
    }

}