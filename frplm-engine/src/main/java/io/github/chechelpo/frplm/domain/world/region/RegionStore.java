package io.github.chechelpo.frplm.domain.world.region;

import io.github.chechelpo.frplm.annotations.Store;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityStore;
import io.github.chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.REGION;

@Store
final class RegionStore extends EntityStore<RegionRecord> {
    RegionStore(@NotNull DSLContext ctx) {
        super(ctx, REGION, EntityConfigs.Types.REGIONS);
    }

    public List<RegionRecord> getDepthOneChildrenOf(RegionRecord record){
        return ctx.selectFrom(main_table)
                .where(
                        REGION.WORLD_ID.eq(record.getWorldId())
                                .and(REGION.PARENT_REGION_ID.eq(record.getId()))
                )
                .fetch();
    }

    public List<RegionRecord> getRoots(int worldId){
        return ctx.selectFrom(main_table)
                .where(
                        REGION.WORLD_ID.eq(worldId)
                                .and(REGION.PARENT_REGION_ID.isNull())
                )
                .fetch();
    }
}
