package chechelpo.frplm.domain.world.edge;

import chechelpo.frplm.frameworks.entities.microservices.ABSControllerAwareHelper;
import chechelpo.frplm.jooq.generated.tables.LocationNeighbors;
import chechelpo.frplm.jooq.generated.tables.records.LocationNeighborsRecord;
import chechelpo.frplm.frameworks.entities.fields.constraints.NumberConstraints;
import chechelpo.frplm.frameworks.entities.fields.FieldInfo;
import chechelpo.frplm.frameworks.entities.fields.constraints.StringConstraints;
import chechelpo.frplm.frameworks.entities.fields.kinds.FieldType;
import org.springframework.stereotype.Component;

@Component
final class EdgeFieldsHelper extends ABSControllerAwareHelper<
        LocationNeighborsRecord,
        EdgeService,
        EdgeController
        > {

    EdgeFieldsHelper(
            EdgeService service,
            EdgeController controller
    ) {
        super(service, controller);

        register_field(
                "location1_id",
                LocationNeighbors.LOCATION_NEIGHBORS.LOCATION1_ID,
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
                "location2_id",
                LocationNeighbors.LOCATION_NEIGHBORS.LOCATION2_ID,
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
                "world_id",
                LocationNeighbors.LOCATION_NEIGHBORS.WORLD_ID,
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
                "description",
                LocationNeighbors.LOCATION_NEIGHBORS.EDGEDESCRIPTION,
                FieldInfo.stringField()
                        .setConstraints(
                                StringConstraints.builder()
                                        .allows_outlets()
                                        .build()
                        )
                        .build()
        );


        register_field(
                "travel_cost",
                LocationNeighbors.LOCATION_NEIGHBORS.TRAVELCOST,
                FieldInfo.numberField(FieldType.LONG)
                        .setConstraints(
                                NumberConstraints.builder(FieldType.LONG)
                                        .setMin(0L)
                                        .build()
                        )
                        .build()
        );
    }
}
