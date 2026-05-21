package chechelpo.frplm.domain.world.location.utils;

import chechelpo.frplm.annotations.Factory;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.frameworks.entities.repository.EntityFactory;
import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import org.jetbrains.annotations.NotNull;

@Factory
public final class LocationFactory extends EntityFactory<LocationsRecord, LocationEntity, LocationRepository>{
    LocationFactory(LocationRepository repository) {
        super(repository);
    }

    @Override
    protected @NotNull LocationEntity instantiate(@NotNull EntityKey<LocationsRecord> key) {
        return new LocationEntity(key,repository);
    }
}
