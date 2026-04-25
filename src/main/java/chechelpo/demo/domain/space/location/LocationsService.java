package chechelpo.demo.domain.space.location;

import chechelpo.demo.config.controllers.EntityTypes;
import chechelpo.demo.frameworks.entities.microservices.ABSEntityService;
import chechelpo.demo.jooq.generated.tables.Locations;
import chechelpo.demo.jooq.generated.tables.Lorebooks;
import chechelpo.demo.jooq.generated.tables.records.LocationsRecord;
import chechelpo.demo.domain.lorebook.main.LorebookService;
import chechelpo.demo.frameworks.entities.data.EntityDataPayload;
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
