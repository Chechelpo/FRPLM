package io.github.chechelpo.frplm.domain.world.region;

import io.github.chechelpo.frplm.core.entities.fields.EntityControllerFieldValidator;
import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.constraints.DoubleConstraint;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import io.github.chechelpo.frplm.jooq.generated.tables.records.RegionRecord;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static io.github.chechelpo.frplm.jooq.generated.Tables.REGION;

@Component
public final class RegionFields
        extends EntityControllerFieldValidator<RegionRecord> {

    public RegionFields() {
        super(EntityConfigs.Types.REGIONS, REGION);
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
                // Application-level policy; not implied by the SQL schema.
                FieldInfo.builder(REGION.LOREBOOK_ID)
                        .readOnly()
                        .build(),

                // CHECK (width > 0)
                FieldInfo.builder(REGION.WIDTH)
                        .addCustomConstraint(value ->
                                value > 0.0
                                        ? Optional.empty()
                                        : Optional.of("Width must be greater than 0")
                        )
                        .build(),

                // CHECK (height > 0)
                FieldInfo.builder(REGION.HEIGHT)
                        .addCustomConstraint(value ->
                                value > 0.0
                                        ? Optional.empty()
                                        : Optional.of("Height must be greater than 0")
                        )
                        .build(),

                // CHECK (
                //     background_opacity >= 0.0
                //     AND background_opacity <= 1.0
                // )
                FieldInfo.builder(REGION.BACKGROUND_OPACITY)
                        .setConstraints(
                                DoubleConstraint.builder()
                                        .setMin(0.0)
                                        .setMax(1.0)
                        )
                        .build(),

                // CHECK (background_fit IN ('CONTAIN', 'COVER'))
                FieldInfo.builder(REGION.BACKGROUND_FIT)
                        .addAllowedValues("CONTAIN", "COVER")
                        .build()
        );
    }
}