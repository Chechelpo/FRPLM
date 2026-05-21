package chechelpo.frplm.domain.sessions.core.microservices;

import chechelpo.frplm.frameworks.entities.fields.FieldInfo;
import chechelpo.frplm.frameworks.entities.fields.constraints.NumberConstraints;
import chechelpo.frplm.frameworks.entities.fields.kinds.FieldType;
import chechelpo.frplm.frameworks.entities.microservices.ABSControllerAwareHelper;
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
                        .setConstraints(NumberConstraints.builder(FieldType.INTEGER)
                                .key()
                                .readOnly()
                        )
                        .build()
        );

        register_field(
                "world_id",
                SESSIONS.WORLD_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraints.builder(FieldType.INTEGER)
                                .readOnly()
                        )
                        .require()
                        .build()
        );

        register_field(
                "user_id",
                SESSIONS.USER_PERSONA_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraints.builder(FieldType.INTEGER))
                        .require()
                        .build()
        );

        register_field(
                "name",
                SESSIONS.WORLD_ID,
                FieldInfo.stringField()
                        .require()
                        .build()
        );
    }
}
