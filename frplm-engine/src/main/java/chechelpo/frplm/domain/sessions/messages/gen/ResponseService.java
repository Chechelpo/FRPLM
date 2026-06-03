package chechelpo.frplm.domain.sessions.messages.gen;

import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.exceptions.Severity;
import chechelpo.frplm.exceptions.runtime.EntityNotFound;
import chechelpo.frplm.exceptions.runtime.UnexpectedException;
import chechelpo.frplm.frameworks.entities.pseudo_services.EntityKey;
import chechelpo.frplm.frameworks.entities.pseudo_services.EntityService;
import chechelpo.frplm.jooq.generated.tables.records.LlmGenRecord;
import chechelpo.frplm.jooq.generated.tables.records.ResponsesRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static chechelpo.frplm.jooq.generated.Tables.RESPONSES;

@Service
class ResponseService extends EntityService<ResponsesRecord, ResponseStore> {
    ResponseService(ResponseStore store, EventBus eventBus) {
        super(store, eventBus);
    }

    @Transactional(readOnly = true)
    public @NotNull ResponsesRecord getLastResponse(@NotNull LlmGenRecord record) throws EntityNotFound {
        return this.find(
                EntityKey.<ResponsesRecord>builder()
                        .set(RESPONSES.SESSION_ID, record.getSessionId())
                        .set(RESPONSES.TICK_NUM, record.getTickNum())
                        .set(RESPONSES.RESPONSE_NUM, record.getResponseNum())
                        .build()
        ).orElseThrow(() -> {
            log.error("Generated {} message exists but has NO response", record);
            return new UnexpectedException("Generated message has no response yet exists", Severity.SYSTEM);
        });
    }
}
