package chechelpo.frplm.domain.character.starting_locations;

import chechelpo.frplm.config.controllers.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityController;
import chechelpo.frplm.jooq.generated.tables.records.StartingLocationsRecord;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(EntityTypes.STARTING_LOCATIONS_URL)
final class StartingLocationsController extends ABSEntityController<
        StartingLocationsRecord,
        StartingLocationsService
        > {

    StartingLocationsController(StartingLocationsService service) {
        super(EntityTypes.Types.STARTING_LOCATIONS,service);
    }
}
