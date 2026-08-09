package io.github.chechelpo.frplm.extensions.implementations.session;

import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.EntityNotFound;
import io.github.chechelpo.frplm.extensions.implementations.standalone.ExtensionContext;
import io.github.chechelpo.frplm.extensions.implementations.standalone.WorldImpl;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.jooq.generated.tables.records.*;
import io.github.chechelpo.frplm.extensions.api.results.MoveResult;
import io.github.chechelpo.frplm.extensions.api.session.SessionCharacter;
import io.github.chechelpo.frplm.extensions.api.session.SessionLocation;
import io.github.chechelpo.frplm.extensions.api.session.SessionWorld;
import io.github.chechelpo.frplm.extensions.api.standalone.LocationSnapshot;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.LOCATIONS;

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
    private MoveResult move(SessionCharacterImpl character, SessionLocationImpl toLocation) {
        if (isAtLocation(character, toLocation)) return MoveResult.alreadyAtLocation(character, toLocation);

        SessionLocationImpl characterLocation = this.locationOf(character);
        if (!isTraversable(characterLocation, toLocation))
            return MoveResult.notNeighbours(character, characterLocation, toLocation);

        boolean success = session.context().movements().move(
                session.getRecord().getId(),
                character.getRecord().getId(),
                toLocation.getRecord().getId()
        );

        if (!success)
            return new MoveResult.FailedMove(MoveResult.FailedMove.Type.UNKNOWN, character, characterLocation, toLocation, "unknown");
        System.out.println("New location of character: " + locationOf(character).getName());
        return MoveResult.success(character, characterLocation, toLocation);
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
                ).orElseThrow(notFound -> new EntityNotFound("Could not find location of message " + message + "\n" + notFound.toString(), Severity.SYSTEM)),
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

    public List<SessionCharacter> getAtLocation(@NotNull SessionLocationImpl location) {
        return Arrays
                .stream(session.context().movements().getAtLocation(location.getRecord(), session.getRecord()))
                .map(record -> (SessionCharacter) new SessionCharacterImpl(record, this.context, session, this))
                .toList();
    }

    @Override
    public String toString() {
        return String.format("World %s of session %s",
                record.getName(),
                session
        );
    }
}
