package chechelpo.frplm.domain.sessions.messages.gen;

import chechelpo.frplm.core.entities.fields.FieldInfo;
import chechelpo.frplm.core.entities.fields.constraints.NumberConstraint;
import chechelpo.frplm.core.entities.fields.constraints.StringConstraint;
import chechelpo.frplm.core.entities.fields.kinds.FieldType;
import chechelpo.frplm.core.entities.pseudo_services.ABSHelper;
import chechelpo.frplm.jooq.generated.tables.records.LlmGenRecord;
import org.springframework.stereotype.Component;

import static chechelpo.frplm.jooq.generated.Tables.LLM_GEN;

@Component
final class GenFieldsHelper extends ABSHelper<LlmGenRecord, GenService> {
    GenFieldsHelper(GenService service) {
        super(service);

        register_field(
                LLM_GEN.SESSION_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .key()
                                .readOnly()
                        )
                        .require()
                        .build()
        );

        register_field(
                LLM_GEN.TICK_NUM,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .key()
                                .readOnly()
                        )
                        .require()
                        .build()
        );

        register_field(
                LLM_GEN.PROMPT,
                FieldInfo.stringField()
                        .setConstraints(StringConstraint.builder())
                        .require()
                        .build()
        );

        register_field(
                LLM_GEN.ACTIVE_RESPONSE,
                FieldInfo.numberField(FieldType.INTEGER)
                        .build()
        );
        register_field(
                LLM_GEN.RESPONSE_NUM,
                FieldInfo.numberField(FieldType.INTEGER)
                        .build()
        );
    }
}
