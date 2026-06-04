package chechelpo.frplm.domain.sessions.movement;

import chechelpo.frplm.domain.character.core.CharacterController;
import chechelpo.frplm.core.entities.pseudo_services.EntityController;
import chechelpo.frplm.jooq.generated.tables.records.CurrentLocationsRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static chechelpo.frplm.domain.EntityTypes.CURRENT_LOCATIONS_URL;

@RestController
@RequestMapping(CURRENT_LOCATIONS_URL)
final class CurrentLocationsController extends EntityController<CurrentLocationsRecord, CurrentLocationService> {
    private final CharacterController characterController;
    CurrentLocationsController(CurrentLocationService service, CharacterController characterController) {
        super(service);
        this.characterController = characterController;
    }

    @GetMapping("/{sessionID}/{locationID}")
    public ResponseEntity<EntityDTO[]> getAtLocation(@PathVariable int sessionID, @PathVariable int locationID) {
        return ResponseEntity.ok(
                characterController.wrapEntities(service.getAtLocation(sessionID,locationID))
        );
    }
}
