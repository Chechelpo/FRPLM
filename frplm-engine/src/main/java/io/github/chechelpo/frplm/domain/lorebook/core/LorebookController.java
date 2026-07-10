package io.github.chechelpo.frplm.domain.lorebook.core;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityController;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs.LOREBOOKS_URL;

@RestController
@Component
@RequestMapping(LOREBOOKS_URL)
public final class LorebookController extends EntityController<LorebooksRecord, LorebookService> {
    LorebookController(LorebookService service) {
        super(service);
    }
}
