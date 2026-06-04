package chechelpo.frplm.domain.sessions.core;

import chechelpo.frplm.core.entities.fields.FieldInfo;
import chechelpo.frplm.core.entities.fields.constraints.NumberConstraint;
import chechelpo.frplm.core.entities.fields.kinds.FieldType;
import chechelpo.frplm.core.entities.pseudo_services.ABSControllerAwareHelper;
import chechelpo.frplm.jooq.generated.tables.records.SessionsRecord;
import org.springframework.stereotype.Component;

import static chechelpo.frplm.jooq.generated.Tables.SESSIONS;

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
