package io.github.chechelpo.frplm.events.crud;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import org.jetbrains.annotations.NotNull;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableRecord;

import java.util.List;

public sealed interface CRUDCommittedEvent<R extends TableRecord<R>> extends CRUDEvent<R> {


    record CreatedEntity<R extends TableRecord<R>>(long operationID, EntityKey<R> key, R record)
            implements CRUDCommittedEvent<R> {
        @Override
        public @NotNull Table<R> table() {
            return record.getTable();
        }
    }
    record DeletedEntity<R extends TableRecord<R>>
            (long operationID, EntityKey<R> key, R deletedRecord) implements CRUDCommittedEvent<R>
    {
        @Override
        public @NotNull Table<R> table() {
            return deletedRecord.getTable();
        }
    }
    record UpdatedEntity<R extends TableRecord<R>>
            (
                    R previousData,
                    long operationID,
                    EntityKey<R> target,
                    EntityDataPayload<R> updatedData
            ) implements CRUDCommittedEvent<R>
    {
        @Override
        public @NotNull Table<R> table() {
            return previousData.getTable();
        }

        public boolean assigned(TableField<R, ?> field) {
            return updatedData.assigns(field);
        }
    }
    record RetrievedEntities<R extends TableRecord<R>>
            (long operationID, List<EntityKey<R>> targets, List<R> recordsToReturn) implements CRUDCommittedEvent<R> {
        @Override
        public @NotNull Table<R> table() {
            return null;
        }
    }
}
