package io.github.chechelpo.frplm.domain.lorebook.outlet;

import io.github.chechelpo.frplm.core.entities.fields.DTOMapper;
import io.github.chechelpo.frplm.core.entities.fields.EntityDTO;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.UnsupportedAction;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityController;
import io.github.chechelpo.frplm.jooq.generated.tables.records.OutletRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs.OUTLET_URL;


@RestController
@RequestMapping(OUTLET_URL)
final class OutletController extends EntityController<OutletRecord, OutletServiceImpl> {
    OutletController(OutletServiceImpl service, DTOMapper<OutletRecord> mapper) {
        super(service, mapper);
    }

    @Override
    protected ResponseEntity<EntityDTO> create(Map<String, Object> params, Map<String, Object> body) {
        throw new UnsupportedAction("Outlets can't be created via entity framework", Severity.USER);
    }
}
