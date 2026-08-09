package io.github.chechelpo.frplm.domain.character.core;

import io.github.chechelpo.frplm.config.directories.AppDirectory;
import io.github.chechelpo.frplm.core.entities.assets.AssetTypes;
import io.github.chechelpo.frplm.core.entities.assets.EntityAssetStore;
import io.github.chechelpo.frplm.extensions.api.standalone.CharacterSnapshot;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import org.springframework.stereotype.Component;

import java.util.EnumSet;

@Component
public final class CharacterAssetStore extends EntityAssetStore<CharactersRecord, CharacterSnapshot.Reference> {
    CharacterAssetStore(AppDirectory directory) {
        super(EntityConfigs.Types.CHARACTER, directory, EnumSet.of(AssetTypes.AVATAR));
    }

    @Override
    protected CharacterSnapshot.Reference getStableReference(CharactersRecord record) {
        return new CharacterSnapshot.Reference(record.getWorldId(), record.getId());
    }
}
