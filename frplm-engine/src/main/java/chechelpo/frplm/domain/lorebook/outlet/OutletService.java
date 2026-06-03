package chechelpo.frplm.domain.lorebook.outlet;

import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.frameworks.entities.pseudo_services.EntityService;
import chechelpo.frplm.frameworks.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.frameworks.entities.pseudo_services.EntityKey;
import chechelpo.frplm.jooq.generated.tables.Outlet;
import chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import chechelpo.frplm.jooq.generated.tables.records.OutletRecord;
import chechelpo.frplm.utils.collections.IntSetFactory;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.IntStream;

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


    @CheckReturnValue
    public IntObjectPair<String>[] getOutlets(LorebooksRecord[] records) {
        return getOutlets(IntSetFactory.ofValues(
                Arrays.stream(records)
                        .flatMapToInt(record -> IntStream.of(record.getId()))
                        .toArray())
        );
    }
    public IntObjectPair<String>[] getOutlets(IntSet lorebookIDs) {
        return store.getOutletsOfLorebooks(lorebookIDs);
    }

    @Transactional
    public int getOrCreateOutlet(@NotNull String name) {
        if (store.existsName(name))
            //noinspection DataFlowIssue
            return store.getOfName(name);

        return this.createAndGet(
                EntityDataPayload.of(Outlet.OUTLET.OUTLET_, name),
                Outlet.OUTLET.ID
        );
    }
}
