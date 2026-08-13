package io.github.chechelpo.frplm.domain.sessions.movementHistory;

import io.github.chechelpo.frplm.core.entities.fields.FieldValidator;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityService;
import io.github.chechelpo.frplm.events.EventBus;
import io.github.chechelpo.frplm.jooq.generated.tables.records.MovementsRecord;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

@Service
class MovementsService extends EntityService<MovementsRecord, MovementStore> {
    MovementsService(
            @NonNull MovementStore store,
            FieldValidator<MovementsRecord> validator,
            @NotNull EventBus eventBus
    ) {
        super(store, validator, eventBus);
    }

    public void rollbackMovementsFrom(int sessionId, int tickNum){
        store.rollbackLatestMovementsAt(sessionId, tickNum);
    }
}
