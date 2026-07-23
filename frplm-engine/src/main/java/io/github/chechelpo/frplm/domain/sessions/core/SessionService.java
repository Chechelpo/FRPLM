package io.github.chechelpo.frplm.domain.sessions.core;

import io.github.chechelpo.frplm.domain.character.core.CharacterService;
import io.github.chechelpo.frplm.domain.character.starting_locations.StartingLocationsService;
import io.github.chechelpo.frplm.events.EventBus;
import io.github.chechelpo.frplm.events.crud.CRUDCommittedEvent;
import io.github.chechelpo.frplm.exceptions.runtime.InvalidValue;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.MessagesRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Optional;

import static io.github.chechelpo.frplm.jooq.generated.Tables.*;

@Service
public class SessionService extends EntityService<SessionsRecord, SessionStore> {
    private final CharacterService characterService;
    private final StartingLocationsService startingLocationsService;

    SessionService(SessionStore store, CharacterService characters, EventBus eventBus, StartingLocationsService startingLocationsService) {
        super(store, eventBus);
        this.characterService = characters;
        this.startingLocationsService = startingLocationsService;
    }

    public EntityKey<SessionsRecord> keyOf(int sessionId){
        return EntityKey.of(SESSIONS.ID, sessionId);
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
        EntityKey<CharactersRecord> characterKey = characterService.keyOf(data.requireValue(SESSIONS.USER_PERSONA_ID));
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
        if (rawEvent.type()!= EntityConfigs.Types.MESSAGES) return;

        CRUDCommittedEvent.DeletedEntity<MessagesRecord> event = (CRUDCommittedEvent.DeletedEntity<MessagesRecord>) rawEvent;
        log.debug("Message deleted, updating tick num");
        if (!store.decrementTickNum(event.key().requireValue(MESSAGES.SESSION_ID))){
            log.error("Couldn't decrement session tick after message deletion");
            throw new IllegalStateException("Couldn't decrement session tick after message deletion");
        }
    }
}
