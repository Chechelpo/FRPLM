package chechelpo.frplm.domain.world.region;

import chechelpo.frplm.annotations.Store;
import chechelpo.frplm.core.entities.pseudo_services.EntityStore;
import chechelpo.frplm.domain.EntityTypes;
import chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import java.util.List;

import static chechelpo.frplm.jooq.generated.Tables.REGION;

@Store
final class RegionStore extends EntityStore<RegionRecord> {
    RegionStore(@NotNull DSLContext ctx) {
        super(ctx, REGION, EntityTypes.Types.REGIONS);
    }

    public List<RegionRecord> getDepthOneChildrenOf(RegionRecord record){
        return ctx.selectFrom(main_table)
                .where(
                        REGION.WORLD_ID.eq(record.getWorldId())
                                .and(REGION.PARENT_REGION_ID.eq(record.getId()))
                )
                .fetch();
    }
}
