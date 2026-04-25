package chechelpo.demo.domain.chars.core;

import chechelpo.demo.config.controllers.EntityTypes;
import chechelpo.demo.frameworks.entities.microservices.ABSEntityStore;
import chechelpo.demo.frameworks.entities.data.EntityDataPayload;
import chechelpo.demo.jooq.generated.tables.records.CharactersRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.*;

import java.time.LocalDateTime;

import static chechelpo.demo.jooq.generated.tables.Characters.CHARACTERS;

@chechelpo.demo.annotations.Store
final class CharacterStore extends ABSEntityStore<CharactersRecord> {
    CharacterStore(DSLContext dsl) {
        super(dsl, CHARACTERS, EntityTypes.Types.CHARACTER);
    }

    @Override
    protected CharactersRecord createAndGet(@NotNull EntityDataPayload<CharactersRecord> data) {
        data.setValue(CHARACTERS.CREATED, LocalDateTime.now());
        return super.createAndGet(data);
    }
}
