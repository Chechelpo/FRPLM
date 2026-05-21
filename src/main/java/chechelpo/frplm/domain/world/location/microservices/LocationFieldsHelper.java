package chechelpo.frplm.domain.world.location.microservices;

import chechelpo.frplm.frameworks.entities.microservices.ABSControllerAwareHelper;
import chechelpo.frplm.jooq.generated.tables.Locations;
import chechelpo.frplm.jooq.generated.tables.records.LocationsRecord;
import chechelpo.frplm.frameworks.entities.fields.FieldInfo;
import chechelpo.frplm.frameworks.entities.fields.constraints.NumberConstraints;
import chechelpo.frplm.frameworks.entities.fields.constraints.StringConstraints;
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
                        .setConstraints(NumberConstraints.builder(FieldType.INTEGER)
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
                        .setConstraints(NumberConstraints.builder(FieldType.INTEGER)
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
                                StringConstraints.builder()
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
                                NumberConstraints.builder(FieldType.INTEGER)
                                        .readOnly()
                                        .build()
                        )
                        .build()
        );
    }
}
