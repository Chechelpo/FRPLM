package chechelpo.frplm.domain.character.starting_locations;

import chechelpo.frplm.frameworks.entities.microservices.ABSFieldInstantiationHelper;
import chechelpo.frplm.jooq.generated.tables.StartingLocations;
import chechelpo.frplm.jooq.generated.tables.records.StartingLocationsRecord;
import chechelpo.frplm.frameworks.entities.fields.constraints.BoolConstraints;
import chechelpo.frplm.frameworks.entities.fields.constraints.NumberConstraints;
import chechelpo.frplm.frameworks.entities.fields.format.BoolFormat;
import chechelpo.frplm.frameworks.entities.fields.FieldInfo;
import chechelpo.frplm.frameworks.entities.fields.kinds.FieldType;
import org.springframework.stereotype.Component;

@Component
public final class StartingLocationFieldsHelper extends ABSFieldInstantiationHelper<
        StartingLocationsRecord,
        StartingLocationsStore,
        StartingLocationsService,
        StartingLocationsController
>
{
    StartingLocationFieldsHelper(
            StartingLocationsStore store,
            StartingLocationsService service,
            StartingLocationsController controller
    ) {
        super(store, service, controller);

        register_field(
                "worldID",
                StartingLocations.STARTING_LOCATIONS.WORLD_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(
                                NumberConstraints.builder(FieldType.INTEGER)
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
                                NumberConstraints.builder(FieldType.INTEGER)
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
                                NumberConstraints.builder(FieldType.INTEGER)
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
                                new BoolConstraints(false)
                        )
                        .setFormat(
                                new BoolFormat("Whether this character is permitted to move locations")
                        )
                        .build()
        );
    }
}
