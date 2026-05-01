package chechelpo.demo.domain.tags.core;

import chechelpo.demo.config.controllers.EntityTypes;
import chechelpo.demo.events.EventListener;
import chechelpo.demo.events.EventManager;
import chechelpo.demo.events.types.DeletedEntity;
import chechelpo.demo.events.types.Event;
import chechelpo.demo.frameworks.entities.microservices.EntityKey;
import chechelpo.demo.frameworks.entities.microservices.ABSEntityService;
import chechelpo.demo.jooq.generated.tables.records.CharacterTagsRecord;
import chechelpo.demo.jooq.generated.tables.records.TagsRecord;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static chechelpo.demo.jooq.generated.Tables.CHARACTER_TAGS;
import static chechelpo.demo.jooq.generated.tables.Tags.TAGS;

@Component
public final class TagService extends ABSEntityService<TagsRecord, TagStore> implements EventListener {
    TagService(TagStore store) {
        super(store, EntityTypes.Types.TAGS);
        EventManager.subscribe(this);
    }

    @Override
    public void onEvent(Event event) {
        if (Objects.requireNonNull(event) instanceof DeletedEntity del) {
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
