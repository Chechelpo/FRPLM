package chechelpo.frplm.domain.sessions.messages.core;

import chechelpo.frplm.frameworks.entities.fields.FieldInfo;
import chechelpo.frplm.frameworks.entities.fields.constraints.NumberConstraints;
import chechelpo.frplm.frameworks.entities.fields.constraints.StringConstraints;
import chechelpo.frplm.frameworks.entities.fields.kinds.FieldType;
import chechelpo.frplm.frameworks.entities.microservices.ABSControllerAwareHelper;
import chechelpo.frplm.jooq.generated.tables.records.MessagesRecord;
import org.springframework.stereotype.Component;

import static chechelpo.frplm.jooq.generated.Tables.MESSAGES;

@Component
final class MessageFieldsHelper extends ABSControllerAwareHelper<MessagesRecord, MessageService, MessageController> {
    MessageFieldsHelper(MessageService service, MessageController controller) {
        super(service, controller);

        register_field(
                "session_id",
                MESSAGES.SESSION_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraints.builder(FieldType.INTEGER)
                                .key()
                                .readOnly()
                        )
                        .require()
                        .build()
        );

        register_field(
                "tick_num",
                MESSAGES.TICK_NUM,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraints.builder(FieldType.INTEGER)
                                .key()
                                .readOnly()
                        )
                        .build()
        );

        register_field(
                "location_id",
                MESSAGES.LOCATION_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .build()
        );

        register_field(
                "user_generated",
                MESSAGES.USER_GENERATED,
                FieldInfo.booleanField()
                        .build()
        );

        register_field(
                "content",
                MESSAGES.CONTENT,
                FieldInfo.stringField()
                        .setConstraints(StringConstraints.builder()
                                .nullable()
                        )
                        .require()
                        .build()
        );

    }
}
