package io.github.chechelpo.frplm.domain.prompts.section;

import io.github.chechelpo.frplm.core.entities.fields.FieldInfo;
import io.github.chechelpo.frplm.core.entities.fields.constraints.StringConstraint;
import io.github.chechelpo.frplm.core.entities.fields.EntityControllerFieldValidator;
import io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs;
import io.github.chechelpo.frplm.jooq.generated.tables.records.PromptSectionRecord;
import io.github.chechelpo.frplm.extensions.api.utils.openai_compatible.ChatCompletionRole;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.github.chechelpo.frplm.jooq.generated.Tables.PROMPT_SECTION;

@Component
final class SectionFieldsHelper extends EntityControllerFieldValidator<PromptSectionRecord> {
    SectionFieldsHelper() {
        super(EntityConfigs.Types.SECTIONS, PROMPT_SECTION);
    }

    @Override
    protected List<DTOField<PromptSectionRecord, ?>> getDTOStructure() {
        return List.of(
                DTOField.of(PROMPT_SECTION.PROMPT_ID, "prompt_id"),
                DTOField.of(PROMPT_SECTION.SECTION_ID, "section_id"),
                DTOField.of(PROMPT_SECTION.NAME, "name"),
                DTOField.of(PROMPT_SECTION.ACTIVE, "active"),
                DTOField.of(PROMPT_SECTION.POSITION, "position"),
                DTOField.of(PROMPT_SECTION.ROLE, "role"),
                DTOField.of(PROMPT_SECTION.CONTENT, "content")
        );
    }

    @Override
    protected List<FieldInfo<PromptSectionRecord, ?>> getCustom() {
        return List.of(
                FieldInfo.builder(PROMPT_SECTION.PROMPT_ID)
                        .requireOnCreate()
                        .key()
                        .build(),

                FieldInfo.builder(PROMPT_SECTION.SECTION_ID)
                        .key()
                        .build(),

                FieldInfo.builder(PROMPT_SECTION.NAME)
                        .setConstraints(
                                StringConstraint.builder()
                                    .setMaxLength(255)
                                    .build()
                        )
                        .requireOnCreate()
                        .build(),

                FieldInfo.builder(PROMPT_SECTION.ROLE)
                        .setConstraints(StringConstraint.builder().setMaxLength(255))
                        .addAllowedValues(ChatCompletionRole.wireValues())
                        .build()
        );
    }
}
