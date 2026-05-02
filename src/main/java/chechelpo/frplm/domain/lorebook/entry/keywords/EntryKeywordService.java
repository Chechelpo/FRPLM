package chechelpo.frplm.domain.lorebook.entry.keywords;

import chechelpo.frplm.config.controllers.EntityTypes;
import chechelpo.frplm.domain.keywords.KeywordService;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityService;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.jooq.generated.tables.records.EntryKeywordsRecord;
import chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import chechelpo.frplm.jooq.generated.tables.records.KeywordRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public final class EntryKeywordService extends ABSEntityService<EntryKeywordsRecord, EntryKeywordStore> {
    EntryKeywordService(EntryKeywordStore store) {
        super(store, EntityTypes.Types.ENTRY_KEYWORDS);
    }

    public @NotNull List<KeywordRecord> keywordsOfEntry(EntityKey<EntryRecord> key){
        return this.store.getByEntry(key);
    }
}
