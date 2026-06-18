package chechelpo.frplm.extensions.implementations.session;

import chechelpo.frplm.exceptions.Severity;
import chechelpo.frplm.exceptions.runtime.EntityNotFound;
import chechelpo.frplm.extensions.api.session.SessionCharacter;
import chechelpo.frplm.extensions.api.session.SessionLocation;
import chechelpo.frplm.extensions.api.session.SessionWorld;
import chechelpo.frplm.extensions.api.standalone.LocationSnapshot;
import chechelpo.frplm.extensions.implementations.standalone.ExtensionContext;
import chechelpo.frplm.extensions.implementations.standalone.WorldImpl;
import chechelpo.frplm.extensions.api.results.MoveResult;
import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.jooq.generated.tables.records.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

import static chechelpo.frplm.jooq.generated.Tables.CURRENT_LOCATIONS;
import static chechelpo.frplm.jooq.generated.Tables.LOCATIONS;

public final class SessionWorldImpl extends WorldImpl implements SessionWorld {
    private final SessionImpl session;

    SessionWorldImpl(WorldsRecord record, SessionImpl session, ExtensionContext standaloneContext) {
        super(record, standaloneContext);
        this.session = session;

    }

    private SessionCharacterImpl require(SessionCharacter impl) {
        return (SessionCharacterImpl) impl;
    }

    MoveResult move(SessionCharacter character, LocationSnapshot location) {
        return move(require(character), (SessionLocationImpl) location);
    }
    private MoveResult move(SessionCharacterImpl character, SessionLocationImpl location) {
        if (isAtLocation(character, location)) return MoveResult.alreadyAtLocation(character, location);

        SessionLocationImpl characterLocation = this.locationOf(character);
        if (!areNeighbours(characterLocation, location))
            return MoveResult.notNeighbours(character, characterLocation, location);

        boolean success = session.context().movements().move(
                session.getRecord().getId(),
                character.getRecord().getId(),
                location.getRecord().getId()
        );

        if (!success)
            return new MoveResult.FailedMove(MoveResult.FailedMove.Type.UNKNOWN, character, characterLocation, location, "unknown");
        System.out.println("New location of character: " + locationOf(character).getName());
        return MoveResult.success(character, characterLocation, location);
    }

    @Override
    public boolean isAtLocation(@NotNull SessionCharacter character, SessionLocation location) {
        return isAtLocation(require(character), (SessionLocationImpl) location);
    }

    public boolean isAtLocation(@NotNull SessionCharacterImpl character, SessionLocationImpl location) {
        CharactersRecord record = character.getRecord();

        int currentLocationID;
        try {
            currentLocationID = session.context().movements().getLocationOf(
                    character.getRecord(), session.getRecord()
            ).getId();
        } catch (EntityNotFound ignored) {
            throw new RuntimeException("Character " + character + " does not have a current location");
        }

        return currentLocationID == location.getRecord().getId();
    }

    @Contract("_ -> new")
    @NotNull
    SessionLocationImpl locationOf(@NotNull ChatMessageImpl message) {
        return new SessionLocationImpl(
                context.locations().find(EntityKey.<LocationsRecord>builder()
                        .set(LOCATIONS.ID, message.getRecord().getLocationId())
                        .set(LOCATIONS.WORLD_ID, message.getRecord().getWorldId())
                        .build()
                ).orElseThrow(() -> new EntityNotFound("Could not find location of message " + message, Severity.SYSTEM)),
                context,
                this
        );

    }

    @Contract("_ -> new")
    @Override
    public @NotNull SessionLocation locationOf(@NotNull SessionCharacter character) {
        return locationOf(require(character));
    }

    @Contract("_ -> new")
    @NotNull
    public SessionLocationImpl locationOf(@NotNull SessionCharacterImpl character) {
        try {
            return new SessionLocationImpl(
                    session.context().movements().getLocationOf(character.getRecord(), session.getRecord()),
                    context,
                    this
            );
        } catch (EntityNotFound ignored) {
            throw new RuntimeException("Character " + character.getName() + " does not have a current location");
        }
    }

    public SessionCharacterImpl @NotNull [] getAtLocation(@NotNull SessionLocationImpl location) {
        return Arrays
                .stream(session.context().movements().getAtLocation(location.getRecord(), session.getRecord()))
                .map(record -> new SessionCharacterImpl(record, this.context, session, this))
                .toArray(SessionCharacterImpl[]::new);
    }

    @Override
    public String toString() {
        return String.format("World %s of session %s",
                record.getName(),
                session
        );
    }
}
