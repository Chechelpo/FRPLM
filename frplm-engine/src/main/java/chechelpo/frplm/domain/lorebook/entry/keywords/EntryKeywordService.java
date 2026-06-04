package chechelpo.frplm.domain.lorebook.entry.keywords;

import chechelpo.frplm.domain.lorebook.keywords.KeywordService;
import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.core.entities.pseudo_services.EntityService;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.jooq.generated.tables.records.EntryKeywordsRecord;
import chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.List;

import static chechelpo.frplm.jooq.generated.Tables.ENTRY_KEYWORDS;

@Component
public class EntryKeywordService extends EntityService<EntryKeywordsRecord, EntryKeywordStore> {
    private final KeywordService keywordService;

    EntryKeywordService(EntryKeywordStore store, EventBus eventBus, KeywordService keywordService) {
        super(store, eventBus);
        this.keywordService = keywordService;
    }

    public @NotNull List<String> keywordsOfEntry(EntityKey<EntryRecord> key){
        return this.store.getOfEntry(key);
    }

    public boolean associate(int lorebookID, int entryID, String name){
        log.debug("Associating entry with lorebookID {} and entryID {} to keyword {}", lorebookID, entryID, name);
        int keywordID = keywordService.getOrGenerate(name);
        store.create(EntityDataPayload.<EntryKeywordsRecord>builder()
                .set(ENTRY_KEYWORDS.LOREBOOK_ID, lorebookID)
                .set(ENTRY_KEYWORDS.ENTRY_ID, entryID)
                .set(ENTRY_KEYWORDS.KEYWORD_ID, keywordID)
                .build()
        );
        return true;
    }

    public boolean dissociate(int lorebookID, int entryID, String name){
        log.debug("Disassociating entry with lorebookID {} and entryID {} to keyword {}", lorebookID, entryID, name);
        int keywordID = keywordService.getOrGenerate(name);
        return store.delete(EntityKey.<EntryKeywordsRecord>builder()
                .set(ENTRY_KEYWORDS.LOREBOOK_ID, lorebookID)
                .set(ENTRY_KEYWORDS.ENTRY_ID, entryID)
                .set(ENTRY_KEYWORDS.KEYWORD_ID,keywordID)
                .build()
        );
    }

    public List<String> keywordsInLorebook(int lorebookID){
        return store.getKeywordNamesOfLorebook(lorebookID);
    }

    @Override
    protected void beforeCreate(EntityDataPayload<EntryKeywordsRecord> data, long operationID) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
