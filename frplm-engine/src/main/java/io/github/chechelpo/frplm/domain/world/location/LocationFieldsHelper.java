package io.github.chechelpo.frplm.domain.world.location;

import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.constraints.StringConstraint;
import io.github.chechelpo.frplm.core.entities.fields.EntityControllerFieldValidator;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static io.github.chechelpo.frplm.jooq.generated.Tables.LOCATIONS;

@Component
final class LocationFieldsHelper
        extends EntityControllerFieldValidator<LocationsRecord> {

    LocationFieldsHelper() {
        super(EntityConfigs.Types.LOCATIONS, LOCATIONS);
    }

    @Override
    protected List<DTOField<LocationsRecord, ?>> getDTOStructure() {
        return List.of(
                DTOField.of(LOCATIONS.WORLD_ID, "worldID"),
                DTOField.of(LOCATIONS.ID, "id"),
                DTOField.of(LOCATIONS.REGION_ID, "region_id"),
                DTOField.of(LOCATIONS.NAME, "name"),
                DTOField.of(LOCATIONS.DESCRIPTION, "description"),
                DTOField.of(LOCATIONS.LOREBOOK_ID, "lorebook_id"),
                DTOField.of(LOCATIONS.LOCKED, "locked"),
                DTOField.of(LOCATIONS.X, "x"),
                DTOField.of(LOCATIONS.Y, "y"),
                DTOField.of(LOCATIONS.RADIUS, "radius")
        );
    }

    @Override
    protected List<FieldInfo<LocationsRecord, ?>> getCustom() {
        return List.of(
                FieldInfo.builder(LOCATIONS.WORLD_ID)
                        .key()
                        .requireOnCreate()
                        .build(),

                FieldInfo.builder(LOCATIONS.ID)
                        .key()
                        .requireOnCreate()
                        .build(),

                FieldInfo.builder(LOCATIONS.REGION_ID)
                        .requireOnCreate()
                        .build(),

                FieldInfo.builder(LOCATIONS.NAME)
                        .setConstraints(
                                StringConstraint.builder()
                                        .setMaxLength(255)
                        )
                        .addCustomConstraint(name ->
                                name.isBlank()
                                        ? Optional.of("name must not be blank")
                                        : Optional.empty()
                        )
                        .requireOnCreate()
                        .build(),

                FieldInfo.builder(LOCATIONS.DESCRIPTION)
                        .build(),

                FieldInfo.builder(LOCATIONS.LOREBOOK_ID)
                        .readOnly()
                        .requireOnCreate()
                        .build(),

                FieldInfo.builder(LOCATIONS.X)
                        .addCustomConstraint(value ->
                                Double.isFinite(value)
                                        ? Optional.empty()
                                        : Optional.of("x must be finite")
                        )
                        .build(),

                FieldInfo.builder(LOCATIONS.Y)
                        .addCustomConstraint(value ->
                                Double.isFinite(value)
                                        ? Optional.empty()
                                        : Optional.of("y must be finite")
                        )
                        .build(),

                FieldInfo.builder(LOCATIONS.RADIUS)
                        .addCustomConstraint(value -> {
                            if (!Double.isFinite(value)) {
                                return Optional.of("radius must be finite");
                            }

                            if (value < 0.0) {
                                return Optional.of("radius must not be negative");
                            }

                            return Optional.empty();
                        })
                        .build()
        );
    }
}