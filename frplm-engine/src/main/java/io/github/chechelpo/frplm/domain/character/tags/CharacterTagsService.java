package io.github.chechelpo.frplm.domain.character.tags;

import io.github.chechelpo.frplm.core.entities.fields.FieldValidator;
import io.github.chechelpo.frplm.events.EventBus;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharacterTagsRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.TagsRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CharacterTagsService extends EntityService<CharacterTagsRecord, CharacterTagsStore> {
    CharacterTagsService(
            CharacterTagsStore store,
            FieldValidator<CharacterTagsRecord> validator,
            EventBus eventBus
    ) {
        super(store, validator, eventBus);
    }

    public @NotNull List<TagsRecord> getTagsOfCharacter(EntityKey<CharacterTagsRecord> key) {
        fieldValidator.validateKey(key).orElseThrow();
        return store.getCharacterTags(key);
    }

    public boolean characterHasTag(int characterId, int tagId){
        return store.characterHasTag(characterId, tagId);
    }
}
