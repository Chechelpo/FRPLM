package io.github.chechelpo.frplm.domain.sessions.movement;

import io.github.chechelpo.frplm.core.entities.pseudo_services.FieldValidator;
import io.github.chechelpo.frplm.domain.character.core.CharacterService;
import io.github.chechelpo.frplm.domain.sessions.core.SessionService;
import io.github.chechelpo.frplm.domain.sessions.messages.MessageService;
import io.github.chechelpo.frplm.domain.world.location.LocationsService;
import io.github.chechelpo.frplm.events.EventBus;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.InvalidValue;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.exceptions.runtime.UnexpectedException;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.*;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

import static io.github.chechelpo.frplm.jooq.generated.Tables.*;

@Service
class CurrentLocationService extends EntityService<CurrentLocationsRecord, CurrentLocationStore> {
    private final MovementHistory movementService;
    private final LocationsService locationsService;
    private final CharacterService characterService;
    private final MessageService messageService;
    private final SessionService sessionService;

    CurrentLocationService(
            @NotNull CurrentLocationStore store,
            @NotNull LocationsService locationsService,
            FieldValidator<CurrentLocationsRecord> validator,
            @NotNull EventBus eventBus,
            @NotNull MovementHistory movements,
            CharacterService characterService, MessageService messageService, SessionService sessionService) {
        super(store, validator, eventBus);
        this.movementService = movements;
        this.locationsService = locationsService;
        this.characterService = characterService;
        this.messageService = messageService;
        this.sessionService = sessionService;
    }

    public void rollbackLocationsTo(int sessionID, int tick){
        store.rollbackSessionTo(sessionID, tick);
    }
    public void rollbackLocationsToBefore(int sessionID, int tick){store.rollbackLocationsToBefore(sessionID, tick);}

    public CharactersRecord[] getAtLocation(int sessionID, int locationID) {
        List<CurrentLocationsRecord> records = store.getAtLocation(sessionID, locationID);
        log.debug(records.toString());
        return characterService.getCharacters(records);
    }

    @Transactional(readOnly = true)
    public CharactersRecord[] getAtLocation(
            @NotNull LocationsRecord location,
            @NotNull SessionsRecord session
    ){
        List<CurrentLocationsRecord> records = store.getAtLocation(session.getId(), location.getId());
        return characterService.getCharacters(records);
    }


    @Transactional(readOnly = true)
    @CheckReturnValue
    public @NotNull LocationsRecord getLocationOf(@NotNull CharactersRecord character, @NotNull SessionsRecord session) throws EntityNotFound {
        return getLocationOf(character.getId(), session.getId());
    }

    @Transactional(readOnly = true)
    @CheckReturnValue
    public @NotNull LocationsRecord getLocationOf(int characterId, int sessionId) throws EntityNotFound {
        EntityKey<CurrentLocationsRecord> key = EntityKey.<CurrentLocationsRecord>builder()
                .set(CURRENT_LOCATIONS.CHARACTER_ID, characterId)
                .set(CURRENT_LOCATIONS.SESSION_ID, sessionId)
                .build();

        List<CurrentLocationsRecord> records = store.getAllMatching(key);
        if (records.isEmpty()) {
            String characterName = characterService.find(EntityKey.of(CHARACTERS.ID, characterId))
                    .orElseThrow("No character this id when fetching location at session id " + sessionId)
                    .getName();
            log.error("No current location for character {} in session {}", characterName, sessionId);
            throw new EntityNotFound("Character has no active location", Severity.USER);
        }
        if (records.size() > 1)
            log.warn("Multiple current locations for character {} in session {}", characterId, sessionId);

        return locationsService.getLocationBy(records.getFirst());
    }


    /** @implNote check the movement is valid */
    @Override
    protected void beforeUpdate(
            @NotNull EntityKey<CurrentLocationsRecord> target,
            @NotNull EntityDataPayload<CurrentLocationsRecord> data,
            long operationID
    ) {
        CurrentLocationsRecord previous;

        //noinspection SpringTransactionalMethodCallsInspection
        previous = this.find(target)
                .orElseThrow( notFound -> {
                    log.error("No previous location for character {} in session {}", target, target.require(CURRENT_LOCATIONS.SESSION_ID));
                    return new UnexpectedException("This character has no previous location", Severity.SYSTEM);
                });

        EntityKey<LocationsRecord> previousLocation = EntityKey.<LocationsRecord>builder()
                        .set(LOCATIONS.WORLD_ID, previous.getWorldId())
                        .set(LOCATIONS.ID, previous.getLocationId())
                        .build();
        EntityKey<LocationsRecord> nextLocation = EntityKey.<LocationsRecord>builder()
                        .set(LOCATIONS.WORLD_ID, data.require(CURRENT_LOCATIONS.WORLD_ID))
                        .set(LOCATIONS.ID, data.require(CURRENT_LOCATIONS.LOCATION_ID))
                        .build();

        if (!Objects.equals(previous.getWorldId(), data.require(CURRENT_LOCATIONS.WORLD_ID)))
            throw new InvalidValue("Previous world id mismatch");

        if (nextLocation.require(LOCATIONS.ID) == previousLocation.require(LOCATIONS.ID))
            data.set(CURRENT_LOCATIONS.TICK_NUM, previous.getTickNum());
        else { //This is a legitimate movement
            int sessionID = target.require(CURRENT_LOCATIONS.SESSION_ID);
            data.set(CURRENT_LOCATIONS.TICK_NUM, messageService.getLastMessageOf(sessionID).getTickNum());
            movementService.registerMovementChange(
                    previous,
                    data.require(CURRENT_LOCATIONS.TICK_NUM)
            );
        }

        super.beforeUpdate(target, data, operationID);
    }

    @Override
    protected void afterSuccessfulUpdate(@NotNull EntityKey<CurrentLocationsRecord> key, @NotNull EntityDataPayload<CurrentLocationsRecord> updated, long operationID) {
        int movedCharacterId = key.require(CURRENT_LOCATIONS.CHARACTER_ID);
        int sessionID = key.require(CURRENT_LOCATIONS.SESSION_ID);
        boolean userCharacterMovement = movedCharacterId == sessionService.getUserCharacterID(sessionID).orElseThrow();

        // If the movement is from user character, we also update the message location.
        if (updated.assigns(CURRENT_LOCATIONS.LOCATION_ID) && updated.assigns(CURRENT_LOCATIONS.TICK_NUM)) {
            if (userCharacterMovement){
                messageService.update(
                        EntityKey.<MessagesRecord>builder()
                                .set(MESSAGES.SESSION_ID, key.require(CURRENT_LOCATIONS.SESSION_ID))
                                .set(MESSAGES.TICK_NUM, updated.require(CURRENT_LOCATIONS.TICK_NUM))
                                .build()
                        ,
                        EntityDataPayload.of(MESSAGES.LOCATION_ID, updated.require(CURRENT_LOCATIONS.LOCATION_ID))
                ).orElseThrow("Couldn't update message location on user movement", Severity.SYSTEM);
            }

        }
        super.afterSuccessfulUpdate(key, updated, operationID);
    }
}
