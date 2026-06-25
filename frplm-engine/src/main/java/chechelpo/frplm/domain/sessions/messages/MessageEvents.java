package chechelpo.frplm.domain.sessions.messages;

import ch.qos.logback.classic.Logger;
import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.domain.character.core.CharacterService;
import chechelpo.frplm.domain.character.starting_locations.StartingLocationsService;
import chechelpo.frplm.events.crud.CRUDCommittedEvent;
import chechelpo.frplm.exceptions.Severity;
import chechelpo.frplm.exceptions.runtime.EntityNotFound;
import chechelpo.frplm.exceptions.runtime.InvalidValue;
import chechelpo.frplm.exceptions.runtime.UnexpectedException;
import chechelpo.frplm.jooq.generated.tables.Characters;
import chechelpo.frplm.jooq.generated.tables.Sessions;
import chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import chechelpo.frplm.jooq.generated.tables.records.MessagesRecord;
import chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Optional;

import static chechelpo.frplm.domain.sessions.messages.MessageService.FIRST_MESSAGE_TICK_NUM;
import static chechelpo.frplm.jooq.generated.Tables.CHARACTERS;
import static chechelpo.frplm.jooq.generated.Tables.MESSAGES;

@Component
final class MessageEvents {
    private static final Logger log = (Logger) LoggerFactory.getLogger(MessageEvents.class);
    private final CharacterService characterService;
    private final StartingLocationsService startingLocationsService;
    private final MessageService messageService;

    MessageEvents(CharacterService characterService, StartingLocationsService startingLocationsService, MessageService messageService) {
        this.characterService = characterService;
        this.startingLocationsService = startingLocationsService;
        this.messageService = messageService;
    }
    private Optional<LocationsRecord> getStartingLocationByWorld(int userCharacterId, int worldId) {
        List<LocationsRecord> result = startingLocationsService.startingLocationAt(
                EntityKey.of(CHARACTERS.ID, userCharacterId),
                worldId
        );
        if (result.isEmpty()) return Optional.empty();
        else return Optional.of(result.getFirst());
    }
    @EventListener
    void createFirstMessage(CRUDCommittedEvent.@NotNull CreatedEntity<?> rawEvent) {
        if (rawEvent.type() != EntityTypes.Types.SESSIONS) return;

        CRUDCommittedEvent.CreatedEntity<SessionsRecord> event =
                (CRUDCommittedEvent.CreatedEntity<SessionsRecord>) rawEvent;
        SessionsRecord session = event.record();
        log.debug("Registering first message for session {}", session.getId());

        EntityKey<CharactersRecord> characterKey =
                EntityKey.of(Characters.CHARACTERS.ID, session.getUserPersonaId());

        String firstMessage = characterService.getValueOf(Characters.CHARACTERS.WELCOME_MESSAGE, characterKey)
                    .orElse("This character has no welcome message");

        LocationsRecord startingLocation = getStartingLocationByWorld(session.getUserPersonaId(), session.getWorldId())
                .orElseThrow(() -> new EntityNotFound("Could not find starting location", Severity.SYSTEM));

        messageService.createAndGet(EntityDataPayload.<MessagesRecord>builder()
                .set(MESSAGES.SESSION_ID, session.getId())
                .set(MESSAGES.TICK_NUM, FIRST_MESSAGE_TICK_NUM)
                .set(MESSAGES.ROLE, "assistant")
                .set(MESSAGES.WORLD_ID, session.getWorldId())
                .set(MESSAGES.LOCATION_ID, startingLocation.getId())
                .set(MESSAGES.CONTENT, firstMessage)
                .build()
        );
    }

}
