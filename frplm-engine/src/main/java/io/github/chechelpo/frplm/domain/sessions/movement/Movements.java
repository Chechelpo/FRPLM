package io.github.chechelpo.frplm.domain.sessions.movement;

import ch.qos.logback.classic.Logger;
import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.domain.character.core.CharacterService;
import io.github.chechelpo.frplm.domain.sessions.core.SessionService;
import io.github.chechelpo.frplm.domain.sessions.messages.MessageService;
import io.github.chechelpo.frplm.domain.world.edge.EdgeService;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.exceptions.runtime.UnsupportedAction;
import io.github.chechelpo.frplm.jooq.generated.tables.records.*;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static io.github.chechelpo.frplm.jooq.generated.Tables.*;

@Component
public class Movements {
    private static final Logger log = (Logger) LoggerFactory.getLogger(Movements.class);
    private final CurrentLocationService currentLocationService;
    private final ResponseMovementService responseMovementService;
    private final MessageService messageService;
    private final SessionService sessionService;
    private final CharacterService characterService;
    private final EdgeService edgeService;
    private final MovementHistory movementHistory;

    Movements(
            CurrentLocationService currentLocationService,
            ResponseMovementService responseMovementService,
            MessageService messageService,
            SessionService sessionService,
            CharacterService characterService,
            EdgeService edgeService, MovementHistory movementHistory) {
        this.currentLocationService = currentLocationService;
        this.responseMovementService = responseMovementService;
        this.messageService = messageService;
        this.sessionService = sessionService;
        this.characterService = characterService;
        this.edgeService = edgeService;
        this.movementHistory = movementHistory;
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
        CharactersRecord character = requireCharacter(characterId);
        SessionsRecord session = requireSession(sessionId);
        LocationsRecord previousLocation = getLocationOf(character, session);

        if (previousLocation.getId().equals(toLocationId)) return false;

        MessagesRecord lastMessage = messageService.getLastMessageOf(sessionId);

        if (messageService.isFirstMessage(lastMessage)) return false; //Can't move in first message

        LocationEdgesRecord edge = requireEdge(previousLocation, toLocationId);
        assertTraversable(previousLocation, edge);

        applyMove(sessionId, characterId, toLocationId, previousLocation);

        return true;
    }

    private CharactersRecord requireCharacter(int characterId) {
        return characterService.find(EntityKey.of(CHARACTERS.ID, characterId))
                .orElseThrow(Severity.SYSTEM);
    }

    private SessionsRecord requireSession(int sessionId) {
        return sessionService.find(EntityKey.of(SESSIONS.ID, sessionId))
                .orElseThrow(Severity.USER);
    }

    private LocationEdgesRecord requireEdge(LocationsRecord previousLocation, int toLocationId) {
        EntityKey<LocationEdgesRecord> edgeKey = EntityKey.<LocationEdgesRecord>builder()
                .set(LOCATION_EDGES.WORLD_ID, previousLocation.getWorldId())
                .set(LOCATION_EDGES.FROM_LOCATION_ID, previousLocation.getId())
                .set(LOCATION_EDGES.TO_LOCATION_ID, toLocationId)
                .build();
        return edgeService.find(edgeKey)
                .orElseThrow(notFound-> new EntityNotFound(
                        "Edge from location \n%s\n to location id %s does not exist"
                                .formatted(previousLocation, toLocationId),
                        Severity.USER
                ));
    }

    private void assertTraversable(LocationsRecord previousLocation, LocationEdgesRecord edge) {
        if (!edge.getTraversable()) {
            throw new UnsupportedAction(
                    "Edge from \n%s\n to \n%s\n is NOT traversable"
                            .formatted(previousLocation.getName(), edge),
                    Severity.EXPECTED
            );
        }
    }

    private void applyMove(int sessionId, int characterId, int toLocationId, LocationsRecord previousLocation) {
        ResponseLocationChangesRecord changesRecord =
                responseMovementService.moveInCurrentResponse(sessionId, characterId, toLocationId);
        currentLocationService.update(
                EntityKey.<CurrentLocationsRecord>builder()
                        .set(CURRENT_LOCATIONS.SESSION_ID, changesRecord.getSessionId())
                        .set(CURRENT_LOCATIONS.CHARACTER_ID, characterId)
                        .build(),
                EntityDataPayload.<CurrentLocationsRecord>builder()
                        .set(CURRENT_LOCATIONS.WORLD_ID, previousLocation.getWorldId())
                        .set(CURRENT_LOCATIONS.LOCATION_ID, toLocationId)
                        .set(CURRENT_LOCATIONS.TICK_NUM, changesRecord.getTickNum())
                        .build()
        ).orElseThrow("Couldn't apply move");
    }
/*
    public CharactersRecord[] getPresent(int sessionId, int tickNum) {
        MessagesRecord lastMessage = messageService.getLastMessageOf(sessionId);
        if (lastMessage.getTickNum() == tickNum)
            return currentLocationService.getAtLocation(lastMessage.getSessionId(), lastMessage.getTickNum());

        MessagesRecord message = messageService.find(EntityKey.<MessagesRecord>builder()
                .set(MESSAGES.SESSION_ID, sessionId)
                .set(MESSAGES.TICK_NUM, tickNum)
                .build()
        ).orElseThrow("No message when getting present", Severity.USER);

        return movementHistory.getPresentAtTickInLocation(
                message.getSessionId(),
                message.getTickNum(),
                message.getLocationId()
        ).toArray(CharactersRecord[]::new);
    }
*/
    public CharactersRecord[] getAtLocation(@NotNull LocationsRecord location, @NotNull SessionsRecord session) {
        return currentLocationService.getAtLocation(location, session);
    }

    @CheckReturnValue
    public @NotNull LocationsRecord getLocationOf(@NotNull CharactersRecord character, @NotNull SessionsRecord session) throws EntityNotFound {
        return currentLocationService.getLocationOf(character, session);
    }

    @CheckReturnValue
    public @NotNull LocationsRecord getLocationOf(@NotNull int characterId, @NotNull int sessionId) throws EntityNotFound {
        return currentLocationService.getLocationOf(characterId, sessionId);
    }
}
