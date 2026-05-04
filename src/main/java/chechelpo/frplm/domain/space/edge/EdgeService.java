package chechelpo.frplm.domain.space.edge;

import chechelpo.frplm.config.controllers.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityService;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.jooq.generated.tables.records.LocationNeighborsRecord;
import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public final class EdgeService extends ABSEntityService<LocationNeighborsRecord, EdgeStore> {
    EdgeService(EdgeStore store) {
        super(store, EntityTypes.Types.EDGES);
    }

    public @NotNull List<LocationsRecord> getNeighbours(EntityKey<LocationsRecord> key){
        return this.store.getNeighboursOf(key);
    }
}
