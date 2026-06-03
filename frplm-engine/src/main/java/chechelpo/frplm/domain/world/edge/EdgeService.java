package chechelpo.frplm.domain.world.edge;

import chechelpo.frplm.domain.world.location.LocationsService;
import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.frameworks.entities.pseudo_services.EntityService;
import chechelpo.frplm.frameworks.entities.pseudo_services.EntityKey;
import chechelpo.frplm.jooq.generated.tables.records.LocationNeighborsRecord;
import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static chechelpo.frplm.jooq.generated.Tables.LOCATIONS;
import static chechelpo.frplm.jooq.generated.Tables.LOCATION_NEIGHBORS;

@Service
public class EdgeService extends EntityService<LocationNeighborsRecord, EdgeStore> {
    private final LocationsService locations;

    EdgeService(LocationsService locations, EdgeStore store, EventBus eventBus) {
        super(store, eventBus);
        this.locations = locations;
    }

    public @NotNull List<LocationsRecord> getNeighboursOf(LocationsRecord location) {
        return this.getNeighbours(locations.keyOf(location));
    }

    public @NotNull List<LocationsRecord> getNeighbours(EntityKey<LocationsRecord> key) {
        return this.store.getNeighboursOf(key);
    }

    /**
     * @return edge exists (thisLocation -> otherLocation || otherLocation -> thisLocation)
     */
    @Transactional(readOnly = true)
    public boolean isNeighbour(@NotNull EntityKey<LocationsRecord> thisKey, @NotNull EntityKey<LocationsRecord> otherKey) {
        locations.throwIfInvalidKey(thisKey, true);
        locations.throwIfInvalidKey(otherKey, true);

        int thisWorldID = thisKey.getValue(LOCATIONS.WORLD_ID);
        int otherWorldID = otherKey.getValue(LOCATIONS.WORLD_ID);

        if (thisWorldID != otherWorldID) return false;

        boolean fromThisToOther = this.exists(EntityKey.<LocationNeighborsRecord>builder()
                .set(LOCATION_NEIGHBORS.WORLD_ID, thisWorldID)
                .set(LOCATION_NEIGHBORS.LOCATION1_ID, thisKey.getValue(LOCATIONS.ID))
                .set(LOCATION_NEIGHBORS.LOCATION2_ID, otherKey.getValue(LOCATIONS.ID))
                .build()
        );
        boolean fromOtherToThis = this.exists(EntityKey.<LocationNeighborsRecord>builder()
                .set(LOCATION_NEIGHBORS.WORLD_ID, thisWorldID)
                .set(LOCATION_NEIGHBORS.LOCATION1_ID, otherKey.getValue(LOCATIONS.ID))
                .set(LOCATION_NEIGHBORS.LOCATION2_ID, thisKey.getValue(LOCATIONS.ID))
                .build()
        );

        return fromThisToOther || fromOtherToThis;
    }
}
