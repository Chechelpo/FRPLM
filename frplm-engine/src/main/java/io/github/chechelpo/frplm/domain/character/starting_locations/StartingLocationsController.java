package io.github.chechelpo.frplm.domain.character.starting_locations;

import io.github.chechelpo.frplm.domain.EntityTypes;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityController;
import chechelpo.frplm.jooq.generated.tables.records.StartingLocationsRecord;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(EntityTypes.STARTING_LOCATIONS_URL)
final class StartingLocationsController extends EntityController<
        StartingLocationsRecord,
        StartingLocationsService
        > {

    StartingLocationsController(StartingLocationsService service) {
        super(service);
    }
}
