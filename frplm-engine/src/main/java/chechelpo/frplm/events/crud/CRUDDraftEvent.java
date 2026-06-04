package chechelpo.frplm.events.crud;

import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import org.jetbrains.annotations.NotNull;
import org.jooq.TableRecord;

import java.util.Optional;

/**
 * Signals an impending CRUD action which may or <b> not </b> succeed.
 */
public sealed interface CRUDDraftEvent extends CRUDEvent{
    record CreateEntityDraft<R extends TableRecord<R>>(
            @NotNull EntityTypes.Types type,
            long operationID,
            @NotNull Optional<EntityKey<R>> initialKey,
            @NotNull EntityDataPayload<R> initialData
    ) implements CRUDDraftEvent {}

    record DeleteEntityDraft<R extends TableRecord<R>>(
            @NotNull EntityTypes.Types type,
            long operationID,
            @NotNull EntityKey<R> key
    ) implements CRUDDraftEvent {}

    record UpdateEntityDraft<R extends TableRecord<R>>(
            @NotNull EntityTypes.Types type,
            long operationID,
            @NotNull EntityKey<R> target,
            @NotNull EntityDataPayload<R> newData
    ) implements CRUDDraftEvent {}

}
