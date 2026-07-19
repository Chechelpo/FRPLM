package io.github.chechelpo.frplm.core.entities.pseudo_services;

import org.jooq.TableField;
import org.jooq.TableRecord;

public interface EntityCreator<R extends TableRecord<R>> {
    R createAndGet(EntityDataPayload<R> data);
    <T> T createAndGet(EntityDataPayload<R> data, TableField<R,T> field);
}
