package chechelpo.frplm.domain.character.core;

import chechelpo.frplm.interfaces.DBReload;
import org.springframework.boot.test.context.TestComponent;

@TestComponent
public class CharacterCoreTestContext implements DBReload {
    public final CharacterService service;
    final CharacterFieldsHelper fields;

    CharacterCoreTestContext(CharacterService service, CharacterFieldsHelper fields) {
        this.service = service;
        this.fields = fields;
    }

    @Override
    public void reload() {}
}
