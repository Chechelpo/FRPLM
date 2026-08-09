package io.github.chechelpo.frplm.extensions.snapshot_mappers;

import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityReader;
import io.github.chechelpo.frplm.extensions.api.standalone.ConnectionSnapshot;
import io.github.chechelpo.frplm.extensions.implementations.standalone.ConnectionImpl;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LlmConnectionRecord;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import static io.github.chechelpo.frplm.jooq.generated.Tables.LLM_CONNECTION;

final class ConnectionMapper extends ReferenceMapper<LlmConnectionRecord, ConnectionSnapshot.Reference, ConnectionSnapshot> {
    ConnectionMapper(EntityReader<LlmConnectionRecord> reader) {
        super(
                ConnectionSnapshot.class,
                ConnectionSnapshot.Reference::fromString,
                ConnectionImpl::new,
                reference -> EntityKey.of(LLM_CONNECTION.ID, reference.id()),
                reader
        );
    }

    @Contract(" -> new")
    @Override
    ConnectionSnapshot.@NonNull Reference getExampleReference() {
        return new ConnectionSnapshot.Reference(1);
    }
}
