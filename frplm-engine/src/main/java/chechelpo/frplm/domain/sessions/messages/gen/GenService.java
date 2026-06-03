package chechelpo.frplm.domain.sessions.messages.gen;

import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.events.crud.CRUDCommittedEvent;
import chechelpo.frplm.exceptions.Severity;
import chechelpo.frplm.exceptions.runtime.EntityNotFound;
import chechelpo.frplm.frameworks.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.frameworks.entities.pseudo_services.EntityKey;
import chechelpo.frplm.frameworks.entities.pseudo_services.EntityService;
import chechelpo.frplm.jooq.generated.tables.records.LlmGenRecord;
import chechelpo.frplm.jooq.generated.tables.records.MessagesRecord;
import chechelpo.frplm.jooq.generated.tables.records.ResponsesRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;

import static chechelpo.frplm.jooq.generated.Tables.*;

@Service
public class GenService extends EntityService<LlmGenRecord, GenStore> {
    private final ResponseService responseService;
    GenService(GenStore store, ResponseService responseService, EventBus eventBus) {
        super(store, eventBus);
        this.responseService = responseService;
    }

    public EntityKey<LlmGenRecord> from(@NotNull EntityKey<MessagesRecord> key) {
        EntityKey<LlmGenRecord> result = EntityKey.<LlmGenRecord>builder()
                .set(LLM_GEN.SESSION_ID, key.requireValue(MESSAGES.SESSION_ID))
                .set(LLM_GEN.TICK_NUM, key.requireValue(MESSAGES.TICK_NUM))
                .build();
        throwIfInvalidKey(result, true);
        return result;
    }

    @Transactional ( readOnly = true )
    public Optional<ResponsesRecord> getActiveResponseOf(EntityKey<MessagesRecord> key) {
        return this.find(this.from(key))
                .map(responseService::getLastResponse);
    }

    public void registerNewResponse(EntityKey<LlmGenRecord> ofKey,  String content){
        if (!exists(ofKey)) {
            log.error("No such entity with key {}", ofKey);
            return;
        }
        short newResponseNum = this.incrementAndGet(LLM_GEN.RESPONSE_NUM, ofKey)
                .orElseThrow(() -> {
                    log.error("No such entity with key {}", ofKey);
                    return new EntityNotFound("Not found", Severity.SYSTEM);
                });

        responseService.createAndGet(EntityDataPayload.<ResponsesRecord>builder()
                .set(RESPONSES.SESSION_ID, ofKey.requireValue(LLM_GEN.SESSION_ID))
                .set(RESPONSES.TICK_NUM, ofKey.requireValue(LLM_GEN.TICK_NUM))
                .set(RESPONSES.RESPONSE_NUM, newResponseNum)
                .set(RESPONSES.ADVANCES_TIME_BY, 0)
                .set(RESPONSES.CONTENT, content)
                .build()
        );

        this.update(ofKey, EntityDataPayload.of(LLM_GEN.ACTIVE_RESPONSE, newResponseNum));
    }


    @TransactionalEventListener
    void beforeReturningRecords(CRUDCommittedEvent.@NotNull RetrievedEntities<?> rawEvent){
        if (rawEvent.type() != EntityTypes.Types.MESSAGES) return;

        CRUDCommittedEvent.RetrievedEntities<MessagesRecord> event =
                (CRUDCommittedEvent.RetrievedEntities<MessagesRecord>) rawEvent;

        assert event.recordsToReturn().size() == event.targets().size();


    }
}
