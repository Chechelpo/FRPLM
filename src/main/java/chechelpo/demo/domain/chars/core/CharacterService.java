package chechelpo.demo.domain.chars.core;

import chechelpo.demo.config.controllers.EntityTypes;
import chechelpo.demo.frameworks.entities.microservices.ABSEntityService;
import chechelpo.demo.jooq.generated.tables.records.CharactersRecord;
import chechelpo.demo.jooq.generated.tables.records.LorebooksRecord;
import chechelpo.demo.domain.lorebook.main.LorebookService;
import chechelpo.demo.frameworks.entities.data.EntityDataPayload;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.Map;

import static chechelpo.demo.jooq.generated.Tables.CHARACTERS;
import static chechelpo.demo.jooq.generated.Tables.LOREBOOKS;

@Component
public final class CharacterService extends ABSEntityService<
        CharactersRecord,
        CharacterStore
        > {
    private final LorebookService lorebookService;
    CharacterService(CharacterStore store, LorebookService lorebookService) {
        super(store, EntityTypes.Types.CHARACTER);
        this.lorebookService = lorebookService;
    }

    @Override
    public @NotNull CharactersRecord createAndGet(EntityDataPayload<CharactersRecord> data) {
        LorebooksRecord record = lorebookService.createAndGet(EntityDataPayload.fromValues(
                Map.of(
                        LOREBOOKS.NAME, data.getValue(CHARACTERS.NAME)
                )
        ));
        data.setValue(CHARACTERS.LOREBOOK_ID, record.getId());
        return super.createAndGet(data);
    }

}

