package chechelpo.frplm.domain.sessions.movement;

import ch.qos.logback.classic.Logger;
import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.domain.character.core.CharacterService;
import chechelpo.frplm.domain.sessions.core.SessionService;
import chechelpo.frplm.domain.sessions.messages.MessageService;
import chechelpo.frplm.domain.world.edge.EdgeService;
import chechelpo.frplm.exceptions.Severity;
import chechelpo.frplm.exceptions.runtime.EntityNotFound;
import chechelpo.frplm.exceptions.runtime.UnsupportedAction;
import chechelpo.frplm.jooq.generated.tables.records.*;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static chechelpo.frplm.jooq.generated.Tables.*;

@Component
public class Movements {
    private static final Logger log = (Logger) LoggerFactory.getLogger(Movements.class);

    private final CurrentLocationService currentLocationService;
    private final ResponseMovementService responseMovementService;
    private final MessageService messageService;
    private final SessionService sessionService;
    private final CharacterService characterService;
    private final EdgeService edgeService;

    Movements(
            CurrentLocationService currentLocationService,
            ResponseMovementService responseMovementService,
            MessageService messageService,
            SessionService sessionService,
            CharacterService characterService, EdgeService edgeService) {
        this.currentLocationService = currentLocationService;
        this.responseMovementService = responseMovementService;
        this.messageService = messageService;
        this.sessionService = sessionService;
        this.characterService = characterService;
        this.edgeService = edgeService;
    }

    public void rollbackLocationsTo(int sessionId, int tickNumber) {
        currentLocationService.rollbackLocationsTo(sessionId, tickNumber);
    }

    /**
     * Checks:
     * <pre>
     *     1. This message is assistant generated
     *     2. The next location is a direct neighbour
     * </pre>
     */
    public boolean move(int sessionId, int characterId, int toLocationId) {
        LocationsRecord previousLocation = getLocationOf(
                characterService.find(EntityKey.of(CHARACTERS.ID, characterId))
                        .orElseThrow(() -> new EntityNotFound("No character with id " + characterId, Severity.EXPECTED)),
                sessionService.find(EntityKey.of(SESSIONS.ID, sessionId))
                        .orElseThrow(() -> new EntityNotFound("No session with id " + sessionId, Severity.EXPECTED))
        );
        MessagesRecord lastMessage = messageService.getLastOf(sessionId);
        if (messageService.isFirstMessage(lastMessage)) return false; //Can't move in first message

        LocationEdgesRecord locationEdgesRecord = edgeService.find(
                EntityKey.<LocationEdgesRecord>builder()
                        .set(LOCATION_EDGES.WORLD_ID, previousLocation.getWorldId())
                        .set(LOCATION_EDGES.FROM_LOCATION_ID, previousLocation.getId())
                        .set(LOCATION_EDGES.TO_LOCATION_ID, toLocationId)
                        .build()
        ).orElseThrow(() -> new EntityNotFound(
                "Edge from location \n%s\n to location id %s does not exist".formatted(previousLocation, toLocationId),
                Severity.USER
        ));

        if (!locationEdgesRecord.getTraversable())
            throw new UnsupportedAction(
                    "Edge from \n%s\n to \n%s\n is NOT traversable".formatted(previousLocation.getName(), locationEdgesRecord),
                    Severity.EXPECTED
            );

        ResponseLocationChangesRecord changesRecord = responseMovementService.moveInCurrentResponse(sessionId, characterId, toLocationId);
        currentLocationService.update(EntityKey.<CurrentLocationsRecord>builder()
                        .set(CURRENT_LOCATIONS.SESSION_ID, changesRecord.getSessionId())
                        .set(CURRENT_LOCATIONS.CHARACTER_ID, characterId)
                .build(),
                EntityDataPayload.<CurrentLocationsRecord>builder()
                        .set(CURRENT_LOCATIONS.WORLD_ID, previousLocation.getWorldId())
                        .set(CURRENT_LOCATIONS.LOCATION_ID, toLocationId)
                        .set(CURRENT_LOCATIONS.TICK_NUM, changesRecord.getTickNum())
                        .build()
                );

        return true;
    }
    private EntityDataPayload<CurrentLocationsRecord> from(ResponseLocationChangesRecord changesRecord) {
        return EntityDataPayload.<CurrentLocationsRecord>builder()
                .set(CURRENT_LOCATIONS.SESSION_ID, changesRecord.getSessionId())
                .set(CURRENT_LOCATIONS.TICK_NUM , changesRecord.getTickNum())

                .set(CURRENT_LOCATIONS.CHARACTER_ID, changesRecord.getCharacterId())

                .set(CURRENT_LOCATIONS.WORLD_ID, changesRecord.getWorldId())
                .set(CURRENT_LOCATIONS.LOCATION_ID, changesRecord.getLocationId())
                .build();
    }

    void rollbackLastResponseChanges(int sessionId){
        MessagesRecord lastMessage = messageService.getLastOf(sessionId);

    }

    public CharactersRecord[] getAtLocation(int sessionID, int locationID) {
        return currentLocationService.getAtLocation(sessionID, locationID);
    }
    public CharactersRecord[] getAtLocation(@NotNull LocationsRecord location, @NotNull SessionsRecord session){
        return currentLocationService.getAtLocation(location, session);
    }
    @CheckReturnValue
    public @NotNull LocationsRecord getLocationOf(@NotNull CharactersRecord character, @NotNull SessionsRecord session) throws EntityNotFound {
        return currentLocationService.getLocationOf(character, session);
    }
    @CheckReturnValue
    public @NotNull LocationsRecord getLocationOf(@NotNull int  characterId, @NotNull int sessionId) throws EntityNotFound {
        return currentLocationService.getLocationOf(characterId, sessionId);
    }
}
