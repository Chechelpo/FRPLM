package io.github.chechelpo.frplm.domain.sessions.movementHistory;

import io.github.chechelpo.frplm.core.entities.fields.FieldValidator;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityService;
import io.github.chechelpo.frplm.events.EventBus;
import io.github.chechelpo.frplm.jooq.generated.tables.records.ResponseLocationChangesRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.ResponsesRecord;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

@Service
class ResponseMovementService extends EntityService<ResponseLocationChangesRecord, ResponseMovementStore> {
    public ResponseMovementService(@NonNull ResponseMovementStore store, FieldValidator<ResponseLocationChangesRecord> validator, @NotNull EventBus eventBus) {
        super(store, validator, eventBus);
    }

    void applyMovementsOfResponse(ResponsesRecord record) {
        store.applyMovementsOfResponse(record.getSessionId(), record.getTickNum(), record.getResponseNum());
    }
}
