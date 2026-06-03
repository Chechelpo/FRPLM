package chechelpo.frplm.domain.lorebook.keywords;

import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.events.crud.CRUDCommittedEvent;
import chechelpo.frplm.frameworks.entities.pseudo_services.EntityService;
import chechelpo.frplm.frameworks.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.frameworks.entities.pseudo_services.EntityKey;
import chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import chechelpo.frplm.jooq.generated.tables.records.KeywordRecord;
import chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import chechelpo.frplm.utils.collections.IntSetFactory;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class KeywordService extends EntityService<KeywordRecord, KeywordStore> {
    KeywordService(KeywordStore store, EventBus eventBus) {
        super(store, eventBus);
    }

    @Override
    public boolean update(EntityKey<KeywordRecord> id, EntityDataPayload<KeywordRecord> update) {
        throw new UnsupportedOperationException("Keywords can't be updated");
    }

    public int getOrGenerate(String keyword) {
        if (existsWith(keyword)) {
            Integer id = store.getWith(keyword);
            if (id != null) return id;

            log.error("Keyword exists with name {} but ID could not be found", keyword);
            throw new IllegalArgumentException("Keyword exists with name " + keyword + " but ID could not be found");
        }
        log.debug("Creating new keyword {}", keyword);
        return store.createWith(keyword);
    }

    public boolean existsWith(String name) {
        return this.store.existsWith(name);
    }

    /**
     * @return array containing [ keywordID, keyword ] that appear in lorebooks
     */
    public IntObjectPair<String>[] getKeywords(LorebooksRecord... lorebooks) {
        return this.getKeywords(IntSetFactory.ofValues(
                        Arrays.stream(lorebooks)
                                .mapToInt(LorebooksRecord::getId)
                                .toArray()
                )
        );
    }

    public IntObjectPair<String>[] getKeywords(IntSet lorebookIDs) {
        return store.getKeywordsOf(lorebookIDs);
    }
/*
    @EventListener
    public void eraseIfEmpty(CRUDCommittedEvent.DeletedEntity<?> deletedEntity) {
        if (deletedEntity.type() != EntityTypes.Types.ENTRIES) return;

        CRUDCommittedEvent.DeletedEntity<EntryRecord> event = (CRUDCommittedEvent.DeletedEntity<EntryRecord>) deletedEntity;

    }*/
}
