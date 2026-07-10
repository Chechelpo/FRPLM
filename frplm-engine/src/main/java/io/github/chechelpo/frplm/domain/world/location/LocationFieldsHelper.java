package io.github.chechelpo.frplm.domain.world.location;

import io.github.chechelpo.frplm.core.entities.pseudo_services.ABSControllerAwareHelper;
import io.github.chechelpo.frplm.jooq.generated.tables.Locations;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.constraints.NumberConstraint;
import io.github.chechelpo.frplm.core.entities.fields.constraints.StringConstraint;
import io.github.chechelpo.frplm.core.entities.fields.kinds.FieldType;
import org.springframework.stereotype.Component;

@Component
final class LocationFieldsHelper extends ABSControllerAwareHelper<
        LocationsRecord,
        LocationsService,
        LocationController
        > {
    LocationFieldsHelper(
            LocationsService service,
            LocationController controller
    ) {
        super(service, controller);

        register_field(
                "worldID",
                Locations.LOCATIONS.WORLD_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .readOnly()
                                .key()
                                .build()
                        )
                        .require()
                        .build()
        );

        register_field(
                "id",
                Locations.LOCATIONS.ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .readOnly()
                                .key()
                                .build()
                        )
                        .build()
        );

        register_field(
                "region_id",
                Locations.LOCATIONS.REGION_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .nullable()
                        )
                        .build()
        );

        register_field(
                "name",
                Locations.LOCATIONS.NAME,
                FieldInfo.stringField()
                        .setConstraints(
                                StringConstraint.builder()
                                        .setMaxLength(255)
                                        .build()
                        )
                        .require()
                        .build()
        );

        register_field(
                "lorebook_id",
                Locations.LOCATIONS.LOREBOOK_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(
                                NumberConstraint.builder(FieldType.INTEGER)
                                        .readOnly()
                                        .build()
                        )
                        .build()
        );
    }
}
