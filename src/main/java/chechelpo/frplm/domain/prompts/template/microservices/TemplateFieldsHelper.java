package chechelpo.frplm.domain.prompts.template.microservices;

import chechelpo.frplm.domain.prompts.template.ReasoningEffort;
import chechelpo.frplm.frameworks.entities.fields.FieldInfo;
import chechelpo.frplm.frameworks.entities.fields.constraints.FloatConstraints;
import chechelpo.frplm.frameworks.entities.fields.constraints.NumberConstraints;
import chechelpo.frplm.frameworks.entities.fields.constraints.StringConstraints;
import chechelpo.frplm.frameworks.entities.fields.kinds.FieldType;
import chechelpo.frplm.frameworks.entities.microservices.ABSControllerAwareHelper;
import chechelpo.frplm.jooq.generated.tables.records.PromptTemplateRecord;
import org.springframework.stereotype.Component;

import static chechelpo.frplm.jooq.generated.Tables.PROMPT_TEMPLATE;

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
                        .setConstraints(NumberConstraints.builder(FieldType.INTEGER)
                                .readOnly()
                                .key()
                        )
        );
        register_field(
                "connection_id",
                PROMPT_TEMPLATE.CONNECTION_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraints
                                .builder(FieldType.INTEGER)
                                .nullable()
                        )
        );

        register_field(
                "name",
                PROMPT_TEMPLATE.NAME,
                FieldInfo.stringField()
                        .setConstraints(StringConstraints.builder().setMaxLength(255))
        );

        register_field(
                "max_tokens",
                PROMPT_TEMPLATE.MAX_TOKENS,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraints.builder(FieldType.INTEGER)
                                .setMin(0L)
                        )
        );
        register_field(
                "streaming",
                PROMPT_TEMPLATE.STREAMING,
                FieldInfo.booleanField()
        );

        // Gen params
        register_field(
                "temperature",
                PROMPT_TEMPLATE.TEMPERATURE,
                FieldInfo.floatField(FieldType.FLOAT)
                        .setConstraints(FloatConstraints.builder(FieldType.FLOAT)
                                .setMax(2D)
                                .setMin(0D)
                        )
        );
        register_field(
                "top_p",
                PROMPT_TEMPLATE.TOP_P,
                FieldInfo.floatField(FieldType.FLOAT)
                        .setConstraints(FloatConstraints.builder(FieldType.FLOAT)
                                .setMax(2D)
                                .setMin(0D)
                        )
        );
        register_field(
                "frequency_penalty",
                PROMPT_TEMPLATE.FREQUENCY_PENALTY,
                FieldInfo.floatField(FieldType.FLOAT)
                        .setConstraints(FloatConstraints.builder(FieldType.FLOAT)
                                .setMax(2D)
                                .setMin(0D)
                        )
        );
        register_field(
                "presence_penalty",
                PROMPT_TEMPLATE.PRESENCE_PENALTY,
                FieldInfo.floatField(FieldType.FLOAT)
                        .setConstraints(FloatConstraints.builder(FieldType.FLOAT)
                                .setMax(2D)
                                .setMin(0D)
                        )
        );
        register_field(
                "repetition_penalty",
                PROMPT_TEMPLATE.REPETITION_PENALTY,
                FieldInfo.floatField(FieldType.FLOAT)
                        .setConstraints(FloatConstraints.builder(FieldType.FLOAT)
                                .setMax(2D)
                                .setMin(0D)
                        )
        );
        register_field(
                "top_k",
                PROMPT_TEMPLATE.TOP_K,
                FieldInfo.floatField(FieldType.FLOAT)
                        .setConstraints(FloatConstraints.builder(FieldType.FLOAT)
                                .setMax(2D)
                                .setMin(0D)
                        )
        );

        register_field(
                "exclude_reasoning",
                PROMPT_TEMPLATE.EXCLUDE_REASONING,
                FieldInfo.booleanField()
        );
        register_field(
                "reasoning_effort",
                PROMPT_TEMPLATE.REASONING_EFFORT,
                FieldInfo.numberField(FieldType.SHORT)
                        .setConstraints(NumberConstraints.builder(FieldType.SHORT)
                                .setPossibleValues(ReasoningEffort.possible_values())
                        )
        );
    }
}
