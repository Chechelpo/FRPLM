package chechelpo.demo.domain.lorebook.main;

import chechelpo.demo.config.controllers.EntityTypes;
import chechelpo.demo.frameworks.entities.microservices.ABSEntityService;
import chechelpo.demo.jooq.generated.tables.records.LorebooksRecord;
import org.springframework.stereotype.Component;

@Component
public final class LorebookService extends ABSEntityService<LorebooksRecord, LorebookStore> {

    public LorebookService(LorebookStore storeAbs) {
        super(storeAbs, EntityTypes.Types.LOREBOOKS);
    }
}
