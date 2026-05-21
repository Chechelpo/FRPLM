package chechelpo.frplm.domain.lorebook.entry.keywords;

import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.frameworks.entities.microservices.EntityService;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.jooq.generated.tables.records.EntryKeywordsRecord;
import chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import chechelpo.frplm.jooq.generated.tables.records.KeywordRecord;
import chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EntryKeywordService extends EntityService<EntryKeywordsRecord, EntryKeywordStore> {
    EntryKeywordService(EntryKeywordStore store, EventBus eventBus) {
        super(store, eventBus);
    }

    public @NotNull List<KeywordRecord> keywordsOfEntry(EntityKey<EntryRecord> key){
        return this.store.getOfEntry(key);
    }

    public IntSet getKeywordIDsOfLorebook(EntityKey<LorebooksRecord> key){
        return store.getKeywordIDsOfLorebook(key);
    }
}
