package io.github.chechelpo.frplm.events.crud;

import org.jetbrains.annotations.NotNull;
import org.jooq.Table;
import org.jooq.TableRecord;

public sealed interface CRUDEvent<R extends TableRecord<R>> permits CRUDCommittedEvent, CRUDDraftEvent {
    long operationID();
    @NotNull Table<R> table();

    default boolean isEventOf(Table<?> table){
        return table().equals(table);
    }
    default boolean isNotEventOf(Table<?> table){
        return !isEventOf(table);
    }

}