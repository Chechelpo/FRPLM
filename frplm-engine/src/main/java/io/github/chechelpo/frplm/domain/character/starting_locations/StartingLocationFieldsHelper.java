package io.github.chechelpo.frplm.domain.character.starting_locations;

import io.github.chechelpo.frplm.core.entities.pseudo_services.ABSControllerAwareHelper;
import chechelpo.frplm.jooq.generated.tables.StartingLocations;
import chechelpo.frplm.jooq.generated.tables.records.StartingLocationsRecord;
import io.github.chechelpo.frplm.core.entities.fields.constraints.BoolConstraint;
import io.github.chechelpo.frplm.core.entities.fields.constraints.NumberConstraint;
import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.kinds.FieldType;
import org.springframework.stereotype.Component;

@Component
public final class StartingLocationFieldsHelper extends ABSControllerAwareHelper<
        StartingLocationsRecord,
        StartingLocationsService,
        StartingLocationsController
>
{
    StartingLocationFieldsHelper(
            StartingLocationsService service,
            StartingLocationsController controller
    ) {
        super(service, controller);

        register_field(
                "worldID",
                StartingLocations.STARTING_LOCATIONS.WORLD_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(
                                NumberConstraint.builder(FieldType.INTEGER)
                                        .readOnly()
                                        .key()
                                        .build()
                        )
                        .require()
                        .build()

        );
        register_field(
                "characterID",
                StartingLocations.STARTING_LOCATIONS.CHARACTER_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(
                                NumberConstraint.builder(FieldType.INTEGER)
                                        .readOnly()
                                        .key()
                                        .build()
                        )
                        .require()
                        .build()
        );

        register_field(
                "locationID",
                StartingLocations.STARTING_LOCATIONS.LOCATION_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(
                                NumberConstraint.builder(FieldType.INTEGER)
                                        .readOnly()
                                        .key()
                                        .build()
                        )
                        .require()
                        .build()
        );
        register_field(
                "reason_why",
                StartingLocations.STARTING_LOCATIONS.REASON_WHY,
                FieldInfo.stringField()
                        .build()

        );


        register_field(
                "is_static",
                StartingLocations.STARTING_LOCATIONS.IS_STATIC,
                FieldInfo.booleanField()
                        .setConstraints(
                                BoolConstraint.builder()
                        )
                        .build()
        );
        register_field(
                "ttl",
                StartingLocations.STARTING_LOCATIONS.TTL,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .setMin(0L)
                                .build()
                        )
                        .build()
        );
    }
}
