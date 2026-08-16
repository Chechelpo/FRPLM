package io.github.chechelpo.frplm.domain.sessions.session_characters;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.core.entities.fields.FieldValidator;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityReader;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityService;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookService;
import io.github.chechelpo.frplm.domain.sessions.core.SessionService;
import io.github.chechelpo.frplm.domain.world.edge.EdgeService;
import io.github.chechelpo.frplm.events.EventBus;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.UnsupportedAction;
import io.github.chechelpo.frplm.jooq.generated.tables.records.*;
import org.jetbrains.annotations.NotNull;
import org.jooq.Result;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import javax.swing.text.html.parser.Entity;
import java.util.List;
import java.util.Objects;

import static io.github.chechelpo.frplm.jooq.generated.Tables.*;

@Service
public class SessionCharacterService extends EntityService<SessionCharactersRecord, SessionCharacterStore> {
    private final LorebookService lorebookService;
    private final EntityReader<CharactersRecord> characterReader;
    private final SessionService sessionService;
    private final EdgeService edgeService;

    SessionCharacterService(
            @NonNull SessionCharacterStore store,
            FieldValidator<SessionCharactersRecord> validator,
            @NotNull EventBus eventBus,
            LorebookService lorebookService,
            EntityReader<CharactersRecord> characterReader,
            SessionService sessionService,
            EdgeService edgeService
    ) {
        super(store, validator, eventBus);
        this.lorebookService = lorebookService;
        this.characterReader = characterReader;
        this.sessionService = sessionService;
        this.edgeService = edgeService;
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // UTILS
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    public SessionCharactersRecord getCharacterOf(int sessionId, CharactersRecord characterRecord){
        return this.getOneMatching(
                EntityDataPayload.<SessionCharactersRecord>builder()
                        .set(SESSION_CHARACTERS.PERMANENT_CHARACTER_ID, characterRecord.getId())
                        .set(SESSION_CHARACTERS.WORLD_ID, characterRecord.getWorldId())
                        .set(SESSION_CHARACTERS.SESSION_ID, sessionId)
                        .build()
        ).resolve();
    }

    public Result<SessionCharactersRecord> instancesOf(CharactersRecord charactersRecord){
        return this.getMatching(
                EntityDataPayload.<SessionCharactersRecord>builder()
                        .set(SESSION_CHARACTERS.WORLD_ID, CHARACTERS.WORLD_ID, charactersRecord)
                        .set(SESSION_CHARACTERS.PERMANENT_CHARACTER_ID, CHARACTERS.ID, charactersRecord)
                        .build()
        );
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // CREATION
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    @Override
    protected void beforeCreate(EntityDataPayload<SessionCharactersRecord> data, long operationID) {
        data.getAssignment(SESSION_CHARACTERS.PERMANENT_CHARACTER_ID)
                .ifUnassignedRun(() ->
                        data.requireAssignments(
                                List.of(SESSION_CHARACTERS.NAME, SESSION_CHARACTERS.DESCRIPTION, SESSION_CHARACTERS.CURRENT_LOCATION_ID),
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

        data.ifUnassignedGet(
                SESSION_CHARACTERS.ID,
                () -> sessionService.incrementAndGet(
                        SESSIONS.NEXT_CHARACTER_ID,
                        EntityKey.of(SESSIONS.ID, data.requireNonNull(SESSION_CHARACTERS.SESSION_ID))
                ).orElseThrow()
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
                                                () -> sessionService.getNonNullValueOf(
                                                        SESSIONS.WORLD_ID,
                                                        EntityKey.of(SESSIONS.ID, data.requireNonNull(SESSION_CHARACTERS.SESSION_ID))
                                                )
                                        )
                        ).build()
        ).orElseThrow("Couldn't find permanent character", Severity.SYSTEM);

        data
                .set(SESSION_CHARACTERS.NAME, record.getName())
                .set(SESSION_CHARACTERS.CURRENT_LOCATION_ID, record.getStartingLocationId())
                .set(SESSION_CHARACTERS.DESCRIPTION, record.getDescription());
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // UPDATE
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    @Override
    protected void beforeUpdate(@NotNull EntityKey<SessionCharactersRecord> target, EntityDataPayload<SessionCharactersRecord> data, long operationID) {
        super.beforeUpdate(target, data, operationID);

        data.getAssignment(SESSION_CHARACTERS.CURRENT_LOCATION_ID)
                .ifAssigned(
                        newLocationId -> {
                            SessionCharactersRecord previousCharacter = this.require(target);
                            if (Objects.equals(newLocationId, previousCharacter.getCurrentLocationId()))
                                return;

                            var findResult = edgeService.find(
                                    EntityKey.<LocationEdgesRecord>builder()
                                            .set(LOCATION_EDGES.WORLD_ID, previousCharacter.getWorldId())
                                            .set(LOCATION_EDGES.FROM_LOCATION_ID, previousCharacter.getCurrentLocationId())
                                            .set(LOCATION_EDGES.TO_LOCATION_ID, newLocationId)
                                            .build()
                            ).orElseThrow(
                                    "Edge from location id %s to %s does not exist".formatted(previousCharacter.getCurrentLocationId(), newLocationId),
                                    Severity.SYSTEM
                            );

                            if (!findResult.getTraversable())
                                throw new UnsupportedAction(
                                        "Edge from location id %s to %s is non-traversable".formatted(previousCharacter.getCurrentLocationId(), newLocationId),
                                        Severity.USER
                                );
                        }
                );
    }

    @Override
    protected void afterSuccessfulUpdate(
            SessionCharactersRecord previousData,
            EntityKey<SessionCharactersRecord> key,
            EntityDataPayload<SessionCharactersRecord> updated,
            long operationID
    ) {
        updated.getAssignment(SESSION_CHARACTERS.KEEP_UPDATED)
                .ifAssigned(keepUpdated -> {
                    if (Boolean.TRUE.equals(keepUpdated)) {
                        goGetPermanentInfo(previousData, key, updated);
                    }
                });
        super.afterSuccessfulUpdate(previousData, key, updated, operationID);
    }

    private void goGetPermanentInfo(
            SessionCharactersRecord previousData,
            EntityKey<SessionCharactersRecord> key,
            EntityDataPayload<SessionCharactersRecord> newData
    ) {
        Integer permanentId = previousData.getPermanentCharacterId();
        if (newData.assigns(SESSION_CHARACTERS.PERMANENT_CHARACTER_ID))
            permanentId = newData.requireNonNull(SESSION_CHARACTERS.PERMANENT_CHARACTER_ID);

        if (permanentId == null) return;

        CharactersRecord permCharacter = characterReader.require(EntityKey.<CharactersRecord>builder()
                        .set(CHARACTERS.ID, permanentId)
                        .set(CHARACTERS.WORLD_ID, previousData.getWorldId())
                        .build()
        );

        update(
                key,
                EntityDataPayload.<SessionCharactersRecord>builder()
                        .set(SESSION_CHARACTERS.NAME, CHARACTERS.NAME, permCharacter)
                        .set(SESSION_CHARACTERS.DESCRIPTION, CHARACTERS.DESCRIPTION, permCharacter)
                        .build()
        ).orElseThrow();
    }
}