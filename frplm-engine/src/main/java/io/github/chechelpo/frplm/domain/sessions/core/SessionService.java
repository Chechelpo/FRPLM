package io.github.chechelpo.frplm.domain.sessions.core;

import io.github.chechelpo.frplm.core.entities.fields.FieldValidator;
import io.github.chechelpo.frplm.domain.character.core.CharacterService;
import io.github.chechelpo.frplm.events.EventBus;
import io.github.chechelpo.frplm.events.crud.CRUDCommittedEvent;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.InvalidValue;
import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.MessagesRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

import static io.github.chechelpo.frplm.jooq.generated.Tables.*;

@Service
public class SessionService extends EntityService<SessionsRecord, SessionStore> {
    private final CharacterService characterService;

    SessionService(
            SessionStore store,
            FieldValidator<SessionsRecord> validator,
            CharacterService characters,
            EventBus eventBus
    ) {
        super(store, validator, eventBus);
        this.characterService = characters;
    }

    public EntityKey<SessionsRecord> keyOf(int sessionId){
        return EntityKey.of(SESSIONS.ID, sessionId);
    }


    @Override
    protected void beforeCreate(@NotNull EntityDataPayload<SessionsRecord> data, long operationID) {
        checkUserPersonaInvariants(data);
        super.beforeCreate(data, operationID);
    }

    /// Checks user's {@link CharactersRecord} of session to create. Tests invariants:
    /// 1. The character must exist. Throws {@link EntityNotFound} on failure
    /// 2. The character must be able to be a user. Throws {@link InvalidValue} on failure
    /// 3. The character must have a starting location. Throws {@link InvalidValue} on failure
    private void checkUserPersonaInvariants(@NonNull EntityDataPayload<SessionsRecord> data) {
        CharactersRecord userCharacter = characterService.find(
                EntityKey.<CharactersRecord>builder()
                        .set(CHARACTERS.ID, data.requireNonNull(SESSIONS.USER_PERSONA_ID))
                        .set(CHARACTERS.WORLD_ID, data.requireNonNull(SESSIONS.WORLD_ID))
                        .build()
        ).orElseThrow(
                "Couldn't find user character for session: " + data.getAssignment(SESSIONS.NAME).orElse("no session name"),
                Severity.USER
        );

        if (!userCharacter.getCanBeUser()) throw new InvalidValue("Picked character can't be user");
        if (userCharacter.getStartingLocationId() == null)
            throw new InvalidValue("This character has no starting locations in this world");
    }

    @TransactionalEventListener
    void onDeleteMessage(CRUDCommittedEvent.@NotNull DeletedEntity<?> rawEvent) {
        if (rawEvent.isNotEventOf(MESSAGES)) return;

        CRUDCommittedEvent.DeletedEntity<MessagesRecord> event = (CRUDCommittedEvent.DeletedEntity<MessagesRecord>) rawEvent;
        log.debug("Message deleted, updating tick num");
        if (!store.decrementTickNum(event.key().requireNonNull(MESSAGES.SESSION_ID))){
            log.error("Couldn't decrement session tick after message deletion");
            throw new IllegalStateException("Couldn't decrement session tick after message deletion");
        }
    }
}
