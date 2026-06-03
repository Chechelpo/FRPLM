package chechelpo.frplm.domain.character.core;

import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.frameworks.entities.pseudo_services.EntityController;
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
    ResponseEntity<EntityDTO[]> getStartingAt(@PathVariable("worldID") int worldID) {
        return ResponseEntity.ok(wrapEntities(
                service.getStartingAt(worldID)
        ));
    }

}
