package chechelpo.frplm.domain.prompts.section;

import chechelpo.frplm.domain.prompts.template.microservices.TemplateService;
import chechelpo.frplm.frameworks.entities.microservices.ABSEntityService;
import chechelpo.frplm.frameworks.entities.microservices.EntityDataPayload;
import chechelpo.frplm.frameworks.entities.microservices.EntityKey;
import chechelpo.frplm.jooq.generated.tables.PromptSection;
import chechelpo.frplm.jooq.generated.tables.PromptTemplate;
import chechelpo.frplm.jooq.generated.tables.records.PromptSectionRecord;
import chechelpo.frplm.jooq.generated.tables.records.PromptTemplateRecord;
import org.springframework.stereotype.Service;

import static chechelpo.frplm.jooq.generated.Tables.PROMPT_SECTION;
import static chechelpo.frplm.jooq.generated.Tables.PROMPT_TEMPLATE;

@Service
public final class SectionService extends ABSEntityService<PromptSectionRecord, SectionStore> {
    private final TemplateService templateService;
    SectionService(SectionStore store, TemplateService templateService) {
        super(store);
        this.templateService = templateService;
    }

    @Override
    protected EntityDataPayload<PromptSectionRecord> beforeCreate(EntityDataPayload<PromptSectionRecord> data) {
        EntityKey.Builder<PromptTemplateRecord> builder = EntityKey.builder();
        coercePayload(data);
        data.setValue(
                PromptSection.PROMPT_SECTION.SECTION_ID,
                templateService.getAndIncrement(
                        PROMPT_TEMPLATE.NEXT_SECTION_ID,
                        builder.set(PROMPT_TEMPLATE.ID, data.getValue(PROMPT_SECTION.PROMPT_ID).shortValue()).build()
                        )
        );
        data.setValue(
                PROMPT_SECTION.POSITION,
                data.getValue(PROMPT_SECTION.SECTION_ID)
        );
        return super.beforeCreate(data);
    }
}
