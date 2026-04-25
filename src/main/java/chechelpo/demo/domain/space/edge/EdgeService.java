package chechelpo.demo.domain.space.edge;

import chechelpo.demo.config.controllers.EntityTypes;
import chechelpo.demo.frameworks.entities.microservices.ABSEntityService;
import chechelpo.demo.jooq.generated.tables.records.LocationNeighborsRecord;
import org.springframework.stereotype.Service;

@Service
public final class EdgeService extends ABSEntityService<LocationNeighborsRecord, EdgeStore> {
    public EdgeService(EdgeStore store) {
        super(store, EntityTypes.Types.EDGES);
    }
}
