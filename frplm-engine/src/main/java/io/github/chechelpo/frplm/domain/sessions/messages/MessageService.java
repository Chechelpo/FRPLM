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
            SessionCharacterService sessionCharacterService
    ) {
        super(store, validator, eventBus);
        this.sessionService = sessionService;
        this.responseService = responseService;
        this.sessionCharacterService = sessionCharacterService;
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // UTILS
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    private SessionsRecord getSession(int sessionId) {
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

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // CREATE
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    @Override
    protected void beforeCreate(@NotNull EntityDataPayload<MessagesRecord> data, long operationID) {
        applyDefaultsOfLastMessage(data);

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

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // New response registration
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
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
    public void registerNewResponse(
            int sessionId,
            int tickNum,
            String content,
            String reasoning
    ) {
        EntityKey<MessagesRecord> messageKey =
                EntityKey.<MessagesRecord>builder()
                        .set(MESSAGES.SESSION_ID, sessionId)
                        .set(MESSAGES.TICK_NUM, tickNum)
                        .build();

        MessagesRecord message = find(messageKey)
                .orElseThrow("No message found when registering response", Severity.SYSTEM);

        List<MessagesRecord> messages = getMatching(
                EntityKey.of(MESSAGES.SESSION_ID, sessionId)
        );
        MessagesRecord previousMessage = messages.get(messages.size() - 2);

        if (!message.getRole().equals(ChatCompletionRole.ASSISTANT.wireValue())) {
            throw new InvalidValue(
                    "Tried to register a new response of a user message"
            );
        }

        registerNewResponse(
                EntityDataPayload.<ResponsesRecord>builder()
                        .set(RESPONSES.SESSION_ID, sessionId)
                        .set(RESPONSES.TICK_NUM, tickNum)
                        .set(RESPONSES.WORLD_ID, message.getWorldId())
                        .set(RESPONSES.LOCATION_ID, previousMessage.getLocationId())
                        .set(RESPONSES.CONTENT, content)
                        .set(RESPONSES.REASONING, reasoning)
                        .build()
        );
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

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // UPDATE
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @Override
    protected void beforeUpdate(@NotNull EntityKey<MessagesRecord> target, EntityDataPayload<MessagesRecord> data, long operationID) {
        if (data.assigns(MESSAGES.ACTIVE_RESPONSE)) {
            validateActiveResponse(target, data.requireNonNull(MESSAGES.ACTIVE_RESPONSE));
            applyActiveResponseValues(target, data);
        }

        super.beforeUpdate(target, data, operationID);
    }

    @Override
    protected void afterSuccessfulUpdate(MessagesRecord previousData, EntityKey<MessagesRecord> key, EntityDataPayload<MessagesRecord> updated, long operationID) {
        if (
                !updated.assigns(MESSAGES.ACTIVE_RESPONSE) &&
                        updated.assignsAny(List.of(MESSAGES.LOCATION_ID, MESSAGES.CONTENT, MESSAGES.TIME))
        )
            updateActiveResponse(key, updated);

        super.afterSuccessfulUpdate(previousData, key, updated, operationID);
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
        ResponsesRecord newActiveResponse = responseService.require(EntityKey.<ResponsesRecord>builder()
                .setNonNull(RESPONSES.SESSION_ID, MESSAGES.SESSION_ID, target)
                .setNonNull(RESPONSES.TICK_NUM, MESSAGES.TICK_NUM, target)
                .setNonNull(RESPONSES.RESPONSE_NUM, MESSAGES.ACTIVE_RESPONSE, data)
                .build()
        );

        data
                .set(MESSAGES.CONTENT, RESPONSES.CONTENT, newActiveResponse)
                .set(MESSAGES.ACTIVE_RESPONSE, RESPONSES.RESPONSE_NUM, newActiveResponse)
                .set(MESSAGES.REASONING, RESPONSES.REASONING, newActiveResponse)
                .set(MESSAGES.LOCATION_ID, RESPONSES.LOCATION_ID, newActiveResponse)
                .set(MESSAGES.TIME, RESPONSES.ADVANCES_TIME_BY, newActiveResponse);
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

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // DELETE
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    /**
     * Simply checks if this message is the last one before deleting
     */
    @Override
    protected void beforeDelete(@NotNull EntityKey<MessagesRecord> id, long operationID) {
        int sessionId = id.requireNonNull(MESSAGES.SESSION_ID);
        int requestedTick = id.requireNonNull(MESSAGES.TICK_NUM);

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
        MessagesRecord lastMessage = getLastMessageOf(data.requireNonNull(MESSAGES.SESSION_ID));
        if (lastMessage == null) return;

        log.trace("Applying last message defaults");
        data
                .ifUnassignedSet(MESSAGES.LOCATION_ID, lastMessage.getLocationId())
                .ifUnassignedSet(MESSAGES.TIME, lastMessage.getTime())
                .ifUnassignedSet(MESSAGES.WORLD_ID, lastMessage.getWorldId());

    }
}
