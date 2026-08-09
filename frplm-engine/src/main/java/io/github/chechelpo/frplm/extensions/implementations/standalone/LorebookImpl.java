package io.github.chechelpo.frplm.extensions.implementations.standalone;

import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.extensions.api.standalone.EntrySnapshot;
import io.github.chechelpo.frplm.extensions.api.standalone.LorebookSnapshot;
import io.github.chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.ENTRY;

public class LorebookImpl extends StandaloneEntity<LorebooksRecord> implements LorebookSnapshot {
    public LorebookImpl(LorebooksRecord record, ExtensionContext context) {
        super(record, context);
    }

    @Override
    public Reference asReference() {
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


    public List<EntryRecord> getElegible(IntSet keywordIds){
        return context.entries().getEntriesWith(IntSet.of(record.getId()), keywordIds);
    }
}
