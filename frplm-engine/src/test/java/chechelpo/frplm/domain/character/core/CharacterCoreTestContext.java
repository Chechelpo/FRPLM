package chechelpo.frplm.domain.character.core;

import chechelpo.frplm.core.entities.pseudo_services.EntityDataPayload;
import chechelpo.frplm.interfaces.DBReload;
import chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import chechelpo.frplm.test_utils.TestText;
import org.springframework.boot.test.context.TestComponent;

import java.util.ArrayList;
import java.util.List;

import static chechelpo.frplm.jooq.generated.Tables.CHARACTERS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

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

    public List<CharactersRecord> createAndGetRecords(int characterAmount) {
        List<EntityDataPayload<CharactersRecord>> charactersData = new ArrayList<>(characterAmount);
        long seed = 10;

        for (int i = 0; i < characterAmount; i++)
            charactersData.add(EntityDataPayload.<CharactersRecord>builder()
                    .set(CHARACTERS.NAME, TestText.randomText(seed + i, 0, 255))
                    .build()
            );

        return charactersData.stream().map(
                data -> assertDoesNotThrow(() -> service.createAndGet(data))
        ).toList();
    }
}
