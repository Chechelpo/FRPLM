package io.github.chechelpo.frplm.domain.sessions.messages;

import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.constraints.NumberConstraint;
import io.github.chechelpo.frplm.core.entities.fields.kinds.FieldType;
import io.github.chechelpo.frplm.core.entities.pseudo_services.ABSHelper;
import io.github.chechelpo.frplm.jooq.generated.tables.records.ResponsesRecord;
import org.springframework.stereotype.Component;

import static io.github.chechelpo.frplm.jooq.generated.Tables.RESPONSES;

@Component
final class ResponseFields extends ABSHelper<ResponsesRecord, ResponseService> {
    ResponseFields(ResponseService service) {
        super(service);

        register_field(
                RESPONSES.SESSION_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .key()
                                .readOnly()
                        )
                        .require()
                        .build()
        );
        register_field(
                RESPONSES.TICK_NUM,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .key()
                                .readOnly()
                        )
                        .require()
                        .build()
        );
        register_field(
                RESPONSES.RESPONSE_NUM,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .key()
                                .readOnly()
                        )
                        .require()
                        .build()
        );

        register_field(
                RESPONSES.CONTENT,
                FieldInfo.stringField()
                        .require()
                        .build()
        );
        register_field(
                RESPONSES.ADVANCES_TIME_BY,
                FieldInfo.numberField(FieldType.INTEGER)
                        .require()
                        .build(),
                0
        );
        register_field(
                RESPONSES.WORLD_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .require()
                        .build()
        );
        register_field(
                RESPONSES.LOCATION_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .require()
                        .build()
        );
    }
}
