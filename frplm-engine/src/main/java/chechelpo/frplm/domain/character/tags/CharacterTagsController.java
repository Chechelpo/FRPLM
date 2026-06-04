package chechelpo.frplm.domain.character.tags;

import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.core.entities.pseudo_services.EntityController;
import chechelpo.frplm.jooq.generated.tables.records.CharacterTagsRecord;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static chechelpo.frplm.domain.EntityTypes.CHARACTER_TAGS_URL;

@RestController
@RequestMapping(CHARACTER_TAGS_URL)
final class CharacterTagsController extends EntityController<CharacterTagsRecord, CharacterTagsService> {
    public CharacterTagsController(CharacterTagsService service) {
        super(service);
    }
}
