package chechelpo.frplm.domain.lorebook.core;

import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.events.crud.CRUDCommittedEvent;
import chechelpo.frplm.exceptions.runtime.EntityNotFound;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.core.entities.pseudo_services.EntityService;
import chechelpo.frplm.jooq.generated.tables.records.*;
import chechelpo.frplm.utils.collections.IntSetFactory;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.stream.IntStream;

import static chechelpo.frplm.jooq.generated.Tables.LOREBOOKS;

@Component
public class LorebookService extends EntityService<LorebooksRecord, LorebookStore> {
    LorebookService(LorebookStore store, EventBus eventBus) {
        super(store, eventBus);
    }

    /**
     * @return list of global records
     */
    @Override
    public @NotNull List<LorebooksRecord> getAll() {
        return this.store.getGlobalLorebooks();
    }

    @Transactional(readOnly = true)
    @CheckReturnValue
    public @NotNull LorebooksRecord getLorebookOf(@NotNull CharactersRecord record) throws EntityNotFound {
        return this.find(EntityKey.of(LOREBOOKS.ID, record.getLorebookId()))
                .orElseThrow(() -> new IllegalStateException("Character " + record.getName() + " without a lorebook"));
    }

    @Transactional(readOnly = true)
    @CheckReturnValue
    public @NotNull List<LorebooksRecord> getLorebookOf(@NotNull List<CharactersRecord> records){
        return store.getLorebooks(
                IntSetFactory.ofValues(
                        records.stream()
                                .flatMapToInt(i -> IntStream.of(i.getLorebookId()))
                                .toArray()
                )
        );
    }

    @Transactional(readOnly = true)
    @CheckReturnValue
    public @NotNull LorebooksRecord getLorebookOf(@NotNull WorldsRecord record) throws EntityNotFound {
        return this.find(EntityKey.of(LOREBOOKS.ID, record.getLorebookId()))
                .orElseThrow(() -> new IllegalStateException("World " + record.getLorebookId() + " without a lorebook"));
    }

    @Transactional(readOnly = true)
    @CheckReturnValue
    public @NotNull LorebooksRecord getLorebookOf(@NotNull LocationsRecord record) throws EntityNotFound {
        return this.find(EntityKey.of(LOREBOOKS.ID, record.getLorebookId()))
                .orElseThrow(() -> new IllegalStateException("Location " + record.getLorebookId() + " without a lorebook"));
    }

    //
    public boolean importFromJSON(JsonNode file){
        return true;
    }
}
