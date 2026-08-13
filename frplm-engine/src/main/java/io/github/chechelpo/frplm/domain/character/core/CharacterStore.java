package io.github.chechelpo.frplm.domain.character.core;

import io.github.chechelpo.frplm.annotations.Store;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityStore;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.*;

import static io.github.chechelpo.frplm.jooq.generated.tables.Characters.CHARACTERS;

@Store
final class CharacterStore extends EntityStore<CharactersRecord> {
    CharacterStore(DSLContext dsl) {
        super(dsl, CHARACTERS, EntityConfigs.Types.CHARACTER);
    }

    public @Nullable CharactersRecord getWithName(String name){
        return ctx.selectFrom(main_table)
                .where(CHARACTERS.NAME.eq(name))
                .fetchOne();
    }

    public @NotNull CharactersRecord @NotNull [] getCharacters(IntSet ids){
        return ctx.selectFrom(main_table)
                .where(CHARACTERS.ID.in(ids))
                .fetch().stream().toArray(CharactersRecord[]::new);
    }
}
