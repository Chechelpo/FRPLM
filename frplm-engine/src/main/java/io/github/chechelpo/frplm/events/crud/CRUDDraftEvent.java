package io.github.chechelpo.frplm.events.crud;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import org.jetbrains.annotations.NotNull;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableRecord;

/**
 * Signals an impending CRUD action which may or <b> not </b> succeed.
 */
public sealed interface CRUDDraftEvent<R extends TableRecord<R>> extends CRUDEvent<R> {

    record CreateEntityDraft<R extends TableRecord<R>>(
            Table<R> table,
            long operationID,
            @NotNull EntityKey<R> initialKey,
            @NotNull EntityDataPayload<R> initialData
    ) implements CRUDDraftEvent<R> { }

    record DeleteEntityDraft<R extends TableRecord<R>>(
            Table<R> table,
            long operationID,
            @NotNull EntityKey<R> key
    ) implements CRUDDraftEvent<R> {}

    record UpdateEntityDraft<R extends TableRecord<R>>(
            Table<R> table,
            long operationID,
            @NotNull EntityKey<R> target,
            @NotNull EntityDataPayload<R> newData
    ) implements CRUDDraftEvent<R> {
        public boolean eventUpdates(TableField<R, ?> field){
            return newData.assigns(field);
        }
    }

}
