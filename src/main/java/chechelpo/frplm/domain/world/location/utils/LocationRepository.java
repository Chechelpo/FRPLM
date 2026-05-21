package chechelpo.frplm.domain.world.location.utils;

import chechelpo.frplm.domain.world.edge.EdgeService;
import chechelpo.frplm.domain.world.location.microservices.LocationsService;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.frameworks.entities.repository.EntityRepository;
import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
final class LocationRepository extends EntityRepository<LocationsRecord, LocationsService> {
    private final EdgeService edgeService;
    LocationRepository(LocationsService service, EdgeService edgeService) {
        super(service);
        this.edgeService = edgeService;
    }

    @NotNull EntityKey<LocationsRecord> @NotNull [] getEdges(EntityKey<LocationsRecord> key) {
        return edgeService.getNeighbours(key).stream().map(service::keyOf).toArray(EntityKey[]::new);
    }
}
