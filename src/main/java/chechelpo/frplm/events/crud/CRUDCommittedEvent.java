package chechelpo.frplm.events.crud;

import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.EntityDataPayload;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import org.jooq.TableRecord;

public sealed interface CRUDCommittedEvent extends CRUDEvent {
    record CreatedEntity<R extends TableRecord<R>>
            (EntityTypes.Types type, long operationID, EntityKey<R> key, R record) implements CRUDCommittedEvent {}
    record DeletedEntity<R extends TableRecord<R>>
            (EntityTypes.Types type, long operationID, EntityKey<R> key) implements CRUDCommittedEvent {}
    record UpdatedEntity<R extends TableRecord<R>>
            (EntityTypes.Types type, long operationID, EntityKey<R> target, EntityDataPayload<R> updatedData) implements CRUDCommittedEvent {}
}
