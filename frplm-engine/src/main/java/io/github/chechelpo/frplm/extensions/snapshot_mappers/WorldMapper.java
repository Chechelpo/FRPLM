package io.github.chechelpo.frplm.extensions.snapshot_mappers;

import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityReader;
import io.github.chechelpo.frplm.extensions.api.standalone.WorldSnapshot;
import io.github.chechelpo.frplm.extensions.implementations.standalone.WorldImpl;
import io.github.chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;

import static io.github.chechelpo.frplm.jooq.generated.Tables.WORLDS;

final class WorldMapper extends ReferenceMapper<WorldsRecord, WorldSnapshot.Reference, WorldSnapshot> {
    WorldMapper(EntityReader<WorldsRecord> worldReader) {
        super(
                WorldSnapshot.class,
                WorldSnapshot.Reference::fromString,
                WorldImpl::new,
                reference -> EntityKey.of(WORLDS.ID, reference.id()),
                worldReader
        );
    }

    @Override
    WorldSnapshot.Reference getExampleReference() {
        return new WorldSnapshot.Reference(1);
    }
}
