package io.github.chechelpo.frplm.domain.sessions.messages;

import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.constraints.NumberConstraint;
import io.github.chechelpo.frplm.core.entities.fields.constraints.StringConstraint;
import io.github.chechelpo.frplm.core.entities.fields.kinds.FieldType;
import io.github.chechelpo.frplm.core.entities.pseudo_services.ABSControllerAwareHelper;
import io.github.chechelpo.frplm.jooq.generated.tables.records.MessagesRecord;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionRole;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import static io.github.chechelpo.frplm.jooq.generated.Tables.MESSAGES;

@Component
@Lazy(false)
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
                "time",
                MESSAGES.TIME,
                FieldInfo.numberField(FieldType.INTEGER)
                        .build(),
                0
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
                "prompt",
                MESSAGES.REQUEST_JSON,
                FieldInfo.stringField()
                        .build()
        );

        register_field(
                "world_id",
                MESSAGES.WORLD_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .require()
                        .build()
        );
        register_field(
                "location_id",
                MESSAGES.LOCATION_ID,
                FieldInfo.numberField(FieldType.INTEGER)
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

        register_field(
                "active_response",
                MESSAGES.ACTIVE_RESPONSE,
                FieldInfo.numberField(FieldType.SHORT)
                        .build()
        );
        register_field(
                "response_num",
                MESSAGES.RESPONSE_NUM,
                FieldInfo.numberField(FieldType.INTEGER)
                        .build()
        );

    }
}
