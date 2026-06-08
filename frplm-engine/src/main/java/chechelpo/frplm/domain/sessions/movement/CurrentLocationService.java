package chechelpo.frplm.domain.sessions.movement;

import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.domain.character.core.CharacterService;
import chechelpo.frplm.domain.character.starting_locations.StartingLocationsService;
import chechelpo.frplm.domain.sessions.core.SessionService;
import chechelpo.frplm.domain.sessions.messages.core.MessageService;
import chechelpo.frplm.domain.world.edge.EdgeService;
import chechelpo.frplm.domain.world.location.LocationsService;
import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.events.crud.CRUDCommittedEvent;
import chechelpo.frplm.exceptions.Severity;
import chechelpo.frplm.exceptions.runtime.InvalidValue;
import chechelpo.frplm.exceptions.runtime.EntityNotFound;
import chechelpo.frplm.exceptions.runtime.UnexpectedException;
import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.core.entities.pseudo_services.EntityService;
import chechelpo.frplm.jooq.generated.tables.records.*;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static chechelpo.frplm.domain.sessions.messages.core.MessageService.FIRST_MESSAGE_TICK_NUM;
import static chechelpo.frplm.jooq.generated.Tables.*;

@Service
public class CurrentLocationService extends EntityService<CurrentLocationsRecord, CurrentLocationStore> {
    private final StartingLocationsService startingLocations;
    private final EdgeService neighbours;
    private final MovementService movementService;
    private final LocationsService locationsService;
    private final CharacterService characterService;
    private final MessageService messageService;
    private final SessionService sessionService;

    CurrentLocationService(
            @NotNull CurrentLocationStore store,
            @NotNull LocationsService locationsService,
            @NotNull EventBus eventBus,
            @NotNull StartingLocationsService startingLocations,
            @NotNull EdgeService neighbours,
            @NotNull MovementService movements,
            CharacterService characterService, MessageService messageService, SessionService sessionService) {
        super(store, eventBus);
        this.neighbours = neighbours;
        this.startingLocations = startingLocations;
        this.movementService = movements;
        this.locationsService = locationsService;
        this.characterService = characterService;
        this.messageService = messageService;
        this.sessionService = sessionService;
    }

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
        EntityKey<CurrentLocationsRecord> key = EntityKey.<CurrentLocationsRecord>builder()
                .set(CURRENT_LOCATIONS.CHARACTER_ID, character.getId())
                .set(CURRENT_LOCATIONS.SESSION_ID, session.getId())
                .build();

        List<CurrentLocationsRecord> records = store.getAllMatching(key);
        if (records.isEmpty()) {
            log.error("No current location for character {} in session {}", character, session);
            throw new EntityNotFound("Character has no active location", Severity.USER);
        }
        if (records.size() > 1)
            log.warn("Multiple current locations for character {} in session {}", character, session);

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

        previous = this.find(target)
                .orElseThrow(() -> {
                    log.error("No previous location for character {} in session {}", target, target.requireValue(CURRENT_LOCATIONS.SESSION_ID));
                    return new UnexpectedException("This character has no previous location", Severity.SYSTEM);
                });

        EntityKey<LocationsRecord> previousLocation = EntityKey.<LocationsRecord>builder()
                        .set(LOCATIONS.WORLD_ID, previous.getWorldId())
                        .set(LOCATIONS.ID, previous.getLocationId())
                        .build();
        EntityKey<LocationsRecord> nextLocation = EntityKey.<LocationsRecord>builder()
                        .set(LOCATIONS.WORLD_ID, data.requireValue(CURRENT_LOCATIONS.WORLD_ID))
                        .set(LOCATIONS.ID, data.requireValue(CURRENT_LOCATIONS.LOCATION_ID))
                        .build();

        if (!neighbours.isNeighbour(previousLocation, nextLocation)){
            log.error("Location {} and {} are not neighbours", previousLocation, nextLocation);
            throw new InvalidValue("Location " + previousLocation + " and " + nextLocation + " are not neighbours");
        }
        int sessionID = target.requireValue(CURRENT_LOCATIONS.SESSION_ID);

        data.set(CURRENT_LOCATIONS.TICK_NUM, messageService.getLastOf(sessionID).getTickNum());

        movementService.registerMovementChange(target, data);
        super.beforeUpdate(target, data, operationID);
    }

    @Override
    protected void afterSuccessfulUpdate(EntityKey<CurrentLocationsRecord> key, EntityDataPayload<CurrentLocationsRecord> updated, long operationID) {
        if (updated.assignsField(CURRENT_LOCATIONS.LOCATION_ID) && updated.assignsField(CURRENT_LOCATIONS.TICK_NUM)) {
            if (key.requireValue(CURRENT_LOCATIONS.CHARACTER_ID) ==
                    sessionService.getUserCharacterID(EntityKey.of(SESSIONS.ID, key.requireValue(CURRENT_LOCATIONS.SESSION_ID))).orElseThrow()){
                boolean success = messageService.update(
                        EntityKey.<MessagesRecord>builder()
                                .set(MESSAGES.SESSION_ID, key.requireValue(CURRENT_LOCATIONS.SESSION_ID))
                                .set(MESSAGES.TICK_NUM, updated.requireValue(CURRENT_LOCATIONS.TICK_NUM))
                                .build()
                        ,
                        EntityDataPayload.of(MESSAGES.LOCATION_ID, updated.requireValue(CURRENT_LOCATIONS.LOCATION_ID))
                );
                if (!success)
                    throw new UnexpectedException("Error when updating location of message", Severity.SYSTEM);
            }

        }
        super.afterSuccessfulUpdate(key, updated, operationID);
    }

    /**
     * Registers starting locations for all characters.
     * @implSpec Session must be created before this runs, but is a necessary side effect.
     * That's why it's a listener and not in beforeUpdate
     */
    @EventListener
    void registerStartingLocations(CRUDCommittedEvent.@NotNull CreatedEntity<?> rawEvent){
        if (rawEvent.type() != EntityTypes.Types.MESSAGES) return;

        CRUDCommittedEvent.CreatedEntity<MessagesRecord> creationEvent =
                (CRUDCommittedEvent.CreatedEntity<MessagesRecord>) rawEvent;

        if (creationEvent.record().getTickNum() != FIRST_MESSAGE_TICK_NUM) return;

        log.debug("Registering starting locations for session id {}", creationEvent.key());

        MessagesRecord message = creationEvent.record();
        List<StartingLocationsRecord> startings = startingLocations.getMatching(
                EntityKey.of(STARTING_LOCATIONS.WORLD_ID, message.getWorldId())
        );
        log.trace("Starting locations \n{}", startings);

        EntityDataPayload<CurrentLocationsRecord> toInsert;
        for (StartingLocationsRecord startingLocation : startings) {
            try{
                toInsert = getFirstBuilt(startingLocation, message);
                this.unsafeCreate(toInsert);

            } catch (Exception e){
                log.error("Error while inserting starting locations for session id {}, deleting it", creationEvent.key(), e);
                return;
            }
        }
    }

    @Contract("_, _ -> new")
    private static @NotNull EntityDataPayload<CurrentLocationsRecord> getFirstBuilt(
            @NotNull StartingLocationsRecord startingLocation,
            @NotNull MessagesRecord record) {
        return EntityDataPayload.<CurrentLocationsRecord>builder()
                .set(CURRENT_LOCATIONS.SESSION_ID, record.getSessionId())
                .set(CURRENT_LOCATIONS.CHARACTER_ID, startingLocation.getCharacterId())
                .set(CURRENT_LOCATIONS.TICK_NUM, record.getTickNum())
                .set(CURRENT_LOCATIONS.WORLD_ID, record.getWorldId())
                .set(CURRENT_LOCATIONS.LOCATION_ID, startingLocation.getLocationId())
                .build();
    }
}
