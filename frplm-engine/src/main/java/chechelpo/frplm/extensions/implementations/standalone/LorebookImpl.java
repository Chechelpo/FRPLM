package chechelpo.frplm.extensions.implementations.standalone;

import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.extensions.api.standalone.EntrySnapshot;
import chechelpo.frplm.extensions.api.standalone.LorebookSnapshot;
import chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;

import static chechelpo.frplm.jooq.generated.Tables.ENTRY;

public class LorebookImpl extends StandaloneEntity<LorebooksRecord> implements LorebookSnapshot {
    LorebookImpl(LorebooksRecord record, ExtensionContext context) {
        super(record, context);
    }

    @Override
    public Reference reference() {
        return new LorebookSnapshot.Reference(getRecord().getId());
    }

    @Override
    public String getName() {
        return record.getName();
    }

    @Override
    public EntrySnapshot[] getEntries() {
        return context.entries().getMatching(EntityKey.of(ENTRY.LOREBOOK_ID, record.getId())).stream()
                .map(record -> new EntryImpl(record, this.context))
                .toArray(EntrySnapshot[]::new);
    }
}
