package chechelpo.frplm.domain.space.edge;

import chechelpo.frplm.config.controllers.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityStore;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.frameworks.entities.microservices.EntityDataPayload;
import chechelpo.frplm.jooq.generated.tables.records.LocationNeighborsRecord;

import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import static chechelpo.frplm.jooq.generated.tables.LocationNeighbors.LOCATION_NEIGHBORS;

@Component
final class EdgeStore extends ABSEntityStore<LocationNeighborsRecord> {
    EdgeStore(DSLContext dsl) {
        super(dsl, LOCATION_NEIGHBORS, EntityTypes.Types.EDGES);
    }

    @Override
    public boolean update(@NotNull EntityKey<LocationNeighborsRecord> id, @NotNull EntityDataPayload<LocationNeighborsRecord> object) {
        return super.update(id, object);
    }

    @Override
    public LocationNeighborsRecord createAndGet(@NotNull EntityDataPayload<LocationNeighborsRecord> data) {
        /*
        //We first search for the inverted table entry.
        EntityKey<LocationNeighborsRecord> key = EntityKey.fromValues(
                Map.of(
                LOCATION_NEIGHBORS.TO_ID, data.getValue(LOCATION_NEIGHBORS.FROM_ID),
                LOCATION_NEIGHBORS.FROM_ID, data.getValue(LOCATION_NEIGHBORS.TO_ID),
                LOCATION_NEIGHBORS.WORLD_ID, data.getValue(LOCATION_NEIGHBORS.WORLD_ID)
        ),
                fields
        );
        LocationNeighborsRecord previous = get(key);
        //If it exists, we must check that is_directed is true, or we could have a duplicate edge
        if(previous != null) {
            boolean is_directed = previous.get(LOCATION_NEIGHBORS.IS_DIRECTED);
            if (!is_directed) throw new IllegalStateException("Edge is not directed yet we are adding another edge");
        }
           */
        return super.createAndGet(data);
    }
    /*
    private boolean isSymetric(EntityDataPayload<LocationNeighborsRecord> data) {
        EntityKey<LocationNeighborsRecord> key = EntityKey.fromValues(
                Map.of(
                        LOCATION_NEIGHBORS.TO_ID, data.getValue(LOCATION_NEIGHBORS.FROM_ID),
                        LOCATION_NEIGHBORS.FROM_ID, data.getValue(LOCATION_NEIGHBORS.TO_ID),
                        LOCATION_NEIGHBORS.WORLD_ID, data.getValue(LOCATION_NEIGHBORS.WORLD_ID)
                ),
                fields
        );

        return isSymetric(key);
    }

    private boolean isSymetric(EntityKey<LocationNeighborsRecord> key) {
        LocationNeighborsRecord previous = get(key);

        if(previous != null) {
            boolean is_directed = previous.get(LOCATION_NEIGHBORS.IS_DIRECTED);
            if (!is_directed) return true;
        }

        return false;
    }

     */
}
