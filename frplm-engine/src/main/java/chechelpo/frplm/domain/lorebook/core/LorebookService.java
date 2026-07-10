package chechelpo.frplm.domain.lorebook.core;

import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.exceptions.Severity;
import chechelpo.frplm.exceptions.runtime.EntityNotFound;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.core.entities.pseudo_services.EntityService;
import chechelpo.frplm.exceptions.runtime.UnexpectedException;
import chechelpo.frplm.jooq.generated.tables.records.*;
import chechelpo.frplm.utils.collections.IntSetFactory;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;
import org.jooq.tools.json.JSONArray;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import static chechelpo.frplm.jooq.generated.Tables.LOREBOOKS;

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

    @Transactional(readOnly = true)
    @CheckReturnValue
    public @NotNull LorebooksRecord getLorebookOf(@NotNull CharactersRecord record) throws EntityNotFound {
        Objects.requireNonNull(record);
        return this.find(EntityKey.of(LOREBOOKS.ID, record.getLorebookId()))
                .orElseThrow(() -> new UnexpectedException("Character " + record.getName() + " without a lorebook", Severity.SYSTEM));
    }
    @Transactional(readOnly = true)
    @CheckReturnValue
    public @NotNull LorebooksRecord getLorebookOf(@NotNull RegionRecord record) throws EntityNotFound {
        Objects.requireNonNull(record);
        return this.find(EntityKey.of(LOREBOOKS.ID, record.getLorebookId()))
                .orElseThrow(() -> new UnexpectedException("Character " + record.getName() + " without a lorebook", Severity.SYSTEM));
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
    @Transactional(readOnly = true)
    @CheckReturnValue
    public @NotNull LorebooksRecord getLorebookOf(@NotNull WorldsRecord record) throws EntityNotFound {
        Objects.requireNonNull(record);
        return this.find(EntityKey.of(LOREBOOKS.ID, record.getLorebookId()))
                .orElseThrow(() -> new UnexpectedException("World " + record.getName() + " without a lorebook", Severity.SYSTEM));
    }
    @Transactional(readOnly = true)
    @CheckReturnValue
    public @NotNull LorebooksRecord getLorebookOf(@NotNull LocationsRecord record) throws EntityNotFound {
        Objects.requireNonNull(record);
        return this.find(EntityKey.of(LOREBOOKS.ID, record.getLorebookId()))
                .orElseThrow(() -> new UnexpectedException("Location " + record.getName() + " without a lorebook", Severity.SYSTEM));
    }
}
