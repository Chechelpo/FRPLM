package io.github.chechelpo.frplm.interfaces;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import org.jooq.TableRecord;

import java.util.Optional;

public interface StableRecord<R extends TableRecord<R>> {
    Optional<EntityDataPayload<R>> toPayload();
    Optional<EntityKey<R>> toKey();
}
