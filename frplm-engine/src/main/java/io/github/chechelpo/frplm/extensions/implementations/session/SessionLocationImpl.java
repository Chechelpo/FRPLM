package io.github.chechelpo.frplm.extensions.implementations.session;

import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.extensions.api.session.SessionCharacter;
import io.github.chechelpo.frplm.extensions.api.session.SessionLocation;
import io.github.chechelpo.frplm.extensions.implementations.standalone.LocationImpl;
import io.github.chechelpo.frplm.extensions.implementations.standalone.ExtensionContext;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationEdgesRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.LOCATIONS;
import static io.github.chechelpo.frplm.jooq.generated.Tables.LOCATION_EDGES;

public final class SessionLocationImpl extends LocationImpl implements SessionLocation {
    private final SessionWorldImpl world;

    SessionLocationImpl(LocationsRecord record, ExtensionContext context, SessionWorldImpl world) {
        super(record, context);
        this.world = world;
    }

    @Override
    public @NonNull @Unmodifiable List<SessionCharacter> getCharactersHere(){
        return world.getAtLocation(this);
    }

    @Override
    public List<Edge<SessionLocation>> getSessionOutEdges() {
        return context.edges().getMatching(
                        EntityKey.<LocationEdgesRecord>builder()
                                .set(LOCATION_EDGES.WORLD_ID, record.getWorldId())
                                .set(LOCATION_EDGES.FROM_LOCATION_ID, record.getId())
                                .build()
                ).stream()
                .map(record ->
                        new Edge<SessionLocation>(
                                new SessionLocationImpl(
                                        context.locations().find(
                                                EntityKey.<LocationsRecord>builder()
                                                        .set(LOCATIONS.WORLD_ID, record.getWorldId())
                                                        .set(LOCATIONS.ID, record.getToLocationId())
                                                        .build()
                                        ).orElseThrow(
                                                "Somehow couldn't find edge that exists in DB but destination location doesn't. Go ape shit",
                                                Severity.SYSTEM
                                        ),
                                        context,
                                        world
                                ),
                                record.getEdgedescription(),
                                record.getTraversable(),
                                record.getShowDestinationName(),
                                record.getShowDestinationDescription()
                        )
                )
                .toList();
    }

    @Override
    public SessionLocation @NotNull [] getSessionNeighbours() {
        return Arrays.stream(this.getNeighbours())
                .map(loc -> new SessionLocationImpl(loc.getRecord(), context, world))
                .toArray(SessionLocationImpl[]::new);
    }
}
