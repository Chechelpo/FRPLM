package chechelpo.frplm.events.crud;

import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import org.jooq.TableRecord;

import java.util.List;

public sealed interface CRUDCommittedEvent extends CRUDEvent {
    record CreatedEntity<R extends TableRecord<R>>
            (EntityTypes.Types type, long operationID, EntityKey<R> key, R record) implements CRUDCommittedEvent {}
    record DeletedEntity<R extends TableRecord<R>>
            (EntityTypes.Types type, long operationID, EntityKey<R> key) implements CRUDCommittedEvent {}
    record UpdatedEntity<R extends TableRecord<R>>
            (EntityTypes.Types type, long operationID, EntityKey<R> target, EntityDataPayload<R> updatedData) implements CRUDCommittedEvent {}
    record RetrievedEntities<R extends TableRecord<R>>
            (EntityTypes.Types type, long operationID, List<EntityKey<R>> targets, List<R> recordsToReturn) implements CRUDCommittedEvent {}
}
