package chechelpo.frplm.domain.character.core;

import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.EntityStore;
import chechelpo.frplm.frameworks.entities.microservices.EntityDataPayload;
import chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.*;

import java.time.LocalDateTime;
import java.util.List;

import static chechelpo.frplm.jooq.generated.Tables.STARTING_LOCATIONS;
import static chechelpo.frplm.jooq.generated.tables.Characters.CHARACTERS;

@chechelpo.frplm.annotations.Store
final class CharacterStore extends EntityStore<CharactersRecord> {
    CharacterStore(DSLContext dsl) {
        super(dsl, CHARACTERS, EntityTypes.Types.CHARACTER);
    }

    @Override
    public CharactersRecord createAndGet(@NotNull EntityDataPayload<CharactersRecord> data) {
        data.set(CHARACTERS.CREATED, LocalDateTime.now());
        return super.createAndGet(data);
    }

    public @NotNull List<CharactersRecord> getStartingAtWorld(int worldID){
        return ctx.select()
                .from(main_table)
                .join(STARTING_LOCATIONS)
                .on(
                        CHARACTERS.ID.eq(STARTING_LOCATIONS.CHARACTER_ID)
                        .and(STARTING_LOCATIONS.WORLD_ID.eq(worldID))
                ).fetchInto(CharactersRecord.class);
    }
}
