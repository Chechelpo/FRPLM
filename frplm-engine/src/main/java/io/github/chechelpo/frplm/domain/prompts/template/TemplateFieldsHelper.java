package io.github.chechelpo.frplm.domain.prompts.template;

import io.github.chechelpo.frplm.domain.prompts.section.DefaultSections;
import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.constraints.FloatConstraint;
import io.github.chechelpo.frplm.core.entities.fields.constraints.NumberConstraint;
import io.github.chechelpo.frplm.core.entities.fields.constraints.StringConstraint;
import io.github.chechelpo.frplm.core.entities.fields.kinds.FieldType;
import io.github.chechelpo.frplm.core.entities.pseudo_services.ABSControllerAwareHelper;
import io.github.chechelpo.frplm.jooq.generated.tables.records.PromptTemplateRecord;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ReasoningEffort;
import org.springframework.stereotype.Component;

import static io.github.chechelpo.frplm.jooq.generated.Tables.PROMPT_TEMPLATE;

@Component
final class TemplateFieldsHelper extends ABSControllerAwareHelper<
        PromptTemplateRecord,
        TemplateService,
        TemplateController
        > {

     TemplateFieldsHelper(TemplateService service, TemplateController controller) {
        super(service, controller);

        register_field(
                "id",
                PROMPT_TEMPLATE.ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .readOnly()
                                .key()
                        )
                        .build()
        );
        register_field(
                "connection_id",
                PROMPT_TEMPLATE.CONNECTION_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint
                                .builder(FieldType.INTEGER)
                                .nullable()
                        )
                        .build()
        );

        register_field(
                "name",
                PROMPT_TEMPLATE.NAME,
                FieldInfo.stringField()
                        .setConstraints(StringConstraint.builder().setMaxLength(255))
                        .build()
        );

        register_field(
                "max_tokens",
                PROMPT_TEMPLATE.MAX_TOKENS,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraint.builder(FieldType.INTEGER)
                                .setMin(0L)
                        )
                        .build()
        );
        register_field(
                "streaming",
                PROMPT_TEMPLATE.STREAMING,
                FieldInfo.booleanField()
                        .build()
        );

        // Gen params
        register_field(
                "temperature",
                PROMPT_TEMPLATE.TEMPERATURE,
                FieldInfo.floatField(FieldType.FLOAT)
                        .setConstraints(FloatConstraint.builder(FieldType.FLOAT)
                                .setMax(2D)
                                .setMin(0D)
                        )
                        .build()
        );
        register_field(
                "top_p",
                PROMPT_TEMPLATE.TOP_P,
                FieldInfo.floatField(FieldType.FLOAT)
                        .setConstraints(FloatConstraint.builder(FieldType.FLOAT)
                                .setMax(2D)
                                .setMin(0D)
                        )
                        .build()
        );
        register_field(
                "frequency_penalty",
                PROMPT_TEMPLATE.FREQUENCY_PENALTY,
                FieldInfo.floatField(FieldType.FLOAT)
                        .setConstraints(FloatConstraint.builder(FieldType.FLOAT)
                                .setMax(2D)
                                .setMin(0D)
                        )
                        .build()
        );
        register_field(
                "presence_penalty",
                PROMPT_TEMPLATE.PRESENCE_PENALTY,
                FieldInfo.floatField(FieldType.FLOAT)
                        .setConstraints(FloatConstraint.builder(FieldType.FLOAT)
                                .setMax(2D)
                                .setMin(0D)
                        )
                        .build()
        );
        register_field(
                "repetition_penalty",
                PROMPT_TEMPLATE.REPETITION_PENALTY,
                FieldInfo.floatField(FieldType.FLOAT)
                        .setConstraints(FloatConstraint.builder(FieldType.FLOAT)
                                .setMax(2D)
                                .setMin(0D)
                        )
                        .build()
        );
        register_field(
                "top_k",
                PROMPT_TEMPLATE.TOP_K,
                FieldInfo.floatField(FieldType.FLOAT)
                        .setConstraints(FloatConstraint.builder(FieldType.FLOAT)
                                .setMax(2D)
                                .setMin(0D)
                        )
                        .build()
        );

        register_field(
                "exclude_reasoning",
                PROMPT_TEMPLATE.EXCLUDE_REASONING,
                FieldInfo.booleanField()
                        .build()
        );
        register_field(
                "reasoning_effort",
                PROMPT_TEMPLATE.REASONING_EFFORT,
                FieldInfo.numberField(FieldType.SHORT)
                        .setConstraints(NumberConstraint.builder(FieldType.SHORT)
                                .setPossibleValues(ReasoningEffort.possible_values())
                        )
                        .build(),
                (short) ReasoningEffort.Maximum.id
        );

        register_field(
                null,
                PROMPT_TEMPLATE.NEXT_SECTION_ID,
                FieldInfo.numberField(FieldType.SHORT)
                        .setConstraints(NumberConstraint.builder(FieldType.SHORT)
                                .readOnly()
                        )
                        .build(),
                DefaultSections.maxReservedSectionID()
        );
    }
}
