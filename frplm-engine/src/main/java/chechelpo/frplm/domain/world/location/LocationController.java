package chechelpo.frplm.domain.world.location;

import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.domain.character.starting_locations.StartingLocationsService;
import chechelpo.frplm.domain.world.edge.EdgeService;
import chechelpo.frplm.core.entities.pseudo_services.EntityController;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.domain.world.region.RegionService;
import chechelpo.frplm.exceptions.Severity;
import chechelpo.frplm.exceptions.runtime.EntityNotFound;
import chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static chechelpo.frplm.config.controllers.ControllerPaths.ENTITY_PATH;
import static chechelpo.frplm.jooq.generated.Tables.*;

@RestController
@RequestMapping(EntityTypes.LOCATIONS_URL)
final class LocationController extends EntityController<LocationsRecord, LocationsService> {
    private final EdgeService edgeService;
    private final StartingLocationsService startLocService;
    private final RegionService regionService;

    LocationController(LocationsService service, EdgeService edgeService, StartingLocationsService startLocService, RegionService regionService) {
        super(service);
        this.edgeService = edgeService;
        this.startLocService = startLocService;
        this.regionService = regionService;
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

    @GetMapping("/ofRegion")
    public ResponseEntity<EntityDTO[]> getOfRegion(@RequestParam int worldId, @RequestParam int regionId){
        RegionRecord region = regionService.find(EntityKey.<RegionRecord>builder()
                .set(REGION.WORLD_ID, worldId)
                .set(REGION.ID, regionId)
                .build()
        ).orElseThrow(() -> new EntityNotFound("No such region", Severity.USER));

        return ResponseEntity.ok(
                wrapEntities(
                        service.getLocationsOfRegion(region)
                )
        );
    }
}
