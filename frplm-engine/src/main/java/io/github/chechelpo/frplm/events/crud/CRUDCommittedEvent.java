package io.github.chechelpo.frplm.events.crud;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import org.jooq.TableRecord;

import java.util.List;

public sealed interface CRUDCommittedEvent extends CRUDEvent {
    record CreatedEntity<R extends TableRecord<R>>
            (EntityConfigs.Types type, long operationID, EntityKey<R> key, R record) implements CRUDCommittedEvent {}
    record DeletedEntity<R extends TableRecord<R>>
            (EntityConfigs.Types type, long operationID, EntityKey<R> key, R deletedRecord) implements CRUDCommittedEvent {}
    record UpdatedEntity<R extends TableRecord<R>>
            (EntityConfigs.Types type, long operationID, EntityKey<R> target, EntityDataPayload<R> updatedData) implements CRUDCommittedEvent {}
    record RetrievedEntities<R extends TableRecord<R>>
            (EntityConfigs.Types type, long operationID, List<EntityKey<R>> targets, List<R> recordsToReturn) implements CRUDCommittedEvent {}
}
