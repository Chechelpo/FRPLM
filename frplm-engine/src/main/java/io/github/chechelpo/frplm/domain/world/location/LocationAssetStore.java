package io.github.chechelpo.frplm.domain.world.location;

import io.github.chechelpo.frplm.config.directories.AppDirectory;
import io.github.chechelpo.frplm.core.entities.assets.AssetTypes;
import io.github.chechelpo.frplm.core.entities.assets.EntityAssetStore;
import io.github.chechelpo.frplm.extensions.api.standalone.LocationSnapshot;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import org.springframework.stereotype.Component;

import java.util.EnumSet;

@Component
public final class LocationAssetStore extends EntityAssetStore<LocationsRecord, LocationSnapshot.Reference> {
    LocationAssetStore(AppDirectory directory) {
        super(
                EntityConfigs.Types.LOCATIONS,
                directory,
                EnumSet.of(AssetTypes.BACKGROUND)
        );
    }

    @Override
    protected LocationSnapshot.Reference getStableReference(LocationsRecord record) {
        return new LocationSnapshot.Reference(record.getWorldId(), record.getId());
    }
}
