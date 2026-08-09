package io.github.chechelpo.frplm.domain.sessions.messages;

import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.EntityFieldsValidator;
import io.github.chechelpo.frplm.jooq.generated.tables.records.ResponsesRecord;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.RESPONSES;

@Component
final class ResponseFields
        extends EntityFieldsValidator<ResponsesRecord> {

    @Override
    protected List<FieldInfo<ResponsesRecord, ?>> getCustom() {
        return List.of(
                FieldInfo.builder(RESPONSES.SESSION_ID)
                        .key()
                        .requireOnCreate()
                        .build(),

                FieldInfo.builder(RESPONSES.TICK_NUM)
                        .key()
                        .requireOnCreate()
                        .build(),

                FieldInfo.builder(RESPONSES.RESPONSE_NUM)
                        .key()
                        .requireOnCreate()
                        .build(),

                FieldInfo.builder(RESPONSES.CONTENT)
                        .requireOnCreate()
                        .build(),

                FieldInfo.builder(RESPONSES.REASONING)
                        .nullable()
                        .build(),

                FieldInfo.builder(RESPONSES.ADVANCES_TIME_BY)
                        .setDefaultValue(0)
                        .build(),

                FieldInfo.builder(RESPONSES.WORLD_ID)
                        .requireOnCreate()
                        .build(),

                FieldInfo.builder(RESPONSES.LOCATION_ID)
                        .requireOnCreate()
                        .build()
        );
    }
}