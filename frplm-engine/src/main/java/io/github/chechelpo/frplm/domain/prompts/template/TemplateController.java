package io.github.chechelpo.frplm.domain.prompts.template;

import io.github.chechelpo.frplm.core.entities.pseudo_services.EntityController;
import io.github.chechelpo.frplm.jooq.generated.tables.records.PromptTemplateRecord;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static io.github.chechelpo.frplm.extensions.api.utils.EntityConfigs.PROMPT_TEMPLATES_URL;

@RestController
@RequestMapping(PROMPT_TEMPLATES_URL)
final class TemplateController extends EntityController<PromptTemplateRecord, TemplateService> {
    TemplateController(TemplateService service) {
        super(service);
    }
}
