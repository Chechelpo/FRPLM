package chechelpo.frplm.domain.sessions.messages.core;

import chechelpo.frplm.frameworks.entities.fields.FieldInfo;
import chechelpo.frplm.frameworks.entities.fields.constraints.NumberConstraint;
import chechelpo.frplm.frameworks.entities.fields.constraints.StringConstraint;
import chechelpo.frplm.frameworks.entities.fields.kinds.FieldType;
import chechelpo.frplm.frameworks.entities.pseudo_services.ABSControllerAwareHelper;
import chechelpo.frplm.jooq.generated.tables.records.MessagesRecord;
import chechelpo.frplm.openai_compatible.ChatCompletionRole;
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
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
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
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .key()
                                .readOnly()
                        )
                        .build()
        );

        register_field(
                "world_id",
                MESSAGES.WORLD_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .build()
        );
        register_field(
                "location_id",
                MESSAGES.LOCATION_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .build()
        );
        register_field(
                "time",
                MESSAGES.TIME,
                FieldInfo.numberField(FieldType.INTEGER)
                        .build()
        );

        register_field(
                "role",
                MESSAGES.ROLE,
                FieldInfo.stringField()
                        .setConstraints(StringConstraint.builder()
                                .setPossibleValues(ChatCompletionRole.wireValues())
                        )
                        .require()
                        .build()
        );

        register_field(
                "content",
                MESSAGES.CONTENT,
                FieldInfo.stringField()
                        .setConstraints(StringConstraint.builder()
                                .nullable()
                        )
                        .require()
                        .build()
        );

    }
}
