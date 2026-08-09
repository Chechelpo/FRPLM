package io.github.chechelpo.frplm.domain.world.location;

import io.github.chechelpo.frplm.core.entities.assets.EntityAssetController;
import io.github.chechelpo.frplm.core.entities.assets.EntityAssetStore;
import io.github.chechelpo.frplm.core.entities.fields.DTOMapper;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityReader;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs.LOCATIONS_URL;

@RestController
@RequestMapping(LOCATIONS_URL)
final class LocationAssetController extends EntityAssetController<LocationsRecord> {
    LocationAssetController(
            EntityAssetStore<LocationsRecord, ?> assetStore,
            DTOMapper<LocationsRecord> mapper,
            EntityReader<LocationsRecord> reader
    ) {
        super(assetStore, mapper, reader);
    }
}
