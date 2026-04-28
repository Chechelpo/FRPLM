package chechelpo.demo.domain.lorebook.entry;

import chechelpo.demo.config.controllers.EntityTypes;
import chechelpo.demo.domain.lorebook.main.LorebookService;
import chechelpo.demo.frameworks.entities.data.EntityDataPayload;
import chechelpo.demo.frameworks.entities.data.EntityKey;
import chechelpo.demo.frameworks.entities.microservices.ABSEntityService;
import chechelpo.demo.jooq.generated.tables.Entry;
import chechelpo.demo.jooq.generated.tables.Lorebooks;
import chechelpo.demo.jooq.generated.tables.records.EntryRecord;
import chechelpo.demo.jooq.generated.tables.records.LorebooksRecord;
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
