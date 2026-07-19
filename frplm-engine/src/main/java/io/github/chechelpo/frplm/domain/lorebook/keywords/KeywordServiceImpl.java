package io.github.chechelpo.frplm.domain.lorebook.keywords;

import io.github.chechelpo.frplm.events.EventBus;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityService;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.jooq.generated.tables.records.KeywordRecord;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.OptionalInt;

@Component
non-sealed class KeywordServiceImpl extends EntityService<KeywordRecord, KeywordStore> implements KeywordService {
    KeywordServiceImpl(KeywordStore store, EventBus eventBus) {
        super(store, eventBus);
    }

    @Override
    @Transactional(readOnly = true)
    public int getOrGenerate(String keyword) {
        return store.getOrCreate(keyword);
    }

    public OptionalInt getIDOfKeywordWith(String name){
        Integer result = store.getWith(name);
        if (result == null) return OptionalInt.empty();
        return OptionalInt.of(result);
    }

    public boolean existsWith(String name) {
        return this.store.existsWith(name);
    }
}
