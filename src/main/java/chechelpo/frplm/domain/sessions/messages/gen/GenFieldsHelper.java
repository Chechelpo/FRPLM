package chechelpo.frplm.domain.sessions.messages.gen;

import chechelpo.frplm.frameworks.entities.fields.FieldInfo;
import chechelpo.frplm.frameworks.entities.fields.constraints.NumberConstraints;
import chechelpo.frplm.frameworks.entities.fields.constraints.StringConstraints;
import chechelpo.frplm.frameworks.entities.fields.kinds.FieldType;
import chechelpo.frplm.frameworks.entities.microservices.ABSHelper;
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
                        .setConstraints(NumberConstraints.builder(FieldType.INTEGER)
                                .key()
                                .readOnly()
                        )
                        .require()
                        .build()
        );

        register_field(
                LLM_GEN.TICK_NUM,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraints.builder(FieldType.INTEGER)
                                .key()
                                .readOnly()
                        )
                        .require()
                        .build()
        );

        register_field(
                LLM_GEN.PROMPT_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraints.builder(FieldType.INTEGER))
                        .require()
                        .build()
        );
        register_field(
                LLM_GEN.PROMPT,
                FieldInfo.stringField()
                        .setConstraints(StringConstraints.builder())
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
