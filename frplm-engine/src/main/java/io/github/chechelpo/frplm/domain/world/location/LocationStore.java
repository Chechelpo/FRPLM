package io.github.chechelpo.frplm.domain.world.location;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityStore;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import io.github.chechelpo.frplm.domain.world.core.WorldService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.LOCATIONS;

@Component
final class LocationStore extends EntityStore<LocationsRecord> {
    private final DSLContext ctx;
    private final WorldService worlds;

    public LocationStore(DSLContext ctx, WorldService worlds) {
        super(ctx, LOCATIONS, EntityConfigs.Types.LOCATIONS);
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

    public List<LocationsRecord> getLocationsOfRegion(int worldId, @Nullable Integer regionId){
        return ctx.selectFrom(main_table)
                .where(
                        LOCATIONS.REGION_ID.eq(regionId)
                                .and(LOCATIONS.WORLD_ID.eq(worldId))
                )
                .fetch();
    }
}
