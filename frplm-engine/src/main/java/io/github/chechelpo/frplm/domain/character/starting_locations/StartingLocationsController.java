package io.github.chechelpo.frplm.domain.character.starting_locations;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityController;
import io.github.chechelpo.frplm.jooq.generated.tables.records.StartingLocationsRecord;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(EntityConfigs.STARTING_LOCATIONS_URL)
final class StartingLocationsController extends EntityController<
        StartingLocationsRecord,
        StartingLocationsService
        > {

    StartingLocationsController(StartingLocationsService service) {
        super(service);
    }
}
