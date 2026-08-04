package io.github.chechelpo.frplm.utils.stable_records;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityService;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TableRecord;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 *
 */
@Component
final class StableRecordCreatorImpl implements SmartInitializingSingleton, StableRecordCreator {
    private final List<StableRecord<?>> stableRecords;
    private final List<EntityService<?, ?>> services;
    private final DSLContext dsl;

    StableRecordCreatorImpl(
            List<StableRecord<?>> directRecords,
            List<StableRecordProvider> providers,
            List<EntityService<?, ?>> services,
            DSLContext dsl
    ) {
        List<StableRecord<?>> records = new ArrayList<>(directRecords);

        providers.stream()
                .map(StableRecordProvider::stableRecords)
                .forEach(records::addAll);

        this.stableRecords = List.copyOf(records);
        this.services = services;
        this.dsl = dsl;
    }

    @Override
    public void afterSingletonsInstantiated() {
        for (StableRecord<?> stableRecord : stableRecords) {
            handle(stableRecord);
            stableRecord.runCustomConfig(dsl);
        }
    }

    @SuppressWarnings("unchecked")
    private <R extends TableRecord<R>> void handle(StableRecord<R> stableRecord) {
        EntityService<R, ?> service = getService(stableRecord.getTable()).orElseThrow(
                () -> new IllegalStateException(
                        "No EntityService registered for table " + stableRecord.getTable()
                )
        );
        stableRecord.toKey().ifPresent(
                key -> service.find(key)
                        .ifNotFound(ignored ->
                                service.createAndGet(stableRecord.toPayload().orElseThrow())
                        )
        );
    }

    private <R extends TableRecord<R>> Optional<? extends EntityService<R, ?>> getService(Table<R> table){
        //noinspection unchecked
        return services.stream()
                .filter(service -> service.getTable().equals(table))
                .map(service -> (EntityService<R, ?>) service)
                .findFirst();
    }

    @Override
    public void run(){
        afterSingletonsInstantiated();
    }
}
