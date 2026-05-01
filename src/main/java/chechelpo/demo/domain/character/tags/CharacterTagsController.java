package chechelpo.demo.domain.character.tags;

import chechelpo.demo.config.controllers.EntityTypes;
import chechelpo.demo.frameworks.entities.microservices.ABSEntityController;
import chechelpo.demo.jooq.generated.tables.records.CharacterTagsRecord;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static chechelpo.demo.config.controllers.EntityTypes.CHARACTER_TAGS_URL;

@RestController
@RequestMapping(CHARACTER_TAGS_URL)
final class CharacterTagsController extends ABSEntityController<CharacterTagsRecord, CharacterTagsService> {
    public CharacterTagsController(CharacterTagsService service) {
        super(EntityTypes.Types.CHARACTER_TAGS, service);
    }
}
