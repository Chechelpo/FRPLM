package io.github.chechelpo.frplm.domain.world.core;

import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.constraints.DoubleConstraint;
import io.github.chechelpo.frplm.core.entities.fields.constraints.StringConstraint;
import io.github.chechelpo.frplm.core.entities.fields.DataPayload;
import io.github.chechelpo.frplm.core.entities.fields.EntityControllerFieldValidator;
import io.github.chechelpo.frplm.core.entities.fields.FieldActionResult;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import io.github.chechelpo.frplm.jooq.generated.tables.records.WorldsRecord;
import org.jetbrains.annotations.Unmodifiable;
import org.jooq.TableField;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static io.github.chechelpo.frplm.jooq.generated.Tables.WORLDS;

@Component
public final class WorldFieldsHelper
        extends EntityControllerFieldValidator<WorldsRecord> {

    WorldFieldsHelper() {
        super(EntityConfigs.Types.WORLDS, WORLDS);
    }

    @Override
    protected List<DTOField<WorldsRecord, ?>> getDTOStructure() {
        return List.of(
                DTOField.of(WORLDS.ID, "id"),
                DTOField.of(WORLDS.NAME, "name"),
                DTOField.of(WORLDS.DESCRIPTION, "description"),
                DTOField.of(WORLDS.LOREBOOK_ID, "lorebook_id"),

                DTOField.of(WORLDS.BACKGROUND_X, "background_x"),
                DTOField.of(WORLDS.BACKGROUND_Y, "background_y"),
                DTOField.of(WORLDS.BACKGROUND_WIDTH, "background_width"),
                DTOField.of(WORLDS.BACKGROUND_HEIGHT, "background_height"),

                DTOField.of(WORLDS.BACKGROUND_OPACITY, "background_opacity"),
                DTOField.of(WORLDS.BACKGROUND_VISIBLE, "background_visible"),
                DTOField.of(WORLDS.BACKGROUND_TRANSFORM_LOCKED, "background_transform_locked"),
                DTOField.of(WORLDS.BACKGROUND_ASPECT_LOCKED, "background_aspect_locked"),
                DTOField.of(WORLDS.BACKGROUND_FIT, "background_fit")
        );
    }

    @Override
    protected @NonNull @Unmodifiable List<FieldInfo<WorldsRecord, ?>> getCustom() {
        return List.of(
                FieldInfo.builder(WORLDS.ID)
                        .key()
                        .build(),

                FieldInfo.builder(WORLDS.NAME)
                        .setConstraints(
                                StringConstraint.builder()
                                        .setMaxLength(255)
                        )
                        .build(),

                FieldInfo.builder(WORLDS.DESCRIPTION)
                        .build(),

                FieldInfo.builder(WORLDS.LOREBOOK_ID)
                        .build(),

                FieldInfo.builder(WORLDS.BACKGROUND_X)
                        .nullable()
                        .build(),

                FieldInfo.builder(WORLDS.BACKGROUND_Y)
                        .nullable()
                        .build(),

                FieldInfo.builder(WORLDS.BACKGROUND_WIDTH)
                        .nullable()
                        .addCustomConstraint(value ->
                                value > 0.0
                                        ? Optional.empty()
                                        : Optional.of("background_width must be greater than 0")
                        )
                        .build(),

                FieldInfo.builder(WORLDS.BACKGROUND_HEIGHT)
                        .nullable()
                        .addCustomConstraint(value ->
                                value > 0.0
                                        ? Optional.empty()
                                        : Optional.of("background_height must be greater than 0")
                        )
                        .build(),

                FieldInfo.builder(WORLDS.BACKGROUND_OPACITY)
                        .setConstraints(
                                DoubleConstraint.builder()
                                        .setMax(1.0)
                                        .setMin(0.0)
                        )
                        .build(),

                FieldInfo.builder(WORLDS.BACKGROUND_VISIBLE)
                        .build(),

                FieldInfo.builder(WORLDS.BACKGROUND_TRANSFORM_LOCKED)
                        .build(),

                FieldInfo.builder(WORLDS.BACKGROUND_ASPECT_LOCKED)
                        .build(),

                FieldInfo.builder(WORLDS.BACKGROUND_FIT)
                        .setDefaultValue("CONTAIN")
                        .addAllowedValues("CONTAIN", "COVER")
                        .build(),

                FieldInfo.builder(WORLDS.NEXT_LOCATION_ID)
                        .build(),

                FieldInfo.builder(WORLDS.NEXT_REGION_ID)
                        .build()
        );
    }

    @Override
    protected <D extends DataPayload<WorldsRecord>> FieldActionResult<WorldsRecord, D> validateCustom(D payload) {
        boolean assignsX = payload.assigns(WORLDS.BACKGROUND_X);
        boolean assignsY = payload.assigns(WORLDS.BACKGROUND_Y);
        boolean assignsWidth = payload.assigns(WORLDS.BACKGROUND_WIDTH);
        boolean assignsHeight = payload.assigns(WORLDS.BACKGROUND_HEIGHT);

        boolean assignsAnyGeometry =
                assignsX
                        || assignsY
                        || assignsWidth
                        || assignsHeight;

        // No background geometry is being created or modified.
        if (!assignsAnyGeometry) {
            return FieldActionResult.success(payload);
        }

        boolean assignsAllGeometry =
                assignsX
                        && assignsY
                        && assignsWidth
                        && assignsHeight;

        /*
         * Geometry is treated as an atomic group. This prevents a partial update
         * from producing a state that violates chk_world_background_geometry.
         */
        if (!assignsAllGeometry) {
            TableField<WorldsRecord, ?> missingField =
                    !assignsX
                            ? WORLDS.BACKGROUND_X
                            : !assignsY
                              ? WORLDS.BACKGROUND_Y
                              : !assignsWidth
                                ? WORLDS.BACKGROUND_WIDTH
                                : WORLDS.BACKGROUND_HEIGHT;

            return FieldActionResult.missingField(
                    """
                    Background geometry must be assigned atomically. \
                    Assign background_x, background_y, background_width and \
                    background_height together.
                    """,
                    missingField,
                    payload
            );
        }

        Double x = payload.require(WORLDS.BACKGROUND_X);
        Double y = payload.require(WORLDS.BACKGROUND_Y);
        Double width = payload.require(WORLDS.BACKGROUND_WIDTH);
        Double height = payload.require(WORLDS.BACKGROUND_HEIGHT);

        boolean allNull =
                x == null
                        && y == null
                        && width == null
                        && height == null;

        boolean allNonNull =
                x != null
                        && y != null
                        && width != null
                        && height != null;

        if (!allNull && !allNonNull) {
            TableField<WorldsRecord, Double> inconsistentField;

            if (x == null) inconsistentField = WORLDS.BACKGROUND_X;
            else if (y == null) inconsistentField = WORLDS.BACKGROUND_Y;
            else if (width == null) inconsistentField = WORLDS.BACKGROUND_WIDTH;
            else inconsistentField = WORLDS.BACKGROUND_HEIGHT;


            return FieldActionResult.wrongValue(
                    """
                    Invalid background geometry. background_x, background_y, \
                    background_width and background_height must either all be null \
                    or all be non-null.
                    """,
                    inconsistentField,
                    null,
                    payload
            );
        }

        return FieldActionResult.success(payload);
    }
}