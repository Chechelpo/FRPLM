package io.github.chechelpo.frplm.extensions.implementations.session;

import io.github.chechelpo.frplm.extensions.api.session.SessionLocation;
import io.github.chechelpo.frplm.extensions.implementations.standalone.LocationImpl;
import io.github.chechelpo.frplm.extensions.implementations.standalone.ExtensionContext;
import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public final class SessionLocationImpl extends LocationImpl implements SessionLocation {
    private final SessionWorldImpl world;

    SessionLocationImpl(LocationsRecord record, ExtensionContext context, SessionWorldImpl world) {
        super(record, context);
        this.world = world;
    }

    public SessionCharacterImpl @NotNull [] getCharactersHere(){
        return world.getAtLocation(this);
    }

    @Override
    public SessionLocation @NotNull [] getSessionNeighbours() {
        return Arrays.stream(this.getNeighbours())
                .map(loc -> new SessionLocationImpl(loc.getRecord(), context, world))
                .toArray(SessionLocationImpl[]::new);
    }
}
