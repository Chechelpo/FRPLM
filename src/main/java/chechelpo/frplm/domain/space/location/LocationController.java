package chechelpo.frplm.domain.space.location;

import chechelpo.frplm.config.controllers.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityController;
import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
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
