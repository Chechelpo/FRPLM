package io.github.chechelpo.frplm.utils.stable_records;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TableRecord;

import java.util.Optional;

public interface StableRecord<R extends TableRecord<R>> {
    void runCustomConfig(DSLContext ctx);
    Table<R> getTable();
    Optional<EntityDataPayload<R>> toPayload();
    Optional<EntityKey<R>> toKey();
}
