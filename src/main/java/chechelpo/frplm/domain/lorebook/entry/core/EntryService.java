package chechelpo.frplm.domain.lorebook.entry.core;

import chechelpo.frplm.domain.lorebook.core.LorebookService;
import chechelpo.frplm.domain.lorebook.outlet.OutletService;
import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.exceptions.types.InvalidValue;
import chechelpo.frplm.frameworks.entities.microservices.EntityDataPayload;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.frameworks.entities.microservices.EntityService;
import chechelpo.frplm.jooq.generated.tables.Entry;
import chechelpo.frplm.jooq.generated.tables.Lorebooks;
import chechelpo.frplm.jooq.generated.tables.records.EntryRecord;
import chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.List;

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
        EntityKey<LorebooksRecord> parentLorebookKey = EntityKey.of(LOREBOOKS.ID, data.getValue(Entry.ENTRY.LOREBOOK_ID));
        data.set(
                Entry.ENTRY.ENTRY_ID,
                lorebooks.getAndIncrement(
                        Lorebooks.LOREBOOKS.NEXT_ENTRY_ID,
                        parentLorebookKey
                )
        );
        data.set(
                ENTRY.OUTLET,
                lorebooks.getValueOf(LOREBOOKS.DEFAULT_OUTLET_ID, parentLorebookKey)
        );

        super.beforeCreate(data, operationID);
    }

    @Override
    protected void beforeUpdate(@NotNull EntityKey<EntryRecord> target, @NotNull EntityDataPayload<EntryRecord> data, long operationID) {
        if (data.assignsField(Entry.ENTRY.OUTLET) && data.getValue(Entry.ENTRY.OUTLET) != null)
            throw new InvalidValue("Cannot update an outlet entry through normal outlet. Use updateOutlet() instead.");
        super.beforeUpdate(target, data, operationID);
    }

    public @NotNull List<EntryRecord> getOfLorebook(Integer lorebookId) {
        return this.store.getOfLorebook(lorebookId);
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

    public @NotNull List<EntryRecord> getWithOutletAndKeywords(
            @NotNull EntityKey<LorebooksRecord> lorebookKey,
            int outletID,
            IntSet keywordIDs
    ) {
        return store.getEntriesWith(lorebookKey, outletID, keywordIDs);
    }
}
