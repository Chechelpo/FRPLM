package io.github.chechelpo.frplm.domain.world.region;

import io.github.chechelpo.frplm.core.dispatch.readers.EntityReaders;
import io.github.chechelpo.frplm.core.entities.assets.EntityAssetStore;
import io.github.chechelpo.frplm.core.entities.fields.EntityDataPayload;
import io.github.chechelpo.frplm.core.entities.fields.EntityKey;
import io.github.chechelpo.frplm.core.entities.mappers.ABSWireMapper;
import io.github.chechelpo.frplm.core.entities.mappers.EntityWireMapper;
import io.github.chechelpo.frplm.domain.lorebook.core.LorebookJSON;
import io.github.chechelpo.frplm.exceptions.Severity;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LorebooksRecord;
import io.github.chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import io.github.chechelpo.frplm.utils.IO.ZipBuilder;
import io.github.chechelpo.frplm.utils.orders.NewLorebookOrder;
import io.github.chechelpo.frplm.utils.orders.NewRegionOrder;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import static io.github.chechelpo.frplm.jooq.generated.Tables.LOREBOOKS;
import static io.github.chechelpo.frplm.jooq.generated.Tables.REGION;

@Component
final class RegionMapper extends ABSWireMapper<RegionRecord, RegionJSON, NewRegionOrder> {
    private final EntityReaders entityReaders;
    private final EntityWireMapper<LorebooksRecord, LorebookJSON, NewLorebookOrder> lorebookMapper;

    RegionMapper(
            ObjectMapper mapper,
            EntityReaders entityReaders,
            EntityWireMapper<LorebooksRecord, LorebookJSON, NewLorebookOrder> lorebookMapper,
            EntityAssetStore<RegionRecord, ?> assetStore
    ) {
        super(mapper, RegionJSON.class, assetStore);
        this.entityReaders = entityReaders;
        this.lorebookMapper = lorebookMapper;
    }

    @Override
    protected String getZipPath(RegionJSON json) {
        return "regions/%s".formatted(json.name());
    }

    @Override
    protected RegionJSON internalRecordFrom(
            @NonNull RegionRecord record,
            @NonNull ZipBuilder zipBuilder
    ) {
        String parentRegionName = null;

        if (record.getParentRegionId() != null) {
            parentRegionName = entityReaders.regions()
                    .find(
                            EntityKey.<RegionRecord>builder()
                                    .set(REGION.WORLD_ID, record.getWorldId())
                                    .set(REGION.ID, record.getParentRegionId())
                                    .build()
                    )
                    .orElseThrow(
                            "No parent region with id: " + record.getParentRegionId(),
                            Severity.SYSTEM
                    )
                    .getName();
        }

        return new RegionJSON(
                record.getName(),
                record.getDescription(),
                parentRegionName,

                record.getLocked(),

                record.getX(),
                record.getY(),
                record.getWidth(),
                record.getHeight(),

                record.getBackgroundOpacity(),
                record.getBackgroundVisible(),
                record.getBackgroundAspectLocked(),
                record.getBackgroundFit(),

                record.getCollapsed(),

                lorebookMapper.jsonRecordFrom(
                        entityReaders.lorebooks().require(
                                EntityKey.of(
                                        LOREBOOKS.ID,
                                        record.getLorebookId()
                                )
                        ),
                        zipBuilder
                )
        );
    }

    @Override
    protected NewRegionOrder internalOrderFrom(@NonNull RegionJSON json) {
        return new NewRegionOrder(
                getZipPath(json),

                EntityDataPayload.<RegionRecord>builder()
                        .set(REGION.NAME, json.name())
                        .set(REGION.DESCRIPTION, json.description())

                        .set(REGION.LOCKED, json.locked())

                        .set(REGION.X, json.x())
                        .set(REGION.Y, json.y())
                        .set(REGION.WIDTH, json.width())
                        .set(REGION.HEIGHT, json.height())

                        .set(REGION.BACKGROUND_OPACITY, json.backgroundOpacity())
                        .set(REGION.BACKGROUND_VISIBLE, json.backgroundVisible())
                        .set(REGION.BACKGROUND_ASPECT_LOCKED, json.backgroundAspectLocked())
                        .set(REGION.BACKGROUND_FIT, json.backgroundFit())

                        .set(REGION.COLLAPSED, json.collapsed())

                        .build(),

                lorebookMapper.orderFrom(json.lorebook()),
                json.parent_name()
        );
    }
}

