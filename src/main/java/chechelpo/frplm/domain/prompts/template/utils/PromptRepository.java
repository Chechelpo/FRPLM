package chechelpo.frplm.domain.prompts.template.utils;

import chechelpo.frplm.domain.connection.llm.utils.LLMFactory;
import chechelpo.frplm.domain.prompts.template.microservices.TemplateService;
import chechelpo.frplm.frameworks.entities.repository.EntityRepository;
import chechelpo.frplm.jooq.generated.tables.records.PromptTemplateRecord;
import org.springframework.stereotype.Component;

@Component
final class PromptRepository extends EntityRepository<PromptTemplateRecord, TemplateService> {
    private final LLMFactory llmFactory;
    PromptRepository(TemplateService service, LLMFactory llmFactory) {
        super(service);
        this.llmFactory = llmFactory;
    }
}
