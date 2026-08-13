package io.github.chechelpo.frplm.domain.sessions.session_characters;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.domain.character.core.CharacterService;
import io.github.chechelpo.frplm.events.crud.CRUDCommittedEvent;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.SessionCharactersRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.*;

@Component
public class SessionCharacterEvents {

    private final CharacterService characterService;
    private final SessionCharacterService sessionCharacterService;

    public SessionCharacterEvents(CharacterService characterService, SessionCharacterService sessionCharacterService) {
        this.characterService = characterService;
        this.sessionCharacterService = sessionCharacterService;
    }

    @EventListener
    public void onSessionCreationCreateCharacters(CRUDCommittedEvent.CreatedEntity<?> rawEvent) {
        if (rawEvent.isNotEventOf(SESSIONS)) return;
        //noinspection unchecked
        onSessionCreation(
                (CRUDCommittedEvent.CreatedEntity<SessionsRecord>) rawEvent
        );
    }

    private void onSessionCreation(CRUDCommittedEvent.CreatedEntity<SessionsRecord> event) {
        SessionsRecord newSession = event.record();

        List<CharactersRecord> characters = characterService.getMatching(
                EntityDataPayload.of(CHARACTERS.WORLD_ID, newSession.getWorldId())
        );

        characters.forEach(
                character -> sessionCharacterService.createAndGet(
                        EntityDataPayload.<SessionCharactersRecord>builder()
                                .set(SESSION_CHARACTERS.SESSION_ID, SESSIONS.ID, newSession)
                                .set(SESSION_CHARACTERS.PERMANENT_CHARACTER_ID, CHARACTERS.ID, character)
                                .set(SESSION_CHARACTERS.WORLD_ID, CHARACTERS.WORLD_ID, character)
                                .set(SESSION_CHARACTERS.NAME, CHARACTERS.NAME, character)
                                .set(SESSION_CHARACTERS.DESCRIPTION, CHARACTERS.DESCRIPTION, character)
                                .set(SESSION_CHARACTERS.CURRENT_LOCATION_ID, CHARACTERS.STARTING_LOCATION_ID, character)
                                .build()
                )
        );
    }

    @EventListener
    public void onPermanentUpdate(CRUDCommittedEvent.UpdatedEntity<?> rawEvent) {
        if (rawEvent.isNotEventOf(CHARACTERS)) return;

        CRUDCommittedEvent.UpdatedEntity<CharactersRecord> event = (CRUDCommittedEvent.UpdatedEntity<CharactersRecord>) rawEvent;
        EntityDataPayload<CharactersRecord> data = event.updatedData();
        if (!data.assignsAny(List.of(CHARACTERS.NAME, CHARACTERS.DESCRIPTION))) return;

        int worldId = event.previousData().getWorldId();
        int characterId = event.target().requireNonNull(CHARACTERS.ID);

        sessionCharacterService.getMatching(
                EntityDataPayload.<SessionCharactersRecord>builder()
                        .set(SESSION_CHARACTERS.WORLD_ID, worldId)
                        .set(SESSION_CHARACTERS.PERMANENT_CHARACTER_ID, characterId)
                        .build()
        ).forEach(
                sessionCharacter -> {
                    if (sessionCharacter.getKeepUpdated())
                        sessionCharacterService.update(
                                sessionCharacterService.keyOf(sessionCharacter),
                                EntityDataPayload.<SessionCharactersRecord>builder()
                                        .copyIfAssigned(SESSION_CHARACTERS.NAME, CHARACTERS.NAME, data)
                                        .copyIfAssigned(SESSION_CHARACTERS.DESCRIPTION, CHARACTERS.DESCRIPTION, data)
                                        .build()
                        );
                }
        );
    }
}
