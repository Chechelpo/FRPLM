package io.github.chechelpo.frplm.domain.world.region;

import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.EntityControllerFieldValidator;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import io.github.chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.REGION;

@Component
final class RegionFields extends EntityControllerFieldValidator<RegionRecord> {

    RegionFields() {
        super(EntityConfigs.Types.REGIONS);
    }

    @Override
    protected List<DTOField<RegionRecord, ?>> getDTOStructure() {
        return List.of(
                DTOField.of(REGION.WORLD_ID, "world_id"),
                DTOField.of(REGION.ID, "id"),
                DTOField.of(REGION.PARENT_REGION_ID, "parent_region_id"),
                DTOField.of(REGION.LOREBOOK_ID, "lorebook_id"),
                DTOField.of(REGION.NAME, "name"),
                DTOField.of(REGION.DESCRIPTION, "description"),

                DTOField.of(REGION.LOCKED, "locked"),
                DTOField.of(REGION.X, "x"),
                DTOField.of(REGION.Y, "y"),
                DTOField.of(REGION.WIDTH, "width"),
                DTOField.of(REGION.HEIGHT, "height"),

                DTOField.of(REGION.BACKGROUND_OPACITY, "background_opacity"),
                DTOField.of(REGION.BACKGROUND_VISIBLE, "background_visible"),
                DTOField.of(
                        REGION.BACKGROUND_ASPECT_LOCKED,
                        "background_aspect_locked"
                ),
                DTOField.of(REGION.BACKGROUND_FIT, "background_fit"),

                DTOField.of(REGION.COLLAPSED, "collapsed")
        );
    }

    @Override
    protected List<FieldInfo<RegionRecord, ?>> getCustom() {
        return List.of(
                FieldInfo.builder(REGION.WORLD_ID)
                        .key()
                        .build(),

                FieldInfo.builder(REGION.ID)
                        .key()
                        .build(),

                FieldInfo.builder(REGION.PARENT_REGION_ID)
                        .nullable()
                        .build(),

                FieldInfo.builder(REGION.LOREBOOK_ID)
                        .readOnly()
                        .build(),

                FieldInfo.builder(REGION.NAME)
                        .requireOnCreate()
                        .build(),

                FieldInfo.builder(REGION.DESCRIPTION)
                        .build()
        );
    }
}

