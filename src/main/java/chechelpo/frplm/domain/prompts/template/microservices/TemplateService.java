package chechelpo.frplm.domain.prompts.template.microservices;

import chechelpo.frplm.frameworks.entities.microservices.ABSEntityService;
import chechelpo.frplm.jooq.generated.tables.records.PromptTemplateRecord;
import org.springframework.stereotype.Service;

@Service
public final class TemplateService extends ABSEntityService<PromptTemplateRecord, TemplateStore>{
    TemplateService(TemplateStore store) {
        super(store);
    }
}
