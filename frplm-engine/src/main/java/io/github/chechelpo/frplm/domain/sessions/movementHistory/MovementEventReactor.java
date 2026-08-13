package io.github.chechelpo.frplm.domain.sessions.movementHistory;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.domain.sessions.core.SessionService;
import io.github.chechelpo.frplm.domain.sessions.messages.MessageService;
import io.github.chechelpo.frplm.events.crud.CRUDCommittedEvent;
import io.github.chechelpo.frplm.events.crud.CRUDDraftEvent;
import io.github.chechelpo.frplm.jooq.generated.tables.records.*;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import static io.github.chechelpo.frplm.jooq.generated.Tables.*;

@Component
class MovementEventReactor {

    private final MovementsService movementsService;
    private final SessionService sessionService;
    private final MessageService messageService;
    private final ResponseMovementService responseMovementService;

    MovementEventReactor(MovementsService movementsService, SessionService sessionService, MessageService messageService, ResponseMovementService responseMovementService) {
        this.movementsService = movementsService;
        this.sessionService = sessionService;
        this.messageService = messageService;
        this.responseMovementService = responseMovementService;
    }


    @EventListener
    void onMoveUpdateHistory(CRUDCommittedEvent.UpdatedEntity<?> rawEvent){
        if (rawEvent.isNotEventOf(SESSION_CHARACTERS)) return;

        // noinspection unchecked
        CRUDCommittedEvent.UpdatedEntity<SessionCharactersRecord> event =
                (CRUDCommittedEvent.UpdatedEntity<SessionCharactersRecord>) rawEvent;
        if (!event.updatedData().assigns(SESSION_CHARACTERS.CURRENT_LOCATION_ID)) return;

        int characterId = event.target().requireNonNull(SESSION_CHARACTERS.ID);
        int sessionId = event.target().requireNonNull(SESSION_CHARACTERS.SESSION_ID);
        int nextLocationId = event.updatedData().requireNonNull(SESSION_CHARACTERS.CURRENT_LOCATION_ID);

        SessionsRecord session = sessionService.find(EntityKey.of(SESSIONS.ID, sessionId)).orElseThrow();
        MessagesRecord currentMessage = messageService.getLastMessageOf(sessionId);
        ResponsesRecord activeResponse = messageService.getActiveResponseOf(currentMessage);

        EntityKey<MovementsRecord> key = EntityKey.<MovementsRecord>builder()
                .set(MOVEMENTS.SESSION_ID, sessionId)
                .set(MOVEMENTS.AT_TICK, currentMessage.getTickNum())
                .set(MOVEMENTS.SES_CHARACTER_ID, characterId)
                .build();

        movementsService.find(key)
                .ifNotFound(ignored ->
                            movementsService.createAndGet(
                                    EntityDataPayload.<MovementsRecord>builder()
                                            .set(MOVEMENTS.SESSION_ID, sessionId)
                                            .set(MOVEMENTS.AT_TICK, currentMessage.getTickNum())
                                            .set(MOVEMENTS.SES_CHARACTER_ID, characterId)

                                            .set(MOVEMENTS.WORLD_ID, session.getWorldId())
                                            .set(MOVEMENTS.PREVIOUS_LOCATION_ID, event.previousData().getCurrentLocationId())

                                            .build()
                            )
                        );

        EntityKey<ResponseLocationChangesRecord> responseKey = EntityKey.<ResponseLocationChangesRecord>builder()
                .set(RESPONSE_LOCATION_CHANGES.SESSION_ID, sessionId)
                .set(RESPONSE_LOCATION_CHANGES.TICK_NUM, currentMessage.getTickNum())
                .set(RESPONSE_LOCATION_CHANGES.RESPONSE_NUM, activeResponse.getResponseNum())
                .set(RESPONSE_LOCATION_CHANGES.SESSION_CHARACTER_ID, characterId)
                .build();

        responseMovementService.find(responseKey)
                .ifNotFound(ignored ->
                        responseMovementService.createAndGet(
                                EntityDataPayload.<ResponseLocationChangesRecord>builder()
                                        .copyAll(responseKey)
                                        .set(RESPONSE_LOCATION_CHANGES.LOCATION_ID, nextLocationId)
                                        .build()
                        )
                )
                .ifFound(
                        ignored -> responseMovementService.update(
                                responseKey,
                                EntityDataPayload.of(RESPONSE_LOCATION_CHANGES.LOCATION_ID, nextLocationId)
                        )
                );
    }

    /**
     * Based on the invariant that only the last message can ever be deleted, this function rollbacks the location changes
     * done from this deletion.
     */
    @EventListener
    void onMessageDeletedRewindLocations(CRUDDraftEvent.DeleteEntityDraft<?> rawEvent){
        if (rawEvent.isNotEventOf(MESSAGES)) return;
        // noinspection unchecked
        CRUDDraftEvent.DeleteEntityDraft<MessagesRecord> event =
                (CRUDDraftEvent.DeleteEntityDraft<MessagesRecord>) rawEvent;

        movementsService.rollbackMovementsFrom(
                event.key().requireNonNull(MESSAGES.SESSION_ID),
                event.key().requireNonNull(MESSAGES.TICK_NUM)
        );
    }

    /**
     * Applies response location changes.
     * <pre>
     *     1. Rollbacks to the state prior to the message
     *     2. Applies the response location changes
     * </pre>
     * This function also works as a rollback when a new response is registered, as there are no response location changes
     * to apply anyways.
     */
    @EventListener
    void onResponseChangeApplyLocationChanges(CRUDCommittedEvent.UpdatedEntity<?> rawEvent){
        if (rawEvent.isNotEventOf(MESSAGES)) return;

        // noinspection unchecked
        CRUDCommittedEvent.UpdatedEntity<MessagesRecord> event =
                (CRUDCommittedEvent.UpdatedEntity<MessagesRecord>) rawEvent;
        if (!event.updatedData().assigns(MESSAGES.ACTIVE_RESPONSE)) return;

        movementsService.rollbackMovementsFrom(
                event.target().requireNonNull(MESSAGES.SESSION_ID),
                event.target().requireNonNull(MESSAGES.TICK_NUM)
        );

        int sessionId = event.target().requireNonNull(MESSAGES.SESSION_ID);
        MessagesRecord lastMessage = messageService.getLastMessageOf(sessionId);

        responseMovementService.applyMovementsOfResponse(
                messageService.getActiveResponseOf(lastMessage)
        );
    }
}
