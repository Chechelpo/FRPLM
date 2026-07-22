package io.github.chechelpo.frplm.extensions.mapper;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityKey;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityReader;
import io.github.chechelpo.frplm.extensions.api.standalone.LocationSnapshot;
import io.github.chechelpo.frplm.extensions.implementations.standalone.ExtensionContext;
import io.github.chechelpo.frplm.extensions.implementations.standalone.LocationImpl;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;

import java.util.function.BiFunction;
import java.util.function.Function;

import static io.github.chechelpo.frplm.jooq.generated.Tables.LOCATIONS;

final class LocationMapper extends ReferenceMapper<LocationsRecord, LocationSnapshot.Reference, LocationSnapshot> {
    LocationMapper(EntityReader<LocationsRecord> reader){
        super(
                LocationSnapshot.class,
                LocationSnapshot.Reference::fromString,
                LocationImpl::new,
                reference -> EntityKey.<LocationsRecord>builder()
                        .set(LOCATIONS.WORLD_ID, reference.worldId())
                        .set(LOCATIONS.ID, reference.id())
                        .build()
                ,
                reader
        );
    }

    @Override
    LocationSnapshot.Reference getExampleReference() {
        return new LocationSnapshot.Reference(1, 1);
    }
}
