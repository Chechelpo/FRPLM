package chechelpo.frplm.domain.lorebook.outlet;

import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.frameworks.entities.microservices.EntityService;
import chechelpo.frplm.frameworks.entities.microservices.EntityDataPayload;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.jooq.generated.tables.Outlet;
import chechelpo.frplm.jooq.generated.tables.records.OutletRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.OptionalInt;

@Service
public class OutletService extends EntityService<OutletRecord, OutletStore> {
    OutletService(OutletStore store, EventBus eventBus) {
        super(store, eventBus);
    }

    @Override
    public void beforeUpdate(EntityKey<OutletRecord> id, EntityDataPayload<OutletRecord> update, long operationID) {
        throw new UnsupportedOperationException("Outlets can't be updated");
    }

    public Optional<Integer> getOutletID(String name){
        return Optional.ofNullable(store.getOfName(name));
    }

    @Transactional
    public int getOrCreateOutlet(@NotNull String name){
        if (store.existsName(name))
            //noinspection DataFlowIssue
            return store.getOfName(name);

        return this.createAndGet(
                EntityDataPayload.of(Outlet.OUTLET.OUTLET_, name),
                Outlet.OUTLET.ID
        );
    }
}
