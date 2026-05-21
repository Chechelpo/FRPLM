package chechelpo.frplm.domain.lorebook.keywords;

import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.frameworks.entities.microservices.EntityService;
import chechelpo.frplm.frameworks.entities.microservices.EntityDataPayload;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.jooq.generated.tables.records.KeywordRecord;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.springframework.stereotype.Component;

@Component
public class KeywordService extends EntityService<KeywordRecord, KeywordStore> {
    KeywordService(KeywordStore store, EventBus eventBus) {
        super(store, eventBus);
    }

    @Override
    public boolean update(EntityKey<KeywordRecord> id, EntityDataPayload<KeywordRecord> update) {
        throw new UnsupportedOperationException("Keywords can't be updated");
    }
}
