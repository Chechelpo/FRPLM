package chechelpo.frplm.domain.character.starting_locations;

import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.EntityController;
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
