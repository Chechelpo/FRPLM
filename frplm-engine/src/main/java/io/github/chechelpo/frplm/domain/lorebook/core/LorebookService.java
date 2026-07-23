package io.github.chechelpo.frplm.domain.lorebook.core;

import io.github.chechelpo.frplm.events.EventBus;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityService;
import io.github.chechelpo.frplm.exceptions.runtime.UnexpectedException;
import io.github.chechelpo.frplm.jooq.generated.tables.records.*;
import io.github.chechelpo.frplm.utils.collections.IntSetFactory;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;

import static io.github.chechelpo.frplm.jooq.generated.Tables.LOREBOOKS;

@Component
public class LorebookService extends EntityService<LorebooksRecord, LorebookStore> {
    LorebookService(LorebookStore store, EventBus eventBus) {
        super(store, eventBus);
    }

    /**
     * @return list of global (not related to characters/worlds/locations) lorebook records
     */
    public @NotNull List<LorebooksRecord> getIndependent(){
        return this.store.getGlobalLorebooks();
    }

    public EntityKey<LorebooksRecord> keyOf(int lorebookId){
        return EntityKey.of(LOREBOOKS.ID, lorebookId);
    }

    @Transactional(readOnly = true)
    @CheckReturnValue
    public @NotNull LorebooksRecord getLorebookOf(@NotNull CharactersRecord record) throws EntityNotFound {
        Objects.requireNonNull(record);
        return this.find(EntityKey.of(LOREBOOKS.ID, record.getLorebookId()))
                .orElseThrow(notFound -> new UnexpectedException("Character " + record.getName() + " without a lorebook " + notFound.toDebugString(), Severity.SYSTEM));
    }
    @Transactional(readOnly = true)
    @CheckReturnValue
    public @NotNull LorebooksRecord getLorebookOf(@NotNull RegionRecord record) throws EntityNotFound {
        Objects.requireNonNull(record);
        return this.find(EntityKey.of(LOREBOOKS.ID, record.getLorebookId()))
                .orElseThrow(notFound -> new UnexpectedException("Region " + record.getName() + " without a lorebook", Severity.SYSTEM));
    }
    @Transactional(readOnly = true)
    @CheckReturnValue
    public @NotNull LorebooksRecord getLorebookOf(@NotNull WorldsRecord record) throws EntityNotFound {
        Objects.requireNonNull(record);
        return this.find(EntityKey.of(LOREBOOKS.ID, record.getLorebookId()))
                .orElseThrow(notFound -> new UnexpectedException("World " + record.getName() + " without a lorebook", Severity.SYSTEM));
    }
    @Transactional(readOnly = true)
    @CheckReturnValue
    public @NotNull LorebooksRecord getLorebookOf(@NotNull LocationsRecord record) throws EntityNotFound {
        Objects.requireNonNull(record);
        return this.find(EntityKey.of(LOREBOOKS.ID, record.getLorebookId()))
                .orElseThrow(notFound -> new UnexpectedException("Location " + record.getName() + " without a lorebook", Severity.SYSTEM));
    }

    @Transactional(readOnly = true)
    @CheckReturnValue
    public @NotNull List<LorebooksRecord> getLorebookOf(@NotNull Set<CharactersRecord> records){
        Objects.requireNonNull(records);
        List<LorebooksRecord> results = store.getLorebooks(
                IntSetFactory.ofValues(
                        records.stream()
                                .flatMapToInt(i -> IntStream.of(i.getLorebookId()))
                                .toArray()
                )
        );

        if (results.size() != records.size()) {
            log.error("Found less lorebooks than characters");
            throw new UnexpectedException("Found less lorebooks than characters", Severity.SYSTEM);
        }

        return results;
    }

}
