package chechelpo.frplm.domain.world.location.microservices;

import chechelpo.frplm.domain.lorebook.outlet.StandardOutlet;
import chechelpo.frplm.domain.world.core.microservices.WorldService;
import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.frameworks.entities.microservices.EntityService;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.jooq.generated.tables.Locations;
import chechelpo.frplm.jooq.generated.tables.Lorebooks;
import chechelpo.frplm.jooq.generated.tables.Worlds;
import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import chechelpo.frplm.domain.lorebook.core.LorebookService;
import chechelpo.frplm.frameworks.entities.microservices.EntityDataPayload;
import chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import static chechelpo.frplm.jooq.generated.Tables.LOCATIONS;
import static chechelpo.frplm.jooq.generated.Tables.LOREBOOKS;

@Service
public class LocationsService extends EntityService<
        LocationsRecord,
        LocationStore>
{
    private final LorebookService lorebooks;
    private final WorldService worlds;
    LocationsService(LocationStore store, LorebookService lorebooks, WorldService worlds, EventBus bus) {
        super(store, bus);

        this.lorebooks = lorebooks;
        this.worlds = worlds;
    }

    @Override
    protected void beforeCreate(@NotNull EntityDataPayload<LocationsRecord> data, long operationID) {
        EntityDataPayload<LorebooksRecord> lorebookData = new EntityDataPayload<>();
        lorebookData.set(LOREBOOKS.NAME, data.getValue(LOCATIONS.NAME));
        lorebookData.set(LOREBOOKS.DEFAULT_OUTLET_ID, StandardOutlet.LOCATION_INFO.stable_id);

        data.set(
                Locations.LOCATIONS.LOREBOOK_ID,
                lorebooks.createAndGet(
                        lorebookData,
                        Lorebooks.LOREBOOKS.ID
                )
        );

        EntityKey<WorldsRecord> worldKey = EntityKey.of(Worlds.WORLDS.ID, data.getValue(Locations.LOCATIONS.WORLD_ID));
        data.set(
                Locations.LOCATIONS.ID,
                worlds.getAndIncrement(
                        Worlds.WORLDS.NEXT_LOCATION_ID,
                        worldKey
                )
        );

        super.beforeCreate(data, operationID);
    }

}
