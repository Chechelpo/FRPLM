package chechelpo.frplm.domain.world.location;

import chechelpo.frplm.frameworks.entities.pseudo_services.ABSControllerAwareHelper;
import chechelpo.frplm.jooq.generated.tables.Locations;
import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import chechelpo.frplm.frameworks.entities.fields.FieldInfo;
import chechelpo.frplm.frameworks.entities.fields.constraints.NumberConstraint;
import chechelpo.frplm.frameworks.entities.fields.constraints.StringConstraint;
import chechelpo.frplm.frameworks.entities.fields.kinds.FieldType;
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
