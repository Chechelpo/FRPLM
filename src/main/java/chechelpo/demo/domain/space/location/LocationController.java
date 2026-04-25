package chechelpo.demo.domain.space.location;

import chechelpo.demo.config.controllers.EntityTypes;
import chechelpo.demo.frameworks.entities.microservices.ABSEntityController;
import chechelpo.demo.jooq.generated.tables.records.LocationsRecord;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(EntityTypes.LOCATIONS_URL)
final class LocationController extends ABSEntityController<LocationsRecord, LocationsService>
{
    LocationController(LocationsService service) {
        super(EntityTypes.Types.LOCATIONS, service);
    }
}
