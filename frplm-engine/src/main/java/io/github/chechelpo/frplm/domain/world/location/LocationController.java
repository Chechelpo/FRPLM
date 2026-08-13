package io.github.chechelpo.frplm.domain.world.location;

import io.github.chechelpo.frplm.core.entities.fields.DTOMapper;
import io.github.chechelpo.frplm.core.entities.pseudo_services.*;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

@RestController
@Component
@RequestMapping(EntityConfigs.LOCATIONS_URL)
public final class LocationController extends EntityController<LocationsRecord, LocationsService> {
    LocationController(LocationsService service, DTOMapper<LocationsRecord> mapper) {
        super(EntityConfigs.Types.LOCATIONS,service, mapper);
    }
}
