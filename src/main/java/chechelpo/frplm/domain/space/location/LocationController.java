package chechelpo.frplm.domain.space.location;

import chechelpo.frplm.config.controllers.EntityTypes;
import chechelpo.frplm.domain.space.edge.EdgeService;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityController;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static chechelpo.frplm.config.controllers.ControllerPaths.ENTITY_PATH;
import static chechelpo.frplm.jooq.generated.Tables.LOCATIONS;

@RestController
@RequestMapping(EntityTypes.LOCATIONS_URL)
final class LocationController extends ABSEntityController<LocationsRecord, LocationsService> {
    private final EdgeService edgeService;

    LocationController(LocationsService service, EdgeService edgeService) {
        super(EntityTypes.Types.LOCATIONS, service);
        this.edgeService = edgeService;
    }

    @GetMapping(ENTITY_PATH + "/ofWorld/{worldID}")
    public ResponseEntity<EntityDTO[]> getLocationsOfWorld(@PathVariable Integer worldID) {
        EntityKey.Builder<LocationsRecord> builder = EntityKey.builder();
        return ResponseEntity.ok(
                wrapEntities(
                        service.getMatching(builder
                                .set(LOCATIONS.WORLD_ID, worldID)
                                .build()
                        )
                )
        );
    }

    @GetMapping(ENTITY_PATH + "/ofLocation/{worldID}/{locationID}")
    public ResponseEntity<EntityDTO[]> getLocationsOfWorld(@PathVariable Integer worldID, @PathVariable Integer locationID) {
        EntityKey.Builder<LocationsRecord> builder = EntityKey.builder();
        return ResponseEntity.ok(
                wrapEntities(
                        edgeService.getNeighbours(builder
                                .set(LOCATIONS.WORLD_ID, worldID)
                                .set(LOCATIONS.ID, locationID)
                                .build()
                        )
                )
        );
    }
}
