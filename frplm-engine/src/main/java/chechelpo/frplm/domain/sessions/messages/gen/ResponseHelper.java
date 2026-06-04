package chechelpo.frplm.domain.sessions.messages.gen;

import chechelpo.frplm.core.entities.fields.FieldInfo;
import chechelpo.frplm.core.entities.fields.constraints.NumberConstraint;
import chechelpo.frplm.core.entities.fields.kinds.FieldType;
import chechelpo.frplm.core.entities.pseudo_services.ABSHelper;
import chechelpo.frplm.jooq.generated.tables.records.ResponsesRecord;
import org.springframework.stereotype.Component;

import static chechelpo.frplm.jooq.generated.Tables.RESPONSES;

@Component
final class ResponseHelper extends ABSHelper<ResponsesRecord, ResponseService> {
    ResponseHelper(ResponseService service) {
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
                RESPONSES.ADVANCES_TIME_BY,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
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

    }
}
