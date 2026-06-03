package chechelpo.frplm.domain.world.location;

import chechelpo.frplm.domain.lorebook.outlet.StandardOutlet;
import chechelpo.frplm.domain.world.core.WorldService;
import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.exceptions.Severity;
import chechelpo.frplm.exceptions.runtime.UnexpectedException;
import chechelpo.frplm.frameworks.entities.pseudo_services.EntityService;
import chechelpo.frplm.frameworks.entities.pseudo_services.EntityKey;
import chechelpo.frplm.jooq.generated.tables.Locations;
import chechelpo.frplm.jooq.generated.tables.Lorebooks;
import chechelpo.frplm.jooq.generated.tables.Worlds;
import chechelpo.frplm.jooq.generated.tables.records.CurrentLocationsRecord;
import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import chechelpo.frplm.domain.lorebook.core.LorebookService;
import chechelpo.frplm.frameworks.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static chechelpo.frplm.jooq.generated.Tables.LOCATIONS;
import static chechelpo.frplm.jooq.generated.Tables.LOREBOOKS;

@Service
public class LocationsService extends EntityService<
        LocationsRecord,
        LocationStore> {
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
        lorebookData.set(LOREBOOKS.NAME, data.requireValue(LOCATIONS.NAME));
        lorebookData.set(LOREBOOKS.DEFAULT_OUTLET_ID, StandardOutlet.LOCATION_INFO.stable_id);

        data.set(
                Locations.LOCATIONS.LOREBOOK_ID,
                lorebooks.createAndGet(
                        lorebookData,
                        Lorebooks.LOREBOOKS.ID
                )
        );

        EntityKey<WorldsRecord> worldKey = EntityKey.of(Worlds.WORLDS.ID, data.requireValue(Locations.LOCATIONS.WORLD_ID));
        int locationID = worlds.incrementAndGet(
                Worlds.WORLDS.NEXT_LOCATION_ID,
                worldKey
        ).orElseThrow(() -> {
                    log.error("Couldn't get the next location ID for world {}", data.requireValue(Locations.LOCATIONS.WORLD_ID));
                    return new UnexpectedException("Couldn't get the next location ID", Severity.SYSTEM);
                }
        );

        data.set(Locations.LOCATIONS.ID, locationID);

        super.beforeCreate(data, operationID);
    }

    @Transactional(readOnly = true)
    @CheckReturnValue
    public @NotNull LocationsRecord getLocationBy(@NotNull CurrentLocationsRecord record) {
        return store.get(
                EntityKey.<LocationsRecord>builder()
                        .set(LOCATIONS.WORLD_ID, record.getWorldId())
                        .set(LOCATIONS.ID, record.getLocationId())
                        .build()
        );
    }

}
