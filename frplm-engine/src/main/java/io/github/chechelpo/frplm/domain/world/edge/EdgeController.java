package io.github.chechelpo.frplm.domain.world.edge;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityController;
import io.github.chechelpo.frplm.domain.world.location.LocationController;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationEdgesRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static io.github.chechelpo.frplm.jooq.generated.Tables.LOCATIONS;

@RestController
@Component
@RequestMapping(EntityConfigs.EDGES_URL)
final class EdgeController extends EntityController<LocationEdgesRecord, EdgeService> {
    private final LocationController locationController;

    EdgeController(EdgeService service, LocationController locationController) {
        super(service);
        this.locationController = locationController;
    }

    @GetMapping("/{worldId}/{locationId}/neighbours")
    public ResponseEntity<EntityDTO[]> getNeighbours(@PathVariable int locationId, @PathVariable int worldId) {
        return ResponseEntity.ok(
                locationController.wrapEntities(
                        service.neighboursOf(EntityKey.<LocationsRecord>builder()
                                .set(LOCATIONS.WORLD_ID, worldId)
                                .set(LOCATIONS.ID, locationId)
                                .build()
                        )
                )
        );
    }
}