package io.github.chechelpo.frplm.domain.world.location;

import io.github.chechelpo.frplm.core.entities.fields.FieldValidator;
import io.github.chechelpo.frplm.domain.lorebook.outlet.StandardOutlet;
import io.github.chechelpo.frplm.domain.world.core.WorldService;
import io.github.chechelpo.frplm.events.EventBus;
import io.github.chechelpo.frplm.events.crud.CRUDDraftEvent;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.UnexpectedException;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityService;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.exceptions.runtime.UnsupportedAction;
import io.github.chechelpo.frplm.jooq.generated.tables.Locations;
import io.github.chechelpo.frplm.jooq.generated.tables.Lorebooks;
import io.github.chechelpo.frplm.jooq.generated.tables.Worlds;
import io.github.chechelpo.frplm.jooq.generated.tables.records.*;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookService;
import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static io.github.chechelpo.frplm.jooq.generated.Tables.*;

@Service
public class LocationsService extends EntityService<
        LocationsRecord,
        LocationStore> {
    private final LorebookService lorebooks;
    private final WorldService worlds;

    LocationsService(LocationStore store,
                     FieldValidator<LocationsRecord> validator,
                     LorebookService lorebooks, WorldService worlds, EventBus bus) {
        super(store, validator, bus);
        this.lorebooks = lorebooks;
        this.worlds = worlds;
    }

    @Override
    protected void beforeCreate(@NotNull EntityDataPayload<LocationsRecord> data, long operationID) {
        data.ifUnassignedGet(
                LOCATIONS.LOREBOOK_ID,
                () -> lorebooks.createAndGet(
                        EntityDataPayload.<LorebooksRecord>builder()
                                .set(LOREBOOKS.NAME, data.requireNonNull(LOCATIONS.NAME))
                                .set(LOREBOOKS.DEFAULT_OUTLET_ID, StandardOutlet.LOCATION_INFO.stable_id)
                                .build(),
                        LOREBOOKS.ID
                )
        );

        data.set(
                LOCATIONS.ID,
                worlds.incrementAndGet(
                        Worlds.WORLDS.NEXT_LOCATION_ID,
                        EntityKey.of(WORLDS.ID, data.requireNonNull(LOCATIONS.WORLD_ID))
                ).orElseThrow(() -> {
                            log.error("Couldn't get the next location ID for world {}", data.require(Locations.LOCATIONS.WORLD_ID));
                            return new UnexpectedException("Couldn't get the next location ID", Severity.SYSTEM);
                        }
                )
        );

        super.beforeCreate(data, operationID);
    }

    @Override
    protected void afterSuccessfulUpdate(LocationsRecord previousData, EntityKey<LocationsRecord> key, EntityDataPayload<LocationsRecord> updated, long operationID) {
        updated.getAssignment(LOCATIONS.NAME)
                .ifAssignedNotNull(
                        newName -> lorebooks.update(
                                LOREBOOKS.NAME, newName,
                                EntityKey.of(LOREBOOKS.ID, previousData.getLorebookId())
                        )
                );
        super.afterSuccessfulUpdate(previousData, key, updated, operationID);
    }

    @Override
    protected void afterSuccessfulDelete(EntityKey<LocationsRecord> id, long operationID, LocationsRecord record) {
        lorebooks.delete(
                lorebooks.keyOf(record.getLorebookId())
        );
        super.afterSuccessfulDelete(id, operationID, record);
    }
}
