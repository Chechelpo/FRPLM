package io.github.chechelpo.frplm.domain.sessions.messages;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityService;
import io.github.chechelpo.frplm.events.EventBus;
import io.github.chechelpo.frplm.jooq.generated.tables.records.ResponsesRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
class ResponseService extends EntityService<ResponsesRecord, ResponseStore> {
    ResponseService(@NotNull ResponseStore store, @NotNull EventBus eventBus) {
        super(store, eventBus);
    }

    @Override
    protected void beforeDelete(EntityKey<ResponsesRecord> id, long operationID) {
        throw new UnsupportedOperationException("Cannot delete responses");
    }
}
