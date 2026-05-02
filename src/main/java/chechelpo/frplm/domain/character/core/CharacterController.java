package chechelpo.frplm.domain.character.core;

import chechelpo.frplm.domain.character.assets.CharacterAvatars;
import chechelpo.frplm.config.controllers.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityController;
import chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
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
