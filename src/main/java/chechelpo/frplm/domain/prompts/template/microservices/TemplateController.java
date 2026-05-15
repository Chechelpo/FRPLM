package chechelpo.frplm.domain.prompts.template.microservices;

import chechelpo.frplm.frameworks.entities.microservices.ABSEntityController;
import chechelpo.frplm.jooq.generated.tables.records.PromptTemplateRecord;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static chechelpo.frplm.domain.EntityTypes.PROMPT_TEMPLATES_URL;

@RestController
@RequestMapping(PROMPT_TEMPLATES_URL)
final class TemplateController extends ABSEntityController<PromptTemplateRecord, TemplateService> {
    TemplateController(TemplateService service) {
        super(service);
    }
}
