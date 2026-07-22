package io.github.chechelpo.frplm.extensions.mapper;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityReader;
import io.github.chechelpo.frplm.extensions.api.standalone.EntrySnapshot;
import io.github.chechelpo.frplm.extensions.implementations.standalone.EntryImpl;
import io.github.chechelpo.frplm.jooq.generated.tables.records.EntryRecord;

import static io.github.chechelpo.frplm.jooq.generated.Tables.ENTRY;

final class EntryMapper extends ReferenceMapper<EntryRecord, EntrySnapshot.Reference, EntrySnapshot> {
    EntryMapper(EntityReader<EntryRecord> reader) {
        super(
                EntrySnapshot.class,
                EntrySnapshot.Reference::fromString,
                EntryImpl::new,
                reference -> EntityKey.<EntryRecord>builder()
                        .set(ENTRY.LOREBOOK_ID, reference.lorebookId())
                        .set(ENTRY.ENTRY_ID, reference.entryId())
                        .build(),
                reader
        );
    }

    @Override
    EntrySnapshot.Reference getExampleReference() {
        return new EntrySnapshot.Reference(1, 1);
    }
}
