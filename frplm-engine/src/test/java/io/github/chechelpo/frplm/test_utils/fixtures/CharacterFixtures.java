package io.github.chechelpo.frplm.test_utils.fixtures;

import io.github.chechelpo.frplm.domain.character.core.CharacterService;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import org.jooq.TableField;
import org.jspecify.annotations.NonNull;

import java.util.Set;

import static io.github.chechelpo.frplm.jooq.generated.Tables.CHARACTERS;

public class CharacterFixtures extends EntityFixtures<CharactersRecord, CharacterService> {
    public CharacterFixtures(CharacterService service, @NonNull String seed) {
        super(service, seed);
    }

    @Override
    protected Set<TableField<CharactersRecord, ?>> doNotGenerateFields() {
        return Set.of(CHARACTERS.LOREBOOK_ID, CHARACTERS.STARTING_LOCATION_ID);
    }
}
