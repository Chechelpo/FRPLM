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
non-sealed class OutletServiceImpl extends EntityService<OutletRecord, OutletStore> implements OutletService {
    OutletServiceImpl(OutletStore store, EventBus eventBus) {
        super(store, eventBus);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Integer> getOutletID(String name) {
        return Optional.ofNullable(store.getOfName(name));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> getOutletName(int id) {
        return Optional.ofNullable(store.getName(id));
    }
    @Override
    @Transactional(readOnly = true)
    public Result<Record2<Integer, String>> getOutletsFromIds(IntSet outletIds){
        Objects.requireNonNull(outletIds, "Outlet ids are null");
        return store.getWithIds(outletIds);
    }

    @Override
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
