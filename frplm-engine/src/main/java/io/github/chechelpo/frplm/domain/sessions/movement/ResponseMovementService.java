package io.github.chechelpo.frplm.domain.sessions.movement;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityService;
import io.github.chechelpo.frplm.domain.sessions.messages.MessageService;
import io.github.chechelpo.frplm.events.EventBus;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.UnexpectedException;
import chechelpo.frplm.jooq.generated.tables.records.MessagesRecord;
import chechelpo.frplm.jooq.generated.tables.records.ResponseLocationChangesRecord;
import chechelpo.frplm.jooq.generated.tables.records.ResponsesRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

import static chechelpo.frplm.jooq.generated.Tables.RESPONSE_LOCATION_CHANGES;

@Component
public class ResponseMovementService extends EntityService<ResponseLocationChangesRecord, ResponseMovementStore> {

    private final MessageService messageService;

    ResponseMovementService(
            @NotNull ResponseMovementStore store,
            @NotNull EventBus eventBus,
            MessageService messageService
    ) {
        super(store, eventBus);
        this.messageService = messageService;
    }

    /**
     * <h3> Algorithm </h3>
     * <pre>
     *     1. Search for the latest tick of the session.
     *     2. Gets the latest response num of that message.
     *     3. Creates a new record signaling the movement of this character to that location
     * </pre>
     * @param characterId to move
     * @param toLocationId location its moving to
     * @return whether it was successful
     */
    @Transactional
    public ResponseLocationChangesRecord moveInCurrentResponse(int sessionId, int characterId, int toLocationId){
        MessagesRecord messagesRecord = messageService.getLastOf(sessionId);
        ResponsesRecord response = messageService.getActiveResponseOf(messagesRecord);
        assert Objects.equals(messagesRecord.getActiveResponse(), response.getResponseNum());

        EntityKey<ResponseLocationChangesRecord> key = EntityKey.<ResponseLocationChangesRecord>builder()
                .set(RESPONSE_LOCATION_CHANGES.SESSION_ID, sessionId)
                .set(RESPONSE_LOCATION_CHANGES.TICK_NUM, messagesRecord.getTickNum())
                .set(RESPONSE_LOCATION_CHANGES.RESPONSE_NUM, response.getResponseNum())
                .set(RESPONSE_LOCATION_CHANGES.CHARACTER_ID, characterId)
                .build();

        if (this.exists(key)) {
            this.update(key, EntityDataPayload.of(RESPONSE_LOCATION_CHANGES.LOCATION_ID, toLocationId));
            return this.find(key).orElseThrow(() -> new UnexpectedException("Couldn't find after update", Severity.SYSTEM));
        } else return this.createAndGet(
                EntityDataPayload.<ResponseLocationChangesRecord>builder()
                        .set(RESPONSE_LOCATION_CHANGES.SESSION_ID, sessionId)
                        .set(RESPONSE_LOCATION_CHANGES.TICK_NUM, messagesRecord.getTickNum())
                        .set(RESPONSE_LOCATION_CHANGES.RESPONSE_NUM, response.getResponseNum())

                        .set(RESPONSE_LOCATION_CHANGES.CHARACTER_ID, characterId)

                        .set(RESPONSE_LOCATION_CHANGES.WORLD_ID, messagesRecord.getWorldId())
                        .set(RESPONSE_LOCATION_CHANGES.LOCATION_ID, toLocationId)
                        .build()
        );


    }

    public List<ResponseLocationChangesRecord> getResponseMovements(int sessionId, int tick_num, short response_num){
        return store.getResponseMovements(sessionId, tick_num, response_num);
    }
}
