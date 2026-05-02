package chechelpo.frplm.domain.lorebook.entry.core;

import chechelpo.frplm.config.controllers.EntityTypes;
import chechelpo.frplm.domain.lorebook.core.LorebookService;
import chechelpo.frplm.frameworks.entities.microservices.EntityDataPayload;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityService;
import chechelpo.frplm.jooq.generated.tables.Entry;
import chechelpo.frplm.jooq.generated.tables.Lorebooks;
import chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public final class EntryService extends ABSEntityService<EntryRecord, EntryStore> {
    private final LorebookService lorebooks;
    public EntryService(EntryStore entriesStore, LorebookService lorebooks) {
        super(entriesStore, EntityTypes.Types.ENTRIES);
        this.lorebooks = lorebooks;
    }

    @Override
    protected @NotNull EntityDataPayload<EntryRecord> beforeCreate(EntityDataPayload<EntryRecord> data) {
        this.coercePayload(data);
        data.setValue(
                Entry.ENTRY.ENTRY_ID,
                lorebooks.getAndIncrement(
                        Lorebooks.LOREBOOKS.NEXT_ENTRY_ID,
                        EntityKey.<LorebooksRecord>builder()
                                .set(Lorebooks.LOREBOOKS.ID, data.getValue(Entry.ENTRY.LOREBOOK_ID))
                                .build()
                )
        );
        return super.beforeCreate(data);
    }

    public @NotNull List<EntryRecord> getOfLorebook(Integer lorebookId) {
        return this.store.getOfLorebook(lorebookId);
    }
}
