package chechelpo.frplm.domain.lorebook.keywords;

import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.core.entities.pseudo_services.EntityService;
import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.jooq.generated.tables.records.KeywordRecord;
import chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import chechelpo.frplm.utils.collections.IntSetFactory;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import it.unimi.dsi.fastutil.ints.IntSet;
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
        return store.getOrCreate(keyword);
    }

    public int getIDOfKeywordWith(String name){
        return store.getWith(name);
    }

    public boolean existsWith(String name) {
        return this.store.existsWith(name);
    }


/*
    @EventListener
    public void eraseIfEmpty(CRUDCommittedEvent.DeletedEntity<?> deletedEntity) {
        if (deletedEntity.type() != EntityTypes.Types.ENTRIES) return;

        CRUDCommittedEvent.DeletedEntity<EntryRecord> event = (CRUDCommittedEvent.DeletedEntity<EntryRecord>) deletedEntity;

    }*/
}
