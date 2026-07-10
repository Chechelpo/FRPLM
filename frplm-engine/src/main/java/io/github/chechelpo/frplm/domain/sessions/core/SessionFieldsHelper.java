package io.github.chechelpo.frplm.domain.sessions.core;

import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.constraints.NumberConstraint;
import io.github.chechelpo.frplm.core.entities.fields.kinds.FieldType;
import io.github.chechelpo.frplm.core.entities.pseudo_services.ABSControllerAwareHelper;
import io.github.chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import org.springframework.stereotype.Component;

import static io.github.chechelpo.frplm.jooq.generated.Tables.SESSIONS;

@Component
final class SessionFieldsHelper extends ABSControllerAwareHelper<SessionsRecord, SessionService, SessionController> {
    SessionFieldsHelper(SessionService service, SessionController controller) {
        super(service, controller);

        register_field(
                "id",
                SESSIONS.ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .key()
                                .readOnly()
                        )
                        .build()
        );

        register_field(
                "current_tick",
                SESSIONS.CURRENT_TICK,
                FieldInfo.numberField(FieldType.INTEGER)
                        .build()
                ,
                0
        );
        register_field(
                "name",
                SESSIONS.NAME,
                FieldInfo.stringField()
                        .build()
        );

        register_field(
                "user_id",
                SESSIONS.USER_PERSONA_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER))
                        .require()
                        .build()
        );

        register_field(
                "world_id",
                SESSIONS.WORLD_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(
                                NumberConstraint.builder(FieldType.INTEGER)
                                .readOnly()
                        )
                        .require()
                        .build()
        );


        register_field(
                "template_id",
                SESSIONS.MAIN_PROMPT,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .nullable()
                        )
                        .build()
        );

    }
}
