package chechelpo.demo.domain.space.edge;

import chechelpo.demo.frameworks.entities.microservices.ABSFieldInstantiationHelper;
import chechelpo.demo.jooq.generated.tables.LocationNeighbors;
import chechelpo.demo.jooq.generated.tables.records.LocationNeighborsRecord;
import chechelpo.demo.frameworks.entities.fields.constraints.NumberConstraints;
import chechelpo.demo.frameworks.entities.fields.FieldInfo;
import chechelpo.demo.frameworks.entities.fields.constraints.StringConstraints;
import chechelpo.demo.frameworks.entities.fields.format.NumberFormat;
import chechelpo.demo.frameworks.entities.fields.format.StringFormat;
import chechelpo.demo.frameworks.entities.fields.kinds.FieldType;
import org.springframework.stereotype.Component;

@Component
final class EdgeFieldsHelper extends ABSFieldInstantiationHelper<
        LocationNeighborsRecord,
        EdgeStore,
        EdgeService,
        EdgeController
        > {

    EdgeFieldsHelper(
            EdgeStore store,
            EdgeService service,
            EdgeController controller
    ) {
        super(store, service, controller);

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
                        .setFormat(
                                NumberFormat.builder()
                                        .setInfo("Next edge")
                                        .setType(NumberFormat.Types.INPUT)
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
                        .setFormat(
                                StringFormat.builder()
                                        .setInfo("Description of the edge, injected constantly in both the next and current location")
                                        .setType(StringFormat.Types.LONG_TEXT)
                                        .build()
                        )
                        .build()
        );


        register_field(
                "travel_cost",
                LocationNeighbors.LOCATION_NEIGHBORS.TRAVELCOST,
                FieldInfo.numberField(FieldType.LONG)
                        .setFormat(
                                NumberFormat.builder()
                                        .setInfo("How many hours does it take to traverse?")
                                        .setType(NumberFormat.Types.INPUT)
                                        .build()
                        )
                        .setConstraints(
                                NumberConstraints.builder(FieldType.LONG)
                                        .setMin(0L)
                                        .build()
                        )
                        .build()
        );
    }
}
