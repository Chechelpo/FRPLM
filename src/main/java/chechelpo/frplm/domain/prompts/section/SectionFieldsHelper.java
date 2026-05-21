package chechelpo.frplm.domain.prompts.section;

import chechelpo.frplm.domain.connection.llm.utils.generationRequest.ChatMessage;
import chechelpo.frplm.frameworks.entities.fields.FieldInfo;
import chechelpo.frplm.frameworks.entities.fields.constraints.NumberConstraints;
import chechelpo.frplm.frameworks.entities.fields.constraints.StringConstraints;
import chechelpo.frplm.frameworks.entities.fields.kinds.FieldType;
import chechelpo.frplm.frameworks.entities.microservices.ABSControllerAwareHelper;
import chechelpo.frplm.jooq.generated.tables.records.PromptSectionRecord;
import org.springframework.stereotype.Component;

import static chechelpo.frplm.jooq.generated.Tables.PROMPT_SECTION;

@Component
final class SectionFieldsHelper extends ABSControllerAwareHelper<PromptSectionRecord, SectionService, SectionController> {
    SectionFieldsHelper(SectionService service, SectionController controller) {
        super(service, controller);

        register_field(
                "prompt_id",
                PROMPT_SECTION.PROMPT_ID,
                FieldInfo.numberField(FieldType.INTEGER)
                        .setConstraints(NumberConstraints.builder(FieldType.INTEGER)
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
                        .setConstraints(NumberConstraints.builder(FieldType.SHORT)
                                .key()
                                .readOnly()
                        )
                        .build()
        );

        register_field(
                "name",
                PROMPT_SECTION.NAME,
                FieldInfo.stringField()
                        .setConstraints(StringConstraints.builder()
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
                        .setConstraints(NumberConstraints.builder(FieldType.SHORT))
                        .build()
        );

        register_field(
                "role",
                PROMPT_SECTION.ROLE,
                FieldInfo.stringField()
                        .setConstraints(StringConstraints.builder()
                                .setMaxLength(9)
                                .setPossibleValues(ChatMessage.Role.wireValues()))
                        .build()
        );

        register_field(
                "content",
                PROMPT_SECTION.CONTENT,
                FieldInfo.stringField()
                        .setConstraints(StringConstraints.builder().allows_outlets())
                        .build()
        );
    }
}
