package io.github.chechelpo.frplm.domain.sessions.messages;

import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.EntityControllerFieldValidator;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionRole;
import io.github.chechelpo.frplm.jooq.generated.tables.records.MessagesRecord;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.MESSAGES;

@Component
@Lazy(false)
final class MessageFieldsHelper extends EntityControllerFieldValidator<MessagesRecord> {
    MessageFieldsHelper() {
        super(EntityConfigs.Types.MESSAGES, MESSAGES);
    }

    @Override
    protected List<DTOField<MessagesRecord, ?>> getDTOStructure() {
        return List.of(
                DTOField.of(MESSAGES.SESSION_ID, "session_id"),
                DTOField.of(MESSAGES.TICK_NUM, "tick_num"),
                DTOField.of(MESSAGES.IS_ENABLED, "is_enabled"),
                DTOField.of(MESSAGES.REASONING, "reasoning"),
                DTOField.of(MESSAGES.TIME, "time"),
                DTOField.of(MESSAGES.ROLE, "role"),
                DTOField.of(MESSAGES.REQUEST_JSON, "prompt"),
                DTOField.of(MESSAGES.WORLD_ID, "world_id"),
                DTOField.of(MESSAGES.LOCATION_ID, "location_id"),
                DTOField.of(MESSAGES.CONTENT, "content"),
                DTOField.of(MESSAGES.ACTIVE_RESPONSE, "active_response"),
                DTOField.of(MESSAGES.RESPONSE_NUM, "response_num")
        );
    }

    @Override
    protected List<FieldInfo<MessagesRecord, ?>> getCustom() {
        return List.of(
                FieldInfo.builder(MESSAGES.SESSION_ID)
                        .key()
                        .requireOnCreate()
                        .build(),

                FieldInfo.builder(MESSAGES.TICK_NUM)
                        .key()
                        .build(),

                FieldInfo.builder(MESSAGES.IS_ENABLED)
                        .build(),

                FieldInfo.builder(MESSAGES.REASONING)
                        .nullable()
                        .build(),

                FieldInfo.builder(MESSAGES.TIME)
                        .setDefaultValue(0)
                        .build(),

                FieldInfo.builder(MESSAGES.ROLE)
                        .addAllowedValues(ChatCompletionRole.wireValues())
                        .requireOnCreate()
                        .build(),

                FieldInfo.builder(MESSAGES.REQUEST_JSON)
                        .build(),

                FieldInfo.builder(MESSAGES.WORLD_ID)
                        .requireOnCreate()
                        .build(),

                FieldInfo.builder(MESSAGES.LOCATION_ID)
                        .requireOnCreate()
                        .build(),

                FieldInfo.builder(MESSAGES.CONTENT)
                        .nullable()
                        .requireOnCreate()
                        .build(),

                FieldInfo.builder(MESSAGES.ACTIVE_RESPONSE)
                        .build(),

                FieldInfo.builder(MESSAGES.RESPONSE_NUM)
                        .build()
        );
    }
}