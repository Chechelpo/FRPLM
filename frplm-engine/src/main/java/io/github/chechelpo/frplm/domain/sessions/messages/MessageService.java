package io.github.chechelpo.frplm.domain.sessions.messages;

import io.github.chechelpo.frplm.core.entities.fields.FieldValidator;
import io.github.chechelpo.frplm.domain.sessions.core.SessionService;
import io.github.chechelpo.frplm.domain.sessions.session_characters.SessionCharacterService;
import io.github.chechelpo.frplm.events.EventBus;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityService;
import io.github.chechelpo.frplm.exceptions.runtime.InvalidValue;
import io.github.chechelpo.frplm.exceptions.runtime.UnexpectedException;
import io.github.chechelpo.frplm.jooq.generated.tables.records.*;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionRole;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

import static io.github.chechelpo.frplm.jooq.generated.Tables.*;

@Service
public class MessageService extends EntityService<MessagesRecord, MessageStore> {
    public static final int FIRST_MESSAGE_TICK_NUM = 1;
    private final SessionService sessionService;
    private final ResponseService responseService;
    private final SessionCharacterService sessionCharacterService;

    MessageService(
            MessageStore store,
            EventBus eventBus,
            FieldValidator<MessagesRecord> validator,
            SessionService sessionService,
            ResponseService responseService,
            SessionCharacterService sessionCharacterService) {
        super(store, validator, eventBus);
        this.sessionService = sessionService;
        this.responseService = responseService;
        this.sessionCharacterService = sessionCharacterService;
    }

    private SessionsRecord getSession(int sessionId){
        return sessionService.find(EntityKey.of(SESSIONS.ID, sessionId)).orElseThrow();
    }

    public MessagesRecord getLastMessageOf(@NotNull SessionsRecord record) {
        return this.getLastMessageOf(record.getId());
    }

    public MessagesRecord getLastMessageOf(int sessionID) {
        return store.getLastMessage(sessionID);
    }

    public MessagesRecord getLastEnabled(int sessionId) {
        return store.getLastEnabled(sessionId);
    }

    public List<MessagesRecord> getLastMessagesOf(int sessionId, int number) {
        return store.getLast(sessionId, number);
    }

    public List<MessagesRecord> getLastEnabledMessages(int sessionId, int number) {
        return store.getLastEnabled(sessionId, number);
    }

    /**
     * @return messages in range from <= tick_num <= to (both inclusive) and descending order (last messages first)
     */
    public List<MessagesRecord> getRange(int sessionId, int from, int to) {
        return store.getRange(sessionId, from, to);
    }

    @Override
    @SuppressWarnings("SpringTransactionalMethodCallsInspection")
    protected void beforeUpdate(@NotNull EntityKey<MessagesRecord> target, EntityDataPayload<MessagesRecord> data, long operationID) {
        if (data.assigns(MESSAGES.ACTIVE_RESPONSE)) {
            validateActiveResponse(target, data.require(MESSAGES.ACTIVE_RESPONSE));
            applyActiveResponseValues(target, data);
        }
        //throwIfAssignsPromptButIsUserMessage(data);
        //throwIfAssignsReasoningButIsUserMessage(data);

        super.beforeUpdate(target, data, operationID);
    }

    private static void throwIfAssignsReasoningButIsUserMessage(EntityDataPayload<MessagesRecord> data) {
        if (
                data.assigns(MESSAGES.REASONING) &&
                        !data.require(MESSAGES.ROLE).equals(ChatCompletionRole.ASSISTANT.wireValue())
        ) throw new InvalidValue("Can't assign reasoning to a user message");
    }

    private static void throwIfAssignsPromptButIsUserMessage(EntityDataPayload<MessagesRecord> data) {
        if (
                data.assigns(MESSAGES.REQUEST_JSON) &&
                        !data.require(MESSAGES.ROLE).equals(ChatCompletionRole.ASSISTANT.wireValue())
        ) throw new InvalidValue("Can't assign request_json to a user message");
    }

    /**
     * Ignored if it's a user message. Ignored if the active response is already this response.
     */
    void validateActiveResponse(EntityKey<MessagesRecord> target, short active_response) {
        int sessionId = target.requireNonNull(MESSAGES.SESSION_ID);
        int tick_num = target.requireNonNull(MESSAGES.TICK_NUM);
        MessagesRecord record = this.find(target)
                .orElseThrow(notFound -> {
                    log.error("No message found for session id {} tick num {}", sessionId, tick_num);
                    return new EntityNotFound("Message not found when setting active response " + notFound.toString(), Severity.EXPECTED);
                });

        if (record.getRole().equals(ChatCompletionRole.USER.wireValue())) {
            log.debug("Attempted changing the response number of a user message");
            return;
        }
        if (record.getActiveResponse() == active_response) {
            log.debug("Response {} of message (session: {} , tick: {}) is already active", active_response, sessionId, tick_num);
            return;
        }

        int maxResponseNum = record.getResponseNum();
        if (maxResponseNum < active_response) {
            log.error("Max number response is {} yet attempted to change to {}", maxResponseNum, active_response);
            throw new InvalidValue("There's no response with number " + active_response);
        }
    }

    @Contract(mutates = "param2")
    private void applyActiveResponseValues(@NotNull EntityKey<MessagesRecord> target, EntityDataPayload<MessagesRecord> data) {
        ResponsesRecord newActiveResponse = responseService.find(EntityKey.<ResponsesRecord>builder()
                .set(RESPONSES.SESSION_ID, target.require(MESSAGES.SESSION_ID))
                .set(RESPONSES.TICK_NUM, target.require(MESSAGES.TICK_NUM))
                .set(RESPONSES.RESPONSE_NUM, data.require(MESSAGES.ACTIVE_RESPONSE))
                .build()
        ).orElseThrow(notFound -> new EntityNotFound("Not Found when applying active response: " + notFound.toString(), Severity.SYSTEM));

        data.set(MESSAGES.CONTENT, newActiveResponse.getContent());
        data.set(MESSAGES.ACTIVE_RESPONSE, newActiveResponse.getResponseNum());
        data.set(MESSAGES.REASONING, newActiveResponse.getReasoning());
        data.set(MESSAGES.LOCATION_ID, newActiveResponse.getLocationId());
        data.set(MESSAGES.TIME, newActiveResponse.getAdvancesTimeBy());
    }

    @Override
    protected void afterSuccessfulUpdate(MessagesRecord previousData, EntityKey<MessagesRecord> key, EntityDataPayload<MessagesRecord> updated, long operationID) {
        if (
                !updated.assigns(MESSAGES.ACTIVE_RESPONSE) &&
                        updated.assigns(MESSAGES.LOCATION_ID) || updated.assigns(MESSAGES.CONTENT) || updated.assigns(MESSAGES.TIME)
        )
            updateActiveResponse(key, updated);

        super.afterSuccessfulUpdate(previousData, key, updated, operationID);
    }

    @SuppressWarnings("SpringTransactionalMethodCallsInspection")
    private void updateActiveResponse(EntityKey<MessagesRecord> target, EntityDataPayload<MessagesRecord> data) {
        ResponsesRecord currentActiveResponse = this.getActiveResponseOf(target);
        EntityDataPayload<ResponsesRecord> changed = EntityDataPayload.<ResponsesRecord>builder().build();

        data.getAssignment(MESSAGES.LOCATION_ID)
                .ifAssignedNotNull(locationId -> changed.set(RESPONSES.LOCATION_ID, locationId));

        data.getAssignment(MESSAGES.CONTENT)
                .ifAssignedNotNull(content -> changed.set(RESPONSES.CONTENT, content));

        data.getAssignment(MESSAGES.REASONING)
                .ifAssignedNotNull(reasoning -> changed.set(RESPONSES.REASONING, reasoning));

        data.getAssignment(MESSAGES.TIME)
                .ifAssignedNotNull(time -> changed.set(RESPONSES.ADVANCES_TIME_BY, time));

        responseService.update(responseService.keyOf(currentActiveResponse), changed)
                .orElseThrow("Couldn't update active response of message: " + target);
    }

    @Override
    protected void beforeCreate(@NotNull EntityDataPayload<MessagesRecord> data, long operationID) {
        applyDefaultsOfLastMessage(data);
        throwIfAssignsPromptButIsUserMessage(data);
        throwIfAssignsReasoningButIsUserMessage(data);

        data.set(
                MESSAGES.TICK_NUM,
                sessionService.incrementAndGet(
                                SESSIONS.CURRENT_TICK,
                                sessionService.keyOf(data.requireNonNull(MESSAGES.SESSION_ID))
                        )
                        .orElseThrow(() -> {
                            log.error("Could not fetch next message tick for new message \n {}", data.assignments());
                            return new EntityNotFound("Could not fetch tick for new message", Severity.SYSTEM);
                        })
        );

        SessionsRecord sessionRecord = this.getSession(data.requireNonNull(MESSAGES.SESSION_ID));
        data.ifUnassignedGet(
                MESSAGES.LOCATION_ID,
                () -> sessionCharacterService.getOneMatching(
                        EntityDataPayload.<SessionCharactersRecord>builder()
                                .set(SESSION_CHARACTERS.SESSION_ID, sessionRecord.getId())
                                .set(SESSION_CHARACTERS.PERMANENT_CHARACTER_ID, sessionRecord.getUserPersonaId())
                                .build()
                ).resolve().getCurrentLocationId()
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
                .set(RESPONSES.REASONING, data.getReasoning())
                .build());
        super.afterSuccessfulCreate(data, operationID);
    }

    @Transactional
    public void registerNewResponse(int sessionId, int tick_num, String content) {
        registerNewResponse(sessionId, tick_num, content, null);
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
    public void registerNewResponse(int sessionId, int tick_num, String content, String reasoning) {
        EntityKey<MessagesRecord> messageKey = EntityKey.<MessagesRecord>builder()
                .set(MESSAGES.SESSION_ID, sessionId)
                .set(MESSAGES.TICK_NUM, tick_num)
                .build();

        if (!exists(messageKey))
            throw new EntityNotFound("No message with this id found when registering response", Severity.SYSTEM);
        ChatCompletionRole messageRole = ChatCompletionRole.fromWireValue(getNonNullValueOf(MESSAGES.ROLE, messageKey));
        if (messageRole != ChatCompletionRole.ASSISTANT)
            throw new InvalidValue("Tried to register a new response of a user message");

        int world_id = sessionService.getNonNullValueOf(SESSIONS.WORLD_ID, sessionService.keyOf(sessionId));
        registerNewResponse(EntityDataPayload.<ResponsesRecord>builder()
                .set(RESPONSES.SESSION_ID, sessionId)
                .set(RESPONSES.TICK_NUM, tick_num)
                .set(RESPONSES.REASONING, reasoning)
                .set(RESPONSES.WORLD_ID, world_id)
                .set(RESPONSES.CONTENT, content)
                .build());
    }

    @SuppressWarnings("SpringTransactionalMethodCallsInspection")
    private void registerNewResponse(EntityDataPayload<ResponsesRecord> payload) {
        EntityKey<MessagesRecord> messageKey = EntityKey.<MessagesRecord>builder()
                .set(MESSAGES.SESSION_ID, payload.require(RESPONSES.SESSION_ID))
                .set(MESSAGES.TICK_NUM, payload.require(RESPONSES.TICK_NUM))
                .build();
        short newActiveResponseNum = this.incrementAndGet(MESSAGES.RESPONSE_NUM, messageKey)
                .orElseThrow(() -> new UnexpectedException("Couldn't increment response num", Severity.SYSTEM));

        payload.set(RESPONSES.RESPONSE_NUM, newActiveResponseNum);

        ResponsesRecord newResponse = responseService.createAndGet(payload);

        this.update(
                messageKey,
                EntityDataPayload.<MessagesRecord>builder()
                        .set(MESSAGES.ACTIVE_RESPONSE, newActiveResponseNum)
                        .build()
        ).orElseThrow();
    }

    /**
     * Simply checks if this message is the last one before deleting
     */
    @Override
    protected void beforeDelete(@NotNull EntityKey<MessagesRecord> id, long operationID) {
        int sessionId = id.require(MESSAGES.SESSION_ID);
        int requestedTick = id.require(MESSAGES.TICK_NUM);

        MessagesRecord lastMessage = getLastMessageOf(sessionId);
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
        MessagesRecord lastMessage = getLastMessageOf(data.require(MESSAGES.SESSION_ID));
        if (lastMessage != null) {
            log.trace("Applying last message defaults");
            if (!data.assigns(MESSAGES.LOCATION_ID))
                data.set(MESSAGES.LOCATION_ID, lastMessage.getLocationId());

            if (!data.assigns(MESSAGES.TIME))
                data.set(MESSAGES.TIME, lastMessage.getTime());

            if (!data.assigns(MESSAGES.WORLD_ID))
                data.set(MESSAGES.WORLD_ID, lastMessage.getWorldId());
            else if (!Objects.equals(data.require(MESSAGES.WORLD_ID), lastMessage.getWorldId())) {
                log.error("WORLD ID mismatch: \n last: {} vs \n new: {}",
                        lastMessage.getWorldId(),
                        data.require(MESSAGES.WORLD_ID)
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
        MessagesRecord record = this.find(key).orElseThrow(Severity.SYSTEM);
        return getActiveResponseOf(record);
    }

    public ResponsesRecord getActiveResponseOf(@NotNull MessagesRecord record) {
        return responseService.find(
                EntityKey.<ResponsesRecord>builder()
                        .set(RESPONSES.SESSION_ID, record.getSessionId())
                        .set(RESPONSES.TICK_NUM, record.getTickNum())
                        .set(RESPONSES.RESPONSE_NUM, record.getActiveResponse())
                        .build()
        ).orElseThrow("Couldn't find active response of message: \n" + record, Severity.SYSTEM);
    }

    public boolean isFirstMessage(@NotNull MessagesRecord record) {
        return record.getTickNum() == FIRST_MESSAGE_TICK_NUM;
    }

    public boolean isFirstMessage(int tickNum) {
        return tickNum == FIRST_MESSAGE_TICK_NUM;
    }
}
