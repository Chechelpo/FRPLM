package chechelpo.demo.domain.chars.core;

import chechelpo.demo.domain.chars.assets.CharacterAvatars;
import chechelpo.demo.config.controllers.EntityTypes;
import chechelpo.demo.frameworks.entities.microservices.ABSEntityController;
import chechelpo.demo.jooq.generated.tables.records.CharactersRecord;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping(EntityTypes.CHARACTERS_URL)
final class CharacterController extends ABSEntityController<
        CharactersRecord,
        CharacterService
        >
{
    private final CharacterAvatars avatars;

    CharacterController(CharacterService service, CharacterAvatars avatars) {
        super(EntityTypes.Types.CHARACTER, service);
        this.avatars = avatars;
    }

}
