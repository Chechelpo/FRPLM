package chechelpo.frplm.domain.prompts.section;

import chechelpo.frplm.core.entities.fields.FieldInfo;
import chechelpo.frplm.core.entities.fields.constraints.NumberConstraint;
import chechelpo.frplm.core.entities.fields.constraints.StringConstraint;
import chechelpo.frplm.core.entities.fields.kinds.FieldType;
import chechelpo.frplm.core.entities.pseudo_services.ABSControllerAwareHelper;
import chechelpo.frplm.jooq.generated.tables.records.PromptSectionRecord;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionRole;
import org.springframework.stereotype.Component;

import static chechelpo.frplm.jooq.generated.Tables.PROMPT_SECTION;

@Component
final class SectionFieldsHelper extends ABSControllerAwareHelper<PromptSectionRecord, SectionService, SectionController> {
    SectionFieldsHelper(SectionService service, SectionController controller) {
        super(service, controller);

        register_field(
                "prompt_id",
                PROMPT_SECTION.PROMPT_ID,
                FieldInfo.numberField(FieldType.SHORT)
                        .setConstraints(NumberConstraint.builder(FieldType.SHORT)
                                .readOnly()
                                .key    ()
                        )
                        .require()
                        .build()
        );
        register_field(
                "section_id",
                PROMPT_SECTION.SECTION_ID,
                FieldInfo.numberField(FieldType.SHORT)
                        .setConstraints(NumberConstraint.builder(FieldType.SHORT)
                                .key()
                                .readOnly()
                        )
                        .build()
        );

        register_field(
                "name",
                PROMPT_SECTION.NAME,
                FieldInfo.stringField()
                        .setConstraints(StringConstraint.builder()
                                .setMaxLength(255)
                        )
                        .require()
                        .build()
        );

        register_field(
                "active",
                PROMPT_SECTION.ACTIVE,
                FieldInfo.booleanField()
                        .build()
        );
        register_field(
                "position",
                PROMPT_SECTION.POSITION,
                FieldInfo.numberField(FieldType.SHORT)
                        .setConstraints(NumberConstraint.builder(FieldType.SHORT))
                        .build()
        );

        register_field(
                "role",
                PROMPT_SECTION.ROLE,
                FieldInfo.stringField()
                        .setConstraints(StringConstraint.builder()
                                .setMaxLength(9)
                                .setPossibleValues(ChatCompletionRole.wireValues()))
                        .build()
        );

        register_field(
                "content",
                PROMPT_SECTION.CONTENT,
                FieldInfo.stringField()
                        .setConstraints(StringConstraint.builder().allows_outlets())
                        .build()
        );
    }
}
