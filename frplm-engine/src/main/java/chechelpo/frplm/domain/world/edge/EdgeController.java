package chechelpo.frplm.domain.world.edge;

import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.frameworks.entities.pseudo_services.EntityController;
import chechelpo.frplm.jooq.generated.tables.records.LocationNeighborsRecord;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(EntityTypes.EDGES_URL)
final class EdgeController extends EntityController<LocationNeighborsRecord, EdgeService> {
    protected EdgeController(EdgeService service) {
        super(EntityTypes.Types.EDGES,service);
    }
}