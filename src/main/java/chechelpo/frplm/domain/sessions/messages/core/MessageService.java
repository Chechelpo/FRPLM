package chechelpo.frplm.domain.sessions.messages.core;

import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.domain.character.core.CharacterService;
import chechelpo.frplm.domain.character.starting_locations.StartingLocationsService;
import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.events.crud.CRUDCommittedEvent;
import chechelpo.frplm.frameworks.entities.microservices.EntityDataPayload;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.frameworks.entities.microservices.EntityService;
import chechelpo.frplm.jooq.generated.tables.Characters;
import chechelpo.frplm.jooq.generated.tables.Sessions;
import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import chechelpo.frplm.jooq.generated.tables.records.MessagesRecord;
import chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

import static chechelpo.frplm.jooq.generated.Tables.*;

@Service
public class MessageService extends EntityService<MessagesRecord, MessageStore> {
    private final CharacterService characters;
    private final StartingLocationsService startingLocations;
    MessageService(CharacterService characters, StartingLocationsService startingLocations, MessageStore store, EventBus eventBus) {
        super(store, eventBus);
        this.characters = characters;
        this.startingLocations = startingLocations;
    }

    @TransactionalEventListener
    void createFirstMessage(CRUDCommittedEvent.@NotNull CreatedEntity<?> event) {
        if (event.type() != EntityTypes.Types.SECTIONS) return;

        CRUDCommittedEvent.CreatedEntity<SessionsRecord> createdSession =
                (CRUDCommittedEvent.CreatedEntity<SessionsRecord>) event;

        SessionsRecord session = createdSession.record();
        String firstMessage = characters.getValueOf(
                Characters.CHARACTERS.WELCOME_MESSAGE,
                EntityKey.of(Characters.CHARACTERS.ID, session.getValue(Sessions.SESSIONS.USER_PERSONA_ID))
        );
        List<LocationsRecord> locations = startingLocations.startingLocationAt(
                EntityKey.of(CHARACTERS.ID, session.getValue(SESSIONS.ID)),
                session.getWorldId()
        );
        if (locations.size() != 1)
            throw new IllegalStateException("Expected exactly one location for a user persona character");

        store.createAndGet(EntityDataPayload.<MessagesRecord>builder()
                        .set(MESSAGES.SESSION_ID, session.getId())
                        .set(MESSAGES.TICK_NUM, 0)
                        .set(MESSAGES.LOCATION_ID, locations.getFirst().getId())
                        .set(MESSAGES.CONTENT, firstMessage)
                        .set(MESSAGES.USER_GENERATED, true)
                        .build()
        );
    }
}
