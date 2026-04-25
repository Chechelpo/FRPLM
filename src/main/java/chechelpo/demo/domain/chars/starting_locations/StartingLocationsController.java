package chechelpo.demo.domain.chars.starting_locations;

import chechelpo.demo.config.controllers.EntityTypes;
import chechelpo.demo.frameworks.entities.microservices.ABSEntityController;
import chechelpo.demo.jooq.generated.tables.records.StartingLocationsRecord;
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
