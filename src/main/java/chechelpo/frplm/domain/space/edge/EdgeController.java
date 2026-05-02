package chechelpo.frplm.domain.space.edge;

import chechelpo.frplm.config.controllers.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityController;
import chechelpo.frplm.jooq.generated.tables.records.LocationNeighborsRecord;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(EntityTypes.EDGES_URL)
final class EdgeController extends ABSEntityController<LocationNeighborsRecord, EdgeService> {
    protected EdgeController(EdgeService service) {
        super(EntityTypes.Types.EDGES,service);
    }
}