package io.github.chechelpo.frplm.domain.lorebook.outlet;

import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.UnsupportedAction;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityController;
import chechelpo.frplm.jooq.generated.tables.records.OutletRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static io.github.chechelpo.frplm.domain.EntityTypes.OUTLET_URL;

@RestController
@RequestMapping(OUTLET_URL)
final class OutletController extends EntityController<OutletRecord, OutletService> {
    OutletController(OutletService service) {
        super(service);
    }

    @Override
    protected ResponseEntity<EntityDTO> create(Map<String, Object> params, Map<String, Object> body) {
        throw new UnsupportedAction("Outlets can't be created via entity framework", Severity.USER);
    }
}
