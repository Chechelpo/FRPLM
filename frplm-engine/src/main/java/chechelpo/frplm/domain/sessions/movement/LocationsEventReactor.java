package chechelpo.frplm.domain.sessions.movement;

import ch.qos.logback.classic.Logger;
import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.domain.character.starting_locations.StartingLocationsService;
import chechelpo.frplm.domain.sessions.core.SessionService;
import chechelpo.frplm.domain.sessions.messages.MessageService;
import chechelpo.frplm.events.crud.CRUDCommittedEvent;
import chechelpo.frplm.events.crud.CRUDDraftEvent;
import chechelpo.frplm.exceptions.Severity;
import chechelpo.frplm.exceptions.runtime.EntityNotFound;
import chechelpo.frplm.exceptions.runtime.UnexpectedException;
import chechelpo.frplm.jooq.generated.tables.records.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static chechelpo.frplm.domain.sessions.messages.MessageService.FIRST_MESSAGE_TICK_NUM;
import static chechelpo.frplm.jooq.generated.Tables.*;
import static chechelpo.frplm.jooq.generated.Tables.CURRENT_LOCATIONS;

/**
 * Class in charge of centralizing logic regarding events that should change/initialize/delete character locations.
 */
@Component
class LocationsEventReactor {
    private static final Logger log = (Logger) LoggerFactory.getLogger(LocationsEventReactor.class);
    private final CurrentLocationService currentLocationService;
    private final StartingLocationsService startingLocationsService;
    private final ResponseMovementService responseMovementService;
    private final SessionService sessionService;
    private final Movements movements;
    private final MessageService messageService;
    private final MovementService movementService;

    LocationsEventReactor(CurrentLocationService currentLocationService, StartingLocationsService startingLocationsService, ResponseMovementService responseMovementService, SessionService sessionService, Movements movements, MessageService messageService, MovementService movementService) {
        this.currentLocationService = currentLocationService;
        this.startingLocationsService = startingLocationsService;
        this.responseMovementService = responseMovementService;
        this.sessionService = sessionService;
        this.movements = movements;
        this.messageService = messageService;
        this.movementService = movementService;
    }

    @EventListener
    public void onMessageDeletionRewindLocations(CRUDDraftEvent.DeleteEntityDraft<?> rawEvent) {
        if (rawEvent.type() != EntityTypes.Types.MESSAGES) return;
        CRUDDraftEvent.DeleteEntityDraft<MessagesRecord> event = (CRUDDraftEvent.DeleteEntityDraft<MessagesRecord>) rawEvent;

        EntityKey<MessagesRecord> deleted = event.key();
        currentLocationService.rollbackLocationsToBefore(
                deleted.get(MESSAGES.SESSION_ID)
                        .orElseThrow(() -> new UnexpectedException("This message key has no sessionID", Severity.SYSTEM)),
                deleted.get(MESSAGES.TICK_NUM)
                        .orElseThrow(() -> new UnexpectedException("This message key has no tick_num", Severity.SYSTEM))
        );
    }

    /**
     * Registers starting locations for all characters.
     *
     * @implSpec Session must be created before this runs, but is a necessary side effect.
     * That's why it's a listener and not in beforeUpdate
     */
    @EventListener
    void onNewSessionRegisterStartingLocations(CRUDCommittedEvent.@NotNull CreatedEntity<?> rawEvent) {
        if (rawEvent.type() != EntityTypes.Types.MESSAGES) return;

        CRUDCommittedEvent.CreatedEntity<MessagesRecord> creationEvent =
                (CRUDCommittedEvent.CreatedEntity<MessagesRecord>) rawEvent;

        if (creationEvent.record().getTickNum() != FIRST_MESSAGE_TICK_NUM) return;

        log.debug("Registering starting locations for session id {}", creationEvent.key());

        MessagesRecord message = creationEvent.record();
        List<StartingLocationsRecord> startings = startingLocationsService.getMatching(
                EntityKey.of(STARTING_LOCATIONS.WORLD_ID, message.getWorldId())
        );
        log.trace("Starting locations \n{}", startings);

        EntityDataPayload<CurrentLocationsRecord> toInsert;
        for (StartingLocationsRecord startingLocation : startings) {
            try {
                toInsert = getFirstBuilt(startingLocation, message);
                currentLocationService.createAndGet(toInsert);

            } catch (Exception e) {
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


    @EventListener
    void onNewMessageInjectUserLocation(CRUDDraftEvent.CreateEntityDraft<?> rawEvent) {
        if (rawEvent.type() != EntityTypes.Types.MESSAGES) return;

        CRUDDraftEvent.CreateEntityDraft<MessagesRecord> event = (CRUDDraftEvent.CreateEntityDraft<MessagesRecord>) rawEvent;

        int sessionId = event.initialData().requireValue(MESSAGES.SESSION_ID);
        int userCharacterId = sessionService.getUserCharacterID(sessionId)
                .orElseThrow(() -> new UnexpectedException("User character not found", Severity.SYSTEM));

        LocationsRecord userCharacterLocation;
        if (messageService.isFirstMessage(event.initialData().requireValue(MESSAGES.TICK_NUM)))
            userCharacterLocation = getStartingLocationBySession(userCharacterId, sessionId);
        else userCharacterLocation = movements.getLocationOf(userCharacterId, sessionId);

        event.initialData().set(MESSAGES.WORLD_ID, userCharacterLocation.getWorldId());
        event.initialData().set(MESSAGES.LOCATION_ID, userCharacterLocation.getId());
    }

    private LocationsRecord getStartingLocationBySession(int userCharacterId, int sessionId) {
            return startingLocationsService.startingLocationAt(
                EntityKey.of(CHARACTERS.ID, userCharacterId),
                sessionService.getValueOf(SESSIONS.WORLD_ID, EntityKey.of(SESSIONS.ID, sessionId))
                        .orElseThrow(() -> new EntityNotFound("Could not find session with id " + sessionId, Severity.SYSTEM))
        ).getFirst();
    }

    /** Applies the current location of user character as this response location */
    @EventListener
    void onNewResponseRegisterLocation(CRUDDraftEvent.@NotNull CreateEntityDraft<?> rawEvent) {
        if (rawEvent.type() != EntityTypes.Types.RESPONSES) return;

        CRUDDraftEvent.CreateEntityDraft<ResponsesRecord> event = (CRUDDraftEvent.CreateEntityDraft<ResponsesRecord>) rawEvent;
        int sessionId = event.initialData().requireValue(RESPONSES.SESSION_ID);
        int tickNum = event.initialData().requireValue(RESPONSES.TICK_NUM);

        LocationsRecord userCharacterLocation;
        if (messageService.isFirstMessage(tickNum)){
            /*
            In charge of the system, ignore this one.
            int world_id = event.initialData().getValue(RESPONSES.WORLD_ID)
                    .orElseThrow(() -> new IllegalStateException("This event reactor needs world id to be supplied manually, as the session is not yet created"));
            userCharacterLocation = getStartingLocationByWorld(sessionId, world_id)
                    .orElseThrow(() -> new EntityNotFound("Could not fetch the starting location of user character", Severity.SYSTEM));*/
            return;
        } else userCharacterLocation = getCurrentUserLocation(sessionId);

        EntityDataPayload<ResponsesRecord> response = event.initialData();
        response.set(RESPONSES.WORLD_ID, userCharacterLocation.getWorldId());
        response.set(RESPONSES.LOCATION_ID, userCharacterLocation.getId());
    }

    private @NotNull LocationsRecord getCurrentUserLocation(int sessionId) {
        return movements.getLocationOf(
                sessionService.getUserCharacterID(sessionId)
                        .orElseThrow(() -> new UnexpectedException("Session with no user character", Severity.SYSTEM)),
                sessionId
        );
    }


    /**
     * <h3> Algorithm </h3>
     * <pre>
     *     1. Reacts to picking a new response.
     *     2. Destroys effects of previous responses of this message.
     *     3. Applies effects of this current response.
     * </pre>
     */
    @EventListener
    @Transactional
    void onActiveResponseChange(CRUDCommittedEvent.@NotNull UpdatedEntity<?> rawEvent) {
        if (rawEvent.type() != EntityTypes.Types.MESSAGES) return;

        CRUDCommittedEvent.UpdatedEntity<MessagesRecord> event = (CRUDCommittedEvent.UpdatedEntity<MessagesRecord>) rawEvent;
        if (!event.updatedData().assignsField(MESSAGES.ACTIVE_RESPONSE)) return;

        EntityKey<MessagesRecord> target = event.target();
        EntityDataPayload<MessagesRecord> data = event.updatedData();

        int sessionId = target.requireValue(MESSAGES.SESSION_ID);
        int tickNum = target.requireValue(MESSAGES.TICK_NUM);
        short newActiveResponseNum = data.requireValue(MESSAGES.ACTIVE_RESPONSE);
        ResponsesRecord newActiveResponse = messageService.getActiveResponseOf(target);

        //onResponseChangeMessageLocation(newActiveResponse, event.updatedData());

        movementService.rollbackLatestMovementsOf(sessionId, tickNum);
        List<ResponseLocationChangesRecord> newLocations = responseMovementService.getResponseMovements(
                sessionId, tickNum, newActiveResponseNum
        );
        newLocations.forEach(newLoc ->
                currentLocationService.update(
                        EntityKey.<CurrentLocationsRecord>builder()
                                .set(CURRENT_LOCATIONS.SESSION_ID, sessionId)
                                .set(CURRENT_LOCATIONS.CHARACTER_ID, newLoc.getCharacterId())
                                .build(),
                        EntityDataPayload.<CurrentLocationsRecord>builder()
                        .set(CURRENT_LOCATIONS.TICK_NUM, tickNum)

                        .set(CURRENT_LOCATIONS.WORLD_ID, newLoc.getWorldId())
                        .set(CURRENT_LOCATIONS.LOCATION_ID, newLoc.getLocationId())
                        .build()
                )
        );
    }
    /** Assigns the new active response location as the new message location */
    void onResponseChangeMessageLocation(ResponsesRecord newActiveResponse, EntityDataPayload<MessagesRecord> toUpdate) {
        toUpdate.set(MESSAGES.WORLD_ID, newActiveResponse.getWorldId());
        toUpdate.set(MESSAGES.LOCATION_ID, newActiveResponse.getLocationId());
        log.debug("Changed current location for message {}", toUpdate);
    }

    void applyLocationChangesOfResponse(int sessionId, int tick_num, int response_num){

    }

}
