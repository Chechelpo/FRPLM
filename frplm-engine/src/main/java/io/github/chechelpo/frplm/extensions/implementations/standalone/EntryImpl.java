package io.github.chechelpo.frplm.extensions.implementations.standalone;

import chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import io.github.chechelpo.frplm.extensions.api.standalone.EntrySnapshot;

public class EntryImpl extends StandaloneEntity<EntryRecord> implements EntrySnapshot {
    public EntryImpl(EntryRecord record, ExtensionContext context) {
        super(record, context);
    }

    @Override
    public Reference asReference() {
        return new EntrySnapshot.Reference(record.getLorebookId(), record.getEntryId());
    }
}
