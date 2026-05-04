package chechelpo.frplm.domain.lorebook.core;

import chechelpo.frplm.config.controllers.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityService;
import chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public final class LorebookService extends ABSEntityService<LorebooksRecord, LorebookStore> {

    public LorebookService(LorebookStore storeAbs) {
        super(storeAbs, EntityTypes.Types.LOREBOOKS);
    }

    /**
     * @return list of global records
     */
    @Override
    public @NotNull List<LorebooksRecord> getAll() {
        return this.store.getGlobalLorebooks();
    }
}
