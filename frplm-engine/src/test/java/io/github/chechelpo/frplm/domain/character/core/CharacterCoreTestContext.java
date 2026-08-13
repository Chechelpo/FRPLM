package io.github.chechelpo.frplm.domain.character.core;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.domain.lorebook.entry.core.EntryTestContext;
import io.github.chechelpo.frplm.interfaces.DBReload;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import io.github.chechelpo.frplm.test_utils.TestText;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

import static io.github.chechelpo.frplm.jooq.generated.Tables.CHARACTERS;

@TestComponent
@Import(EntryTestContext.class)
public class CharacterCoreTestContext implements DBReload {
    public final CharacterService service;
    final CharacterFieldsHelper fields;
    private final EntryTestContext entryTestContext;

    CharacterCoreTestContext(CharacterService service, CharacterFieldsHelper fields, EntryTestContext entryTestContext) {
        this.service = service;
        this.fields = fields;
        this.entryTestContext = entryTestContext;
    }

    @Override
    public void reload() {}

    public List<CharactersRecord> createAndGetRecords(int characterAmount) {
        List<CharactersRecord> charactersData =
                new ArrayList<>(characterAmount);

        long seed = 10L;
        SplittableRandom random = new SplittableRandom(seed);

        for (int i = 0; i < characterAmount; i++) {
            CharactersRecord record = service.createAndGet(
                    EntityDataPayload.<CharactersRecord>builder()
                            .set(
                                    CHARACTERS.NAME,
                                    TestText.randomText(seed + i, 0, 255)
                            )
                            .set(
                                    CHARACTERS.DESCRIPTION,
                                    "Character description " + i
                            )
                            .build()
            );


            charactersData.add(record);
        }

        return charactersData;
    }
}
