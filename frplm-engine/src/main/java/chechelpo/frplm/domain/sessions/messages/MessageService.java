package chechelpo.frplm.domain.sessions.messages;

import chechelpo.frplm.domain.character.core.CharacterService;
import chechelpo.frplm.domain.character.starting_locations.StartingLocationsService;
import chechelpo.frplm.domain.sessions.core.SessionService;
import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.exceptions.Severity;
import chechelpo.frplm.exceptions.runtime.EntityNotFound;
import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.core.entities.pseudo_services.EntityService;
import chechelpo.frplm.exceptions.runtime.InvalidValue;
import chechelpo.frplm.exceptions.runtime.UnexpectedException;
import chechelpo.frplm.jooq.generated.tables.records.*;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionRole;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static chechelpo.frplm.jooq.generated.Tables.*;

@Service
public class MessageService extends EntityService<MessagesRecord, MessageStore> {
    public static final int FIRST_MESSAGE_TICK_NUM = 1;
    private final CharacterService characters;
    private final StartingLocationsService startingLocations;
    private final SessionService sessionService;
    private final ResponseService responseService;

    MessageService(
            CharacterService characters,
            StartingLocationsService startingLocations,
            MessageStore store,
            EventBus eventBus,
            SessionService sessionService,
            ResponseService responseService
    ) {
        super(store, eventBus);
        this.characters = characters;
        this.startingLocations = startingLocations;
        this.sessionService = sessionService;
        this.responseService = responseService;
    }

    public MessagesRecord getLastOf(@NotNull SessionsRecord record) {
        return this.getLastOf(record.getId());
    }

    public MessagesRecord getLastOf(int sessionID) {
        return store.getLastMessage(sessionID);
    }
    public List<MessagesRecord> getLastOf(int sessionId, int number){
        return store.getLast(sessionId, number);
    }

    public List<MessagesRecord> getRange(int sessionId, int from, int to){
        return store.getLast(sessionId, from, to);
    }

    @Override
    @SuppressWarnings("SpringTransactionalMethodCallsInspection")
    protected void beforeUpdate(@NotNull EntityKey<MessagesRecord> target, EntityDataPayload<MessagesRecord> data, long operationID) {
        if (data.assignsField(MESSAGES.ACTIVE_RESPONSE)) {
            validateActiveResponse(target, data.requireValue(MESSAGES.ACTIVE_RESPONSE));
            applyActiveResponseValues(target, data);
        }

        super.beforeUpdate(target, data, operationID);
    }

    /**
     * Ignored if it's a user message. Ignored if the active response is already this response.
     */
    void validateActiveResponse(EntityKey<MessagesRecord> target, short active_response) {
        int sessionId = target.requireValue(MESSAGES.SESSION_ID);
        int tick_num = target.requireValue(MESSAGES.TICK_NUM);
        Optional<MessagesRecord> record = this.find(target);
        if (record.isEmpty()) {
            log.error("No message found for session id {} tick num {}", sessionId, tick_num);
            throw new EntityNotFound("Message not found when setting active response", Severity.EXPECTED);
        }
        if (record.get().getRole().equals(ChatCompletionRole.USER.wireValue())) {
            log.debug("Attempted changing the response number of a user message");
            return;
        }
        if (record.get().getActiveResponse() == active_response) {
            log.debug("Response {} of message (session: {} , tick: {}) is already active", active_response, sessionId, tick_num);
            return;
        }

        int maxResponseNum = record.get().getResponseNum();
        if (maxResponseNum < active_response) {
            log.error("Max number response is {} yet attempted to change to {}", maxResponseNum, active_response);
            throw new InvalidValue("There's no response with number " + active_response);
        }
    }

    @Contract(mutates = "param2")
    private void applyActiveResponseValues(@NotNull EntityKey<MessagesRecord> target, EntityDataPayload<MessagesRecord> data) {
        ResponsesRecord newActiveResponse = responseService.find(EntityKey.<ResponsesRecord>builder()
                .set(RESPONSES.SESSION_ID, target.requireValue(MESSAGES.SESSION_ID))
                .set(RESPONSES.TICK_NUM, target.requireValue(MESSAGES.TICK_NUM))
                .set(RESPONSES.RESPONSE_NUM, data.requireValue(MESSAGES.ACTIVE_RESPONSE))
                .build()
        ).orElseThrow(() ->
                new EntityNotFound("Response with number " + data.requireValue(MESSAGES.ACTIVE_RESPONSE) + " not found."
                        , Severity.SYSTEM)
        );
        data.set(MESSAGES.CONTENT, newActiveResponse.getContent());
        data.set(MESSAGES.ACTIVE_RESPONSE, newActiveResponse.getResponseNum());
        data.set(MESSAGES.LOCATION_ID, newActiveResponse.getLocationId());
        data.set(MESSAGES.TIME, newActiveResponse.getAdvancesTimeBy());
    }

    @Override
    protected void afterSuccessfulUpdate(EntityKey<MessagesRecord> key, EntityDataPayload<MessagesRecord> updated, long operationID) {
        if (
                !updated.assignsField(MESSAGES.ACTIVE_RESPONSE) &&
                updated.assignsField(MESSAGES.LOCATION_ID) || updated.assignsField(MESSAGES.CONTENT) || updated.assignsField(MESSAGES.TIME)
        )
            updateActiveResponse(key, updated);

        super.afterSuccessfulUpdate(key, updated, operationID);
    }

    @SuppressWarnings("SpringTransactionalMethodCallsInspection")
    private void updateActiveResponse(EntityKey<MessagesRecord> target, EntityDataPayload<MessagesRecord> data) {
        ResponsesRecord currentActiveResponse = this.getActiveResponseOf(target);
        EntityDataPayload<ResponsesRecord> changed = EntityDataPayload.<ResponsesRecord>builder().build();

        if (data.assignsField(MESSAGES.LOCATION_ID)) changed.set(RESPONSES.LOCATION_ID, data.requireValue(MESSAGES.LOCATION_ID));
        if (data.assignsField(MESSAGES.CONTENT)) changed.set(RESPONSES.CONTENT, data.requireValue(MESSAGES.CONTENT));
        if (data.assignsField(MESSAGES.TIME)) changed.set(RESPONSES.ADVANCES_TIME_BY, data.requireValue(MESSAGES.TIME));

        responseService.update(responseService.keyOf(currentActiveResponse), changed);
    }



    @Override
    protected void beforeCreate(@NotNull EntityDataPayload<MessagesRecord> data, long operationID) {
        applyDefaultsOfLastMessage(data);
        boolean isAssistantMessage = data.requireValue(MESSAGES.ROLE).equals(ChatCompletionRole.ASSISTANT.wireValue());
        if (data.assignsField(MESSAGES.REQUEST_JSON) && !isAssistantMessage) {
            log.error("Cannot assign request JSON as user role");
            throw new InvalidValue("Cannot assign request JSON as user role");
        }

        data.set(MESSAGES.TICK_NUM,
                sessionService.incrementAndGet(
                                SESSIONS.CURRENT_TICK,
                                EntityKey.of(SESSIONS.ID, data.requireValue(MESSAGES.SESSION_ID))
                        )
                        .orElseThrow(() -> {
                            log.error("Could not fetch next message tick for new message \n {}", data.assignments());
                            return new EntityNotFound("Could not fetch tick for new message", Severity.SYSTEM);
                        })
        );

        super.beforeCreate(data, operationID);
    }

    @Override
    protected void afterSuccessfulCreate(MessagesRecord data, long operationID) {
        registerNewResponse(EntityDataPayload.<ResponsesRecord>builder()
                .set(RESPONSES.SESSION_ID, data.getSessionId())
                .set(RESPONSES.TICK_NUM, data.getTickNum())
                .set(RESPONSES.WORLD_ID, data.getWorldId())
                .set(RESPONSES.LOCATION_ID, data.getLocationId())
                .set(RESPONSES.CONTENT, data.getContent())
                .build());
        super.afterSuccessfulCreate(data, operationID);
    }

    /**
     * Rejects new responses for:
     * <pre>
     *     1. Non-existing messages.
     *     2. User messages
     * </pre>
     *
     * @apiNote to existing message
     */
    @Transactional
    public void registerNewResponse(int sessionId, int tick_num, String content) {
        EntityKey<MessagesRecord> messageKey = EntityKey.<MessagesRecord>builder()
                .set(MESSAGES.SESSION_ID, sessionId)
                .set(MESSAGES.TICK_NUM, tick_num)
                .build();

        if (!exists(messageKey))
            throw new EntityNotFound("No message with this id found when registering response", Severity.SYSTEM);
        ChatCompletionRole messageRole = ChatCompletionRole.fromWireValue(getValueOf(MESSAGES.ROLE, messageKey)
                .orElseThrow(() -> new UnexpectedException("Could find message but not its role", Severity.SYSTEM))
        );
        if (messageRole != ChatCompletionRole.ASSISTANT)
            throw new InvalidValue("Tried to register a new response of a user message");

        int world_id = sessionService.getValueOf(SESSIONS.WORLD_ID, EntityKey.of(SESSIONS.ID, sessionId))
                .orElseThrow(() -> new EntityNotFound("Could not session with id " + sessionId, Severity.SYSTEM));
        registerNewResponse(EntityDataPayload.<ResponsesRecord>builder()
                .set(RESPONSES.SESSION_ID, sessionId)
                .set(RESPONSES.TICK_NUM, tick_num)

                .set(RESPONSES.WORLD_ID, world_id)
                .set(RESPONSES.CONTENT, content)
                .build());
    }

    @SuppressWarnings("SpringTransactionalMethodCallsInspection")
    private void registerNewResponse(EntityDataPayload<ResponsesRecord> payload) {
        EntityKey<MessagesRecord> messageKey = EntityKey.<MessagesRecord>builder()
                .set(MESSAGES.SESSION_ID, payload.requireValue(RESPONSES.SESSION_ID))
                .set(MESSAGES.TICK_NUM, payload.requireValue(RESPONSES.TICK_NUM))
                .build();
        short newActiveResponseNum = this.incrementAndGet(MESSAGES.RESPONSE_NUM, messageKey)
                .orElseThrow(() -> new UnexpectedException("Couldn't increment response num", Severity.SYSTEM));

        payload.set(RESPONSES.RESPONSE_NUM, newActiveResponseNum);

        ResponsesRecord newResponse = responseService.createAndGet(payload);

        this.update(messageKey, EntityDataPayload.of(MESSAGES.ACTIVE_RESPONSE, newActiveResponseNum));
    }

    /**
     * Simply checks if this message is the last one before deleting
     */
    @Override
    protected void beforeDelete(@NotNull EntityKey<MessagesRecord> id, long operationID) {
        int sessionId = id.requireValue(MESSAGES.SESSION_ID);
        int requestedTick = id.requireValue(MESSAGES.TICK_NUM);

        MessagesRecord lastMessage = getLastOf(sessionId);
        int lastTick = lastMessage.getTickNum();

        if (requestedTick != lastTick) {
            log.error(
                    "Tried to delete message tick {} when last tick was {}",
                    requestedTick,
                    lastTick
            );
            throw new InvalidValue("Tried to delete some message that was not the last one");
        }

        /*
         * Important: this emits the delete draft event only after the deletion is proved to be of the last message.
         */
        super.beforeDelete(id, operationID);
    }

    private void applyDefaultsOfLastMessage(@NotNull EntityDataPayload<MessagesRecord> data) {
        MessagesRecord lastMessage = getLastOf(data.requireValue(MESSAGES.SESSION_ID));
        if (lastMessage != null) {
            log.trace("Applying last message defaults");
            if (!data.assignsField(MESSAGES.LOCATION_ID))
                data.set(MESSAGES.LOCATION_ID, lastMessage.getLocationId());

            if (!data.assignsField(MESSAGES.TIME))
                data.set(MESSAGES.TIME, lastMessage.getTime());

            if (!data.assignsField(MESSAGES.WORLD_ID))
                data.set(MESSAGES.WORLD_ID, lastMessage.getWorldId());
            else if (!Objects.equals(data.requireValue(MESSAGES.WORLD_ID), lastMessage.getWorldId())) {
                log.error("WORLD ID mismatch: \n last: {} vs \n new: {}",
                        lastMessage.getWorldId(),
                        data.requireValue(MESSAGES.WORLD_ID)
                );
                throw new IllegalArgumentException("World id mismatch");
            }
        }
    }


    @Transactional(readOnly = true)
    public List<MessagesRecord> getMessages(@NotNull SessionsRecord session) {
        return this.getMatching(EntityKey.of(MESSAGES.SESSION_ID, session.getId()));
    }
    @Transactional(readOnly = true)
    public ResponsesRecord getActiveResponseOf(@NotNull EntityKey<MessagesRecord> key) {
        MessagesRecord record = this.find(key)
                .orElseThrow(() -> new EntityNotFound("No message with this key " + key, Severity.SYSTEM));
        return getActiveResponseOf(record);
    }

    public ResponsesRecord getActiveResponseOf(@NotNull MessagesRecord record) {
        return responseService.find(
                EntityKey.<ResponsesRecord>builder()
                        .set(RESPONSES.SESSION_ID, record.getSessionId())
                        .set(RESPONSES.TICK_NUM, record.getTickNum())
                        .set(RESPONSES.RESPONSE_NUM, record.getActiveResponse())
                        .build()
        ).orElseThrow(() -> new EntityNotFound("Response not found", Severity.SYSTEM));
    }

    public boolean isFirstMessage(@NotNull MessagesRecord record) {
        return record.getTickNum() == FIRST_MESSAGE_TICK_NUM;
    }

    public boolean isFirstMessage(int tickNum) {
        return tickNum == FIRST_MESSAGE_TICK_NUM;
    }
}
