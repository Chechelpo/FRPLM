package chechelpo.demo.domain.lorebook.core;

import chechelpo.demo.config.controllers.EntityTypes;
import chechelpo.demo.frameworks.entities.microservices.ABSEntityController;
import chechelpo.demo.jooq.generated.tables.records.LorebooksRecord;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static chechelpo.demo.config.controllers.EntityTypes.LOREBOOKS_URL;

@RestController
@RequestMapping(LOREBOOKS_URL)
final class LorebookController extends ABSEntityController<LorebooksRecord, LorebookService> {

    LorebookController(LorebookService service) {
        super(EntityTypes.Types.LOREBOOKS, service);
    }
}
