package chechelpo.frplm.domain.sessions.messages.core;

import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.domain.character.core.CharacterService;
import chechelpo.frplm.domain.character.starting_locations.StartingLocationsService;
import chechelpo.frplm.domain.sessions.core.SessionService;
import chechelpo.frplm.domain.sessions.messages.gen.GenService;
import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.events.crud.CRUDCommittedEvent;
import chechelpo.frplm.exceptions.Severity;
import chechelpo.frplm.exceptions.runtime.EntityNotFound;
import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.core.entities.pseudo_services.EntityService;
import chechelpo.frplm.jooq.generated.tables.Characters;
import chechelpo.frplm.jooq.generated.tables.Sessions;
import chechelpo.frplm.jooq.generated.tables.records.*;
import chechelpo.frplm.openai_compatible.ChatCompletionRole;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Objects;

import static chechelpo.frplm.jooq.generated.Tables.*;

@Service
public class MessageService extends EntityService<MessagesRecord, MessageStore> {
    public static final int FIRST_MESSAGE_TICK_NUM = 1;
    private final CharacterService characters;
    private final StartingLocationsService startingLocations;
    private final GenService gen;
    private final SessionService sessionService;
    private final GenService genService;

    MessageService(CharacterService characters,
                   StartingLocationsService startingLocations,
                   GenService genService,
                   MessageStore store,
                   EventBus eventBus,
                   SessionService sessionService
                   ) {
        super(store, eventBus);
        this.characters = characters;
        this.startingLocations = startingLocations;
        this.gen = genService;
        this.sessionService = sessionService;
        this.genService = genService;
    }


    private void fillContentOf(@NotNull MessagesRecord record) {
        if (
                record.getContent() == null &&
                        ChatCompletionRole.fromWireValue(record.getRole()) == ChatCompletionRole.ASSISTANT
        ) {
            String content = this.gen.getActiveResponseOf(keyOf(record))
                        .orElseThrow(() -> {
                            log.error("Tried to fill the response of a generated message but found no response");
                            return new EntityNotFound("Tried to fill content of a generated message with no response", Severity.SYSTEM);
                        }).getContent();

            record.set(MESSAGES.CONTENT, content);
        }

    }

    public MessagesRecord getLastOf(@NotNull SessionsRecord record) {
        return this.getLastOf(record.getId());
    }

    public MessagesRecord getLastOf(int sessionID) {
        return store.getLastMessage(sessionID);
    }

    @Override
    protected void beforeCreate(@NotNull EntityDataPayload<MessagesRecord> data, long operationID) {
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
    protected void afterSuccessfulCreate(MessagesRecord messageRecord, long operationID) {
        super.afterSuccessfulCreate(messageRecord, operationID);
        if (messageRecord.getRole().equals(ChatCompletionRole.ASSISTANT.wireValue()) && !isFirstMessage(messageRecord))
            registerGeneratedResponse(messageRecord);
    }

    protected void registerGeneratedResponse(@NotNull MessagesRecord record) {
        EntityKey<LlmGenRecord> key = EntityKey.<LlmGenRecord>builder()
                .set(LLM_GEN.SESSION_ID, record.getSessionId())
                .set(LLM_GEN.TICK_NUM, record.getTickNum())
                .build();
        LlmGenRecord generated = genService.find(key)
                .orElse(
                        genService.createAndGet(EntityDataPayload.<LlmGenRecord>builder()
                                .set(LLM_GEN.SESSION_ID, record.getSessionId())
                                .set(LLM_GEN.TICK_NUM, record.getTickNum())
                                .set(LLM_GEN.PROMPT, " ")
                                .build()
                        )
                );

        genService.registerNewResponse(key, record.getContent());
        this.update(keyOf(record), EntityDataPayload.of(MESSAGES.CONTENT, null));
    }


    @Override
    protected void afterRetrieve(@NotNull List<MessagesRecord> records, long operationID) {
        for (MessagesRecord record : records) fillContentOf(record);
    }

    @Transactional (readOnly = true)
    public List<MessagesRecord> getMessages(@NotNull SessionsRecord session) {
        return this.getMatching(EntityKey.of(MESSAGES.SESSION_ID, session.getId()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener
    void createFirstMessage(CRUDCommittedEvent.@NotNull CreatedEntity<?> event) {
        if (event.type() != EntityTypes.Types.SESSIONS) return;

        CRUDCommittedEvent.CreatedEntity<SessionsRecord> createdSession =
                (CRUDCommittedEvent.CreatedEntity<SessionsRecord>) event;
        SessionsRecord session = createdSession.record();
        log.debug("Registering first message for session {}", session.getId());

        EntityKey<CharactersRecord> characterKey =
                EntityKey.of(Characters.CHARACTERS.ID, session.getValue(Sessions.SESSIONS.USER_PERSONA_ID));

        String firstMessage;
        try {
            firstMessage = characters.getValueOf(Characters.CHARACTERS.WELCOME_MESSAGE, characterKey)
                    .orElse("This character has no welcome message");

        } catch (EntityNotFound e) {
            log.error("No such character: {}", e.getMessage());
            throw new RuntimeException(e);
        }

        List<LocationsRecord> locations = startingLocations.startingLocationAt(
                characterKey,
                session.getWorldId()
        );

        if (locations.size() != 1){
            log.error("Expected only one location for session {}, got instead: \n {}", session.getId(), locations);
            throw new IllegalStateException("Expected exactly one location for a user persona character");
        }

        this.createAndGet(EntityDataPayload.<MessagesRecord>builder()
                .set(MESSAGES.TIME, 0)
                .set(MESSAGES.SESSION_ID, session.getId())
                .set(MESSAGES.TICK_NUM, FIRST_MESSAGE_TICK_NUM)
                .set(MESSAGES.WORLD_ID, session.getWorldId())
                .set(MESSAGES.LOCATION_ID, locations.getFirst().getId())
                .set(MESSAGES.CONTENT, firstMessage)
                .set(MESSAGES.ROLE, "assistant")
                .build()
        );
    }
    private boolean isFirstMessage(@NotNull MessagesRecord record) {
        return record.getTickNum() == FIRST_MESSAGE_TICK_NUM;
    }
}
