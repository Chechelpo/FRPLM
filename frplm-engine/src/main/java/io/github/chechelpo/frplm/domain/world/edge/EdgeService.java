package io.github.chechelpo.frplm.domain.world.edge;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.domain.world.location.LocationsService;
import io.github.chechelpo.frplm.events.EventBus;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityService;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.exceptions.runtime.InvalidValue;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationEdgesRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

import static io.github.chechelpo.frplm.jooq.generated.Tables.LOCATIONS;
import static io.github.chechelpo.frplm.jooq.generated.Tables.LOCATION_EDGES;

@Service
public class EdgeService extends EntityService<LocationEdgesRecord, EdgeStore> {
    private final LocationsService locations;

    EdgeService(LocationsService locations, EdgeStore store, EventBus eventBus) {
        super(store, eventBus);
        this.locations = locations;
    }

    /** @return all locations associated with this location ( thisLocation -> otherLocation ) || ( thisLocation <- otherLocation )*/
    public @NotNull List<LocationsRecord> neighboursOf(LocationsRecord location) {
        return this.neighboursOf(locations.keyOf(location));
    }

    /** @return all locations associated with this location ( thisLocation -> otherLocation ) || ( thisLocation <- otherLocation ) */
    public @NotNull List<LocationsRecord> neighboursOf(EntityKey<LocationsRecord> key) {
        return this.store.getNeighboursOf(key);
    }

    @SuppressWarnings("SpringTransactionalMethodCallsInspection")
    @Override
    protected void beforeCreate(EntityDataPayload<LocationEdgesRecord> data, long operationID) {
        if (Objects.equals(data.requireValue(LOCATION_EDGES.FROM_LOCATION_ID), data.requireValue(LOCATION_EDGES.TO_LOCATION_ID)))
            throw new InvalidValue("Locations neighbours must have the same ID");

        super.beforeCreate(data, operationID);
    }

    /** @return edge exists ( thisLocation -> otherLocation ) || ( thisLocation <- otherLocation ) */
    @Transactional(readOnly = true)
    public boolean isNeighbour(@NotNull EntityKey<LocationsRecord> fromKey, @NotNull EntityKey<LocationsRecord> toKey) {
        locations.throwIfInvalidKey(fromKey, true);
        locations.throwIfInvalidKey(toKey, true);

        int thisWorldID = fromKey.getValue(LOCATIONS.WORLD_ID);
        int otherWorldID = toKey.getValue(LOCATIONS.WORLD_ID);
        if (thisWorldID != otherWorldID) return false;

        return this.isNeighbour(thisWorldID, fromKey.requireValue(LOCATIONS.ID), toKey.requireValue(LOCATIONS.ID));
    }
    public boolean isNeighbour(int worldID, int location1ID, int location2ID) {
        return super.exists(EntityKey.<LocationEdgesRecord>builder()
                .set(LOCATION_EDGES.WORLD_ID, worldID)
                .set(LOCATION_EDGES.FROM_LOCATION_ID, location1ID)
                .set(LOCATION_EDGES.TO_LOCATION_ID, location2ID)
                .build()
        ) || super.exists(EntityKey.<LocationEdgesRecord>builder()
                .set(LOCATION_EDGES.WORLD_ID, worldID)
                .set(LOCATION_EDGES.FROM_LOCATION_ID, location2ID)
                .set(LOCATION_EDGES.TO_LOCATION_ID, location1ID)
                .build()
        );
    }
}
