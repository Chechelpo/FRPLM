package chechelpo.frplm.domain.lorebook.entry.core;

import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.domain.lorebook.core.LorebookTestContext;
import chechelpo.frplm.domain.lorebook.keywords.KeywordService;
import chechelpo.frplm.domain.lorebook.keywords.KeywordTestContext;
import chechelpo.frplm.interfaces.DBReload;
import chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static chechelpo.frplm.jooq.generated.Tables.ENTRY;

@TestComponent
@Import({LorebookTestContext.class, KeywordTestContext.class})
public class EntryTestContext implements DBReload {
    @Autowired
    public LorebookTestContext lorebooks;
    @Autowired
    public KeywordTestContext keywords;

    @Autowired
    public EntryService entryService;
    @Autowired EntryFieldsHelper fields;

    public Map<LorebooksRecord, List<EntryRecord>> createEntries(Long seed, int lorebookAmount, int entriesPerLorebook){
        if (seed == null) seed = ThreadLocalRandom.current().nextLong();
        List<LorebooksRecord> testLorebooks = lorebooks.createLorebooks(seed, lorebookAmount);
        HashMap<LorebooksRecord, List<EntryRecord>> result = new HashMap<>(lorebookAmount);

        for (LorebooksRecord lorebook : testLorebooks) {
            List<EntryRecord> thisEntries = new ArrayList<>(entriesPerLorebook);
            for (int i = 0; i < entriesPerLorebook; i++){
                String entryIdentifier = "Entry (lorebookID:%s) %s".formatted(lorebook.getId(), "entry"+i);
                thisEntries.add(entryService.createAndGet(
                        EntityDataPayload.<EntryRecord>builder()
                                .set(ENTRY.LOREBOOK_ID, lorebook.getId())
                                .set(ENTRY.NAME, entryIdentifier)
                                .set(ENTRY.CONTENT, "Content of " + entryIdentifier)
                                .build()
                ));
            }

            result.put(lorebook, thisEntries);
        }

        return result;
    }

    @Override
    public void reload() {
        lorebooks.reload();
    }
}
