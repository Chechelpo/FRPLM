package chechelpo.frplm.domain.space.edge;

import chechelpo.frplm.config.controllers.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityService;
import chechelpo.frplm.jooq.generated.tables.records.LocationNeighborsRecord;
import org.springframework.stereotype.Service;

@Service
public final class EdgeService extends ABSEntityService<LocationNeighborsRecord, EdgeStore> {
    public EdgeService(EdgeStore store) {
        super(store, EntityTypes.Types.EDGES);
    }
}
