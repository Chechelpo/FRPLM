package chechelpo.frplm.domain.lorebook.core;

import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.EntityController;
import chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static chechelpo.frplm.domain.EntityTypes.LOREBOOKS_URL;

@RestController
@RequestMapping(LOREBOOKS_URL)
final class LorebookController extends EntityController<LorebooksRecord, LorebookService> {
    LorebookController(LorebookService service) {
        super(EntityTypes.Types.LOREBOOKS, service);
    }
}
