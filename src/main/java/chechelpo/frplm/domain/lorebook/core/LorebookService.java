package chechelpo.frplm.domain.lorebook.core;

import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.frameworks.entities.microservices.EntityService;
import chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LorebookService extends EntityService<LorebooksRecord, LorebookStore> {
    LorebookService(LorebookStore storeAbs, EventBus eventBus) {
        super(storeAbs, eventBus);
    }

    /**
     * @return list of global records
     */
    @Override
    public @NotNull List<LorebooksRecord> getAll() {
        return this.store.getGlobalLorebooks();
    }
}
