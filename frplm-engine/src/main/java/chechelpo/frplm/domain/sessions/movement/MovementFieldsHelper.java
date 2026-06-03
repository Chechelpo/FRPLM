package chechelpo.frplm.domain.sessions.movement;

import chechelpo.frplm.frameworks.entities.fields.FieldInfo;
import chechelpo.frplm.frameworks.entities.fields.constraints.NumberConstraint;
import chechelpo.frplm.frameworks.entities.fields.kinds.FieldType;
import chechelpo.frplm.frameworks.entities.pseudo_services.ABSHelper;
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
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .key()
                                .readOnly()
                        )
                        .require()
                        .build()
        );

        register_field(
                MOVEMENTS.AT_TICK,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .key()
                                .readOnly()
                        )
                        .build()
        );

        register_field(
                MOVEMENTS.WORLD_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .readOnly()
                        )
                        .build()
        );
    }
}
