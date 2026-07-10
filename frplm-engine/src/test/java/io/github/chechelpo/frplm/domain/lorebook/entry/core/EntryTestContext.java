package io.github.chechelpo.frplm.domain.lorebook.entry.core;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookTestContext;
import io.github.chechelpo.frplm.domain.lorebook.entry.ActivationStrategy;
import io.github.chechelpo.frplm.domain.lorebook.entry.keywords.EntryKeywordsTestContext;
import io.github.chechelpo.frplm.domain.lorebook.keywords.KeywordTestContext;
import io.github.chechelpo.frplm.interfaces.DBReload;
import chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Import;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static chechelpo.frplm.jooq.generated.Tables.ENTRY;

@TestComponent
@Import({LorebookTestContext.class, KeywordTestContext.class, EntryKeywordsTestContext.class})
public class EntryTestContext implements DBReload {
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
