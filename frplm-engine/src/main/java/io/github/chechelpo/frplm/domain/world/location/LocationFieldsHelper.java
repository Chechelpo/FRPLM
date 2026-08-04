package io.github.chechelpo.frplm.domain.world.location;

import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.constraints.StringConstraint;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityControllerFieldValidator;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.LOCATIONS;

@Component
final class LocationFieldsHelper
        extends EntityControllerFieldValidator<LocationsRecord> {

    LocationFieldsHelper() {
        super(EntityConfigs.Types.LOCATIONS);
    }

    @Override
    protected List<DTOField<LocationsRecord, ?>> getDTOStructure() {
        return List.of(
                DTOField.of(LOCATIONS.WORLD_ID, "worldID"),
                DTOField.of(LOCATIONS.ID, "id"),
                DTOField.of(LOCATIONS.REGION_ID, "region_id"),
                DTOField.of(LOCATIONS.NAME, "name"),
                DTOField.of(LOCATIONS.DESCRIPTION, "description"),
                DTOField.of(LOCATIONS.LOREBOOK_ID, "lorebook_id")
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
                        .build(),

                FieldInfo.builder(LOCATIONS.REGION_ID)
                        .nullable()
                        .build(),

                FieldInfo.builder(LOCATIONS.NAME)
                        .setConstraints(
                                StringConstraint.builder()
                                        .setMaxLength(255)
                        )
                        .requireOnCreate()
                        .build(),

                FieldInfo.builder(LOCATIONS.DESCRIPTION)
                        .build(),

                FieldInfo.builder(LOCATIONS.LOREBOOK_ID)
                        .readOnly()
                        .build()
        );
    }
}