package chechelpo.demo.domain.chars.assets;

import chechelpo.demo.frameworks.entities.assets.AssetType;
import chechelpo.demo.frameworks.entities.assets.EntityAssetRegistry;
import chechelpo.demo.domain.chars.core.CharacterService;
import chechelpo.demo.frameworks.entities.data.EntityKey;
import chechelpo.demo.jooq.generated.tables.records.CharactersRecord;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.EnumSet;

@Component
public class CharacterAssetsRegistry extends
        EntityAssetRegistry<CharactersRecord, CharacterService>
{
    CharacterAssetsRegistry(CharacterService service){
        super(service, "characters", EnumSet.of(AssetType.AVATAR));
    }

    @Override
    protected @Nullable Path getPrefix(EntityKey<CharactersRecord> key) {
        return null;
    }
}
