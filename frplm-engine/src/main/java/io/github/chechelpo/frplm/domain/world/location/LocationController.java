package io.github.chechelpo.frplm.domain.world.location;

import io.github.chechelpo.frplm.domain.character.starting_locations.StartingLocationsService;
import io.github.chechelpo.frplm.domain.world.edge.EdgeService;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityController;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.domain.world.region.RegionService;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import static io.github.chechelpo.frplm.config.controllers.ControllerPaths.ENTITY_PATH;
import static chechelpo.frplm.jooq.generated.Tables.*;

@RestController
@Component
@RequestMapping(EntityConfigs.LOCATIONS_URL)
public final class LocationController extends EntityController<LocationsRecord, LocationsService> {
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
    public ResponseEntity<EntityDTO[]> getLocationsOfRegion(@RequestParam int worldId, @RequestParam(required = false) Integer regionId){
        if (regionId !=null && !regionService.exists(EntityKey.<RegionRecord>builder()
                .set(REGION.WORLD_ID, worldId)
                .set(REGION.ID, regionId)
                .build()
        )) throw new EntityNotFound("No such region", Severity.USER);

        return ResponseEntity.ok(
                wrapEntities(
                        service.getLocationsOfRegion(worldId, regionId)
                )
        );
    }
}
