package chechelpo.frplm.domain.space.location;

import chechelpo.frplm.config.controllers.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityService;
import chechelpo.frplm.jooq.generated.tables.Locations;
import chechelpo.frplm.jooq.generated.tables.Lorebooks;
import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import chechelpo.frplm.domain.lorebook.core.LorebookService;
import chechelpo.frplm.frameworks.entities.microservices.EntityDataPayload;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public final class LocationsService extends ABSEntityService<
        LocationsRecord,
        LocationStore>
{
    private final LorebookService service;
    LocationsService(LocationStore store, LorebookService service) {
        super(store, EntityTypes.Types.LOCATIONS);
        this.service = service;
    }

    @Override
    public @NotNull LocationsRecord createAndGet(EntityDataPayload<LocationsRecord> data) {
        data.setValue(
                Locations.LOCATIONS.LOREBOOK_ID,
                service.createAndGet(
                        EntityDataPayload.fromValues(
                            Map.of(
                            Lorebooks.LOREBOOKS.NAME, data.getValue(Locations.LOCATIONS.NAME)
                            )
                        ),
                        Lorebooks.LOREBOOKS.ID
                )
        );
        return super.createAndGet(data);
    }
}
