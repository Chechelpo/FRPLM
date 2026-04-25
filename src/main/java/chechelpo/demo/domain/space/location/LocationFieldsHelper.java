package chechelpo.demo.domain.space.location;

import chechelpo.demo.frameworks.entities.microservices.ABSFieldInstantiationHelper;
import chechelpo.demo.jooq.generated.tables.Locations;
import chechelpo.demo.jooq.generated.tables.records.LocationsRecord;
import chechelpo.demo.frameworks.entities.fields.FieldInfo;
import chechelpo.demo.frameworks.entities.fields.constraints.NumberConstraints;
import chechelpo.demo.frameworks.entities.fields.constraints.StringConstraints;
import chechelpo.demo.frameworks.entities.fields.format.StringFormat;
import chechelpo.demo.frameworks.entities.fields.kinds.FieldType;
import org.springframework.stereotype.Component;

@Component
final class LocationFieldsHelper extends ABSFieldInstantiationHelper<
        LocationsRecord,
        LocationStore,
        LocationsService,
        LocationController
        > {
    LocationFieldsHelper(
            LocationStore store,
            LocationsService service,
            LocationController controller
    ) {
        super(store, service, controller);

        register_field(
                "worldID",
                Locations.LOCATIONS.WORLD_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraints.builder(FieldType.INTEGER)
                                .readOnly()
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
                        .setFormat(
                                StringFormat.builder()
                                        .setInfo("Location's name")
                                        .setType(StringFormat.Types.SHORT_TEXT)
                                        .build()
                        )
                        .require()
                        .build()
        );

        register_field(
                "description",
                Locations.LOCATIONS.DESCRIPTION,
                FieldInfo.stringField()
                        .setConstraints(
                                StringConstraints.builder()
                                        .allows_outlets()
                                        .build()
                        )
                        .setFormat(
                                StringFormat.builder()
                                        .setInfo("Text to be injected when inside the location. Good for phyiscal grounding")
                                        .setType(StringFormat.Types.LONG_TEXT)
                                        .build()
                        )
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
