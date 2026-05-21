package chechelpo.frplm.domain.character.core;

import chechelpo.frplm.domain.character.assets.CharacterAvatars;
import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.EntityController;
import chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping(EntityTypes.CHARACTERS_URL)
final class CharacterController extends EntityController<
        CharactersRecord,
        CharacterService
        >
{
    private final CharacterAvatars avatars;

    CharacterController(CharacterService service, CharacterAvatars avatars) {
        super(service);
        this.avatars = avatars;
    }

    @GetMapping( "/{worldID}")
    ResponseEntity<EntityDTO[]> getStartingAt(@PathVariable("worldID") int worldID) {
        return ResponseEntity.ok(wrapEntities(
                service.getStartingAt(worldID)
        ));
    }
}
