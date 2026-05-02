package chechelpo.frplm.domain.lorebook.core;

import chechelpo.frplm.config.controllers.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityController;
import chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static chechelpo.frplm.config.controllers.EntityTypes.LOREBOOKS_URL;

@RestController
@RequestMapping(LOREBOOKS_URL)
final class LorebookController extends ABSEntityController<LorebooksRecord, LorebookService> {
    LorebookController(LorebookService service) {
        super(EntityTypes.Types.LOREBOOKS, service);
    }
}
