package chechelpo.frplm.domain.sessions.core;

import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.domain.character.core.CharacterService;
import chechelpo.frplm.domain.character.starting_locations.StartingLocationsService;
import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.events.crud.CRUDCommittedEvent;
import chechelpo.frplm.exceptions.runtime.InvalidValue;
import chechelpo.frplm.exceptions.runtime.EntityNotFound;
import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.core.entities.pseudo_services.EntityService;
import chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import chechelpo.frplm.jooq.generated.tables.records.MessagesRecord;
import chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Optional;

import static chechelpo.frplm.jooq.generated.Tables.*;

@Service
public class SessionService extends EntityService<SessionsRecord, SessionStore> {
    private final CharacterService characterService;
    private final StartingLocationsService startingLocationsService;

    SessionService(SessionStore store, CharacterService characters, EventBus eventBus, StartingLocationsService startingLocationsService) {
        super(store, eventBus);
        this.characterService = characters;
        this.startingLocationsService = startingLocationsService;
    }
    @Transactional(readOnly = true)
    public Optional<Integer> getUserCharacterID(int sessionID) {
        return this.getValueOf(SESSIONS.USER_PERSONA_ID, EntityKey.of(SESSIONS.ID, sessionID));
    }
    @Transactional(readOnly = true)
    public Optional<Integer> getUserCharacterID(EntityKey<SessionsRecord> key){
        return this.getValueOf(SESSIONS.USER_PERSONA_ID, key);
    }

    @Override
    protected void beforeCreate(@NotNull EntityDataPayload<SessionsRecord> data, long operationID) {
        EntityKey<CharactersRecord> characterKey = EntityKey.of(CHARACTERS.ID, data.requireValue(SESSIONS.USER_PERSONA_ID));
        boolean canBeUser =  characterService.getValueOf(
                    CHARACTERS.CAN_BE_USER,
                        characterKey
            )
                .orElseThrow(() -> new IllegalArgumentException("Character does not exist"));

        if (!canBeUser) throw new InvalidValue("Picked character can't be user");
        List<LocationsRecord> thisStartingLocations = startingLocationsService
                .startingLocationAt(characterKey, data.requireValue(SESSIONS.WORLD_ID));

        if (thisStartingLocations.isEmpty())
            throw new InvalidValue("This character has no starting locations in this world");
        super.beforeCreate(data, operationID);
    }

    @TransactionalEventListener
    void onDeleteMessage(CRUDCommittedEvent.@NotNull DeletedEntity<?> rawEvent) {
        if (rawEvent.type()!= EntityTypes.Types.MESSAGES) return;

        CRUDCommittedEvent.DeletedEntity<MessagesRecord> event = (CRUDCommittedEvent.DeletedEntity<MessagesRecord>) rawEvent;
        log.debug("Message deleted, updating tick num");
        if (!store.decrementTickNum(event.key().requireValue(MESSAGES.SESSION_ID))){
            log.error("Couldn't decrement session tick after message deletion");
            throw new IllegalStateException("Couldn't decrement session tick after message deletion");
        }
    }
}
