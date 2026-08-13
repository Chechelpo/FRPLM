package io.github.chechelpo.frplm.domain.lorebook.core;

import io.github.chechelpo.frplm.core.entities.fields.FieldValidator;
import io.github.chechelpo.frplm.events.EventBus;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
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
    LorebookService(LorebookStore store, FieldValidator<LorebooksRecord> validator, EventBus eventBus) {
        super(store, validator, eventBus);
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
}
