package io.github.chechelpo.frplm.extensions.implementations.session;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
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
import java.util.Objects;

import static io.github.chechelpo.frplm.jooq.generated.Tables.LOCATIONS;
import static io.github.chechelpo.frplm.jooq.generated.Tables.SESSION_CHARACTERS;

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
        throw new UnsupportedOperationException("A");
    }

    @Override
    public boolean isAtLocation(@NotNull SessionCharacter character, SessionLocation location) {
        return isAtLocation(require(character), (SessionLocationImpl) location);
    }

    public boolean isAtLocation(@NotNull SessionCharacterImpl character, SessionLocationImpl location) {
        return Objects.equals(character.getRecord().getCurrentLocationId(), location.getRecord().getId());
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
                    context.locations().require(EntityKey.<LocationsRecord>builder()
                            .set(LOCATIONS.ID, character.getRecord().getCurrentLocationId())
                            .set(LOCATIONS.WORLD_ID, character.getRecord().getWorldId())
                            .build()),
                    context,
                    this
            );
        } catch (EntityNotFound ignored) {
            throw new RuntimeException("Character " + character.getName() + " does not have a current location");
        }
    }

    public List<SessionCharacter> getAtLocation(@NotNull SessionLocationImpl location) {
        return session.context().sessionCharacters().getMatching(
                        EntityDataPayload.<SessionCharactersRecord>builder()
                                .set(SESSION_CHARACTERS.SESSION_ID, this.session.getRecord().getId())
                                .set(SESSION_CHARACTERS.WORLD_ID, this.record.getId())
                                .set(SESSION_CHARACTERS.CURRENT_LOCATION_ID, location.getRecord().getId())
                                .build()
                )
                .map(record -> (SessionCharacter) new SessionCharacterImpl(record, this.context, session, this));
    }

    @Override
    public String toString() {
        return String.format("World %s of session %s",
                record.getName(),
                session
        );
    }
}
