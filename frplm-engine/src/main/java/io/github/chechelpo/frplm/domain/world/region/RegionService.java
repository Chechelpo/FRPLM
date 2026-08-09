package io.github.chechelpo.frplm.domain.world.region;

import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityService;
import io.github.chechelpo.frplm.core.entities.fields.FieldValidator;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookService;
import io.github.chechelpo.frplm.domain.world.core.WorldService;
import io.github.chechelpo.frplm.events.EventBus;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.exceptions.runtime.InvalidValue;
import io.github.chechelpo.frplm.exceptions.runtime.UnexpectedException;
import io.github.chechelpo.frplm.exceptions.runtime.UnsupportedAction;
import io.github.chechelpo.frplm.jooq.generated.tables.Lorebooks;
import io.github.chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.*;

@Component
public class RegionService extends EntityService<RegionRecord, RegionStore> {
    private final LorebookService lorebookService;
    private final WorldService worldService;

    RegionService(
            @NonNull RegionStore store,
            FieldValidator<RegionRecord> validator,
            @NotNull EventBus eventBus,
            LorebookService lorebookService,
            WorldService worldService
    ) {
        super(store, validator, eventBus);
        this.lorebookService = lorebookService;
        this.worldService = worldService;
    }

    @Override
    protected void beforeCreate(@NonNull EntityDataPayload<RegionRecord> data, long operationID) {
        if (!data.assigns(REGION.LOREBOOK_ID)) {
            int lorebookId = lorebookService.createAndGet(
                    EntityDataPayload.of(Lorebooks.LOREBOOKS.NAME, data.require(REGION.NAME)),
                    LOREBOOKS.ID
            );
            data.set(REGION.LOREBOOK_ID, lorebookId);
        }
        data.set(
                REGION.ID,
                worldService.incrementAndGet(WORLDS.NEXT_REGION_ID, worldService.keyOf(data.require(REGION.WORLD_ID)))
                        .orElseThrow(() -> new UnexpectedException("Could not fetch id for region " + data, Severity.SYSTEM))
        );

        super.beforeCreate(data, operationID);
    }

    @Override
    protected void beforeUpdate(@NotNull EntityKey<RegionRecord> target, EntityDataPayload<RegionRecord> data, long operationID) {
        if (data.assigns(REGION.PARENT_REGION_ID) && data.require(REGION.PARENT_REGION_ID) != null) {
            if (updateCreatesCycle(
                    target.require(REGION.WORLD_ID),
                    target.require(REGION.ID),
                    data.require(REGION.PARENT_REGION_ID))
            ) throw new InvalidValue("This parent region assignment will create a cycle");
        }
        super.beforeUpdate(target, data, operationID);
    }
    @SuppressWarnings("SpringTransactionalMethodCallsInspection")
    private boolean updateCreatesCycle(int worldId, int fromRegionId, int newParent) {
        RegionRecord currentRegion = this.find(getKey(worldId, newParent))
                .orElseThrow(Severity.SYSTEM);
        while (currentRegion.getParentRegionId() != null) {
            if (currentRegion.getParentRegionId() == fromRegionId) return true;
            currentRegion = this.find(getKey(worldId, currentRegion.getParentRegionId()))
                    .orElseThrow(Severity.SYSTEM);
        }

        return false;
    }

    @Override
    @SuppressWarnings("SpringTransactionalMethodCallsInspection")
    protected void beforeDelete(EntityKey<RegionRecord> id, long operationID) {
        RegionRecord toDelete = this.find(id).orElseThrow(Severity.USER);
        if (!getDepthOneChildrenOf(toDelete).isEmpty())
            throw new UnsupportedAction("Cannot delete a region when it has children", Severity.EXPECTED);

        super.beforeDelete(id, operationID);
    }

    @Override
    protected void afterSuccessfulDelete(EntityKey<RegionRecord> id, long operationID, @NonNull RegionRecord record) {
        boolean lorebookDeleted = lorebookService.delete(lorebookService.keyOf(record.getLorebookId()));
        if (!lorebookDeleted)
            log.error("Could not delete associated lorebook when deleting \n {}", record);

        super.afterSuccessfulDelete(id, operationID, record);
    }

    public List<RegionRecord> getDepthOneChildrenOf(RegionRecord record){
        return store.getDepthOneChildrenOf(record);
    }

    public List<RegionRecord> getRoots(int worldId){
        return store.getRoots(worldId);
    }

    private static EntityKey<RegionRecord> getKey(int worldId, int fromRegionId) {
        return EntityKey.<RegionRecord>builder()
                .set(REGION.WORLD_ID, worldId)
                .set(REGION.ID, fromRegionId)
                .build();
    }
}
