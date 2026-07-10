package chechelpo.frplm.domain.world.edge;

import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.core.entities.pseudo_services.EntityStore;
import chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.jooq.generated.tables.records.LocationEdgesRecord;

import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import java.util.List;

import static chechelpo.frplm.jooq.generated.Tables.LOCATIONS;
import static chechelpo.frplm.jooq.generated.tables.LocationEdges.LOCATION_EDGES;

@Component
final class EdgeStore extends EntityStore<LocationEdgesRecord> {
    EdgeStore(DSLContext dsl) {
        super(dsl, LOCATION_EDGES, EntityTypes.Types.EDGES);
    }

    public @NotNull List<LocationsRecord> getNeighboursOf(@NotNull EntityKey<LocationsRecord> key){
        var locationId = key.getValue(LOCATIONS.ID);

        return ctx
                .selectFrom(LOCATIONS)
                .whereExists(
                        ctx.selectOne()
                                .from(LOCATION_EDGES)
                                .where(
                                        LOCATION_EDGES.FROM_LOCATION_ID.eq(locationId)
                                                .and(
                                                        LOCATION_EDGES.TO_LOCATION_ID.eq(LOCATIONS.ID)
                                                )
                                                .or(
                                                        LOCATION_EDGES.TO_LOCATION_ID.eq(locationId)
                                                                .and(
                                                                        LOCATION_EDGES.FROM_LOCATION_ID.eq(LOCATIONS.ID)
                                                                )
                                                )
                                )
                )
                .fetchInto(LocationsRecord.class);
    }
}
