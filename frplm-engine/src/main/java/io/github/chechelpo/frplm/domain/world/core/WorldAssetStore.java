package io.github.chechelpo.frplm.domain.world.core;

import io.github.chechelpo.frplm.config.directories.AppDirectory;
import io.github.chechelpo.frplm.core.entities.assets.AssetTypes;
import io.github.chechelpo.frplm.core.entities.assets.EntityAssetStore;
import io.github.chechelpo.frplm.extensions.api.standalone.WorldSnapshot;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import io.github.chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.EnumSet;

@Component
public final class WorldAssetStore extends EntityAssetStore<WorldsRecord, WorldSnapshot.Reference> {
    WorldAssetStore(AppDirectory directory) {
        super(
                EntityConfigs.Types.WORLDS,
                directory,
                EnumSet.of(AssetTypes.BACKGROUND)
        );
    }

    @Override
    protected WorldSnapshot.Reference getStableReference(@NonNull WorldsRecord record) {
        return new WorldSnapshot.Reference(record.getId());
    }
}
