package chechelpo.frplm.domain.lorebook.core;

import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.frameworks.entities.pseudo_services.EntityStore;
import chechelpo.frplm.jooq.generated.tables.Lorebooks;
import chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import java.util.List;

import static chechelpo.frplm.jooq.generated.Tables.*;

@Component
final class LorebookStore extends EntityStore<LorebooksRecord> {
    LorebookStore(@NotNull DSLContext ctx) {
        super(ctx, Lorebooks.LOREBOOKS, EntityTypes.Types.LOREBOOKS);
    }

    /**
     * @return a list of lorebooks not associated with characters/locations/worlds
     */
    @NotNull List<LorebooksRecord> getGlobalLorebooks() {
        return ctx.selectFrom(LOREBOOKS)
                .where(LOREBOOKS.ID.notIn(
                        ctx.select(CHARACTERS.LOREBOOK_ID)
                                .from(CHARACTERS)
                                .where(CHARACTERS.LOREBOOK_ID.isNotNull())
                ))
                .and(LOREBOOKS.ID.notIn(
                        ctx.select(LOCATIONS.LOREBOOK_ID)
                                .from(LOCATIONS)
                                .where(LOCATIONS.LOREBOOK_ID.isNotNull())
                ))
                .and(LOREBOOKS.ID.notIn(
                        ctx.select(WORLDS.LOREBOOK_ID)
                                .from(WORLDS)
                                .where(WORLDS.LOREBOOK_ID.isNotNull())
                ))
                .fetch();
    }

    @NotNull List<LorebooksRecord> getLorebooks(IntSet lorebookIDs) {
        return ctx.selectFrom(main_table)
                .where(LOREBOOKS.ID.in(lorebookIDs))
                .fetch();
    }
}
