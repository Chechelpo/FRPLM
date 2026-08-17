package io.github.chechelpo.frplm.domain.sessions.messages;

import ch.qos.logback.classic.Logger;
import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.domain.character.core.CharacterService;
import io.github.chechelpo.frplm.domain.sessions.core.SessionService;
import io.github.chechelpo.frplm.domain.sessions.session_characters.SessionCharacterService;
import io.github.chechelpo.frplm.events.crud.CRUDCommittedEvent;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.MessagesRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.SessionCharactersRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static io.github.chechelpo.frplm.domain.sessions.messages.MessageService.FIRST_MESSAGE_TICK_NUM;
import static io.github.chechelpo.frplm.jooq.generated.Tables.*;

@Component
final class MessageEvents {
    private static final Logger log = (Logger) LoggerFactory.getLogger(MessageEvents.class);
    private final CharacterService characterService;
    private final MessageService messageService;
    private final SessionService sessionService;

    MessageEvents(
            CharacterService characterService,
            MessageService messageService,
            SessionService sessionService
    ) {
        this.characterService = characterService;
        this.messageService = messageService;
        this.sessionService = sessionService;
    }

    @EventListener
    void createFirstMessage(CRUDCommittedEvent.@NotNull CreatedEntity<?> rawEvent) {
        if (rawEvent.isNotEventOf(SESSIONS)) return;

        //noinspection unchecked
        CRUDCommittedEvent.CreatedEntity<SessionsRecord> event =
                (CRUDCommittedEvent.CreatedEntity<SessionsRecord>) rawEvent;
        SessionsRecord session = event.record();
        log.debug("Registering first message for session {}", session.getId());

        CharactersRecord userCharacter = characterService.find(
                EntityKey.<CharactersRecord>builder()
                        .set(CHARACTERS.ID, session.getUserPersonaId())
                        .set(CHARACTERS.WORLD_ID, session.getWorldId())
                        .build()
                ).orElseThrow("Could not find starting location", Severity.SYSTEM);

        messageService.createAndGet(EntityDataPayload.<MessagesRecord>builder()
                .set(MESSAGES.SESSION_ID, SESSIONS.ID, session)
                .set(MESSAGES.TICK_NUM, FIRST_MESSAGE_TICK_NUM)
                .set(MESSAGES.ROLE, "assistant")
                .set(MESSAGES.WORLD_ID, SESSIONS.WORLD_ID, session)
                .set(MESSAGES.LOCATION_ID, CHARACTERS.STARTING_LOCATION_ID, userCharacter)
                .set(MESSAGES.CONTENT, CHARACTERS.WELCOME_MESSAGE, userCharacter)
                .build()
        );
    }

    @SuppressWarnings("unchecked")
    @EventListener
    void onUserMovementChangeMessage(CRUDCommittedEvent.UpdatedEntity<?> rawEvent){
        if (rawEvent.isNotEventOf(SESSION_CHARACTERS)) return;
        onUserMovementChangeMessageLocation(
                (CRUDCommittedEvent.UpdatedEntity<SessionCharactersRecord>) rawEvent
        );
    }

    private void onUserMovementChangeMessageLocation(CRUDCommittedEvent.UpdatedEntity<SessionCharactersRecord> event){
        if (!event.assigned(SESSION_CHARACTERS.CURRENT_LOCATION_ID)) return;

        int sessionId = event.target().requireNonNull(SESSION_CHARACTERS.SESSION_ID);
        SessionsRecord session = sessionService.require(EntityKey.of(SESSIONS.ID, sessionId));
        if (
                !Objects.equals(
                        session.getUserPersonaId(),
                        event.previousData().getPermanentCharacterId()
                )
        ) return;

        MessagesRecord message = messageService.getLastMessageOf(session);

        messageService.update(
                MESSAGES.LOCATION_ID, event.updatedData().requireNonNull(SESSION_CHARACTERS.CURRENT_LOCATION_ID),
                message
        ).orElseThrow();
    }
}
