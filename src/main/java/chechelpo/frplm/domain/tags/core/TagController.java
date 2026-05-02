package chechelpo.frplm.domain.tags.core;

import chechelpo.frplm.config.controllers.EntityTypes;
import chechelpo.frplm.domain.character.tags.CharacterTagsService;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityController;
import chechelpo.frplm.jooq.generated.tables.CharacterTags;
import chechelpo.frplm.jooq.generated.tables.records.CharacterTagsRecord;
import chechelpo.frplm.jooq.generated.tables.records.TagsRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static chechelpo.frplm.config.controllers.EntityTypes.CHARACTER_TAGS_str;
import static chechelpo.frplm.config.controllers.EntityTypes.TAGS_URL;

@RestController
@RequestMapping(TAGS_URL)
final class TagController extends ABSEntityController<TagsRecord, TagService> {
    CharacterTagsService characterTagsService;
    public TagController(TagService service, CharacterTagsService characterTagsService) {
        super(EntityTypes.Types.TAGS, service);
        this.characterTagsService = characterTagsService;
    }

    @GetMapping( "/" + CHARACTER_TAGS_str + "/{id}")
    public ResponseEntity<EntityDTO[]> getTagsOfCharacter(@PathVariable("id") Integer id) {
        this.log.info("Getting tags of character with id {}", id);
        EntityKey.Builder<CharacterTagsRecord> builder = EntityKey.builder();

        List<TagsRecord> records = characterTagsService.getTagsOfCharacter(builder
                .set(CharacterTags.CHARACTER_TAGS.CHAR_ID, id)
                .build()
        );

        return ResponseEntity.ok(
                wrapEntities(records)
        );
    }

}
