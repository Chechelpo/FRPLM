package chechelpo.frplm.extensions.implementations.standalone;

import chechelpo.frplm.extensions.api.standalone.LocationSnapshot;
import chechelpo.frplm.extensions.api.standalone.WorldSnapshot;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import org.jetbrains.annotations.NotNull;

public class WorldImpl extends StandaloneEntity<WorldsRecord> implements WorldSnapshot {
    public WorldImpl(WorldsRecord record, ExtensionContext context) {
        super(record, context);
    }

    protected LocationImpl requireImpl(LocationSnapshot loc){
        return (LocationImpl) loc;
    }

    @Override
    public Reference reference() {
        return new WorldSnapshot.Reference(this.record.getId());
    }

    @Override
    public boolean areNeighbours(LocationSnapshot location, @NotNull LocationSnapshot other){
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

    @Override
    public LocationSnapshot @NotNull [] getNeighboursOf(@NotNull LocationSnapshot location){
        LocationImpl loc = requireImpl(location);
        return context.edges().getNeighboursOf(loc.getRecord())
                .stream()
                .map(record -> new LocationImpl(record, context))
                .toArray(LocationImpl[]::new);
    }
}
