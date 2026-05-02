package chechelpo.frplm.domain.character.assets;

import chechelpo.frplm.frameworks.entities.assets.AssetType;
import chechelpo.frplm.frameworks.entities.assets.EntityAssetRegistry;
import chechelpo.frplm.domain.character.core.CharacterService;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
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
