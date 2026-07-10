package io.github.chechelpo.frplm.domain.lorebook.outlet;

import io.github.chechelpo.frplm.events.EventBus;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityService;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.jooq.generated.tables.Outlet;
import io.github.chechelpo.frplm.jooq.generated.tables.records.OutletRecord;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.NotNull;
import org.jooq.Record2;
import org.jooq.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Service
public class OutletService extends EntityService<OutletRecord, OutletStore> {
    OutletService(OutletStore store, EventBus eventBus) {
        super(store, eventBus);
    }

    @Override
    public void beforeUpdate(EntityKey<OutletRecord> id, EntityDataPayload<OutletRecord> update, long operationID) {
        throw new UnsupportedOperationException("Outlets can't be updated");
    }

    public Optional<Integer> getOutletID(String name) {
        return Optional.ofNullable(store.getOfName(name));
    }
    public Optional<String> getOutletName(int id) {
        return Optional.ofNullable(store.getName(id));
    }

    /** @return outletId, value of all entries with these lorebook ids */
    public Result<Record2<Integer, String>> getOutletsFromLorebook(IntSet lorebookIDs) {
        Objects.requireNonNull(lorebookIDs, "lorebookIDs must not be null");
        return store.getOutletsOfLorebooks(lorebookIDs);
    }

    public Result<Record2<Integer, String>> getOutletsFromIds(IntSet outletIds){
        Objects.requireNonNull(outletIds, "Outlet ids are null");
        return store.getWithIds(outletIds);
    }

    @Transactional
    public int getOrCreateOutlet(@NotNull String name) {
        Objects.requireNonNull(name, "Outlet name must not be null");
        if (store.existsName(name))
            //noinspection DataFlowIssue
            return store.getOfName(name);

        return this.createAndGet(
                EntityDataPayload.of(Outlet.OUTLET.OUTLET_, name),
                Outlet.OUTLET.ID
        );
    }
}
