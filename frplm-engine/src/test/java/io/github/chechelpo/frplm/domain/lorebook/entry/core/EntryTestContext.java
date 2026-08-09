package io.github.chechelpo.frplm.domain.lorebook.entry.core;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookTestContext;
import io.github.chechelpo.frplm.domain.lorebook.entry.ActivationStrategy;
import io.github.chechelpo.frplm.domain.lorebook.entry.keywords.EntryKeywordsTestContext;
import io.github.chechelpo.frplm.domain.lorebook.keywords.KeywordTestContext;
import io.github.chechelpo.frplm.interfaces.DBReload;
import io.github.chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import io.github.chechelpo.frplm.utils.importers.sillytavern.STLorebookImporter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static io.github.chechelpo.frplm.jooq.generated.Tables.ENTRY;

@TestComponent
@Import({LorebookTestContext.class, KeywordTestContext.class, EntryKeywordsTestContext.class})
public class EntryTestContext implements DBReload {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    @Autowired
    public LorebookTestContext lorebooks;
    @Autowired
    public KeywordTestContext keywords;
    @Autowired
    public EntryKeywordsTestContext entryKeywords;
    @Autowired
    public EntryService entryService;
    @Autowired
    EntryFieldsHelper fields;

    public Map<LorebooksRecord, List<EntryRecord>> createEntries(
            Long seed,
            int lorebookAmount,
            int entriesPerLorebook
    ) {
        long actualSeed = seed != null
                ? seed
                : ThreadLocalRandom.current().nextLong();

        SplittableRandom random = new SplittableRandom(actualSeed);

        List<LorebooksRecord> testLorebooks =
                lorebooks.createLorebooks(actualSeed, lorebookAmount);

        Map<LorebooksRecord, List<EntryRecord>> result =
                new LinkedHashMap<>(lorebookAmount);

        for (LorebooksRecord lorebook : testLorebooks) {
            result.put(
                    lorebook,
                    createEntriesForLorebook(
                            lorebook,
                            entriesPerLorebook,
                            random
                    )
            );
        }

        return result;
    }

    public List<EntryRecord> createEntriesForLorebook(
            LorebooksRecord lorebook,
            int entryAmount,
            SplittableRandom random
    ) {
        List<EntryRecord> entries = new ArrayList<>(entryAmount);

        for (int i = 0; i < entryAmount; i++) {
            String entryIdentifier = "Entry (lorebookID:%d) entry%d"
                    .formatted(lorebook.getId(), i);

            EntryRecord entry = entryService.createAndGet(
                    EntityDataPayload.<EntryRecord>builder()
                            .set(ENTRY.LOREBOOK_ID, lorebook.getId())
                            .set(ENTRY.NAME, entryIdentifier)
                            .set(
                                    ENTRY.CONTENT,
                                    "Content of %s. Generated value: %d"
                                            .formatted(
                                                    entryIdentifier,
                                                    random.nextInt()
                                            )
                            )
                            .set(ENTRY.ENABLED, true)
                            .set(
                                    ENTRY.PROBABILITY,
                                    randomShort(random, 0, 100)
                            )
                            .set(
                                    ENTRY.DELAY,
                                    random.nextInt(0, 101)
                            )
                            .set(
                                    ENTRY.COOLDOWN,
                                    random.nextInt(0, 501)
                            )
                            .set(
                                    ENTRY.STICK_THROUGH,
                                    random.nextInt(0, 11)
                            )
                            .set(
                                    ENTRY.POSITION,
                                    randomShort(random, 0, 101)
                            )
                            .set(
                                    ENTRY.STRATEGY,
                                    ActivationStrategy.COMMON.stable_id
                            )
                            .set(
                                    ENTRY.PREVENT_FURTHER_RECURSION,
                                    random.nextBoolean()
                            )
                            .set(
                                    ENTRY.NON_RECURSABLE,
                                    random.nextBoolean()
                            )
                            .set(
                                    ENTRY.SCAN_DEPTH,
                                    randomShort(random, 0, 21)
                            )
                            .build()
            );

            entries.add(entry);
        }

        return entries;
    }

    public enum KNOWN_ST_LOREBOOKS {
        Eldoria("eldoria.json"),
        Mansion("Mansion_1.json")
        ;
        public final String path;
        KNOWN_ST_LOREBOOKS(String path){
            this.path = path;
        }
    }

    public LorebooksRecord importTestLorebookFile(KNOWN_ST_LOREBOOKS lorebookFile){
        LorebooksRecord lorebook = lorebooks.createLorebooks(0L, 1).getFirst();
        entryService.importEntriesFromJSON(
                lorebook.getId(),
                OBJECT_MAPPER.readTree(STLorebookImporter.class.getResourceAsStream("imports/st_lorebooks/" + lorebookFile.path))
        );
        return lorebook;
    }

    private static short randomShort(
            SplittableRandom random,
            int origin,
            int bound
    ) {
        return (short) random.nextInt(origin, bound);
    }

    @Override
    public void reload() {
        lorebooks.reload();
    }
}
