package chechelpo.frplm.domain.space.edge;

import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityStore;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.frameworks.entities.microservices.EntityDataPayload;
import chechelpo.frplm.jooq.generated.tables.records.LocationNeighborsRecord;

import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import java.util.List;

import static chechelpo.frplm.jooq.generated.Tables.LOCATIONS;
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

    public @NotNull List<LocationsRecord> getNeighboursOf(@NotNull EntityKey<LocationsRecord> key){
        List<LocationsRecord> records = ctx
                .select(LOCATIONS.fields())
                .from(LOCATION_NEIGHBORS)
                .join(LOCATIONS)
                .on(
                        LOCATION_NEIGHBORS.LOCATION1_ID.eq(key.getValue(LOCATIONS.ID))
                                .and(LOCATIONS.ID.eq(LOCATION_NEIGHBORS.LOCATION2_ID))
                                .or(
                                        LOCATION_NEIGHBORS.LOCATION2_ID.eq(key.getValue(LOCATIONS.ID))
                                                .and(LOCATIONS.ID.eq(LOCATION_NEIGHBORS.LOCATION1_ID))
                                )
                )
                .fetchInto(LocationsRecord.class);
        log.info("Found {} neighbours", records);
        return records;
    }

    @Override
    public LocationNeighborsRecord get(@NotNull EntityKey<LocationNeighborsRecord> id) {
        LocationNeighborsRecord normal_record = super.get(id);
        if (normal_record != null) return normal_record;

        //We invert the key, get both possibilities
        return ctx.selectFrom(LOCATION_NEIGHBORS)
                .where(LOCATION_NEIGHBORS.LOCATION2_ID.eq(id.getValue(LOCATION_NEIGHBORS.LOCATION1_ID))
                        .and(LOCATION_NEIGHBORS.LOCATION1_ID.eq(id.getValue(LOCATION_NEIGHBORS.LOCATION2_ID))))
                .fetchOne();
    }

    @Override
    public boolean delete(@NotNull EntityKey<LocationNeighborsRecord> id) {
        try{
            return super.delete(id);
        }catch (Exception ignored) {
            return ctx.deleteFrom(LOCATION_NEIGHBORS)
                    .where(LOCATION_NEIGHBORS.LOCATION2_ID.eq(id.getValue(LOCATION_NEIGHBORS.LOCATION1_ID))
                            .and(LOCATION_NEIGHBORS.LOCATION1_ID.eq(id.getValue(LOCATION_NEIGHBORS.LOCATION2_ID))))
                    .execute() == 1;
        }
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
