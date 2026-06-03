package chechelpo.frplm.domain.sessions.core;

import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.domain.character.core.CharacterService;
import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.events.crud.CRUDCommittedEvent;
import chechelpo.frplm.exceptions.runtime.InvalidValue;
import chechelpo.frplm.exceptions.runtime.EntityNotFound;
import chechelpo.frplm.frameworks.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.frameworks.entities.pseudo_services.EntityKey;
import chechelpo.frplm.frameworks.entities.pseudo_services.EntityService;
import chechelpo.frplm.jooq.generated.tables.records.MessagesRecord;
import chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;

import static chechelpo.frplm.jooq.generated.Tables.*;

@Service
public class SessionService extends EntityService<SessionsRecord, SessionStore> {
    private final CharacterService characterService;

    SessionService(SessionStore store, CharacterService characters, EventBus eventBus) {
        super(store, eventBus);
        this.characterService = characters;
    }

    public Optional<Integer> getUserCharacterID(EntityKey<SessionsRecord> key){
        return this.getValueOf(SESSIONS.USER_PERSONA_ID, key);
    }

    @Override
    protected void beforeCreate(@NotNull EntityDataPayload<SessionsRecord> data, long operationID) {
        boolean canBeUser;
        try{
            canBeUser =  characterService.getValueOf(
                    CHARACTERS.CAN_BE_USER,
                    EntityKey.of(CHARACTERS.ID, data.requireValue(SESSIONS.USER_PERSONA_ID))
            ).orElseThrow(() -> new RuntimeException("a"));
        } catch (EntityNotFound e) {
            throw new RuntimeException(e);
        }
        if (!canBeUser) throw new InvalidValue("Picked character can't be user");
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
