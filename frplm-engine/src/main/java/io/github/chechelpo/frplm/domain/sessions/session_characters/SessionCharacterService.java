package io.github.chechelpo.frplm.domain.sessions.session_characters;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.core.entities.fields.FieldValidator;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityReader;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityService;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookService;
import io.github.chechelpo.frplm.events.EventBus;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.MovementsRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.SessionCharactersRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.*;

@Service
public class SessionCharacterService extends EntityService<SessionCharactersRecord, SessionCharacterStore> {
    private final LorebookService lorebookService;
    private final EntityReader<CharactersRecord> characterReader;
    private final EntityReader<SessionsRecord> sessionReader;

    SessionCharacterService(
            @NonNull SessionCharacterStore store,
            FieldValidator<SessionCharactersRecord> validator,
            @NotNull EventBus eventBus,
            LorebookService lorebookService,
            EntityReader<CharactersRecord> characterReader,
            EntityReader<SessionsRecord> sessionReader
    ) {
        super(store, validator, eventBus);
        this.lorebookService = lorebookService;
        this.characterReader = characterReader;
        this.sessionReader = sessionReader;
    }

    @Override
    protected void beforeCreate(EntityDataPayload<SessionCharactersRecord> data, long operationID) {
        data.getAssignment(SESSION_CHARACTERS.PERMANENT_CHARACTER_ID)
                .ifUnassignedRun(() ->
                        data.requireAssignments(List.of(SESSION_CHARACTERS.NAME, SESSION_CHARACTERS.DESCRIPTION),
                                true
                        ))
                .ifAssigned(characterId -> assignPermanentCharacterData(characterId, data));

        data.ifUnassignedGet(
                SESSION_CHARACTERS.SESSION_LOREBOOK_ID,
                () -> lorebookService.createAndGet(
                        EntityDataPayload.of(LOREBOOKS.NAME, data.requireNonNull(SESSION_CHARACTERS.NAME)),
                        LOREBOOKS.ID
                )
        );

        super.beforeCreate(data, operationID);
    }

    private void assignPermanentCharacterData(int characterId, EntityDataPayload<SessionCharactersRecord> data) {
        CharactersRecord record = characterReader.find(
                EntityKey.<CharactersRecord>builder()
                        .set(CHARACTERS.ID, characterId)
                        .set(
                                CHARACTERS.WORLD_ID,
                                data.getAssignment(SESSION_CHARACTERS.WORLD_ID)
                                        .orElseGet(
                                                () -> sessionReader.getNonNullValueOf(
                                                        SESSIONS.WORLD_ID,
                                                        EntityKey.of(SESSIONS.ID, data.requireNonNull(SESSION_CHARACTERS.SESSION_ID))
                                                )
                                        )
                        ).build()
        ).orElseThrow("Couldn't find permanent character", Severity.SYSTEM);

        data.set(SESSION_CHARACTERS.NAME, record.getName());
        data.set(SESSION_CHARACTERS.DESCRIPTION, record.getDescription());
    }
}