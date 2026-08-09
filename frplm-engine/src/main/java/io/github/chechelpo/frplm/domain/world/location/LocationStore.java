package io.github.chechelpo.frplm.domain.world.location;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityStore;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import static io.github.chechelpo.frplm.jooq.generated.Tables.LOCATIONS;

@Component
final class LocationStore extends EntityStore<LocationsRecord> {
    public LocationStore(DSLContext ctx) {
        super(ctx, LOCATIONS, EntityConfigs.Types.LOCATIONS);
    }
}
