package chechelpo.demo.domain.character.tags;

import chechelpo.demo.config.controllers.EntityTypes;
import chechelpo.demo.frameworks.entities.microservices.EntityKey;
import chechelpo.demo.frameworks.entities.microservices.ABSEntityService;
import chechelpo.demo.jooq.generated.tables.records.CharacterTagsRecord;
import chechelpo.demo.jooq.generated.tables.records.TagsRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public final class CharacterTagsService extends ABSEntityService<CharacterTagsRecord, CharacterTagsStore> {
    CharacterTagsService(CharacterTagsStore store) {
        super(store, EntityTypes.Types.CHARACTER_TAGS);
    }

    public @NotNull List<TagsRecord> getTagsOfCharacter(EntityKey<CharacterTagsRecord> key) {
        throwIfNotPartialKey(key);
        return store.getCharacterTags(key);
    }
}
