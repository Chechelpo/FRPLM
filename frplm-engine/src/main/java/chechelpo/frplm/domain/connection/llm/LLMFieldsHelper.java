package chechelpo.frplm.domain.connection.llm;

import chechelpo.frplm.core.entities.fields.FieldInfo;
import chechelpo.frplm.core.entities.fields.constraints.NumberConstraint;
import chechelpo.frplm.core.entities.fields.constraints.StringConstraint;
import chechelpo.frplm.core.entities.fields.kinds.FieldType;
import chechelpo.frplm.core.entities.pseudo_services.ABSControllerAwareHelper;
import chechelpo.frplm.jooq.generated.tables.LlmConnection;
import chechelpo.frplm.jooq.generated.tables.records.LlmConnectionRecord;
import org.springframework.stereotype.Component;

@Component
final class LLMFieldsHelper extends ABSControllerAwareHelper<LlmConnectionRecord, LLMService, LLMController> {
    LLMFieldsHelper(LLMService service, LLMController controller) {
        super(service, controller);

        register_field(
                "id",
                LlmConnection.LLM_CONNECTION.ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .readOnly()
                                .key()
                                .build()
                        )
                        .build()
        );

        register_field(
                "name",
                LlmConnection.LLM_CONNECTION.NAME,
                FieldInfo.stringField()
                        .setConstraints(StringConstraint.builder()
                                .setMaxLength(255)
                                .build()
                        )
                        .build()
        );

        register_field(
                "host_id",
                LlmConnection.LLM_CONNECTION.HOST_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(
                                NumberConstraint.builder(FieldType.INTEGER)
                                        .nullable()
                        )
                        .build()
        );

        register_field(
                "modelID",
                LlmConnection.LLM_CONNECTION.MODEL,
                FieldInfo.stringField()
                        .setConstraints(StringConstraint.builder()
                                .setMaxLength(255)
                        )
                        .build()
        );

        register_field(
                "max_tokens",
                LlmConnection.LLM_CONNECTION.MAX_TOKENS,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .setMin(0l)
                        )
                        .build()
        );
    }

}
