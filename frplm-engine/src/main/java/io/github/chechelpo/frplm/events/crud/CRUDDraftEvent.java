package io.github.chechelpo.frplm.events.crud;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import org.jetbrains.annotations.NotNull;
import org.jooq.TableRecord;

import java.util.Optional;

/**
 * Signals an impending CRUD action which may or <b> not </b> succeed.
 */
public sealed interface CRUDDraftEvent extends CRUDEvent{
    record CreateEntityDraft<R extends TableRecord<R>>(
            @NotNull EntityConfigs.Types type,
            long operationID,
            @NotNull EntityKey<R> initialKey,
            @NotNull EntityDataPayload<R> initialData
    ) implements CRUDDraftEvent {}

    record DeleteEntityDraft<R extends TableRecord<R>>(
            @NotNull EntityConfigs.Types type,
            long operationID,
            @NotNull EntityKey<R> key
    ) implements CRUDDraftEvent {}

    record UpdateEntityDraft<R extends TableRecord<R>>(
            @NotNull EntityConfigs.Types type,
            long operationID,
            @NotNull EntityKey<R> target,
            @NotNull EntityDataPayload<R> newData
    ) implements CRUDDraftEvent {}

}
