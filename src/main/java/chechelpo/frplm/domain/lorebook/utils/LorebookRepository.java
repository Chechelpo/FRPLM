package chechelpo.frplm.domain.lorebook.utils;

import chechelpo.frplm.domain.lorebook.core.LorebookService;
import chechelpo.frplm.domain.lorebook.entry.keywords.EntryKeywordService;
import chechelpo.frplm.domain.lorebook.entry.utils.Entry;
import chechelpo.frplm.domain.lorebook.entry.utils.EntryFactory;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.frameworks.entities.repository.EntityRepository;
import chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public final class LorebookRepository extends EntityRepository<LorebooksRecord, LorebookService> {
    private final EntryFactory entries;
    private final EntryKeywordService keywords;
    LorebookRepository(LorebookService service, EntryFactory entries, EntryKeywordService keywords) {
        super(service);
        this.entries = entries;
        this.keywords = keywords;
    }

    @NotNull Entry @NotNull [] getEntriesWithOutletAndKeywords(
            @NotNull EntityKey<LorebooksRecord> lorebookKey,
            int outletID,
            @NotNull IntSet keywords
    ){
        return entries.getWithOutlet(lorebookKey, outletID, keywords);
    }

    IntSet getKeywordsIDsOfLorebook(EntityKey<LorebooksRecord> lorebookKey) {
        return keywords.getKeywordIDsOfLorebook(lorebookKey);
    }
}
