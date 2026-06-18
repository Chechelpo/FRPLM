package chechelpo.frplm.domain.sessions.movement;

import chechelpo.frplm.core.entities.fields.FieldInfo;
import chechelpo.frplm.core.entities.fields.constraints.NumberConstraint;
import chechelpo.frplm.core.entities.fields.kinds.FieldType;
import chechelpo.frplm.core.entities.pseudo_services.ABSHelper;
import chechelpo.frplm.jooq.generated.tables.records.ResponseLocationChangesRecord;
import org.springframework.stereotype.Component;

import static chechelpo.frplm.jooq.generated.Tables.RESPONSE_LOCATION_CHANGES;

@Component
public class ResponseMovementFields extends ABSHelper<ResponseLocationChangesRecord, ResponseMovementService> {
    public ResponseMovementFields(ResponseMovementService service) {
        super(service);
        register_field(
                RESPONSE_LOCATION_CHANGES.SESSION_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .key()
                                .readOnly()
                        )
                        .build()
        );
        register_field(
                RESPONSE_LOCATION_CHANGES.TICK_NUM,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .key()
                                .readOnly()
                        )
                        .build()
        );
        register_field(
                RESPONSE_LOCATION_CHANGES.RESPONSE_NUM,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .key()
                                .readOnly()
                        )
                        .build()
        );

        register_field(
                RESPONSE_LOCATION_CHANGES.CHARACTER_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .key()
                                .readOnly()
                        )
                        .build()
        );

        register_field(
                RESPONSE_LOCATION_CHANGES.WORLD_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .build()
        );
        register_field(
                RESPONSE_LOCATION_CHANGES.LOCATION_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .build()
        );
    }
}
