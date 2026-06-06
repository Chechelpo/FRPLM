package chechelpo.frplm.domain.lorebook.entry.core;

import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.domain.lorebook.core.LorebookTestContext;
import chechelpo.frplm.domain.lorebook.entry.keywords.EntryKeywordService;
import chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import chechelpo.frplm.test_utils.TestText;
import chechelpo.frplm.utils.collections.IntSetFactory;
import chechelpo.frplm.utils.importers.NewEntryOrder;
import chechelpo.frplm.utils.importers.STLorebookImporter;
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
    void getByOutletsWith_requiresAllEntryKeywordsToBeDetected() {
        long seed = 3000;
        int lorebookAmount = 3;
        int entriesPerLorebook = 10;

        Map<LorebooksRecord, List<EntryRecord>> created =
                entryTestContext.createEntries(seed, lorebookAmount, entriesPerLorebook);

        String outletA = "test_outlet_a";
        String outletB = "test_outlet_b";

        String keywordA = "test_keyword_a";
        String keywordB = "test_keyword_b";
        String keywordNoise = "test_keyword_noise";

        Set<String> expectedKeys = new HashSet<>();

        int index = 0;

        for (var lorebookEntries : created.entrySet()) {
            for (EntryRecord entry : lorebookEntries.getValue()) {
                EntityKey<EntryRecord> entryKey = EntityKey.<EntryRecord>builder()
                        .set(ENTRY.LOREBOOK_ID, entry.getLorebookId())
                        .set(ENTRY.ENTRY_ID, entry.getEntryId())
                        .build();

                boolean useOutletA = index % 2 == 0;
                boolean useKeywordA = index % 3 == 0;
                boolean addNoiseKeyword = index % 5 == 0;

                String outlet = useOutletA ? outletA : outletB;
                String baseKeyword = useKeywordA ? keywordA : keywordB;

                assertTrue(entryService.updateOutlet(entryKey, outlet));

                assertTrue(keywords.associate(
                        entry.getLorebookId(),
                        entry.getEntryId(),
                        baseKeyword
                ));

                if (addNoiseKeyword) {
                    assertTrue(keywords.associate(
                            entry.getLorebookId(),
                            entry.getEntryId(),
                            keywordNoise
                    ));
                }

                /*
                 * Query will only contain keywordA.
                 *
                 * Therefore the entry matches only if:
                 * 1. it belongs to outletA,
                 * 2. it has keywordA,
                 * 3. it does NOT have keywordNoise.
                 *
                 * Because entry keywords are interpreted as AND.
                 */
                if (useOutletA && useKeywordA && !addNoiseKeyword) {
                    expectedKeys.add(entry.getLorebookId() + ":" + entry.getEntryId());
                }

                index++;
            }
        }

        int outletAID = lorebookTestContext.outlets.outletService
                .getOutletID(outletA)
                .orElseThrow();

        int keywordAID = entryTestContext.keywords.service
                .getIDOfKeywordWith(keywordA);

        IntSet lorebookIDs = IntSetFactory.ofValues(
                created.keySet()
                        .stream()
                        .mapToInt(LorebooksRecord::getId)
                        .toArray()
        );

        IntSet detectedKeywordIDs = IntSetFactory.ofValues(keywordAID);

        Map<Integer, List<EntryRecord>> actual =
                entryService.getByOutletsWith(lorebookIDs, detectedKeywordIDs);

        assertTrue(
                actual.containsKey(outletAID),
                "Expected result to contain outletA"
        );

        Set<String> actualKeys = actual.get(outletAID)
                .stream()
                .map(entry -> entry.getLorebookId() + ":" + entry.getEntryId())
                .collect(Collectors.toSet());

        assertEquals(expectedKeys, actualKeys);
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