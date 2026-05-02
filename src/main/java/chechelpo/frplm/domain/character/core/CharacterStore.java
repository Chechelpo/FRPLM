package chechelpo.frplm.domain.character.core;

import chechelpo.frplm.config.controllers.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityStore;
import chechelpo.frplm.frameworks.entities.microservices.EntityDataPayload;
import chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.*;

import java.time.LocalDateTime;

import static chechelpo.frplm.jooq.generated.tables.Characters.CHARACTERS;

@chechelpo.frplm.annotations.Store
final class CharacterStore extends ABSEntityStore<CharactersRecord> {
    CharacterStore(DSLContext dsl) {
        super(dsl, CHARACTERS, EntityTypes.Types.CHARACTER);
    }

    @Override
    public CharactersRecord createAndGet(@NotNull EntityDataPayload<CharactersRecord> data) {
        data.setValue(CHARACTERS.CREATED, LocalDateTime.now());
        return super.createAndGet(data);
    }
}
