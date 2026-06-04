package chechelpo.frplm.domain.lorebook.entry.core;

import chechelpo.frplm.domain.lorebook.core.LorebookService;
import chechelpo.frplm.domain.lorebook.outlet.OutletService;
import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.exceptions.Severity;
import chechelpo.frplm.exceptions.runtime.InvalidValue;
import chechelpo.frplm.exceptions.runtime.UnexpectedException;
import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.core.entities.pseudo_services.EntityService;
import chechelpo.frplm.jooq.generated.tables.Entry;
import chechelpo.frplm.jooq.generated.tables.Lorebooks;
import chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static chechelpo.frplm.jooq.generated.Tables.ENTRY;
import static chechelpo.frplm.jooq.generated.Tables.LOREBOOKS;

@Component
public class EntryService extends EntityService<EntryRecord, EntryStore> {
    private final LorebookService lorebooks;
    private final OutletService outlets;

    EntryService(EntryStore entriesStore, LorebookService lorebooks, OutletService outlets, EventBus eventBus) {
        super(entriesStore, eventBus);
        this.lorebooks = lorebooks;
        this.outlets = outlets;
    }

    @Override
    protected void beforeCreate(@NotNull EntityDataPayload<EntryRecord> data, long operationID) {
        EntityKey<LorebooksRecord> parentLorebookKey = EntityKey.of(LOREBOOKS.ID, data.requireValue(Entry.ENTRY.LOREBOOK_ID));

        int newEntryID = lorebooks.incrementAndGet(
                Lorebooks.LOREBOOKS.NEXT_ENTRY_ID,
                parentLorebookKey
        ).orElseThrow(() -> {
            log.error("Could not fetch entryID creating entry\n Assignments: {}", data.assignments());
            return new UnexpectedException("Could not fetch new entry ID", Severity.SYSTEM);
        });
        int defaultOutletID = lorebooks.getValueOf(LOREBOOKS.DEFAULT_OUTLET_ID, parentLorebookKey)
                .orElseThrow(() -> new UnexpectedException("Could not fetch defaultOutletID from parent", Severity.SYSTEM));

        data.set(Entry.ENTRY.ENTRY_ID,newEntryID);
        data.set(ENTRY.OUTLET, defaultOutletID);

        super.beforeCreate(data, operationID);
    }

    @Override
    protected void beforeUpdate(@NotNull EntityKey<EntryRecord> target, @NotNull EntityDataPayload<EntryRecord> data, long operationID) {
        if (data.assignsField(Entry.ENTRY.OUTLET) && data.requireValue(Entry.ENTRY.OUTLET) != null)
            throw new InvalidValue("Cannot update an outlet entry through normal outlet. Use updateOutlet() instead.");
        super.beforeUpdate(target, data, operationID);
    }

    public @NotNull List<EntryRecord> of(@NotNull LorebooksRecord record) {
        return this.of(record.getId());
    }

    public @NotNull List<EntryRecord> of(int lorebookID) {
        return this.store.getOfLorebook(lorebookID);
    }

    public boolean updateOutlet(EntityKey<EntryRecord> id, String newOutletName) {
        return super.update(
                id,
                EntityDataPayload.of(
                        ENTRY.OUTLET,
                        outlets.getOrCreateOutlet(newOutletName)
                )
        );
    }

    public @NotNull Map<Integer, List<EntryRecord>> getByOutletsWith(
            IntSet lorebookIDs,
            IntSet keywordIDs
    ) {
        return store.getEntriesByOutletWith(lorebookIDs, keywordIDs);
    }
}
