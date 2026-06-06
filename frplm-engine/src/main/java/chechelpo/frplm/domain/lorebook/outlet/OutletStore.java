package chechelpo.frplm.domain.lorebook.outlet;

import chechelpo.frplm.annotations.Store;
import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.core.entities.pseudo_services.EntityStore;
import chechelpo.frplm.jooq.generated.tables.records.OutletRecord;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.DSLContext;

import static chechelpo.frplm.jooq.generated.Tables.ENTRY;
import static chechelpo.frplm.jooq.generated.Tables.OUTLET;

@Store
final class OutletStore extends EntityStore<OutletRecord> {
    OutletStore(@NotNull DSLContext ctx) {
        super(ctx, OUTLET, EntityTypes.Types.OUTLET);
    }

    public @Nullable Integer getOfName(@NotNull String name) {
        return ctx.selectFrom(main_table)
                .where(OUTLET.OUTLET_.equal(name))
                .fetchOne(OUTLET.ID);
    }

    public boolean existsName(@NotNull String name) {
        return ctx.fetchExists(
                ctx.selectOne()
                        .from(main_table)
                        .where(OUTLET.OUTLET_.eq(name))
        );
    }

    public IntObjectPair<String> @NotNull [] getOutletsOfLorebooks(IntSet lorebookIDs){
        return ctx.selectDistinct(OUTLET.OUTLET_, OUTLET.ID)
                .from(OUTLET)
                .join(ENTRY)
                .on(
                        ENTRY.OUTLET.eq(OUTLET.ID)
                                .and(ENTRY.LOREBOOK_ID.in(lorebookIDs))
                )
                .stream()
                .map(result -> IntObjectPair.of(result.getValue(OUTLET.ID), result.getValue(OUTLET.OUTLET_)))
                .toArray(IntObjectPair[]::new);
    }
}
