package io.github.chechelpo.frplm.core.entities.pseudo_services;

import org.jooq.Result;
import org.jooq.TableField;
import org.jooq.TableRecord;

import java.util.Optional;

public interface EntityReader<R extends TableRecord<R>> {
    Optional<R> find(EntityKey<R> target);

    @Deprecated
    Result<R> getMatching(EntityKey<R> target);

    Result<R> getMatching(EntityDataPayload<R> target);

    <T> Result<R> getMatching(TableField<R, T> field, T value);

    /**
     * @throws IllegalStateException if it finds more than one result to your query
     * @return query result, may be null or a single record
     */
    Optional<R> getOneMatching(EntityDataPayload<R> target);

    Result<R> getAll();
}
