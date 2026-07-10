package io.github.chechelpo.frplm.domain.character.tags;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityController;
import chechelpo.frplm.jooq.generated.tables.records.CharacterTagsRecord;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs.CHARACTER_TAGS_URL;

@RestController
@RequestMapping(CHARACTER_TAGS_URL)
final class CharacterTagsController extends EntityController<CharacterTagsRecord, CharacterTagsService> {
    public CharacterTagsController(CharacterTagsService service) {
        super(service);
    }
}
