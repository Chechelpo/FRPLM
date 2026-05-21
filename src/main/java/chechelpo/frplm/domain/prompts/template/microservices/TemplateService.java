package chechelpo.frplm.domain.prompts.template.microservices;

import chechelpo.frplm.events.EventBus;
import chechelpo.frplm.frameworks.entities.microservices.EntityService;
import chechelpo.frplm.jooq.generated.tables.records.PromptTemplateRecord;
import org.springframework.stereotype.Service;

@Service
public class TemplateService extends EntityService<PromptTemplateRecord, TemplateStore> {
    TemplateService(TemplateStore store, EventBus bus) {
        super(store, bus);
    }
}
