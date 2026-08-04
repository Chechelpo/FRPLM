package io.github.chechelpo.frplm.domain.tags;

import io.github.chechelpo.frplm.core.entities.pseudo_services.DTOMapper;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDTO;
import io.github.chechelpo.frplm.domain.character.tags.CharacterTagsService;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityController;
import io.github.chechelpo.frplm.jooq.generated.tables.CharacterTags;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharacterTagsRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.TagsRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs.CHARACTER_TAGS_str;
import static io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs.TAGS_URL;

@RestController
@RequestMapping(TAGS_URL)
final class TagController extends EntityController<TagsRecord, TagService> {
    CharacterTagsService characterTagsService;
    public TagController(TagService service, CharacterTagsService characterTagsService, DTOMapper<TagsRecord> mapper) {
        super(service, mapper);
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
