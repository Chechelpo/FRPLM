package chechelpo.frplm.domain.space.location;

import ch.qos.logback.classic.Level;
import chechelpo.frplm.config.controllers.EntityTypes;
import chechelpo.frplm.domain.space.world.WorldService;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityService;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.jooq.generated.tables.Locations;
import chechelpo.frplm.jooq.generated.tables.Lorebooks;
import chechelpo.frplm.jooq.generated.tables.Worlds;
import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import chechelpo.frplm.domain.lorebook.core.LorebookService;
import chechelpo.frplm.frameworks.entities.microservices.EntityDataPayload;
import chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public final class LocationsService extends ABSEntityService<
        LocationsRecord,
        LocationStore>
{
    private final LorebookService lorebooks;
    private final WorldService worlds;
    LocationsService(LocationStore store, LorebookService lorebooks, WorldService worlds) {
        super(store, EntityTypes.Types.LOCATIONS);

        this.lorebooks = lorebooks;
        this.worlds = worlds;
    }

    @Override
    protected EntityDataPayload<LocationsRecord> beforeCreate(@NotNull EntityDataPayload<LocationsRecord> data) {
        coercePayload(data);
        // Create a matching lorebook for this location
        data.setValue(
                Locations.LOCATIONS.LOREBOOK_ID,
                lorebooks.createAndGet(
                        EntityDataPayload.fromValues(
                                Map.of(
                                        Lorebooks.LOREBOOKS.NAME, data.getValue(Locations.LOCATIONS.NAME)
                                )
                        ),
                        Lorebooks.LOREBOOKS.ID
                )
        );
        //Insert increasing ID of the world
        EntityKey.Builder<WorldsRecord> builder = EntityKey.builder();
        data.setValue(
                Locations.LOCATIONS.ID,
                worlds.getAndIncrement(
                        Worlds.WORLDS.NEXT_LOCATION_ID,
                        builder
                                .set(Worlds.WORLDS.ID, data.getValue(Locations.LOCATIONS.WORLD_ID))
                                .build()
                )
        );

        return super.beforeCreate(data);
    }

}
