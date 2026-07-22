package io.github.chechelpo.frplm.extensions.implementations.standalone;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.extensions.api.standalone.*;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class WorldImpl extends StandaloneEntity<WorldsRecord> implements WorldSnapshot {
    public WorldImpl(WorldsRecord record, ExtensionContext context) {
        super(record, context);
    }

    protected LocationImpl requireImpl(LocationSnapshot loc) {
        return (LocationImpl) loc;
    }

    @Override
    public Reference asReference() {
        return new WorldSnapshot.Reference(this.record.getId());
    }

    @Override
    public boolean areNeighbours(LocationSnapshot location, @NotNull LocationSnapshot other) {
        LocationImpl location1 = requireImpl(location);
        LocationImpl location2 = requireImpl(other);
        LocationSnapshot[] neighbours = getNeighboursOf(location1);

        EntityKey<LocationsRecord> key = context.locations().keyOf(location1.getRecord());
        EntityKey<LocationsRecord> otherKey = context.locations().keyOf(location2.getRecord());
        if (key.equals(otherKey)) return true;

        for (LocationSnapshot neighbour : neighbours) {
            LocationImpl loc = requireImpl(neighbour);
            key = context.locations().keyOf(loc.getRecord());
            if (otherKey.equals(key)) return true;
        }

        return false;
    }

    public boolean isTraversable(LocationSnapshot fromLocation, LocationSnapshot toLocation) {
        Objects.requireNonNull(toLocation, "To location is null");
        return Objects.requireNonNull(fromLocation, "From location is null when checking traversable to " + toLocation)
                .getOutEdges()
                .stream()
                .anyMatch(
                        locationSnapshotEdge ->
                                locationSnapshotEdge.toLocation().sameEntityAs(toLocation)
                                        &&
                                        locationSnapshotEdge.traversable()
                );
    }

    @Override
    public String getName() {
        return record.getName();
    }

    @Override
    public String getDescription() {
        return record.getDescription();
    }

    @Override
    public LocationSnapshot @NotNull [] getNeighboursOf(@NotNull LocationSnapshot location) {
        LocationImpl loc = requireImpl(location);
        return context.edges().neighboursOf(loc.getRecord())
                .stream()
                .map(record -> new LocationImpl(record, context))
                .toArray(LocationImpl[]::new);
    }

    @Override
    public List<RegionSnapshot> getRootRegions() {
        return context.regions().getRoots(this.record.getId()).stream()
                .map(record -> (RegionSnapshot) new RegionImpl(record, context))
                .toList();
    }

    @Override
    public LorebookSnapshot lorebook() {
        return new LorebookImpl(context.lorebooks().getLorebookOf(getRecord()), context);
    }
}
