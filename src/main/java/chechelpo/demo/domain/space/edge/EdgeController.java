package chechelpo.demo.domain.space.edge;

import chechelpo.demo.config.controllers.EntityTypes;
import chechelpo.demo.frameworks.entities.microservices.ABSEntityController;
import chechelpo.demo.jooq.generated.tables.records.LocationNeighborsRecord;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(EntityTypes.EDGES_URL)
final class EdgeController extends ABSEntityController<LocationNeighborsRecord, EdgeService> {
    protected EdgeController(EdgeService service) {
        super(EntityTypes.Types.EDGES,service);
    }
}