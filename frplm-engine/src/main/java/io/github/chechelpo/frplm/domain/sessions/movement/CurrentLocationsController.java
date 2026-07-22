package io.github.chechelpo.frplm.domain.sessions.movement;

import io.github.chechelpo.frplm.domain.character.core.CharacterController;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityController;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CurrentLocationsRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

import static io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs.CURRENT_LOCATIONS_URL;

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
        CharactersRecord[] atLocation = service.getAtLocation(sessionID, locationID);
        return ResponseEntity.ok(
                characterController.wrapEntities(atLocation)
        );
    }
}
