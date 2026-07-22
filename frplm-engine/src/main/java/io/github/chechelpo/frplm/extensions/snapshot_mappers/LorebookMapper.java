package io.github.chechelpo.frplm.extensions.snapshot_mappers;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityReader;
import io.github.chechelpo.frplm.extensions.api.standalone.LorebookSnapshot;
import io.github.chechelpo.frplm.extensions.implementations.standalone.LorebookImpl;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import static io.github.chechelpo.frplm.jooq.generated.Tables.LOREBOOKS;

final class LorebookMapper extends ReferenceMapper<LorebooksRecord, LorebookSnapshot.Reference, LorebookSnapshot> {
    LorebookMapper(EntityReader<LorebooksRecord> lorebookReader) {
        super(
                LorebookSnapshot.class,
                LorebookSnapshot.Reference::fromString,
                LorebookImpl::new,
                reference -> EntityKey.of(LOREBOOKS.ID, reference.id()),
                lorebookReader
        );
    }

    @Contract(" -> new")
    @Override
    LorebookSnapshot.@NonNull Reference getExampleReference() {
        return new LorebookSnapshot.Reference(1);
    }
}
