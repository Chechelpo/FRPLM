package chechelpo.frplm.domain.lorebook.outlet;

import chechelpo.frplm.annotations.Store;
import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.frameworks.entities.microservices.EntityStore;
import chechelpo.frplm.jooq.generated.tables.records.OutletRecord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.DSLContext;

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
        return ctx.fetchExists(ctx.selectFrom(main_table
                .where(OUTLET.OUTLET_.equal(name)))
        );
    }
}
