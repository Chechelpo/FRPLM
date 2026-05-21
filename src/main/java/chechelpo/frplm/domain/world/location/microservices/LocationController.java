package chechelpo.frplm.domain.world.location.microservices;

import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.domain.character.starting_locations.StartingLocationsService;
import chechelpo.frplm.domain.world.edge.EdgeService;
import chechelpo.frplm.frameworks.entities.microservices.EntityController;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static chechelpo.frplm.config.controllers.ControllerPaths.ENTITY_PATH;
import static chechelpo.frplm.jooq.generated.Tables.CHARACTERS;
import static chechelpo.frplm.jooq.generated.Tables.LOCATIONS;

@RestController
@RequestMapping(EntityTypes.LOCATIONS_URL)
final class LocationController extends EntityController<LocationsRecord, LocationsService> {
    private final EdgeService edgeService;
    private final StartingLocationsService startLocService;

    LocationController(LocationsService service, EdgeService edgeService, StartingLocationsService startLocService) {
        super(EntityTypes.Types.LOCATIONS, service);
        this.edgeService = edgeService;
        this.startLocService = startLocService;
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
    public ResponseEntity<EntityDTO[]> getNeighboursOf(@PathVariable Integer worldID, @PathVariable Integer locationID) {
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
    @GetMapping(ENTITY_PATH + "/ofCharacter/{characterID}")
    public ResponseEntity<EntityDTO[]> getStartingLocationsOf(@PathVariable(required = true) Integer characterID){
        EntityKey.Builder<CharactersRecord> builder = EntityKey.builder();
        return ResponseEntity.ok(
                wrapEntities(
                        startLocService.getStartingLocationsOf(
                                builder
                                        .set(CHARACTERS.ID, characterID)
                                        .build()
                        )
                )
        );
    }
}
