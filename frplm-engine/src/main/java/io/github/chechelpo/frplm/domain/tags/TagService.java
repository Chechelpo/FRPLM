package io.github.chechelpo.frplm.domain.tags;

import io.github.chechelpo.frplm.domain.EntityTypes;
import io.github.chechelpo.frplm.events.EventBus;
import io.github.chechelpo.frplm.events.crud.CRUDCommittedEvent;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityService;
import chechelpo.frplm.jooq.generated.tables.records.CharacterTagsRecord;
import chechelpo.frplm.jooq.generated.tables.records.TagsRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import static chechelpo.frplm.jooq.generated.Tables.CHARACTER_TAGS;
import static chechelpo.frplm.jooq.generated.tables.Tags.TAGS;

@Component
public class TagService extends EntityService<TagsRecord, TagStore> {
    TagService(TagStore store, EventBus eventBus) {
        super(store, eventBus);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener
    public void onDeleteCharacterTag(CRUDCommittedEvent.@NotNull DeletedEntity<?> event) {
        if (event.type() != EntityTypes.Types.CHARACTER_TAGS) return;

        CRUDCommittedEvent.DeletedEntity<CharacterTagsRecord> del = (CRUDCommittedEvent.DeletedEntity<CharacterTagsRecord>) event;
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
