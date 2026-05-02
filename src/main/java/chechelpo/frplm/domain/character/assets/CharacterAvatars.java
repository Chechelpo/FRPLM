package chechelpo.frplm.domain.character.assets;

import chechelpo.frplm.frameworks.entities.assets.AvatarManager;
import chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import org.springframework.stereotype.Component;

@Component
public class CharacterAvatars
        extends AvatarManager<CharactersRecord, CharacterAssetsRegistry> {
    public CharacterAvatars(CharacterAssetsRegistry characters) {
        super(characters);
    }
}
