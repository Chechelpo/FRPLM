package io.github.chechelpo.frplm.domain.tags;

import io.github.chechelpo.frplm.core.entities.pseudo_services.FieldValidator;
import io.github.chechelpo.frplm.events.EventBus;
import io.github.chechelpo.frplm.events.crud.CRUDCommittedEvent;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharacterTagsRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.TagsRecord;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import static io.github.chechelpo.frplm.jooq.generated.Tables.CHARACTER_TAGS;
import static io.github.chechelpo.frplm.jooq.generated.tables.Tags.TAGS;

@Component
public class TagService extends EntityService<TagsRecord, TagStore> {
    TagService(TagStore store, FieldValidator<TagsRecord> validator, EventBus eventBus) {
        super(store, validator, eventBus);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener
    public void onDeleteCharacterTag(CRUDCommittedEvent.@NotNull DeletedEntity<?> event) {
        if (event.type() != EntityConfigs.Types.CHARACTER_TAGS) return;

        CRUDCommittedEvent.DeletedEntity<CharacterTagsRecord> del = (CRUDCommittedEvent.DeletedEntity<CharacterTagsRecord>) event;
        EntityKey.Builder<TagsRecord> builder = new EntityKey.Builder<>();
        EntityKey<CharacterTagsRecord> deletedKey = (EntityKey<CharacterTagsRecord>) del.key();

        EntityKey<TagsRecord> key = builder.set(
                TAGS.ID,
                deletedKey.getAssignment(CHARACTER_TAGS.TAG_ID).orElseThrow()
        ).build();

        if (this.store.countUsages(key) == 0)
            this.delete(key);
    }
}
