package io.github.chechelpo.frplm.domain.character.core;

import io.github.chechelpo.frplm.annotations.Store;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityStore;
import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.jooq.generated.tables.records.CharactersRecord;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.*;

import java.time.LocalDateTime;
import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.STARTING_LOCATIONS;
import static io.github.chechelpo.frplm.jooq.generated.tables.Characters.CHARACTERS;

@Store
final class CharacterStore extends EntityStore<CharactersRecord> {
    CharacterStore(DSLContext dsl) {
        super(dsl, CHARACTERS, EntityConfigs.Types.CHARACTER);
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

    public @NotNull List<CharactersRecord> getStartingAtLocation(int worldId, int locationId){
        return ctx.select()
                .from(main_table)
                .join(STARTING_LOCATIONS)
                .on(
                        CHARACTERS.ID.eq(STARTING_LOCATIONS.CHARACTER_ID)
                )
                .where(STARTING_LOCATIONS.WORLD_ID.eq(worldId)
                        .and(STARTING_LOCATIONS.LOCATION_ID.eq(locationId))
                )
                .fetchInto(CharactersRecord.class);
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
