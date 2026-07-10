package io.github.chechelpo.frplm.domain.character.core;

import io.github.chechelpo.frplm.domain.EntityTypes;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityController;
import chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping(EntityTypes.CHARACTERS_URL)
public final class CharacterController extends EntityController<
        CharactersRecord,
        CharacterService
        >
{

    CharacterController(CharacterService service) {
        super(service);
    }

    @GetMapping( "/{worldID}")
    ResponseEntity<EntityDTO[]> getStartingAt(@PathVariable int worldID) {
        return ResponseEntity.ok(wrapEntities(
                service.getStartingAt(worldID)
        ));
    }

    @GetMapping("/startingAt")
    ResponseEntity<EntityDTO[]> getStartingAt(@RequestParam int worldId, @RequestParam int locationId) {
        return ResponseEntity.ok(
                wrapEntities(
                        service.getStartingAt(worldId, locationId)
                )
        );
    }

}
