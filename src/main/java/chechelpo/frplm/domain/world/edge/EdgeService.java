package chechelpo.frplm.domain.world.edge;

import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.frameworks.entities.microservices.EntityService;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.jooq.generated.tables.records.LocationNeighborsRecord;
import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EdgeService extends EntityService<LocationNeighborsRecord, EdgeStore> {
    EdgeService(EdgeStore store, EventBus eventBus) {
        super(store, eventBus);
    }

    public @NotNull List<LocationsRecord> getNeighbours(EntityKey<LocationsRecord> key){
        return this.store.getNeighboursOf(key);
    }

}
