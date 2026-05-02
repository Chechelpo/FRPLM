package chechelpo.frplm.domain.lorebook.core;

import chechelpo.frplm.config.controllers.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityService;
import chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import org.springframework.stereotype.Component;

@Component
public final class LorebookService extends ABSEntityService<LorebooksRecord, LorebookStore> {

    public LorebookService(LorebookStore storeAbs) {
        super(storeAbs, EntityTypes.Types.LOREBOOKS);
    }
}
