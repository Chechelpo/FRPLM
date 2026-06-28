package chechelpo.frplm.domain.world.location;

import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.core.entities.pseudo_services.EntityStore;
import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import chechelpo.frplm.domain.world.core.WorldService;
import chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import java.util.List;

import static chechelpo.frplm.jooq.generated.Tables.LOCATIONS;
import static chechelpo.frplm.jooq.generated.Tables.REGION;

@Component
final class LocationStore extends EntityStore<LocationsRecord> {
    private final DSLContext ctx;
    private final WorldService worlds;

    public LocationStore(DSLContext ctx, WorldService worlds) {
        super(ctx, LOCATIONS, EntityTypes.Types.LOCATIONS);
        this.ctx = ctx;
        this.worlds = worlds;
    }


    @Override
    public LocationsRecord createAndGet(@NotNull EntityDataPayload<LocationsRecord> data) {
        /*
        EntityKey<WorldsRecord> worldKey = EntityKey.builder().setAll(
                Map.of(WORLDS.ID, data.getValue(LOCATIONS.WORLD_ID))
        ).build();
        data.setValue(
                LOCATIONS.ID,
                worlds.getAndIncrement(WORLDS.NEXT_LOCATION_ID, worldKey)
        );*/

        return super.createAndGet(data);
    }

    public List<LocationsRecord> getLocationsOfRegion(RegionRecord record){
        return ctx.selectFrom(main_table)
                .where(
                        LOCATIONS.REGION_ID.eq(record.getId())
                        .and(LOCATIONS.WORLD_ID.eq(record.getWorldId()))
                )
                .fetch();
    }

    public List<LocationsRecord> getLocationsOfRegion(int worldId, int regionId){
        return ctx.selectFrom(main_table)
                .where(
                        LOCATIONS.REGION_ID.eq(regionId)
                                .and(LOCATIONS.WORLD_ID.eq(worldId))
                )
                .fetch();
    }
}
