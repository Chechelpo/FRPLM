package io.github.chechelpo.frplm.domain.connection.llm;

import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.constraints.IntegerConstraint;
import io.github.chechelpo.frplm.core.entities.fields.constraints.StringConstraint;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityControllerFieldValidator;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import io.github.chechelpo.frplm.jooq.generated.tables.records.LlmConnectionRecord;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.tables.LlmConnection.LLM_CONNECTION;

@Component
final class LLMFieldsHelper extends EntityControllerFieldValidator<LlmConnectionRecord> {

    LLMFieldsHelper() {
        super(EntityConfigs.Types.LLM_CONNECTION);
    }

    @Override
    protected List<FieldInfo<LlmConnectionRecord, ?>> getCustom() {
        return List.of(
                FieldInfo.builder(LLM_CONNECTION.ID)
                        .readOnly()
                        .key()
                        .build(),

                FieldInfo.builder(LLM_CONNECTION.NAME)
                        .setConstraints(
                                StringConstraint.builder()
                                        .setMaxLength(255)
                        )
                        .build(),

                FieldInfo.builder(LLM_CONNECTION.HOST_ID)
                        .nullable()
                        .build(),

                FieldInfo.builder(LLM_CONNECTION.MODEL)
                        .setConstraints(
                                StringConstraint.builder()
                                        .setMaxLength(255)
                        )
                        .build(),

                FieldInfo.builder(LLM_CONNECTION.MAX_TOKENS)
                        .setConstraints(
                                IntegerConstraint.builder().setMin(0)
                        )
                        .build()
        );
    }

    @Override
    protected List<DTOField<LlmConnectionRecord, ?>> getDTOStructure() {
        return List.of(
                DTOField.of(LLM_CONNECTION.ID,         "id"),
                DTOField.of(LLM_CONNECTION.NAME,       "name"),
                DTOField.of(LLM_CONNECTION.HOST_ID,    "host_id"),
                DTOField.of(LLM_CONNECTION.MODEL,      "modelID"),
                DTOField.of(LLM_CONNECTION.MAX_TOKENS, "max_tokens")
        );
    }
}

