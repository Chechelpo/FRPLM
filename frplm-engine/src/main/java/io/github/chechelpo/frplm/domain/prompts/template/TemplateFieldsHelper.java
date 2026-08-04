package io.github.chechelpo.frplm.domain.prompts.template;

import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.constraints.FloatConstraint;
import io.github.chechelpo.frplm.core.entities.fields.constraints.IntegerConstraint;
import io.github.chechelpo.frplm.core.entities.fields.constraints.StringConstraint;
import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityControllerFieldValidator;
import io.github.chechelpo.frplm.domain.prompts.section.DefaultSections;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ReasoningEffort;
import io.github.chechelpo.frplm.jooq.generated.tables.records.PromptTemplateRecord;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.PROMPT_TEMPLATE;

@Component
final class TemplateFieldsHelper
        extends EntityControllerFieldValidator<PromptTemplateRecord> {
    TemplateFieldsHelper() {
        super(EntityConfigs.Types.PROMPT_TEMPLATES);
    }

    @Override
    protected List<DTOField<PromptTemplateRecord, ?>> getDTOStructure() {
        return List.of(
                DTOField.of(PROMPT_TEMPLATE.ID, "id"),
                DTOField.of(PROMPT_TEMPLATE.CONNECTION_ID, "connection_id"),
                DTOField.of(PROMPT_TEMPLATE.NAME, "name"),
                DTOField.of(PROMPT_TEMPLATE.MAX_TOKENS, "max_tokens"),
                DTOField.of(PROMPT_TEMPLATE.STREAMING, "streaming"),

                DTOField.of(PROMPT_TEMPLATE.TEMPERATURE, "temperature"),
                DTOField.of(PROMPT_TEMPLATE.TOP_P, "top_p"),
                DTOField.of(PROMPT_TEMPLATE.FREQUENCY_PENALTY, "frequency_penalty"),
                DTOField.of(PROMPT_TEMPLATE.PRESENCE_PENALTY, "presence_penalty"),
                DTOField.of(PROMPT_TEMPLATE.REPETITION_PENALTY, "repetition_penalty"),
                DTOField.of(PROMPT_TEMPLATE.TOP_K, "top_k"),

                DTOField.of(PROMPT_TEMPLATE.EXCLUDE_REASONING, "exclude_reasoning"),
                DTOField.of(PROMPT_TEMPLATE.REASONING_EFFORT, "reasoning_effort"),

                DTOField.of(PROMPT_TEMPLATE.CHAT_HISTORY_BUDGET, "chat_history_budget"),
                DTOField.of(PROMPT_TEMPLATE.LOREBOOKS_BUDGET, "lorebooks_budget")
        );
    }

    @Override
    protected List<FieldInfo<PromptTemplateRecord, ?>> getCustom() {
        return List.of(
                FieldInfo.builder(PROMPT_TEMPLATE.ID)
                        .key()
                        .build(),

                FieldInfo.builder(PROMPT_TEMPLATE.CONNECTION_ID)
                        .nullable()
                        .build(),

                FieldInfo.builder(PROMPT_TEMPLATE.NAME)
                        .setConstraints(
                                StringConstraint.builder()
                                        .setMaxLength(255)
                        )
                        .build(),

                FieldInfo.builder(PROMPT_TEMPLATE.MAX_TOKENS)
                        .setConstraints(
                                IntegerConstraint.builder()
                                        .setMin(0)
                        )
                        .build(),

                FieldInfo.builder(PROMPT_TEMPLATE.STREAMING)
                        .build(),

                FieldInfo.builder(PROMPT_TEMPLATE.TEMPERATURE)
                        .setConstraints(
                                FloatConstraint.builder()
                                        .setMin(0F)
                                        .setMax(2F)
                        )
                        .build(),

                FieldInfo.builder(PROMPT_TEMPLATE.TOP_P)
                        .setConstraints(
                                FloatConstraint.builder()
                                        .setMin(0F)
                                        .setMax(2F)
                        )
                        .build(),

                FieldInfo.builder(PROMPT_TEMPLATE.FREQUENCY_PENALTY)
                        .setConstraints(
                                FloatConstraint.builder()
                                        .setMin(0F)
                                        .setMax(2F)
                        )
                        .build(),

                FieldInfo.builder(PROMPT_TEMPLATE.PRESENCE_PENALTY)
                        .setConstraints(
                                FloatConstraint.builder()
                                        .setMin(0F)
                                        .setMax(2F)
                        )
                        .build(),

                FieldInfo.builder(PROMPT_TEMPLATE.REPETITION_PENALTY)
                        .setConstraints(
                                FloatConstraint.builder()
                                        .setMin(0F)
                                        .setMax(2F)
                        )
                        .build(),

                FieldInfo.builder(PROMPT_TEMPLATE.TOP_K)
                        .setConstraints(
                                IntegerConstraint.builder()
                                        .setMin(0)
                        )
                        .build(),

                FieldInfo.builder(PROMPT_TEMPLATE.EXCLUDE_REASONING)
                        .build(),

                FieldInfo.builder(PROMPT_TEMPLATE.REASONING_EFFORT)
                        .addAllowedValues(ReasoningEffort.possible_values())
                        .setDefaultValue(ReasoningEffort.Maximum.id)
                        .build(),

                FieldInfo.builder(PROMPT_TEMPLATE.CHAT_HISTORY_BUDGET)
                        .setConstraints(
                                FloatConstraint.builder()
                                        .setMin(0F)
                                        .setMax(1F)
                        )
                        .build(),

                FieldInfo.builder(PROMPT_TEMPLATE.LOREBOOKS_BUDGET)
                        .setConstraints(
                                FloatConstraint.builder()
                                        .setMin(0F)
                                        .setMax(1F)
                        )
                        .build(),

                FieldInfo.builder(PROMPT_TEMPLATE.NEXT_SECTION_ID)
                        .readOnly()
                        .setDefaultValue(DefaultSections.maxReservedSectionID())
                        .build()
        );
    }
}