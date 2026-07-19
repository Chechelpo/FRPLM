package io.github.chechelpo.frplm.domain.world.edge;

import io.github.chechelpo.frplm.core.entities.pseudo_services.ABSControllerAwareHelper;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationEdgesRecord;
import io.github.chechelpo.frplm.core.entities.fields.constraints.NumberConstraint;
import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.constraints.StringConstraint;
import io.github.chechelpo.frplm.core.entities.fields.kinds.FieldType;
import org.springframework.stereotype.Component;

import static io.github.chechelpo.frplm.jooq.generated.Tables.LOCATION_EDGES;

@Component
final class EdgeFieldsHelper extends ABSControllerAwareHelper<
        LocationEdgesRecord,
        EdgeService,
        EdgeController
        > {

    EdgeFieldsHelper(
            EdgeService service,
            EdgeController controller
    ) {
        super(service, controller);

        register_field(
                "from_id",
                LOCATION_EDGES.FROM_LOCATION_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(
                                NumberConstraint.builder(FieldType.INTEGER)
                                        .readOnly()
                                        .key()
                                        .build()
                        )
                        .requireOnCreate()
                        .build()
        );

        register_field(
                "to_id",
                LOCATION_EDGES.TO_LOCATION_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(
                                NumberConstraint.builder(FieldType.INTEGER)
                                        .readOnly()
                                        .key()
                                        .build()
                        )
                        .requireOnCreate()
                        .build()
        );

        register_field(
                "world_id",
                LOCATION_EDGES.WORLD_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(
                                NumberConstraint.builder(FieldType.INTEGER)
                                        .readOnly()
                                        .key()
                                        .build()
                        )
                        .requireOnCreate()
                        .build()
        );

        register_field(
                "edge_description",
                LOCATION_EDGES.EDGEDESCRIPTION,
                FieldInfo.stringField()
                        .setConstraints(
                                StringConstraint.builder()
                                        .allows_outlets()
                                        .build()
                        )
                        .build()
        );

        register_field(
                "show_destination_name",
                LOCATION_EDGES.SHOW_DESTINATION_NAME,
                FieldInfo.booleanField()
                        .build()
        );

        register_field(
                "show_destination_description",
                LOCATION_EDGES.SHOW_DESTINATION_DESCRIPTION,
                FieldInfo.booleanField()
                        .build()
        );

        register_field(
                "is_traversable",
                LOCATION_EDGES.TRAVERSABLE,
                FieldInfo.booleanField()
                        .build()
        );
    }
}
