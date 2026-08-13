package io.github.chechelpo.frplm.domain.world.region;

import io.github.chechelpo.frplm.config.directories.AppDirectory;
import io.github.chechelpo.frplm.core.entities.assets.AssetTypes;
import io.github.chechelpo.frplm.core.entities.assets.EntityAssetStore;
import io.github.chechelpo.frplm.extensions.api.standalone.RegionSnapshot;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import io.github.chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import org.springframework.stereotype.Component;

import java.util.EnumSet;

import static io.github.chechelpo.frplm.jooq.generated.Tables.REGION;

@Component
public final class RegionAssetStore extends EntityAssetStore<RegionRecord, RegionSnapshot.Reference> {
    RegionAssetStore(AppDirectory directory) {
        super(REGION, directory, EnumSet.of(AssetTypes.BACKGROUND));
    }

    @Override
    protected RegionSnapshot.Reference getStableReference(RegionRecord record) {
        return new RegionSnapshot.Reference(record.getWorldId(), record.getId());
    }
}
