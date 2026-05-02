package chechelpo.frplm.domain.tags.core;

import chechelpo.frplm.config.controllers.EntityTypes;
import chechelpo.frplm.events.EventListener;
import chechelpo.frplm.events.EventManager;
import chechelpo.frplm.events.Event;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityService;
import chechelpo.frplm.jooq.generated.tables.records.CharacterTagsRecord;
import chechelpo.frplm.jooq.generated.tables.records.TagsRecord;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static chechelpo.frplm.jooq.generated.Tables.CHARACTER_TAGS;
import static chechelpo.frplm.jooq.generated.tables.Tags.TAGS;

@Component
public final class TagService extends ABSEntityService<TagsRecord, TagStore> implements EventListener {
    TagService(TagStore store) {
        super(store, EntityTypes.Types.TAGS);
        EventManager.subscribe(this);
    }

    @Override
    public void onEvent(Event event) {
        if (Objects.requireNonNull(event) instanceof Event.DeletedEntity del) {
            if (del.type().equals(EntityTypes.Types.CHARACTER_TAGS)) {
                EntityKey.Builder<TagsRecord> builder = new EntityKey.Builder<>();
                EntityKey<CharacterTagsRecord> deletedKey = (EntityKey<CharacterTagsRecord>) del.key();

                EntityKey<TagsRecord> key = builder.set(
                        TAGS.ID,
                        deletedKey.getValue(CHARACTER_TAGS.TAG_ID)
                ).build();

                if (this.store.countUsages(key) == 0)
                    this.delete(key);
            }
        }
    }
}
