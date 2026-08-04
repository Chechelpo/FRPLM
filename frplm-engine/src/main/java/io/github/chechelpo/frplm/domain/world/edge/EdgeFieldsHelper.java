package io.github.chechelpo.frplm.domain.world.edge;

import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.constraints.StringConstraint;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityControllerFieldValidator;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LocationEdgesRecord;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.LOCATION_EDGES;

@Component
final class EdgeFieldsHelper
        extends EntityControllerFieldValidator<LocationEdgesRecord> {

    EdgeFieldsHelper() {
        super(EntityConfigs.Types.EDGES);
    }

    @Override
    protected List<DTOField<LocationEdgesRecord, ?>> getDTOStructure() {
        return List.of(
                DTOField.of(LOCATION_EDGES.FROM_LOCATION_ID, "from_id"),
                DTOField.of(LOCATION_EDGES.TO_LOCATION_ID, "to_id"),
                DTOField.of(LOCATION_EDGES.WORLD_ID, "world_id"),
                DTOField.of(LOCATION_EDGES.EDGEDESCRIPTION, "edge_description"),
                DTOField.of(LOCATION_EDGES.SHOW_DESTINATION_NAME, "show_destination_name"),
                DTOField.of(LOCATION_EDGES.SHOW_DESTINATION_DESCRIPTION, "show_destination_description"),
                DTOField.of(LOCATION_EDGES.TRAVERSABLE, "is_traversable")
        );
    }

    @Override
    protected List<FieldInfo<LocationEdgesRecord, ?>> getCustom() {
        return List.of(
                FieldInfo.builder(LOCATION_EDGES.FROM_LOCATION_ID)
                        .key()
                        .requireOnCreate()
                        .build(),

                FieldInfo.builder(LOCATION_EDGES.TO_LOCATION_ID)
                        .key()
                        .requireOnCreate()
                        .build(),

                FieldInfo.builder(LOCATION_EDGES.WORLD_ID)
                        .key()
                        .requireOnCreate()
                        .build(),

                FieldInfo.builder(LOCATION_EDGES.EDGEDESCRIPTION)
                        .build(),

                FieldInfo.builder(LOCATION_EDGES.SHOW_DESTINATION_NAME)
                        .build(),

                FieldInfo.builder(LOCATION_EDGES.SHOW_DESTINATION_DESCRIPTION)
                        .build(),

                FieldInfo.builder(LOCATION_EDGES.TRAVERSABLE)
                        .build()
        );
    }
}