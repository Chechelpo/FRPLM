package io.github.chechelpo.frplm.domain.world.location;

import io.github.chechelpo.frplm.domain.lorebook.outlet.StandardOutlet;
import io.github.chechelpo.frplm.domain.world.core.WorldService;
import io.github.chechelpo.frplm.events.EventBus;
import io.github.chechelpo.frplm.events.crud.CRUDDraftEvent;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.UnexpectedException;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityService;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.exceptions.runtime.UnsupportedAction;
import io.github.chechelpo.frplm.jooq.generated.tables.Locations;
import io.github.chechelpo.frplm.jooq.generated.tables.Lorebooks;
import io.github.chechelpo.frplm.jooq.generated.tables.Worlds;
import io.github.chechelpo.frplm.jooq.generated.tables.records.*;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookService;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.*;

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
        if (!data.assignsField(LOCATIONS.LOREBOOK_ID)){
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
        }

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

    @Override
    protected void afterSuccessfulDelete(EntityKey<LocationsRecord> id, long operationID, LocationsRecord record) {
        lorebooks.delete(
                lorebooks.keyOf(lorebooks.getLorebookOf(record))
        );
        super.afterSuccessfulDelete(id, operationID, record);
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

    @EventListener
    void checkRegionDeletion(CRUDDraftEvent.DeleteEntityDraft<?> rawEvent){
        if (rawEvent.type() != EntityConfigs.Types.REGIONS) return;

        //noinspection unchecked
        CRUDDraftEvent.DeleteEntityDraft<RegionRecord> event = (CRUDDraftEvent.DeleteEntityDraft<RegionRecord>) rawEvent;
        int worldId = event.key().requireValue(REGION.WORLD_ID);
        int regionId = event.key().requireValue(REGION.ID);

        if (!store.getMatching(
                EntityDataPayload.<LocationsRecord>builder()
                        .set(LOCATIONS.WORLD_ID, worldId)
                        .set(LOCATIONS.REGION_ID, regionId)
                        .build())
                .isEmpty()
        ) throw new UnsupportedAction("This region still has associated locations", Severity.USER);
    }
}
