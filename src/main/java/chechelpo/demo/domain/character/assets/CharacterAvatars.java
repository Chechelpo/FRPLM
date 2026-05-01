package chechelpo.demo.domain.character.assets;

import chechelpo.demo.frameworks.entities.assets.AvatarManager;
import chechelpo.demo.jooq.generated.tables.records.CharactersRecord;
import org.springframework.stereotype.Component;

@Component
public class CharacterAvatars
        extends AvatarManager<CharactersRecord, CharacterAssetsRegistry> {
    public CharacterAvatars(CharacterAssetsRegistry characters) {
        super(characters);
    }
}
