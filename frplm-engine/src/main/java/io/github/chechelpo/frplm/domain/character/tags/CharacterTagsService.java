package io.github.chechelpo.frplm.domain.character.tags;

import io.github.chechelpo.frplm.events.EventBus;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityService;
import chechelpo.frplm.jooq.generated.tables.records.CharacterTagsRecord;
import chechelpo.frplm.jooq.generated.tables.records.TagsRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CharacterTagsService extends EntityService<CharacterTagsRecord, CharacterTagsStore> {
    CharacterTagsService(CharacterTagsStore store, EventBus eventBus) {
        super(store, eventBus);
    }

    public @NotNull List<TagsRecord> getTagsOfCharacter(EntityKey<CharacterTagsRecord> key) {
        throwIfInvalidKey(key, false);
        return store.getCharacterTags(key);
    }
}
