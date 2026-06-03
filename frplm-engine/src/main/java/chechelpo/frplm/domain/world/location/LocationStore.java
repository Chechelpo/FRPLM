package chechelpo.frplm.domain.world.location;

import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.frameworks.entities.pseudo_services.EntityStore;
import chechelpo.frplm.frameworks.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import chechelpo.frplm.domain.world.core.WorldService;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import static chechelpo.frplm.jooq.generated.Tables.LOCATIONS;

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
}
