package chechelpo.frplm.domain.sessions.movement.history;

import chechelpo.frplm.frameworks.entities.fields.FieldInfo;
import chechelpo.frplm.frameworks.entities.fields.constraints.NumberConstraints;
import chechelpo.frplm.frameworks.entities.fields.kinds.FieldType;
import chechelpo.frplm.frameworks.entities.microservices.ABSHelper;
import chechelpo.frplm.jooq.generated.tables.records.MovementsRecord;
import org.springframework.stereotype.Component;

import static chechelpo.frplm.jooq.generated.Tables.MOVEMENTS;

@Component
final class MovementFieldsHelper extends ABSHelper<MovementsRecord, MovementService> {
    MovementFieldsHelper(MovementService service) {
        super(service);

        register_field(
                MOVEMENTS.SESSION_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraints.builder(FieldType.INTEGER)
                                .key()
                                .readOnly()
                        )
                        .require()
                        .build()
        );

        register_field(
                MOVEMENTS.CURRENT_TICK,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraints.builder(FieldType.INTEGER)
                                .key()
                                .readOnly()
                        )
                        .build()
        );

        register_field(
                MOVEMENTS.WORLD_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraints.builder(FieldType.INTEGER)
                                .readOnly()
                        )
                        .build()
        );
    }
}
